package com.ferry.common.core.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 数值转换测试
 *
 * <p>两条边界必须同时成立：空值返回 null（超管不挂部门这类场景依赖它），
 * 非法格式必须抛异常（脏数据不能被当成空值吞掉）。
 */
class NumUtilTest {

    @Test
    void blankShouldConvertToNull() {
        assertNull(NumUtil.toLong(null));
        assertNull(NumUtil.toLong(""));
        assertNull(NumUtil.toLong("   "));
        assertNull(NumUtil.toInt(null));
        assertNull(NumUtil.toInt(""));
    }

    @Test
    void numericStringShouldConvert() {
        assertEquals(123L, NumUtil.toLong("123"));
        assertEquals(123L, NumUtil.toLong(123));
        assertEquals(123L, NumUtil.toLong(" 123 "));
        assertEquals(123, NumUtil.toInt("123"));
        assertEquals(123, NumUtil.toInt(123));
    }

    @Test
    void illegalFormatShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> NumUtil.toLong("abc"));
        assertThrows(IllegalArgumentException.class, () -> NumUtil.toInt("abc"));
        assertThrows(IllegalArgumentException.class, () -> NumUtil.toInt("1.5"));
        // 超出 int 范围不能被截断成别的值
        assertThrows(IllegalArgumentException.class, () -> NumUtil.toInt("9999999999"));
    }

    @Test
    void defaultShouldApplyOnBlankOrIllegal() {
        assertEquals(0L, NumUtil.toLong(null, 0L));
        assertEquals(0L, NumUtil.toLong("", 0L));
        assertEquals(0L, NumUtil.toLong("abc", 0L));
        assertEquals(123L, NumUtil.toLong("123", 0L));

        assertEquals(0, NumUtil.toInt(null, 0));
        assertEquals(0, NumUtil.toInt("abc", 0));
        assertEquals(123, NumUtil.toInt("123", 0));
    }
}
