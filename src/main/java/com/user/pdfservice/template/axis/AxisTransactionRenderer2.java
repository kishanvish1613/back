
package com.user.pdfservice.template.axis;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.Transaction;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

public final class AxisTransactionRenderer2 {

    // ================= COLUMN RIGHT EDGES =================
    private static final float X_DATE = 38.54f;
    private static final float X_CHQ = 91.73f;
    private static final float X_PART = 132.14f;
    private static final float R_DEBIT = 381.18f;
    private static final float R_CREDIT = 443.94f;
    private static final float R_BAL = 532.85f;
    private static final float X_INIT = 539.17f;
    private static final float X_INIT_TEXT = 537.80f; // "Init."
    private static final float X_BR = 540.93f;

    // ================= PAGE GEOMETRY =================
    private static final float HEADER_Y = 580f;
    private static final float HEADER_TEXT_Y = 591f;
    private static final float HEADER_H = 22f;

    private static final float OPEN_Y = 560f;
    private static final float OPEN_TEXT_Y = 562f;
    private static final float OPEN_H = 20f;

    private static final float PAGE1_TXN_START_Y = 538f;
    private static final float PAGE_TXN_START_Y = 784f;
    private static final float PAGE_BOTTOM_Y = 72f;

    private static final float FOOTER_LEFT_X = 38f;
    private static final float FOOTER_FONT_SIZE = 9f;
    private static final float FOOTER_LINE_GAP = 9f;
    private static final float FOOTER_BOTTOM_MARGIN = 60f;

    private static final float LEGEND_LEFT_X = 38f;
    private static final float LEGEND_LINE_GAP = 13.5f;

    private static final float END_BLOCK_HEIGHT = 30f;

    private static final String[] FOOTER_PARAGRAPHS = new String[] {
            "Unless the constituent notifies the bank immediately of any discrepancy found by him/her in this statement of Account, it will be taken that he/she has found the account correct.",

            "The closing balance as shown/displayed includes not only the credit balance and / or overdraft limit, but also funds which are under clearing. It excludes the amount marked as lien, if any. Hence the closing balance displayed may not be the effective available balance. For any further clarifications, please contact the Branch.",

            "We would like to reiterate that, as a policy, Axis Bank never asks you to share, disclose, or revalidate your login Id, password or debit card number through emails OR phone call. Further, Axis Bank shall not be liable for any losses arising from you sharing/disclosing of your login id, password and debit card number to anyone. Please co-operate by forwarding all such suspicious/spam emails, if received by you, to customer.service@axisbank.com",

            "With effect from 1st August 2016, the replacement charges for Debit card and ATM card applicable on Current accounts have been revised. To know more about the applicable charges, please visit www.axisbank.com",

            "Deposit Insurance and Credit Guarantee Corporation (DICGC) insurance cover is applicable in all Banks' deposits, such as savings, current, fixed, recurring etc* up to maximum amount of Rs 5 Lakh including principal & interest both* (* or exceptions and details please refer www.dicgc.org.in)",

            "In compliance with regulatory guidelines, the non-CTS cheque books attached to the accounts would be destroyed in banks core banking System. Thus, Non CTS cheques will not be valid for CASH, Clearing and Transfer transactions",

            "To ensure you never miss any critical communication from us, it is important that your latest / correct mobile number and email ID are updated in our records. Kindly visit your nearest Axis Bank Loan Centre /Branch / Agri area office for updating your latest / correct mobile number and email ID in our records. You can also update your email ID using Internet Banking & Mobile Banking App, open",
            "REGISTERED OFFICE - AXIS BANK LTD,TRISHUL,Opp. Samartheswar Temple, Near Law Garden, Ellisbridge, Ahmedabad . 380006."
    };

