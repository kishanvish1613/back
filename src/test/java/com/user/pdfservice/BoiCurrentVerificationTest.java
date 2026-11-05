package com.user.pdfservice;

import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.StatementMeta;
import com.user.pdfservice.model.StatementUserDetails;
import com.user.pdfservice.model.Transaction;
import com.user.pdfservice.template.boi.BoiTemplate;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Map;

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

        // Validate metadata, text content and render PNGs
        try (PDDocument doc = PDDocument.load(outFile)) {
            assertEquals(3, doc.getNumberOfPages());
            assertEquals(1.4f, doc.getVersion(), 0.01f);

            // Verify Document Information matches original BOI statement
            PDDocumentInformation info = doc.getDocumentInformation();
            assertNotNull(info);
            assertEquals("iText1.3.1 by lowagie.com (based on itext-paulo-154)", info.getProducer());
            assertEquals("JasperReports (Account_statement)", info.getCreator());
            assertNull(info.getTitle(), "Title should not be set (matching original JasperReports output)");
            assertNull(info.getAuthor(), "Author should not be set");
            assertNull(info.getSubject(), "Subject should not be set");
            assertNotNull(info.getCreationDate());
            assertNotNull(info.getModificationDate());

            // Verify JasperReports Named Page Anchors
            assertNotNull(doc.getDocumentCatalog().getNames());
            assertNotNull(doc.getDocumentCatalog().getNames().getDests());
            Map<String, ?> dests = doc.getDocumentCatalog().getNames().getDests().getNames();
            assertNotNull(dests);
            assertTrue(dests.containsKey("JR_PAGE_ANCHOR_0_1"));
            assertTrue(dests.containsKey("JR_PAGE_ANCHOR_0_2"));
            assertTrue(dests.containsKey("JR_PAGE_ANCHOR_0_3"));

            // Verify Page Resources & Dimensions
            for (int p = 0; p < doc.getNumberOfPages(); p++) {
                assertEquals(792.0f, doc.getPage(p).getMediaBox().getWidth(), 0.01f);
                assertEquals(842.0f, doc.getPage(p).getMediaBox().getHeight(), 0.01f);
                assertNotNull(doc.getPage(p).getResources().getFont(COSName.getPDFName("F1")));
                assertNotNull(doc.getPage(p).getResources().getFont(COSName.getPDFName("F2")));
            }
            // Page 1 has img1; Page 2 & 3 have no XObjects
            assertNotNull(doc.getPage(0).getResources().getXObject(COSName.getPDFName("img1")));
            assertFalse(doc.getPage(1).getResources().getXObjectNames().iterator().hasNext());
            assertFalse(doc.getPage(2).getResources().getXObjectNames().iterator().hasNext());

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

    @Test
    public void testGenerateBoiCurrentPdfEncrypted() throws Exception {
        Statement stmt = new Statement();
        stmt.meta = new StatementMeta();
        stmt.meta.statementPeriodStart = "2025-02-01";
        stmt.meta.statementPeriodEnd = "2026-01-31";
        stmt.meta.generatedAt = "2026-03-23T11:49:30Z";
        stmt.meta.password = "Shivay@123";

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
        Transaction tx = new Transaction();
        tx.date = "2025-02-01";
        tx.description = "OPENING BALANCE";
        tx.debit = 0.0;
        tx.credit = 10000.0;
        tx.balance = 10501.50;
        stmt.transactions.add(tx);

        BoiTemplate template = new BoiTemplate();
        byte[] pdfBytes = template.generate(stmt);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        try (PDDocument doc = PDDocument.load(pdfBytes, "Shivay@123")) {
            assertTrue(doc.isEncrypted());
            assertEquals(1.4f, doc.getVersion(), 0.01f);

            // Verify encryption matches original BOI RC4 128-bit /P -1836
            assertNotNull(doc.getEncryption());
            assertEquals("Standard", doc.getEncryption().getFilter());
            assertEquals(128, doc.getEncryption().getLength());
            assertEquals(2, doc.getEncryption().getVersion()); // Algorithm 2 (RC4 128-bit)
            assertEquals(3, doc.getEncryption().getRevision()); // Revision 3
            assertEquals(-1836, doc.getEncryption().getPermissions()); // Exact permissions integer

            org.apache.pdfbox.pdmodel.encryption.AccessPermission ap = doc.getCurrentAccessPermission();
            assertTrue(ap.canPrint(), "Should allow Print");
            assertTrue(ap.canExtractContent(), "Should allow Copy / Extract");
            assertTrue(ap.canPrintDegraded(), "Should allow High Quality Print");
            assertFalse(ap.canModify(), "Should NOT allow Modify");
            assertFalse(ap.canModifyAnnotations(), "Should NOT allow Modify Annotations");
            assertFalse(ap.canFillInForm(), "Should NOT allow Fill in Forms");
            assertFalse(ap.canAssembleDocument(), "Should NOT allow Assemble");

            PDDocumentInformation info = doc.getDocumentInformation();
            assertEquals("iText1.3.1 by lowagie.com (based on itext-paulo-154)", info.getProducer());
            assertEquals("JasperReports (Account_statement)", info.getCreator());
            assertNull(info.getTitle());
            assertNull(info.getAuthor());
            assertNull(info.getSubject());
        }
    }
}
