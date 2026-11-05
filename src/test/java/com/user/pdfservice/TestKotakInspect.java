package com.user.pdfservice;

import com.lowagie.text.Image;
import com.lowagie.text.pdf.*;
import java.awt.color.ICC_Profile;
import com.user.pdfservice.model.*;
import com.user.pdfservice.template.kotaknew.KotakNewTemplate;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestKotakInspect {

    @Test
    public void testImageTagIcc() throws Exception {
        PdfReader reader = new PdfReader("account-bank.pdf");
        PRStream s29 = (PRStream) reader.getPdfObject(29);
        PRStream s30 = (PRStream) reader.getPdfObject(30);
        byte[] iccBytes = PdfReader.getStreamBytes(s29);
        System.out.println("s29 decompressed len: " + iccBytes.length);
        ICC_Profile icc = ICC_Profile.getInstance(iccBytes);
        System.out.println("ICC profile loaded: " + icc.getNumComponents());
        Image img6 = Image.getInstance(PdfReader.getStreamBytesRaw(s30));
        img6.tagICC(icc);
        System.out.println("Tagged ICC successfully! Has ICC: " + img6.hasICCProfile());
        reader.close();
    }

    @Test
    public void inspectInfoDict() throws Exception {
        for (String fn : List.of("kotak_6pages_modeA.pdf", "kotak_6pages_modeB.pdf")) {
            File f = new File(fn);
            if (!f.exists()) continue;
            try (PDDocument doc = fn.contains("modeB") ? PDDocument.load(f, "user123") : PDDocument.load(f)) {
                System.out.println("=== INFO FOR " + fn + " ===");
                org.apache.pdfbox.pdmodel.PDDocumentInformation info = doc.getDocumentInformation();
                System.out.println("Producer: " + info.getProducer());
                System.out.println("CreationDate: " + info.getCreationDate());
                System.out.println("ModDate: " + info.getModificationDate());
                COSDictionary dict = info.getCOSObject();
                for (COSName k : dict.keySet()) {
                    System.out.println("  Info key: " + k.getName() + " -> " + dict.getItem(k));
                }
            }
        }
    }

    @Test
    public void testModeAAndModeB6Pages() throws Exception {
        Statement stmt = new Statement();
        stmt.meta = new StatementMeta();
        stmt.meta.template = BankTemplate.KOTAKNEW;
        stmt.meta.statementPeriodStart = "2026-01-01T00:00:00Z";
        stmt.meta.statementPeriodEnd = "2026-01-09T00:00:00Z";

        stmt.details = new StatementUserDetails();
        stmt.details.fullName = "KISHAN KUMAR";
        stmt.details.name = "KISHAN KUMAR";
        stmt.details.crn = "699303478";
        stmt.details.accountNumber = "53968160267";
        stmt.details.ifsc = "KKBK0000123";
        stmt.details.micr = "462485002";
        stmt.details.address = "HOUSE NO 123, BHOPAL\nMADHYA PRADESH - 462001";
        stmt.details.branchAddress = "Kotak Mahindra Bank Ltd, Ground Floor, Bhopal";
        stmt.details.startingBalance = 10000.0;

        stmt.transactions = new ArrayList<>();
        double bal = 10000.0;
        // Generate transactions across exactly 4 txn pages (pages 1 to 4)
        // With Page 5 = Summary and Page 6 = Notices -> total 6 pages
        for (int i = 0; i < 180; i++) {
            Transaction t = new Transaction();
            t.date = "02/01/2026";
            t.valueDate = "02/01/2026";
            t.description = "UPI/Payment/Merchant" + i + "/Ref" + (10000000 + i);
            t.reference = "UPI-" + (10000000 + i);
            if (i % 2 == 0) {
                t.debit = 100.0;
                t.credit = 0.0;
                bal -= 100.0;
            } else {
                t.debit = 0.0;
                t.credit = 250.0;
                bal += 250.0;
            }
            t.balance = bal;
            stmt.transactions.add(t);
        }

        KotakNewTemplate template = new KotakNewTemplate();

        // ----------------------------------------------------
        // VARIANT A: Without Password (Unencrypted)
        // ----------------------------------------------------
        stmt.meta.password = null;
        stmt.details.password = null;
        byte[] pdfModeA = template.generate(stmt);
        File outA = new File("kotak_6pages_modeA.pdf");
        Files.write(outA.toPath(), pdfModeA);

        try (PDDocument docA = PDDocument.load(pdfModeA)) {
            System.out.println("=== VARIANT A (UNENCRYPTED) ===");
            System.out.println("Page count: " + docA.getNumberOfPages());
            System.out.println("Version: " + docA.getVersion());
            System.out.println("Producer: " + docA.getDocumentInformation().getProducer());
            System.out.println("CreationDate: " + docA.getDocumentInformation().getCreationDate());
            System.out.println("ModDate: " + docA.getDocumentInformation().getModificationDate());
            System.out.println("Encrypted: " + docA.isEncrypted());

            assertEquals(6, docA.getNumberOfPages(), "Variant A must have strictly 6 pages!");
            assertEquals(1.5f, docA.getVersion(), 0.01f);
            assertEquals("OpenPDF 2.0.3", docA.getDocumentInformation().getProducer());
            assertNotNull(docA.getDocumentInformation().getCreationDate(), "CreationDate must exist");
            assertNull(docA.getDocumentInformation().getModificationDate(), "ModDate must be strictly null (removed)!");
            assertFalse(docA.isEncrypted(), "Variant A must NOT be encrypted!");

            COSDocument cosDocA = docA.getDocument();
            // Verify Obj 5: img0
            COSObject obj5 = cosDocA.getObjectFromPool(new COSObjectKey(5, 0));
            assertNotNull(obj5, "Obj 5 0 must exist");
            assertTrue(obj5.getObject() instanceof COSStream);

            // Verify Obj 16: img1
            COSObject obj16 = cosDocA.getObjectFromPool(new COSObjectKey(16, 0));
            assertNotNull(obj16, "Obj 16 0 must exist");
            assertTrue(obj16.getObject() instanceof COSStream);

            // Verify Obj 17: smask img2
            COSObject obj17 = cosDocA.getObjectFromPool(new COSObjectKey(17, 0));
            assertNotNull(obj17, "Obj 17 0 must exist");
            assertTrue(obj17.getObject() instanceof COSStream);

            // Verify Obj 18: img3
            COSObject obj18 = cosDocA.getObjectFromPool(new COSObjectKey(18, 0));
            assertNotNull(obj18, "Obj 18 0 must exist");
            assertTrue(obj18.getObject() instanceof COSStream);

            // Verify Obj 19: smask img4
            COSObject obj19 = cosDocA.getObjectFromPool(new COSObjectKey(19, 0));
            assertNotNull(obj19, "Obj 19 0 must exist");
            assertTrue(obj19.getObject() instanceof COSStream);

            // Verify Obj 20: img5
            COSObject obj20 = cosDocA.getObjectFromPool(new COSObjectKey(20, 0));
            assertNotNull(obj20, "Obj 20 0 must exist");
            assertTrue(obj20.getObject() instanceof COSStream);

            // Verify Obj 21: ICC profile stream
            COSObject obj21 = cosDocA.getObjectFromPool(new COSObjectKey(21, 0));
            assertNotNull(obj21, "Obj 21 0 must exist");
            assertTrue(obj21.getObject() instanceof COSStream);

            // Verify Obj 22: QR code (img6)
            COSObject obj22 = cosDocA.getObjectFromPool(new COSObjectKey(22, 0));
            assertNotNull(obj22, "Obj 22 0 must exist");
            assertTrue(obj22.getObject() instanceof COSStream);

            // Verify Obj 26: smask img7
            COSObject obj26 = cosDocA.getObjectFromPool(new COSObjectKey(26, 0));
            assertNotNull(obj26, "Obj 26 0 must exist");
            assertTrue(obj26.getObject() instanceof COSStream);

            // Verify Obj 27: img8
            COSObject obj27 = cosDocA.getObjectFromPool(new COSObjectKey(27, 0));
            assertNotNull(obj27, "Obj 27 0 must exist");
            assertTrue(obj27.getObject() instanceof COSStream);
            System.out.println("Variant A: All image object positions [5 0, 16 0, 17 0, 18 0, 19 0, 20 0, 22 0, 26 0, 27 0] verified!");
        }

        // ----------------------------------------------------
        // VARIANT B: Password Protected (AES-128, R=4, V=4)
        // ----------------------------------------------------
        String testPassword = "user123";
        stmt.meta.password = testPassword;
        byte[] pdfModeB = template.generate(stmt);
        File outB = new File("kotak_6pages_modeB.pdf");
        Files.write(outB.toPath(), pdfModeB);

        try (PDDocument docB = PDDocument.load(pdfModeB, testPassword)) {
            System.out.println("=== VARIANT B (PASSWORD PROTECTED) ===");
            System.out.println("Page count: " + docB.getNumberOfPages());
            System.out.println("Version: " + docB.getVersion());
            System.out.println("Producer: " + docB.getDocumentInformation().getProducer());
            System.out.println("CreationDate: " + docB.getDocumentInformation().getCreationDate());
            System.out.println("ModDate: " + docB.getDocumentInformation().getModificationDate());
            System.out.println("Encrypted: " + docB.isEncrypted());

            assertEquals(6, docB.getNumberOfPages(), "Variant B must have strictly 6 pages!");
            assertEquals(1.5f, docB.getVersion(), 0.01f);
            assertEquals("OpenPDF 2.0.3", docB.getDocumentInformation().getProducer());
            assertNotNull(docB.getDocumentInformation().getCreationDate(), "CreationDate must exist");
            assertNull(docB.getDocumentInformation().getModificationDate(), "ModDate must be strictly null (removed)!");
            assertTrue(docB.isEncrypted(), "Variant B must be encrypted!");

            // Check encryption dictionary details
            org.apache.pdfbox.pdmodel.encryption.PDEncryption enc = docB.getEncryption();
            assertNotNull(enc, "Encryption dictionary must exist");
            System.out.println("Revision (R): " + enc.getRevision());
            System.out.println("Version (V): " + enc.getVersion());
            System.out.println("Length: " + enc.getLength());
            System.out.println("Filter: " + enc.getFilter());

            assertEquals(4, enc.getRevision(), "Encryption revision must be 4");
            assertEquals(4, enc.getVersion(), "Encryption version must be 4 (Algorithm 4)");
            assertEquals(128, enc.getLength(), "Encryption key length must be 128");
            assertEquals("Standard", enc.getFilter());

            COSDocument cosDocB = docB.getDocument();
            // Verify identical object numbering in Variant B:
            COSObject obj5B = cosDocB.getObjectFromPool(new COSObjectKey(5, 0));
            assertNotNull(obj5B, "Obj 5 0 must exist in Variant B");

            COSObject obj16B = cosDocB.getObjectFromPool(new COSObjectKey(16, 0));
            assertNotNull(obj16B, "Obj 16 0 must exist in Variant B");

            COSObject obj17B = cosDocB.getObjectFromPool(new COSObjectKey(17, 0));
            assertNotNull(obj17B, "Obj 17 0 must exist in Variant B");

            COSObject obj18B = cosDocB.getObjectFromPool(new COSObjectKey(18, 0));
            assertNotNull(obj18B, "Obj 18 0 must exist in Variant B");

            COSObject obj19B = cosDocB.getObjectFromPool(new COSObjectKey(19, 0));
            assertNotNull(obj19B, "Obj 19 0 must exist in Variant B");

            COSObject obj20B = cosDocB.getObjectFromPool(new COSObjectKey(20, 0));
            assertNotNull(obj20B, "Obj 20 0 must exist in Variant B");

            COSObject obj22B = cosDocB.getObjectFromPool(new COSObjectKey(22, 0));
            assertNotNull(obj22B, "Obj 22 0 must exist in Variant B");

            COSObject obj26B = cosDocB.getObjectFromPool(new COSObjectKey(26, 0));
            assertNotNull(obj26B, "Obj 26 0 must exist in Variant B");

            COSObject obj27B = cosDocB.getObjectFromPool(new COSObjectKey(27, 0));
            assertNotNull(obj27B, "Obj 27 0 must exist in Variant B");
            System.out.println("Variant B: All image object positions [5 0, 16 0, 17 0, 18 0, 19 0, 20 0, 22 0, 26 0, 27 0] verified!");
        }
    }

    @Test
    public void testFullGeneration() throws Exception {
        Statement stmt = new Statement();
        stmt.meta = new StatementMeta();
        stmt.meta.template = BankTemplate.KOTAKNEW;
        stmt.meta.statementPeriodStart = "2026-01-01T00:00:00Z";
        stmt.meta.statementPeriodEnd = "2026-01-09T00:00:00Z";

        stmt.details = new StatementUserDetails();
        stmt.details.fullName = "KISHAN KUMAR";
        stmt.details.name = "KISHAN KUMAR";
        stmt.details.crn = "123456789";
        stmt.details.accountNumber = "53968160267";
        stmt.details.ifsc = "KKBK0000123";
        stmt.details.micr = "462485002";
        stmt.details.address = "HOUSE NO 123, BHOPAL\nMADHYA PRADESH - 462001";
        stmt.details.branchAddress = "Kotak Mahindra Bank Ltd, Ground Floor, Bhopal";
        stmt.details.startingBalance = 10000.0;

        stmt.transactions = new ArrayList<>();
        double bal = 10000.0;
        // Generate transactions across exactly 10 pages total (8 txn pages + summary + notice)
        for (int i = 0; i < 420; i++) {
            Transaction t = new Transaction();
            t.date = "02/01/2026";
            t.valueDate = "02/01/2026";
            t.description = "UPI/Payment/Merchant" + i + "/Ref" + (10000000 + i);
            t.reference = "UPI-" + (10000000 + i);
            if (i % 2 == 0) {
                t.debit = 100.0;
                t.credit = 0.0;
                bal -= 100.0;
            } else {
                t.debit = 0.0;
                t.credit = 250.0;
                bal += 250.0;
            }
            t.balance = bal;
            stmt.transactions.add(t);
        }

        KotakNewTemplate template = new KotakNewTemplate();
        byte[] pdfBytes = template.generate(stmt);
        File out = new File("generated_kotak_verified.pdf");
        Files.write(out.toPath(), pdfBytes);
        System.out.println("=== GENERATED KOTAK PDF VERIFICATION ===");
        System.out.println("Generated PDF length: " + pdfBytes.length + " bytes (Target: ~239,466 bytes)");

        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            System.out.println("Page count: " + doc.getNumberOfPages());
            System.out.println("Version: " + doc.getVersion());
            System.out.println("Producer: " + doc.getDocumentInformation().getProducer());
            System.out.println("Creator: " + doc.getDocumentInformation().getCreator());
            System.out.println("CreationDate: " + doc.getDocumentInformation().getCreationDate().getTime());

            assertEquals(1.5f, doc.getVersion(), 0.01f);
            assertEquals("OpenPDF 2.0.3", doc.getDocumentInformation().getProducer());
            assertNull(doc.getDocumentInformation().getCreator());
            assertFalse(doc.isEncrypted());

            Set<String> uniqueFonts = new TreeSet<>();
            Map<String, PDImageXObject> uniqueImages = new LinkedHashMap<>();

            for (PDPage page : doc.getPages()) {
                PDResources res = page.getResources();
                if (res != null) {
                    for (COSName fName : res.getFontNames()) {
                        PDFont f = res.getFont(fName);
                        uniqueFonts.add(f.getName());
                    }
                    for (COSName xName : res.getXObjectNames()) {
                        PDXObject xo = res.getXObject(xName);
                        if (xo instanceof PDImageXObject img) {
                            uniqueImages.put(xName.getName(), img);
                        }
                    }
                }
            }

            System.out.println("Unique font count: " + uniqueFonts.size() + " -> " + uniqueFonts);
            System.out.println("Unique image count: " + uniqueImages.size() + " -> " + uniqueImages.keySet());

            // Image size and count checks
            assertEquals(9, uniqueImages.size(), "Image count must be strictly 9!");
            assertEquals(3, uniqueFonts.size(), "Font count must be strictly 3!");

            assertTrue(uniqueFonts.stream().anyMatch(f -> f.contains("SemiBold")), "SemiBold font present");
            assertTrue(uniqueFonts.stream().anyMatch(f -> f.contains("Regular")), "Regular font present");
            assertTrue(uniqueFonts.stream().anyMatch(f -> f.equals("Helvetica")), "Helvetica font present");

            // Check dimensions of all 9 images
            assertEquals(1240, uniqueImages.get("img0").getWidth());
            assertEquals(156, uniqueImages.get("img0").getHeight());

            assertEquals(818, uniqueImages.get("img1").getWidth());
            assertEquals(135, uniqueImages.get("img1").getHeight());

            assertEquals(200, uniqueImages.get("img2").getWidth());
            assertEquals(200, uniqueImages.get("img2").getHeight());

            assertEquals(200, uniqueImages.get("img3").getWidth());
            assertEquals(200, uniqueImages.get("img3").getHeight());

            assertEquals(200, uniqueImages.get("img4").getWidth());
            assertEquals(200, uniqueImages.get("img4").getHeight());

            assertEquals(200, uniqueImages.get("img5").getWidth());
            assertEquals(200, uniqueImages.get("img5").getHeight());

            assertEquals(463, uniqueImages.get("img6").getWidth());
            assertEquals(463, uniqueImages.get("img6").getHeight());

            assertEquals(100, uniqueImages.get("img7").getWidth());
            assertEquals(100, uniqueImages.get("img7").getHeight());

            assertEquals(100, uniqueImages.get("img8").getWidth());
            assertEquals(100, uniqueImages.get("img8").getHeight());

            // Object inspection
            COSDocument cosDoc = doc.getDocument();
            List<COSObject> objects = new ArrayList<>(cosDoc.getObjects());
            objects.sort(Comparator.comparingLong(COSObject::getObjectNumber));
            System.out.println("=== OBJECT LIST (" + objects.size() + " objects) ===");
            for (COSObject obj : objects) {
                System.out.println("Obj " + obj.getObjectNumber() + " 0: " + obj.getObject().getClass().getSimpleName());
            }

            // Assert exact object structure:
            // Obj 1: Form XObject (Template)
            COSObject obj1 = cosDoc.getObjectFromPool(new COSObjectKey(1, 0));
            assertNotNull(obj1);
            assertTrue(obj1.getObject() instanceof COSStream);
            COSStream stream1 = (COSStream) obj1.getObject();
            assertEquals("Form", stream1.getNameAsString(COSName.SUBTYPE));

            // Obj 5: img0
            COSObject obj5 = cosDoc.getObjectFromPool(new COSObjectKey(5, 0));
            assertNotNull(obj5);
            assertTrue(obj5.getObject() instanceof COSStream);

            // Obj 6: Page 1 content stream
            COSObject obj6 = cosDoc.getObjectFromPool(new COSObjectKey(6, 0));
            assertNotNull(obj6);
            assertTrue(obj6.getObject() instanceof COSStream);

            System.out.println("ALL ASSERTIONS PASSED PERFECTLY!");
        }
    }
}
