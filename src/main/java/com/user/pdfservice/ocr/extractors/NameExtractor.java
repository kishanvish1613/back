package com.user.pdfservice.ocr.extractors;

import java.util.regex.*;

public class NameExtractor {

    public static String extract(String text, String bank) {
        if (text == null || text.isBlank())
            return null;

        String found = null;
        if (bank != null) {
            if (bank.equalsIgnoreCase("SBI") || bank.equalsIgnoreCase("SBINEW") || bank.equalsIgnoreCase("SBI2")) {
                found = extractSbi(text);
            } else if (bank.equalsIgnoreCase("AXIS")) {
                found = extractAxis(text);
            } else if (bank.equalsIgnoreCase("BOI")) {
                found = extractBoi(text);
            } else if (bank.equalsIgnoreCase("HDFC") || bank.equalsIgnoreCase("HDFCCURRENT")) {
                found = extractHdfc(text);
            } else if (bank.equalsIgnoreCase("KOTAK") || bank.equalsIgnoreCase("KOTAKNEW")) {
                found = extractKotak(text);
            } else if (bank.equalsIgnoreCase("AU")) {
                found = extractAu(text);
            } else if (bank.equalsIgnoreCase("UNIONBANK")) {
                found = extractUnionBank(text);
            }
        }

        if (found != null && !found.isBlank()) {
            return clean(found);
        }

        // Labeled fallback
        Matcher labeled = Pattern.compile(
                "(?:WELCOME|CUSTOMER\\s+NAME|ACCOUNT\\s+HOLDER(?:\\s+NAME)?|ACCOUNT\\s+NAME|NAME)\\s*[:\\-]\\s*([^\\r\\n]+)",
                Pattern.CASE_INSENSITIVE).matcher(text);

        if (labeled.find()) {
            String cand = clean(labeled.group(1));
            if (cand.length() >= 2 && !cand.toLowerCase().contains("statement") && !cand.toLowerCase().contains("account number")) {
                return cand;
            }
        }

        // Final fallback: try Kotak / generic line extractor
        return extractKotak(text);
    }

    public static String extractAu(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("(?i)Name\\s*[:\\-]?\\s*([A-Za-z0-9\\s.&'\\-]+?)(?=\\s*Customer\\s*ID|\\s*Account\\s*Number|\\r|\\n|$)").matcher(text);
        if (m.find()) {
            return clean(m.group(1));
        }
        return null;
    }

