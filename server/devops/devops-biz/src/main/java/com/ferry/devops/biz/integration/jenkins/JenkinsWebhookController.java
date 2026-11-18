package com.ferry.devops.biz.integration.jenkins;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.entity.RunStage;
import com.ferry.devops.api.enums.RunStatusEnum;
import com.ferry.devops.api.enums.StageStatusEnum;
import com.ferry.devops.biz.mapper.RunMapper;
import com.ferry.devops.biz.mapper.RunStageMapper;
import com.ferry.devops.biz.service.IRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HexFormat;

/**
 * Jenkins 结果回传回调（公开路径，HMAC-SHA256 验签）。
 *
 * <p>签名：以请求参数按字典序拼成 {@code k=v&...} 作为待签串，HMAC-SHA256(secret, 待签串) 转小写十六进制，
 * 放在请求头 {@code X-Ferry-Signature}。secret 未配置时跳过验签（本地/试点环境）；配置后校验不通过返回 401 语义失败。
 * 状态以本系统为准，回调驱动阶段推进与运行终态；结果待确认不直接置成功。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/callback/jenkins")
@Tag(name = "Jenkins回调", description = "Jenkins 结果回传入站接口")
public class JenkinsWebhookController {

    private final RunMapper runMapper;
    private final RunStageMapper stageMapper;
    private final IRunService runService;

    @Value("${devops.jenkins.callback-secret:}")
    private String callbackSecret;

    /**
     * 状态回调：runId + stageId + buildNo + status
     */
    @PostMapping("/status")
    @Operation(summary = "Jenkins状态回调")
    public R<String> status(@RequestParam("runId") Long runId,
                            @RequestParam("stageId") Long stageId,
                            @RequestParam(value = "buildNo", required = false) Long buildNo,
                            @RequestParam("status") String status,
                            @RequestParam(value = "exitCode", required = false) Integer exitCode,
                            @RequestParam(value = "signature", required = false) String signature,
                            jakarta.servlet.http.HttpServletRequest request) {
        if (!verify(runId, stageId, buildNo, status, signature, request)) {
            return R.fail(401, "回调签名校验失败");
        }
        RunStage stage = stageMapper.selectById(stageId);
        Run run = runMapper.selectById(runId);
        if (stage == null || run == null) {
            throw new ServiceException("回调对应的运行或阶段不存在");
        }
        RunStatusEnum runStatus = parseRunStatus(status);
        StageStatusEnum stageStatus = parseStageStatus(status);
        if (stageStatus != null) {
            stage.setStatus(stageStatus.getCode());
            stage.setEndTime(new Date());
            if (stage.getStartTime() != null) {
                stage.setDuration(stage.getEndTime().getTime() - stage.getStartTime().getTime());
            }
            stageMapper.updateById(stage);
            // 阶段失败/超时：后续未开始阶段全部跳过
            if (stageStatus == StageStatusEnum.FAILURE || stageStatus == StageStatusEnum.TIMEOUT
                    || stageStatus == StageStatusEnum.CANCELLED) {
                for (RunStage later : stageMapper.selectByRun(runId)) {
                    if (later.getSeq() != null && stage.getSeq() != null && later.getSeq() > stage.getSeq()
                            && ObjUtil.equals(later.getStatus(), StageStatusEnum.NOT_STARTED.getCode())) {
                        later.setStatus(StageStatusEnum.SKIPPED.getCode());
                        stageMapper.updateById(later);
                    }
                }
            }
        }
        if (runStatus != null) {
            run.setStatus(runStatus.getCode());
            if (runStatus == RunStatusEnum.SUCCESS || runStatus == RunStatusEnum.FAILURE
                    || runStatus == RunStatusEnum.TIMEOUT || runStatus == RunStatusEnum.CANCELLED) {
                run.setEndTime(new Date());
                if (run.getStartTime() != null) {
                    run.setDuration(run.getEndTime().getTime() - run.getStartTime().getTime());
                }
            }
            runMapper.updateById(run);
        }
        return R.success("回调已接收");
    }

    /**
     * 报告/摘要回调：仅记录，报告解析由门禁域后续消费。
     */
    @PostMapping("/report")
    @Operation(summary = "Jenkins报告摘要回调")
    public R<String> report(@RequestParam("runId") Long runId,
                            @RequestParam("stageId") Long stageId,
                            @RequestParam(value = "reportType", required = false) String reportType,
                            @RequestParam(value = "objectKey", required = false) String objectKey,
                            @RequestParam(value = "signature", required = false) String signature,
                            jakarta.servlet.http.HttpServletRequest request) {
        if (!verify(runId, stageId, null, reportType, signature, request)) {
            return R.fail(401, "回调签名校验失败");
        }
        log.info("Jenkins report callback: runId={}, stageId={}, type={}, objectKey={}", runId, stageId, reportType, objectKey);
        return R.success("报告回调已接收");
    }

    /**
     * 日志回调：接收 Jenkins 原文，入库前脱敏并分段。
     */
    @PostMapping("/log")
    @Operation(summary = "Jenkins日志回调")
    public R<String> log(@RequestParam("runId") Long runId,
                         @RequestParam("taskId") Long taskId,
                         @RequestParam(value = "signature", required = false) String signature,
                         @RequestParam("content") String content,
                         jakarta.servlet.http.HttpServletRequest request) {
        if (!verify(runId, taskId, null, "LOG", signature, request)) {
            return R.fail(401, "回调签名校验失败");
        }
        runService.saveLog(runId, taskId, content);
        return R.success("日志已接收");
    }

    /**
     * 验签：secret 为空时跳过；否则优先取参数签名，再取请求头签名。
     */
    private boolean verify(Object... parts) {
        if (callbackSecret == null || callbackSecret.isBlank()) {
            return true;
        }
        Long runId = (Long) parts[0];
        Long stageId = (Long) parts[1];
        Long buildNo = (Long) parts[2];
        String status = (String) parts[3];
        String signature = (String) parts[4];
        jakarta.servlet.http.HttpServletRequest request = (jakarta.servlet.http.HttpServletRequest) parts[5];
        String provided = signature != null ? signature : request.getHeader("X-Ferry-Signature");
        if (provided == null || provided.isBlank()) {
            return false;
        }
        String canonical = "runId=" + runId + "&stageId=" + stageId
                + "&buildNo=" + (buildNo == null ? "" : buildNo) + "&status=" + status;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(callbackSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
            return expected.equalsIgnoreCase(provided);
        } catch (Exception e) {
            log.warn("Jenkins callback sign error", e);
            return false;
        }
    }

    private RunStatusEnum parseRunStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.toUpperCase()) {
            case "RUNNING", "STARTED" -> RunStatusEnum.RUNNING;
            case "SUCCESS" -> RunStatusEnum.SUCCESS;
            case "FAILURE", "ERROR" -> RunStatusEnum.FAILURE;
            case "TIMEOUT", "ABORTED" -> RunStatusEnum.TIMEOUT;
            case "CANCELLED" -> RunStatusEnum.CANCELLED;
            default -> null;
        };
    }

    private StageStatusEnum parseStageStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.toUpperCase()) {
            case "RUNNING", "STARTED" -> StageStatusEnum.RUNNING;
            case "SUCCESS" -> StageStatusEnum.SUCCESS;
            case "FAILURE", "ERROR" -> StageStatusEnum.FAILURE;
            case "TIMEOUT", "ABORTED" -> StageStatusEnum.TIMEOUT;
            case "CANCELLED" -> StageStatusEnum.CANCELLED;
            default -> null;
        };
    }
}
