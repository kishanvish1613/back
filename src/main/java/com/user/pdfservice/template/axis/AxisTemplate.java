package com.user.pdfservice.template.axis;

import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.StatementUserDetails;
import com.user.pdfservice.template.BankPdfTemplate;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class AxisTemplate implements BankPdfTemplate {

    @Override
    public byte[] generate(Statement stmt) throws Exception {
        sanitize(stmt);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        
        Date now = new Date();
        String pdfDate = toPdfDateIST(now);
        
        writer.getInfo().put(
            PdfName.CREATIONDATE,
            new PdfString(pdfDate)
        );
        
        writer.getInfo().put(
            PdfName.MODDATE,
            new PdfString(pdfDate)
        );

        writer.getInfo().put(
                PdfName.PRODUCER,
                new PdfString("iText 2.1.7 by 1T3XT"));

        Fonts fonts = new Fonts();

        doc.open();
        addAxisHeaderExact(writer, fonts, stmt);

        AxisTransactionRenderer tx = new AxisTransactionRenderer(stmt);
        tx.render(doc, writer, fonts);
        doc.close();

        byte[] rawPdf = out.toByteArray();
        byte[] pdf = PdfHeaderAdder.addHeader(rawPdf, "/templates/axis2.png");

        return pdf;
    }
    
    public static String toPdfDateIST(Date date) {
        TimeZone ist = TimeZone.getTimeZone("GMT+05:30");
        Calendar cal = Calendar.getInstance(ist);
        cal.setTime(date);
    
        return String.format(
            "D:%04d%02d%02d%02d%02d%02d+05'30'",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            cal.get(Calendar.SECOND)
        );
    }

    private void addAxisHeaderExact(PdfWriter writer, Fonts fonts, Statement stmt) {
        PdfContentByte cb = writer.getDirectContent();

        // Fonts
        BaseFont f2 = fonts.timesBold.getBaseFont(); // /F2
        BaseFont f3 = fonts.timesRoman.getBaseFont(); // /F3
        BaseFont f1 = fonts.helvetica.getBaseFont();

        float leftX = 38f;

        cb.beginText();
        cb.setFontAndSize(f1, 12); // Helvetica
        cb.setTextMatrix(36, 806); // off-content header area
        cb.showText("          "); // single space = invisible
        cb.endText();

        // ---------------- NAME ----------------
        writeUnderlinedText(cb, f2, 9, leftX, 773, stmt.details.name);

        // ---------------- ADDRESS BLOCK ----------------
        writeAddressBlock(cb, f3, 9, leftX, 760f, "Joint Holder :- -");
        writeAddressBlock(cb, f3, 9, leftX, 747f, stmt.details.address);

        // ---------------- RIGHT COLUMN ----------------
        text(cb, f3, 9, 465f, 708, "Customer ID :" + stmt.details.customerRelNo);

        // text(cb, f3, 9, leftX, 695, stmt.details.state + "-INDIA");
        text(cb, f3, 9, 459.49f, 695, "IFSC Code :" + stmt.details.ifsc);

        // text(cb, f3, 9, leftX, 682, stmt.details.pincode);
        text(cb, f3, 9, 467.49f, 682, "MICR Code :" + stmt.details.micr);

        text(cb, f3, 9, 469.76f, 669, "Nominee Registered : N");

        text(cb, f3, 9, leftX, 656, "Registered Mobile No :"
                + (stmt.details.phoneNumber != null ? stmt.details.phoneNumber : "9876543210"));
        text(cb, f3, 9, leftX, 643,
                "Registered Email ID:" + (stmt.details.email != null ? stmt.details.email : "RAXXXX15@GMAIL.COM"));

        text(cb, f3, 9, 483.25f, 643, "PAN :" + (stmt.details.pan != null ? stmt.details.pan : "ITEST5954M"));

        text(cb, f3, 9, leftX, 630, "Scheme :"
                + (stmt.details.accountType != null ? stmt.details.accountType : "PRESTIGE SAVINGS ACCOUNT"));
        text(cb, f3, 9, 403.51f, 630, "CKYC NUMBER :XXXXXXXXXX8315");

        // ---------------- STATEMENT TITLE ----------------
        text(cb, f2, 10, 82.4f, 612,
                "Statement of Axis Account No :" + stmt.details.accountNumber + " " +
                        "for the period (From : " + formatDate(stmt.meta.statementPeriodStart) + "  To : "
                        + formatDate(stmt.meta.statementPeriodEnd) + ")");
    }

    private void text(
            PdfContentByte cb,
            BaseFont font,
            float size,
            float x,
            float y,
            String value) {

        cb.beginText();
        cb.setFontAndSize(font, size);
        cb.setTextMatrix(x, y);
        cb.showText(value);
        cb.endText();
    }

    private void writeUnderlinedText(
            PdfContentByte cb,
            BaseFont font,
            float fontSize,
            float x,
            float y,
            String text) {

        // Write text
        cb.beginText();
        cb.setFontAndSize(font, fontSize);
        cb.setTextMatrix(x, y);
        cb.showText(text);
        cb.endText();

        // Calculate text width
        float textWidth = font.getWidthPoint(text, fontSize);

        // Draw underline
        cb.setLineWidth(0.5f);
        cb.moveTo(x, y - 3); // underline position
        cb.lineTo(x + textWidth, y - 3);
        cb.stroke();
    }

    private void writeAddressBlock(
            PdfContentByte cb,
            BaseFont font,
            float fontSize,
            float x,
            float startY,
            String address) {

        if (address == null || address.trim().isEmpty()) {
            return;
        }

        float y = startY;
        float leading = 13f; // EXACT Axis spacing

        String[] lines = address.split("\\r?\\n");

        cb.beginText();
        cb.setFontAndSize(font, fontSize);

        for (String line : lines) {
            cb.setTextMatrix(x, y);
            cb.showText(line.trim());
            y -= leading;
        }

        cb.endText();
    }

    private static String formatDate(String rawDate) {
        try {
            Instant instant = Instant.parse(rawDate);

            DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                    .withZone(ZoneOffset.UTC);
            return outputFmt.format(instant);
        } catch (Exception e) {
            // fallback – never break PDF generation
            return rawDate;
        }
    }

    private void sanitize(Statement stmt) {
        if (stmt == null || stmt.details == null)
            return;

        StatementUserDetails d = stmt.details;

        d.name = upper(d.name);
        d.address = formatAddress(d.address, false);
        d.branch = upper(d.branch);
        d.state = upper(d.state);
        d.branchAddress = formatAddress(d.branchAddress, true);
    }

    private String formatAddress(String address, boolean oneline) {
        if (address == null)
            return "";

        String[] lines = address
                .replace("\r", "")
                .split("\n");

        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            String cleaned = upper(line);
            if (!cleaned.isEmpty()) {
                if (result.length() > 0 && !oneline) {
                    result.append("\n");
                }
                result.append(cleaned);
            }
        }
        return result.toString();
    }

    private String upper(String value) {
        return value.isEmpty() ? value : value.toUpperCase();
    }

}
