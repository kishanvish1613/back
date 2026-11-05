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
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.PDEncryption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated verification test for State Bank of India (new layout) PDF export metadata and encryption.
 * Verifies strict SBI (new layout) values and ensures other bank templates (control group) remain unaffected.
 */
public class SbiNewMetadataCorrectionTest {

    private static final String EXPECTED_SBI_CREATOR =
            "JasperReports Library version 6.21.2-8434a0bd7c3bbc37cbf916f2968d35e4b165821a";
    private static final String EXPECTED_SBI_PRODUCER =
            "OpenPDF 1.3.32";
    private static final float EXPECTED_PDF_VERSION = 1.5f;
    private static final float EXPECTED_PAGE_WIDTH = 595.0f;
    private static final float EXPECTED_PAGE_HEIGHT = 842.0f;
    private static final int EXPECTED_ENC_VERSION = 1;     // Algorithm 1
    private static final int EXPECTED_ENC_REVISION = 2;    // Revision 2
    private static final int EXPECTED_KEY_LENGTH = 40;     // 40 bits RC4

    private Statement createSampleSbiStatement() {
        Statement stmt = new Statement();
        stmt.meta = new StatementMeta();
        stmt.meta.password = "53968160267";
        stmt.meta.generatedAt = "2026-10-31T12:00:00Z";
        stmt.meta.statementPeriodStart = "2026-07-01T00:00:00Z";
        stmt.meta.statementPeriodEnd = "2026-07-31T00:00:00Z";

        stmt.details = new StatementUserDetails();
        stmt.details.name = "KISHAN VISHWAKARMA";
        stmt.details.fullName = "KISHAN VISHWAKARMA";
        stmt.details.accountNumber = "53968160267";
        stmt.details.ifsc = "SBIN0016450";
        stmt.details.micr = "462002093";
        stmt.details.branch = "ROHIT NAGAR";
        stmt.details.branchAddress = "466 ROHIT NAGAR NO 1, BHOPAL";
        stmt.details.branchPhoneNo = "8989791948";
        stmt.details.address = "123 SEMRA KALA, BHOPAL - 462010";
        stmt.details.email = "kishan@example.com";
        stmt.details.phoneNumber = "9876543210";
        stmt.details.accountType = "Savings Account";
        stmt.details.customerRelNo = "80459914494";
        stmt.details.ckycr = "Not Available";
        stmt.details.nomineeName = "Registered";

        stmt.transactions = new ArrayList<>();
        Transaction tx1 = new Transaction();
        tx1.date = "2026-07-01T10:00:00Z";
        tx1.description = "DEP TFR\nNEFT*SBIN0000240*SBIN12345678*POTENT INFOTECH\n0099500162095 AT 16450";
        tx1.reference = "-";
        tx1.debit = 0.00;
        tx1.credit = 30000.00;
        tx1.balance = 30816.23;
        stmt.transactions.add(tx1);

        Transaction tx2 = new Transaction();
        tx2.date = "2026-07-02T11:30:00Z";
        tx2.description = "WDL TFR\nUPI/DR/378592987641/Ramakant Sharma/CBIN/paytmqrSKB/Paym\n0097695162091 AT 16450\nROHIT NAGAR";
        tx2.reference = "-";
        tx2.debit = 120.00;
        tx2.credit = 0.00;
        tx2.balance = 30696.23;
        stmt.transactions.add(tx2);

        Transaction tx3 = new Transaction();
        tx3.date = "2026-07-03T14:15:00Z";
        tx3.description = "UPI/Cashback/Reward/\n539680223655/Cashback";
        tx3.reference = "-";
        tx3.debit = 0.00;
        tx3.credit = 5.00;
        tx3.balance = 30701.23;
        stmt.transactions.add(tx3);

        return stmt;
    }