    private static final String[] LEGENDS = {
            "ICONN\t\t\t\t-\t\tTransaction trough Internet Banking",
            "VMT-ICON\t\t\t-\t\tVisa Money Transfer through Internet Banking",
            "AUTOSWEEP\t\t\t-\t\tTransfer to linked fixed deposit",
            "REV SWEEP\t\t\t-\t\tInterest on Linked fixed Deposit",
            "SWEEP TRF\t\t\t-\t\tTransfer from Linked Fixed Deposit / Account",
            "VMT\t\t\t\t\t-\t\tVisa Money Transfer through ATM",
            "CWDR\t\t\t\t-\t\tCash Withdrawal through ATM",
            "PUR\t\t\t\t\t-\t\tPOS purchase",
            "TIP/ SCG\t\t\t-\t\tSurcharge on usage of debit card at pumps/railway ticket purchase or hotel tips",
            "RATE.DIFF\t\t\t-\t\tDifference in rates on usage of card internationally",
            "CLG\t\t\t\t\t-\t\tCheque Clearing Transaction",
            "EDC\t\t\t\t\t-\t\tCredit transaction through EDC Machine",
            "SETU \t\t\t\t-\t\tSeamless electronic fund transfer through AXIS Bank",
            "Int.pd\t\t\t\t-\t\tInterest paid to customer",
            "Int.Coll\t\t\t-\t\tInterest collected from the customer"
    };

    private final Statement stmt;
    private final List<Transaction> txns;

    public AxisTransactionRenderer2(Statement stmt) {
        this.stmt = stmt;
        this.txns = stmt.transactions;
    }

    // ===================================================
    // ENTRY POINT
    // ===================================================
    public void render(Document doc, PdfWriter writer, Fonts fonts) {

        PdfContentByte cb = writer.getDirectContent();

        BaseFont bold = fonts.timesBold.getBaseFont();
        BaseFont body = fonts.timesRoman.getBaseFont();

        float y = PAGE1_TXN_START_Y + 22f;

        // ---------- HEADER (ONLY ON FIRST PAGE) ----------
        drawHeaderGrid(cb);
        drawHeaderText(cb, bold);
        drawOpeningGrid(cb);
        drawOpeningBalance(cb, bold);

        double totalDebit = 0;
        double totalCredit = 0;
        double closingBalance = 0;
        float rowHeight = 0;

        for (Transaction tx : txns) {

            if (y < PAGE_BOTTOM_Y) {
                doc.newPage();
                y = PAGE_TXN_START_Y;
            }

            rowHeight = computeTxnRowHeight(body, 9f, tx.description);

            float bottomY = y - rowHeight;
            drawTxnGrid(cb, bottomY, rowHeight);

            y = bottomY;

            drawTransactionText(
                    cb,
                    body,
                    y,
                    formatDate(tx.date),
                    tx.description,
                    formatAmount(tx.debit),
                    formatAmount(tx.credit),
                    formatAmount(tx.balance),
                    "821");

            totalDebit += tx.debit;
            totalCredit += tx.credit;
            closingBalance = tx.balance;
        }

        // ---------- TOTALS ----------
        final float SUMMARY_REQUIRED = 46f + rowHeight;

        // Only affects summary, not transactions
        if (y < PAGE_BOTTOM_Y + SUMMARY_REQUIRED) {
            doc.newPage();
            y = PAGE_TXN_START_Y;
        }

        y -= 2f + rowHeight;
        drawTransactionTotalRow(cb, bold, y,
                formatAmount(totalDebit),
                formatAmount(totalCredit));

        y -= 20f;
        drawClosingBalanceRow(cb, bold, y, formatAmount(closingBalance));

        y -= 44f;
        final float footer = y;

        float MOVE_UP = 582f;
        // y = drawFooterSpacer(doc, cb, fonts, y);
        drawFooterAtY(doc, cb, fonts.timesRoman.getBaseFont(), footer + 2f, stmt.details.branchAddress,
        stmt.details.branchPhoneNo);
        float y3 = drawLegendAxisStyle(doc, cb, fonts, y + MOVE_UP);


        // FIX
        // if(y + MOVE_UP  > PAGE_TXN_START_Y){
        //     y = PAGE_BOTTOM_Y + MOVE_UP - PAGE_TXN_START_Y;
        // }


        // yAfterFooter -= 9f;
        // y = drawBranchAddress(
        //         doc,
        //         cb,
        //         fonts.timesRoman.getBaseFont(),
        //         yAfterFooter,
        //         stmt.details.branchAddress);


        y3 -= 13f;
        y = drawEndOfStatement(
                doc,
                cb,
                fonts,
                y3,
                "192.168.114.155");
    }

