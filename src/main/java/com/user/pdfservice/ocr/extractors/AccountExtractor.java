package com.user.pdfservice.ocr.extractors;

import java.util.regex.*;

public class AccountExtractor {

    public static String extract(String text, String bank) {
        if (text == null || text.isBlank()) return null;

        if (bank != null && (bank.equalsIgnoreCase("SBI") || bank.equalsIgnoreCase("SBINEW") || bank.equalsIgnoreCase("SBI2"))) {
            return extractSbiAccount(text);
        }

        // 1. Explicit Account Number labels
        Matcher labeled = Pattern.compile(
                "(?:ACCOUNT\\s*(?:NO\\.?|NUMBER)?|A/C\\s*(?:NO\\.?|NUMBER)?|Statement\\s+of\\s+(?:Axis\\s+)?Account\\s+No\\.?)\\s*[:\\-]?\\s*([\\d\\s]{9,20})",
                Pattern.CASE_INSENSITIVE).matcher(text);
        if (labeled.find()) {
            String acc = labeled.group(1).replaceAll("\\s+", "").trim();
            if (acc.length() >= 9) return acc;
        }

        // 2. Statement of Account <ACC_NO>
        Matcher stmtAcc = Pattern.compile(
                "Statement\\s+of\\s+Account\\s+([0-9]{10,18})",
                Pattern.CASE_INSENSITIVE).matcher(text);
        if (stmtAcc.find()) {
            return stmtAcc.group(1).trim();
        }

        // 3. Fallback: longest digit sequence in header
        return longestDigitSequence(text);
    }

    /**
     * SBI‑specific extraction – directly matches Account Number label while ignoring CIF.
     */
    private static String extractSbiAccount(String text) {
        if (text == null) return null;

        // 1. Direct label pattern
        Matcher mDirect = Pattern.compile("(?i)Account\\s*(?:Number|No\\.?)\\s*[:\\-]?\\s*([0-9]{10,18})").matcher(text);
        if (mDirect.find()) {
            return mDirect.group(1).trim();
        }

        // 2. Per line inspection
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.toLowerCase().contains("account number") && !line.toLowerCase().contains("cif")) {
                Matcher m = Pattern.compile("\\b([0-9]{10,18})\\b").matcher(line);
                if (m.find()) {
                    return m.group(1);
                }
            }
        }

        return bestNonCif11Digit(text);
    }

    private static String bestNonCif11Digit(String text) {
        Matcher mCif = Pattern.compile("(?i)CIF\\s*(?:Number|No\\.?)?\\s*[:\\-]?\\s*([0-9]{10,14})").matcher(text);
        String cif = mCif.find() ? mCif.group(1).trim() : null;

        Matcher m = Pattern.compile("\\b(\\d{11,17})\\b").matcher(text);
        while (m.find()) {
            String candidate = m.group(1);
            if (cif != null && candidate.equals(cif)) continue;
            if (candidate.startsWith("80") || candidate.startsWith("73") || candidate.startsWith("9095")) {
                // likely CIF
                continue;
            }
            return candidate;
        }
        return longestDigitSequence(text);
    }

    private static String longestDigitSequence(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("\\b\\d{9,18}\\b").matcher(text);
        String best = null;
        while (m.find()) {
            String candidate = m.group();
            if (best == null || candidate.length() > best.length()) {
                best = candidate;
            }
        }
        return best;
    }
}