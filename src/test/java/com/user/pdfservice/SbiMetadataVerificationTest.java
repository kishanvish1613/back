package com.user.pdfservice;



import com.user.pdfservice.model.*;

import com.user.pdfservice.template.sbinew.SbiNewTemplate;

import com.user.pdfservice.template.sbi.SbiTemplate;

import com.user.pdfservice.template.sbi2.SbiTemplate2;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import org.apache.pdfbox.pdmodel.encryption.AccessPermission;

import org.junit.jupiter.api.Test;



import java.util.ArrayList;



import static org.junit.jupiter.api.Assertions.*;



public class SbiMetadataVerificationTest {



    private Statement createDummyStatement() {

        Statement stmt = new Statement();

        stmt.meta = new StatementMeta();

        stmt.meta.password = "1234";

        stmt.meta.generatedAt = "2024-01-31T12:00:00Z";

        stmt.meta.statementPeriodStart = "2024-01-01T00:00:00Z";

        stmt.meta.statementPeriodEnd = "2024-01-31T00:00:00Z";



        stmt.details = new StatementUserDetails();

        stmt.details.name = "TEST USER";

        stmt.details.fullName = "TEST USER";

        stmt.details.accountNumber = "12345678901";

        stmt.details.ifsc = "SBIN0016450";

        stmt.details.micr = "462002093";

        stmt.details.branch = "ROHIT NAGAR";

        stmt.details.branchAddress = "466 ROHIT NAGAR NO 1, BHOPAL";

        stmt.details.branchPhoneNo = "8989791948";

        stmt.details.address = "123 TEST ROAD, INDORE";

        stmt.details.email = "test@example.com";

        stmt.details.phoneNumber = "9876543210";

        stmt.details.accountType = "Savings Account";

        stmt.details.customerRelNo = "80459914494";

        stmt.details.ckycr = "Not Available";

        stmt.details.nomineeName = "XXXXXXXXXXXXXXX";



        stmt.transactions = new ArrayList<>();

        Transaction tx = new Transaction();

        tx.date = "2024-01-01T00:00:00Z";

        tx.description = "TRANSFER TO TEST";

        tx.reference = "REF123456";

        tx.debit = 100.00;

        tx.credit = 0.00;

        tx.balance = 5000.00;

        stmt.transactions.add(tx);



        return stmt;

    }



    @Test

    void testSbiNewTemplateMetadataAndPermissions() throws Exception {

        SbiNewTemplate template = new SbiNewTemplate();

        Statement stmt = createDummyStatement();



        byte[] pdfBytes = template.generate(stmt);
        java.nio.file.Files.write(java.nio.file.Path.of("test_sbinew_output.pdf"), pdfBytes);

        assertNotNull(pdfBytes);

        assertTrue(pdfBytes.length > 0);



        try (PDDocument doc = PDDocument.load(pdfBytes, "1234")) {

            PDDocumentInformation info = doc.getDocumentInformation();

            System.out.println("SbiNewTemplate Producer: " + info.getProducer());

            System.out.println("SbiNewTemplate Creator: " + info.getCreator());

            System.out.println("SbiNewTemplate Version: " + doc.getVersion());

            assertEquals("OpenPDF 1.3.32", info.getProducer());

            assertEquals("JasperReports Library version 6.21.2-8434a0bd7c3bbc37cbf916f2968d35e4b165821a", info.getCreator());

            assertEquals(1.5f, doc.getVersion(), 0.01f);



            org.apache.pdfbox.pdmodel.encryption.PDEncryption enc = doc.getEncryption();

            System.out.println("SbiNewTemplate Encryption Version: " + enc.getVersion());

            System.out.println("SbiNewTemplate Encryption Revision: " + enc.getRevision());

            System.out.println("SbiNewTemplate Encryption Length: " + enc.getLength());

            System.out.println("SbiNewTemplate EncryptMetadata: " + enc.isEncryptMetaData());

            org.apache.pdfbox.cos.COSDictionary cosEnc = enc.getCOSObject();

            for (org.apache.pdfbox.cos.COSName key : cosEnc.keySet()) {

                System.out.println("  EncDict " + key.getName() + " = " + cosEnc.getItem(key));

            }



            assertEquals(1, enc.getVersion()); // Algorithm 1

            assertEquals(2, enc.getRevision()); // Revision 2

            assertEquals(40, enc.getLength()); // Key length 40


            assertTrue(doc.getCurrentAccessPermission().isOwnerPermission(), "Supplied password should authenticate as owner (<owner, user>)");
            assertTrue(doc.isEncrypted(), "Must be encrypted");

            // Verify Catalog matches JasperReports / OpenPDF
            org.apache.pdfbox.cos.COSDictionary cat = doc.getDocumentCatalog().getCOSObject();
            assertNull(cat.getItem(org.apache.pdfbox.cos.COSName.VERSION), "Catalog Version must not be set");
            assertNull(cat.getItem(org.apache.pdfbox.cos.COSName.getPDFName("Trapped")), "Catalog Trapped must not be set");
            org.apache.pdfbox.cos.COSDictionary vp = (org.apache.pdfbox.cos.COSDictionary) cat.getDictionaryObject(org.apache.pdfbox.cos.COSName.VIEWER_PREFERENCES);
            assertNotNull(vp, "ViewerPreferences must exist");
            assertEquals("AppDefault", vp.getNameAsString(org.apache.pdfbox.cos.COSName.getPDFName("PrintScaling")), "PrintScaling must be AppDefault");



            for (PDPage p : doc.getPages()) {

                assertEquals(595f, p.getMediaBox().getWidth(), 0.5f);

                assertEquals(842f, p.getMediaBox().getHeight(), 0.5f);

            }



            AccessPermission ap = doc.getCurrentAccessPermission();

            assertTrue(ap.canPrint(), "Should allow Print");

            assertTrue(ap.canExtractContent(), "Should allow Copy / Extract");

            assertTrue(ap.canModifyAnnotations(), "Should allow Annotate");

            assertTrue(ap.isOwnerPermission(), "Should authenticate as owner (<owner, user>)");

        }

    }



