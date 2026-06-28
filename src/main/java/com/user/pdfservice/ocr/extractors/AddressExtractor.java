package com.user.pdfservice.ocr.extractors;

import java.util.Arrays;
import java.util.regex.*;

public class AddressExtractor {

    public static String extract(String text, String bank) {
        if (text == null)
            return null;
            
        if ("BOI".equals(bank)) {
            return extractBoiCustomerAddress(text);
        }

        if ("AXIS".equals(bank)) {
            return extractAxisCustomerAddress(text);
        }

        if ("KOTAK".equals(bank)) {
            return extractKotakCustomerAddress(text);
        } else if("HDFC".equals(bank)){
            return extractHdfc(text);
        }

        String normalized = text
                .replaceAll("[\\t ]+", " ")
                .replaceAll("\\r", "");

        Matcher m = Pattern.compile(
                "\\bADDRESS\\b\\s*[:\\-]?\\s*" +
                        "(.{20,400}?)" +
                        "(?=\\n\\s*(DATE|ACCOUNT|IFSC|MICR)|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(normalized);

        if (m.find()) {
            return clean(m.group(1));
        }

        Matcher pinBlock = Pattern.compile(
                "([A-Za-z0-9,\\-/ ]{20,200}\\b\\d{6}\\b)",
                Pattern.CASE_INSENSITIVE).matcher(text);

        if (pinBlock.find()) {
            return clean(pinBlock.group(1));
        }

        return null;
    }
    
    public static String extractHdfc(String text) {
        if (text == null)
            return null;
        Pattern pattern = Pattern.compile(
                "(?is)Page No\\s*\\.\\s*:?\\s*\\d+\\s*Statement of account\\s*\\n+[^\\n]+\\n(.*?)(?=\\n\\s*JOINT)");
        Matcher m = pattern.matcher(text);
        if(m.find()){
            return m.group(1);
        }
        return null;
    }

    
    public static String extractBoiCustomerAddress(String text) {
        if (text == null)
            return null;
        Pattern pattern = Pattern.compile(
                "(?is)account holder address\\s*:\\s*(.*?)(?=Customer)");
        Matcher m = pattern.matcher(text);
        if(m.find()){
            return m.group(1);
        }
        return null;
    }

    /* ---------------- AXIS ---------------- */

    private static String extractAxisCustomerAddress(String text) {
        String normalized = text
                .replace("\r\n", "\n")
                .replace("\r", "\n");

        Pattern p = Pattern.compile(
                "(?is)Joint\\s+Holder.*?\\R(.*?)\\s*(?=MICR\\s*Code)");

        Matcher m = p.matcher(normalized);

        if (m.find()) {
            String block = m.group(1).trim();

            block = block.replaceAll("(?i)Customer\\s*ID\\s*[:\\-]?\\s*\\d+", "");
            block = block.replaceAll("(?i)IFSC\\s*Code\\s*[:\\-]?\\s*[A-Z0-9]+", "");
            block = block.replaceAll("(?i)MICR\\s*Code\\s*[:\\-]?\\s*\\d+", "");
            block = block
                    .replaceAll("[ \t]+", " ")
                    .replaceAll("\\n{2,}", "\n")
                    .trim();

            return block;
        }

        return null;
    }

    /* ---------------- KOTAK ---------------- */
    public static String extractKotakCustomerAddress(String text) {
        if (text == null)
            return null;

        Pattern crnPattern = Pattern.compile(
                "(?is)CRN\\s+[xX\\d]{9,11}\\s*(.*?)\\s*(?=MICR|IFSC|Account\\s+No)");

        Matcher crnMatcher = crnPattern.matcher(text);
        if (crnMatcher.find()) {
            return crnMatcher.group(1).trim();
        }

        Pattern ifscPattern = Pattern.compile(
                "(?is)IFSC\\s*Code\\s*[:\\-]?\\s*[A-Z0-9]+\\s*(.*?)\\s*(?=Date|Narration|Account|MICR)");

        Matcher ifscMatcher = ifscPattern.matcher(text);
        if (ifscMatcher.find()) {
            String block = ifscMatcher.group(1).trim();
            return stripFirstLine(block); // remove name only
        }

        return null;
    }

    private static String stripFirstLine(String block) {
        if (block == null)
            return null;

        String[] lines = block.split("\\r?\\n");
        if (lines.length <= 1)
            return block;

        return String.join("\n",
                Arrays.copyOfRange(lines, 1, lines.length)).trim();
    }

    /* ---------------- CLEANER ---------------- */

    private static String clean(String s) {
        return s
                .replaceAll("[ \t]+", " ")
                .replaceAll("\\s*,\\s*", ", ")
                .replaceAll(",\\s*$", "")
                .replaceAll("\\b(Date|Period|Statement).*", "")
                .replaceAll("\\n\\s+", "\n")
                .trim();
    }
}