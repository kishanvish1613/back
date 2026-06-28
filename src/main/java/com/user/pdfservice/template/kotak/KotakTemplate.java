package com.user.pdfservice.template.kotak;

import java.io.ByteArrayOutputStream;

import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.StatementUserDetails;
import com.user.pdfservice.template.BankPdfTemplate;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PdfAConformance;

public class KotakTemplate implements BankPdfTemplate {

    @Override
    public byte[] generate(Statement stmt) throws Exception {
        sanitize(stmt);
        String html = KotakHtmlBuilder.build(stmt);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.usePdfAConformance(PdfAConformance.NONE);
        builder.withHtmlContent(html, KotakTemplate.class.getResource("/templates/").toExternalForm());
        builder.toStream(out);
        builder.run();

        byte[] rawPdf = out.toByteArray();

        byte[] finalPdf = PdfHeaderAdder.addHeader(rawPdf, "/templates/kotak_header.png");

        return finalPdf;
    }

    private void sanitize(Statement stmt){
        if(stmt == null || stmt.details == null) return;

        StatementUserDetails d = stmt.details;

        d.name = clean(d.name);
        d.address = formatAddress(clean(d.address));
        d.branch = upper(clean(d.branch));
        d.state = upper(clean(d.state));
        d.branchAddress = formatAddress(clean(d.branchAddress));
    }

    private String clean(String value) {
        if (value == null)
            return "";
        return value
                .replace("\r", "")
                .replaceAll("[ \t]+", " ")
                .trim();
    }

    private String formatAddress(String address) {
        if (address == null)
            return "";

        String[] lines = address
                .replace("\r", "")
                .split("\n");

        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            String cleaned = upper(clean(line));
            if (!cleaned.isEmpty()) {
                if (result.length() > 0) {
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

    private String toTitleCase(String value) {
    value = clean(value);
    if (value.isEmpty()) return value;

    String[] parts = value.toLowerCase().split(" ");
    StringBuilder result = new StringBuilder();

    for (String part : parts) {
        if (part.isEmpty()) continue;

        result.append(Character.toUpperCase(part.charAt(0)))
              .append(part.substring(1))
              .append(" ");
    }

    return result.toString().trim();
}

}