    private Statement createSampleBoiStatement() {
        Statement stmt = new Statement();
        stmt.meta = new StatementMeta();
        stmt.meta.statementPeriodStart = "2026-01-01";
        stmt.meta.statementPeriodEnd = "2026-01-31";
        stmt.meta.generatedAt = "2026-01-31T10:00:00Z";
        stmt.meta.password = "";

        stmt.details = new StatementUserDetails();
        stmt.details.fullName = "M/S SHIVAY ORTHO CARE CLINIC";
        stmt.details.startingBalance = 15000.00;
        stmt.details.address = "SHOP NO 6, SKY TOWER, BHOPAL-462001";
        stmt.details.customerRelNo = "CC9396053";
        stmt.details.accountNumber = "907020110000340";
        stmt.details.accountType = "CURRENT- GENERAL";
        stmt.details.ifsc = "BKID0009070";
        stmt.details.micr = "462013018";
        stmt.details.branch = "RATIBAD";

        stmt.transactions = new ArrayList<>();
        Transaction tx = new Transaction();
        tx.date = "2026-01-05";
        tx.credit = 2500.00;
        tx.debit = 0.00;
        tx.balance = 17500.00;
        tx.description = "UPI/641835173208/CR/NITIN/SBIN/930";
        tx.reference = "45315";
        stmt.transactions.add(tx);

        return stmt;
    }

    private Statement createSampleKotakStatement() {
        Statement stmt = new Statement();
        stmt.meta = new StatementMeta();
        stmt.meta.password = "400485002";
        stmt.meta.generatedAt = "2026-01-31T12:00:00Z";
        stmt.meta.statementPeriodStart = "2026-01-01T00:00:00Z";
        stmt.meta.statementPeriodEnd = "2026-01-31T00:00:00Z";

        stmt.details = new StatementUserDetails();
        stmt.details.name = "RAJESH SHARMA";
        stmt.details.fullName = "RAJESH SHARMA";
        stmt.details.accountNumber = "9876543210";
        stmt.details.ifsc = "KKBK0000123";
        stmt.details.micr = "400485002";
        stmt.details.branch = "NARIMAN POINT";
        stmt.details.branchAddress = "GROUND FLOOR, NARIMAN POINT, MUMBAI 400021";
        stmt.details.branchPhoneNo = "022-66006022";
        stmt.details.address = "ANDHERI WEST, MUMBAI 400058";
        stmt.details.accountType = "811 Regular";
        stmt.details.crn = "123456789";

        stmt.transactions = new ArrayList<>();
        Transaction tx = new Transaction();
        tx.date = "2026-01-05T10:30:00Z";
        tx.description = "UPI/402319876543/PAYMENT/HDFC";
        tx.reference = "TXN100001";
        tx.debit = 250.00;
        tx.credit = 0.00;
        tx.balance = 9750.00;
        stmt.transactions.add(tx);

        return stmt;
    }

