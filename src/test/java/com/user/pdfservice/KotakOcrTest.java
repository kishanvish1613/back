package com.user.pdfservice;

import com.user.pdfservice.ocr.OcrProcessingService;
import com.user.pdfservice.ocr.OcrResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

public class KotakOcrTest {

    @Test
    void testKotakOcr() throws Exception {
        String[] testFiles = {"/test/kotaknew.pdf", "/test/kotak.pdf", "/test/kotak2.pdf"};
        for (String fileName : testFiles) {
            System.out.println("\n=======================================================");
            System.out.println("TESTING FILE: " + fileName);
            System.out.println("=======================================================");
            try (InputStream is = getClass().getResourceAsStream(fileName)) {
                if (is == null) {
                    System.out.println(fileName + " not found in classpath");
                    continue;
                }
                try (PDDocument doc = PDDocument.load(is)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setSortByPosition(true);
                    stripper.setStartPage(1);
                    stripper.setEndPage(Math.min(3, doc.getNumberOfPages()));
                    String text = stripper.getText(doc);
                    System.out.println("=== EXTRACTED TEXT (First 1500 chars) ===");
                    System.out.println(text.length() > 1500 ? text.substring(0, 1500) : text);

                    OcrResult result = OcrProcessingService.process(text);
                    System.out.println("\n--- OCR PARSED RESULT ---");
                    System.out.println("Bank: " + result.bankName);
                    System.out.println("Name: " + result.name);
                    System.out.println("Account Number: " + result.accountNumber);
                    System.out.println("CRN: " + result.crn);
                    System.out.println("Cust ID / CRN: " + result.customerRelNo);
                    System.out.println("Account Type: " + result.accountType);
                    System.out.println("Branch: " + result.branch);
                    System.out.println("Branch Address: " + result.branchAddress);
                    System.out.println("IFSC: " + result.ifsc);
                    System.out.println("MICR: " + result.micr);
                    System.out.println("Customer Address: " + result.address);
                    System.out.println("Pincode: " + result.pincode);
                    System.out.println("Phone: " + result.phoneNumber);
                    System.out.println("Email: " + result.email);
                    System.out.println("Period Start: " + result.statementPeriodStart);
                    System.out.println("Period End: " + result.statementPeriodEnd);
                } catch (Exception e) {
                    System.err.println("Error processing " + fileName + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}
