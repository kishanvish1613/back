package com.user.pdfservice;

import com.user.pdfservice.ocr.OcrProcessingService;
import com.user.pdfservice.ocr.OcrResult;
import com.user.pdfservice.service.PdfOcrService;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OcrHardDrillTest {

    private final PdfOcrService pdfOcrService = new PdfOcrService();

    @Test
    public void hardDrillAllBankPdfs() throws Exception {
        List<String> files = List.of(
                "test/axis.pdf",
                "test/axisog.pdf",
                "test/boi.pdf",
                "test/hdfc.pdf",
                "test/kotak.pdf",
                "test/kotak2.pdf",
                "test/kotaknew.pdf",
                "test/sbi.pdf",
                "test/sbinew.pdf"
        );

        List<String> failures = new ArrayList<>();

        for (String file : files) {
            System.out.println("\n=======================================================");
            System.out.println(">>> TESTING OCR DRILL: " + file);
            System.out.println("=======================================================");

            ClassPathResource res = new ClassPathResource(file);
            byte[] bytes;
            try (InputStream is = res.getInputStream()) {
                bytes = is.readAllBytes();
            }

            String text;
            try {
                text = pdfOcrService.extractTextFromPdf(bytes, null);
            } catch (InvalidPasswordException e) {
                System.out.println("[SKIPPED PASSWORD ENCRYPTED] " + file);
                continue;
            }

            assertNotNull(text, "Extracted text cannot be null for " + file);
            assertFalse(text.isBlank(), "Extracted text cannot be blank for " + file);

            OcrResult r = OcrProcessingService.process(text);

            System.out.println("Bank:            " + r.bankName);
            System.out.println("Customer Name:   " + r.name);
            System.out.println("Account Number:  " + r.accountNumber);
            System.out.println("IFSC:            " + r.ifsc);
            System.out.println("MICR:            " + r.micr);
            System.out.println("Branch:          " + r.branch);
            System.out.println("Branch Address:  " + r.branchAddress);
            System.out.println("Customer Addr:   " + (r.address != null ? r.address.replace("\n", " | ") : null));
            System.out.println("Opening Balance: " + r.openingBalance);
            System.out.println("Closing Balance: " + r.closingBalance);
            System.out.println("Period Start:    " + r.statementPeriodStart);
            System.out.println("Period End:      " + r.statementPeriodEnd);
            System.out.println("City / State:    " + r.city + " / " + r.state);
            System.out.println("Pincode:         " + r.pincode);

            // Audit critical fields
            List<String> missing = new ArrayList<>();
            if (isEmpty(r.bankName)) missing.add("bankName");
            if (isEmpty(r.name)) missing.add("name");
            if (isEmpty(r.accountNumber)) missing.add("accountNumber");
            if (isEmpty(r.ifsc)) missing.add("ifsc");
            if (isEmpty(r.branch)) missing.add("branch");
            if (isEmpty(r.address)) missing.add("address");
            if (isEmpty(r.openingBalance)) missing.add("openingBalance");
            if (isEmpty(r.closingBalance)) missing.add("closingBalance");
            if (isEmpty(r.statementPeriodStart)) missing.add("statementPeriodStart");
            if (isEmpty(r.statementPeriodEnd)) missing.add("statementPeriodEnd");

            if (!missing.isEmpty()) {
                String msg = file + " is missing fields: " + String.join(", ", missing);
                System.err.println(">>> [MISSING FIELDS] " + msg);
                failures.add(msg);
            } else {
                System.out.println(">>> [SUCCESS: ALL CRITICAL FIELDS PRESENT]");
            }
        }

        if (!failures.isEmpty()) {
            System.err.println("\n========== HARD DRILL AUDIT FAILURES ==========");
            for (String f : failures) {
                System.err.println("FAIL: " + f);
            }
            fail("Some PDFs failed complete OCR field extraction:\n" + String.join("\n", failures));
        }
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
