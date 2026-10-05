package com.user.pdfservice.template.boi;

import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.Transaction;
import com.user.pdfservice.template.BankPdfTemplate;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class BoiTemplate implements BankPdfTemplate {

    private static final float PAGE_W = 792.00f;
    private static final float PAGE_H = 842.00f;

    // Table Column Widths (Sum = 731.00 pt, matching x = 31.00 to 762.00)
    private static final float TABLE_X = 31.00f;
    private static final float TABLE_W = 731.00f;
    private static final float COL_SNO_W = 29.00f;
    private static final float COL_DATE_W = 79.50f;
    private static final float COL_INST_W = 76.00f;
    private static final float COL_DESC_W = 245.00f;
    private static final float COL_DEBIT_W = 79.00f;
    private static final float COL_CREDIT_W = 81.00f;
    private static final float COL_BAL_W = 141.50f;

    private static final float ROW_H = 25.00f;
    private static final float HEADER_ROW_H = 20.00f;
    private static final float FOOTER_Y = 809.50f;

    private final PDType1Font fontRegular = PDType1Font.HELVETICA;
    private final PDType1Font fontBold = PDType1Font.HELVETICA_BOLD;

    private PDImageXObject logoImage;

    @Override
    public byte[] generate(Statement statement) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            // Password Protection if provided
            if (statement.meta.password != null && !statement.meta.password.trim().isEmpty()) {
                AccessPermission ap = new AccessPermission();
                ap.setReadOnly();
                StandardProtectionPolicy spp = new StandardProtectionPolicy(
                        statement.meta.password,
                        statement.meta.password,
                        ap
                );
                spp.setEncryptionKeyLength(128);
                spp.setPreferAES(true);
                doc.protect(spp);
            }

            // Document Info
            PDDocumentInformation info = new PDDocumentInformation();
            info.setProducer("iText1.3.1 by lowagie.com (based on itext-paulo-154)");
            info.setCreator("JasperReports (Account_statement)");
            info.setTitle("Statement of Account");
            Calendar now = Calendar.getInstance();
            info.setCreationDate(now);
            info.setModificationDate(now);
            doc.setDocumentInformation(info);

            // Load Logo
            loadLogo(doc);

            List<Transaction> txns = statement.transactions != null ? statement.transactions : new ArrayList<>();
            int txIndex = 0;
            int pageNum = 1;

            // First Page: exactly 15 transactions
            int page1Rows = Math.min(15, txns.size());
            List<Transaction> p1Txns = txns.subList(0, page1Rows);
            renderPage(doc, statement, p1Txns, 0, 1, true, false, page1Rows == txns.size());
            txIndex = page1Rows;

            // Subsequent pages: 29 transactions per page
            while (txIndex < txns.size()) {
                pageNum++;
                int remaining = txns.size() - txIndex;
                int count = Math.min(29, remaining);
                List<Transaction> pageTxns = txns.subList(txIndex, txIndex + count);
                boolean isLastChunk = (txIndex + count == txns.size());

                // Check if last page disclaimer fits
                boolean canFitDisclaimerOnThisPage = isLastChunk && (count <= 24);

                renderPage(doc, statement, pageTxns, txIndex, pageNum, false, false, canFitDisclaimerOnThisPage);
                txIndex += count;

                if (isLastChunk && !canFitDisclaimerOnThisPage) {
                    pageNum++;
                    renderPage(doc, statement, new ArrayList<>(), txIndex, pageNum, false, true, true);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private void loadLogo(PDDocument doc) {
        try {
            InputStream is = getClass().getResourceAsStream("/templates/boi_logo.png");
            if (is != null) {
                BufferedImage bi = ImageIO.read(is);
                logoImage = LosslessFactory.createFromImage(doc, bi);
            }
        } catch (Exception ignored) {
        }
    }

    private void renderPage(PDDocument doc, Statement statement, List<Transaction> pageTxns,
                            int startIndex, int pageNum, boolean isFirstPage, boolean isDisclaimerOnlyPage, boolean renderDisclaimer) throws Exception {
        PDPage page = new PDPage(new PDRectangle(PAGE_W, PAGE_H));
        doc.addPage(page);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            float topY;

            if (isFirstPage) {
                topY = renderPage1Header(cs, statement);
            } else {
                topY = renderContinuousHeader(cs, statement);
            }

            float currentY = topY;

            if (!isDisclaimerOnlyPage && pageTxns != null && !pageTxns.isEmpty()) {
                // Table Header
                renderTableHeader(cs, currentY);
                currentY += HEADER_ROW_H;

                // Render Transaction Rows
                for (int i = 0; i < pageTxns.size(); i++) {
                    Transaction tx = pageTxns.get(i);
                    renderTableRow(cs, tx, startIndex + i + 1, currentY);
                    currentY += ROW_H;
                }
            }

            if (renderDisclaimer) {
                float disclaimerTopY = isDisclaimerOnlyPage ? topY + 30.00f : Math.max(currentY + 25.00f, 630.00f);
                renderDisclaimerBlock(cs, disclaimerTopY);
            }

            // Page Number Footer (centered at bottom)
            drawText(cs, fontRegular, 8.5f, "Page:" + pageNum, PAGE_W / 2.0f, FOOTER_Y, true);
        }
    }

    private float renderPage1Header(PDPageContentStream cs, Statement stmt) throws Exception {
        // 1. Top Date (Left, aligned with calibrated coordinate y=105.50)
        String genDate = formatDate(stmt.meta.generatedAt);
        drawText(cs, fontBold, 9.0f, "Date", 36.00f, 105.50f, false);
        drawText(cs, fontRegular, 9.0f, genDate, 69.93f, 105.50f, false);

        // 2. Bank of India Logo (Top Right, calibrated geometry)
        if (logoImage != null) {
            cs.drawImage(logoImage, 529.00f, 729.00f, 152.97f, 88.00f);
        }

        // 3. Center Titles (Calibrated: Bank of India Y=134.35, Branch Y=155.46)
        drawText(cs, fontBold, 14.0f, "Bank Of India", PAGE_W / 2.0f, 134.35f, true);

        // Clean branch string
        String rawBranch = stmt.details.branch != null ? stmt.details.branch.trim() : "RATIBAD";
        String cleanBranch = rawBranch.replaceAll("(?i)^Bank\\s+Of\\s+India\\s*", "").trim();
        if (cleanBranch.isEmpty()) cleanBranch = "RATIBAD";

        // Subtitle line at Y=155.46: "Bank Of India" (X: 298.77pt, Font: 9.0pt) and Branch (X: 385.00pt, Font: 9.0pt)
        drawText(cs, fontBold, 9.0f, "Bank Of India", 298.77f, 155.46f, false);
        drawText(cs, fontBold, 9.0f, cleanBranch.toUpperCase(), 385.00f, 155.46f, false);

        // 4. Details Container Box: calibrated frame
        float boxTopY = 166.00f;
        float boxH = 185.50f;
        float boxBottomY = boxTopY + boxH; // 351.50f
        float pdfBoxBottomY = PAGE_H - boxBottomY;

        cs.setLineWidth(1.00f);
        cs.setStrokingColor(0, 0, 0);
        cs.addRect(TABLE_X, pdfBoxBottomY, TABLE_W, boxH);
        cs.stroke();

        // 5. Left Column (Customer Details)
        float leftX = 33.00f;
        float leftStartY = 176.28f;

        // Clean Name
        String name = (stmt.details.fullName != null && !stmt.details.fullName.trim().isEmpty())
                ? sanitizeLine(stmt.details.fullName)
                : (stmt.details.name != null ? sanitizeLine(stmt.details.name) : "M/S SHIVAY ORTHO CARE CLINIC PHYSIOTHERAPY &");
        drawText(cs, fontRegular, 9.0f, name.toUpperCase(), leftX, leftStartY, false);

        // Clean Address Lines
        List<String> cleanAddrLines = getCleanAddressLines(stmt.details.address);
        float[] addrYPositions = { 196.28f, 216.28f, 236.28f, 256.28f, 277.28f };
        for (int i = 0; i < Math.min(addrYPositions.length, cleanAddrLines.size()); i++) {
            drawText(cs, fontRegular, 9.0f, cleanAddrLines.get(i).toUpperCase(), leftX, addrYPositions[i], false);
        }

        // JOINT HOLDER
        drawText(cs, fontBold, 9.0f, "JOINT HOLDER:", leftX, 295.28f, false);

        // OPENING BALANCE
        float opBalY = 335.50f;
        drawText(cs, fontBold, 10.0f, "OPENING BALANCE:", leftX, opBalY, false);
        double opBal = stmt.details.startingBalance;
        String opBalStr = formatAmount(opBal) + (opBal >= 0 ? " Cr" : " Dr");
        drawText(cs, fontRegular, 9.0f, opBalStr, 270.42f, opBalY, false);

        // 6. Right Column (Account Metadata - Calibrated Positions)
        float labelX = 518.00f;
        float valX = 600.00f;

        drawText(cs, fontBold, 9.0f, "CUSTID :", labelX, 176.28f, false);
        drawText(cs, fontRegular, 9.0f, safe(stmt.details.customerRelNo, "CC9396053"), valX, 176.28f, false);

        drawText(cs, fontBold, 9.0f, "A/C NO :", labelX, 196.28f, false);
        drawText(cs, fontRegular, 9.0f, safe(stmt.details.accountNumber, "907020110000340"), valX, 196.28f, false);

        drawText(cs, fontBold, 9.0f, "TYPE :", labelX, 216.28f, false);
        drawText(cs, fontRegular, 9.0f, safe(stmt.details.accountType, "CURRENT- GENERAL"), valX, 216.28f, false);

        drawText(cs, fontBold, 9.0f, "CURRENCY :", labelX, 236.28f, false);
        drawText(cs, fontRegular, 9.0f, "INR", valX, 236.28f, false);

        drawText(cs, fontBold, 9.0f, "IFSC CODE :", labelX, 256.28f, false);
        drawText(cs, fontRegular, 9.0f, safe(stmt.details.ifsc, "BKID0009070"), valX, 256.28f, false);

        drawText(cs, fontBold, 9.0f, "MICR CODE :", labelX, 277.28f, false);
        drawText(cs, fontRegular, 9.0f, safe(stmt.details.micr, "462013018"), 599.00f, 277.28f, false);

        drawText(cs, fontBold, 9.0f, "NOMINEE :", labelX, 295.28f, false);
        drawText(cs, fontRegular, 9.0f, safe(stmt.details.nomineeName, "YES"), valX, 295.28f, false);

        // 7. Statement of Account Sub-Header (Centered, calibrated at Y=376.50)
        String subHeader = "Statement of Account " + safe(stmt.details.accountNumber, "907020110000340")
                + "          FROM " + formatDate(stmt.meta.statementPeriodStart)
                + " TO " + formatDate(stmt.meta.statementPeriodEnd);
        drawText(cs, fontBold, 10.0f, subHeader, PAGE_W / 2.0f, 376.50f, true);

        return 384.50f; // Table Header starts exactly at calibrated Y=384.50
    }

    private float renderContinuousHeader(PDPageContentStream cs, Statement stmt) throws Exception {
        String subHeader = "Statement of Account " + safe(stmt.details.accountNumber, "907020110000340")
                + "          FROM " + formatDate(stmt.meta.statementPeriodStart)
                + " TO " + formatDate(stmt.meta.statementPeriodEnd);
        drawText(cs, fontBold, 10.0f, subHeader, PAGE_W / 2.0f, 30.00f, true);
        return 45.00f; // Table Header row starts here
    }

    private void renderTableHeader(PDPageContentStream cs, float topY) throws Exception {
        float pdfY = PAGE_H - topY - HEADER_ROW_H;
        cs.setLineWidth(0.75f);
        cs.setStrokingColor(0, 0, 0);

        float[] colWidths = { COL_SNO_W, COL_DATE_W, COL_INST_W, COL_DESC_W, COL_DEBIT_W, COL_CREDIT_W, COL_BAL_W };
        String[] colTitles = { "SNO", "TRAN DATE", "INST NO", "DESCRIPTION", "DEBITS", "CREDITS", "BALANCE" };

        float curX = TABLE_X;
        for (int i = 0; i < colWidths.length; i++) {
            cs.addRect(curX, pdfY, colWidths[i], HEADER_ROW_H);
            cs.stroke();

            float textY = topY + 14.0f;
            drawText(cs, fontBold, 10.0f, colTitles[i], curX + colWidths[i] / 2.0f, textY, true);
            curX += colWidths[i];
        }
    }

    private void renderTableRow(PDPageContentStream cs, Transaction tx, int sno, float topY) throws Exception {
        float pdfY = PAGE_H - topY - ROW_H;
        cs.setLineWidth(0.75f);
        cs.setStrokingColor(0, 0, 0);

        float[] colWidths = { COL_SNO_W, COL_DATE_W, COL_INST_W, COL_DESC_W, COL_DEBIT_W, COL_CREDIT_W, COL_BAL_W };
        float curX = TABLE_X;
        for (float w : colWidths) {
            cs.addRect(curX, pdfY, w, ROW_H);
            cs.stroke();
            curX += w;
        }

        float textY = topY + 15.0f;

        // 1. SNO (Center aligned)
        drawText(cs, fontRegular, 10.0f, String.valueOf(sno), TABLE_X + COL_SNO_W / 2.0f, textY, true);

        // 2. TRAN DATE (Center aligned)
        String txDateStr = formatDate(tx.date);
        drawText(cs, fontRegular, 10.0f, txDateStr, TABLE_X + COL_SNO_W + COL_DATE_W / 2.0f, textY, true);

        // 3. INST NO (Center aligned)
        String instNo = (tx.reference != null && tx.reference.matches("\\d{4,6}")) ? tx.reference : "";
        drawText(cs, fontRegular, 10.0f, instNo, TABLE_X + COL_SNO_W + COL_DATE_W + COL_INST_W / 2.0f, textY, true);

        // 4. DESCRIPTION (Center aligned in reference of description box)
        String desc = safe(tx.description, "");
        float maxDescW = COL_DESC_W - 8.0f;
        if (fontRegular.getStringWidth(desc) / 1000.0f * 10.0f > maxDescW) {
            while (desc.length() > 5 && fontRegular.getStringWidth(desc + "...") / 1000.0f * 10.0f > maxDescW) {
                desc = desc.substring(0, desc.length() - 1);
            }
            desc += "...";
        }
        float descCenterX = TABLE_X + COL_SNO_W + COL_DATE_W + COL_INST_W + COL_DESC_W / 2.0f;
        drawText(cs, fontRegular, 10.0f, desc, descCenterX, textY, true);

        // 5. DEBITS (Right aligned flush with column boundary without gap)
        String debitStr = tx.debit > 0 ? String.format(Locale.US, "%.2f", tx.debit) : "0.00";
        float debitRightX = TABLE_X + COL_SNO_W + COL_DATE_W + COL_INST_W + COL_DESC_W + COL_DEBIT_W;
        drawRightAlignedText(cs, fontRegular, 10.0f, debitStr, debitRightX, textY);

        // 6. CREDITS (Right aligned flush with column boundary without gap)
        String creditStr = tx.credit > 0 ? String.format(Locale.US, "%.2f", tx.credit) : "0.00";
        float creditRightX = TABLE_X + COL_SNO_W + COL_DATE_W + COL_INST_W + COL_DESC_W + COL_DEBIT_W + COL_CREDIT_W;
        drawRightAlignedText(cs, fontRegular, 10.0f, creditStr, creditRightX, textY);

        // 7. BALANCE (Right aligned flush with column boundary without gap, formatted with one space before Cr/Dr without dot)
        String balStr = String.format(Locale.US, "%.2f", Math.abs(tx.balance)) + (tx.balance >= 0 ? " Cr" : " Dr");
        float balRightX = TABLE_X + TABLE_W;
        drawRightAlignedText(cs, fontRegular, 10.0f, balStr, balRightX, textY);
    }

    private void renderDisclaimerBlock(PDPageContentStream cs, float topY) throws Exception {
        String[] lines = {
                "This is a system generated statement and does not require any signature.",
                "Any discrepancy in the account statement should be notified to the bank within period of 30 days of generation of statement. It will be treated that the",
                "entries,contents of this statement are checked and found correct by you, if no such complaint is made within the period stated above.",
                "Bank of India Helpline No: 022-40919191, Email id of the Complaint Cell: Headoffice.GOD@bankofindia.co.in",
                "**** Update your MOBILE NUMBER & EMAIL ID for getting SMS & EMAIL from BOI ****",
                "********* Toll free no. of our call center : 1800 220 229,1800 103 1906. *********",
                "******************** RELATIONSHIP BEYOND BANKING ********************"
        };

        float curY = topY;
        for (String line : lines) {
            drawText(cs, fontRegular, 10.0f, line, PAGE_W / 2.0f, curY, true);
            curY += 11.64f;
        }
    }

    private void drawText(PDPageContentStream cs, PDType1Font font, float fontSize, String text, float x, float topY, boolean centered) throws Exception {
        if (text == null) return;
        String clean = sanitize(text);
        if (clean.isEmpty()) return;

        float pdfY = PAGE_H - topY;
        float textW = font.getStringWidth(clean) / 1000.0f * fontSize;
        float drawX = centered ? (x - textW / 2.0f) : x;

        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(drawX, pdfY);
        cs.showText(clean);
        cs.endText();
    }

    private void drawRightAlignedText(PDPageContentStream cs, PDType1Font font, float fontSize, String text, float rightX, float topY) throws Exception {
        if (text == null) return;
        String clean = sanitize(text);
        if (clean.isEmpty()) return;

        float pdfY = PAGE_H - topY;
        float textW = font.getStringWidth(clean) / 1000.0f * fontSize;
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(rightX - textW, pdfY);
        cs.showText(clean);
        cs.endText();
    }

    private String sanitize(String text) {
        if (text == null) return "";
        return text.replace("\r", " ")
                   .replace("\n", " ")
                   .replace("\t", " ")
                   .replace("₹", "Rs. ")
                   .replace("\u00A0", " ")
                   .replace("–", "-")
                   .replace("—", "-")
                   .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                   .trim();
    }

    private String sanitizeLine(String line) {
        if (line == null) return "";
        return line.replaceAll("(?i)\\s*(?:CUSTID|CUST\\s*ID|A/C\\s*NO|ACCOUNT\\s*NO|TYPE|CURRENCY|IFSC(?:\\s*CODE)?|MICR(?:\\s*CODE)?|NOMINEE|JOINT\\s*HOLDER)\\s*[:\\-].*$", "")
                .trim();
    }

    private List<String> getCleanAddressLines(String rawAddress) {
        List<String> list = new ArrayList<>();
        if (rawAddress != null && !rawAddress.trim().isEmpty()) {
            String[] rawLines = rawAddress.split("\n");
            for (String l : rawLines) {
                String cleaned = sanitizeLine(l);
                if (!cleaned.isEmpty()) {
                    list.add(cleaned);
                }
            }
        }
        if (list.isEmpty()) {
            list.add("SHOP NO 6");
            list.add("SKY TOWER ROYAL MARKET");
            list.add("BHOPAL-462001");
            list.add("MADHYA PRADESH");
            list.add("INDIA");
        } else {
            boolean hasCountry = false;
            for (String l : list) {
                if (l.equalsIgnoreCase("INDIA")) {
                    hasCountry = true;
                    break;
                }
            }
            if (!hasCountry && list.size() < 5) {
                list.add("INDIA");
            }
        }
        return list;
    }

    private String safe(String val, String def) {
        return (val != null && !val.trim().isEmpty()) ? val.trim() : def;
    }

    private String formatDate(String isoOrStr) {
        if (isoOrStr == null || isoOrStr.trim().isEmpty()) return "01-01-2026";
        try {
            if (isoOrStr.contains("T")) {
                Instant inst = Instant.parse(isoOrStr);
                return DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneId.of("Asia/Kolkata")).format(inst);
            }
            if (isoOrStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                String[] p = isoOrStr.split("-");
                return p[2] + "-" + p[1] + "-" + p[0];
            }
            return sanitize(isoOrStr);
        } catch (Exception e) {
            return sanitize(isoOrStr);
        }
    }

    private String formatAmount(double amt) {
        return String.format(Locale.US, "%.2f", amt);
    }
}
