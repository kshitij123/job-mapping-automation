package com.nokri.taxonomy.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SampleDataWriter {
    public void writeCanonical(Path file) throws IOException {
        write(file, "canonical", List.of("canonical_id", "canonical_name"), List.of(
                List.of("C1001", "TCS"),
                List.of("C1002", "Infosys Limited"),
                List.of("C1003", "Wipro Limited"),
                List.of("C1004", "Accenture"),
                List.of("C1005", "Amazon"),
                List.of("C1006", "Google"),
                List.of("C1007", "Microsoft"),
                List.of("C1008", "IBM"),
                List.of("C1009", "Reliance Industries"),
                List.of("C1010", "HDFC Bank"),
                List.of("C1011", "Deloitte"),
                List.of("C1012", "Cognizant"),
                List.of("C1013", "Capgemini"),
                List.of("C1014", "HCL Technologies"),
                List.of("C1015", "Oracle")
        ));
    }

    public void writeDump(Path file) throws IOException {
        write(file, "dump", List.of("company_id", "company_name"), List.of(
                List.of("67435", "Tata Consultancy Services"),
                List.of("67436", "TCS"),
                List.of("67481", "tcs jobs"),
                List.of("68012", "Infosys"),
                List.of("68044", "infosys ltd"),
                List.of("69101", "9876543210"),
                List.of("69102", "john.doe@gmail.com"),
                List.of("70211", "Wipro"),
                List.of("70245", "Accenture India"),
                List.of("71008", "amazon"),
                List.of("71009", "Google LLC"),
                List.of("72100", "+91 9988776655"),
                List.of("73321", "Microsoft"),
                List.of("73390", "IBM India"),
                List.of("74001", "Reliance"),
                List.of("75110", "hdfc"),
                List.of("76002", "Deloitte"),
                List.of("77111", "xyzabc123 not a firm"),
                List.of("78020", "Cognizant Technology Solutions"),
                List.of("79015", "HCL")
        ));
    }

    private void write(Path file, String sheetName, List<String> header, List<List<String>> rows) throws IOException {
        Files.createDirectories(file.getParent());
        try (Workbook workbook = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(file)) {
            Sheet sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < header.size(); i++) {
                headerRow.createCell(i).setCellValue(header.get(i));
            }
            int rowNum = 1;
            for (List<String> values : rows) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < values.size(); i++) {
                    row.createCell(i).setCellValue(values.get(i));
                }
            }
            for (int i = 0; i < header.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
        }
    }
}
