package com.user.pdfservice.ocr.extractors;

import java.util.regex.*;

public class BranchExtractor {

    public static String extract(String text, String bank) {
        if (text == null) return null;

        // 1. Explicit labeled "Branch Name : <val>"
        Matcher mBrName = Pattern.compile(
                "(?im)^\\s*BRANCH\\s+NAME\\s*[:\\-]\\s*([^:\\r\\n]{2,80})",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (mBrName.find()) {
            String val = clean(mBrName.group(1));
            if (isValidBranch(val)) return val;
        }

        // 2. Explicit labeled "Branch : <val>"
        Matcher mBr = Pattern.compile(
                "(?im)^\\s*BRANCH\\s*[:\\-]\\s*([^:\\r\\n]{2,80})",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (mBr.find()) {
            String val = clean(mBr.group(1));
            if (isValidBranch(val)) return val;
        }

        // 3. Inline label before another label
        Matcher mInline = Pattern.compile(
                "(?i)Branch\\s*(?:Name)?\\s*[:\\-]\\s*([A-Za-z0-9\\s\\-]+?)(?=\\s*(?:Branch\\s+Code|Branch\\s+Email|Branch\\s+Phone|Account|Date|Drawing|Interest|MOD|CIF|CKYCR|Nominee|Address|Customer|\\r|\\n|$))",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (mInline.find()) {
            String val = clean(mInline.group(1));
            if (isValidBranch(val)) return val;
        }

        // 4. Bank of India specific: "Bank Of India <Branch>"
        if (bank != null && bank.equalsIgnoreCase("BOI")) {
            Matcher mBoi = Pattern.compile("(?i)Bank\\s+Of\\s+India\\s+([A-Za-z0-9\\s\\-]+?)(?=\\r|\\n|M/S|SHOP|CUSTID|$)").matcher(text);
            if (mBoi.find()) {
                String val = clean(mBoi.group(1));
                if (isValidBranch(val) && !val.equalsIgnoreCase("Bank Of India") && !val.equalsIgnoreCase("Date")) {
                    return val;
                }
            }
        }

        // 5. Kotak specific: "Branch : <Branch>"
        if (bank != null && (bank.equalsIgnoreCase("KOTAK") || bank.equalsIgnoreCase("KOTAKNEW"))) {
            Matcher mKotak = Pattern.compile("(?i)Branch\\s*[:\\-]?\\s*([A-Za-z0-9\\s\\-]+?)(?=\\s*Nominee|\\s*Branch\\s+Address|\\s*Account|\\r|\\n|$)", Pattern.CASE_INSENSITIVE).matcher(text);
            if (mKotak.find()) {
                String val = clean(mKotak.group(1));
                if (isValidBranch(val)) return val;
            }
        }

        return null;
    }

    private static boolean isValidBranch(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase();
        if (t.length() < 2 || t.length() > 60) return false;
        if (t.contains("office") && t.contains("updating")) return false; // reject disclaimer text
        if (t.contains("discrepancy") || t.contains("nearest") || t.contains("contact your")) return false;
        return true;
    }

    private static String clean(String s) {
        if (s == null) return null;
        return s.replaceAll("\\s{2,}", " ")
                .replaceAll("(?i)^\\s*(?:BRANCH\\s+NAME|BRANCH)\\s*[:\\-]?\\s*", "")
                .trim();
    }
}