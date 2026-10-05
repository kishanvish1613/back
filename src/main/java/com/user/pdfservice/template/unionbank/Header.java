package com.user.pdfservice.template.unionbank;

import com.user.pdfservice.model.Statement;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Page-1 "Statement of Account" header block:
 *  - blank logo band + title
 *  - left column: account holder, address, contact, statement date, records note
 *  - right column: bank / account details and the statement period
 *
 * Every coordinate and font size below is measured from the real UBI statement
 * and expressed in that document's own units, then scaled by {@link #S}. The
 * reference page is 782x1100; ours is A4, so REF_* values are the raw numbers
 * read out of the reference and must not be "rounded to something nicer" — they
 * are what makes this block line up with the bank's output.
 */
final class Header {

    // ---- Page geometry (mirrors UnionBankTemplate) ----
    private static final float PAGE_W = 595;
    private static final float MARGIN_TOP = 40;

    /** Reference statement's page width; all REF_* coordinates are in its units. */
    private static final float REF_W = 782;
    private static final float S = PAGE_W / REF_W;

    // ---- Reference X positions ----
    private static final float LEFT_LABEL_X = 59 * S;
    private static final float LEFT_VALUE_X = 159 * S;
    private static final float RIGHT_LABEL_X = 541 * S;
    private static final float RIGHT_VALUE_X = 634 * S;
    private static final float TITLE_X = 300 * S;
    private static final float PERIOD_X = 487 * S;
    private static final float RECORDS_X = 60 * S;
    private static final float NO_MORE_X = 210 * S;

    // ---- Reference font sizes ----
    private static final float FONT_TITLE = 20 * S;
    private static final float FONT_LABEL = 10 * S;   // labels, values and the bank name
    private static final float FONT_ADDR = 9 * S;     // C/O line and the address lines

    // ---- Reference baselines, measured from the top of the reference page ----
    private static final float REF_TITLE_Y = 111.09f;
    private static final float REF_NAME_Y = 125.54f;
    private static final float REF_BANK_Y = 130.28f;
    private static final float REF_CO_Y = 141.62f;
    private static final float REF_ADDR_Y = 156.62f;  // first address line
    private static final float REF_CITY_Y = 189.04f;
    private static final float REF_BRANCH_Y = 153.54f;
    private static final float REF_CUSTID_Y = 175.54f;
    private static final float REF_STMT_DATE_Y = 278.28f;
    private static final float REF_RECORDS_Y = 296.28f;
    private static final float REF_PERIOD_Y = 286.28f;
    private static final float REF_ROW_GAP = 15f;     // every label/value row
    private static final float REF_TABLE_TOP_Y = 322f; // top of the column-header bar

    // ---- Logo band ----
    // The reference's own logo occupies x 236..526, y 17..53.95 (its units) at a
    // 7.85 aspect. Ours is a different shape, so only the top edge carries over;
    // the image is scaled to fit inside the band and centred. Sized at draw time
    // from the file's own aspect, so swapping the PNG cannot stretch it.
    private static final String LOGO_RESOURCE = "/templates/ulogo.png";
    private static final float REF_LOGO_TOP_Y = 17f;
    private static final float LOGO_MAX_W = 300f;
    private static final float LOGO_MAX_H = 50f;

    private final Statement stmt;
    private final Fonts fonts;
    private final PDDocument doc;

    /** Title ink; the reference prints "Statement of Account" in blue, not black. */
    private static final float[] C_TITLE = {0.13f, 0.13f, 0.75f};

    Header(Statement stmt, Fonts fonts, PDDocument doc) {
        this.stmt = stmt;
        this.fonts = fonts;
        this.doc = doc;
    }

    /**
     * Draws the header starting at {@code topY} and returns the Y the transaction
     * table's header bar starts from.
     */
    float draw(PDPageContentStream cs, float topY) throws IOException {
        drawLogo(cs, topY);

        cs.setNonStrokingColor(C_TITLE[0], C_TITLE[1], C_TITLE[2]);
        TemplateUtils.drawText(cs, "Statement of Account", TITLE_X, y(topY, REF_TITLE_Y),
                fonts.regular, FONT_TITLE);
        cs.setNonStrokingColor(0f, 0f, 0f);

        drawHolderColumn(cs, topY);
        drawBankColumn(cs, topY);

        return y(topY, REF_TABLE_TOP_Y);
    }

    /** Centred logo in the band above the title. Skipped if the resource is absent. */
    private void drawLogo(PDPageContentStream cs, float topY) throws IOException {
        byte[] bytes;
        try (InputStream in = getClass().getResourceAsStream(LOGO_RESOURCE)) {
            if (in == null) return;
            bytes = in.readAllBytes();
        }
        PDImageXObject logo = PDImageXObject.createFromByteArray(doc, bytes, "ulogo");

        // Contain within the band: never upscale past the box on either axis.
        float aspect = logo.getWidth() / (float) logo.getHeight();
        float h = Math.min(LOGO_MAX_H, LOGO_MAX_W / aspect);
        float w = h * aspect;

        float bandTop = topY + MARGIN_TOP - REF_LOGO_TOP_Y * S;
        cs.drawImage(logo, (PAGE_W - w) / 2f, bandTop - h, w, h);
    }

