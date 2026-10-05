package com.user.pdfservice.template.au;

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
import java.awt.Color;
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
public class AuTemplate implements BankPdfTemplate {

    private static final float PAGE_W = 595.28f;
    private static final float PAGE_H = 841.89f;

    // Table Column Widths (Sum = 548.00 pt, Margin Left = 23.64 pt, Margin Right = 571.64 pt)
    private static final float TABLE_X = 23.64f;
    private static final float TABLE_W = 548.00f;
    private static final float COL_DATE_W = 49.32f;   // 9%
    private static final float COL_VDATE_W = 49.32f;  // 9%
    private static final float COL_DESC_W = 186.32f;  // 34%
    private static final float COL_REF_W = 98.64f;    // 18%
    private static final float COL_DEBIT_W = 54.80f;  // 10%
    private static final float COL_CREDIT_W = 54.80f; // 10%
    private static final float COL_BAL_W = 54.80f;    // 10%

    private static final float HEADER_ROW_H = 24.00f;

    // Brand Colors
    private static final Color BANNER_PURPLE = new Color(108, 43, 138);    // #6C2B8A AU Brand Purple
    private static final Color HEADER_BG = new Color(242, 242, 242);       // #F2F2F2
    private static final Color GRID_LINE = new Color(221, 221, 221);       // #DDDDDD
    private static final Color TEXT_DARK = new Color(34, 34, 34);          // #222222
    private static final Color TEXT_MUTED = new Color(102, 102, 102);      // #666666
    private static final Color AU_ORANGE = new Color(245, 130, 32);        // #F58220

    private final PDType1Font fontRegular = PDType1Font.HELVETICA;
    private final PDType1Font fontBold = PDType1Font.HELVETICA_BOLD;

    private PDImageXObject logoImage;

