package com.ferry.common.core.utils;

import com.ferry.common.core.exception.UtilException;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Jackson工具类
 *
 * <p>对外全是静态方法，Mapper 只能挂在静态字段上，因此由构造器在容器创建本类时写入，
 * 依赖在对象构造完成时即已就绪，不会出现调用静态方法时字段仍为空的窗口。
 *
 * <p>按字段取值的 getXxx 一族语义一致：Json 为空、字段缺失、或值类型与所取类型不符，一律返回
 * null，不做隐式转换也不猜默认值；需要区分「字段不存在」与「值为 null」时用 getNode 拿原始节点。
 *
 * <p>catch 里先记日志再包成 {@link UtilException} 上抛：请求线程虽有更外层的全局异常处理兜底，
 * 但定时任务、启动期与异步线程走不到那里，不记就只剩一条被上层吞掉的堆栈。日志只写操作类型与
 * 目标类名，不写 Json 内容和字段值——载荷里可能带口令、令牌这类不允许落日志的信息。
 */
@Slf4j
public class JsonUtil {

    private static ObjectMapper objectMapper;

    /**
     * 构造器注入 Mapper，仅在首次赋值，避免多容器场景相互覆盖
     *
     * @param objectMapper Jackson Mapper
     */
    public JsonUtil(ObjectMapper objectMapper) {
        if (JsonUtil.objectMapper == null) {
            JsonUtil.objectMapper = objectMapper;
        }
    }

    /**
     * 对象转为Json字符串
     *
     * @param obj 对象
     * @param <T> 泛型
     * @return Json字符串
     */
    public static <T> String toJson(T obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Json serialize failed, type={}", obj.getClass().getName(), e);
            throw new UtilException(e);
        }
    }

    /**
     * Json字符串转为对象
     *
     * @param json  Json字符串
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 目标对象，空字符串返回 null
     */
    public static <T> T toBean(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Json deserialize failed, type={}", clazz.getName(), e);
            throw new UtilException(e);
        }
    }

    /**
     * Json字符串转为树模型
     *
     * <p>只需要取其中个别字段时用它，避免为一段载荷专门补一个承载类。
     *
     * @param json Json字符串
     * @return 根节点，空字符串返回 null
     */
    public static JsonNode readTree(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            // 只记长度：内容可能含敏感信息，长度够用来判断是空串、截断还是脏数据
            log.error("Json parse failed, length={}", json.length(), e);
            throw new UtilException(e);
        }
    }

    /**
     * 读取 Json 字符串里指定字段的节点
     *
     * <p>数组与嵌套对象的入口：拿到的节点可继续 path 取值，也可交给 toBean 转成对象。
     *
     * @param json  Json字符串
     * @param field 字段名
     * @return 字段节点；整段 Json 为空返回 null，字段不存在返回 MissingNode（链式取值不会空指针）
     */
    public static JsonNode getNode(String json, String field) {
        JsonNode root = readTree(json);
        return root == null ? null : root.path(field);
    }

    /**
     * 读取 Json 字符串里指定字段的文本值
     *
     * @param json  Json字符串
     * @param field 字段名
     * @return 字段值，取不到返回 null
     */
    public static String getStr(String json, String field) {
        JsonNode node = getNode(json, field);
        return node != null && node.isString() ? node.stringValue() : null;
    }

    /**
     * 读取 Json 字符串里指定字段的整数值
     *
     * @param json  Json字符串
     * @param field 字段名
     * @return 字段值，取不到返回 null
     */
    public static Integer getInt(String json, String field) {
        JsonNode node = getNode(json, field);
        return node != null && node.isNumber() ? node.intValue() : null;
    }

    /**
     * 读取 Json 字符串里指定字段的中长整数值
     *
     * <p>雪花 id 在 Json 里既可能是数字也可能是字符串，这里只认数字，跨类型转换留给调用方决定。
     *
     * @param json  Json字符串
     * @param field 字段名
     * @return 字段值，取不到返回 null
     */
    public static Long getLong(String json, String field) {
        JsonNode node = getNode(json, field);
        return node != null && node.isNumber() ? node.longValue() : null;
    }

    /**
     * 读取 Json 字符串里指定字段的布尔值
     *
     * @param json  Json字符串
     * @param field 字段名
     * @return 字段值，取不到返回 null
     */
    public static Boolean getBool(String json, String field) {
        JsonNode node = getNode(json, field);
        return node != null && node.isBoolean() ? node.booleanValue() : null;
    }

    /**
     * 对象转为Json字节数组
     *
     * @param obj 对象
     * @return Json字节数组
     */
    public static byte[] toJsonBytes(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (Exception e) {
            log.error("Json serialize to bytes failed, type={}", obj.getClass().getName(), e);
            throw new UtilException(e);
        }
    }
}
