package com.user.pdfservice;

import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.StatementMeta;
import com.user.pdfservice.model.StatementUserDetails;
import com.user.pdfservice.model.Transaction;
import com.user.pdfservice.template.boi.BoiTemplate;
import com.user.pdfservice.template.kotaknew.KotakNewTemplate;
import com.user.pdfservice.template.sbinew.SbiNewTemplate;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class KotakNewMetadataVerificationTest {

    private Statement createSampleKotakStatement() {
        Statement stmt = new Statement();
        stmt.meta = new StatementMeta();
        stmt.meta.generatedAt = "2026-10-18T17:07:59Z";
        stmt.meta.statementPeriodStart = "2026-07-01T00:00:00Z";
        stmt.meta.statementPeriodEnd = "2026-09-30T00:00:00Z";

        stmt.details = new StatementUserDetails();
        stmt.details.name = "KISHAN VISHWAKARMA";
        stmt.details.accountNumber = "9947281077";
        stmt.details.customerRelNo = "xxxxxx478";
        stmt.details.ifsc = "KKBK0005891";
        stmt.details.micr = "462485007";
        stmt.details.branch = "Bhopal - Hamedia Road";
        stmt.details.branchPhoneNo = "9926587917";
        stmt.details.branchAddress = "Bhopal - Hamedia Road";
        stmt.details.accountType = "Savings";
        stmt.details.address = "118\nSemra Kala Purushottam Nagar B\nPurushottam Nagar Semra Kala B\nBhopal - 462010\nMadhya Pradesh - India";
        stmt.details.startingBalance = 816.23;

        stmt.transactions = new ArrayList<>();
        Transaction t1 = new Transaction();
        t1.date = "2026-07-01T00:00:00Z";
        t1.description = "NEFT KKBKH56268631539 POTENT INFOTECH";
        t1.reference = "NEFTINW-1901238374";
        t1.credit = 30000.0;
        t1.debit = 0.0;
        t1.balance = 30816.23;
        stmt.transactions.add(t1);

        return stmt;
    }

    @Test
    @DisplayName("Verify Kotak Mahindra Bank (new / 811) Strict Metadata & Properties")
    public void testKotakNewMetadataAndProperties() throws Exception {
        KotakNewTemplate template = new KotakNewTemplate();
        byte[] pdfBytes = template.generate(createSampleKotakStatement());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            // PDF Version = 1.5
            assertEquals(1.5f, doc.getVersion(), 0.001f);

            // Paper Size = 595 x 842 pt (A4)
            PDPage page1 = doc.getPage(0);
            PDRectangle mb = page1.getMediaBox();
            assertEquals(595.0f, mb.getWidth(), 0.1f);
            assertEquals(842.0f, mb.getHeight(), 0.1f);

            // Linearized = no
            String headerBytes = new String(pdfBytes, 0, Math.min(1024, pdfBytes.length));
            assertFalse(headerBytes.contains("/Linearized"));

            // Encrypted = no
            assertFalse(doc.isEncrypted());

            // Metadata: Producer = OpenPDF 2.0.3, Creator = null
            PDDocumentInformation info = doc.getDocumentInformation();
            assertNotNull(info);
            assertEquals("OpenPDF 2.0.3", info.getProducer());
            assertNull(info.getCreator());

            COSDictionary infoDict = info.getCOSObject();
            assertFalse(infoDict.containsKey(COSName.CREATOR));
            assertFalse(infoDict.containsKey(COSName.MOD_DATE));
            assertTrue(infoDict.containsKey(COSName.PRODUCER));
            assertTrue(infoDict.containsKey(COSName.CREATION_DATE));

            // Catalog dictionary: Type = Catalog, Pages, NO Version key
            COSDictionary catDict = doc.getDocumentCatalog().getCOSObject();
            assertEquals("Catalog", catDict.getNameAsString(COSName.TYPE));
            assertTrue(catDict.containsKey(COSName.PAGES));
            assertFalse(catDict.containsKey(COSName.VERSION));
            assertFalse(catDict.containsKey(COSName.getPDFName("Trapped")));
        }
    }

    @Test
    @DisplayName("Control Group Isolation: SBI New & BOI remain untouched")
    public void testControlGroupIsolation() throws Exception {
        // BOI should still produce its original metadata
        BoiTemplate boiTemplate = new BoiTemplate();
        Statement boiStmt = createSampleKotakStatement();
        byte[] boiBytes = boiTemplate.generate(boiStmt);
        try (PDDocument doc = PDDocument.load(boiBytes)) {
            assertEquals("iText1.3.1 by lowagie.com (based on itext-paulo-154)", doc.getDocumentInformation().getProducer());
            assertEquals("JasperReports (Account_statement)", doc.getDocumentInformation().getCreator());
        }

        // SBI New should still produce its strict OpenPDF 1.3.32 / JasperReports 6.21.2 metadata and 40-bit RC4 encryption
        SbiNewTemplate sbiTemplate = new SbiNewTemplate();
        Statement sbiStmt = createSampleKotakStatement();
        sbiStmt.meta.password = "1234";
        byte[] sbiBytes = sbiTemplate.generate(sbiStmt);
        try (PDDocument doc = PDDocument.load(sbiBytes, "1234")) {
            assertEquals("OpenPDF 1.3.32", doc.getDocumentInformation().getProducer());
            assertEquals("JasperReports Library version 6.21.2-8434a0bd7c3bbc37cbf916f2968d35e4b165821a", doc.getDocumentInformation().getCreator());
            assertTrue(doc.isEncrypted());
        }
    }
}
