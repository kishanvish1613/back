package com.user.pdfservice.template.unionbank;

import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.Transaction;
import com.user.pdfservice.template.BankPdfTemplate;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Union Bank of India — "Statement of Account" template.
 * Rebuilt to visually match the real UBI current-account statement:
 *  - centered brand/logo + "Statement of Account" title (page 1 only)
 *  - two-column header (account holder / bank details) + records note
 *  - blue table-header bar, 8 columns, zebra rows, continuous balance band
 *  - Indian-grouped amounts, dd-MM-yyyy + HH:mm:ss cells
 *  - repeating footer on every page
 */
public class UnionBankTemplate implements BankPdfTemplate {

    private Statement stmt;
    private Fonts fonts;
    private PageCursor pc;
    private PDDocument doc;

    // ---- Page geometry (points) ----
    private static final float PAGE_W = 595;
    private static final float PAGE_H = 842;
    private static final float MARGIN_L = 40;
    private static final float MARGIN_R = 40;
    private static final float MARGIN_TOP = 40;
    private static final float MID_PAGE_TOP_MARGIN = 0; // top gap on pages 2+ (no header) — flush to top edge

    private static final float TABLE_LEFT = MARGIN_L;
    private static final float TABLE_RIGHT = PAGE_W - MARGIN_R;   // 555
    private static final float TABLE_WIDTH = TABLE_RIGHT - TABLE_LEFT; // 515

    // ---- Column widths (sum == 515), proportional to the spec (72/150/90/120/120/110/110/125) ----
    private static final float COL_DATE_W = 41;
    private static final float COL_REMARKS_W = 86;
    private static final float COL_TRAN_ID_W = 52;
    private static final float COL_UTR_W = 69;
    private static final float COL_INSTR_ID_W = 69;
    private static final float COL_WITHDRAWAL_W = 63;
    private static final float COL_DEPOSIT_W = 63;
    private static final float COL_BALANCE_W = 72;

    private float colDateX, colRemarksX, colTranIdX, colUtrX, colInstrIdX,
            colWithdrawalX, colDepositX, colBalanceX;

    // Parallel arrays (left->right) for column-band fills and header cells.
    private float[] colXs, colWs;
    private float[][] colColors;

    // ---- Fonts ----
    private static final float FONT_TABLE_HEADER = 8;
    private static final float FONT_TABLE_BODY = 7;

    private static final float LINE_HEIGHT_BODY = 10;
    private static final float PAD_TEXT = 3;  // left padding for text cells
    private static final float PAD_NUM = 4;   // right padding for numeric cells
    private static final float HEADER_BAR_H = 15;

    // ---- Theme colours: alternating per-column bands (spec) ----
    private static final float[] C_COL_GRAY   = {0.8745f, 0.8745f, 0.8745f}; // #DFDFDF (cols 1,3,5,7)
    private static final float[] C_COL_BLUE   = {0.5961f, 0.6824f, 0.7765f}; // #98AEC6 (cols 2,4,6,8)
    private static final float[] C_HEADER_TEXT = {0.067f, 0.067f, 0.067f};   // #111111 header/body text
    private static final float[] C_SEP         = {1f, 1f, 1f};               // thin white separators
    private static final float[] C_RULE        = {0.184f, 0.310f, 0.686f};   // #2F4FAF footer rule

    // ---- Footer geometry (fixed on every page) ----
    // Reference order, top -> bottom: blue rule, "Page NoX", then the disclaimer block.
    // FOOTER_TEXT_TOP_Y is the pinned anchor; the rule and page number are derived above it.
    private static final float FOOTER_TEXT_TOP_Y = 130;  // first disclaimer line baseline (pinned)
    private static final float FOOTER_LINE_GAP = 10;     // spacing between disclaimer lines
    private static final float FOOTER_PAGENUM_Y = FOOTER_TEXT_TOP_Y + 12; // "Page No" sits above the disclaimers
    private static final float FOOTER_RULE_Y = FOOTER_PAGENUM_Y + 12;     // blue separator caps the block
    private static final float FOOTER_GAP = 38;          // blank band between last row and the rule (~1.5 row heights)
    private static final float FOOTER_RULE_INSET = 7;    // rule spans the full page width, 2pt shy of each page edge
    private static final float TABLE_FLOOR = FOOTER_RULE_Y + FOOTER_GAP;  // rows never cross below this

