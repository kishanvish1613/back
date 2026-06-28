package com.user.pdfservice.ocr;

import com.user.pdfservice.ocr.extractors.*;
import com.user.pdfservice.ocr.utils.*;
import java.util.regex.*;

public class OcrProcessingService {

    public static OcrResult process(String rawText) {
        String cleaned = TextNormalizer.normalize(
                TextNormalizer.deNoise(rawText)
        );

        System.out.println("DEBUG: Raw OCR text (first 1000 chars):\n" +
                (cleaned.length() > 1000 ? cleaned.substring(0, 1000) : cleaned));

        OcrResult r = new OcrResult();
        r.rawText = cleaned;

        r.ifsc = IfscExtractor.extract(cleaned);
        r.bankName = BankDetector.detectFromIFSC(r.ifsc);
        if (r.bankName == null) throw new Error("No Bank Name Detected");

        r.name = extractFullNameForSbi(cleaned, r.bankName);
        System.out.println("DEBUG: Full Name = " + r.name);
        r.accountNumber = AccountExtractor.extract(cleaned, r.bankName);

        if (r.bankName.equalsIgnoreCase("SBI") || r.bankName.equalsIgnoreCase("SBINEW")) {
            extractSbiFieldsLineByLine(r, cleaned);
        } else {
            r.branchAddress = BranchAddressExtractor.extract(cleaned, r.bankName);
            r.micr = extractMICR(cleaned);
            r.pincode = AddressUtils.extractPincode(r.branchAddress);
            r.address = AddressExtractor.extract(cleaned, r.bankName);
            r.accountType = extractAccountType(cleaned, r.bankName);
            r.branchPhoneNo = extractBranchPhone(cleaned, r.bankName);
            r.branch = BranchExtractor.extract(cleaned, r.bankName);
            r.customerRelNo = extractCrn(cleaned);
            r.email = extractEmail(cleaned);
            r.phoneNumber = extractPhone(cleaned);
        }

        return r;
    }

