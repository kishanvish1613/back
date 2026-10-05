package com.user.pdfservice;

import com.user.pdfservice.ocr.OcrProcessingService;
import com.user.pdfservice.ocr.OcrResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class BoiOcrExtractionTest {

    @Test
    public void testBoiOcrExtraction() throws Exception {
        File pdfFile = new File("C:/Users/real/.gemini/antigravity/brain/5e191c5a-8270-4075-8fa4-b9ceab0cc3cf/.user_uploaded/media_1790241713400.pdf");
        if (!pdfFile.exists()) {
            System.out.println("PDF file not found: " + pdfFile.getAbsolutePath());
            return;
        }

        String rawText;
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            rawText = stripper.getText(doc);
        }

        System.out.println("=== EXTRACTED OCR RAW TEXT ===");
        System.out.println(rawText);

        OcrResult result = OcrProcessingService.process(rawText);

        System.out.println("=== OCR RESULT ===");
        System.out.println("Bank: " + result.bankName);
        System.out.println("Name: " + result.name);
        System.out.println("Address: " + result.address);
        System.out.println("A/C NO: " + result.accountNumber);
        System.out.println("CUSTID: " + result.customerRelNo);
        System.out.println("IFSC: " + result.ifsc);
        System.out.println("MICR: " + result.micr);
        System.out.println("TYPE: " + result.accountType);
        System.out.println("NOMINEE: " + result.nomineeName);
        System.out.println("BRANCH: " + result.branch);
        System.out.println("OPENING BAL: " + result.openingBalance);
        System.out.println("PERIOD: " + result.statementPeriodStart + " to " + result.statementPeriodEnd);

        assertEquals("BOI", result.bankName);
        assertTrue(result.name.contains("SHIVAY ORTHO CARE"));
        assertTrue(result.address.contains("SKY TOWER"));
        assertEquals("907020110000340", result.accountNumber);
        assertEquals("CC9396053", result.customerRelNo);
        assertEquals("BKID0009070", result.ifsc);
        assertEquals("462013018", result.micr);
        assertTrue(result.accountType.contains("CURRENT"));
        assertEquals("RATIBAD", result.branch);
        assertEquals("501.50", result.openingBalance);
        assertEquals("01-02-2025", result.statementPeriodStart);
        assertEquals("31-01-2026", result.statementPeriodEnd);
    }
}

