package com.user.pdfservice.template.sbi2;

import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.Transaction;
import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfImage;
import com.lowagie.text.pdf.PdfIndirectObject;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

public class SbiFirstPageHeaderEvent extends PdfPageEventHelper {

        private final Fonts fonts;
        private final Statement stmt;

        public static final float EXTRA_TOP_GAP = 13.5f;
        // Adjust this value to change the gap between the logo and the account details.
        // Larger value → header starts lower → larger gap; smaller value → smaller gap.
        private static final float HEADER_START_Y = 750f;

        // Internal use only – can be removed, no external callers needed now.
        private float headerEndY;
        private float headerBottomY;

        public SbiFirstPageHeaderEvent(Fonts fonts, Statement stmt) {
                this.fonts = fonts;
                this.stmt = stmt;
        }

        @Override
        public void onStartPage(PdfWriter writer, Document document) {
                if (writer.getPageNumber() == 1) {
                        try {
                                drawLogo(writer);
                                addHeaderInfo(writer, fonts, stmt);
                                addAccountStatement(writer, fonts, stmt, headerEndY);
                        } catch (Exception e) {
                                throw new RuntimeException("Error drawing SBI header", e);
                        }
                }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
                if (writer.getPageNumber() == 1) {
                        document.setMargins(
                                document.leftMargin(),
                                document.rightMargin(),
                                36f,
                                document.bottomMargin());
                }
        }

        private void drawLogo(PdfWriter writer) throws Exception {
                PdfContentByte cb = writer.getDirectContent();
                final int WIDTH = 242;
                final int HEIGHT = 72;
                final float SCALE = 0.70f;
                final float LEFT = 50f;
                final float TOP_Y = 801f;

                byte[] rgb = Files.readAllBytes(Paths.get("image.rgb"));
                byte[] alpha = Files.readAllBytes(Paths.get("smask.gray"));

                Image img = Image.getInstance(WIDTH, HEIGHT, 3, 8, rgb);
                img.setAbsolutePosition(LEFT, TOP_Y - (HEIGHT * SCALE));
                img.scaleToFit(WIDTH * SCALE, HEIGHT * SCALE);
                img.setTransparency(new int[] { 0, 0 });

                PdfImage pdfImg = new PdfImage(img, "", null);
                PdfDictionary calRgbDict = new PdfDictionary(PdfName.CALRGB);
                calRgbDict.put(PdfName.GAMMA, new PdfArray(new float[] { 2.2f, 2.2f, 2.2f }));
                calRgbDict.put(PdfName.WHITEPOINT, new PdfArray(new float[] { 0.95043f, 1f, 1.09f }));
                calRgbDict.put(PdfName.MATRIX, new PdfArray(new float[] {
                        0.41239f, 0.21264f, 0.01933f,
                        0.35758f, 0.71517f, 0.11919f,
                        0.18045f, 0.07218f, 0.9504f
                }));
                PdfArray calRgb = new PdfArray();
                calRgb.add(PdfName.CALRGB);
                calRgb.add(calRgbDict);
                pdfImg.put(PdfName.COLORSPACE, calRgb);
                pdfImg.put(PdfName.INTENT, PdfName.PERCEPTUAL);
                pdfImg.put(PdfName.FILTER, PdfName.FLATEDECODE);

                Image maskImg = Image.getInstance(WIDTH, HEIGHT, 1, 8, alpha);
                PdfImage smask = new PdfImage(maskImg, "", null);
                smask.put(PdfName.COLORSPACE, PdfName.DEVICEGRAY);
                smask.put(PdfName.FILTER, PdfName.FLATEDECODE);
                PdfIndirectObject smaskRef = writer.addToBody(smask);
                pdfImg.put(PdfName.SMASK, smaskRef.getIndirectReference());

                PdfIndirectObject imgRef = writer.addToBody(pdfImg);
                img.setDirectReference(imgRef.getIndirectReference());
                cb.addImage(img);
        }

