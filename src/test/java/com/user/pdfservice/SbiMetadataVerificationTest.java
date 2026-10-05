package com.user.pdfservice;

import com.user.pdfservice.model.*;
import com.user.pdfservice.template.sbinew.SbiNewTemplate;
import com.user.pdfservice.template.sbi.SbiTemplate;
import com.user.pdfservice.template.sbi2.SbiTemplate2;
import org.apache.pdfbox.pdmodel.PDDocument;
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
        }
    }
}
