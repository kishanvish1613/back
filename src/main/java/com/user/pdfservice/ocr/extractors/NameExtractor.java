package com.user.pdfservice.ocr.extractors;

import java.util.regex.*;

public class NameExtractor {

    public static String extract(String text, String bank) {
        if (text == null || text.isBlank())
            return null;

        if (bank != null) {
            if (bank.equalsIgnoreCase("SBI") || bank.equalsIgnoreCase("SBINEW")) {
                return extractSbi(text);
            } else if (bank.equalsIgnoreCase("AXIS")) {
                return extractAxis(text);
            } else if (bank.equalsIgnoreCase("BOI")) {
                return extractBoi(text);
            } else if (bank.equalsIgnoreCase("HDFC") || bank.equalsIgnoreCase("HDFCCURRENT")) {
                return extractHdfc(text);
            } else if (bank.equalsIgnoreCase("KOTAK") || bank.equalsIgnoreCase("KOTAKNEW")) {
                return extractKotak(text);
            } else if (bank.equalsIgnoreCase("AU")) {
                return extractAu(text);
            } else if (bank.equalsIgnoreCase("UNIONBANK")) {
                return extractUnionBank(text);
            }
        }

        Matcher labeled = Pattern.compile(
                "(?:WELCOME|NAME|CUSTOMER|ACCOUNT HOLDER|ACCOUNT\\s+NAME)\\s*[:\\-]\\s*([^\\r\\n]+)",
                Pattern.CASE_INSENSITIVE).matcher(text);

        if (labeled.find()) {
            return clean(labeled.group(1));
        }

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
                for (int j = i + 1; j < lines.length; j++) {
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

        // 1. Check Page 2/3 header: Name on separate line right before "Account No."
        Matcher mPage2 = Pattern.compile("(?im)^([A-Za-z][A-Za-z\\s.]{2,40})\\nAccount\\s+No\\.?", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (mPage2.find()) {
            String candidate = mPage2.group(1).trim();
            if (isValidKotakName(candidate)) return clean(candidate);
        }

        // 2. Name on same line before "Account No." (e.g. "Rakhi Nindane Account No. 6149933926" or "Shalu Account No. 5747413366")
        Matcher mSameLineAcc = Pattern.compile("(?im)^([A-Za-z][A-Za-z\\s.]{2,40})\\s+Account\\s+No\\.?", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (mSameLineAcc.find()) {
            String candidate = mSameLineAcc.group(1).trim();
            if (isValidKotakName(candidate)) return clean(candidate);
        }

        // 3. Name on same line before "Period :" (e.g. "Sikandar Ansari Period : ...")
        Matcher mSameLinePeriod = Pattern.compile("(?im)^([A-Za-z][A-Za-z\\s.]{2,40})\\s+Period\\s*:", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (mSameLinePeriod.find()) {
            String candidate = mSameLinePeriod.group(1).trim();
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

        return null;
    }

    private static boolean isValidKotakName(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase();
        if (t.length() < 2 || t.length() > 50) return false;
        if (t.contains("statement") || t.contains("kotak") || t.contains("bank") || t.contains("page") || t.contains("particulars")) {
            return false;
        }
        if (t.matches(".*\\d{4}.*")) return false;
        return true;
    }
    
    public static String extractHdfc(String text) {
        if (text == null) return null;
        String normalized = text.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        String[] lines = normalized.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i].toLowerCase();
            if (l.contains("page no") || l.contains("statement of account")) {
                for (int j = i + 1; j < lines.length && j <= i + 4; j++) {
                    String cand = lines[j].trim();
                    if (!cand.isEmpty() && !cand.toLowerCase().contains("page no")
                            && !cand.toLowerCase().contains("statement") && !cand.toLowerCase().contains("hdfc bank")
                            && !cand.toLowerCase().startsWith("from ") && !cand.toLowerCase().contains("joint holders")) {
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
                    && !upper.contains("TYPE :") && line.trim().length() > 3 && line.trim().matches(".*[A-Za-z]{3,}.*")) {
                return clean(line.trim());
            }
        }
        return null;
    }

    public static String extractSbi(String text) {
        if (text == null) return null;
        String normalized = text.replaceAll("\r\n", "\n").replaceAll("\r", "\n");

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

        Matcher m = Pattern.compile(
            "(?im)^\\s*(.+)\\R\\s*Joint Holder\\b"
        ).matcher(normalized);
        
        if (m.find()) {
            return clean(m.group(1));
        }

        return null;
    }

    private static String clean(String s) {
        return s.replaceAll("\\s{2,}", " ").trim();
    }
}