    public static String extractUnionBank(String text) {
        if (text == null) return null;
        String normalized = text.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        String[] lines = normalized.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].toLowerCase().contains("statement of account")) {
                for (int j = i + 1; j < lines.length && j <= i + 5; j++) {
                    String candidate = lines[j].trim();
                    if (!candidate.isEmpty() && !candidate.toLowerCase().contains("statement") && !candidate.toLowerCase().contains("union bank")) {
                        return clean(candidate);
                    }
                }
            }
        }
        return null;
    }

    public static String extractKotak(String text) {
        if (text == null) return null;
        String normalized = text.replaceAll("\r\n", "\n").replaceAll("\r", "\n");

        // 1. Name on first line before "Period :" (e.g. "Sikandar Ansari Period : 19-07-2025 to 01-01-2026")
        Matcher mSameLinePeriod = Pattern.compile("(?im)^([A-Za-z][A-Za-z\\s.]{2,40}?)\\s+Period\\s*:", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (mSameLinePeriod.find()) {
            String candidate = mSameLinePeriod.group(1).trim();
            if (isValidKotakName(candidate)) return clean(candidate);
        }

        // 2. Name right before "Account No." on same line
        Matcher mSameLineAcc = Pattern.compile("(?im)^([A-Za-z][A-Za-z\\s.]{2,40}?)\\s+Account\\s+No\\.?", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (mSameLineAcc.find()) {
            String candidate = mSameLineAcc.group(1).trim();
            if (isValidKotakName(candidate)) return clean(candidate);
        }

        // 3. Name on separate line right before "Account No."
        Matcher mPage2 = Pattern.compile("(?im)^([A-Za-z][A-Za-z\\s.]{2,40}?)\\nAccount\\s+No\\.?", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (mPage2.find()) {
            String candidate = mPage2.group(1).trim();
            if (isValidKotakName(candidate)) return clean(candidate);
        }

        // 4. Line preceding CRN / Cust Reln No
        String[] lines = normalized.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i].trim();
            if (l.matches("(?i)^CRN\\b.*") || l.matches("(?i).*Cust\\.?\\s*Reln\\.?\\s*No.*")) {
                for (int j = i - 1; j >= 0; j--) {
                    String prev = lines[j].trim();
                    if (prev.isEmpty()) continue;
                    String cleanedPrev = prev.replaceAll("(?i)\\s*Account\\s+No\\.?.*", "").trim();
                    if (isValidKotakName(cleanedPrev)) {
                        return clean(cleanedPrev);
                    }
                }
                break;
            }
        }

        // 5. Line after IFSC code in classic Kotak
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].toUpperCase().contains("IFSC CODE") || lines[i].toUpperCase().contains("KKBK")) {
                for (int j = i + 1; j < lines.length && j <= i + 3; j++) {
                    String cand = lines[j].trim();
                    if (isValidKotakName(cand)) {
                        return clean(cand);
                    }
                }
                break;
            }
        }

        return null;
    }

    private static boolean isValidKotakName(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase();
        if (t.length() < 2 || t.length() > 50) return false;
        if (t.contains("statement") || t.contains("kotak") || t.contains("bank") || t.contains("page") || t.contains("particulars")
                || t.contains("period") || t.contains("account no") || t.contains("currency") || t.contains("branch")
                || t.contains("nominee") || t.contains("ifsc") || t.contains("micr") || t.matches("^page\\s+\\d+.*")) {
            return false;
        }
        if (t.matches(".*\\d{3}.*")) return false;
        return true;
    }

    public static String extractHdfc(String text) {
        if (text == null) return null;
        String normalized = text.replaceAll("\r\n", "\n").replaceAll("\r", "\n");

        // 1. Check for entity name (M/S. ...)
        Matcher mEntity = Pattern.compile("(?im)^\\s*(M/S\\.?\\s+[A-Za-z0-9\\s.&'-]+?)(?=\\s*(?:City|State|Address|Account|Phone|Cust|A/C|RTGS|IFSC|MICR|OD|Currency|Email|\\r|\\n|$))").matcher(normalized);
        if (mEntity.find()) {
            return clean(mEntity.group(1));
        }

        // 2. Personal name line
        String[] lines = normalized.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i].toLowerCase();
            if (l.contains("page no") || l.contains("statement of account")) {
                for (int j = i + 1; j < lines.length && j <= i + 6; j++) {
                    String cand = lines[j].trim();
                    if (cand.isEmpty() || cand.toLowerCase().contains("page no")
                            || cand.toLowerCase().contains("statement") || cand.toLowerCase().contains("hdfc bank")
                            || cand.toLowerCase().startsWith("from ") || cand.toLowerCase().contains("joint holders")
                            || cand.toLowerCase().contains("account branch") || cand.toLowerCase().startsWith("address")) {
                        continue;
                    }
                    if (cand.matches("(?i).*(?:b-\\d+|opp\\b|near\\b|shop\\b|plot\\b|floor\\b|street\\b|road\\b|indrapuri).*")) {
                        continue;
                    }
                    if (cand.matches("(?i).*?\\b(city|state|address|phone|account branch|cust id|od limit)\\s*:.*")) {
                        cand = cand.replaceAll("(?i)\\s*\\b(city|state|address|phone|account branch|cust id|od limit)\\s*:.*$", "").trim();
                    }
                    if (!cand.isBlank() && cand.length() > 2) {
                        return clean(cand);
                    }
                }
            }
        }
        return null;
    }

    public static String extractBoi(String text) {
        if (text == null) return null;
        String normalized = text.replaceAll("\r\n", "\n").replaceAll("\r", "\n");

        // 1. Explicit "Account holder name: <NAME>"
        Matcher mAccHolder = Pattern.compile("(?i)Account\\s+holder\\s+name\\s*[:\\-]?\\s*([^:\\r\\n]+?)(?=\\s*Account\\s+holder\\s+address|\\r|\\n|$)", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (mAccHolder.find()) {
            return clean(mAccHolder.group(1));
        }

        String[] lines = normalized.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.toUpperCase().startsWith("M/S") || trimmed.toUpperCase().startsWith("MS ")) {
                return clean(trimmed);
            }
        }
        for (String line : lines) {
            String upper = line.trim().toUpperCase();
            if (!upper.contains("BANK OF INDIA") && !upper.contains("DATE") && !upper.contains("OPENING BALANCE")
                    && !upper.contains("STATEMENT OF ACCOUNT") && !upper.contains("CUSTID") && !upper.contains("A/C NO")
                    && !upper.contains("ACCOUNT HOLDER") && !upper.contains("TYPE :") && line.trim().length() > 3 && line.trim().matches(".*[A-Za-z]{3,}.*")) {
                return clean(line.trim());
            }
        }
        return null;
    }

    public static String extractSbi(String text) {
        if (text == null) return null;
        String normalized = text.replaceAll("\r\n", "\n").replaceAll("\r", "\n");

        // 0. Classic SBI: "Account Name : Mrs. KAMLA MALVIYA"
        Matcher mAccName = Pattern.compile("(?im)^\\s*Account\\s+Name\\s*[:\\-]?\\s*([A-Za-z][A-Za-z\\s.]{2,50}?)(?:\\r|\\n|$)", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (mAccName.find()) {
            String name = mAccName.group(1).trim();
            if (isValidSbiName(name)) return clean(name);
        }

        // 1. Under "STATEMENT OF ACCOUNT" (e.g. "STATEMENT OF ACCOUNT\nRAVINDRA  NASHIKKAR  State Bank of India")
        Matcher mStmt = Pattern.compile("(?im)STATEMENT\\s+OF\\s+ACCOUNT\\s*\\n\\s*([A-Za-z][A-Za-z\\s.]{2,50}?)(?:\\s+State\\s+Bank|\\r|\\n|$)", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (mStmt.find()) {
            String name = mStmt.group(1).trim().replaceAll("(?i)\\s*State\\s+Bank.*", "").trim();
            if (isValidSbiName(name)) return clean(name);
        }

        // 2. Line before email address
        String[] lines = normalized.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("@")) {
                for (int j = i - 1; j >= 0; j--) {
                    String prev = lines[j].trim().replaceAll("(?i)\\s*State\\s+Bank.*", "").trim();
                    if (isValidSbiName(prev) && !prev.equalsIgnoreCase("STATEMENT OF ACCOUNT") && !prev.equalsIgnoreCase("Account Summary")) {
                        return clean(prev);
                    }
                }
            }
        }

        // 3. Welcome line fallback
        Matcher mWelcome = Pattern.compile("(?im)Welcome\\s*:\\s*\\n?\\s*([A-Za-z][A-Za-z\\s.]{2,50}?)(?:\\s*\\n|\\r|Account\\s+Summary|$)", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (mWelcome.find()) {
            String name = mWelcome.group(1).trim().replaceAll("\\.+$", "").trim();
            if (isValidSbiName(name)) return clean(name);
        }

        return null;
    }

    private static boolean isValidSbiName(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase();
        if (t.length() < 3 || t.length() > 60) return false;
        if (t.contains("state bank") || t.contains("statement of account") || t.contains("account summary") || t.contains("welcome:")) return false;
        if (t.matches(".*\\d{4}.*")) return false;
        return true;
    }

    public static String extractAxis(String text) {
        if (text == null) return null;
        String normalized = text.replaceAll("\r\n", "\n").replaceAll("\r", "\n");

        // 1. Line before "Joint Holder"
        Matcher m = Pattern.compile("(?im)^\\s*([A-Za-z][A-Za-z\\s.]{2,40}?)\\s*\\R\\s*Joint\\s+Holder\\b").matcher(normalized);
        if (m.find()) {
            return clean(m.group(1));
        }

        // 2. Line before S/O address line
        Matcher mSo = Pattern.compile("(?im)^\\s*([A-Za-z][A-Za-z\\s.]{2,40}?)\\s*\\R\\s*S/O:").matcher(normalized);
        if (mSo.find()) {
            return clean(mSo.group(1));
        }

        return null;
    }

    private static String clean(String s) {
        if (s == null) return null;
        return s.replaceAll("\\s{2,}", " ").trim();
    }
}