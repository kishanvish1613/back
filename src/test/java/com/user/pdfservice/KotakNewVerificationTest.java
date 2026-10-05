package com.user.pdfservice;

import com.user.pdfservice.model.*;
import com.user.pdfservice.template.kotaknew.KotakNewTemplate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class KotakNewVerificationTest {

    private Statement createDummyKotakStatement(int txCount) {
        Statement stmt = new Statement();
        stmt.meta = new StatementMeta();
        stmt.meta.password = "1234";
        stmt.meta.generatedAt = "2024-01-31T12:00:00Z";
        stmt.meta.statementPeriodStart = "2024-01-01T00:00:00Z";
        stmt.meta.statementPeriodEnd = "2024-01-31T00:00:00Z";

        stmt.details = new StatementUserDetails();
        stmt.details.name = "RAJESH SHARMA";
        stmt.details.fullName = "RAJESH SHARMA";
        stmt.details.accountNumber = "9876543210";
        stmt.details.ifsc = "KKBK0000123";
        stmt.details.micr = "400485002";
        stmt.details.branch = "NARIMAN POINT";
        stmt.details.branchAddress = "GROUND FLOOR, BAKHTAWAR, NARIMAN POINT, MUMBAI 400021";
        stmt.details.branchPhoneNo = "022-66006022";
        stmt.details.address = "FLAT NO 402, PALM GROVE, ANDHERI WEST, MUMBAI 400058";
        stmt.details.email = "rajesh.sharma@example.com";
        stmt.details.phoneNumber = "9820123456";
        stmt.details.accountType = "811 Regular";
        stmt.details.crn = "123456789";
        stmt.details.nomineeName = "Registered";

        stmt.transactions = new ArrayList<>();
        for (int i = 1; i <= txCount; i++) {
            Transaction tx = new Transaction();
            tx.date = "2024-01-" + String.format("%02d", (i % 28) + 1) + "T10:30:00Z";
            tx.description = "UPI/402319876543/PAYMENT " + i + "/HDFC/user@hdfcbank";
            tx.reference = "TXN" + (100000 + i);
            tx.debit = i % 2 == 0 ? 250.00 : 0.00;
            tx.credit = i % 2 != 0 ? 500.00 : 0.00;
            tx.balance = 10000.00 + (i * 100.00);
            stmt.transactions.add(tx);
        }

        return stmt;
    }

    @Test
    void testKotakNewTemplateMultiPageGeneration() throws Exception {
        KotakNewTemplate template = new KotakNewTemplate();
        // 25 transactions will span Page 1, Page 2, Page 3 (summary), Page 4 (important info)
        Statement stmt = createDummyKotakStatement(25);

        byte[] pdfBytes = template.generate(stmt);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        try (PDDocument doc = PDDocument.load(pdfBytes, "1234")) {
            System.out.println("Generated PDF Page Count: " + doc.getNumberOfPages());
            assertTrue(doc.getNumberOfPages() >= 3, "Kotak statement should have at least 3-4 pages");

            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(doc);

            // Verify customer details
            assertTrue(fullText.contains("RAJESH SHARMA"), "Should contain customer name");
            assertTrue(fullText.contains("9876543210"), "Should contain account number");
            assertTrue(fullText.contains("811 Regular"), "Should contain account type");
            
            // Verify Important Information is present
            assertTrue(fullText.contains("Important Information"), "Should contain Important Information header");
            assertTrue(fullText.contains("TDS"), "Should contain TDS rules in Important Information");
            assertTrue(fullText.contains("End of Statement"), "Should contain End of Statement block");
            assertTrue(fullText.contains("Account Summary"), "Should contain Account Summary section");

            // Verify Page 1 text doesn't contain the branch phone number in the top details block
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            String page1Text = stripper.getText(doc);
            assertFalse(page1Text.contains("Branch Phone"), "Page 1 right column should not contain Branch Phone");

            // Verify customer details on Page 2 and Page 3 headers
            if (doc.getNumberOfPages() >= 2) {
                stripper.setStartPage(2);
                stripper.setEndPage(2);
                String page2Text = stripper.getText(doc);
                assertTrue(page2Text.contains("RAJESH SHARMA"), "Page 2 header should contain Customer Name");
                assertTrue(page2Text.contains("9876543210"), "Page 2 header should contain Account Number");
            }

            if (doc.getNumberOfPages() >= 4) {
                org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
                java.awt.image.BufferedImage bim = renderer.renderImageWithDPI(3, 150); // page index 3 (page 4)
                javax.imageio.ImageIO.write(bim, "PNG", new java.io.File("target/page4_preview.png"));
                System.out.println("Page 4 rendered successfully to target/page4_preview.png");
            }
        }
    }
}

