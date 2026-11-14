package com.ferry.common.core.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.ferry.common.core.exception.ServiceException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Excel 读写工具类
 *
 * <p>基于 EasyExcel，列与实体字段的对应关系由 {@code @ExcelProperty} 注解决定。
 * 导出把一批行按注解写成 xlsx 并塞进下载响应流；导入读取首个工作表，
 * 从第 2 行（表头之后）开始逐行还原成实体，非空校验等业务规则由调用方逐行做。
 *
 * <p>失败路径：导出写流失败抛 {@link IOException}，由调用方决定如何收场——响应流一旦开始写出
 * 就改不了状态码，只有调用方知道自己是否已经写出；导入失败统一抛 {@link ServiceException}，
 * 单元格转不进目标类型时报 Excel 行号，文件本身无法解析时抛通用解析失败提示并记日志。
 */
@Slf4j
public final class PoiUtil {

    private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private PoiUtil() {
    }

    /**
     * 导出 xlsx 到下载响应流，工作表名与下载文件名相同
     *
     * @param fileName 下载文件名，不含扩展名
     * @param rows     数据行
     * @param head     表头类型，列由该类上的注解决定，必须是可公开实例化的类型
     * @param response HTTP 响应
     * @throws IOException 写流失败
     */
    public static <T> void exports(String fileName, List<T> rows,
                                   Class<T> head, HttpServletResponse response) throws IOException {
        exports(fileName, fileName, rows, head, response);
    }

    /**
     * 导出 xlsx 到下载响应流
     *
     * <p>响应头在这里一次设全：Controller 一旦开始写流就改不了状态码。
     * 文件名按 RFC 5987 编码并统一补一个 .xlsx 后缀（传入已带后缀也不会重复），否则中文名在
     * Firefox 与部分网关下会乱码或被截断。
     *
     * @param fileName  下载文件名，不含扩展名
     * @param sheetName 工作表名
     * @param rows      数据行
     * @param head      表头类型，列由该类上的注解决定，必须是可公开实例化的类型
     * @param response  HTTP 响应
     * @throws IOException 写流失败
     */
    public static <T> void exports(String fileName, String sheetName, List<T> rows,
                                   Class<T> head, HttpServletResponse response) throws IOException {
        // 已带 .xlsx 后缀的只保留一个，避免拼成 xlsx.xlsx 双后缀
        String plain = fileName.endsWith(".xlsx") ? fileName.substring(0, fileName.length() - 5) : fileName;
        String encoded = URLEncoder.encode(plain, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(CONTENT_TYPE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encoded + ".xlsx");
        // 导出内容随筛选变化，网关与浏览器都不该缓存
        response.setHeader("Cache-Control", "no-store");
        // autoCloseStream 关掉是因为响应流归 servlet 容器管，EasyExcel 不该替它关
        EasyExcel.write(response.getOutputStream(), head).autoCloseStream(Boolean.FALSE).sheet(sheetName).doWrite(rows);
    }

    /**
     * 导入 Excel 首个工作表为实体列表
     *
     * @param is   Excel 文件流，调用方负责关闭来源
     * @param head 行类型，列由该类上的注解决定，必须是可公开实例化的类型
     * @param <T>  行类型
     * @return 全部数据行，文件没有数据行时为空列表
     */
    public static <T> List<T> imports(InputStream is, Class<T> head) {
        try {
            // 默认按表头名匹配列，第 1 行是表头，从第 2 行开始读
            return EasyExcel.read(is).head(head).sheet().doReadSync();
        } catch (ExcelDataConvertException e) {
            throw new ServiceException("第 " + (e.getRowIndex() + 1) + " 行数据格式不正确，请修改后重新导入");
        } catch (Exception e) {
            log.error("Excel 导入解析失败", e);
            throw new ServiceException("文件解析失败，请确认上传的是有效的 Excel 文件");
        }
    }
}
