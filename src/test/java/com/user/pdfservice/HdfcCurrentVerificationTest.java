package com.user.pdfservice;

import com.user.pdfservice.model.*;
import com.user.pdfservice.template.BankPdfTemplate;
import com.user.pdfservice.template.TemplateRegistry;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class HdfcCurrentVerificationTest {

    private Statement createDummyHdfcCurrentStatement(int txCount) {
        Statement stmt = new Statement();
        stmt.meta = new StatementMeta();
        stmt.meta.password = "1234";
        stmt.meta.generatedAt = "18-Aug-2026 14:33";
        stmt.meta.statementPeriodStart = "01/01/2026";
        stmt.meta.statementPeriodEnd = "17/08/2026";
        stmt.meta.template = BankTemplate.HDFCCURRENT;

        stmt.details = new StatementUserDetails();
        stmt.details.name = "TALHA ENTERPRISES";
        stmt.details.fullName = "M/S. TALHA ENTERPRISES";
        stmt.details.accountNumber = "50200115012821";
        stmt.details.ifsc = "HDFC0009689";
        stmt.details.micr = "321240004";
        stmt.details.branch = "MAHWA";
        stmt.details.branchCode = "9689";
        stmt.details.branchAddress = "HDFC BANK LTD,\nJAIPUR ROAD ,MAHWA DIST DAUSA\nRAJASTHAN";
        stmt.details.city = "MAHWA 321608";
        stmt.details.state = "RAJASTHAN";
        stmt.details.branchPhoneNo = "18002600/18001600";
        stmt.details.address = "NA SHOP NO 03 BHARATPUR ROAD\nNEAR KHANDALWAL HOTEL NA MAHWA\nDAUSA RAJASTHAN 321608 KESARBAG\nMAHWA 321608\nRAJASTHAN INDIA";
        stmt.details.email = "AARISKHAN219545@GMAIL.COM";
        stmt.details.phoneNumber = "919601783479";
        stmt.details.customerRelNo = "338318140";
        stmt.details.accountType = "BIZ PRO PLUS ACCOUNT(1482)";
        stmt.details.nomineeName = "Registered";
        stmt.details.startingBalance = 0.00;

        stmt.transactions = new ArrayList<>();
        double runningBal = 0.00;
        for (int i = 1; i <= txCount; i++) {
            Transaction tx = new Transaction();
            tx.date = "2026-01-" + String.format("%02d", (i % 28) + 1);
            tx.reference = String.format("0000%012d", 844964422770L + i);
            if (i % 2 == 0) {
                tx.debit = 1000.00 * (i % 5 + 1);
                tx.credit = 0.00;
                runningBal -= tx.debit;
                tx.description = "UPI-AARIS\nKHAN-AARISKHAN219545@AXL-BARB0\nALWDEL-" + tx.reference + "-PAYMENT FROM PHONE";
            } else {
                tx.debit = 0.00;
                tx.credit = 5000.00 * (i % 3 + 1);
                runningBal += tx.credit;
                tx.description = "CHQ DEP - CTS CLG1 - MOHAN NAGAR\nHINDAUN\n: YAKUB KHAN :KOTAK MAHINDRA BANK";
            }
            tx.balance = Math.max(0, runningBal);
            stmt.transactions.add(tx);
        }

        return stmt;
    }

    @Test
    void testHdfcCurrentTemplateGeneration() throws Exception {
        BankPdfTemplate template = TemplateRegistry.get(BankTemplate.HDFCCURRENT);
        assertNotNull(template, "HDFCCURRENT should be registered in TemplateRegistry");

        Statement stmt = createDummyHdfcCurrentStatement(35);
        byte[] pdfBytes = template.generate(stmt);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        try (PDDocument doc = PDDocument.load(pdfBytes, "1234")) {
            int pageCount = doc.getNumberOfPages();
            System.out.println("Generated HDFC Current PDF Page Count: " + pageCount);
            assertTrue(pageCount >= 2, "Statement with 35 transactions should span at least 2 pages");

            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(doc);

            // Verify core entities
            assertTrue(fullText.contains("M/S. TALHA ENTERPRISES"), "Should contain customer entity name");
            assertTrue(fullText.contains("50200115012821"), "Should contain account number");
            assertTrue(fullText.contains("HDFC0009689"), "Should contain IFSC code");
            assertTrue(fullText.contains("321240004"), "Should contain MICR code");
            assertTrue(fullText.contains("BIZ PRO PLUS ACCOUNT(1482)"), "Should contain account type");
            assertTrue(fullText.contains("MAHWA"), "Should contain branch name");
            assertTrue(fullText.contains("STATEMENT SUMMARY"), "Should contain Statement Summary header");
            assertTrue(fullText.contains("HDFC BANK LIMITED"), "Should contain HDFC Bank footer");

            // Verify Page 1 has "Page No .: 1"
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            String p1 = stripper.getText(doc);
            assertTrue(p1.contains("Page No .: 1"), "Page 1 should have Page No .: 1");

            // Verify Page 2 has "Page No .: 2"
            stripper.setStartPage(2);
            stripper.setEndPage(2);
            String p2 = stripper.getText(doc);
            assertTrue(p2.contains("Page No .: 2"), "Page 2 should have Page No .: 2");

            // Render preview images for all generated pages
            PDFRenderer renderer = new PDFRenderer(doc);
            for (int p = 0; p < pageCount; p++) {
                BufferedImage bim = renderer.renderImageWithDPI(p, 150);
                File fp = new File("target/hdfccurrent_page" + (p + 1) + "_preview.png");
                ImageIO.write(bim, "PNG", fp);
                System.out.println("Saved preview: " + fp.getAbsolutePath());
            }
        }

        // Also test terminal overflow page with 0 transactions
        Statement emptyStmt = createDummyHdfcCurrentStatement(0);
        byte[] emptyPdf = template.generate(emptyStmt);
        try (PDDocument doc = PDDocument.load(emptyPdf, "1234")) {
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage bim = renderer.renderImageWithDPI(0, 150);
            File fp = new File("target/hdfccurrent_terminal_preview.png");
            ImageIO.write(bim, "PNG", fp);
            System.out.println("Saved terminal preview: " + fp.getAbsolutePath());
        }
    }
}