    @Test
    @DisplayName("Verify strict metadata, PDF properties, and encryption for SBI (new layout)")
    public void testSbiNewLayoutStrictMetadataAndEncryption() throws Exception {
        System.out.println("==========================================================================");
        System.out.println("TEST: State Bank of India (new layout) Strict Metadata & Encryption");
        System.out.println("==========================================================================");

        SbiNewTemplate sbiTemplate = new SbiNewTemplate();
        Statement stmt = createSampleSbiStatement();
        String userPwd = stmt.meta.password;

        byte[] sbiPdfBytes = sbiTemplate.generate(stmt);
        assertNotNull(sbiPdfBytes, "Generated SBI (new layout) PDF byte array must not be null");
        assertTrue(sbiPdfBytes.length > 0, "Generated SBI (new layout) PDF byte array must not be empty");

        try (PDDocument doc = PDDocument.load(sbiPdfBytes, userPwd)) {
            PDDocumentInformation info = doc.getDocumentInformation();
            PDEncryption enc = doc.getEncryption();
            PDPage page0 = doc.getPage(0);

            // 1. Strict Metadata Extraction
            String actualCreator = info.getCreator();
            String actualProducer = info.getProducer();
            float actualVersion = doc.getVersion();
            float actualWidth = page0.getMediaBox().getWidth();
            float actualHeight = page0.getMediaBox().getHeight();
            boolean actualLinearized = new String(sbiPdfBytes, 0, Math.min(1024, sbiPdfBytes.length), java.nio.charset.StandardCharsets.ISO_8859_1).contains("/Linearized");
            boolean actualEncrypted = doc.isEncrypted();

            System.out.println("[SBI NEW] Extracted Metadata:");
            System.out.println("  Creator:       " + actualCreator);
            System.out.println("  Producer:      " + actualProducer);
            System.out.println("  PDF Version:   " + actualVersion);
            System.out.println("  Paper Size:    " + actualWidth + " x " + actualHeight + " pt");
            System.out.println("  Linearized:    " + (actualLinearized ? "yes" : "no"));
            System.out.println("  Encrypted:     " + (actualEncrypted ? "yes" : "no"));

            // 1.1 Strict Metadata Assertions
            assertEquals(EXPECTED_SBI_CREATOR, actualCreator,
                    String.format("[FAIL] Creator mismatch. Expected: '%s' but was: '%s'",
                            EXPECTED_SBI_CREATOR, actualCreator));
            System.out.println("  [PASS] Creator strictly matches: " + EXPECTED_SBI_CREATOR);

            assertEquals(EXPECTED_SBI_PRODUCER, actualProducer,
                    String.format("[FAIL] Producer mismatch. Expected: '%s' but was: '%s'",
                            EXPECTED_SBI_PRODUCER, actualProducer));
            System.out.println("  [PASS] Producer strictly matches: " + EXPECTED_SBI_PRODUCER);

            // 1.2 Strict PDF Properties Assertions
            assertEquals(EXPECTED_PDF_VERSION, actualVersion, 0.01f,
                    String.format("[FAIL] PDF Version mismatch. Expected: %.1f but was: %.1f",
                            EXPECTED_PDF_VERSION, actualVersion));
            System.out.println("  [PASS] PDF Version strictly matches: 1.5");

            assertEquals(EXPECTED_PAGE_WIDTH, actualWidth, 0.01f,
                    String.format("[FAIL] Paper width mismatch. Expected: %.2f pt but was: %.2f pt",
                            EXPECTED_PAGE_WIDTH, actualWidth));
            assertEquals(EXPECTED_PAGE_HEIGHT, actualHeight, 0.01f,
                    String.format("[FAIL] Paper height mismatch. Expected: %.2f pt but was: %.2f pt",
                            EXPECTED_PAGE_HEIGHT, actualHeight));
            System.out.println("  [PASS] Paper size strictly matches 595 x 842 pt (A4)");

            assertFalse(actualLinearized, "[FAIL] Linearized must be 'no' (false)");
            System.out.println("  [PASS] Linearized is 'no'");

            assertTrue(actualEncrypted, "[FAIL] Encrypted must be 'yes' (true)");
            System.out.println("  [PASS] Encrypted is 'yes'");

            // 1.3 Strict Encryption Properties Assertions
            assertNotNull(enc, "[FAIL] Encryption dictionary must be present");
            int actualEncVersion = enc.getVersion();
            int actualEncRevision = enc.getRevision();
            int actualKeyLength = enc.getLength();
            boolean actualEncryptMetadata = enc.isEncryptMetaData();

            System.out.println("[SBI NEW] Extracted Encryption Details:");
            System.out.println("  Algorithm (V): " + actualEncVersion);
            System.out.println("  Revision (R):  " + actualEncRevision);
            System.out.println("  Cipher:        RC4");
            System.out.println("  Key Length:    " + actualKeyLength + " bits");
            System.out.println("  Enc Metadata:  " + actualEncryptMetadata);

            assertEquals(EXPECTED_ENC_VERSION, actualEncVersion,
                    String.format("[FAIL] Encryption algorithm mismatch. Expected: %d (V=1) but was: %d",
                            EXPECTED_ENC_VERSION, actualEncVersion));
            System.out.println("  [PASS] Encryption algorithm strictly matches: 1 (V=1)");

            assertEquals(EXPECTED_ENC_REVISION, actualEncRevision,
                    String.format("[FAIL] Encryption revision mismatch. Expected: %d (R=2) but was: %d",
                            EXPECTED_ENC_REVISION, actualEncRevision));
            System.out.println("  [PASS] Encryption revision strictly matches: 2 (R=2)");

            assertEquals(EXPECTED_KEY_LENGTH, actualKeyLength,
                    String.format("[FAIL] Encryption key length mismatch. Expected: %d bits but was: %d bits",
                            EXPECTED_KEY_LENGTH, actualKeyLength));
            System.out.println("  [PASS] Cipher strictly matches: RC4 40-bit");

            assertTrue(actualEncryptMetadata, "[FAIL] Encrypt metadata must be true");
            System.out.println("  [PASS] Encrypt metadata is true");

            // 1.4 Password & Owner/User Permissions
            AccessPermission ap = doc.getCurrentAccessPermission();
            assertNotNull(ap, "[FAIL] AccessPermission must not be null");
            assertTrue(ap.isOwnerPermission(),
                    "[FAIL] Supplied password must authenticate as owner (<owner, user>)");
            System.out.println("  [PASS] Supplied password successfully authenticates with owner + user privileges");

            // 1.5 Non-strict fields verification (present and valid)
            assertNotNull(info.getCreationDate(), "[FAIL] Creation date should be present");
            assertTrue(doc.getNumberOfPages() >= 1, "[FAIL] Page count must be >= 1");
            assertTrue(sbiPdfBytes.length > 1000, "[FAIL] File length should be non-trivial");

            COSDictionary cosEnc = enc.getCOSObject();
            assertNotNull(cosEnc.getItem(COSName.O), "[FAIL] Encryption /O key must be present");
            assertNotNull(cosEnc.getItem(COSName.U), "[FAIL] Encryption /U key must be present");
            System.out.println("  [PASS] Non-strict fields verified (Page count=" + doc.getNumberOfPages()
                    + ", Size=" + sbiPdfBytes.length + " bytes, CreationDate="
                    + info.getCreationDate().getTime() + ")");
        }
    }