    @Test

    void testSbiTemplateMetadataAndPermissions() throws Exception {

        SbiTemplate template = new SbiTemplate();

        Statement stmt = createDummyStatement();



        byte[] pdfBytes = template.generate(stmt);

        assertNotNull(pdfBytes);

        assertTrue(pdfBytes.length > 0);



        try (PDDocument doc = PDDocument.load(pdfBytes, "1234")) {

            PDDocumentInformation info = doc.getDocumentInformation();

            System.out.println("SbiTemplate Producer: " + info.getProducer());

            System.out.println("SbiTemplate Creator: " + info.getCreator());

            System.out.println("SbiTemplate Version: " + doc.getVersion());

            assertEquals("OpenPDF 1.3.32", info.getProducer());

            assertEquals("JasperReports Library version 6.21.2-8434a0bd7c3bbc37cbf916f2968d35e4b165821a", info.getCreator());

            assertEquals(1.5f, doc.getVersion(), 0.01f);



            org.apache.pdfbox.pdmodel.encryption.PDEncryption enc = doc.getEncryption();

            System.out.println("SbiNewTemplate Encryption Version: " + enc.getVersion());

            System.out.println("SbiNewTemplate Encryption Revision: " + enc.getRevision());

            System.out.println("SbiNewTemplate Encryption Length: " + enc.getLength());

            System.out.println("SbiNewTemplate EncryptMetadata: " + enc.isEncryptMetaData());

            org.apache.pdfbox.cos.COSDictionary cosEnc = enc.getCOSObject();

            for (org.apache.pdfbox.cos.COSName key : cosEnc.keySet()) {

                System.out.println("  EncDict " + key.getName() + " = " + cosEnc.getItem(key));

            }



            AccessPermission ap = doc.getCurrentAccessPermission();

            assertTrue(ap.canPrint(), "Should allow Print");

            assertTrue(ap.canExtractContent(), "Should allow Copy / Extract");

            assertTrue(ap.canModifyAnnotations(), "Should allow Annotate");

            assertTrue(ap.canFillInForm(), "Should allow Fill forms");

            assertTrue(ap.canAssembleDocument(), "Should allow Assemble");

            assertTrue(ap.canPrintDegraded(), "Should allow Print high-res");

            assertFalse(ap.canModify(), "Should not allow generic Modify / Change");

        }

    }



    @Test

