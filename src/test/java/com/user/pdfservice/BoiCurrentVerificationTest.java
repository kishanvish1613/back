package com.user.pdfservice;

import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.StatementMeta;
import com.user.pdfservice.model.StatementUserDetails;
import com.user.pdfservice.model.Transaction;
import com.user.pdfservice.template.boi.BoiTemplate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class BoiCurrentVerificationTest {

    @Test
    public void testGenerateBoiCurrentPdf() throws Exception {
        Statement stmt = new Statement();
        stmt.meta = new StatementMeta();
        stmt.meta.statementPeriodStart = "2025-02-01";
        stmt.meta.statementPeriodEnd = "2026-01-31";
        stmt.meta.generatedAt = "2026-03-23T10:00:00Z";
        stmt.meta.password = "";

        stmt.details = new StatementUserDetails();
        stmt.details.fullName = "M/S SHIVAY ORTHO CARE CLINIC PHYSIOTHERAPY &";
        stmt.details.startingBalance = 501.50;
        stmt.details.address = "SHOP NO 6\nSKY TOWER ROYAL MARKET\nBHOPAL-462001\nMADHYA PRADESH\nINDIA";
        stmt.details.customerRelNo = "CC9396053";
        stmt.details.accountNumber = "907020110000340";
        stmt.details.accountType = "CURRENT- GENERAL";
        stmt.details.ifsc = "BKID0009070";
        stmt.details.micr = "462013018";
        stmt.details.nomineeName = "YES";
        stmt.details.branch = "RATIBAD";

        stmt.transactions = new ArrayList<>();
        double currentBal = stmt.details.startingBalance;

        // Generate 50 sample transactions (Page 1 = 17, Page 2 = 29, Page 3 = 4 + disclaimer)
        for (int i = 1; i <= 50; i++) {
            Transaction tx = new Transaction();
            tx.date = String.format("2025-02-%02d", (i % 28) + 1);
            if (i % 3 == 0) {
                tx.credit = 5000.00;
                tx.debit = 0.00;
                currentBal += tx.credit;
                tx.description = "UPI/641835173208/CR/NITIN /SBIN/930";
            } else {
                tx.credit = 0.00;
                tx.debit = 1000.00;
                currentBal -= tx.debit;
                tx.description = "UPI/108079564686/DR/SHRI G/YESB/pay";
            }
            tx.balance = currentBal;
            tx.reference = (i % 5 == 0) ? "45315" : "";
            stmt.transactions.add(tx);
        }

        BoiTemplate template = new BoiTemplate();
        byte[] pdfBytes = template.generate(stmt);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        File outFile = new File("test_boi_current_generated.pdf");
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(pdfBytes);
        }
        System.out.println("Generated BOI Current PDF at: " + outFile.getAbsolutePath());

        // Validate text content and render PNGs
        try (PDDocument doc = PDDocument.load(outFile)) {
            assertEquals(3, doc.getNumberOfPages());

            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(doc);

            assertTrue(fullText.contains("Bank Of India"));
            assertTrue(fullText.contains("RATIBAD"));
            assertTrue(fullText.contains("M/S SHIVAY ORTHO CARE CLINIC PHYSIOTHERAPY &"));
            assertTrue(fullText.contains("907020110000340"));
            assertTrue(fullText.contains("BKID0009070"));
            assertTrue(fullText.contains("CURRENT- GENERAL"));
            assertTrue(fullText.contains("OPENING BALANCE:"));
            assertTrue(fullText.contains("This is a system generated statement"));
            assertTrue(fullText.contains("RELATIONSHIP BEYOND BANKING"));
            assertTrue(fullText.contains("Page:1"));
            assertTrue(fullText.contains("Page:2"));
            assertTrue(fullText.contains("Page:3"));

            PDFRenderer renderer = new PDFRenderer(doc);
            for (int p = 0; p < doc.getNumberOfPages(); p++) {
                BufferedImage img = renderer.renderImageWithDPI(p, 150);
                File imgFile = new File("target/boi_current_page_" + (p + 1) + ".png");
                imgFile.getParentFile().mkdirs();
                ImageIO.write(img, "PNG", imgFile);
                System.out.println("Saved rendered page " + (p + 1) + " to: " + imgFile.getAbsolutePath());
            }
        }
    }
}
