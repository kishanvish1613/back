package com.user.pdfservice;

import com.user.pdfservice.model.BankTemplate;
import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.StatementUserDetails;
import com.user.pdfservice.model.StatementMeta;
import com.user.pdfservice.model.Transaction;
import com.user.pdfservice.ocr.OcrProcessingService;
import com.user.pdfservice.ocr.OcrResult;
import com.user.pdfservice.template.TemplateRegistry;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuCurrentVerificationTest {

    @Test
    public void testAuOcrExtraction() throws Exception {
        File pdfFile = new File("C:/Users/real/.gemini/antigravity/brain/5e191c5a-8270-4075-8fa4-b9ceab0cc3cf/.user_uploaded/media_1790250362396.pdf");
        if (!pdfFile.exists()) {
            System.out.println("AU PDF not found for OCR test");
            return;
        }

        try (PDDocument doc = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(2);
            String rawText = stripper.getText(doc);

            OcrResult r = OcrProcessingService.process(rawText);

            System.out.println("=== AU OCR RESULT ===");
            System.out.println("Bank: " + r.bankName);
            System.out.println("Name: " + r.name);
            System.out.println("A/C No: " + r.accountNumber);
            System.out.println("Cust ID: " + r.customerRelNo);
            System.out.println("IFSC: " + r.ifsc);
            System.out.println("Type: " + r.accountType);
            System.out.println("Branch: " + r.branch);
            System.out.println("Nominee: " + r.nomineeName);
            System.out.println("Opening Bal: " + r.openingBalance);
            System.out.println("Period: " + r.statementPeriodStart + " to " + r.statementPeriodEnd);

            assertEquals("AU", r.bankName);
            assertNotNull(r.accountNumber);
            assertEquals("2502248474908993", r.accountNumber);
            assertEquals("39867327", r.customerRelNo);
            assertEquals("AUBL0002484", r.ifsc);
            assertEquals("Hamidiya Road Bhopal", r.branch);
            assertEquals("Shabnam Qureshi", r.nomineeName);
        }
    }

    @Test
    public void testAuPdfGenerationAndRender() throws Exception {
        Statement stmt = new Statement();

        stmt.details = new StatementUserDetails();
        stmt.details.fullName = "M S PHARMA";
        stmt.details.accountNumber = "2502248474908993";
        stmt.details.customerRelNo = "39867327";
        stmt.details.customerType = "Sole Propertary";
        stmt.details.accountType = "AU Current Account-Basic";
        stmt.details.branch = "Hamidiya Road Bhopal";
        stmt.details.ifsc = "AUBL0002484";
        stmt.details.nomineeName = "Shabnam Qureshi";
        stmt.details.address = "Shop No/plot No Shop No G-70\nGround, Floor Vardhman City Plaza\nDawa, Bazar, Hamidia Road Tehsil\nHuzur\nBhopal - 462001, Madhya Pradesh -\nIndia";
        stmt.details.startingBalance = 85482.15;

        stmt.meta = new StatementMeta();
        stmt.meta.generatedAt = "11 Aug 2026";
        stmt.meta.statementPeriodStart = "01 Jan 2026";
        stmt.meta.statementPeriodEnd = "11 Aug 2026";

        // Generate 25 realistic AU transactions
        List<Transaction> txns = new ArrayList<>();
        double balance = 85482.15;

        String[] narrations = {
                "I/W CHEQUE PAID-PENJAA LIFE SCIENCE-000017",
                "UPI/DR/053256600898/BANK ACCOUNT XXXXXXXX7338/INDB/201000827338/PAYMENT FROM PHONEPE AU JAGATPURA",
                "UPI/CR/327501249380/SOHAIL MANSOORI/AIRP/8770251953/PAYMENT FROM PHONEPE AU JAGATPURA",
                "UPI/CR/600118109537/ARZAN KHAN/KKBK/7549069611/UPI AU JAGATPURA",
                "UPI/DR/118533330605/AYRA COLLECTION/AUBL/2021248429617818/PAYMENT FROM PHONEPE AU JAGATPURA",
                "AUQR C2 010126 MSPHARMA0891 CR - 2402201158745230 - AU SMALL FINANCE BANK LIMITED QR SETTLEM - AU BANK",
                "UPI/CR/625988794136/AHTESHAM SO MOHTASHIM MOHD KHAN/BKID/900810110012777/PAYMENT FROM PHONEPE AU JAGATPURA",
                "UPI/DR/348516389659/SUBHAAN/KKBK/9147488368/PAYMENT FROM PHONEPE AU JAGATPURA",
                "UPI/CR/636728440812/MD FAIZ/KKBK/9846318287/ADARSH OTC KE BILL KA PAYMENT AU JAGATPURA",
                "UPI/DR/746098383078/MS AVI PHARMACEUTICALS/ICIC/326705000875/PAYMENT FROM PHONEPE AU JAGATPURA",
                "UPI/CR/636820595262/MD FAIZ/KKBK/9846318287/PAID VIA CRED AU JAGATPURA",
                "UPI/CR/636815595543/MD FAIZ/KKBK/9846318287/PAID VIA CRED AU JAGATPURA",
                "AUQR C2 030126 MSPHARMA0891 CR - 2402201158745230 - AU SMALL FINANCE BANK LIMITED QR SETTLEM - AU BANK",
                "UPI/CR/365793405480/KHALID MIYAN S O MOHAMMAD MIYAN/BARB/04960100017121/PAYMENT FROM PHONEPE AU JAGATPURA",
                "UPI/CR/362102286969/AMIT KUMAR JAIN/SBIN/00000034436744227/PAYMENT FROM PHONEPE AU JAGATPURA",
                "UPI/CR/147852870189/HAMEED KHAN/SBIN/00000033978388870/PAYMENT FROM PHONEPE AU JAGATPURA",
                "UPI/CR/462176739607/SABA SHENAZ/UBIN/325202011022181/PAYMENT FROM PHONEPE AU JAGATPURA",
                "UPI/DR/985374674768/SABA SHENAZ/UBIN/325202011022181/PAYMENT FROM PHONEPE AU JAGATPURA",
                "UPI/DR/689180681042/SABA SHENAZ/UBIN/325202011022181/PAYMENT FROM PHONEPE AU JAGATPURA",
                "SMS_ALERT_CHARGE_OCT25_DEC25",
                "UPI/DR/177150595934/M/S.J M K MEDICAL AGENCY/ICIC/004105018005/PAYMENT FROM PHONEPE AU JAGATPURA",
                "UPI/CR/637135478525/MR IZHAR MAKRANI/IDIB/7687688391/UPI AU JAGATPURA",
                "UPI/CR/608965064799/SOHAIL MANSOORI/AIRP/8770251953/PAYMENT FROM PHONEPE AU JAGATPURA",
                "UPI/CR/603976092251/SOHAIL MANSOORI/AIRP/8770251953/PAYMENT FROM PHONEPE AU JAGATPURA",
                "UPI/CR/637127144493/MD FAIZ/KKBK/9846318287/ADARSH OTC AU JAGATPURA"
        };

        String[] refs = {
                "000000000017",
                "AXLcf68e610b6334f7dae3fd6c832ec038a",
                "AXLa4bf9cdee53c4d01b5288659475fe871",
                "KMBMABCD99NDnthO0ghsIInlZVafarrZTz",
                "AXLbb05b5de7a4a4169b1a067c144341afa",
                "MR6120155146ic5IJS6V",
                "AXL3226bb179b854012a3ef157737208d0f",
                "AXLcd7b8642a7f84bcab50bf23b8ded9d6d",
                "YCD01KDXCXGVYBQR5D9XCKMQ0GEDJVvZLXr",
                "YBLc1668a3f70ed4896af442fda985df2ec",
                "YCD01KDZGKK55JV2K9SS6Z8GMW5B30k2mna",
                "YCD01KDZGXBPZH9M48DMKDWR5S3WH0Da9Gz",
                "MR6311810630KrAHlhn3",
                "YBLec97cd7cbabc427396caf7ba725ccb9f",
                "YBL06dc237f57a4963b6bd906ae479c2ba",
                "YBL723fcb2fe43f48ce91d5cfacf26d27a2",
                "YBLe2cc2fbf1f164e96a05989fce4c0c3f",
                "YBLc043baed04144fb6ae74e0ed54247fb9",
                "YBL15a893ed0be94b4389e4909553ec1537",
                "",
                "AXL03c064f838f04a7393dbf75484758de3",
                "AXI784542e3edf84c2ebe641a03faed58d5",
                "AXL30df183287fe423a8bc62d32730bf8a",
                "AXL4fba9bbdfe7948798217cdc332991738",
                "YCD01KE78EDXN8DQ37C2R5FX8Q85ZVQrXnY"
        };

        double[] debits = {
                10710.00, 32676.00, 0, 0, 14850.00, 0, 0, 3385.00, 0, 10000.00,
                0, 0, 0, 0, 0, 0, 0, 200.00, 100.00, 17.70,
                13138.00, 0, 0, 0, 0
        };

        double[] credits = {
                0, 0, 23.00, 342.00, 0, 55.00, 913.00, 0, 535.00, 0,
                1103.00, 2970.00, 265.00, 100.00, 1318.00, 2000.00, 6000.00, 0, 0, 0,
                0, 1000.00, 80.00, 130.00, 520.00
        };

        for (int i = 0; i < narrations.length; i++) {
            Transaction tx = new Transaction();
            tx.date = "2026-01-01";
            tx.valueDate = "2026-01-01";
            tx.description = narrations[i];
            tx.reference = refs[i];
            tx.debit = debits[i];
            tx.credit = credits[i];
            if (tx.debit > 0) balance -= tx.debit;
            if (tx.credit > 0) balance += tx.credit;
            tx.balance = balance;
            txns.add(tx);
        }

        stmt.transactions = txns;

        byte[] pdfBytes = TemplateRegistry.get(BankTemplate.AU).generate(stmt);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        File pdfOut = new File("test_au_current_generated.pdf");
        try (FileOutputStream fos = new FileOutputStream(pdfOut)) {
            fos.write(pdfBytes);
        }
        System.out.println("Generated AU Current PDF at: " + pdfOut.getAbsolutePath());

        // Render generated PDF pages to PNG
        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            for (int p = 0; p < doc.getNumberOfPages(); p++) {
                BufferedImage bi = renderer.renderImageWithDPI(p, 150);
                File imgOut = new File("target/au_current_page_" + (p + 1) + ".png");
                imgOut.getParentFile().mkdirs();
                ImageIO.write(bi, "PNG", imgOut);
                System.out.println("Saved rendered page " + (p + 1) + " to: " + imgOut.getAbsolutePath());
            }
        }
    }
}

