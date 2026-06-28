package com.user.pdfservice.template.sbi;

import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.Transaction;
import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
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
                Image logo = Image.getInstance("sbi.jpg");

                logo.scaleToFit(242f * 0.75f, 72f * 0.75f);

                float x = 36f;
                float y = 806;

                logo.setAbsolutePosition(x, y - logo.getScaledHeight());
                cb.addImage(logo);

                // return logo.getScaledHeight();
        }

        private void addHeaderInfo(PdfWriter writer, Fonts fonts, Statement stmt) {

                PdfContentByte cb = writer.getDirectContent();
                BaseFont bf = fonts.body.getBaseFont();

                cb.beginText();

                // EXACT SBI ANCHOR
                cb.moveText(36, 806);
                cb.moveText(0, -54);
                cb.moveText(0, -13.5f);

                cb.setFontAndSize(bf, 9);

                final int LABEL_WIDTH = 30;
                final float LH = 13.5f;

                // helper
                java.util.function.Consumer<String> line = text -> {
                        cb.showText(text);
                        cb.moveText(0, -LH);
                };

                line.accept(pad("Account Name", LABEL_WIDTH) +
                                " : " + stmt.details.name);

                String[] addr = stmt.details.address.split("\\r?\\n");

                line.accept(pad("Address", LABEL_WIDTH + 5) +
                                " :  " + addr[0]);

                for (int i = 1; i < addr.length; i++) {
                        line.accept(pad("", LABEL_WIDTH + 12) + "   " + addr[i]);
                }

                line.accept(pad("Date", LABEL_WIDTH + 9) +
                                " : " + format(stmt.meta.generatedAt));

                String acct = String.format("%17s", stmt.details.accountNumber)
                                .replace(' ', '0');
                line.accept(pad("Account Number", LABEL_WIDTH) +
                                " : " + acct);

                line.accept(pad("Account Description", LABEL_WIDTH) +
                                " : " + stmt.details.accountType);

                line.accept(pad("Branch", LABEL_WIDTH + 7) +
                                " : " + (stmt.details.branch != null
                                                ? stmt.details.branch
                                                : stmt.details.branch));

                line.accept(pad("Drawing Power", LABEL_WIDTH + 1) +
                                " : 0.00");

                line.accept(pad("Interest Rate(% p.a.)", LABEL_WIDTH) +
                                " : 2.5");

                line.accept(pad("MOD Balance", LABEL_WIDTH + 1) +
                                " : 0.00");

                line.accept(pad("CIF No.", LABEL_WIDTH + 7) +
                                " : " + stmt.details.customerRelNo);

                line.accept(pad("CKYCR Number", LABEL_WIDTH + 1) + " :"
                                + (stmt.details.cykr != null ? stmt.details.cykr : ""));

                line.accept(pad("IFS Code", LABEL_WIDTH + 5) +
                                " :" + stmt.details.ifsc);

                line.accept("(Indian Financial System)");

                line.accept(pad("MICR Code", LABEL_WIDTH + 3) +
                                " : " + stmt.details.micr);

                line.accept("(Magnetic Ink Character Recognition)");

                line.accept(pad("Nomination Registered", LABEL_WIDTH - 3) +
                                " : No");

                BigDecimal openingBalance = computeOpeningBalance(stmt.transactions);

                cb.showText(pad("Balance as on " + format(stmt.meta.statementPeriodStart), LABEL_WIDTH - 5) +
                                " : " + formatAmount(openingBalance));

                cb.moveText(0, -18);
                cb.setFontAndSize(bf, 12);
                cb.showText(" ");
                cb.moveText(0, -18);
                cb.showText("Account Statement from " + format(stmt.meta.statementPeriodStart) + " to "
                                + format(stmt.meta.statementPeriodEnd));
                cb.moveText(0, -18);
                cb.showText(" ");
                cb.endText();
        }

        private String pad(String s, int width) {
                if (s.length() >= width)
                        return s;
                return s + " ".repeat(width - s.length());
        }

        private static final DateTimeFormatter SBI_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

        private BigDecimal computeOpeningBalance(List<Transaction> txns) {
                if (txns == null || txns.isEmpty()) {
                        return BigDecimal.ZERO;
                }

                Transaction first = txns.get(0);

                return BigDecimal.valueOf(first.balance)
                                .add(BigDecimal.valueOf(first.debit))
                                .subtract(BigDecimal.valueOf(first.credit));
        }

        private String format(String iso) {
                return OffsetDateTime.parse(iso).format(SBI_FORMAT);
        }

        private String formatAmount(BigDecimal value) {
                boolean negative = value.signum() < 0;
                value = value.abs();

                String[] parts = value
                                .setScale(2, RoundingMode.HALF_UP)
                                .toPlainString()
                                .split("\\.");

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

                if (!rest.isEmpty()) {
                        sb.insert(0, rest);
                }

                sb.append(",").append(last3).append(".").append(parts[1]);

                return (negative ? "-" : "") + sb.toString();
        }

}