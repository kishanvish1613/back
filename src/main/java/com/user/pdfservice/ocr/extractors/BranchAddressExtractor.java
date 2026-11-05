package com.user.pdfservice.ocr.extractors;

import java.util.regex.*;

public class BranchAddressExtractor {

    public static String extract(String text, String bank) {
        if (text == null)
            return null;

        String b = bank != null ? bank.toUpperCase() : "";

        if (b.contains("AXIS")) {
            return extractAxis(text);
        } else if (b.contains("KOTAK")) {
            return extractKotak(text);
        } else if (b.contains("HDFC")) {
            return extractHdfc(text);
        } else if (b.contains("SBI")) {
            return extractSbi(text);
        }

        return extractLabeled(text);
    }

    private static String extractLabeled(String text) {
        Matcher m = Pattern.compile(
                "BRANCH\\s+ADDRESS\\s*(?:[:\\-]\\s*)?([^\\r\\n]{20,200})",
                Pattern.CASE_INSENSITIVE).matcher(text);

        if (m.find()) {
            return clean(m.group(1));
        }

        return null;
    }

    private static String extractKotak(String text) {
        Matcher m = Pattern.compile(
                "(?is)BRANCH\\s+ADDRESS\\s*[:\\-]?\\s*" +
                        "(Kotak\\s+Mahindra\\s+Bank\\s+Ltd[^\n]+(?:\n[^\n]+){1,4}?)" +
                        "(?=\\s*BRANCH\\s+PHONE|\\s*MICR|\\s*IFSC|\\r|\\n|$)",
                Pattern.CASE_INSENSITIVE).matcher(text);

        if (m.find()) {
            return clean(m.group(1));
        }

        Matcher m2 = Pattern.compile(
                "(?is)Branch\\s+Address\\s*\\n(.*?)(?=\\s*Branch\\s+Phone|\\s*Remember|\\s*India|\\r|\\n|$)",
                Pattern.CASE_INSENSITIVE).matcher(text);
        if (m2.find()) {
            return clean(m2.group(1));
        }

        return extractLabeled(text);
    }

    private static String extractHdfc(String text) {
        Matcher m = Pattern.compile(
                "(?is)Address\\s*:\\s*(HDFC\\s+BANK\\s+LTD[^\n]*(?:\\n[^\n]+){1,3}?)(?=\\s*(?:M/S|MR\\.|MRS\\.|MS\\.|City\\s*:|State\\s*:|Phone\\s*no|\\r|\\n\\s*\\n))"
        ).matcher(text);
        if (m.find()) {
            return clean(m.group(1));
        }
        return extractLabeled(text);
    }

    private static String extractSbi(String text) {
        // In new SBI (sbinew), the branch address block starts with GOSWAMI COMPLEX / building
        Matcher mNew = Pattern.compile(
                "(?is)State\\s+Bank\\s+of\\s+India\\s*\\n([^\\n]+(?:\\n[^\\n]+){0,2}?)(?=\\s*(?:vedpatel|Branch\\s+Code|Branch\\s+Name|Date|Clear|Uncleared|\\r|\\n))",
                Pattern.CASE_INSENSITIVE).matcher(text);
        if (mNew.find()) {
            String val = clean(mNew.group(1));
            val = val.replaceAll("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}", "")
                     .replaceAll("(?i)WARD\\s+NO\\..*", "")
                     .replaceAll("(?i)S/O:.*", "").trim();
            if (val.length() > 8) {
                Matcher m2 = Pattern.compile("(?i)(ROAD,\\s*NEAR\\s+[A-Za-z0-9\\s,.-]+?\\bBHOPAL\\b)").matcher(text);
                if (m2.find()) {
                    val = val + ", " + clean(m2.group(1));
                }
                return clean(val);
            }
        }
        return null;
    }

    public static String extractAxis(String text) {
        if (text == null) return null;

        Matcher m = Pattern.compile(
                "(?is)BRANCH\\s+ADDRESS\\s*(?:[:\\-]\\s*)?" + "(AXIS\\s+BANK\\s+LTD[^\n]+(?:\n[^\n]+){0,4}?)" + "(?=\\s*TEL\\s*:|\\s*Registered\\s+Office|\\s*Opening\\s+Hours|\\r|\\n|$)",
                Pattern.CASE_INSENSITIVE).matcher(text);

        if (m.find()) {
            return clean(m.group(1));
        }

        return extractLabeled(text);
    }

    private static String clean(String s) {
        if (s == null) return null;
        return s.replaceAll("[ \t]+", " ")
                .replaceAll("\\r?\\n", ", ")
                .replaceAll(",\\s*,", ",")
                .replaceAll(",\\s*$", "")
                .trim();
    }
}
