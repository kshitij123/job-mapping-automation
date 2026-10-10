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
        write(file, "dump", List.of("naukriCompanyId", "companyName", "emailWebsite"), List.of(
                List.of("750299", "Tata Consultancy Services", "hr@tcs-india.com"),
                List.of("750300", "TCS", "recruiter@tcs.test"),
                List.of("750301", "tcs jobs", "jobs@tcs.test"),
                List.of("68012", "Infosys", "talent@infosys.com"),
                List.of("68044", "infosys ltd", "campus@gmail.com"),
                List.of("55547", "Google Ireland Limited", "sriram@google.com"),
                List.of("71009", "Google LLC", "jobs@google.test"),
                List.of("71008", "amazon", "hiring@amazon.test"),
                List.of("70211", "Wipro", "careers@wipro.test"),
                List.of("70245", "Accenture India", "in.careers@accenture.test"),
                List.of("73321", "Microsoft", "recruiter@microsoft.test"),
                List.of("73390", "IBM India", "ibm.jobs@ibm.test"),
                List.of("74001", "Reliance", "hr@ril.test"),
                List.of("75110", "hdfc", "careers@hdfcbank.test"),
                List.of("76002", "Deloitte", "in.jobs@deloitte.test"),
                List.of("78020", "Cognizant Technology Solutions", "india@cognizant.test"),
                List.of("79015", "HCL", "careers@hcltech.test"),
                List.of("79016", "Capgemini", "jobs@capgemini.test"),
                List.of("79017", "Oracle India", "in.jobs@oracle.test"),
                List.of("647527", "yashika testing one", "yashikatesting1@yopmail.com"),
                List.of("166349", "demo11", "demo1@demo.com"),
                List.of("158249", "jklmnl", "fgh@yahoo.com"),
                List.of("152018", "bguspvtld", "tanzanite@gmail.com"),
                List.of("138118", "all bpo company", "shaikhmohammedzuberahmed@gmail.com"),
                List.of("69101", "9876543210", "phone.search@example.com"),
                List.of("69102", "john.doe@gmail.com", "john.doe@gmail.com")
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