    // ===================================================
    // TRANSACTION ROW
    // ===================================================
    private void drawTxnGrid(PdfContentByte cb, float y, float h) {

        cb.setLineWidth(0.5f);
        cb.rectangle(36f, y, 47.07f, h);
        cb.rectangle(83.07f, y, 47.07f, h);
        cb.rectangle(130.14f, y, 188.28f, h);
        cb.rectangle(318.42f, y, 62.76f, h);
        cb.rectangle(381.18f, y, 62.76f, h);
        cb.rectangle(443.94f, y, 88.91f, h);
        cb.rectangle(532.85f, y, 26.15f, h);
        cb.stroke();
    }

    // ===================================================
    // TOTAL & CLOSING
    // ===================================================
    private void drawTransactionTotalRow(
            PdfContentByte cb,
            BaseFont f,
            float y,
            String debit,
            String credit) {

        float h = 24f;

        cb.rectangle(36f, y, 94.14f, h);
        cb.rectangle(130.14f, y, 188.28f, h);
        cb.rectangle(318.42f, y, 62.76f, h);
        cb.rectangle(381.18f, y, 62.76f, h);
        cb.rectangle(443.94f, y, 115.06f, h);
        cb.stroke();

        text(cb, f, 9, 132.14f, y + 2, "TRANSACTION TOTAL");
        drawRightAligned(cb, f, 10, 381.18f, y + 2, debit);
        drawRightAligned(cb, f, 9, 435.94f, y + 2, credit);

    }

    private void drawRightAligned(
            PdfContentByte cb,
            BaseFont font,
            float fontSize,
            float rightEdgeX,
            float y,
            String text) {

        float w = font.getWidthPoint(text, fontSize);
        cb.beginText();
        cb.setFontAndSize(font, fontSize);
        cb.setTextMatrix(rightEdgeX - w - 2f, y);
        cb.showText(text);
        cb.endText();
    }

    private void drawClosingBalanceRow(
            PdfContentByte cb,
            BaseFont f,
            float y,
            String balance) {

        float h = 20f;

        cb.rectangle(36f, y, 94.14f, h);
        cb.rectangle(130.14f, y, 313.8f, h);
        cb.rectangle(443.94f, y, 88.91f, h);
        cb.rectangle(532.85f, y, 26.15f, h);
        cb.stroke();

        text(cb, f, 9, 132.14f, y + 2, "CLOSING BALANCE");
        right(cb, f, 10, R_BAL, y + 2, balance);
    }

    // ===================================================
    // HEADER & OPENING
    // ===================================================
    private void drawHeaderGrid(PdfContentByte cb) {
        cb.setLineWidth(0.5f);
        cb.rectangle(36f, HEADER_Y, 47.07f, HEADER_H);
        cb.rectangle(83.07f, HEADER_Y, 47.07f, HEADER_H);
        cb.rectangle(130.14f, HEADER_Y, 188.28f, HEADER_H);
        cb.rectangle(318.42f, HEADER_Y, 62.76f, HEADER_H);
        cb.rectangle(381.18f, HEADER_Y, 62.76f, HEADER_H);
        cb.rectangle(443.94f, HEADER_Y, 88.91f, HEADER_H);
        cb.rectangle(532.85f, HEADER_Y, 26.15f, HEADER_H);
        cb.stroke();
    }

    private void drawHeaderText(PdfContentByte cb, BaseFont f) {
        text(cb, f, 9, 39.66f, HEADER_TEXT_Y, "Tran Date");
        text(cb, f, 9, X_CHQ, HEADER_TEXT_Y, "Chq No");
        text(cb, f, 9, 202.78f, HEADER_TEXT_Y, "Particulars");
        text(cb, f, 9, 339.30f, HEADER_TEXT_Y, "Debit");
        text(cb, f, 9, 400.06f, HEADER_TEXT_Y, "Credit");
        text(cb, f, 9, 473.14f, HEADER_TEXT_Y, "Balance");

        cb.beginText();
        cb.setFontAndSize(f, 9);
        cb.setTextMatrix(X_INIT_TEXT, HEADER_TEXT_Y);
        cb.showText("Init.");
        cb.setTextMatrix(X_BR, HEADER_TEXT_Y - 9);
        cb.showText("Br");
        cb.endText();
    }

    private void drawOpeningGrid(PdfContentByte cb) {
        cb.rectangle(36f, OPEN_Y, 94.14f, OPEN_H);
        cb.rectangle(130.14f, OPEN_Y, 313.8f, OPEN_H);
        cb.rectangle(443.94f, OPEN_Y, 88.91f, OPEN_H);
        cb.rectangle(532.85f, OPEN_Y, 26.15f, OPEN_H);
        cb.stroke();
    }

