package com.user.pdfservice;

import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDestinationNameTreeNode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BoiPdfAnalyzerTest {

    @Test
    public void analyzeOriginalBoiPdf() throws Exception {
        File file = new File("C:/Users/real/.gemini/antigravity/brain/ffddd075-61d9-416b-98f0-31880d7be51c/.user_uploaded/media_1792427481346_ed50bdd6.pdf");
        if (!file.exists()) {
            file = new File("boi.pdf");
        }
        if (!file.exists()) {
            System.out.println("File not found!");
            return;
        }

        System.out.println("Inspecting file: " + file.getAbsolutePath());
        // Try without password or with common passwords
        PDDocument doc = null;
        try {
            doc = PDDocument.load(file, "");
        } catch (Exception e1) {
            try {
                doc = PDDocument.load(file, "user");
            } catch (Exception e2) {
                System.out.println("Could not open with empty or 'user': " + e2.getMessage());
                // Still inspect low-level COS objects if encrypted
            }
        }

        if (doc != null) {
            try {
                System.out.println("=== BOI PDF ANALYSIS ===");
                System.out.println("Version: " + doc.getVersion());
                System.out.println("Pages: " + doc.getNumberOfPages());
                System.out.println("Encrypted: " + doc.isEncrypted());
                if (doc.getEncryption() != null) {
                    System.out.println("--- ENCRYPTION ---");
                    System.out.println("Filter: " + doc.getEncryption().getFilter());
                    System.out.println("Length: " + doc.getEncryption().getLength());
                    System.out.println("Version (V): " + doc.getEncryption().getVersion());
                    System.out.println("Revision (R): " + doc.getEncryption().getRevision());
                    System.out.println("Permissions: " + doc.getEncryption().getPermissions());
                }

                PDDocumentInformation info = doc.getDocumentInformation();
                if (info != null) {
                    System.out.println("--- INFO DICTIONARY ---");
                    System.out.println("Creator: " + info.getCreator());
                    System.out.println("Producer: " + info.getProducer());
                    System.out.println("Title: " + info.getTitle());
                    System.out.println("Subject: " + info.getSubject());
                    System.out.println("Author: " + info.getAuthor());
                    System.out.println("CreationDate: " + info.getCreationDate());
                    System.out.println("ModDate: " + info.getModificationDate());
                    COSDictionary infoDict = info.getCOSObject();
                    for (COSName key : infoDict.keySet()) {
                        System.out.println("  Key: " + key.getName() + " -> " + infoDict.getItem(key));
                    }
                }

                System.out.println("--- CATALOG ---");
                COSDictionary catDict = doc.getDocumentCatalog().getCOSObject();
                for (COSName key : catDict.keySet()) {
                    System.out.println("  Catalog Key: " + key.getName() + " -> " + catDict.getItem(key));
                }

                if (doc.getDocumentCatalog().getNames() != null) {
                    System.out.println("--- NAMES DICTIONARY ---");
                    COSDictionary namesDict = doc.getDocumentCatalog().getNames().getCOSObject();
                    for (COSName key : namesDict.keySet()) {
                        System.out.println("  Names Key: " + key.getName() + " -> " + namesDict.getItem(key));
                    }
                    if (doc.getDocumentCatalog().getNames().getDests() != null) {
                        PDDestinationNameTreeNode dests = doc.getDocumentCatalog().getNames().getDests();
                        System.out.println("  Dests names: " + (dests.getNames() != null ? dests.getNames().keySet() : "null"));
                        if (dests.getNames() != null) {
                            for (String dName : dests.getNames().keySet()) {
                                Object dVal = dests.getNames().get(dName);
                                System.out.println("    Dest: " + dName + " -> " + dVal);
                                if (dVal instanceof org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination) {
                                    org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination xyz = (org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination) dVal;
                                    System.out.printf("      left=%d, top=%d, zoom=%.2f%n", xyz.getLeft(), xyz.getTop(), xyz.getZoom());
                                }
                                break; // print first
                            }
                        }
                    }
                }

                if (doc.getNumberOfPages() > 1) {
                    PDPage p2 = doc.getPage(1);
                    System.out.println("--- PAGE 2 ---");
                    System.out.println("MediaBox: " + p2.getMediaBox());
                    PDResources res2 = p2.getResources();
                    if (res2 != null) {
                        System.out.println("Font names P2: " + res2.getFontNames());
                        System.out.println("XObject names P2: " + (res2.getXObjectNames() != null ? res2.getXObjectNames() : "none"));
                    }
                }

                if (doc.getNumberOfPages() > 0) {
                    PDPage p1 = doc.getPage(0);
                    PDResources res = p1.getResources();
                    if (res != null) {
                        for (COSName xn : res.getXObjectNames()) {
                            org.apache.pdfbox.pdmodel.graphics.PDXObject xo = res.getXObject(xn);
                            System.out.println("  XObject: " + xn.getName() + " -> " + xo.getClass().getSimpleName());
                            if (xo instanceof PDImageXObject) {
                                PDImageXObject img = (PDImageXObject) xo;
                                System.out.println("    Width: " + img.getWidth() + ", Height: " + img.getHeight());
                                try (InputStream rawIs = img.getCOSObject().createRawInputStream();
                                     java.io.FileOutputStream fos = new java.io.FileOutputStream("src/main/resources/templates/boi_" + xn.getName() + "_raw.flate")) {
                                    byte[] buf = rawIs.readAllBytes();
                                    fos.write(buf);
                                    System.out.println("    Saved " + xn.getName() + " raw bytes: " + buf.length);
                                }
                                try (InputStream is = img.createInputStream();
                                     java.io.FileOutputStream fos = new java.io.FileOutputStream("src/main/resources/templates/boi_" + xn.getName() + "_decompressed.bin")) {
                                    byte[] buf = is.readAllBytes();
                                    fos.write(buf);
                                    System.out.println("    Saved " + xn.getName() + " decompressed bytes: " + buf.length);
                                }
                            }
                        }

                        // Also extract ICC Profile
                        COSBase csBase = p1.getResources().getCOSObject().getCOSDictionary(COSName.XOBJECT)
                                .getCOSDictionary(COSName.getPDFName("img1")).getDictionaryObject(COSName.COLORSPACE);
                        System.out.println("  ColorSpace base: " + csBase);
                        if (csBase instanceof COSArray) {
                            COSArray csArr = (COSArray) csBase;
                            COSBase streamObj = csArr.getObject(1);
                            if (streamObj instanceof COSStream) {
                                COSStream iccStream = (COSStream) streamObj;
                                try (InputStream rawIs = iccStream.createRawInputStream();
                                     java.io.FileOutputStream fos = new java.io.FileOutputStream("src/main/resources/templates/boi_icc_raw.flate")) {
                                    byte[] buf = rawIs.readAllBytes();
                                    fos.write(buf);
                                    System.out.println("    Saved ICC raw bytes: " + buf.length);
                                }
                            }
                        }
                    }
                }
            } finally {
                doc.close();
            }
        }
    }

    @Test
    public void inspectGeneratedBoiObjects() throws Exception {
        com.user.pdfservice.model.Statement stmt = new com.user.pdfservice.model.Statement();
        stmt.meta = new com.user.pdfservice.model.StatementMeta();
        stmt.meta.statementPeriodStart = "2025-02-01";
        stmt.meta.statementPeriodEnd = "2026-01-31";
        stmt.meta.generatedAt = "2026-03-23T11:49:30Z";
        stmt.meta.password = ""; // Empty password like original

        stmt.details = new com.user.pdfservice.model.StatementUserDetails();
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
        com.user.pdfservice.model.Transaction tx = new com.user.pdfservice.model.Transaction();
        tx.date = "2025-02-01";
        tx.description = "OPENING BALANCE";
        tx.debit = 0.0;
        tx.credit = 10000.0;
        tx.balance = 10501.50;
        stmt.transactions.add(tx);

        com.user.pdfservice.template.boi.BoiTemplate template = new com.user.pdfservice.template.boi.BoiTemplate();
        byte[] pdfBytes = template.generate(stmt);

        try (PDDocument genDoc = PDDocument.load(pdfBytes, "")) {
            System.out.println("=== GENERATED BOI PDF ===");
            System.out.println("Version: " + genDoc.getVersion());
            System.out.println("Pages: " + genDoc.getNumberOfPages());
            System.out.println("Encrypted: " + genDoc.isEncrypted());
            if (genDoc.getEncryption() != null) {
                System.out.println("--- ENCRYPTION ---");
                System.out.println("Filter: " + genDoc.getEncryption().getFilter());
                System.out.println("Length: " + genDoc.getEncryption().getLength());
                System.out.println("Version: " + genDoc.getEncryption().getVersion());
                System.out.println("Revision: " + genDoc.getEncryption().getRevision());
                System.out.println("Permissions: " + genDoc.getEncryption().getPermissions());
            }

            System.out.println("--- INFO ---");
            PDDocumentInformation info = genDoc.getDocumentInformation();
            System.out.println("Producer: " + info.getProducer());
            System.out.println("Creator: " + info.getCreator());
            System.out.println("Title: " + info.getTitle());

            System.out.println("--- CATALOG ---");
            for (COSName key : genDoc.getDocumentCatalog().getCOSObject().keySet()) {
                System.out.println("Catalog: " + key.getName() + " -> " + genDoc.getDocumentCatalog().getCOSObject().getItem(key));
            }

            System.out.println("--- PAGE 1 RESOURCES ---");
            PDPage p1 = genDoc.getPage(0);
            COSDictionary p1Dict = p1.getCOSObject();
            System.out.println("Page 1 keys: " + p1Dict.keySet());
            System.out.println("Page 1 Resources item: " + p1Dict.getItem(COSName.RESOURCES));
            if (p1.getResources() != null) {
                COSDictionary resDict = p1.getResources().getCOSObject();
                for (COSName k : resDict.keySet()) {
                    System.out.println("  Res: " + k.getName() + " -> " + resDict.getItem(k));
                }
            }
        }
    }
}
