package com.user.pdfservice.template.hdfccurrent;

import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.Transaction;
import com.user.pdfservice.template.BankPdfTemplate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

public class HdfcCurrentTemplate implements BankPdfTemplate {

    // Document Geometry & Page Setup (Custom Extended A4: 638.00 pt x 842.00 pt)
    private static final float PAGE_W = 638.00f;
    private static final float PAGE_H = 842.00f;

    // Total Table Span: x = 28.346 pt to x = 628.456 pt (Width = 600.110 pt)
    private static final float TABLE_LEFT = 28.346f;
    private static final float TABLE_RIGHT = 628.456f;
    private static final float TABLE_WIDTH = TABLE_RIGHT - TABLE_LEFT; // 600.110f

    // 7-Column Widths & Boundaries matching exact marked dimensions:
    // Col 1 (Date): 39.69pt | Col 2 (Narration): 186.52pt | Col 3 (Ref): 101.48pt
    // Col 4 (Value Dt): 39.12pt | Col 5 (Withdrawal): 77.39pt | Col 6 (Deposit): 77.39pt | Col 7 (Balance): 78.52pt
    private static final float COL_DATE_X = 28.346f;                              // 28.346..68.036  (W=39.690)
    private static final float COL_NARRATION_X = COL_DATE_X + 39.690f;            // 68.036..254.556 (W=186.520)
    private static final float COL_REF_X = COL_NARRATION_X + 186.520f;           // 254.556..356.036(W=101.480)
    private static final float COL_VALDT_X = COL_REF_X + 101.480f;                // 356.036..395.156(W=39.120)
    private static final float COL_WITHDRAWAL_X = COL_VALDT_X + 39.120f;         // 395.156..472.546(W=77.390)
    private static final float COL_DEPOSIT_X = COL_WITHDRAWAL_X + 77.390f;       // 472.546..549.936(W=77.390)
    private static final float COL_BALANCE_X = COL_DEPOSIT_X + 77.390f;           // 549.936..628.456(W=78.520)

    private static final float TABLE_TOP_Y = 612.553f;
    private static final float TABLE_HEADER_H = 17.614f;
    private static final float TABLE_DATA_TOP_Y = 594.939f; // TABLE_TOP_Y - TABLE_HEADER_H
    private static final float TABLE_BOTTOM_Y = 60.0f;     // Table floor margin

    // Exact Color Palette: Uniform #E5FFFF pale ice-blue & #959595 gray borders
    private static final float[] TABLE_BG_COLOR = {229f / 255f, 255f / 255f, 255f / 255f}; // #E5FFFF
    private static final float[] GRID_LINE_COLOR = {149f / 255f, 149f / 255f, 149f / 255f}; // #959595

    // Strict 27px (17.200 pt) baseline vertical pitch unit
    private static final float LINE_UNIT = 17.200f;
    private static final float PAD_R = 6.37f;

    private Statement stmt;
    private Fonts fonts;
    private Header header;
    private PDDocument doc;

