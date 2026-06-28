package com.user.pdfservice.ocr.extractors;

import java.util.regex.*;

public class NameExtractor {

    public static String extract(String text, String bank) {
        if (text == null)
            return null;

        if(bank.equalsIgnoreCase("AXIS")){
            return extractAxis(text);
        } else if(bank.equalsIgnoreCase("BOI")){
            return extractBoi(text);
        } else if(bank.equalsIgnoreCase("HDFC")){
            return extractHdfc(text);
        }

        Matcher labeled = Pattern.compile(
                "(WELCOME|NAME|CUSTOMER|ACCOUNT HOLDER|ACCOUNT\\s+NAME)\\s*[:\\-]\\s*([^\r\n]+)",
                Pattern.CASE_INSENSITIVE).matcher(text);

        if (labeled.find()) {
            return clean(labeled.group(2));
        }

        // Kotak-style fallback (no label)
        if ("KOTAK".equals(bank)) {
            Matcher m = Pattern.compile(
                    "\\n(MR|MRS|MS)?\\s*([A-Z][A-Z\\s]{5,40})\\n",
                    Pattern.CASE_INSENSITIVE).matcher(text);

            if (m.find()) {
                return clean(m.group(2));
            }
        }

        return null;
    }
    
    public static String extractHdfc(String text) {
        String normalized = text.replaceAll("\r\n", "\n").replaceAll("\r", "\n");

        Pattern p = Pattern.compile(
            "(?i)Page No\\s*\\.:?\\s*\\d+\\s*Statement of account\\s*\\n+(.*?)(?=\\n)"
        );
        Matcher m = p.matcher(normalized);

        if (m.find()) {
            return m.group(1);
        }

        return null;
    }
    
    public static String extractBoi(String text) {
        String normalized = text.replaceAll("\r\n", "\n").replaceAll("\r", "\n");

        Pattern p = Pattern.compile(
            "(?is)account holder name\\s*:\\s*(.*?)(?=\\baccount holder address\\b|\\baccount\\b|\\bdate\\b|$)"
        );
        Matcher m = p.matcher(normalized);

        if (m.find()) {
            return m.group(1);
        }

        return null;
    }

    public static String extractAxis(String text) {
        String normalized = text.replaceAll("\r\n", "\n").replaceAll("\r", "\n");

        // changed this
        Matcher m = Pattern.compile(
            "(?im)^\\s*(.+)\\R\\s*Joint Holder\\b"
        ).matcher(normalized);
        
        if (m.find()) {
            return m.group(1);
        }

        return null;
    }

    private static String clean(String s) {
        return s.replaceAll("\\s{2,}", " ").trim();
    }
}