    void testSbiTemplate2Metadata() throws Exception {

        SbiTemplate2 template = new SbiTemplate2();

        Statement stmt = createDummyStatement();



        byte[] pdfBytes = template.generate(stmt);

        assertNotNull(pdfBytes);

        assertTrue(pdfBytes.length > 0);



        try (PDDocument doc = PDDocument.load(pdfBytes, "1234")) {

            PDDocumentInformation info = doc.getDocumentInformation();

            System.out.println("SbiTemplate2 Producer: " + info.getProducer());

            System.out.println("SbiTemplate2 Creator: " + info.getCreator());

            System.out.println("SbiTemplate2 Version: " + doc.getVersion());

            assertEquals("OpenPDF 1.3.32", info.getProducer());

            assertEquals("JasperReports Library version 6.21.2-8434a0bd7c3bbc37cbf916f2968d35e4b165821a", info.getCreator());

            assertEquals(1.5f, doc.getVersion(), 0.01f);



            org.apache.pdfbox.pdmodel.encryption.PDEncryption enc = doc.getEncryption();

            System.out.println("SbiNewTemplate Encryption Version: " + enc.getVersion());

            System.out.println("SbiNewTemplate Encryption Revision: " + enc.getRevision());

            System.out.println("SbiNewTemplate Encryption Length: " + enc.getLength());

            System.out.println("SbiNewTemplate EncryptMetadata: " + enc.isEncryptMetaData());

            org.apache.pdfbox.cos.COSDictionary cosEnc = enc.getCOSObject();

            for (org.apache.pdfbox.cos.COSName key : cosEnc.keySet()) {

                System.out.println("  EncDict " + key.getName() + " = " + cosEnc.getItem(key));

            }

        }

    }

    @Test
    void testSbiNewNarrationWrappingWithProblematicStrings() throws Exception {
        SbiNewTemplate template = new SbiNewTemplate();
        Statement stmt = createDummyStatement();
        stmt.transactions.clear();

        Transaction tx1 = new Transaction();
        tx1.date = "2024-01-01T00:00:00Z";
        tx1.description = "UPI/Cashback/Reward/710023687989/A";
        tx1.reference = "UPI-710023687989";
        tx1.credit = 5.00;
        tx1.debit = 0.00;
        tx1.balance = 17416.29;
        stmt.transactions.add(tx1);

        Transaction tx2 = new Transaction();
        tx2.date = "2024-01-02T00:00:00Z";
        tx2.description = "WDL TFR\nUPI/DR/378592987641/Ramakant Sharma/CBIN/paytmqrSKB/Paym\n0097695162091 AT 16450\nROHIT NAGAR";
        tx2.reference = "-";
        tx2.debit = 120.00;
        tx2.credit = 0.00;
        tx2.balance = 17212.29;
        stmt.transactions.add(tx2);

        byte[] pdfBytes = template.generate(stmt);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        try (PDDocument doc = PDDocument.load(pdfBytes, "1234")) {
            assertEquals(1.5f, doc.getVersion(), 0.01f);
            assertNotNull(doc.getEncryption());
            assertEquals(2, doc.getEncryption().getRevision());
        }
    }

    @Test
    void testSbiNewAlwaysEncryptedEvenWithoutExplicitPassword() throws Exception {
        SbiNewTemplate template = new SbiNewTemplate();
        Statement stmt = createDummyStatement();
        stmt.meta.password = ""; // No explicit password provided
        stmt.details.accountNumber = "53968160267";
        stmt.details.customerRelNo = "80459914494";

        byte[] pdfBytes = template.generate(stmt);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        // Verify it is encrypted and can be opened with the account number as both owner and user (<owner, user>)
        try (PDDocument doc = PDDocument.load(pdfBytes, "53968160267")) {
            assertEquals(1.5f, doc.getVersion(), 0.01f);
            assertNotNull(doc.getEncryption());
            assertEquals(1, doc.getEncryption().getVersion()); // Algorithm 1
            assertEquals(2, doc.getEncryption().getRevision()); // Revision 2
            assertEquals(40, doc.getEncryption().getLength()); // Key length 40
            assertTrue(doc.getEncryption().isEncryptMetaData());
            assertTrue(doc.getCurrentAccessPermission().isOwnerPermission(), "Must authenticate as owner (<owner, user>)");
            assertTrue(doc.isEncrypted(), "Must be encrypted");
        }
    }
}
