package com.user.pdfservice;

import com.user.pdfservice.ocr.BankDetector;
import com.user.pdfservice.ocr.OcrProcessingService;
import com.user.pdfservice.ocr.OcrResult;
import com.user.pdfservice.service.PdfOcrService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

public class SbiDebugUploadTest {

    @Test
    public void debugSbiUpload() throws Exception {
        File file = new File("C:/Users/real/Downloads/RAVIN18081966.pdf");
        if (!file.exists()) {
            System.out.println("File not found!");
            return;
        }

        byte[] bytes = Files.readAllBytes(file.toPath());
        
        try (PDDocument doc = PDDocument.load(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(2, doc.getNumberOfPages()));
            String p12 = stripper.getText(doc);
            System.out.println("====== PAGES 1 & 2 RAW TEXT ======");
            System.out.println(p12);
            System.out.println("==================================");
        }

        PdfOcrService pdfOcrService = new PdfOcrService();
        String extractedText = pdfOcrService.extractTextFromPdf(bytes, null);
        System.out.println("====== EXTRACTED TEXT FROM PDF OCR SERVICE ======");
        System.out.println(extractedText);
        System.out.println("================================================");

        OcrResult result = OcrProcessingService.process(extractedText);

        System.out.println("====== OCR RESULT ======");
        System.out.println("Bank Name: " + result.bankName);
        System.out.println("Account Number: " + result.accountNumber);
        System.out.println("IFSC: " + result.ifsc);
        System.out.println("Name: " + result.name);
        System.out.println("Address: " + result.address);
        System.out.println("Opening Balance: " + result.openingBalance);
        System.out.println("Closing Balance: " + result.closingBalance);
        System.out.println("Statement Start: " + result.statementPeriodStart);
        System.out.println("Statement End: " + result.statementPeriodEnd);
        System.out.println("Branch: " + result.branch);
        System.out.println("MICR: " + result.micr);
        System.out.println("========================");
    }
}