    @Override
    public byte[] generate(Statement statement) throws Exception {
        this.stmt = statement;
        try (PDDocument document = new PDDocument()) {
            this.doc = document;
            doc.getDocument().setVersion(1.5f);

            PDDocumentInformation info = new PDDocumentInformation();
            info.setProducer("Union Bank Statement Generator");
            info.setCreationDate(Calendar.getInstance());
            doc.setDocumentInformation(info);

            fonts = new Fonts(doc);
            computeColumnPositions();

            pc = new PageCursor(doc, fonts, PAGE_H - MARGIN_TOP);

            // Page-1 header block lives in Header; returns y just below it.
            float chromeTop = new Header(stmt, fonts, doc).draw(pc.cs(), pc.y());
            List<Page> pages = layoutPages(chromeTop);
            renderPages(pages);
            pc.close();

            if (stmt.meta.password != null && !stmt.meta.password.isEmpty()) {
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

    private void computeColumnPositions() {
        colDateX = TABLE_LEFT;
        colRemarksX = colDateX + COL_DATE_W;
        colTranIdX = colRemarksX + COL_REMARKS_W;
        colUtrX = colTranIdX + COL_TRAN_ID_W;
        colInstrIdX = colUtrX + COL_UTR_W;
        colWithdrawalX = colInstrIdX + COL_INSTR_ID_W;
        colDepositX = colWithdrawalX + COL_WITHDRAWAL_W;
        colBalanceX = colDepositX + COL_DEPOSIT_W;

        colXs = new float[]{colDateX, colRemarksX, colTranIdX, colUtrX, colInstrIdX,
                colWithdrawalX, colDepositX, colBalanceX};
        colWs = new float[]{COL_DATE_W, COL_REMARKS_W, COL_TRAN_ID_W, COL_UTR_W, COL_INSTR_ID_W,
                COL_WITHDRAWAL_W, COL_DEPOSIT_W, COL_BALANCE_W};
        // Alternating band colours: gray, blue, gray, blue, ...
        colColors = new float[][]{C_COL_GRAY, C_COL_BLUE, C_COL_GRAY, C_COL_BLUE,
                C_COL_GRAY, C_COL_BLUE, C_COL_GRAY, C_COL_BLUE};
    }

    // ==================== TABLE HEADER (alternating coloured cells, centered labels) ====================
    private static final String[] COL_HEADERS = {
            "Date", "Remarks", "Tran Id-1", "UTR Number", "Instr. ID",
            "Withdrawals", "Deposits", "Balance"
    };

    private void drawTableHeaderBar(float top) throws IOException {
        PDPageContentStream cs = pc.cs();
        float barBottom = top - HEADER_BAR_H;
        // Each header cell painted with its own column band colour.
        for (int c = 0; c < colXs.length; c++) {
            float[] col = colColors[c];
            cs.setNonStrokingColor(col[0], col[1], col[2]);
            cs.addRect(colXs[c], barBottom, colWs[c], HEADER_BAR_H);
            cs.fill();
        }
        // Centered bold labels.
        cs.setNonStrokingColor(C_HEADER_TEXT[0], C_HEADER_TEXT[1], C_HEADER_TEXT[2]);
        float ty = barBottom + (HEADER_BAR_H - FONT_TABLE_HEADER) / 2f + 1;
        for (int c = 0; c < colXs.length; c++) {
            float w = TemplateUtils.getTextWidth(COL_HEADERS[c], fonts.bold, FONT_TABLE_HEADER);
            float tx = colXs[c] + (colWs[c] - w) / 2f;
            TemplateUtils.drawText(cs, COL_HEADERS[c], tx, ty, fonts.bold, FONT_TABLE_HEADER);
        }
        cs.setNonStrokingColor(0f, 0f, 0f);
    }

    /** Fill each column band from tableTop down to tableBottom with its alternating colour. */
    private void drawColumnBands(float topY, float bottomY) throws IOException {
        PDPageContentStream cs = pc.cs();
        for (int c = 0; c < colXs.length; c++) {
            float[] col = colColors[c];
            cs.setNonStrokingColor(col[0], col[1], col[2]);
            cs.addRect(colXs[c], bottomY, colWs[c], topY - bottomY);
            cs.fill();
        }
        cs.setNonStrokingColor(0f, 0f, 0f);
    }

    // ==================== TRANSACTION ROWS (two-phase: layout, then render) ====================

    /** One rendered row: which transaction, its height and its top Y. */
    private static final class Row {
        final Transaction txn; final float height; final float topY;
        Row(Transaction txn, float height, float topY) {
            this.txn = txn; this.height = height; this.topY = topY;
        }
    }

    /** One page worth of rows plus the table geometry that frames them. */
    private static final class Page {
        boolean hasHeader;    // column-header bar drawn on page 1 only (matches the reference)
        float headerBarTop;   // top edge of the blue header bar (only when hasHeader)
        float tableTop;       // top of the row area
        float tableBottom;    // bottom edge of the last row on the page
        final List<Row> rows = new ArrayList<>();
    }

    /**
     * Phase 1 — pack every transaction into pages using identical geometry so that
     * page 2..N are visually the same and the balance band / grid stop exactly at
     * the last row (no floating colour past the data).
     */
    private List<Page> layoutPages(float chromeTop) throws IOException {
        List<Transaction> txns = stmt.transactions;
        List<Page> pages = new ArrayList<>();
        int i = 0;
        boolean firstPage = true;
        while (i < txns.size()) {
            Page pg = new Page();
            if (firstPage) {
                // Page 1: column-header bar sits just below the account block.
                pg.hasHeader = true;
                pg.headerBarTop = chromeTop;
                pg.tableTop = chromeTop - HEADER_BAR_H;
            } else {
                // Middle/last pages: no header — rows start near the top (smaller gap).
                pg.hasHeader = false;
                pg.tableTop = PAGE_H - MID_PAGE_TOP_MARGIN;
            }

            float y = pg.tableTop - 4; // top of the first row on this page
            while (i < txns.size()) {
                float h = calculateRowHeight(txns.get(i));
                if (y - h < TABLE_FLOOR && !pg.rows.isEmpty()) break; // page full
                pg.rows.add(new Row(txns.get(i), h, y));
                y -= h;
                i++;
            }
            pg.tableBottom = y;
            pages.add(pg);
            firstPage = false;
        }
        return pages;
    }

    /**
     * Phase 2 — draw each page in a fixed order: coloured column bands first,
     * then row text, then thin white separators on top.
     */
    private void renderPages(List<Page> pages) throws IOException {
        for (int p = 0; p < pages.size(); p++) {
            Page pg = pages.get(p);
            if (p > 0) pc.newPage();

            if (pg.hasHeader) drawTableHeaderBar(pg.headerBarTop);
            drawColumnBands(pg.tableTop, pg.tableBottom);
            for (Row r : pg.rows) drawTransactionRow(r.txn, r.topY);
            drawSeparators(pg);
            drawFooter();
        }
    }

    /** Thin white separators between columns (spanning header + body) and between rows. */
    private void drawSeparators(Page pg) throws IOException {
        PDPageContentStream cs = pc.cs();
        cs.setStrokingColor(C_SEP[0], C_SEP[1], C_SEP[2]);
        cs.setLineWidth(0.8f);
        float top = pg.hasHeader ? pg.headerBarTop : pg.tableTop, bot = pg.tableBottom;

        // Vertical separators at every interior column boundary (skip the outer edges).
        for (int c = 1; c < colXs.length; c++) { cs.moveTo(colXs[c], top); cs.lineTo(colXs[c], bot); }

        // Horizontal rule under the header bar, then between each body row.
        cs.moveTo(TABLE_LEFT, pg.tableTop); cs.lineTo(TABLE_RIGHT, pg.tableTop);
        for (Row r : pg.rows) {
            float rb = r.topY - r.height;
            cs.moveTo(TABLE_LEFT, rb); cs.lineTo(TABLE_RIGHT, rb);
        }
        cs.stroke();
        cs.setStrokingColor(0f, 0f, 0f);
    }

    private float calculateRowHeight(Transaction t) throws IOException {
        List<String> remarks = TemplateUtils.wrapText(nz(t.description), fonts.regular, FONT_TABLE_BODY, COL_REMARKS_W - PAD_TEXT - 1);
        List<String> utr = TemplateUtils.wrapText(utrText(t), fonts.regular, FONT_TABLE_BODY, COL_UTR_W - PAD_TEXT - 1);
        int maxLines = Math.max(2, Math.max(remarks.size(), utr.size()));
        return maxLines * LINE_HEIGHT_BODY + 6;
    }

    private void drawTransactionRow(Transaction t, float rowTopY) throws IOException {
        PDPageContentStream cs = pc.cs();
        float first = rowTopY - 8;   // ~3pt top padding below the row's top border

        // Date + time (stacked, left-aligned)
        TemplateUtils.drawText(cs, bodyDate(t.date), colDateX + PAD_TEXT, first, fonts.regular, FONT_TABLE_BODY);
        TemplateUtils.drawText(cs, bodyTime(t.date), colDateX + PAD_TEXT, first - LINE_HEIGHT_BODY, fonts.regular, FONT_TABLE_BODY);

        // Remarks (wrapped)
        float ry = first;
        for (String line : TemplateUtils.wrapText(nz(t.description), fonts.regular, FONT_TABLE_BODY, COL_REMARKS_W - PAD_TEXT - 1)) {
            TemplateUtils.drawText(cs, line, colRemarksX + PAD_TEXT, ry, fonts.regular, FONT_TABLE_BODY);
            ry -= LINE_HEIGHT_BODY;
        }

        // Tran Id-1
        TemplateUtils.drawText(cs, tranIdText(t), colTranIdX + PAD_TEXT, first, fonts.regular, FONT_TABLE_BODY);

        // UTR Number (wrapped)
        float uy = first;
        for (String line : TemplateUtils.wrapText(utrText(t), fonts.regular, FONT_TABLE_BODY, COL_UTR_W - PAD_TEXT - 1)) {
            TemplateUtils.drawText(cs, line, colUtrX + PAD_TEXT, uy, fonts.regular, FONT_TABLE_BODY);
            uy -= LINE_HEIGHT_BODY;
        }

        // Instr. ID
        if (t.instrId != null && !t.instrId.isEmpty()) {
            TemplateUtils.drawText(cs, t.instrId, colInstrIdX + PAD_TEXT, first, fonts.regular, FONT_TABLE_BODY);
        }

        // Amounts (right-aligned)
        drawRight(cs, t.debit > 0 ? formatIndian(t.debit) : "", colWithdrawalX, first, COL_WITHDRAWAL_W, fonts.regular);
        drawRight(cs, t.credit > 0 ? formatIndian(t.credit) : "", colDepositX, first, COL_DEPOSIT_W, fonts.regular);
        drawRight(cs, formatBalance(t.balance), colBalanceX, first, COL_BALANCE_W, fonts.regular);
    }

    private void drawRight(PDPageContentStream cs, String text, float x, float y, float colW, PDFont font) throws IOException {
        if (text == null || text.isEmpty()) return;
        float w = TemplateUtils.getTextWidth(text, font, FONT_TABLE_BODY);
        TemplateUtils.drawText(cs, text, x + colW - w - PAD_NUM, y, font, FONT_TABLE_BODY);
    }

    // ==================== FOOTER ====================
    // Fixed at the page bottom on every page; the table never stretches to fill the gap.
    private static final String[] FOOTER_LINES = {
            "For any queries, please get in touch with us on our 24 x 7 customer service help line no.1800 2222 44 #. Customers outside India need to dial +91 80 2530 2510.",
            "This is a system generated output and requires no signature. Customers are requested to immediately notify the Bank of any discrepancy in the statement",
            "TO AVAIL OUR LOAN PRODUCTS GIVE MISSED CALL AT 9619333333 OR SMS <ULOAN> TO 56161"
    };

    private void drawFooter() throws IOException {
        PDPageContentStream cs = pc.cs();
        // Thin blue separator caps the block; page number and disclaimers sit below it.
        cs.setStrokingColor(C_RULE[0], C_RULE[1], C_RULE[2]);
        cs.setLineWidth(0.8f);
        cs.moveTo(FOOTER_RULE_INSET, FOOTER_RULE_Y);
        cs.lineTo(PAGE_W - FOOTER_RULE_INSET, FOOTER_RULE_Y);
        cs.stroke();
        cs.setStrokingColor(0f, 0f, 0f);

        // Page number (regular, black, centered) — directly under the rule.
        drawCentered(cs, "Page No" + pc.pageNumber(), FOOTER_PAGENUM_Y, fonts.regular, 9);

        // Disclaimer block — one uniform size chosen so the longest line fits the width.
        float size = fitFooterSize(FOOTER_LINES, 7.5f, TABLE_WIDTH, 6f);
        float y = FOOTER_TEXT_TOP_Y;
        for (String line : FOOTER_LINES) {
            drawCentered(cs, line, y, fonts.regular, size);
            y -= FOOTER_LINE_GAP;
        }
    }

    /** Largest size <= desired at which every line fits maxWidth (never below min). */
    private float fitFooterSize(String[] lines, float desired, float maxWidth, float min) throws IOException {
        float scale = 1f;
        for (String s : lines) {
            float w = TemplateUtils.getTextWidth(s, fonts.regular, desired);
            if (w > maxWidth) scale = Math.min(scale, maxWidth / w);
        }
        return Math.max(min, desired * scale);
    }

    private void drawCentered(PDPageContentStream cs, String text, float y, PDFont font, float size) throws IOException {
        float w = TemplateUtils.getTextWidth(text, font, size);
        TemplateUtils.drawText(cs, text, (PAGE_W - w) / 2, y, font, size);
    }

    // ==================== FIELD HELPERS ====================
    private String tranIdText(Transaction t) {
        if (t.tranId != null && !t.tranId.isEmpty()) return t.tranId;
        return (t.reference != null && !t.reference.isEmpty()) ? t.reference : "-";
    }

    private String utrText(Transaction t) {
        if (t.utr != null && !t.utr.isEmpty()) return t.utr;
        return "-";
    }

    // ==================== FORMAT HELPERS ====================
    private String bodyDate(String iso) {
        LocalDateTime dt = parse(iso);
        return dt == null ? nz(iso) : dt.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    }
    private String bodyTime(String iso) {
        LocalDateTime dt = parse(iso);
        return dt == null ? "" : dt.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    private LocalDateTime parse(String iso) {
        if (iso == null) return null;
        try { return LocalDateTime.ofInstant(Instant.parse(iso), ZoneOffset.UTC); }
        catch (Exception e) {
            try { return LocalDateTime.parse(iso); } catch (Exception e2) { return null; }
        }
    }

    /** Balance always prints, even when the account lands on exactly zero. */
    private String formatBalance(double v) {
        return Math.abs(v) < 0.005 ? "0.00" : formatIndian(v);
    }

    /** Indian grouping, e.g. 584344.07 -> "5,84,344.07". Empty for ~0. */
    private String formatIndian(double v) {
        if (Math.abs(v) < 0.005) return "";
        boolean neg = v < 0;
        v = Math.abs(v);
        long rupees = (long) Math.floor(v + 1e-6);
        int paise = (int) Math.round((v - rupees) * 100);
        if (paise >= 100) { rupees++; paise -= 100; }
        String out = groupIndian(rupees) + String.format(".%02d", paise);
        return neg ? "-" + out : out;
    }

    private String groupIndian(long n) {
        String s = Long.toString(n);
        if (s.length() <= 3) return s;
        String last3 = s.substring(s.length() - 3);
        String rest = s.substring(0, s.length() - 3);
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = rest.length() - 1; i >= 0; i--) {
            sb.append(rest.charAt(i));
            if (++count % 2 == 0 && i != 0) sb.append(',');
        }
        return sb.reverse().append(',').append(last3).toString();
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
