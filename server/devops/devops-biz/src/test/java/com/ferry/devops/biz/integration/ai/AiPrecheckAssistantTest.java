package com.ferry.devops.biz.integration.ai;

import com.ferry.common.ai.utils.AiUtil;
import com.ferry.devops.api.vo.PrecheckItemVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * AI 预检助手测试：模型结论解析与降级行为。
 */
class AiPrecheckAssistantTest {

    private MockedStatic<AiUtil> aiUtilMock;
    private AiPrecheckAssistant assistant;

    @BeforeEach
    void setUp() {
        aiUtilMock = Mockito.mockStatic(AiUtil.class);
        assistant = new AiPrecheckAssistant();
    }

    @AfterEach
    void tearDown() {
        aiUtilMock.close();
    }

    @Test
    void 模型返回PASS_建议放行() {
        aiUtilMock.when(() -> AiUtil.chat(anyString())).thenReturn("PASS\n风险可控，建议放行");

        AiPrecheckAssistant.RiskVerdict verdict = assistant.assess(
                "订单应用", "主干流水线", "master", "abc1234", passedItems());

        assertTrue(verdict.passed());
        assertTrue(verdict.reason().contains("风险可控"));
    }

    @Test
    void 模型返回FAIL_否决() {
        aiUtilMock.when(() -> AiUtil.chat(anyString())).thenReturn("FAIL\n同类提交近期失败率偏高，建议人工确认");

        AiPrecheckAssistant.RiskVerdict verdict = assistant.assess(
                "订单应用", "主干流水线", "master", "abc1234", passedItems());

        assertFalse(verdict.passed());
        assertTrue(verdict.reason().contains("失败率偏高"));
    }

    @Test
    void 模型降级_不拦截触发() {
        aiUtilMock.when(() -> AiUtil.chat(anyString())).thenReturn(null);

        AiPrecheckAssistant.RiskVerdict verdict = assistant.assess(
                "订单应用", "主干流水线", "master", "abc1234", passedItems());

        assertTrue(verdict.passed());
        assertTrue(verdict.reason().contains("不可用"));
    }

    @Test
    void 模型输出空串_不拦截触发() {
        aiUtilMock.when(() -> AiUtil.chat(anyString())).thenReturn("   ");

        AiPrecheckAssistant.RiskVerdict verdict = assistant.assess(
                "订单应用", "主干流水线", "master", "abc1234", passedItems());

        assertTrue(verdict.passed());
        assertEquals("AI 风险分析不可用，按确定性校验结果放行", verdict.reason());
    }

    @Test
    void 超长结论_截断到200字() {
        aiUtilMock.when(() -> AiUtil.chat(anyString())).thenReturn("PASS\n" + "风".repeat(500));

        AiPrecheckAssistant.RiskVerdict verdict = assistant.assess(
                "订单应用", "主干流水线", "master", "abc1234", passedItems());

        assertEquals(200, verdict.reason().length());
    }

    private List<PrecheckItemVO> passedItems() {
        PrecheckItemVO item = new PrecheckItemVO();
        item.setType(1);
        item.setConclusion(1);
        item.setDetail("仓库信息已登记");
        return List.of(item);
    }
}