        private void addHeaderInfo(PdfWriter writer, Fonts fonts, Statement stmt) {
                PdfContentByte cb = writer.getDirectContent();
                BaseFont bf = fonts.body.getBaseFont();
                final float fontSize = 9f;
                final float lineHeight = 13.5f;
                final float leftMargin = 36f;
                final float gap = 3f;

                String[] allLabels = {
                        "Account Name", "Address", "Date", "Account Number",
                        "Account Description", "Branch", "Drawing Power",
                        "Interest Rate(% p.a.)", "MOD Balance", "CIF No.",
                        "CKYCR Number", "IFS Code", "MICR Code", "Nomination Registered"
                };
                String balanceLabel = "Balance as on " + format(stmt.meta.statementPeriodStart);

                float maxLabelW = 0f;
                for (String lbl : allLabels) {
                        float w = bf.getWidthPoint(lbl, fontSize);
                        if (w > maxLabelW) maxLabelW = w;
                }
                float balLabelW = bf.getWidthPoint(balanceLabel, fontSize);
                if (balLabelW > maxLabelW) maxLabelW = balLabelW;

                float colonW = bf.getWidthPoint(":", fontSize);
                // Base colon position – identical calculation as before
                float defaultColonX = leftMargin + maxLabelW;

                // Use HEADER_START_Y constant to control gap between logo and details
                final float[] y = { HEADER_START_Y - EXTRA_TOP_GAP };

                // Reusable method for drawing one row
                java.util.function.Consumer<String> drawNote = (note) -> {
                        cb.beginText();
                        cb.setFontAndSize(bf, fontSize);
                        cb.setTextMatrix(leftMargin, y[0]);
                        cb.showText(note);
                        cb.endText();
                        y[0] -= lineHeight;
                };

                // ------------------- ROWS -------------------
                // Each call now supplies its own colon shift.
                // Shift = 5f keeps the original PDF appearance unchanged.

                drawRow(cb, bf, fontSize, leftMargin, defaultColonX, colonW, gap, lineHeight, y,
                        "Account Name", cleanValue(stmt.details.name), 5f);

                // Address splitting: now splits on both newlines and commas.
                // Commas are removed and each part becomes a new line.
                String[] addr = stmt.details.address != null
                        ? stmt.details.address.split("[\\r\\n,]+")
                        : new String[0];
                String firstAddrLine = (addr.length > 0 && !addr[0].isBlank()) ? cleanValue(addr[0]) : "";
                float addrValueX = drawRow(cb, bf, fontSize, leftMargin, defaultColonX, colonW, gap, lineHeight, y,
                        "Address", firstAddrLine, 4f);

                // Remaining address lines indented under the value column of the Address row
                for (int i = 1; i < addr.length; i++) {
                        String line = addr[i];
                        if (line != null && !line.isBlank()) {
                                cb.beginText();
                                cb.setFontAndSize(bf, fontSize);
                                cb.setTextMatrix(addrValueX, y[0]);
                                cb.showText(cleanValue(line));
                                cb.endText();
                                y[0] -= lineHeight;
                        }
                }

                drawRow(cb, bf, fontSize, leftMargin, defaultColonX, colonW, gap, lineHeight, y,
                        "Date", format(stmt.meta.generatedAt), 6f);

                drawRow(cb, bf, fontSize, leftMargin, defaultColonX, colonW, gap, lineHeight, y,
                        "Account Number",
                        cleanValue(String.format("%17s", stmt.details.accountNumber).replace(' ', '0')), 5f);

                drawRow(cb, bf, fontSize, leftMargin, defaultColonX, colonW, gap, lineHeight, y,
                        "Account Description", cleanValue(stmt.details.accountType), 6f);

                drawRow(cb, bf, fontSize, leftMargin, defaultColonX, colonW, gap, lineHeight, y,
                        "Branch", cleanValue(stmt.details.branch), 4f);

                drawRow(cb, bf, fontSize, leftMargin, defaultColonX, colonW, gap, lineHeight, y,
                        "Drawing Power", "0.00", 5f);

                drawRow(cb, bf, fontSize, leftMargin, defaultColonX, colonW, gap, lineHeight, y,
                        "Interest Rate(% p.a.)", "2.5", 3f);

                drawRow(cb, bf, fontSize, leftMargin, defaultColonX, colonW, gap, lineHeight, y,
                        "MOD Balance", "0.00", 5f);

                drawRow(cb, bf, fontSize, leftMargin, defaultColonX, colonW, gap, lineHeight, y,
                        "CIF No.", cleanValue(stmt.details.customerRelNo), 5f);

                drawRow(cb, bf, fontSize, leftMargin, defaultColonX, colonW, gap, lineHeight, y,
                        "CKYCR Number",
                        stmt.details.ckycr != null ? cleanValue(stmt.details.ckycr) : "NOT AVAILABLE", 14f);

                drawRow(cb, bf, fontSize, leftMargin, defaultColonX, colonW, gap, lineHeight, y,
                        "IFS Code", cleanValue(stmt.details.ifsc), 5f);
                drawNote.accept("(Indian Financial System)");

                drawRow(cb, bf, fontSize, leftMargin, defaultColonX, colonW, gap, lineHeight, y,
                        "MICR Code", cleanValue(stmt.details.micr), 6f);
                drawNote.accept("(Magnetic Ink Character Recognition)");

                drawRow(cb, bf, fontSize, leftMargin, defaultColonX, colonW, gap, lineHeight, y,
                        "Nomination Registered", "No", 4f);

                BigDecimal openingBalance = computeOpeningBalance(stmt.transactions);
                drawRow(cb, bf, fontSize, leftMargin, defaultColonX, colonW, gap, lineHeight, y,
                        balanceLabel, formatAmount(openingBalance), 5f);

                headerEndY = y[0];
        }