    /** Left column: account holder / address / contact / statement date / records note. */
    private void drawHolderColumn(PDPageContentStream cs, float topY) throws IOException {
        TemplateUtils.drawText(cs, nz(stmt.details.name), LEFT_LABEL_X, y(topY, REF_NAME_Y),
                fonts.bold, FONT_LABEL);
        TemplateUtils.drawText(cs, "C/O " + nz(stmt.details.name), LEFT_LABEL_X, y(topY, REF_CO_Y),
                fonts.regular, FONT_ADDR);

        String[] addr = nz(stmt.details.address).split("[,\\n]");
        float refY = REF_ADDR_Y;
        for (int i = 0; i < Math.min(addr.length, 3); i++) {
            if (addr[i].trim().isEmpty()) continue;
            TemplateUtils.drawText(cs, addr[i].trim(), LEFT_LABEL_X, y(topY, refY), fonts.regular, FONT_ADDR);
            refY += REF_ROW_GAP;
        }

        refY = REF_CITY_Y;
        refY = drawLV(cs, topY, "City", def(stmt.details.city, "VADODARA"), LEFT_LABEL_X, LEFT_VALUE_X, refY);
        refY = drawLV(cs, topY, "State", def(stmt.details.state, "GUJARAT"), LEFT_LABEL_X, LEFT_VALUE_X, refY);
        refY = drawLV(cs, topY, "Country", "INDIA", LEFT_LABEL_X, LEFT_VALUE_X, refY);
        refY = drawLV(cs, topY, "Zip", def(stmt.details.pincode, "390023"), LEFT_LABEL_X, LEFT_VALUE_X, refY);
        refY = drawLV(cs, topY, "Mobile No", nz(stmt.details.phoneNumber), LEFT_LABEL_X, LEFT_VALUE_X, refY);
        drawLV(cs, topY, "E-mail", nz(stmt.details.email), LEFT_LABEL_X, LEFT_VALUE_X, refY);

        TemplateUtils.drawText(cs, "Statement Date : " + headerDateTime(LocalDateTime.now()),
                LEFT_LABEL_X, y(topY, REF_STMT_DATE_Y), fonts.regular, FONT_LABEL);

        // The reference prints these as two runs at fixed columns, not one sentence.
        TemplateUtils.drawText(cs, "Records from 1 to " + stmt.transactions.size() + ".",
                RECORDS_X, y(topY, REF_RECORDS_Y), fonts.regular, FONT_LABEL);
        TemplateUtils.drawText(cs, "No more records available.",
                NO_MORE_X, y(topY, REF_RECORDS_Y), fonts.regular, FONT_LABEL);
    }

    /** Right column: bank / account details and the statement period. */
    private void drawBankColumn(PDPageContentStream cs, float topY) throws IOException {
        TemplateUtils.drawText(cs, "Union Bank of India", RIGHT_LABEL_X, y(topY, REF_BANK_Y),
                fonts.regular, FONT_LABEL);

        drawLV(cs, topY, "Branch", branchLabel(), RIGHT_LABEL_X, RIGHT_VALUE_X, REF_BRANCH_Y);

        // Branch sits apart from the rest of the block in the reference.
        float refY = REF_CUSTID_Y;
        refY = drawLV(cs, topY, "Customer Id", nz(stmt.details.customerRelNo), RIGHT_LABEL_X, RIGHT_VALUE_X, refY);
        refY = drawLV(cs, topY, "Account No", nz(stmt.details.accountNumber), RIGHT_LABEL_X, RIGHT_VALUE_X, refY);
        refY = drawLV(cs, topY, "Account Currency", "INR", RIGHT_LABEL_X, RIGHT_VALUE_X, refY);
        refY = drawLV(cs, topY, "Account Type", def(stmt.details.accountType, "Current Account"), RIGHT_LABEL_X, RIGHT_VALUE_X, refY);
        refY = drawLV(cs, topY, "MICR Code", nz(stmt.details.micr), RIGHT_LABEL_X, RIGHT_VALUE_X, refY);
        refY = drawLV(cs, topY, "IFSC Code", nz(stmt.details.ifsc), RIGHT_LABEL_X, RIGHT_VALUE_X, refY);
        drawLV(cs, topY, "CKYC Number", nz(stmt.details.ckycr), RIGHT_LABEL_X, RIGHT_VALUE_X, refY);

        TemplateUtils.drawText(cs, "Statement Period From -" + headerDate(stmt.meta.statementPeriodStart)
                        + "  To " + headerDate(stmt.meta.statementPeriodEnd),
                PERIOD_X, y(topY, REF_PERIOD_Y), fonts.regular, FONT_LABEL);
    }

    /** One bold-label / regular-value row; returns the next row's reference Y. */
    private float drawLV(PDPageContentStream cs, float topY, String label, String value,
                         float labelX, float valueX, float refY) throws IOException {
        float baseline = y(topY, refY);
        TemplateUtils.drawText(cs, label, labelX, baseline, fonts.bold, FONT_LABEL);
        TemplateUtils.drawText(cs, nz(value), valueX, baseline, fonts.regular, FONT_LABEL);
        return refY + REF_ROW_GAP;
    }

    /** Reference Y (measured down from the page top) -> PDF baseline on our page. */
    private float y(float topY, float refY) {
        return topY + MARGIN_TOP - refY * S;
    }

    private String branchLabel() {
        String b = def(stmt.details.branch, "");
        if (b.toUpperCase().contains("PBB")) return b;
        return b.isEmpty() ? "- PBB" : b + " - PBB";
    }

    // ==================== FORMAT HELPERS ====================
    private String headerDate(String iso) {
        LocalDateTime dt = parse(iso);
        return dt == null ? nz(iso) : dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String headerDateTime(LocalDateTime dt) {
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private LocalDateTime parse(String iso) {
        if (iso == null) return null;
        try { return LocalDateTime.ofInstant(Instant.parse(iso), ZoneOffset.UTC); }
        catch (Exception e) {
            try { return LocalDateTime.parse(iso); } catch (Exception e2) { return null; }
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }
    private static String def(String s, String d) { return (s == null || s.isEmpty()) ? d : s; }
}