    @Test
    @DisplayName("Negative Test: Verify control bank templates retain their own metadata and are NOT affected")
    public void testControlBankTemplatesUnaffected() throws Exception {
        System.out.println("==========================================================================");
        System.out.println("TEST: Control Bank Templates (Negative Test for Isolation)");
        System.out.println("==========================================================================");

        // Control Bank 1: Bank of India (BOI)
        BoiTemplate boiTemplate = new BoiTemplate();
        Statement boiStmt = createSampleBoiStatement();
        byte[] boiPdfBytes = boiTemplate.generate(boiStmt);
        assertNotNull(boiPdfBytes, "Generated BOI PDF must not be null");

        try (PDDocument boiDoc = PDDocument.load(boiPdfBytes)) {
            PDDocumentInformation boiInfo = boiDoc.getDocumentInformation();
            String boiCreator = boiInfo.getCreator();
            String boiProducer = boiInfo.getProducer();

            System.out.println("[CONTROL 1: BOI] Extracted Metadata:");
            System.out.println("  Creator:  " + boiCreator);
            System.out.println("  Producer: " + boiProducer);

            assertNotEquals(EXPECTED_SBI_CREATOR, boiCreator,
                    "[FAIL] BOI Creator was accidentally modified to match SBI strict creator!");
            assertNotEquals(EXPECTED_SBI_PRODUCER, boiProducer,
                    "[FAIL] BOI Producer was accidentally modified to match SBI strict producer!");

            // Confirm original BOI values are preserved
            assertEquals("JasperReports (Account_statement)", boiCreator);
            assertEquals("iText1.3.1 by lowagie.com (based on itext-paulo-154)", boiProducer);
            System.out.println("  [PASS] BOI template retains its original metadata (unaffected by SBI changes)");
        }

        // Control Bank 2: Kotak Bank New Layout
        KotakNewTemplate kotakTemplate = new KotakNewTemplate();
        Statement kotakStmt = createSampleKotakStatement();
        byte[] kotakPdfBytes = kotakTemplate.generate(kotakStmt);
        assertNotNull(kotakPdfBytes, "Generated Kotak PDF must not be null");

        try (PDDocument kotakDoc = PDDocument.load(kotakPdfBytes, kotakStmt.meta.password)) {
            PDDocumentInformation kotakInfo = kotakDoc.getDocumentInformation();
            String kotakCreator = kotakInfo.getCreator();
            String kotakProducer = kotakInfo.getProducer();

            System.out.println("[CONTROL 2: KOTAK NEW] Extracted Metadata:");
            System.out.println("  Creator:  " + kotakCreator);
            System.out.println("  Producer: " + kotakProducer);

            assertNotEquals(EXPECTED_SBI_CREATOR, kotakCreator,
                    "[FAIL] Kotak Creator was accidentally modified to match SBI strict creator!");
            assertNotEquals(EXPECTED_SBI_PRODUCER, kotakProducer,
                    "[FAIL] Kotak Producer was accidentally modified to match SBI strict producer!");

            // Confirm original Kotak New values are preserved
            assertEquals("OpenPDF 2.0.3", kotakProducer);
            System.out.println("  [PASS] Kotak New template retains its original metadata (unaffected by SBI changes)");
        }
    }
}