    private void drawOpeningBalance(PdfContentByte cb, BaseFont f) {
        text(cb, f, 9, 132.14f, OPEN_TEXT_Y, "OPENING BALANCE");
        right(cb, f, 10, R_BAL, OPEN_TEXT_Y, "50031.19");
    }

    // ===================================================
    // LOW LEVEL
    // ===================================================
    private void right(PdfContentByte cb, BaseFont f, float size, float rightX, float y, String v) {
        float w = f.getWidthPoint(v, size);
        text(cb, f, size, rightX - w - 2f, y, v);
    }

    private void text(PdfContentByte cb, BaseFont f, float size, float x, float y, String v) {
        cb.beginText();
        cb.setFontAndSize(f, size);
        cb.setTextMatrix(x, y);
        cb.showText(v == null ? "" : v);
        cb.endText();
    }

    private static String[] splitIntoMax3Lines(BaseFont f, float s, float w, String t) {
        String[] r = { "", "", "" };
        if (t == null)
            return r;
        String[] words = t.split(" ");
        StringBuilder line = new StringBuilder();
        int i = 0;

        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (f.getWidthPoint(test, s) <= w) {
                line.setLength(0);
                line.append(test);
            } else {
                r[i++] = line.toString();
                if (i == 3)
                    return r;
                line.setLength(0);
                line.append(word);
            }
        }
        if (i < 3)
            r[i] = line.toString();
        return r;
    }

    private static String formatDate(String d) {
        try {
            return DateTimeFormatter.ofPattern("dd-MM-yyyy")
                    .withZone(ZoneOffset.UTC)
                    .format(Instant.parse(d));
        } catch (Exception e) {
            return d;
        }
    }

    private static String formatAmount(double v) {
        return v == 0 ? "" : String.format(Locale.ENGLISH, "%.2f", v);
    }

    private float drawFooter(
            Document doc,
            PdfContentByte cb,
            BaseFont font,
            float startY) {

        float y = startY;

        for (String para : FOOTER_PARAGRAPHS) {

            String[] lines = splitIntoLines(font, FOOTER_FONT_SIZE, 520f, para);

            if (y < FOOTER_BOTTOM_MARGIN) {
                doc.newPage();
                y = PAGE_TXN_START_Y;
            }

            cb.beginText();
            cb.setFontAndSize(font, FOOTER_FONT_SIZE);

            for (String line : lines) {
                cb.setTextMatrix(FOOTER_LEFT_X, y);
                cb.showText(line);
                y -= FOOTER_LINE_GAP;
            }

            cb.endText();

            // paragraph gap
            y -= 4f;
        }

        return y;
    }

    private String[] splitIntoLines(
            BaseFont font,
            float fontSize,
            float maxWidth,
            String text) {

        List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();

        for (String word : text.split(" ")) {
            String test = line.length() == 0 ? word : line + " " + word;

            if (font.getWidthPoint(test, fontSize) <= maxWidth) {
                line.setLength(0);
                line.append(test);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }

        if (line.length() > 0) {
            lines.add(line.toString());
        }

        return lines.toArray(new String[0]);
    }

    private float drawBranchAddress(
            Document doc,
            PdfContentByte cb,
            BaseFont font,
            float startY,
            String branchAddress) {

        if (branchAddress == null || branchAddress.isEmpty()) {
            return startY;
        }

        // Ensure space on page
        if (startY < FOOTER_BOTTOM_MARGIN) {
            doc.newPage();
            startY = PAGE_TXN_START_Y;
        }

        String text = "BRANCH ADDRESS - " +
                branchAddress +
                " TEL:" + stmt.details.branchPhoneNo +
                " FAX:4285393";

        float x = 38f;
        float maxWidth = 520f;
        float leading = 9f;
        float y = startY;

        List<String> lines = wrapText(font, 9f, maxWidth, text);

        cb.beginText();
        cb.setFontAndSize(font, 9f);

        for (String line : lines) {

            if (y < 72f) { // bottom margin
                cb.endText();
                doc.newPage();
                y = PAGE_TXN_START_Y;
                cb.beginText();
                cb.setFontAndSize(font, 9f);
            }

            cb.setTextMatrix(x, y);
            cb.showText(line);

            y -= leading;
        }

        cb.endText();

        return y;
    }

    private float drawLegends(
            Document doc,
            PdfContentByte cb,
            Fonts fonts,
            float startY) {

        BaseFont bold = fonts.timesBold.getBaseFont();
        BaseFont body = fonts.timesRoman.getBaseFont();
        BaseFont helvetica = fonts.helvetica.getBaseFont();

        float y = startY;

        // Ensure space
        if (y < FOOTER_BOTTOM_MARGIN + 200) {
            doc.newPage();
            y = PAGE_TXN_START_Y;
        }

        cb.beginText();

        // ---- Legends title ----
        cb.setFontAndSize(bold, 9f);
        cb.setTextMatrix(LEGEND_LEFT_X, y);
        cb.showText("Legends :");
        y -= LEGEND_LINE_GAP;

        cb.setFontAndSize(body, 9f);

        String[] legends = {
                "ICONN\t\t\t\t-\t\tTransaction trough Internet Banking",
                "VMT-ICON\t\t\t-\t\tVisa Money Transfer through Internet Banking",
                "AUTOSWEEP\t\t\t-\t\tTransfer to linked fixed deposit",
                "REV SWEEP\t\t\t-\t\tInterest on Linked fixed Deposit",
                "SWEEP TRF\t\t\t-\t\tTransfer from Linked Fixed Deposit / Account",
                "VMT\t\t\t\t\t-\t\tVisa Money Transfer through ATM",
                "CWDR\t\t\t\t-\t\tCash Withdrawal through ATM",
                "PUR\t\t\t\t\t-\t\tPOS purchase",
                "TIP/ SCG\t\t\t-\t\tSurcharge on usage of debit card at pumps/railway ticket purchase or hotel tips",
                "RATE.DIFF\t\t\t-\t\tDifference in rates on usage of card internationally",
                "CLG\t\t\t\t\t-\t\tCheque Clearing Transaction",
                "EDC\t\t\t\t\t-\t\tCredit transaction through EDC Machine",
                "SETU \t\t\t\t-\t\tSeamless electronic fund transfer through AXIS Bank",
                "Int.pd\t\t\t\t-\t\tInterest paid to customer",
                "Int.Coll\t\t\t-\t\tInterest collected from the customer"
        };

        for (String line : legends) {
            cb.setTextMatrix(LEGEND_LEFT_X, y);
            cb.showText(line);
            y -= LEGEND_LINE_GAP;
        }

        // ---- Footer note ----
        y -= LEGEND_LINE_GAP;

        cb.setTextMatrix(LEGEND_LEFT_X, y);
        cb.showText("This is a system generated output and requires no signature.");

        cb.moveText(0, -18f);
        cb.setFontAndSize(helvetica, 12f);
        cb.showText("          ");
        cb.moveText(0, 0);

        cb.endText();

        return y - 18f;
    }

    private float drawEndOfStatement(
            Document doc,
            PdfContentByte cb,
            Fonts fonts,
            float startY,
            String requestIp) {

        // Keep block atomic
        if (startY - END_BLOCK_HEIGHT < FOOTER_BOTTOM_MARGIN) {
            doc.newPage();
            startY = PAGE_TXN_START_Y;
        }

        float y = startY;

        // ---- End of Statement ----
        cb.beginText();
        cb.setFontAndSize(fonts.timesBold.getBaseFont(), 10f);
        cb.setTextMatrix(234.98f, y);
        cb.showText("++++ End of Statement ++++");
        cb.endText();

        y -= 13f;

        // ---- Request From ----
        cb.beginText();
        cb.setFontAndSize(fonts.timesRoman.getBaseFont(), 9f);
        cb.setTextMatrix(239.37f, y);
        cb.showText("Request From: " + requestIp);
        cb.endText();

        return y - 10f;
    }

    private List<String> wrapText(
            BaseFont font,
            float fontSize,
            float maxWidth,
            String text) {

        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        for (String word : text.split(" ")) {
            String test = line.length() == 0
                    ? word
                    : line + " " + word;

            float width = font.getWidthPoint(test, fontSize);

            if (width <= maxWidth) {
                line.setLength(0);
                line.append(test);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }

        if (line.length() > 0) {
            lines.add(line.toString());
        }

        return lines;
    }

    private void drawTransactionText(
            PdfContentByte cb,
            BaseFont f,
            float gridY,
            String date,
            String particulars,
            String debit,
            String credit,
            String balance,
            String br) {

        final float fontSize = 9f;
        final float yBase = gridY + 2f;

        // ---- Date
        text(cb, f, fontSize, X_DATE, yBase, date);

        // ---- CLEAN narration (NO \n)
        String clean = particulars == null ? "" : particulars.replace("\n", " ").trim();
        String[] lines = splitIntoMax3Lines(f, fontSize, 188.28f, clean);

        int lineCount = !lines[2].isEmpty() ? 3 : !lines[1].isEmpty() ? 2 : 1;

        float shift = 0;
        if (lineCount == 3)
            shift = 9f;
        else if (lineCount == 1)
            shift = -9f;

        // ---- Axis baselines
        float y1 = gridY + 12f + shift;
        float y2 = gridY + 2f + shift;
        float y3 = gridY + 2f;

        drawParticularsBlock(
                cb,
                f,
                fontSize,
                X_PART,
                y1,
                y2,
                y3,
                lines);

        // ---- Amounts
        if (!debit.isEmpty())
            right(cb, f, fontSize, R_DEBIT, yBase, debit);

        if (!credit.isEmpty())
            right(cb, f, fontSize, R_CREDIT, yBase, credit);

        right(cb, f, fontSize, R_BAL, yBase, balance);
        text(cb, f, fontSize, X_INIT, yBase, br);
    }

    private float computeTxnRowHeight(BaseFont f, float fontSize, String particulars) {
        String clean = particulars == null ? "" : particulars.replace("\n", " ").trim();
        String[] lines = splitIntoMax3Lines(f, fontSize, 188.28f, clean);

        // return lines[2].isEmpty() ? 22f : 33f;

        if (lines[1].isEmpty())
            return 11f; // 1 lines
        else if (lines[2].isEmpty())
            return 22f; // 2 lines
        else
            return 33f; // 3 lines
    }

    private void drawParticularsBlock(
            PdfContentByte cb,
            BaseFont f,
            float fontSize,
            float x,
            float y1,
            float y2,
            float y3,
            String[] lines) {

        cb.beginText();
        cb.setFontAndSize(f, fontSize);

        if (!lines[0].isEmpty()) {
            cb.setTextMatrix(x, y1);
            cb.showText(lines[0]);
        }

        if (!lines[1].isEmpty()) {
            cb.setTextMatrix(x, y2);
            cb.showText(lines[1]);
        }

        if (!lines[2].isEmpty()) {
            cb.setTextMatrix(x, y3);
            cb.showText(lines[2]);
        }

        cb.endText();
    }

    private float drawFooterAtY(
            Document doc,
            PdfContentByte cb,
            BaseFont font,
            float startY,
            String branchAddress,
            String branchPhone) {

        float y = startY;


        // ---- STANDARD FOOTER PARAGRAPHS ----
        for (String para : FOOTER_PARAGRAPHS) {
            String[] lines = splitIntoLines(font, FOOTER_FONT_SIZE, 520f, para);

            cb.beginText();
            cb.setFontAndSize(font, FOOTER_FONT_SIZE);
            for (String line : lines) {

                if (y < FOOTER_BOTTOM_MARGIN) {
                    cb.endText();
                    doc.newPage();
                    y = PAGE_TXN_START_Y;
                    cb.beginText();
                    cb.setFontAndSize(font, FOOTER_FONT_SIZE);
                }

                cb.setTextMatrix(FOOTER_LEFT_X, y);
                cb.showText(line);
                y -= FOOTER_LINE_GAP;
            }
            cb.endText();

            y -= 4f; // paragraph gap
        }

        // ---- BRANCH ADDRESS (AXIS STYLE) ----
        if (branchAddress != null && !branchAddress.isEmpty()) {

            String branchText = "BRANCH ADDRESS - " +
                    branchAddress +
                    " TEL:" + branchPhone +
                    " FAX:4285393";

            String[] addrLines = splitIntoLines(font, FOOTER_FONT_SIZE, 520f, branchText);

            for (String line : addrLines) {

                if (y < FOOTER_BOTTOM_MARGIN) {
                    cb.endText();
                    doc.newPage();
                    y = PAGE_TXN_START_Y;
                    cb.beginText();
                    cb.setFontAndSize(font, FOOTER_FONT_SIZE);
                }

                cb.setTextMatrix(FOOTER_LEFT_X, y);
                cb.showText(line);
                y -= FOOTER_LINE_GAP;
            }
        }

        cb.endText();
        return y;
    }

    private float drawLegendAxisStyle(
            Document doc,
            PdfContentByte cb,
            Fonts fonts,
            float startY) {

        BaseFont helvetica = fonts.helvetica.getBaseFont();
        BaseFont bold = fonts.timesBold.getBaseFont();
        BaseFont body = fonts.timesRoman.getBaseFont();

        float x = 36f;
        float y = startY;

        cb.beginText();
        cb.setTextMatrix(x, y);

        float[] spacerMoves = { 22f, 13f, 22f, 13f, 22f, 22f, 22f, 22f, 22f, 22f, 22f, 22f, 22f, 13f, 22f, 22f, 22f, 22f, 22f, 22f, 22f, 31f, 22f, 24f, 20f, 18f };

        for (float dy : spacerMoves) {
            if (y - dy < FOOTER_BOTTOM_MARGIN) {
                cb.endText();
                doc.newPage();
                y = PAGE_TXN_START_Y;
                cb.beginText();
                cb.setTextMatrix(x, y);
            }
            cb.moveText(0, -dy);
            y -= dy;
        }

        if (y - 36f < FOOTER_BOTTOM_MARGIN) {
            cb.endText();
            doc.newPage();
            y = PAGE_TXN_START_Y;
            cb.beginText();
            cb.setTextMatrix(x, y);
        }

        cb.setFontAndSize(helvetica, 12f);
        cb.showText("   ");
        cb.moveText(0, 0);
        cb.moveText(0, -18f);
        y -= 18f;

        cb.showText("          ");
        cb.moveText(0, 0);

        float[] moreMoves = { 22f, 31f, 40f, 22f, 31f, 22f, 31f, 13f, 31f, 4f};

        for (float dy : moreMoves) {
            if (y - dy < FOOTER_BOTTOM_MARGIN) {
                cb.endText();
                doc.newPage();
                y = PAGE_TXN_START_Y;
                cb.beginText();
                cb.setTextMatrix(x, y);
            }
            cb.moveText(0, -dy);
            y -= dy;
        }

        // ------------------------------
        // Legends title
        // ------------------------------
        if (y - 13.5f < FOOTER_BOTTOM_MARGIN) {
            cb.endText();
            doc.newPage();
            y = PAGE_TXN_START_Y;
            cb.beginText();
            cb.setTextMatrix(x, y);
        }

        cb.moveText(0, -13.5f);
        y -= 13.5f;

        cb.setFontAndSize(bold, 9f);
        cb.showText("Legends :");

        cb.setFontAndSize(body, 9f);

        for (String line : LEGENDS) {
            if (y - 13.5f < FOOTER_BOTTOM_MARGIN) {
                cb.endText();
                doc.newPage();
                y = PAGE_TXN_START_Y;
                cb.beginText();
                cb.setTextMatrix(x, y);
                cb.setFontAndSize(body, 9f);
            }
            cb.moveText(0, -13.5f);
            y -= 13.5f;
            cb.showText(line);
        }

        if (y - 27f < FOOTER_BOTTOM_MARGIN) {
            cb.endText();
            doc.newPage();
            y = PAGE_TXN_START_Y;
            cb.beginText();
            cb.setTextMatrix(x, y);
            cb.setFontAndSize(body, 9f);
        }

        cb.moveText(0, -13.5f);
        y -= 13.5f;
        cb.showText(" ");

        cb.moveText(0, -13.5f);
        y -= 13.5f;
        cb.showText("This is a system generated output and requires no signature.");
        cb.moveText(0, 0);

        if (y - 18f < FOOTER_BOTTOM_MARGIN) {
            cb.endText();
            doc.newPage();
            y = PAGE_TXN_START_Y;
            cb.beginText();
            cb.setTextMatrix(x, y);
        }

        cb.moveText(0, -18f);
        y -= 18f;
        cb.setFontAndSize(helvetica, 12f);
        cb.showText("          ");
        cb.moveText(0, 0);
        cb.moveText(0, -14f);
        cb.moveText(0, -13f);

        cb.endText();

        return y;
    }

}