    @Override
    public byte[] generate(Statement statement) throws Exception {
        this.stmt = statement;
        try (PDDocument document = new PDDocument()) {
            this.doc = document;
            doc.getDocument().setVersion(1.5f);

            PDDocumentInformation info = new PDDocumentInformation();
            info.setProducer("HDFC Bank Document Engine");
            info.setCreationDate(Calendar.getInstance());
            doc.setDocumentInformation(info);

            this.fonts = new Fonts(doc);
            this.header = new Header(stmt, fonts, doc);

            renderPages();

            if (stmt.meta != null && stmt.meta.password != null && !stmt.meta.password.isEmpty()) {
                AccessPermission ap = new AccessPermission();
                ap.setCanPrint(true);
                ap.setCanExtractContent(true);
                ap.setCanModify(false);
                StandardProtectionPolicy policy = new StandardProtectionPolicy(
                        stmt.meta.password, stmt.meta.password, ap);
                policy.setEncryptionKeyLength(128);
                doc.protect(policy);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private void renderPages() throws IOException {
        List<Transaction> txns = (stmt.transactions != null) ? stmt.transactions : List.of();
        int txnIndex = 0;
        int pageNum = 1;

        double runningBal = (stmt.details != null) ? stmt.details.startingBalance : 0.0;
        double totalDebits = 0.0;
        double totalCredits = 0.0;
        int drCount = 0;
        int crCount = 0;

        while (txnIndex < txns.size()) {
            PDPage page = new PDPage(new PDRectangle(PAGE_W, PAGE_H));
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.OVERWRITE, false)) {
                // 1. Draw top customer and branch header
                header.draw(cs, pageNum);

                // 2. Draw table column header
                drawTableHeader(cs);

                // 3. Render transactions on this page
                float currentY = TABLE_DATA_TOP_Y;

                while (txnIndex < txns.size()) {
                    Transaction txn = txns.get(txnIndex);
                    List<String> narrationLines = TemplateUtils.wrapText(txn.description, fonts.regular, 8.0f, 180.0f);
                    int lineCount = Math.max(1, narrationLines.size());
                    float rowH = lineCount * LINE_UNIT; // Exact 27px (17.200 pt) per line

                    // Check if row fits on current page
                    if (currentY - rowH < TABLE_BOTTOM_Y) {
                        break; // Move to next page
                    }

                    // Accumulate totals
                    if (txn.debit > 0) {
                        totalDebits += txn.debit;
                        drCount++;
                    }
                    if (txn.credit > 0) {
                        totalCredits += txn.credit;
                        crCount++;
                    }
                    runningBal = txn.balance;

                    // Draw transaction row
                    drawTransactionRow(cs, txn, narrationLines, currentY, rowH);
                    currentY -= rowH;
                    txnIndex++;
                }

                // Close table grid on this page (vertical borders + outer box)
                drawTableBorders(cs, TABLE_TOP_Y, currentY);

                // If this is the last transaction page, check if summary fits
                // If this is the last transaction page, check if summary fits
                if (txnIndex >= txns.size()) {
                    float totalTerminalH = 228.431f; // Exact terminal box height from layout spec
                    float summaryBoxH = 170.0f;
                    float noteBoxH = totalTerminalH - summaryBoxH; // 58.431f

                    if (currentY - totalTerminalH >= TABLE_BOTTOM_Y) {
                        // Both Summary Box and Computer-Generated Note Box fit on this page
                        float summaryTop = currentY;
                        drawSummaryContainerBox(cs, summaryTop, summaryBoxH, totalDebits, totalCredits, drCount, crCount, runningBal);
                        float summaryBottom = summaryTop - summaryBoxH;
                        drawNoteBox(cs, summaryBottom, noteBoxH, false);
                    } else if (currentY - summaryBoxH >= TABLE_BOTTOM_Y) {
                        // Only Summary Box fits on this page, Note Box goes to next page
                        float summaryTop = currentY;
                        drawSummaryContainerBox(cs, summaryTop, summaryBoxH, totalDebits, totalCredits, drCount, crCount, runningBal);
                        drawFooter(cs);
                        cs.close();

                        pageNum++;
                        PDPage terminalPage = new PDPage(new PDRectangle(PAGE_W, PAGE_H));
                        doc.addPage(terminalPage);
                        try (PDPageContentStream sumCs = new PDPageContentStream(doc, terminalPage, PDPageContentStream.AppendMode.OVERWRITE, false)) {
                            header.draw(sumCs, pageNum);
                            drawNoteBox(sumCs, TABLE_TOP_Y, totalTerminalH, true);
                            drawFooter(sumCs);
                        }
                        return;
                    } else {
                        // Neither fits on this page, both move to next page
                        drawFooter(cs);
                        cs.close();

                        pageNum++;
                        PDPage terminalPage = new PDPage(new PDRectangle(PAGE_W, PAGE_H));
                        doc.addPage(terminalPage);
                        try (PDPageContentStream sumCs = new PDPageContentStream(doc, terminalPage, PDPageContentStream.AppendMode.OVERWRITE, false)) {
                            header.draw(sumCs, pageNum);
                            float summaryTop = TABLE_TOP_Y;
                            drawSummaryContainerBox(sumCs, summaryTop, summaryBoxH, totalDebits, totalCredits, drCount, crCount, runningBal);
                            float summaryBottom = summaryTop - summaryBoxH;
                            drawNoteBox(sumCs, summaryBottom, noteBoxH, false);
                            drawFooter(sumCs);
                        }
                        return;
                    }
                }

                // 4. Draw footer
                drawFooter(cs);
            }

            pageNum++;
        }

        // If statement had 0 transactions
        if (txns.isEmpty()) {
            PDPage page = new PDPage(new PDRectangle(PAGE_W, PAGE_H));
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.OVERWRITE, false)) {
                header.draw(cs, 1);
                float totalTerminalH = 228.431f;
                float summaryBoxH = 170.0f;
                float summaryTop = TABLE_TOP_Y;
                drawSummaryContainerBox(cs, summaryTop, summaryBoxH, 0.0, 0.0, 0, 0, runningBal);
                float summaryBottom = summaryTop - summaryBoxH;
                drawNoteBox(cs, summaryBottom, totalTerminalH - summaryBoxH, false);
                drawFooter(cs);
            }
        }
    }

    private void drawSummaryContainerBox(PDPageContentStream cs, float boxTop, float boxHeight, double debits, double credits, int drCount, int crCount, double closingBal) throws IOException {
        float boxBottom = boxTop - boxHeight;

        // 1. Background: Solid pure white #FFFFFF
        cs.saveGraphicsState();
        cs.setNonStrokingColor(1.0f, 1.0f, 1.0f);
        cs.addRect(TABLE_LEFT, boxBottom, TABLE_WIDTH, boxHeight);
        cs.fill();
        cs.restoreGraphicsState();

        // 2. Borders:
        cs.saveGraphicsState();
        cs.setLineWidth(0.566f); // 1px solid

        // Top border: 1px solid #AEB0B0 (RGB: 174, 176, 176)
        cs.setStrokingColor(174f / 255f, 176f / 255f, 176f / 255f);
        cs.moveTo(TABLE_LEFT, boxTop);
        cs.lineTo(TABLE_RIGHT, boxTop);
        cs.stroke();

        // Bottom border: 1px solid #DBDBDB (RGB: 219, 219, 219)
        cs.setStrokingColor(219f / 255f, 219f / 255f, 219f / 255f);
        cs.moveTo(TABLE_LEFT, boxBottom);
        cs.lineTo(TABLE_RIGHT, boxBottom);
        cs.stroke();

        // Left border: 1px subtle frame #D2D2D2 (RGB: 210, 210, 210)
        cs.setStrokingColor(210f / 255f, 210f / 255f, 210f / 255f);
        cs.moveTo(TABLE_LEFT, boxBottom);
        cs.lineTo(TABLE_LEFT, boxTop);
        cs.stroke();

        // Right border: 1px subtle frame #EDEDED (RGB: 237, 237, 237)
        cs.setStrokingColor(237f / 255f, 237f / 255f, 237f / 255f);
        cs.moveTo(TABLE_RIGHT, boxBottom);
        cs.lineTo(TABLE_RIGHT, boxTop);
        cs.stroke();
        cs.restoreGraphicsState();

        // 3. STATEMENT SUMMARY TITLE:
        float titleX = TABLE_LEFT + 76.5f;
        float titleY = boxTop - 25.0f;
        TemplateUtils.drawText(cs, "STATEMENT SUMMARY :-", titleX, titleY, fonts.bold, 10.0f, 0f, 0f, 0f);

        // 4. 6-COLUMN METRICS GRID:
        float headersY = titleY - 18.0f;
        float valuesY = headersY - 15.0f;

        float scale = TABLE_WIDTH / 945.0f;
        float cx1 = 265.0f * scale;
        float cx2 = 493.0f * scale;
        float cx3 = 605.0f * scale;
        float cx4 = 698.0f * scale;
        float cx5 = 821.0f * scale;
        float cx6 = 944.0f * scale;

        // Draw Headers:
        TemplateUtils.drawCenteredText(cs, "Opening Balance", cx1, headersY, fonts.bold, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawCenteredText(cs, "Dr Count", cx2, headersY, fonts.bold, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawCenteredText(cs, "Cr Count", cx3, headersY, fonts.bold, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawCenteredText(cs, "Debits", cx4, headersY, fonts.bold, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawCenteredText(cs, "Credits", cx5, headersY, fonts.bold, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawCenteredText(cs, "Closing Bal", cx6, headersY, fonts.bold, 8.0f, 0f, 0f, 0f);

        // Draw Values:
        double startingBal = (stmt.details != null) ? stmt.details.startingBalance : 0.0;
        TemplateUtils.drawCenteredText(cs, TemplateUtils.formatIndianCurrency(startingBal), cx1, valuesY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawCenteredText(cs, String.valueOf(drCount), cx2, valuesY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawCenteredText(cs, String.valueOf(crCount), cx3, valuesY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawCenteredText(cs, TemplateUtils.formatIndianCurrency(debits), cx4, valuesY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawCenteredText(cs, TemplateUtils.formatIndianCurrency(credits), cx5, valuesY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawCenteredText(cs, TemplateUtils.formatIndianCurrency(closingBal), cx6, valuesY, fonts.regular, 8.0f, 0f, 0f, 0f);

        // 5. GENERATION METADATA BAR:
        float metaY = boxBottom + 16.0f;
        String genDate = (stmt.meta != null && stmt.meta.generatedAt != null && !stmt.meta.generatedAt.isBlank())
                ? stmt.meta.generatedAt
                : "18-Aug-2026 14:33";

        float mx1 = 141.0f * scale;
        float mx2 = 426.0f * scale;
        float mx3 = 708.0f * scale;

        TemplateUtils.drawText(cs, "Generated On: " + genDate, mx1, metaY, fonts.bold, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, "Generated By: 348583054", mx2, metaY, fonts.bold, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, "Requesting Branch Code: NET", mx3, metaY, fonts.bold, 8.0f, 0f, 0f, 0f);
    }

    private void drawNoteBox(PDPageContentStream cs, float topY, float height, boolean isStandalone) throws IOException {
        float bottomY = topY - height;
        cs.saveGraphicsState();
        cs.setLineWidth(0.566f);
        cs.setStrokingColor(GRID_LINE_COLOR[0], GRID_LINE_COLOR[1], GRID_LINE_COLOR[2]);
        cs.addRect(TABLE_LEFT, bottomY, TABLE_WIDTH, height);
        cs.stroke();

        if (isStandalone) {
            // Draw horizontal partition line separating upper area and bottom note section
            float dividerY = topY - 170.0f;
            cs.moveTo(TABLE_LEFT, dividerY);
            cs.lineTo(TABLE_RIGHT, dividerY);
            cs.stroke();
        }
        cs.restoreGraphicsState();

        float sigX = TABLE_RIGHT - 165.0f;
        float sigY = isStandalone ? (topY - 170.0f - 22.0f) : (topY - 22.0f);
        TemplateUtils.drawText(cs, "This is a computer generated statement and does", sigX, sigY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, "not require signature.", sigX, sigY - 12.0f, fonts.regular, 8.0f, 0f, 0f, 0f);
    }

    private void drawTableHeader(PDPageContentStream cs) throws IOException {
        // Draw Header Background (#E5FFFF)
        cs.saveGraphicsState();
        cs.setNonStrokingColor(TABLE_BG_COLOR[0], TABLE_BG_COLOR[1], TABLE_BG_COLOR[2]);
        cs.addRect(TABLE_LEFT, TABLE_DATA_TOP_Y, TABLE_WIDTH, TABLE_HEADER_H);
        cs.fill();
        cs.restoreGraphicsState();

        float y = 602.145f;
        // Text alignments matching specifications:
        TemplateUtils.drawText(cs, "Date", COL_DATE_X + 8.30f, y, fonts.bold, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, "Narration", COL_NARRATION_X + 5.74f, y, fonts.bold, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, "Chq./Ref.No.", COL_REF_X + 33.15f, y, fonts.bold, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, "Value Dt", COL_VALDT_X + 5.74f, y, fonts.bold, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawRightAlignedText(cs, "Withdrawal Amt.", COL_DEPOSIT_X - PAD_R, y, fonts.bold, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawRightAlignedText(cs, "Deposit Amt.", COL_BALANCE_X - PAD_R, y, fonts.bold, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawRightAlignedText(cs, "Closing Balance", TABLE_RIGHT - PAD_R, y, fonts.bold, 8.0f, 0f, 0f, 0f);

        // Divider below header row (1px solid #959595)
        cs.saveGraphicsState();
        cs.setLineWidth(0.566f);
        cs.setStrokingColor(GRID_LINE_COLOR[0], GRID_LINE_COLOR[1], GRID_LINE_COLOR[2]);
        cs.moveTo(TABLE_LEFT, TABLE_DATA_TOP_Y);
        cs.lineTo(TABLE_RIGHT, TABLE_DATA_TOP_Y);
        cs.stroke();
        cs.restoreGraphicsState();
    }

    private void drawTransactionRow(PDPageContentStream cs, Transaction txn, List<String> narrationLines, float topY, float rowH) throws IOException {
        // Fill row background (#E5FFFF) - NO alternating zebra rows
        cs.saveGraphicsState();
        cs.setNonStrokingColor(TABLE_BG_COLOR[0], TABLE_BG_COLOR[1], TABLE_BG_COLOR[2]);
        cs.addRect(TABLE_LEFT, topY - rowH, TABLE_WIDTH, rowH);
        cs.fill();
        cs.restoreGraphicsState();

        float baseline = topY - 12.0f;

        // 1. Date (Left-aligned: padding-left = 13px / 8.3pt -> x = COL_DATE_X + 8.30f) - Line 1 only
        String dateStr = TemplateUtils.formatDateDDMMYY(txn.date);
        TemplateUtils.drawText(cs, dateStr, COL_DATE_X + 8.30f, baseline, fonts.regular, 8.0f, 0f, 0f, 0f);

        // 2. Narration lines with alternating indents:
        // Line 1: padding-left 9px (5.74pt) -> x = COL_NARRATION_X + 5.74f
        // Line 2: padding-left 2px (1.27pt) -> x = COL_NARRATION_X + 1.27f
        // Line 3+: padding-left 9px (5.74pt) -> x = COL_NARRATION_X + 5.74f
        float narrY = baseline;
        for (int i = 0; i < narrationLines.size(); i++) {
            float nX = (i == 1) ? (COL_NARRATION_X + 1.27f) : (COL_NARRATION_X + 5.74f);
            TemplateUtils.drawText(cs, narrationLines.get(i), nX, narrY, fonts.regular, 8.0f, 0f, 0f, 0f);
            narrY -= LINE_UNIT;
        }

        // 3. Chq./Ref.No. (Left-aligned: padding-left = 52px / 33.15pt -> x = COL_REF_X + 33.15f) - Line 1 only
        String ref = (txn.reference != null && !txn.reference.isBlank()) ? txn.reference : "";
        TemplateUtils.drawText(cs, ref, COL_REF_X + 33.15f, baseline, fonts.regular, 8.0f, 0f, 0f, 0f);

        // 4. Value Dt (Left-aligned: padding-left = 9px / 5.74pt -> x = COL_VALDT_X + 5.74f) - Line 1 only
        TemplateUtils.drawText(cs, dateStr, COL_VALDT_X + 5.74f, baseline, fonts.regular, 8.0f, 0f, 0f, 0f);

        // 5. Withdrawal Amt. (Right-aligned: padding-right = 10px / 6.37pt -> aligned to COL_DEPOSIT_X - PAD_R) - Line 1 only
        if (txn.debit > 0) {
            String drStr = TemplateUtils.formatIndianCurrency(txn.debit);
            TemplateUtils.drawRightAlignedText(cs, drStr, COL_DEPOSIT_X - PAD_R, baseline, fonts.regular, 8.0f, 0f, 0f, 0f);
        }

        // 6. Deposit Amt. (Right-aligned: padding-right = 10px / 6.37pt -> aligned to COL_BALANCE_X - PAD_R) - Line 1 only
        if (txn.credit > 0) {
            String crStr = TemplateUtils.formatIndianCurrency(txn.credit);
            TemplateUtils.drawRightAlignedText(cs, crStr, COL_BALANCE_X - PAD_R, baseline, fonts.regular, 8.0f, 0f, 0f, 0f);
        }

        // 7. Closing Balance (Right-aligned: padding-right = 10px / 6.37pt -> aligned to TABLE_RIGHT - PAD_R) - Line 1 only
        String balStr = TemplateUtils.formatIndianCurrency(txn.balance);
        TemplateUtils.drawRightAlignedText(cs, balStr, TABLE_RIGHT - PAD_R, baseline, fonts.regular, 8.0f, 0f, 0f, 0f);

        // NOTE: ZERO horizontal row lines between individual transactions as specified.
    }

    private void drawTableBorders(PDPageContentStream cs, float topY, float bottomY) throws IOException {
        cs.saveGraphicsState();
        cs.setLineWidth(0.566f);
        cs.setStrokingColor(GRID_LINE_COLOR[0], GRID_LINE_COLOR[1], GRID_LINE_COLOR[2]);

        // Outer Rectangle (1px solid #959595)
        cs.addRect(TABLE_LEFT, bottomY, TABLE_WIDTH, topY - bottomY);
        cs.stroke();

        // Vertical column dividing lines (1px solid #959595) running continuously
        float[] vLines = {COL_NARRATION_X, COL_REF_X, COL_VALDT_X, COL_WITHDRAWAL_X, COL_DEPOSIT_X, COL_BALANCE_X};
        for (float vx : vLines) {
            cs.moveTo(vx, topY);
            cs.lineTo(vx, bottomY);
            cs.stroke();
        }
        cs.restoreGraphicsState();
    }

    private void drawFooter(PDPageContentStream cs) throws IOException {
        // Line 1: HDFC BANK LIMITED (Times-Bold 9.0 pt, #0000FF, y = 50.108 pt)
        TemplateUtils.drawText(cs, "HDFC BANK LIMITED", TABLE_LEFT, 50.108f, fonts.bold, 9.0f, 0.0f, 0.0f, 1.0f);

        // Line 2: *Closing balance includes funds earmarked for hold and uncleared funds (Times-Roman 8.0 pt, #0000FF, y = 40.799 pt)
        TemplateUtils.drawText(cs, "*Closing balance includes funds earmarked for hold and uncleared funds",
                TABLE_LEFT, 40.799f, fonts.regular, 8.0f, 0.0f, 0.0f, 1.0f);

        // Line 3: Contents of this statement... (Times-Roman 7.0 pt, #000000, y = 32.282 pt)
        TemplateUtils.drawText(cs, "Contents of this statement will be considered correct if no error is reported within 30 days of receipt of statement.The address on this statement is that on record with the Bank as at the day of requesting",
                TABLE_LEFT, 32.282f, fonts.regular, 7.0f, 0f, 0f, 0f);

        // Line 4: this statement. (Times-Roman 7.0 pt, #000000, y = 24.982 pt)
        TemplateUtils.drawText(cs, "this statement.",
                TABLE_LEFT, 24.982f, fonts.regular, 7.0f, 0f, 0f, 0f);

        // Line 5: State account branch GSTN... (Times-Bold 7.0 pt, #000000, y = 17.731 pt)
        TemplateUtils.drawText(cs, "State account branch GSTN:08AAACH2702H1Z0",
                TABLE_LEFT, 17.731f, fonts.bold, 7.0f, 0f, 0f, 0f);

        // Line 6: HDFC Bank GSTIN number details... (Times-Roman 7.0 pt, #000000, y = 10.515 pt)
        TemplateUtils.drawText(cs, "HDFC Bank GSTIN number details are available at https://www.hdfcbank.com/personal/making-payments/online-tax-payment/goods-and-service-tax.",
                TABLE_LEFT, 10.515f, fonts.regular, 7.0f, 0f, 0f, 0f);

        // Line 7: Registered Office Address... (Times-Roman 7.0 pt, #000000, y = 3.215 pt)
        TemplateUtils.drawText(cs, "Registered Office Address: HDFC Bank House,Senapati Bapat Marg,Lower Parel,Mumbai 400013",
                TABLE_LEFT, 3.215f, fonts.regular, 7.0f, 0f, 0f, 0f);
    }
}



