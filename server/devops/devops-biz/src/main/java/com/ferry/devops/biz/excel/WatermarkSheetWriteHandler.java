package com.ferry.devops.biz.excel;

import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.handler.context.SheetWriteHandlerContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.List;

/**
 * 导出水印
 *
 * <p>水印与数据同表写在表头之上：导出文件离开系统后无法追溯来源，把导出人、导出时间与筛选条件
 * 直接印进文件，拿到表格的人自己就能看出这是哪一次导出、按什么条件取的数。
 * 不用页眉，是因为页眉只在打印预览里可见。
 *
 * <p>行数必须与 {@code relativeHeadRowIndex} 一致，否则水印会与表头抢行；行数由 {@link #rowCount()} 给出，
 * 由调用方设置偏移，保证两处永远对得上。
 */
public class WatermarkSheetWriteHandler implements SheetWriteHandler {

    private final List<String> lines;

    /**
     * @param lines 水印文本，每行一条
     */
    public WatermarkSheetWriteHandler(List<String> lines) {
        this.lines = List.copyOf(lines);
    }

    /** 水印占用的行数，调用方据此设置表头起始行 */
    public int rowCount() {
        return lines.size();
    }

    @Override
    public void afterSheetCreate(SheetWriteHandlerContext context) {
        if (lines.isEmpty()) {
            return;
        }
        Sheet sheet = context.getWriteSheetHolder().getSheet();
        Workbook workbook = context.getWriteWorkbookHolder().getWorkbook();

        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 9);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);

        // 此时 sheet 还是空的，表头由 EasyExcel 在 relativeHeadRowIndex 处开始写，这里只占前几行
        for (int i = 0; i < lines.size(); i++) {
            Row row = sheet.createRow(i);
            Cell cell = row.createCell(0);
            cell.setCellValue(lines.get(i));
            cell.setCellStyle(style);
        }
    }
}
