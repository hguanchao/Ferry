package com.ferry.devops.biz.service.impl;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.PipelineStageDTO;
import com.ferry.devops.api.dto.PipelineTaskDTO;
import com.ferry.devops.api.vo.ValidateResultVO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 定义校验测试：YAML 显式拒绝、执行器口径统一、结构约束与空定义口径。
 */
class PipelineDefinitionCodecTest {

    private PipelineStageDTO stage(int type, String name, List<PipelineTaskDTO> tasks, Integer timeoutSeconds) {
        PipelineStageDTO stage = new PipelineStageDTO();
        stage.setType(type);
        stage.setName(name);
        stage.setTasks(tasks);
        stage.setTimeoutSeconds(timeoutSeconds);
        return stage;
    }

    private PipelineTaskDTO task(String executor) {
        PipelineTaskDTO task = new PipelineTaskDTO();
        task.setName("任务");
        task.setExecutor(executor);
        task.setScriptContent("echo hi");
        return task;
    }

    @Test
    void YAML模式显式不通过_提示使用表单模式() {
        ValidateResultVO result = PipelineDefinitionCodec.validate("{\"stages\":[]}", 2);
        assertFalse(result.getValid());
        assertTrue(result.getErrors().get(0).getMessage().contains("YAML"));
    }

    @Test
    void executor校验大小写不敏感() {
        PipelineDefinitionDTO dto = new PipelineDefinitionDTO();
        dto.setStages(List.of(stage(1, "构建", List.of(task("Jenkins")), null)));
        assertTrue(PipelineDefinitionCodec.validateDto(dto, false).getValid());
    }

    @Test
    void 前四类阶段缺jenkins任务报错() {
        PipelineDefinitionDTO dto = new PipelineDefinitionDTO();
        dto.setStages(List.of(stage(1, "构建", List.of(task("deployer")), null)));
        ValidateResultVO result = PipelineDefinitionCodec.validateDto(dto, false);
        assertFalse(result.getValid());
        assertTrue(result.getErrors().get(0).getMessage().contains("executor=jenkins"));
    }

    @Test
    void 阶段类型重复与超时越界报错() {
        PipelineDefinitionDTO dto = new PipelineDefinitionDTO();
        dto.setStages(List.of(
                stage(1, "构建A", List.of(task("jenkins")), 30),
                stage(1, "构建B", List.of(task("jenkins")), null)));
        ValidateResultVO result = PipelineDefinitionCodec.validateDto(dto, false);
        assertFalse(result.getValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("重复")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("60–86400")));
    }

    @Test
    void 阶段数超过8个报错() {
        PipelineDefinitionDTO dto = new PipelineDefinitionDTO();
        List<PipelineStageDTO> stages = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            stages.add(stage(i <= 8 ? i : 8, "阶段" + i, List.of(task("jenkins")), null));
        }
        dto.setStages(stages);
        assertFalse(PipelineDefinitionCodec.validateDto(dto, false).getValid());
    }

    @Test
    void 对外校验拒绝空阶段_保存链路允许空阶段以支持显式清空() {
        PipelineDefinitionDTO empty = new PipelineDefinitionDTO();
        empty.setStages(new ArrayList<>());
        assertFalse(PipelineDefinitionCodec.validate("{\"stages\":[]}", 1).getValid());
        assertTrue(PipelineDefinitionCodec.validateDto(empty, false).getValid());
    }

    @Test
    void 非法JSON整体报错且行号为0() {
        ValidateResultVO result = PipelineDefinitionCodec.validate("{bad json", 1);
        assertFalse(result.getValid());
        assertEquals0(result);
    }

    private void assertEquals0(ValidateResultVO result) {
        assertTrue(result.getErrors().get(0).getLine() == 0);
        assertTrue(result.getErrors().get(0).getMessage().contains("JSON"));
    }

    @Test
    void decode空文本与非JSON抛业务异常() {
        assertThrows(ServiceException.class, () -> PipelineDefinitionCodec.decode("  "));
        assertThrows(ServiceException.class, () -> PipelineDefinitionCodec.decode("not json"));
    }
}
