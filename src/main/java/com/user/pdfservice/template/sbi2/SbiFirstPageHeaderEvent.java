package com.user.pdfservice.template.sbi2;

import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.BiConsumer;

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
                                addAccountStatement(writer, fonts, stmt);
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
                final float SCALE = 0.75f;
                final float LEFT = 36f;
                final float TOP_Y = 806f;

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
                cb.setFontAndSize(bf, 9);

                BiConsumer<Float, String> left = (y, text) -> {
                        cb.beginText();
                        cb.setTextMatrix(36f, y);
                        cb.showText(text);
                        cb.endText();
                };
                BiConsumer<Float, String> right = (y, text) -> {
                        cb.beginText();
                        cb.setTextMatrix(185.43f, y);
                        cb.showText(text);
                        cb.endText();
                };

                final float LINE = 13.5f;
                float y = 738.5f;

                left.accept(y, "Account Name");
                right.accept(y, ":\t" + cleanValue(stmt.details.name));
                y -= LINE;

                left.accept(y, "Address");
                String[] addr = stmt.details.address.split("\\r?\\n");
                for (int i = 0; i < addr.length; i++) {
                        if (addr[i] != null && !addr[i].isBlank()) {
                                right.accept(y, (i == 0 ? ":\t" : " \t") + cleanValue(addr[i]));
                                y -= LINE;
                        }
                }

                left.accept(y, "Date");
                right.accept(y, ":\t" + format(stmt.meta.generatedAt));
                y -= LINE;

                left.accept(y, "Account Number");
                right.accept(y, ":\t" + cleanValue(
                        String.format("%17s", stmt.details.accountNumber).replace(' ', '0')));
                y -= LINE;

                left.accept(y, "Account Description");
                right.accept(y, ":\t" + cleanValue(stmt.details.accountType));
                y -= LINE;

                left.accept(y, "Branch");
                right.accept(y, ":\t" + cleanValue(stmt.details.branch));
                y -= LINE;

                left.accept(y, "Drawing Power");
                right.accept(y, ":\t0.00");
                y -= LINE;

                left.accept(y, "Interest Rate(% p.a.)");
                right.accept(y, ":\t2.5");
                y -= LINE;

                left.accept(y, "MOD Balance");
                right.accept(y, ":\t0.00");
                y -= LINE;

                left.accept(y, "CIF No.");
                right.accept(y, ":\t" + cleanValue(stmt.details.customerRelNo));
                y -= LINE;

                left.accept(y, "CKYCR Number");
                right.accept(y, ":\t" + (stmt.details.ckycr != null ? cleanValue(stmt.details.ckycr) : "NOT AVAILABLE"));
                y -= LINE;

                left.accept(y, "IFS Code");
                right.accept(y, ":\t" + cleanValue(stmt.details.ifsc));
                y -= LINE;

                left.accept(y, "MICR Code");
                right.accept(y, ":\t" + cleanValue(stmt.details.micr));
                y -= LINE;

                left.accept(y, "Nomination Registered");
                right.accept(y, ":\tNo");
                y -= LINE;

                BigDecimal openingBalance = computeOpeningBalance(stmt.transactions);
                left.accept(y, "Balance as on " + format(stmt.meta.statementPeriodStart));
                right.accept(y, ":\t" + formatAmount(openingBalance));
        }

        private void addAccountStatement(PdfWriter writer, Fonts fonts, Statement stmt) {
                PdfContentByte cb = writer.getDirectContent();
                BaseFont bf = fonts.body.getBaseFont();
                cb.beginText();
                int addrLines = stmt.details.address.split("\\r?\n").length;
                final float LINE = 13.5f;
                final int BASE_ADDR_LINES = 4;
                final float BASE_OFFSET = -243f;
                float addrShift = (addrLines - BASE_ADDR_LINES) * LINE;
                cb.moveText(36, 806);
                cb.moveText(0, -54);
                cb.moveText(0, BASE_OFFSET - addrShift);
                cb.moveText(0, -18);
                cb.setFontAndSize(bf, 12);
                cb.showText(" ");
                cb.moveText(0, -18);
                cb.showText("Account Statement from " + format(stmt.meta.statementPeriodStart) + " to " + format(stmt.meta.statementPeriodEnd));
                cb.moveText(0, -18);
                cb.showText(" ");
                cb.moveText(0, -13.5f);
                cb.setFontAndSize(bf, 9);
                cb.showText("\t");
                cb.endText();
        }

        private static String cleanValue(String raw) {
                if (raw == null) return "";
                // remove any leading non-alphanumeric characters (like '&')
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