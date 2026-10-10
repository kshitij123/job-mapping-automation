package com.nokri.taxonomy.excel;

import com.nokri.taxonomy.domain.CanonicalCompany;
import com.nokri.taxonomy.domain.DumpKeyword;
import com.nokri.taxonomy.domain.MappingResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ExcelIO {
    private static final DataFormatter FORMATTER = new DataFormatter();

    public List<CanonicalCompany> readCanonical(Path file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.toFile())) {
            Sheet sheet = workbook.getSheetAt(0);
            List<CanonicalCompany> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String id = cell(row, 0);
                String name = cell(row, 1);
                if (!id.isBlank() && !name.isBlank()) {
                    rows.add(new CanonicalCompany(id, name));
                }
            }
            return rows;
        }
    }

    public List<DumpKeyword> readDump(Path file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.toFile())) {
            Sheet sheet = workbook.getSheetAt(0);
            List<DumpKeyword> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String id = cell(row, 0);
                String name = cell(row, 1);
                String email = cell(row, 2);
                if (!id.isBlank() && !name.isBlank()) {
                    rows.add(new DumpKeyword(id, name, email));
                }
            }
            return rows;
        }
    }

    public void writeMapping(Path file, List<MappingResult> results) throws IOException {
        Files.createDirectories(file.getParent());
        try (Workbook workbook = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(file)) {
            Sheet sheet = workbook.createSheet("mapping");
            Row header = sheet.createRow(0);
            String[] columns = {
                    "dump_company_id", "dump_company_name", "canonical_id", "canonical_name",
                    "match_status", "match_score", "resolved_company_name", "used_llm", "used_google", "reason"
            };
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }
            int rowNum = 1;
            for (MappingResult result : results) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(result.dumpCompanyId());
                row.createCell(1).setCellValue(result.dumpCompanyName());
                row.createCell(2).setCellValue(nullToEmpty(result.canonicalId()));
                row.createCell(3).setCellValue(nullToEmpty(result.canonicalName()));
                row.createCell(4).setCellValue(result.matchStatus());
                row.createCell(5).setCellValue(result.matchScore());
                row.createCell(6).setCellValue(nullToEmpty(result.resolvedCompanyName()));
                row.createCell(7).setCellValue(result.usedLlm());
                row.createCell(8).setCellValue(result.usedGoogle());
                row.createCell(9).setCellValue(nullToEmpty(result.reason()));
            }
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
        }
    }

    private static String cell(Row row, int index) {
        Cell cell = row.getCell(index);
        return cell == null ? "" : FORMATTER.formatCellValue(cell).trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
