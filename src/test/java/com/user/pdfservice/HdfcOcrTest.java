package com.user.pdfservice;

import com.user.pdfservice.ocr.OcrProcessingService;
import com.user.pdfservice.ocr.OcrResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

public class HdfcOcrTest {

    @Test
    void testHdfcOcr() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/test/hdfc.pdf")) {
            if (is == null) {
                System.out.println("hdfc.pdf not found in classpath");
                return;
            }
            try (PDDocument doc = PDDocument.load(is)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(1);
                stripper.setEndPage(Math.min(3, doc.getNumberOfPages()));
                String text = stripper.getText(doc);
                System.out.println("=== EXTRACTED HDFC PDF TEXT (START) ===");
                System.out.println(text);
                System.out.println("=== EXTRACTED HDFC PDF TEXT (END) ===");

                OcrResult result = OcrProcessingService.process(text);
                System.out.println("--- OCR PARSED RESULT ---");
                System.out.println("Bank: " + result.bankName);
                System.out.println("Name: " + result.name);
                System.out.println("Account Number: " + result.accountNumber);
                System.out.println("Cust ID / CRN: " + result.customerRelNo);
                System.out.println("Account Type: " + result.accountType);
                System.out.println("Branch: " + result.branch);
                System.out.println("Branch Code: " + result.branchCode);
                System.out.println("Branch Address: " + result.branchAddress);
                System.out.println("City: " + result.city);
                System.out.println("State: " + result.state);
                System.out.println("IFSC: " + result.ifsc);
                System.out.println("MICR: " + result.micr);
                System.out.println("Customer Address: " + result.address);
                System.out.println("Phone: " + result.phoneNumber);
                System.out.println("Branch Phone: " + result.branchPhoneNo);
                System.out.println("Email: " + result.email);
                System.out.println("Nominee: " + result.nomineeName);
                System.out.println("Period Start: " + result.statementPeriodStart);
                System.out.println("Period End: " + result.statementPeriodEnd);
                System.out.println("Opening Balance: " + result.openingBalance);
            }
        }
    }

    @Test
    void testUploadedHdfcPdf() throws Exception {
        java.io.File file = new java.io.File("C:/Users/real/Downloads/179023868395862I2WSjxfITY.pdf");
        if (!file.exists()) {
            System.out.println("User uploaded HDFC file not found");
            return;
        }
        try (PDDocument doc = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(2, doc.getNumberOfPages()));
            String text = stripper.getText(doc);

            System.out.println("=== UNSORTED TEXT FROM UPLOADED PDF (START) ===");
            System.out.println(text.length() > 1500 ? text.substring(0, 1500) : text);
            System.out.println("=== UNSORTED TEXT FROM UPLOADED PDF (END) ===");

            OcrResult result = OcrProcessingService.process(text);
            org.junit.jupiter.api.Assertions.assertEquals("HDFCCURRENT", result.bankName);
            org.junit.jupiter.api.Assertions.assertEquals("M/S. TALHA ENTERPRISES", result.name);
            org.junit.jupiter.api.Assertions.assertEquals("50200115012821", result.accountNumber);
            org.junit.jupiter.api.Assertions.assertEquals("338318140", result.customerRelNo);
            org.junit.jupiter.api.Assertions.assertEquals("MAHWA", result.branch);
            org.junit.jupiter.api.Assertions.assertEquals("HDFC0009689", result.ifsc);
            org.junit.jupiter.api.Assertions.assertEquals("321240004", result.micr);
            org.junit.jupiter.api.Assertions.assertTrue(result.address.contains("BHARATPUR ROAD"));
        }
    }
}