    @Override
    public byte[] generate(Statement statement) throws Exception {
        try (PDDocument doc = new PDDocument()) {
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

            PDDocumentInformation info = new PDDocumentInformation();
            info.setProducer("iText1.3.1 by lowagie.com (based on itext-paulo-154)");
            info.setCreator("AU Small Finance Bank Statement Generator");
            info.setTitle("ACCOUNT STATEMENT");
            Calendar now = Calendar.getInstance();
            info.setCreationDate(now);
            info.setModificationDate(now);
            doc.setDocumentInformation(info);

            loadLogo(doc);

            List<Transaction> txns = statement.transactions != null ? statement.transactions : new ArrayList<>();
            
            // Calculate total pages dynamically
            List<List<Transaction>> pages = paginateTransactions(txns);
            int totalPages = Math.max(1, pages.size());

            // Compute cumulative totals for summary
            double totalDebits = 0.0;
            double totalCredits = 0.0;
            for (Transaction t : txns) {
                if (t.debit > 0) totalDebits += t.debit;
                if (t.credit > 0) totalCredits += t.credit;
            }

            for (int p = 0; p < totalPages; p++) {
                List<Transaction> pageTxns = (p < pages.size()) ? pages.get(p) : new ArrayList<>();
                boolean isFirstPage = (p == 0);
                boolean isLastPage = (p == totalPages - 1);

                renderPage(doc, statement, pageTxns, p + 1, totalPages, isFirstPage, isLastPage, totalDebits, totalCredits);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private void loadLogo(PDDocument doc) {
        try {
            InputStream is = getClass().getResourceAsStream("/templates/au_logo.png");
            if (is != null) {
                BufferedImage bi = ImageIO.read(is);
                logoImage = LosslessFactory.createFromImage(doc, bi);
            }
        } catch (Exception ignored) {
        }
    }

    private List<List<Transaction>> paginateTransactions(List<Transaction> txns) {
        List<List<Transaction>> pages = new ArrayList<>();
        if (txns == null || txns.isEmpty()) {
            pages.add(new ArrayList<>());
            return pages;
        }

        int index = 0;
        int pageNum = 1;

        while (index < txns.size()) {
            float maxContentH = (pageNum == 1) ? 440.0f : 635.0f;
            int maxRowsAllowed = (pageNum == 1) ? 6 : 10;
            float currentH = 0.0f;
            List<Transaction> currentPage = new ArrayList<>();

            while (index < txns.size() && currentPage.size() < maxRowsAllowed) {
                Transaction tx = txns.get(index);
                float rowH = calculateRowHeight(tx);
                if (currentH + rowH > maxContentH && !currentPage.isEmpty()) {
                    break;
                }
                currentPage.add(tx);
                currentH += rowH;
                index++;
            }

            pages.add(currentPage);
            pageNum++;
        }

        return pages;
    }

    private float calculateRowHeight(Transaction tx) {
        int descLines = wrapNarration(tx.description != null ? tx.description : "", COL_DESC_W - 8.0f, fontRegular, 7.5f).size();
        int refLines = wrapReference(tx.reference != null ? tx.reference : "", COL_REF_W - 8.0f, fontRegular, 7.5f).size();
        int maxLines = Math.max(1, Math.max(descLines, refLines));
        return Math.max(22.0f, maxLines * 9.5f + 8.0f);
    }

    private void renderPage(PDDocument doc, Statement stmt, List<Transaction> pageTxns,
                            int pageNum, int totalPages, boolean isFirstPage, boolean isLastPage,
                            double totalDebits, double totalCredits) throws Exception {
        PDPage page = new PDPage(new PDRectangle(PAGE_W, PAGE_H));
        doc.addPage(page);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            // 1. Top Header Banner (on every page)
            renderTopBanner(cs);

            float currentTopY;
            if (isFirstPage) {
                // 2. Metadata Block (Page 1 only)
                currentTopY = renderPage1Metadata(cs, stmt);
            } else {
                currentTopY = 66.00f;
            }

            // 3. Table Header
            renderTableHeader(cs, currentTopY);
            currentTopY += HEADER_ROW_H;

            // 4. Data Rows
            for (Transaction tx : pageTxns) {
                float rowH = calculateRowHeight(tx);
                renderTableRow(cs, tx, currentTopY, rowH);
                currentTopY += rowH;
            }

            // 5. Total Row on Last Page
            if (isLastPage) {
                renderTotalRow(cs, currentTopY, totalDebits, totalCredits);
            }

            // 6. Page Footer (on every page)
            renderFooter(cs, pageNum, totalPages);
        }
    }

    private void renderTopBanner(PDPageContentStream cs) throws Exception {
        // Full bleed Purple Rectangle (height = 54 pt)
        cs.setNonStrokingColor(BANNER_PURPLE.getRed() / 255f, BANNER_PURPLE.getGreen() / 255f, BANNER_PURPLE.getBlue() / 255f);
        cs.addRect(0, PAGE_H - 54.00f, PAGE_W, 54.00f);
        cs.fill();

        // Title
        cs.setNonStrokingColor(1.0f, 1.0f, 1.0f);
        drawText(cs, fontBold, 17.0f, "ACCOUNT STATEMENT", TABLE_X, 33.0f, false);

        // Vector AU Small Finance Bank Logo (Top Right)
        float logoRightX = PAGE_W - TABLE_X;
        float logoCenterY = PAGE_H - 27.0f;
        float radius = 16.0f;
        float circleCenterX = logoRightX - 65.0f;

        // Orange Circle for AU
        cs.setNonStrokingColor(AU_ORANGE.getRed() / 255f, AU_ORANGE.getGreen() / 255f, AU_ORANGE.getBlue() / 255f);
        drawCircle(cs, circleCenterX, logoCenterY, radius);
        cs.fill();

        // White "AU" inside circle
        cs.setNonStrokingColor(1.0f, 1.0f, 1.0f);
        drawText(cs, fontBold, 14.0f, "AU", circleCenterX, 33.0f, true);

        // White side text: "AU \n SMALL \n FINANCE \n BANK"
        float textX = circleCenterX + radius + 5.0f;
        drawText(cs, fontBold, 6.5f, "AU", textX, 17.0f, false);
        drawText(cs, fontBold, 5.5f, "SMALL", textX, 24.5f, false);
        drawText(cs, fontBold, 5.5f, "FINANCE", textX, 32.0f, false);
        drawText(cs, fontBold, 5.5f, "BANK", textX, 39.5f, false);
    }

    private void drawCircle(PDPageContentStream cs, float cx, float cy, float r) throws Exception {
        float k = 0.552284749831f; // control point constant for circle
        cs.moveTo(cx - r, cy);
        cs.curveTo(cx - r, cy + k * r, cx - k * r, cy + r, cx, cy + r);
        cs.curveTo(cx + k * r, cy + r, cx + r, cy + k * r, cx + r, cy);
        cs.curveTo(cx + r, cy - k * r, cx + k * r, cy - r, cx, cy - r);
        cs.curveTo(cx - k * r, cy - r, cx - r, cy - k * r, cx - r, cy);
    }

    private float renderPage1Metadata(PDPageContentStream cs, Statement stmt) throws Exception {
        cs.setNonStrokingColor(TEXT_DARK.getRed() / 255f, TEXT_DARK.getGreen() / 255f, TEXT_DARK.getBlue() / 255f);

        float leftLabelX = TABLE_X;
        float leftColonX = TABLE_X + 80.00f;
        float leftValX = TABLE_X + 86.00f;

        float rightLabelX = 310.00f;
        float rightColonX = 412.00f;
        float rightValX = 418.00f;

        // Row 1: Name & Account Number (y=74.0f)
        float r1Y = 74.0f;
        drawText(cs, fontRegular, 8.5f, "Name", leftLabelX, r1Y, false);
        drawText(cs, fontBold, 8.5f, ":", leftColonX, r1Y, false);
        String name = safe(stmt.details.fullName, safe(stmt.details.name, "M S PHARMA"));
        drawText(cs, fontBold, 8.5f, name.toUpperCase(), leftValX, r1Y, false);

        drawText(cs, fontRegular, 8.5f, "Account Number", rightLabelX, r1Y, false);
        drawText(cs, fontBold, 8.5f, ":", rightColonX, r1Y, false);
        drawText(cs, fontBold, 8.5f, safe(stmt.details.accountNumber, "2502248474908993"), rightValX, r1Y, false);

        // Row 2: Customer ID & Account Type (y=89.0f)
        float r2Y = 89.0f;
        drawText(cs, fontRegular, 8.5f, "Customer ID", leftLabelX, r2Y, false);
        drawText(cs, fontBold, 8.5f, ":", leftColonX, r2Y, false);
        drawText(cs, fontBold, 8.5f, safe(stmt.details.customerRelNo, "39867327"), leftValX, r2Y, false);

        drawText(cs, fontRegular, 8.5f, "Account Type", rightLabelX, r2Y, false);
        drawText(cs, fontBold, 8.5f, ":", rightColonX, r2Y, false);
        drawText(cs, fontBold, 8.5f, safe(stmt.details.accountType, "AU Current Account-Basic"), rightValX, r2Y, false);

        // Row 3: Customer Type (y=104.0f)
        float r3Y = 104.0f;
        drawText(cs, fontRegular, 8.5f, "Customer Type", leftLabelX, r3Y, false);
        drawText(cs, fontBold, 8.5f, ":", leftColonX, r3Y, false);
        drawText(cs, fontBold, 8.5f, safe(stmt.details.customerType, "Sole Property"), leftValX, r3Y, false);

        // Row 4: Address & Branch (y=119.0f)
        float r4Y = 119.0f;
        drawText(cs, fontRegular, 8.5f, "Address", leftLabelX, r4Y, false);
        drawText(cs, fontBold, 8.5f, ":", leftColonX, r4Y, false);

        List<String> addrLines = getAddressLines(stmt);
        for (int i = 0; i < addrLines.size(); i++) {
            drawText(cs, fontBold, 8.5f, addrLines.get(i), leftValX, r4Y + i * 11.5f, false);
        }

        drawText(cs, fontRegular, 8.5f, "Branch", rightLabelX, r4Y, false);
        drawText(cs, fontBold, 8.5f, ":", rightColonX, r4Y, false);
        drawText(cs, fontBold, 8.5f, safe(stmt.details.branch, "Hamidiya Road Bhopal"), rightValX, r4Y, false);

        // IFSC (y=145.0f)
        float ifscY = 145.0f;
        drawText(cs, fontRegular, 8.5f, "IFSC", rightLabelX, ifscY, false);
        drawText(cs, fontBold, 8.5f, ":", rightColonX, ifscY, false);
        drawText(cs, fontBold, 8.5f, safe(stmt.details.ifsc, "AUBL0002484"), rightValX, ifscY, false);

        // Nominee (y=165.0f)
        float nomY = 165.0f;
        drawText(cs, fontRegular, 8.5f, "Nominee", rightLabelX, nomY, false);
        drawText(cs, fontBold, 8.5f, ":", rightColonX, nomY, false);
        drawText(cs, fontBold, 8.5f, safe(stmt.details.nomineeName, "Shabnam Qureshi"), rightValX, nomY, false);

        // Row 5: Statement Date & Opening Balance (y=205.0f)
        float r5Y = 205.0f;
        drawText(cs, fontRegular, 8.5f, "Statement Date", leftLabelX, r5Y, false);
        drawText(cs, fontBold, 8.5f, ":", leftColonX, r5Y, false);
        drawText(cs, fontBold, 8.5f, formatDate(stmt.meta.generatedAt), leftValX, r5Y, false);

        drawText(cs, fontRegular, 8.5f, "Opening Balance", rightLabelX, r5Y, false);
        drawText(cs, fontBold, 8.5f, ":", rightColonX, r5Y, false);
        String opBalStr = formatCurrency(stmt.details.startingBalance);
        drawText(cs, fontBold, 8.5f, opBalStr, rightValX, r5Y, false);

        // Row 6: Statement Period & Closing Balance (y=220.0f)
        float r6Y = 220.0f;
        drawText(cs, fontRegular, 8.5f, "Statement Period", leftLabelX, r6Y, false);
        drawText(cs, fontBold, 8.5f, ":", leftColonX, r6Y, false);
        String periodStr = formatDate(stmt.meta.statementPeriodStart) + " to " + formatDate(stmt.meta.statementPeriodEnd);
        drawText(cs, fontBold, 8.5f, periodStr, leftValX, r6Y, false);

        drawText(cs, fontRegular, 8.5f, "Closing Balance", rightLabelX, r6Y, false);
        drawText(cs, fontBold, 8.5f, ":", rightColonX, r6Y, false);
        double clBal = stmt.details.startingBalance;
        if (stmt.transactions != null && !stmt.transactions.isEmpty()) {
            clBal = stmt.transactions.get(stmt.transactions.size() - 1).balance;
        }
        String clBalStr = formatCurrency(clBal);
        drawText(cs, fontBold, 8.5f, clBalStr, rightValX, r6Y, false);

        return 245.00f; // Table Header starts here on Page 1
    }

    private void renderTableHeader(PDPageContentStream cs, float topY) throws Exception {
        float pdfY = PAGE_H - topY - HEADER_ROW_H;

        // Background Fill
        cs.setNonStrokingColor(HEADER_BG.getRed() / 255f, HEADER_BG.getGreen() / 255f, HEADER_BG.getBlue() / 255f);
        cs.addRect(TABLE_X, pdfY, TABLE_W, HEADER_ROW_H);
        cs.fill();

        // Stroke Lines
        cs.setStrokingColor(GRID_LINE.getRed() / 255f, GRID_LINE.getGreen() / 255f, GRID_LINE.getBlue() / 255f);
        cs.setLineWidth(0.50f);

        float[] colWidths = { COL_DATE_W, COL_VDATE_W, COL_DESC_W, COL_REF_W, COL_DEBIT_W, COL_CREDIT_W, COL_BAL_W };
        String[] titles = { "Transaction\nDate", "Value Date", "Description/Narration", "Cheque/\nReference No.", "Debit (INR)", "Credit (INR)", "Balance (INR)" };

        float curX = TABLE_X;
        for (int i = 0; i < colWidths.length; i++) {
            cs.addRect(curX, pdfY, colWidths[i], HEADER_ROW_H);
            cs.stroke();

            cs.setNonStrokingColor(TEXT_DARK.getRed() / 255f, TEXT_DARK.getGreen() / 255f, TEXT_DARK.getBlue() / 255f);
            String title = titles[i];

            // Right align monetary headers, left/center align other headers
            if (i >= 4) {
                drawRightAlignedText(cs, fontBold, 8.0f, title, curX + colWidths[i] - 4.0f, topY + 14.5f);
            } else if (title.contains("\n")) {
                String[] lines = title.split("\n");
                drawText(cs, fontBold, 7.5f, lines[0], curX + 4.0f, topY + 9.5f, false);
                drawText(cs, fontBold, 7.5f, lines[1], curX + 4.0f, topY + 18.5f, false);
            } else {
                drawText(cs, fontBold, 8.0f, title, curX + 4.0f, topY + 14.5f, false);
            }

            curX += colWidths[i];
        }
    }

    private void renderTableRow(PDPageContentStream cs, Transaction tx, float topY, float rowH) throws Exception {
        float pdfY = PAGE_H - topY - rowH;

        // Grid Borders
        cs.setStrokingColor(GRID_LINE.getRed() / 255f, GRID_LINE.getGreen() / 255f, GRID_LINE.getBlue() / 255f);
        cs.setLineWidth(0.50f);

        float[] colWidths = { COL_DATE_W, COL_VDATE_W, COL_DESC_W, COL_REF_W, COL_DEBIT_W, COL_CREDIT_W, COL_BAL_W };
        float curX = TABLE_X;
        for (float w : colWidths) {
            cs.addRect(curX, pdfY, w, rowH);
            cs.stroke();
            curX += w;
        }

        cs.setNonStrokingColor(TEXT_DARK.getRed() / 255f, TEXT_DARK.getGreen() / 255f, TEXT_DARK.getBlue() / 255f);

        // 1. Transaction Date (Left aligned)
        String txDate = formatDate(tx.date);
        drawText(cs, fontRegular, 7.5f, txDate, TABLE_X + 4.0f, topY + 11.0f, false);

        // 2. Value Date (Left aligned)
        String valDate = tx.valueDate != null ? formatDate(tx.valueDate) : txDate;
        drawText(cs, fontRegular, 7.5f, valDate, TABLE_X + COL_DATE_W + 4.0f, topY + 11.0f, false);

        // 3. Description / Narration (Wrapped, left aligned)
        List<String> descLines = wrapNarration(safe(tx.description, ""), COL_DESC_W - 8.0f, fontRegular, 7.5f);
        float descX = TABLE_X + COL_DATE_W + COL_VDATE_W + 4.0f;
        for (int i = 0; i < descLines.size(); i++) {
            drawText(cs, fontRegular, 7.5f, descLines.get(i), descX, topY + 11.0f + i * 9.5f, false);
        }

        // 4. Cheque / Reference No. (Wrapped, left aligned)
        List<String> refLines = wrapReference(safe(tx.reference, ""), COL_REF_W - 8.0f, fontRegular, 7.5f);
        float refX = TABLE_X + COL_DATE_W + COL_VDATE_W + COL_DESC_W + 4.0f;
        for (int i = 0; i < refLines.size(); i++) {
            drawText(cs, fontRegular, 7.5f, refLines.get(i), refX, topY + 11.0f + i * 9.5f, false);
        }

        // 5. Debit (INR) (Right aligned)
        String debitStr = tx.debit > 0 ? formatCurrency(tx.debit) : "-";
        float debitRightX = TABLE_X + COL_DATE_W + COL_VDATE_W + COL_DESC_W + COL_REF_W + COL_DEBIT_W - 4.0f;
        drawRightAlignedText(cs, fontRegular, 7.5f, debitStr, debitRightX, topY + 11.0f);

        // 6. Credit (INR) (Right aligned)
        String creditStr = tx.credit > 0 ? formatCurrency(tx.credit) : "-";
        float creditRightX = TABLE_X + COL_DATE_W + COL_VDATE_W + COL_DESC_W + COL_REF_W + COL_DEBIT_W + COL_CREDIT_W - 4.0f;
        drawRightAlignedText(cs, fontRegular, 7.5f, creditStr, creditRightX, topY + 11.0f);

        // 7. Balance (INR) (Right aligned)
        String balStr = formatCurrency(tx.balance);
        float balRightX = TABLE_X + TABLE_W - 4.0f;
        drawRightAlignedText(cs, fontRegular, 7.5f, balStr, balRightX, topY + 11.0f);
    }

    private void renderTotalRow(PDPageContentStream cs, float topY, double totalDebits, double totalCredits) throws Exception {
        float rowH = 20.00f;
        float pdfY = PAGE_H - topY - rowH;

        cs.setStrokingColor(GRID_LINE.getRed() / 255f, GRID_LINE.getGreen() / 255f, GRID_LINE.getBlue() / 255f);
        cs.setLineWidth(0.50f);

        // Span 1-4 for "Total"
        float span14W = COL_DATE_W + COL_VDATE_W + COL_DESC_W + COL_REF_W;
        cs.addRect(TABLE_X, pdfY, span14W, rowH);
        cs.stroke();

        // Debit Box
        float debitX = TABLE_X + span14W;
        cs.addRect(debitX, pdfY, COL_DEBIT_W, rowH);
        cs.stroke();

        // Credit Box
        float creditX = debitX + COL_DEBIT_W;
        cs.addRect(creditX, pdfY, COL_CREDIT_W, rowH);
        cs.stroke();

        // Balance Box
        float balX = creditX + COL_CREDIT_W;
        cs.addRect(balX, pdfY, COL_BAL_W, rowH);
        cs.stroke();

        // Text
        cs.setNonStrokingColor(TEXT_DARK.getRed() / 255f, TEXT_DARK.getGreen() / 255f, TEXT_DARK.getBlue() / 255f);
        drawText(cs, fontBold, 8.5f, "Total", TABLE_X + span14W - 16.0f, topY + 13.5f, true);

        String debTotalStr = formatCurrency(totalDebits);
        drawRightAlignedText(cs, fontBold, 7.5f, debTotalStr, debitX + COL_DEBIT_W - 4.0f, topY + 13.5f);

        String credTotalStr = formatCurrency(totalCredits);
        drawRightAlignedText(cs, fontBold, 7.5f, credTotalStr, creditX + COL_CREDIT_W - 4.0f, topY + 13.5f);
    }

    private void renderFooter(PDPageContentStream cs, int pageNum, int totalPages) throws Exception {
        // Line 1: Auto generated text & page number (topY = 726.0f)
        cs.setNonStrokingColor(TEXT_DARK.getRed() / 255f, TEXT_DARK.getGreen() / 255f, TEXT_DARK.getBlue() / 255f);
        drawText(cs, fontRegular, 7.5f, "This is an auto generated statement and requires no signature", TABLE_X, 726.0f, false);
        drawRightAlignedText(cs, fontRegular, 8.0f, "Page " + pageNum + " of " + totalPages, TABLE_X + TABLE_W, 726.0f);

        // Divider Line (topY = 734.0f)
        cs.setStrokingColor(GRID_LINE.getRed() / 255f, GRID_LINE.getGreen() / 255f, GRID_LINE.getBlue() / 255f);
        cs.setLineWidth(0.50f);
        cs.moveTo(TABLE_X, PAGE_H - 734.0f);
        cs.lineTo(TABLE_X + TABLE_W, PAGE_H - 734.0f);
        cs.stroke();

        // Line 2: Disclaimer
        cs.setNonStrokingColor(BANNER_PURPLE.getRed() / 255f, BANNER_PURPLE.getGreen() / 255f, BANNER_PURPLE.getBlue() / 255f);
        drawText(cs, fontRegular, 7.5f, "Please review the information provided in the statement. In case of any discrepancy, please inform the Bank immediately", TABLE_X, 746.0f, false);

        // 5-Column Contact Info
        float colW = TABLE_W / 5.0f;
        String[] titles = { "Call us at", "Website", "Email", "Write to us at", "Follow us on" };
        String[] values = { "1800 1200 1200", "www.aubank.in", "customercare@aubank.in", "Reg. office address", "Facebook/Twitter" };

        for (int i = 0; i < 5; i++) {
            float cX = TABLE_X + i * colW;
            cs.setNonStrokingColor(TEXT_MUTED.getRed() / 255f, TEXT_MUTED.getGreen() / 255f, TEXT_MUTED.getBlue() / 255f);
            drawText(cs, fontRegular, 7.0f, titles[i], cX, 762.0f, false);
            cs.setNonStrokingColor(TEXT_DARK.getRed() / 255f, TEXT_DARK.getGreen() / 255f, TEXT_DARK.getBlue() / 255f);
            drawText(cs, fontRegular, 7.0f, values[i], cX, 772.0f, false);
        }

        // Bottom Full-Bleed Purple Strip (height = 28.0f)
        cs.setNonStrokingColor(BANNER_PURPLE.getRed() / 255f, BANNER_PURPLE.getGreen() / 255f, BANNER_PURPLE.getBlue() / 255f);
        cs.addRect(0, 0, PAGE_W, 28.0f);
        cs.fill();

        // Registered Office Text (Two lines, white, centered)
        cs.setNonStrokingColor(1.0f, 1.0f, 1.0f);
        drawText(cs, fontRegular, 6.5f, "19A, DHULESHWAR GARDEN, AJMER ROAD, JAIPUR – 302001, RAJASTHAN (INDIA)", PAGE_W / 2.0f, 825.0f, true);
        drawText(cs, fontRegular, 6.5f, "Ph.: +91 141 4110060/61, TOLL-FREE: 1800 1200 1200", PAGE_W / 2.0f, 834.0f, true);
    }

    private List<String> getAddressLines(Statement stmt) {
        List<String> list = new ArrayList<>();
        if (stmt.details != null && stmt.details.address != null && !stmt.details.address.trim().isEmpty()) {
            String[] raw = stmt.details.address.split("[\\r\\n]+");
            for (String l : raw) {
                String clean = sanitize(l);
                if (!clean.isEmpty()) list.add(clean);
            }
        }
        if (list.isEmpty()) {
            list.add("Shop No/plot No Shop No G-70");
            list.add("Ground, Floor Vardhman City Plaza");
            list.add("Dawa, Bazar, Hamidia Road Tehsil");
            list.add("Huzur");
            list.add("Bhopal - 462001, Madhya Pradesh -");
            list.add("India");
        }
        return list;
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

    private List<String> wrapNarration(String rawText, float maxWidth, PDType1Font font, float fontSize) {
        List<String> lines = new ArrayList<>();
        if (rawText == null || rawText.trim().isEmpty()) return lines;

        String cleanText = rawText.replace("\r\n", " ")
                                  .replace("\r", " ")
                                  .replace("\n", " ")
                                  .replace("\t", " ")
                                  .replace("₹", "Rs. ")
                                  .replace("\u00A0", " ")
                                  .replace("–", "-")
                                  .replace("—", "-")
                                  .replaceAll("\\s+", " ")
                                  .trim();

        if (cleanText.isEmpty()) return lines;

        // Split by words, slashes, or hyphens
        String[] tokens = cleanText.split("(?<=/)|(?=/)|(?<=-)|(?=-)|\\s+");
        StringBuilder cur = new StringBuilder();

        for (String t : tokens) {
            if (t.trim().isEmpty()) continue;
            try {
                String test = cur.length() == 0 ? t : cur + (cur.toString().endsWith("/") || t.startsWith("/") ? "" : " ") + t;
                if (font.getStringWidth(test) / 1000.0f * fontSize <= maxWidth) {
                    cur = new StringBuilder(test);
                } else {
                    if (cur.length() > 0) {
                        lines.add(cur.toString());
                    }
                    // If token itself is longer than maxWidth, break by length
                    if (font.getStringWidth(t) / 1000.0f * fontSize > maxWidth) {
                        for (int i = 0; i < t.length(); i += 24) {
                            int end = Math.min(t.length(), i + 24);
                            lines.add(t.substring(i, end));
                        }
                        cur = new StringBuilder();
                    } else {
                        cur = new StringBuilder(t);
                    }
                }
            } catch (Exception e) {
                cur.append(t);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    private List<String> wrapReference(String rawText, float maxWidth, PDType1Font font, float fontSize) {
        List<String> lines = new ArrayList<>();
        if (rawText == null || rawText.trim().isEmpty()) return lines;

        String cleanText = rawText.replace("\r\n", " ")
                                  .replace("\r", " ")
                                  .replace("\n", " ")
                                  .replace("\t", " ")
                                  .replace("₹", "Rs. ")
                                  .replace("\u00A0", " ")
                                  .replace("–", "-")
                                  .replace("—", "-")
                                  .replaceAll("\\s+", " ")
                                  .trim();

        if (cleanText.isEmpty()) return lines;

        // If string length > 18 and no spaces, wrap in chunks of 18
        if (!cleanText.contains(" ") && cleanText.length() > 18) {
            for (int i = 0; i < cleanText.length(); i += 18) {
                int end = Math.min(cleanText.length(), i + 18);
                lines.add(cleanText.substring(i, end));
            }
            return lines;
        }

        return wrapNarration(cleanText, maxWidth, font, fontSize);
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

    private String safe(String val, String def) {
        return (val != null && !val.trim().isEmpty()) ? val.trim() : def;
    }

    private String formatDate(String isoOrStr) {
        if (isoOrStr == null || isoOrStr.trim().isEmpty()) return "01 Jan 2026";
        try {
            if (isoOrStr.contains("T")) {
                Instant inst = Instant.parse(isoOrStr);
                return DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US).withZone(ZoneId.of("Asia/Kolkata")).format(inst);
            }
            if (isoOrStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                String[] p = isoOrStr.split("-");
                int year = Integer.parseInt(p[0]);
                int month = Integer.parseInt(p[1]);
                int day = Integer.parseInt(p[2]);
                String[] months = { "", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
                return String.format(Locale.US, "%02d %s %04d", day, months[month], year);
            }
            return sanitize(isoOrStr);
        } catch (Exception e) {
            return sanitize(isoOrStr);
        }
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.US, "%,.2f", amount);
    }
}

