package com.user.pdfservice.ocr.extractors;

import java.util.regex.*;

public class AccountExtractor {

    public static String extract(String text, String bank) {
        if (text == null || text.isBlank()) return null;

        if (bank != null && (bank.equalsIgnoreCase("SBI") || bank.equalsIgnoreCase("SBINEW"))) {
            return extractSbiAccount(text);
        }

        // 1. Explicit Account Number labels
        Matcher labeled = Pattern.compile(
                "(?:ACCOUNT\\s*(?:NO\\.?|NUMBER)?|A/C\\s*(?:NO\\.?|NUMBER)?)\\s*[:\\-]?\\s*([\\d\\s]{9,20})",
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
     * SBI‑specific extraction – takes the last number from the line
     * containing "Account Number", ignoring CIF numbers.
     */
    private static String extractSbiAccount(String text) {
        if (text == null) return null;

        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.toLowerCase().contains("account number")) {
                // Take the last 9‑18 digit number on this line
                String number = lastNumberOnLine(line);
                if (number != null && !isCif(number)) {
                    return number;
                }

                // If no number on this line, try the next line
                if (i + 1 < lines.length) {
                    number = firstNumberOnLine(lines[i + 1]);
                    if (number != null && !isCif(number)) {
                        return number;
                    }
                }
            }
        }

        // Fallback: longest 11‑digit number that is not a CIF number
        return bestNonCif11Digit(text);
    }

    private static String lastNumberOnLine(String line) {
        Matcher m = Pattern.compile("\\b\\d{9,18}\\b").matcher(line);
        String last = null;
        while (m.find()) {
            last = m.group();
        }
        return last;
    }

    private static String firstNumberOnLine(String line) {
        Matcher m = Pattern.compile("\\b\\d{9,18}\\b").matcher(line);
        return m.find() ? m.group() : null;
    }

    /** CIF numbers in SBI statements often start with 80 and are 11 digits. */
    private static boolean isCif(String num) {
        return num != null && num.length() == 11 && num.startsWith("80");
    }

    private static String bestNonCif11Digit(String text) {
        Matcher m = Pattern.compile("\\b\\d{11}\\b").matcher(text);
        while (m.find()) {
            String candidate = m.group();
            if (!isCif(candidate)) {
                return candidate;
            }
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