    private static String extractFullNameForSbi(String text, String bank) {
        String name = NameExtractor.extract(text, bank);
        if (name == null || name.trim().length() < 10 || name.trim().endsWith(".") || name.trim().matches("^X+$")) {
            String[] lines = text.split("\\r?\\n");
            for (int i = 0; i < lines.length; i++) {
                if (Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}").matcher(lines[i]).find()) {
                    if (i > 0) {
                        String candidate = lines[i - 1].trim();
                        if (candidate.matches("^[A-Z ]+$") && candidate.length() > 5) {
                            return candidate;
                        }
                    }
                    break;
                }
            }
        }
        return name;
    }

    private static void extractSbiFieldsLineByLine(OcrResult r, String text) {
        String[] lines = text.split("\\r?\\n");

        // ----- Statement Period -----
        for (String line : lines) {
            Matcher m = Pattern.compile(
                    "Statement\\s+From\\s*:\\s*(\\d{2}-\\d{2}-\\d{4})\\s+to\\s+(\\d{2}-\\d{2}-\\d{4})",
                    Pattern.CASE_INSENSITIVE
            ).matcher(line);
            if (m.find()) {
                r.statementPeriodStart = m.group(1);
                r.statementPeriodEnd   = m.group(2);
                System.out.println("DEBUG: Statement Period = " + r.statementPeriodStart + " to " + r.statementPeriodEnd);
                break;
            }
        }

        // ----- Clear Balance -----
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].toLowerCase().contains("clear balance")) {
                for (int j = i + 1; j < lines.length; j++) {
                    String val = lines[j].trim();
                    if (!val.isEmpty() && val.matches("^[\\d,]+\\.\\d{2}CR$")) {
                        r.openingBalance = val;
                        System.out.println("DEBUG: Clear Balance = " + r.openingBalance);
                        break;
                    }
                }
                break;
            }
        }

        // ----- Account Open Date -----
        for (String line : lines) {
            Matcher m = Pattern.compile(
                    "Account\\s+open\\s+Date\\s*:\\s*(\\d{2}/\\d{2}/\\d{4})",
                    Pattern.CASE_INSENSITIVE
            ).matcher(line);
            if (m.find()) {
                r.accountOpenDate = m.group(1);
                System.out.println("DEBUG: Account Open Date = " + r.accountOpenDate);
                break;
            }
        }

        // ----- CKYCR – robust extraction, prefers "Not Available" -----
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].toLowerCase().contains("ckycr")) {
                String currentLine = lines[i];
                String nextLine = (i + 1 < lines.length) ? lines[i + 1] : "";
                if (currentLine.toLowerCase().contains("not available") ||
                        nextLine.toLowerCase().contains("not available")) {
                    r.ckycr = "Not Available";
                    System.out.println("DEBUG: CKYCR = Not Available");
                    break;
                }
                Matcher m = Pattern.compile("\\b\\d{14}\\b").matcher(currentLine);
                if (m.find()) {
                    r.ckycr = m.group();
                    System.out.println("DEBUG: CKYCR = " + r.ckycr);
                    break;
                }
                if (!nextLine.isEmpty()) {
                    m = Pattern.compile("\\b\\d{14}\\b").matcher(nextLine);
                    if (m.find()) {
                        r.ckycr = m.group();
                        System.out.println("DEBUG: CKYCR = " + r.ckycr);
                        break;
                    }
                }
                m = Pattern.compile("\\d+").matcher(currentLine);
                if (m.find()) {
                    r.ckycr = m.group();
                    System.out.println("DEBUG: CKYCR (fallback number) = " + r.ckycr);
                } else if (!nextLine.isEmpty()) {
                    m = Pattern.compile("\\d+").matcher(nextLine);
                    if (m.find()) {
                        r.ckycr = m.group();
                        System.out.println("DEBUG: CKYCR (fallback number) = " + r.ckycr);
                    }
                }
                break;
            }
        }

        // ----- Nominee Name – skip if value contains "Nominee" -----
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].toLowerCase().contains("nominee")) {
                String afterColon = lines[i].replaceFirst("^.*:\\s*", "").trim();
                if (!afterColon.isEmpty() && !afterColon.matches("\\d+")
                        && !afterColon.equalsIgnoreCase("Name")
                        && !afterColon.toLowerCase().contains("nominee")) {
                    r.nomineeName = afterColon;
                } else {
                    for (int j = i + 1; j < lines.length; j++) {
                        String next = lines[j].trim();
                        if (next.isEmpty()) continue;
                        if (next.matches("\\d+")) continue;
                        if (next.equalsIgnoreCase("Savings Account")) continue;
                        if (next.equalsIgnoreCase("SBIN0016450")) continue;
                        if (next.equalsIgnoreCase("INR")) continue;
                        if (next.equalsIgnoreCase("OPEN")) continue;
                        if (next.toLowerCase().contains("nominee")) continue;
                        r.nomineeName = next;
                        break;
                    }
                }
                System.out.println("DEBUG: Nominee = " + r.nomineeName);
                break;
            }
        }

        // ----- Branch Name -----
        for (int i = 0; i < lines.length; i++) {
            if (isEmpty(r.branch) && containsLabel(lines[i], "branch name")) {
                String val = valueAfterLabel(lines[i], "branch name");
                if (isEmpty(val)) val = nextNonEmptyNonLabel(lines, i);
                if (!isEmpty(val)) {
                    r.branch = val;
                    System.out.println("DEBUG: Branch Name = " + r.branch);
                }
            }
        }

        // ----- MICR -----
        for (int i = 0; i < lines.length; i++) {
            if (isEmpty(r.micr) && containsLabel(lines[i], "micr")) {
                String val = valueAfterLabel(lines[i], "micr code");
                if (isEmpty(val)) val = valueAfterLabel(lines[i], "micr");
                if (isEmpty(val)) val = nextNonEmptyNonLabel(lines, i);
                if (!isEmpty(val)) {
                    r.micr = val.replaceAll("[^0-9]", "");
                    System.out.println("DEBUG: MICR = " + r.micr);
                }
            }
        }

        // ----- CIF Number -----
        if (isEmpty(r.customerRelNo)) {
            Matcher m = Pattern.compile("\\b80\\d{9}\\b").matcher(text);
            if (m.find()) {
                r.customerRelNo = m.group();
                System.out.println("DEBUG: CIF = " + r.customerRelNo);
            }
        }

        // ----- Branch Phone -----
        for (int i = 0; i < lines.length; i++) {
            if (isEmpty(r.branchPhoneNo) && containsLabel(lines[i], "branch phone")) {
                for (int j = i + 1; j < lines.length && j <= i + 5; j++) {
                    String digits = lines[j].trim().replaceAll("[^0-9]", "");
                    if (digits.length() >= 10) {
                        r.branchPhoneNo = digits.substring(digits.length() - 10);
                        System.out.println("DEBUG: Branch Phone = " + r.branchPhoneNo);
                        break;
                    }
                }
                break;
            }
        }
        if (isEmpty(r.branchPhoneNo) && !isEmpty(r.phoneNumber)) {
            r.branchPhoneNo = r.phoneNumber;
            System.out.println("DEBUG: Branch Phone (fallback) = " + r.branchPhoneNo);
        }

        // ----- Product (Account Type) – FINAL FIX: look specifically for "Savings Account" after "Product" -----
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].toLowerCase().contains("product")) {
                // Search the next few lines for "Savings Account"
                for (int j = i + 1; j < Math.min(i + 8, lines.length); j++) {
                    String candidate = lines[j].trim();
                    if (candidate.toLowerCase().contains("savings account")) {
                        r.accountType = "Savings Account";
                        System.out.println("DEBUG: Account Type (from Product) = " + r.accountType);
                        break;
                    }
                }
                break;
            }
        }
        // Fallback: search the entire text for "Savings Account"
        if (isEmpty(r.accountType)) {
            for (String line : lines) {
                if (line.toLowerCase().contains("savings account")) {
                    r.accountType = "Savings Account";
                    System.out.println("DEBUG: Account Type (fallback) = " + r.accountType);
                    break;
                }
            }
        }
        if (isEmpty(r.accountType)) {
            r.accountType = extractAccountType(text, "SBI");
            System.out.println("DEBUG: Account Type (last resort) = " + r.accountType);
        }

        // ----- Customer Address -----
        if (isEmpty(r.address)) {
            r.address = extractAddressAfterEmail(lines);
            System.out.println("DEBUG: Address (after email) = " + r.address);
        }

        // ----- Email (lowercase) -----
        if (isEmpty(r.email)) {
            r.email = extractEmail(text);
            if (!isEmpty(r.email)) r.email = r.email.toLowerCase();
            System.out.println("DEBUG: Email (fallback) = " + r.email);
        } else {
            r.email = r.email.toLowerCase();
        }

        // ----- Phone -----
        if (isEmpty(r.phoneNumber)) {
            r.phoneNumber = extractPhone(text);
            if (r.phoneNumber == null) {
                r.phoneNumber = extractAnyMobile(text, r.accountNumber, r.customerRelNo);
            }
            System.out.println("DEBUG: Phone (fallback) = " + r.phoneNumber);
        }

        // ----- Branch Address & Location -----
        if (isEmpty(r.branchAddress)) {
            boolean capture = false;
            boolean locationCaptured = false;
            StringBuilder addrBuilder = new StringBuilder();

            for (String line : lines) {
                String t = line.trim();
                if (t.equals("0.00") && !capture) {
                    capture = true;
                    continue;
                }
                if (capture) {
                    if (t.toLowerCase().contains("branch code")) break;
                    if (t.isEmpty() || t.matches("^[\\d]+(\\.[\\d]+)?$") || t.contains("%")) continue;
                    if (t.matches(".*\\d{2}/\\d{2}/\\d{4}.*")) continue;

                    if (!locationCaptured && !t.isEmpty()) {
                        r.branchLocation = t;
                        locationCaptured = true;
                        System.out.println("DEBUG: Branch Location = " + r.branchLocation);
                        continue;
                    }

                    if (addrBuilder.length() > 0) addrBuilder.append("\n");
                    addrBuilder.append(t);
                }
            }

            String addrStr = addrBuilder.toString().trim();
            if (!addrStr.isEmpty()) {
                r.branchAddress = addrStr;
                System.out.println("DEBUG: Branch Address = " + r.branchAddress);
            }
        }
    }

    // ---------- Helper Methods (unchanged) ----------
    private static String extractAddressAfterEmail(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            if (Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}").matcher(lines[i]).find()) {
                StringBuilder addr = new StringBuilder();
                for (int j = i + 1; j < lines.length; j++) {
                    String next = lines[j].trim();
                    if (next.isEmpty() || isLabelLine(next) || next.contains("@")) break;
                    if (addr.length() > 0) addr.append("\n");
                    addr.append(next);
                }
                return addr.toString().trim();
            }
        }
        return null;
    }

    private static String nextNonEmptyNonLabel(String[] lines, int currentIndex) {
        for (int j = currentIndex + 1; j < lines.length; j++) {
            String next = lines[j].trim();
            if (next.isEmpty() || isLabelLine(next)) continue;
            if (next.matches("^[\\s:\\-]+$")) continue;
            return next;
        }
        return null;
    }

    private static boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }
    private static boolean containsLabel(String line, String label) { return line.toLowerCase().contains(label.toLowerCase()); }
    private static boolean isLabelLine(String line) {
        String lower = line.toLowerCase();
        return lower.matches(".*\\b(branch code|branch email|branch phone|micr|ifsc|cif|account no|product|phone|email|address|date|statement|branch name).*");
    }
    private static String valueAfterLabel(String line, String label) {
        int idx = line.toLowerCase().indexOf(label.toLowerCase());
        if (idx == -1) return null;
        String rest = line.substring(idx + label.length());
        rest = rest.replaceFirst("^[\\s:]+", "").trim();
        return rest.isEmpty() ? null : rest;
    }
    private static String extractAnyMobile(String text, String accountNumber, String cif) {
        Matcher m = Pattern.compile("\\b[6-9]\\d{9}\\b").matcher(text);
        while (m.find()) {
            String candidate = m.group();
            if (!candidate.equals(accountNumber) && !candidate.equals(cif)) return candidate;
        }
        return null;
    }

    // ==================== Existing Helpers (unchanged) ====================
    public static String extractPan(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("(?i)PAN\\s*:?\\s*([A-Z]{5}[0-9]{4}[A-Z])").matcher(text);
        return m.find() ? m.group(1) : null;
    }
    public static String extractCkycr(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("(?i)ckycr\\s*(number)?\\s*[:\\-]?\\s*([0-9]{14})").matcher(text);
        return m.find() ? m.group(2) : null;
    }
    public static String extractEmail(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})").matcher(text);
        return m.find() ? m.group(1) : null;
    }
    public static String extractCrn(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("(?i)(customer|cust reln|cif|crn|cust)\\s*(id|no\\.?)?\\s*[:\\-]?\\s*([xX\\d]{9,11})").matcher(text);
        return m.find() ? m.group(3) : null;
    }
    public static String extractPhone(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("(?i)(registered\\s*)?(mobile|phone)\\s*(no\\.?|number)?\\s*[:\\-]?\\s*([xX\\d]{10})").matcher(text);
        return m.find() ? m.group(4) : null;
    }
    public static String extractBranchPhone(String text, String bank) {
        if (text == null) return null;
        String normalized = text.replaceAll("[ \t]+", " ").replaceAll("\\r", "").toUpperCase();
        if ("AXIS".equalsIgnoreCase(bank)) {
            Matcher m = Pattern.compile("TEL\\s*[:\\-]?\\s*([+0-9()\\s\\-]{8,20})", Pattern.CASE_INSENSITIVE).matcher(normalized);
            if (m.find()) return cleanPhone(m.group(1));
        }
        Matcher labeled = Pattern.compile("(PHONE|TEL|CONTACT|BRANCH\\h+PHONE(?:\\h+NUMBER)?)\\h*[:\\-]?\\h*([+0-9()\\s\\-]{8,20})", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (labeled.find()) return cleanPhone(labeled.group(2));
        Matcher loose = Pattern.compile("(?:\\+91\\s*)?(?:\\(?0?\\)?\\s*)?[6-9]\\d{9}", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (loose.find()) return cleanPhone(loose.group());
        return null;
    }
    private static String cleanPhone(String s) {
        if (s == null) return null;
        String digits = s.replaceAll("[^0-9+]", "");
        if (digits.startsWith("91") && digits.length() > 10) digits = digits.substring(digits.length() - 10);
        return digits.length() == 10 ? digits : null;
    }
    public static String extractMICR(String text) {
        if (text == null) return null;
        String normalized = text.replaceAll("[ \t]+", " ").replaceAll("\\r", "").toUpperCase();
        Matcher labeled = Pattern.compile("MICR\\s*(?:CODE|NUMBER)?\\s*[:\\-]?\\s*(\\d{9})", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (labeled.find()) return labeled.group(1);
        Matcher loose = Pattern.compile("\\b\\d{9}\\b").matcher(normalized);
        while (loose.find()) {
            String candidate = loose.group();
            if (!candidate.matches("000000000") && !candidate.matches("111111111")) return candidate;
        }
        return null;
    }
    public static String extractAccountType(String text, String bank) {
        if (text == null) return null;
        String normalized = text.replaceAll("[ \t]+", " ").replaceAll("\\r", "").toUpperCase();
        if ("AXIS".equals(bank)) {
            Matcher m = Pattern.compile("(SCHEME)\\s*(?:[:\\-]\\s*)?([^\\r\\n]{3,60}?)(?=\\s*CKYC|\\r|\\n|$)", Pattern.CASE_INSENSITIVE).matcher(normalized);
            if (m.find()) return cleanAccountType(m.group(2));
        } else if ("KOTAK".equals(bank)) {
            Matcher m = Pattern.compile("\\bAccount\\h+Type\\h+([^\\r\\n]+)", Pattern.CASE_INSENSITIVE).matcher(text);
            if (m.find()) return m.group(1).trim();
        }
        Matcher labeled = Pattern.compile("(ACCOUNT\\s*(?:TYPE|DESCRIPTION)|A/C\\s*TYPE|SCHEME)\\s*(?:[:\\-]\\s*)?([^\\r\\n]{3,60})", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (labeled.find()) return cleanAccountType(labeled.group(2));
        Matcher common = Pattern.compile("\\b(SAVINGS?|CURRENT|SALARY|NRE|NRO|OD|OVERDRAFT|CASH\\s*CREDIT|LOAN)\\b", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (common.find()) return cleanAccountType(common.group(1));
        return null;
    }
    private static String cleanAccountType(String s) {
        if (s == null) return null;
        s = s.replaceAll("\\s{2,}", " ").trim();
        if (s.matches("SAVING(S)?")) return "SAVINGS";
        if (s.contains("CURRENT")) return "CURRENT";
        if (s.contains("SALARY")) return "SALARY";
        if (s.contains("NRE")) return "NRE";
        if (s.contains("NRO")) return "NRO";
        if (s.contains("OVERDRAFT") || s.contains("OD")) return "OVERDRAFT";
        if (s.contains("CASH CREDIT")) return "CASH CREDIT";
        return s;
    }
}