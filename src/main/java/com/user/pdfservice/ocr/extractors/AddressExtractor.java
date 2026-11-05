package com.user.pdfservice.ocr.extractors;

import java.util.Arrays;
import java.util.regex.*;

public class AddressExtractor {

    public static String extract(String text, String bank) {
        if (text == null)
            return null;

        String b = bank != null ? bank.toUpperCase() : "";

        if (b.contains("BOI")) {
            return extractBoiCustomerAddress(text);
        }
        if (b.contains("AXIS")) {
            return extractAxisCustomerAddress(text);
        }
        if (b.contains("KOTAK")) {
            return extractKotakCustomerAddress(text);
        }
        if (b.contains("HDFC")) {
            return extractHdfc(text);
        }
        if (b.contains("SBI")) {
            return extractSbiCustomerAddress(text);
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
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n");

        int startIdx = -1;
        for (int i = 0; i < lines.length && i < 30; i++) {
            String l = lines[i].trim();
            if (l.toUpperCase().contains("M/S.") || l.toUpperCase().contains("M/S ") || l.matches("(?i).*(?:TRADING\\s+COMPANY|ENTERPRISES|PVT\\s+LTD|LIMITED).*")) {
                startIdx = i;
                break;
            }
        }

        if (startIdx != -1) {
            StringBuilder sb = new StringBuilder();
            for (int i = startIdx + 1; i < lines.length && i < startIdx + 12; i++) {
                String line = lines[i].trim();
                if (line.matches("(?i)(?:JOINT\\s+HOLDERS|Nomination|Statement\\s+From|From\\s*:|Account\\s+Status|RTGS|IFSC|MICR|Date\\s+Narration).*")) {
                    break;
                }
                String left = line
                        .replaceAll("(?i)\\s*(?:Phone\\s*no\\.?|OD\\s*Limit|Currency|Email|Cust\\s*ID|Customer\\s*ID|Account\\s*No\\.?|PRIME\\s+POTENTIAL|A/C\\s*Open\\s*Date|Account\\s*Status|RTGS|IFSC|MICR|Branch\\s*Code).*$", "")
                        .replaceAll("(?i)\\s*\\b(City|State)\\s*:\\s*[A-Za-z\\s]+$", "")
                        .trim();
                if (!left.isEmpty() && !left.equals(".")) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(left);
                }
            }
            if (sb.length() > 10) return clean(sb.toString());
        }

        Pattern pattern = Pattern.compile(
                "(?is)(?:Statement of account|Page No[^\n]+)\\s*\\n+([^\n]+)\\n+(.*?)(?=\\n\\s*(?:JOINT|Nomination|Account Branch|From\\s*:))");
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            return clean(m.group(2));
        }
        return null;
    }

    public static String extractBoiCustomerAddress(String text) {
        if (text == null)
            return null;
        Pattern pattern = Pattern.compile(
                "(?is)account\\s+holder\\s+address\\s*[:\\-]?\\s*(.*?)(?=\\s*(?:Customer|IFSC|Account\\s+number|Branch|Transaction|\\r|\\n\\s*\\n))");
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            return clean(m.group(1));
        }
        return null;
    }

    public static String extractAxisCustomerAddress(String text) {
        if (text == null) return null;
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");

        Pattern p = Pattern.compile(
                "(?is)Joint\\s+Holder.*?\\R(.*?)\\s*(?=MICR\\s*Code|Scheme\\s*:|Statement\\s+of\\s+Axis)");
        Matcher m = p.matcher(normalized);
        if (m.find()) {
            String block = m.group(1).trim();
            block = block.replaceAll("(?i)Customer\\s*ID\\s*[:\\-]?\\s*\\d+", "");
            block = block.replaceAll("(?i)IFSC\\s*Code\\s*[:\\-]?\\s*[A-Z0-9]+", "");
            block = block.replaceAll("(?i)MICR\\s*Code\\s*[:\\-]?\\s*\\d+", "");
            block = block.replaceAll("(?i)Nominee\\s*Registered\\s*[:\\-]?[A-Za-z]+", "");
            block = block.replaceAll("(?i)Registered\\s*Mobile\\s*(?:No\\.?)?\\s*[:\\-]?[0-9xX]+", "");
            block = block.replaceAll("(?i)Registered\\s*Email\\s*(?:ID)?\\s*[:\\-]?[^\\s]+", "");
            block = block.replaceAll("(?i)PAN\\s*[:\\-]?[A-Z0-9]+", "");
            return clean(block);
        }
        return null;
    }

    public static String extractKotakCustomerAddress(String text) {
        if (text == null)
            return null;

        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");

        // Format 1: Classic Kotak with columns
        // Search for address block following the customer name line
        Pattern pClassic = Pattern.compile(
                "(?im)^([A-Za-z0-9\\s,.-]+?\\b\\d{6}\\b(?:\\n[A-Za-z\\s,-]+)?)(?=\\s*(?:Date\\s+Narration|#\\s+Date|Date|Savings\\s+Account))"
        );

        // Pattern 2: After CRN line
        Pattern crnPattern = Pattern.compile(
                "(?is)CRN\\s+[xX\\d]{6,12}\\s*(.*?)\\s*(?=MICR|IFSC|Account\\s+No|Branch)");
        Matcher crnMatcher = crnPattern.matcher(normalized);
        if (crnMatcher.find()) {
            String b = crnMatcher.group(1).trim();
            if (b.length() > 5) return clean(b);
        }

        // Pattern 3: Lines preceding "Date Narration" having pincode
        String[] lines = normalized.split("\n");
        StringBuilder addr = new StringBuilder();
        for (String line : lines) {
            String t = line.trim();
            if (t.matches("(?i).*(?:Date\\s+Narration|#\\s+Date\\s+Description).*")) break;
            // Clean right column tags
            String leftOnly = t
                    .replaceAll("(?i)\\s*Account\\s+No\\.?.*", "")
                    .replaceAll("(?i)\\s*Currency\\s*:.*", "")
                    .replaceAll("(?i)\\s*Branch\\s*:.*", "")
                    .replaceAll("(?i)\\s*Nominee\\s*:.*", "")
                    .replaceAll("(?i)\\s*Branch\\s+Address\\s*:.*", "")
                    .replaceAll("(?i)\\s*Branch\\s+Phone.*", "")
                    .replaceAll("(?i)\\s*MICR\\s*(?:Code)?.*", "")
                    .replaceAll("(?i)\\s*IFSC\\s*(?:Code)?.*", "")
                    .trim();

            if (leftOnly.toLowerCase().startsWith("plot") || leftOnly.toLowerCase().startsWith("shop")
                    || leftOnly.toLowerCase().startsWith("w-o") || leftOnly.toLowerCase().startsWith("s-o")
                    || leftOnly.matches(".*\\b\\d{6}\\b.*") || leftOnly.toLowerCase().contains("colony")
                    || leftOnly.toLowerCase().contains("nagar") || leftOnly.toLowerCase().contains("road")
                    || leftOnly.toLowerCase().contains("estate") || leftOnly.toLowerCase().contains("surat")
                    || leftOnly.toLowerCase().contains("bhopal") || leftOnly.toLowerCase().contains("mumbai")) {
                if (!leftOnly.toLowerCase().contains("branch") && !leftOnly.toLowerCase().contains("kotak")) {
                    if (addr.length() > 0) addr.append("\n");
                    addr.append(leftOnly);
                }
            }
        }
        if (addr.length() > 10) {
            return clean(addr.toString());
        }

        return null;
    }

    public static String extractSbiCustomerAddress(String text) {
        if (text == null) return null;
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");

        // Classic SBI: under "Address :"
        Matcher mClassic = Pattern.compile(
                "(?is)^\\s*Address\\s*:\\s*(.*?)(?=\\n\\s*(?:Date|Account Number|CIF|Branch|Drawing|MOD|IFS|MICR|Nomination|Balance))",
                Pattern.MULTILINE).matcher(normalized);
        if (mClassic.find()) {
            return clean(mClassic.group(1));
        }

        // New SBI: lines after email before "Date of Statement :"
        String[] lines = normalized.split("\n");
        StringBuilder sb = new StringBuilder();
        boolean start = false;
        for (String line : lines) {
            String t = line.trim();
            if (t.contains("@") && !t.contains("SBI.")) {
                start = true;
                continue;
            }
            if (start) {
                if (t.toLowerCase().contains("date of statement") || t.toLowerCase().contains("statement from") || t.toLowerCase().contains("clear balance") || t.toLowerCase().contains("branch code")) {
                    break;
                }
                // strip right-hand columns if any (including branch address fragment)
                String left = t.replaceAll("(?i)\\s*(?:Branch\\s+Code|Branch\\s+Name|Date\\s+of\\s+Statement|Clear\\s+Balance).*$", "")
                               .replaceAll("(?i)\\s*ROAD,\\s*NEAR\\s+STUTI\\s+COLONY.*$", "")
                               .trim();
                if (!left.isEmpty()) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(left);
                }
            }
        }
        if (sb.length() > 10) {
            return clean(sb.toString());
        }

        return null;
    }

    private static String clean(String s) {
        if (s == null) return null;
        return s
                .replaceAll("[ \t]+", " ")
                .replaceAll("\\s*,\\s*", ", ")
                .replaceAll(",\\s*$", "")
                .replaceAll("\\b(Date|Period|Statement).*", "")
                .replaceAll("\\n\\s+", "\n")
                .trim();
    }
}