        /**
         * Draws a single header row (label : value).
         *
         * @param defaultColonX base x-position of the colon (leftMargin + max label width)
         * @param colonShift    per‑row adjustment of the colon position (can be positive or negative)
         * @return the x‑coordinate where the value was drawn (useful for continuation lines)
         */
        private float drawRow(PdfContentByte cb, BaseFont bf, float fontSize,
                              float leftMargin, float defaultColonX, float colonW, float gap,
                              float lineHeight, float[] y,
                              String label, String value, float colonShift) {
                float colonX = defaultColonX + colonShift;
                float valueX = colonX + colonW + gap;

                // Draw label
                cb.beginText();
                cb.setFontAndSize(bf, fontSize);
                cb.setTextMatrix(leftMargin, y[0]);
                cb.showText(label);
                cb.endText();

                // Draw colon
                cb.beginText();
                cb.setFontAndSize(bf, fontSize);
                cb.setTextMatrix(colonX, y[0]);
                cb.showText(":");
                cb.endText();

                // Draw value
                cb.beginText();
                cb.setFontAndSize(bf, fontSize);
                cb.setTextMatrix(valueX, y[0]);
                cb.showText(value);
                cb.endText();

                y[0] -= lineHeight;
                return valueX;
        }

        private void addAccountStatement(PdfWriter writer, Fonts fonts, Statement stmt, float startY) {
                PdfContentByte cb = writer.getDirectContent();
                BaseFont bf = fonts.body.getBaseFont();
                cb.beginText();

                float yPos = startY - 0f;

                cb.setTextMatrix(36, yPos);
                cb.setFontAndSize(bf, 12);
                cb.showText(" ");
                yPos -= 18f;
                cb.setTextMatrix(36, yPos);
                cb.showText("Account Statement from " + format(stmt.meta.statementPeriodStart) + " to " + format(stmt.meta.statementPeriodEnd));
                yPos -= 18f;
                cb.setTextMatrix(36, yPos);
                cb.showText(" ");
                yPos -= 0f;
                cb.setFontAndSize(bf, 9);
                cb.setTextMatrix(36, yPos);
                cb.showText("\t");
                cb.endText();

                headerBottomY = yPos;   // unused externally now
        }

        private static String cleanValue(String raw) {
                if (raw == null) return "";
                return raw.replaceFirst("^[^\\w\\s]+", "").trim();
        }

        private BigDecimal computeOpeningBalance(List<Transaction> txns) {
                if (txns == null || txns.isEmpty()) return BigDecimal.ZERO;
                Transaction first = txns.get(0);
                return BigDecimal.valueOf(first.balance)
                        .add(BigDecimal.valueOf(first.debit))
                        .subtract(BigDecimal.valueOf(first.credit));
        }

        private static final DateTimeFormatter SBI_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

        private String format(String iso) {
                return OffsetDateTime.parse(iso).format(SBI_FORMAT);
        }

        private String formatAmount(BigDecimal value) {
                boolean negative = value.signum() < 0;
                value = value.abs();
                String[] parts = value.setScale(2, RoundingMode.HALF_UP).toPlainString().split("\\.");
                String number = parts[0];
                if (number.length() <= 3) {
                        return (negative ? "-" : "") + number + "." + parts[1];
                }
                String last3 = number.substring(number.length() - 3);
                String rest = number.substring(0, number.length() - 3);
                StringBuilder sb = new StringBuilder();
                while (rest.length() > 2) {
                        sb.insert(0, "," + rest.substring(rest.length() - 2));
                        rest = rest.substring(0, rest.length() - 2);
                }
                if (!rest.isEmpty()) sb.insert(0, rest);
                sb.append(",").append(last3).append(".").append(parts[1]);
                return (negative ? "-" : "") + sb.toString();
        }
}