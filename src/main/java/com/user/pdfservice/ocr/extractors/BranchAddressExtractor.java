package com.user.pdfservice.ocr.extractors;

import java.util.regex.*;

public class BranchAddressExtractor {

    public static String extract(String text, String bank) {
        if (text == null || bank == null)
            return null;

        bank = bank.toUpperCase();

        switch (bank) {
            case "SBI":
                return null;
            case "AXIS":
                return extractAxis(text);
            case "KOTAK":
                return extractKotak(text);
            case "HDFC":
                Matcher m = Pattern.compile("(?is)\\s*Address\\s*:\\s*(.*?)(?=\\n+\\s*City)").matcher(text);
                if(m.find()) return m.group(1);
            default:
                return extractLabeled(text);
        }
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
                "BRANCH\\s+ADDRESS\\s*[:\\-]?\\s*" +
                        "(.{30,400}?)" +
                        "(?=\\n\\s*BRANCH\\b)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(text);

        if (m.find()) {
            return clean(m.group(1));
        }

        return extractLabeled(text); // fallback
    }

    public static String extractAxis(String text) {
        if (text == null)
            return null;

        Matcher m = Pattern.compile(
                "BRANCH\\s+ADDRESS\\s*(?:[:\\-]\\s*)?" + "(.{20,200}?)" + "(?=\\s*TEL\\s*:)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(text);

        if (m.find()) {
            return m.group(1)
                    .replaceAll("[ \t]+", " ")
                    .replaceAll(",\\s*$", "")
                    .trim();
        }

        return null;
    }

    /* ---------- CLEAN ---------- */
    private static String clean(String s) {
        return s
                .replaceAll("[ \t]+", " ")
                .replaceAll(",\\s*$", "")
                .trim();
    }
}
