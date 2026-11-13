package com.ferry.devops.biz.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Excel 导出
 *
 * <p>只做「把一批行按注解写成 xlsx 并塞进响应流」，筛选与取数留在 Service，
 * 导出内容才有机会与列表走同一段查询条件。
 */
public final class ExcelExporter {

    private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private ExcelExporter() {
    }

    /**
     * 输出 xlsx 到响应流
     *
     * <p>响应头必须在这里一次设全：Controller 一旦开始写流就改不了状态码。
     * 文件名按 RFC 5987 编码，否则中文名在 Firefox 与部分网关下会乱码或被截断。
     *
     * @param response   HTTP 响应
     * @param fileName   下载文件名，不含扩展名
     * @param sheetName  工作表名
     * @param head       表头类型，列由该类上的注解决定
     * @param rows       数据行
     * @param watermarks 水印行，写在表头之上
     * @throws IOException 写流失败
     */
    public static <T> void write(HttpServletResponse response, String fileName, String sheetName,
                                 Class<T> head, List<T> rows, List<String> watermarks) throws IOException {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(CONTENT_TYPE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encoded + ".xlsx");
        // 导出内容随筛选变化，网关与浏览器都不该缓存
        response.setHeader("Cache-Control", "no-store");

        WatermarkSheetWriteHandler watermark = new WatermarkSheetWriteHandler(watermarks);
        ExcelWriter writer = EasyExcel.write(response.getOutputStream(), head)
                .registerWriteHandler(style())
                .registerWriteHandler(watermark)
                .build();
        try {
            // 表头偏移必须与占用的水印行数一致，否则水印会被表头覆盖
            WriteSheet sheet = EasyExcel.writerSheet(sheetName)
                    .relativeHeadRowIndex(watermark.rowCount())
                    .build();
            writer.write(rows, sheet);
        } finally {
            // finish 负责写出 xlsx 的中央目录，少了它文件打不开，异常路径也要执行
            writer.finish();
        }
    }

    /** 表头加粗居中、内容带细边框：导出的表要交给业务方看，裸格式在 Excel 里很难读 */
    private static HorizontalCellStyleStrategy style() {
        WriteCellStyle headStyle = new WriteCellStyle();
        headStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        headStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        WriteFont headFont = new WriteFont();
        headFont.setBold(true);
        headFont.setFontHeightInPoints((short) 11);
        headStyle.setWriteFont(headFont);

        WriteCellStyle bodyStyle = new WriteCellStyle();
        bodyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        bodyStyle.setBorderBottom(BorderStyle.THIN);
        bodyStyle.setBorderLeft(BorderStyle.THIN);
        bodyStyle.setBorderRight(BorderStyle.THIN);
        bodyStyle.setBorderTop(BorderStyle.THIN);
        return new HorizontalCellStyleStrategy(headStyle, bodyStyle);
    }
}
