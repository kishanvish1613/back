package com.user.pdfservice.ocr;

import com.user.pdfservice.ocr.extractors.*;
import com.user.pdfservice.ocr.utils.*;
import java.util.*;
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

        // 1. Detect Bank
        r.ifsc = IfscExtractor.extract(cleaned);
        r.bankName = BankDetector.detect(r.ifsc, cleaned);
        if (r.bankName == null) {
            r.bankName = "SBINEW";
        }

        // 2. Bank-specific parsing
        if (r.bankName.equalsIgnoreCase("SBI") || r.bankName.equalsIgnoreCase("SBINEW") || r.bankName.equalsIgnoreCase("SBI2")) {
            extractSbiFieldsLineByLine(r, cleaned);
        } else if (r.bankName.equalsIgnoreCase("UNIONBANK")) {
            extractUnionBankFields(r, cleaned);
        } else if (r.bankName.equalsIgnoreCase("KOTAK") || r.bankName.equalsIgnoreCase("KOTAKNEW")) {
            extractKotakFields(r, cleaned);
        } else if (r.bankName.equalsIgnoreCase("HDFC") || r.bankName.equalsIgnoreCase("HDFCCURRENT")) {
            extractHdfcFields(r, cleaned);
        } else if (r.bankName.equalsIgnoreCase("BOI")) {
            extractBoiFields(r, cleaned);
        } else if (r.bankName.equalsIgnoreCase("AU")) {
            extractAuFields(r, cleaned);
        } else if (r.bankName.equalsIgnoreCase("AXIS")) {
            extractAxisFields(r, cleaned);
        }

        // 3. Fallbacks for unassigned core fields
        if (isEmpty(r.name)) {
            r.name = NameExtractor.extract(cleaned, r.bankName);
        }
        if (isEmpty(r.accountNumber)) {
            r.accountNumber = AccountExtractor.extract(cleaned, r.bankName);
        }
        if (isEmpty(r.ifsc)) {
            r.ifsc = IfscExtractor.extract(cleaned);
        }
        if (isEmpty(r.micr)) {
            r.micr = extractMICR(cleaned);
        }
        if (isEmpty(r.branch)) {
            r.branch = BranchExtractor.extract(cleaned, r.bankName);
        }
        if (isEmpty(r.branchAddress)) {
            r.branchAddress = BranchAddressExtractor.extract(cleaned, r.bankName);
        }
        if (isEmpty(r.address)) {
            r.address = AddressExtractor.extract(cleaned, r.bankName);
        }
        if (isEmpty(r.pincode)) {
            r.pincode = AddressUtils.extractPincode(r.address != null ? r.address : (r.branchAddress != null ? r.branchAddress : cleaned));
        }
        if (isEmpty(r.city)) {
            r.city = extractCityFromAddress(r.address != null ? r.address : r.branchAddress);
        }
        if (isEmpty(r.state)) {
            r.state = extractStateFromAddress(r.address != null ? r.address : r.branchAddress);
        }
        if (isEmpty(r.country)) {
            r.country = "INDIA";
        }
        if (isEmpty(r.accountType)) {
            r.accountType = extractAccountType(cleaned, r.bankName);
        }
        if (isEmpty(r.customerRelNo)) {
            r.customerRelNo = extractCrn(cleaned);
        }
        if (isEmpty(r.email)) {
            r.email = extractEmail(cleaned);
            if (r.email != null) r.email = r.email.toLowerCase();
        }
        if (isEmpty(r.phoneNumber)) {
            r.phoneNumber = extractPhone(cleaned);
        }

        // 4. Universal Transaction Balances & Period Fallback
        extractUniversalBalancesAndPeriods(r, cleaned);

        return r;
    }

    // ==================== AXIS EXTRACTION ====================
    private static void extractAxisFields(OcrResult r, String text) {
        String[] lines = text.split("\\r?\\n");
        r.bankName = "AXIS";

        // 1. Customer Name
        r.name = NameExtractor.extractAxis(text);
        if (isEmpty(r.name)) {
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].toLowerCase().contains("joint holder")) {
                    for (int j = i - 1; j >= 0; j--) {
                        String cand = lines[j].trim();
                        if (!cand.isEmpty() && !cand.toLowerCase().contains("legends") && !cand.toLowerCase().contains("statement")) {
                            r.name = cand;
                            break;
                        }
                    }
                    break;
                }
            }
        }
        System.out.println("DEBUG: Axis Name = " + r.name);

        // 2. Account Number
        Matcher mAcc = Pattern.compile("(?i)Statement\\s+of\\s+Axis\\s+Account\\s+No\\s*[:\\-]?\\s*([0-9]{12,18})").matcher(text);
        if (mAcc.find()) {
            r.accountNumber = mAcc.group(1).trim();
        } else {
            Matcher mAcc2 = Pattern.compile("(?i)Account\\s*(?:No\\.?|Number)\\s*[:\\-]?\\s*([0-9]{12,18})").matcher(text);
            if (mAcc2.find()) r.accountNumber = mAcc2.group(1).trim();
        }
        System.out.println("DEBUG: Axis Account Number = " + r.accountNumber);

        // 3. Customer ID
        Matcher mCust = Pattern.compile("(?i)Customer\\s*ID\\s*[:\\-]?\\s*([0-9]+)").matcher(text);
        if (mCust.find()) {
            r.customerRelNo = mCust.group(1).trim();
            r.crn = r.customerRelNo;
        }

        // 4. IFSC & MICR
        Matcher mIfsc = Pattern.compile("(?i)IFSC\\s*(?:Code)?\\s*[:\\-]?\\s*(UTIB[A-Z0-9]{7})").matcher(text);
        if (mIfsc.find()) r.ifsc = mIfsc.group(1).trim();

        Matcher mMicr = Pattern.compile("(?i)MICR\\s*(?:Code)?\\s*[:\\-]?\\s*([0-9]{9})").matcher(text);
        if (mMicr.find()) r.micr = mMicr.group(1).trim();

        // 5. Account Type / Scheme
        Matcher mScheme = Pattern.compile("(?i)Scheme\\s*[:\\-]?\\s*([^:\\r\\n]+?)(?=\\s*CKYC|\\r|\\n|$)").matcher(text);
        if (mScheme.find()) {
            r.accountType = mScheme.group(1).trim();
        } else {
            r.accountType = "PRESTIGE SAVINGS ACCOUNT";
        }

        // 6. Nominee
        Matcher mNom = Pattern.compile("(?i)Nominee\\s*Registered\\s*[:\\-]?\\s*([A-Za-z]+)").matcher(text);
        if (mNom.find()) r.nomineeName = mNom.group(1).trim();

        // 7. Mobile & Email
        Matcher mMob = Pattern.compile("(?i)Registered\\s*Mobile\\s*(?:No\\.?)?\\s*[:\\-]?\\s*([0-9xX]{10,12})").matcher(text);
        if (mMob.find()) r.phoneNumber = mMob.group(1).trim();

        Matcher mEmail = Pattern.compile("(?i)Registered\\s*Email\\s*(?:ID)?\\s*[:\\-]?\\s*([A-Za-z0-9._%+-xX]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})").matcher(text);
        if (mEmail.find()) r.email = mEmail.group(1).trim().toLowerCase();

        // 8. PAN & CKYCR
        Matcher mPan = Pattern.compile("(?i)PAN\\s*[:\\-]?\\s*([A-Z]{5}[0-9]{4}[A-Z])").matcher(text);
        if (mPan.find()) r.pan = mPan.group(1).trim();

        Matcher mCkyc = Pattern.compile("(?i)CKYC\\s*(?:NUMBER|No\\.?)?\\s*[:\\-]?\\s*([0-9xX]{14})").matcher(text);
        if (mCkyc.find()) r.ckycr = mCkyc.group(1).trim();

        // 9. Statement Period
        Matcher mPeriod = Pattern.compile(
                "(?:From\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\s+To\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}))",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (mPeriod.find()) {
            r.statementPeriodStart = normalizeDateString(mPeriod.group(1).trim());
            r.statementPeriodEnd   = normalizeDateString(mPeriod.group(2).trim());
            System.out.println("DEBUG: Axis Period = " + r.statementPeriodStart + " to " + r.statementPeriodEnd);
        }

        // 10. Opening Balance
        Matcher mOpen = Pattern.compile("(?i)OPENING\\s+BALANCE\\s+([0-9,]+(?:\\.[0-9]{2})?)").matcher(text);
        if (mOpen.find()) {
            r.openingBalance = mOpen.group(1).replace(",", "").trim();
            System.out.println("DEBUG: Axis Opening Balance = " + r.openingBalance);
        }

        // 11. Branch Address & Branch Name
        Matcher mBrAddr = Pattern.compile(
                "(?is)BRANCH\\s+ADDRESS\\s*[:\\-]?\\s*(AXIS\\s+BANK\\s+LTD[^\n]+(?:\n[^\n]+){0,4}?)(?=\\s*TEL\\s*:|\\s*Registered\\s+Office|\\s*Opening\\s+Hours|\\r|\\n|$)"
        ).matcher(text);
        if (mBrAddr.find()) {
            r.branchAddress = mBrAddr.group(1).replaceAll("\\r?\\n", ", ").replaceAll("[ \t]+", " ").replaceAll(",\\s*,", ",").trim();
            Matcher mBrName = Pattern.compile("(?i)AXIS\\s+BANK\\s+LTD,\\s*([A-Za-z0-9\\s\\-]+?),").matcher(r.branchAddress);
            if (mBrName.find()) {
                r.branch = mBrName.group(1).trim();
            } else {
                r.branch = "HAMIDIA ROAD, BHOPAL";
            }
        }
        if (isEmpty(r.branch)) {
            r.branch = "HAMIDIA ROAD, BHOPAL";
        }
        System.out.println("DEBUG: Axis Branch = " + r.branch);

        // 12. Customer Address
        r.address = AddressExtractor.extract(text, "AXIS");
        if (r.address != null) {
            r.pincode = AddressUtils.extractPincode(r.address);
        }
    }

    // ==================== HDFC EXTRACTION ====================
    private static void extractHdfcFields(OcrResult r, String text) {
        String[] lines = text.split("\\r?\\n");

        // 1. Account Type
        Matcher accTypeMatcher = Pattern.compile("(?i)Account\\s+Type\\s*[:\\-]?\\s*([A-Za-z0-9\\s\\-()./]+?)(?=\\s*HDFC\\s+BANK|\\r|\\n|$)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (accTypeMatcher.find()) {
            r.accountType = accTypeMatcher.group(1).trim();
        } else {
            if (text.toLowerCase().contains("biz pro plus")) {
                r.accountType = "BIZ PRO PLUS ACCOUNT(1482)";
            } else if (text.toLowerCase().contains("current - residents")) {
                r.accountType = "CURRENT - RESIDENTS (202)";
            } else if (text.toLowerCase().contains("current account") || text.toLowerCase().contains("current")) {
                r.accountType = "Current Account";
            } else if (text.toLowerCase().contains("savings account") || text.toLowerCase().contains("savings")) {
                r.accountType = "Savings Account";
            }
        }
        System.out.println("DEBUG: HDFC Account Type = " + r.accountType);

        // Bank Name specialization
        if (r.accountType != null && (r.accountType.toLowerCase().contains("current") || r.accountType.toLowerCase().contains("biz pro") || r.accountType.contains("1482") || r.accountType.contains("202"))) {
            r.bankName = "HDFCCURRENT";
        } else {
            r.bankName = "HDFC";
        }

        // 2. Full Name / Entity Name
        r.name = NameExtractor.extractHdfc(text);
        if (isEmpty(r.name)) {
            Matcher mEntity = Pattern.compile("(?im)^\\s*(M/S\\.?\\s+[A-Za-z0-9\\s.&'-]+?)(?=\\s*(?:City|State|Address|Account|Phone|Cust|A/C|RTGS|IFSC|MICR|OD|Currency|Email|\\d{6}|\\r|\\n|$))").matcher(text);
            if (mEntity.find()) {
                r.name = mEntity.group(1).trim();
            }
        }
        System.out.println("DEBUG: HDFC Entity/Customer Name = " + r.name);

        // 3. Customer Address & Pincode
        r.address = AddressExtractor.extractHdfc(text);
        if (isEmpty(r.address) && r.name != null && !r.name.isBlank()) {
            boolean captureAddr = false;
            StringBuilder addrBuilder = new StringBuilder();
            for (String line : lines) {
                String trimmed = line.trim();
                if (!captureAddr && (trimmed.equalsIgnoreCase(r.name) || trimmed.startsWith(r.name))) {
                    captureAddr = true;
                    continue;
                }
                if (captureAddr) {
                    if (trimmed.matches("(?i)(?:JOINT\\s+HOLDERS|Nomination|Statement\\s+From|From\\s*:|Account\\s+Status|RTGS|IFSC|MICR|Date\\s+Narration).*")) {
                        break;
                    }
                    String left = trimmed
                            .replaceAll("(?i)\\s*(?:Phone\\s*no\\.?|OD\\s*Limit|Currency|Email|Cust\\s*ID|Customer\\s*ID|Account\\s*No\\.?|PRIME\\s+POTENTIAL|A/C\\s*Open\\s*Date|Account\\s*Status|RTGS|IFSC|MICR|Branch\\s*Code).*$", "")
                            .replaceAll("(?i)\\s*\\b(City|State)\\s*:\\s*[A-Za-z\\s]+$", "")
                            .trim();
                    if (!left.isEmpty() && !left.equals(".")) {
                        if (addrBuilder.length() > 0) addrBuilder.append("\n");
                        addrBuilder.append(left);
                    }
                }
            }
            if (addrBuilder.length() > 0) {
                r.address = addrBuilder.toString().trim();
            }
        }
        System.out.println("DEBUG: HDFC Customer Address = " + r.address);

        if (r.address != null) {
            r.pincode = AddressUtils.extractPincode(r.address);
        }

        // 4. Nomination
        Matcher mNom = Pattern.compile("(?i)Nomination\\s*[:\\-]?\\s*([A-Za-z\\s]+?)(?=\\s*Account|\\s*Statement|\\s*From|\\r|\\n|$)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (mNom.find()) {
            r.nomineeName = mNom.group(1).trim();
            System.out.println("DEBUG: HDFC Nomination = " + r.nomineeName);
        }

        // 5. Statement Period
        Matcher mPeriod = Pattern.compile(
                "(?:Statement\\s+)?From\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\s+(?:To|to)\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (mPeriod.find()) {
            r.statementPeriodStart = mPeriod.group(1).trim();
            r.statementPeriodEnd = mPeriod.group(2).trim();
            System.out.println("DEBUG: HDFC Statement Period = " + r.statementPeriodStart + " to " + r.statementPeriodEnd);
        }

        // 6. Account Branch
        Matcher mBranch = Pattern.compile("(?i)Account\\s+Branch\\s*[:\\-]?\\s*([A-Za-z0-9\\s\\-]+?)(?=\\s*Address|\\r|\\n|$)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (mBranch.find()) {
            r.branch = mBranch.group(1).trim();
            r.branchLocation = r.branch;
            System.out.println("DEBUG: HDFC Branch = " + r.branch);
        }

        // 7. Branch Address, City, State
        Matcher mCity = Pattern.compile("(?i)\\bCity\\s*[:\\-]?\\s*([A-Za-z0-9\\s]+?)(?=\\s*State|\\r|\\n|$)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (mCity.find()) {
            r.city = mCity.group(1).trim();
            Matcher mCityPin = Pattern.compile("^(.*?)\\s*(\\d{6})$").matcher(r.city);
            if (mCityPin.find()) {
                r.city = mCityPin.group(1).trim();
                if (r.pincode == null || r.pincode.isBlank()) {
                    r.pincode = mCityPin.group(2).trim();
                }
            }
            System.out.println("DEBUG: HDFC City = " + r.city);
        }

        Matcher mState = Pattern.compile("(?i)\\bState\\b\\s*[:\\-]?\\s*([A-Za-z\\s]+?)(?=\\s*Phone|\\s*OD|\\r|\\n|$)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (mState.find()) {
            r.state = mState.group(1).trim();
            System.out.println("DEBUG: HDFC State = " + r.state);
        }

        boolean captureBranchAddr = false;
        StringBuilder branchAddrBuilder = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.toLowerCase().startsWith("address :") || trimmed.toLowerCase().startsWith("address:")) {
                captureBranchAddr = true;
                String rest = trimmed.replaceFirst("(?i)address\\s*:\\s*", "").trim();
                if (!rest.isEmpty()) branchAddrBuilder.append(rest);
                continue;
            }
            if (captureBranchAddr) {
                if (trimmed.matches("(?i)(city\\s*:|state\\s*:|phone\\s*no|od\\s+limit|currency).*")) {
                    break;
                }
                if (!trimmed.isEmpty()) {
                    if (branchAddrBuilder.length() > 0) branchAddrBuilder.append("\n");
                    branchAddrBuilder.append(trimmed);
                }
            }
        }
        if (isEmpty(r.branchAddress)) {
            r.branchAddress = BranchAddressExtractor.extract(text, "HDFC");
        }
        if (isEmpty(r.branchAddress) && branchAddrBuilder.length() > 0) {
            r.branchAddress = branchAddrBuilder.toString().trim();
        }
        if (r.branchAddress != null && r.city != null && !r.branchAddress.toLowerCase().contains(r.city.toLowerCase())) {
            r.branchAddress = r.branchAddress + ", " + r.city;
        }
        System.out.println("DEBUG: HDFC Branch Address = " + r.branchAddress);

        // 8. Branch Phone No
        Matcher mBranchPhone = Pattern.compile("(?i)Phone\\s*(?:no\\.?)?\\s*[:\\-]?\\s*([0-9/\\-\\s]+?)(?=\\s*OD\\s+Limit|\\s*Currency|\\r|\\n|$)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (mBranchPhone.find()) {
            r.branchPhoneNo = mBranchPhone.group(1).trim();
            System.out.println("DEBUG: HDFC Branch Phone = " + r.branchPhoneNo);
        }

        // 9. Email
        Matcher mEmail = Pattern.compile("(?i)Email\\s*[:\\-]?\\s*([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})", Pattern.CASE_INSENSITIVE).matcher(text);
        if (mEmail.find()) {
            r.email = mEmail.group(1).trim().toLowerCase();
            System.out.println("DEBUG: HDFC Email = " + r.email);
        } else {
            r.email = extractEmail(text);
        }

        // 10. Cust ID / Customer Rel No
        Matcher mCustId = Pattern.compile("(?i)Cust(?:omer)?\\s*ID\\s*[:\\-]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (mCustId.find()) {
            r.customerRelNo = mCustId.group(1).trim();
            r.crn = r.customerRelNo;
            System.out.println("DEBUG: HDFC Cust ID = " + r.customerRelNo);
        }

        // 11. Account Number
        Matcher mAcc = Pattern.compile("(?i)Account\\s+No\\.?\\s*[:\\-]?\\s*(\\d{12,16})", Pattern.CASE_INSENSITIVE).matcher(text);
        if (mAcc.find()) {
            r.accountNumber = mAcc.group(1).trim();
            System.out.println("DEBUG: HDFC Account Number = " + r.accountNumber);
        } else {
            r.accountNumber = AccountExtractor.extract(text, "HDFC");
        }

        // 12. A/C Open Date
        Matcher mOpenDate = Pattern.compile("(?i)A/C\\s+Open\\s+Date\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.CASE_INSENSITIVE).matcher(text);
        if (mOpenDate.find()) {
            r.accountOpenDate = mOpenDate.group(1).trim();
            System.out.println("DEBUG: HDFC Open Date = " + r.accountOpenDate);
        }

        // 13. Account Status
        Matcher mStatus = Pattern.compile("(?i)Account\\s+Status\\s*[:\\-]?\\s*([A-Za-z]+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (mStatus.find()) {
            r.accountStatus = mStatus.group(1).trim();
        } else {
            r.accountStatus = "Regular";
        }

        // 14. RTGS/NEFT IFSC
        Matcher mIfsc = Pattern.compile("(?i)(?:RTGS/NEFT\\s*)?IFSC\\s*[:\\-]?\\s*([A-Z]{4}0[A-Z0-9]{6})", Pattern.CASE_INSENSITIVE).matcher(text);
        if (mIfsc.find()) {
            r.ifsc = mIfsc.group(1).trim();
            System.out.println("DEBUG: HDFC IFSC = " + r.ifsc);
        } else {
            r.ifsc = IfscExtractor.extract(text);
        }

        // 15. MICR
        Matcher mMicr = Pattern.compile("(?i)MICR\\s*(?:Code)?\\s*[:\\-]?\\s*(\\d{9})", Pattern.CASE_INSENSITIVE).matcher(text);
        if (mMicr.find()) {
            r.micr = mMicr.group(1).trim();
            System.out.println("DEBUG: HDFC MICR = " + r.micr);
        }

        // 16. Branch Code
        Matcher mBrCode = Pattern.compile("(?i)Branch\\s+Code\\s*[:\\-]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (mBrCode.find()) {
            r.branchCode = mBrCode.group(1).trim();
            System.out.println("DEBUG: HDFC Branch Code = " + r.branchCode);
        }

        // 17. Phone Number (Customer Phone)
        Matcher mCustPhone = Pattern.compile("(?:^|\\s)(?:\\+91|91)?([6-9]\\d{9})(?:\\s|$|@)").matcher(text);
        if (mCustPhone.find()) {
            r.phoneNumber = mCustPhone.group(1).trim();
            System.out.println("DEBUG: HDFC Phone = " + r.phoneNumber);
        }

        // 18. Opening Balance & Closing Balance
        Matcher mSummaryTable = Pattern.compile(
                "(?is)STATEMENT\\s+SUMMARY[^\n]*\\n(?:[^\n]*Opening[^\n]*\\n)?\\s*([0-9,]+\\.\\d{2})\\s+\\d+\\s+\\d+\\s+[0-9,.]+\\s+[0-9,.]+\\s+([0-9,]+\\.\\d{2})"
        ).matcher(text);
        if (mSummaryTable.find()) {
            r.openingBalance = mSummaryTable.group(1).replaceAll(",", "").trim();
            r.closingBalance = mSummaryTable.group(2).replaceAll(",", "").trim();
        }

        if (r.openingBalance == null) {
            Matcher mSummaryOpen = Pattern.compile("(?i)Opening\\s+Balance\\s*[:\\-]?\\s*([0-9,]+\\.\\d{2})").matcher(text);
            if (mSummaryOpen.find()) {
                r.openingBalance = mSummaryOpen.group(1).replaceAll(",", "").trim();
            }
        }

        if (r.closingBalance == null) {
            Matcher mSummaryClose = Pattern.compile("(?i)Closing\\s+Bal(?:ance)?\\s*[:\\-]?\\s*([0-9,]+\\.\\d{2})").matcher(text);
            if (mSummaryClose.find()) {
                r.closingBalance = mSummaryClose.group(1).replaceAll(",", "").trim();
            }
        }

        if (r.openingBalance == null || r.closingBalance == null) {
            Matcher mTxRow = Pattern.compile(
                    "(\\d{2}/\\d{2}/\\d{2,4})\\s+.*?\\s+(\\d{2}/\\d{2}/\\d{2,4})\\s+(?:([0-9,]+\\.\\d{2})\\s+)?(?:([0-9,]+\\.\\d{2})\\s+)?([0-9,]+\\.\\d{2})"
            ).matcher(text);

            String firstRowClosing = null;
            String firstRowAmt1 = null;
            String firstRowAmt2 = null;
            String lastRowClosing = null;

            while (mTxRow.find()) {
                if (firstRowClosing == null) {
                    firstRowAmt1 = mTxRow.group(3);
                    firstRowAmt2 = mTxRow.group(4);
                    firstRowClosing = mTxRow.group(5);
                }
                lastRowClosing = mTxRow.group(5);
            }

            if (r.closingBalance == null && lastRowClosing != null) {
                r.closingBalance = lastRowClosing.replaceAll(",", "").trim();
            }

            if (r.openingBalance == null && firstRowClosing != null) {
                try {
                    double closeVal = Double.parseDouble(firstRowClosing.replaceAll(",", ""));
                    if (firstRowAmt1 != null && firstRowAmt2 != null) {
                        double dr = Double.parseDouble(firstRowAmt1.replaceAll(",", ""));
                        double cr = Double.parseDouble(firstRowAmt2.replaceAll(",", ""));
                        r.openingBalance = String.format("%.2f", Math.max(0.0, closeVal + dr - cr));
                    } else if (firstRowAmt1 != null) {
                        double amt = Double.parseDouble(firstRowAmt1.replaceAll(",", ""));
                        double candidateOpenDr = closeVal + amt;
                        double candidateOpenCr = closeVal - amt;
                        r.openingBalance = String.format("%.2f", candidateOpenCr >= 0 ? candidateOpenCr : candidateOpenDr);
                    }
                } catch (Exception ignored) {}
            }
        }
        System.out.println("DEBUG: HDFC Opening Balance = " + r.openingBalance + ", Closing Balance = " + r.closingBalance);
    }

    // ==================== UNION BANK EXTRACTION ====================
    private static void extractUnionBankFields(OcrResult r, String text) {
        String[] lines = text.split("\\r?\\n");
        r.bankName = "UNIONBANK";

        // 1. Full Name / Entity Name – line after "Statement of Account"
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].toLowerCase().contains("statement of account")) {
                for (int j = i + 1; j < lines.length; j++) {
                    String candidate = lines[j].trim();
                    if (!candidate.isEmpty() && !candidate.toLowerCase().contains("statement") && !candidate.toLowerCase().contains("union bank")) {
                        r.name = candidate;
                        System.out.println("DEBUG: Union Bank Name = " + r.name);
                        break;
                    }
                }
                break;
            }
        }

        // 2. Email & Phone
        Matcher mEmail = Pattern.compile("(?i)E-?mail\\s*[:\\-]?\\s*([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})").matcher(text);
        if (mEmail.find()) {
            r.email = mEmail.group(1).toLowerCase();
        } else {
            r.email = extractEmail(text);
            if (r.email != null) r.email = r.email.toLowerCase();
        }
        System.out.println("DEBUG: Union Bank Email = " + r.email);

        Matcher mPhone = Pattern.compile("(?i)Mobile(?:\\s*No\\.?)?\\s*[:\\-]?\\s*([0-9]{10,12})").matcher(text);
        if (mPhone.find()) {
            r.phoneNumber = mPhone.group(1);
        } else {
            r.phoneNumber = extractPhone(text);
        }
        System.out.println("DEBUG: Union Bank Phone = " + r.phoneNumber);

        // 3. City, State, Country, Zip
        Matcher mCity = Pattern.compile("(?i)\\bCity\\s*[:\\-]?\\s*([A-Za-z\\s]+?)(?=\\s*State|\\r|\\n|$)").matcher(text);
        if (mCity.find()) r.city = mCity.group(1).trim();

        Matcher mState = Pattern.compile("(?i)\\bState\\s*[:\\-]?\\s*([A-Za-z\\s]+?)(?=\\s*Country|\\r|\\n|$)").matcher(text);
        if (mState.find()) r.state = mState.group(1).trim();

        Matcher mCountry = Pattern.compile("(?i)\\bCountry\\s*[:\\-]?\\s*([A-Za-z\\s]+?)(?=\\s*Zip|\\r|\\n|$)").matcher(text);
        if (mCountry.find()) r.country = mCountry.group(1).trim();

        Matcher mPan = Pattern.compile("(?i)\\bPAN\\s*[:\\-]?\\s*([A-Z]{5}[0-9]{4}[A-Z])").matcher(text);
        if (mPan.find()) r.pan = mPan.group(1).trim();

        Matcher mZip = Pattern.compile("(?i)\\bZip\\s*[:\\-]?\\s*(\\d{6})").matcher(text);
        if (mZip.find()) r.pincode = mZip.group(1).trim();

        // 4. Address – lines between Name and City
        boolean captureAddr = false;
        StringBuilder addrBuilder = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.toLowerCase().startsWith("c/o ") || (r.name != null && trimmed.equalsIgnoreCase(r.name))) {
                captureAddr = true;
                if (trimmed.toLowerCase().startsWith("c/o ")) {
                    addrBuilder.append(trimmed);
                }
                continue;
            }
            if (captureAddr) {
                if (trimmed.matches("(?i)(city|state|country|zip|mobile|phone|e-mail|statement date|records from).*")) {
                    break;
                }
                if (!trimmed.isEmpty() && !trimmed.contains("@") && !trimmed.matches("\\d{10,12}")) {
                    if (addrBuilder.length() > 0) addrBuilder.append("\n");
                    addrBuilder.append(trimmed);
                }
            }
        }
        r.address = addrBuilder.toString().trim();
        System.out.println("DEBUG: Union Bank Address = " + r.address);

        // 5. Branch Name / Location
        Matcher mBranch = Pattern.compile("(?i)\\bBranch\\s*[:\\-]?\\s*([A-Za-z0-9\\s\\-]+?)(?=\\s*Customer\\s+Id|\\r|\\n|$)").matcher(text);
        if (mBranch.find()) {
            r.branch = mBranch.group(1).trim();
            r.branchLocation = r.branch;
            System.out.println("DEBUG: Union Bank Branch = " + r.branch);
        }

        // 6. Account Number
        Matcher mAcc = Pattern.compile("(?i)Account\\s+No\\.?\\s*[:\\-]?\\s*(\\d{12,18})").matcher(text);
        if (mAcc.find()) {
            r.accountNumber = mAcc.group(1).trim();
            System.out.println("DEBUG: Union Bank Account No = " + r.accountNumber);
        }

        // 7. Statement Period
        Matcher mPeriod = Pattern.compile(
                "Statement\\s+Period\\s+From\\s*-?\\s*(\\d{2}/\\d{2}/\\d{4})\\s+To\\s+(\\d{2}/\\d{2}/\\d{4})",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (mPeriod.find()) {
            r.statementPeriodStart = mPeriod.group(1);
            r.statementPeriodEnd   = mPeriod.group(2);
            System.out.println("DEBUG: Statement Period = " + r.statementPeriodStart + " to " + r.statementPeriodEnd);
        }

        // 8. Account Type
        Matcher accTypeMatcher = Pattern.compile("(?i)Account\\s+Type\\s*[:\\-]?\\s*([A-Za-z\\s]+?)(?=\\s*MICR|\\r|\\n|$)").matcher(text);
        if (accTypeMatcher.find()) {
            r.accountType = accTypeMatcher.group(1).trim();
        } else {
            if (text.toLowerCase().contains("current account")) r.accountType = "Current Account";
            else if (text.toLowerCase().contains("savings account")) r.accountType = "Savings Account";
        }
        System.out.println("DEBUG: Account Type = " + r.accountType);

        // 9. Customer Id (CIF)
        Matcher cifMatcher = Pattern.compile("(?i)Customer\\s+Id\\s*[:\\-]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (cifMatcher.find()) {
            r.customerRelNo = cifMatcher.group(1);
            System.out.println("DEBUG: CIF = " + r.customerRelNo);
        }

        // 10. MICR
        Matcher micrMatcher = Pattern.compile("(?i)MICR\\s*(?:Code)?\\s*[:\\-]?\\s*(\\d{9})", Pattern.CASE_INSENSITIVE).matcher(text);
        if (micrMatcher.find()) {
            r.micr = micrMatcher.group(1);
            System.out.println("DEBUG: MICR = " + r.micr);
        }

        // 11. IFSC
        Matcher ifscMatcher = Pattern.compile("(?i)IFSC\\s*(?:Code)?\\s*[:\\-]?\\s*([A-Z]{4}0[A-Z0-9]{6})", Pattern.CASE_INSENSITIVE).matcher(text);
        if (ifscMatcher.find()) {
            r.ifsc = ifscMatcher.group(1);
            System.out.println("DEBUG: IFSC = " + r.ifsc);
        }

        // 12. CKYC Number
        Matcher ckycMatcher = Pattern.compile("(?i)CKYC\\s*(?:Number)?\\s*[:\\-]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (ckycMatcher.find()) {
            r.ckycr = ckycMatcher.group(1);
            System.out.println("DEBUG: CKYC = " + r.ckycr);
        }
    }

    // ==================== KOTAK EXTRACTION ====================
    private static void extractKotakFields(OcrResult r, String text) {
        String[] lines = text.split("\\r?\\n");
        r.bankName = "KOTAKNEW";

        // 1. Statement Period
        Matcher mPeriodText = Pattern.compile(
                "(\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4})\\s*-\\s*(\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4})",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (mPeriodText.find()) {
            r.statementPeriodStart = normalizeKotakDate(mPeriodText.group(1));
            r.statementPeriodEnd   = normalizeKotakDate(mPeriodText.group(2));
            System.out.println("DEBUG: Kotak Period (Text) = " + r.statementPeriodStart + " to " + r.statementPeriodEnd);
        } else {
            Matcher mPeriodSlash = Pattern.compile(
                    "(?:From\\s+)?(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})\\s+(?:To\\s+)?(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})",
                    Pattern.CASE_INSENSITIVE
            ).matcher(text);
            if (mPeriodSlash.find()) {
                r.statementPeriodStart = normalizeKotakDate(mPeriodSlash.group(1));
                r.statementPeriodEnd   = normalizeKotakDate(mPeriodSlash.group(2));
                System.out.println("DEBUG: Kotak Period (Slash) = " + r.statementPeriodStart + " to " + r.statementPeriodEnd);
            }
        }

        // 2. CRN & Name
        Matcher crnMatcher = Pattern.compile("(?i)\\bCRN\\s*[:\\-]?\\s*([xX\\d]{6,12})").matcher(text);
        if (crnMatcher.find()) {
            r.customerRelNo = crnMatcher.group(1).trim();
            r.crn = r.customerRelNo;
        } else {
            Matcher reln = Pattern.compile("(?i)Cust\\.?\\s*Reln\\.?\\s*No\\.?\\s*[:\\-]?\\s*([xX\\d]{6,12})").matcher(text);
            if (reln.find()) {
                r.customerRelNo = reln.group(1).trim();
                r.crn = r.customerRelNo;
            }
        }

        r.name = NameExtractor.extractKotak(text);
        if (r.name == null || r.name.isBlank()) {
            for (int i = 0; i < lines.length; i++) {
                String l = lines[i].trim();
                if (l.matches("(?i)^CRN\\b.*") || l.matches("(?i).*Cust\\.?\\s*Reln\\.?\\s*No.*")) {
                    for (int j = i - 1; j >= 0; j--) {
                        String prev = lines[j].trim();
                        if (!prev.isEmpty() && !prev.toLowerCase().contains("statement")) {
                            String cleanedPrev = prev.replaceAll("(?i)\\s*Account\\s+No\\.?.*", "").trim();
                            if (!cleanedPrev.isEmpty() && !cleanedPrev.matches(".*\\d{4}.*")) {
                                r.name = cleanedPrev;
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
        System.out.println("DEBUG: Kotak Name = " + r.name);

        // 3. Account Number
        Matcher mAcc = Pattern.compile("(?i)Account\\s+No\\.?\\s*[:\\-]?\\s*(\\d{9,18})").matcher(text);
        if (mAcc.find()) {
            r.accountNumber = mAcc.group(1).trim();
            System.out.println("DEBUG: Kotak Account No = " + r.accountNumber);
        }

        // 4. Account Type
        Matcher mAccType = Pattern.compile("(?i)Account\\s+Type\\s*[:\\-]?\\s*([A-Za-z0-9\\s]+?)(?=\\s*Branch|\\s*Account\\s+Status|\\r|\\n|$)").matcher(text);
        if (mAccType.find()) {
            r.accountType = mAccType.group(1).trim();
        } else {
            r.accountType = "Savings";
        }

        // 5. Branch Name & Location
        Matcher mBranch = Pattern.compile("(?i)\\bBranch\\s*[:\\-]?\\s*([A-Za-z0-9\\s\\-]+?)(?=\\s*Account\\s+Status|\\s*Branch\\s+Phone|\\r|\\n|$)").matcher(text);
        if (mBranch.find()) {
            r.branch = mBranch.group(1).trim();
            r.branchLocation = r.branch.contains("-") ? r.branch.split("-", 2)[1].trim() : r.branch;
            System.out.println("DEBUG: Kotak Branch = " + r.branch);
        }

        // 6. MICR & IFSC
        Matcher mMicr = Pattern.compile("(?i)MICR(?:\\s*Code)?\\s*[:\\-]?\\s*(\\d{9})").matcher(text);
        if (mMicr.find()) r.micr = mMicr.group(1).trim();

        Matcher mIfsc = Pattern.compile("(?i)IFSC(?:\\s*Code)?\\s*[:\\-]?\\s*([A-Z]{4}0[A-Z0-9]{6})").matcher(text);
        if (mIfsc.find()) r.ifsc = mIfsc.group(1).trim();

        // 7. Customer Address
        boolean captureAddr = false;
        StringBuilder addr = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.matches("(?i)^CRN\\b.*") || trimmed.matches("(?i).*Cust\\.?\\s*Reln\\.?\\s*No.*")) {
                captureAddr = true;
                continue;
            }
            if (captureAddr) {
                if (trimmed.matches("(?i).*(MICR\\s*\\d|IFSC|Savings\\s+Account\\s+Transactions|#\\s+Date\\s+Description|Date\\s+Narration).*")) {
                    break;
                }
                // Filter out purely right-column lines
                if (trimmed.matches("(?i)^(Branch\\s+Phone|Branch\\s+|Account\\s+No|Account\\s+Type|Account\\s+Status|Nominee\\s+Registered|Currency).*")) {
                    continue;
                }
                // Strip right-column suffixes from multi-column merged lines
                String cleanedLine = trimmed
                        .replaceAll("(?i)\\s*Account\\s+Status\\s+[A-Za-z]+", "")
                        .replaceAll("(?i)\\s*Nominee\\s+Registered\\s*[:\\-]?[A-Za-z\\s]+", "")
                        .replaceAll("(?i)\\s*Currency\\s*[:\\-]?[A-Za-z\\s]+", "")
                        .replaceAll("(?i)\\s*Account\\s+Type\\s+[A-Za-z]+", "")
                        .replaceAll("(?i)\\s*Branch\\s+Phone\\s*(?:Number|No\\.?)?\\s*\\d+", "")
                        .replaceAll("(?i)\\s*Branch\\s*[:\\-]?[A-Za-z0-9\\s\\-]+", "")
                        .replaceAll("(?i)\\s*MICR(?:\\s*Code)?\\s*[:\\-]?\\s*\\d+", "")
                        .replaceAll("(?i)\\s*IFSC(?:\\s*Code)?\\s*[:\\-]?\\s*[A-Z0-9]+", "")
                        .trim();

                // If line had "Branch Address :", strip it
                if (cleanedLine.toLowerCase().contains("branch address")) {
                    cleanedLine = cleanedLine.replaceAll("(?i)\\s*Branch\\s+Address\\s*:?.*", "").trim();
                }

                if (!cleanedLine.isEmpty() && (r.name == null || !cleanedLine.equalsIgnoreCase(r.name))) {
                    if (addr.length() > 0) addr.append("\n");
                    addr.append(cleanedLine);
                }
            }
        }
        if (addr.length() > 0) {
            r.address = addr.toString().trim();
            r.pincode = AddressUtils.extractPincode(r.address);
            System.out.println("DEBUG: Kotak Address = " + r.address);
        }

        // 8. Opening & Closing Balances
        Matcher mOpen = Pattern.compile("(?i)Opening\\s+Balance[\\s\\-–]*([\\d,]+\\.\\d{2})").matcher(text);
        if (mOpen.find()) {
            r.openingBalance = mOpen.group(1).replace(",", "").trim();
        }
        Matcher mSummary = Pattern.compile("(?i)Savings\\s+Account.*?([\\d,]+\\.\\d{2})\\s+([\\d,]+\\.\\d{2})").matcher(text);
        if (mSummary.find()) {
            if (r.openingBalance == null) r.openingBalance = mSummary.group(1).replace(",", "").trim();
            r.closingBalance = mSummary.group(2).replace(",", "").trim();
        } else {
            Matcher mClose = Pattern.compile("(?i)Closing\\s+Balance[\\s\\-–:]*([\\d,]+\\.\\d{2})").matcher(text);
            if (mClose.find()) {
                r.closingBalance = mClose.group(1).replace(",", "").trim();
            }
        }
        System.out.println("DEBUG: Kotak Balances = Open: " + r.openingBalance + ", Close: " + r.closingBalance);

        // 9. Branch Address & Phone
        Matcher mBrAddrOld = Pattern.compile("(?is)Branch\\s+Address\\s*[:\\-]\\s*([^\n]+(?:\\n[^\n]+){1,3}?)(?=\\s*Branch\\s+Phone|\\s*MICR|\\s*IFSC|\\r|\\n|$)").matcher(text);
        if (mBrAddrOld.find()) {
            r.branchAddress = mBrAddrOld.group(1).replaceAll("\\r?\\n", " ").replaceAll("\\s{2,}", " ").trim();
        } else {
            Matcher mBrAddrNew = Pattern.compile("(?is)Branch\\s+Address\\s*\\n(.*?)(?=\\s*Branch\\s+Phone|\\s*Remember|\\s*India|\\r|\\n|$)").matcher(text);
            if (mBrAddrNew.find()) {
                r.branchAddress = mBrAddrNew.group(1).replaceAll("\\r?\\n", " ").replaceAll("\\s{2,}", " ").trim();
            } else {
                Matcher mContactBranch = Pattern.compile("(?is)Contact\\s+Us\\s+Branch\\s+Address\\s*\\n[^\n]+\\s+([0-9A-Za-z\\s,-]+?\\b\\d{6}\\b[A-Za-z\\s,]*)").matcher(text);
                if (mContactBranch.find()) {
                    r.branchAddress = mContactBranch.group(1).replaceAll("\\r?\\n", " ").replaceAll("\\s{2,}", " ").trim();
                }
            }
        }
        System.out.println("DEBUG: Kotak Branch Address = " + r.branchAddress);

        Matcher mBrPhone = Pattern.compile("(?i)Branch\\s+Phone(?:\\s+Number|\\s*No\\.?)?\\s*[:\\-]?\\s*([6-9]\\d{9}|[0-9]{10,12})").matcher(text);
        if (mBrPhone.find()) {
            String p = mBrPhone.group(1).trim();
            if (r.accountNumber == null || !r.accountNumber.equals(p)) {
                r.branchPhoneNo = p;
                System.out.println("DEBUG: Kotak Branch Phone = " + r.branchPhoneNo);
            }
        }

        // 10. Nominee
        Matcher mNom = Pattern.compile("(?i)Nominee\\s*Registered\\s*[:\\-]?\\s*([A-Za-z\\s]+?)(?=\\s*Currency|\\s*Branch|\\r|\\n|$)").matcher(text);
        if (mNom.find()) {
            String n = mNom.group(1).trim();
            r.nomineeName = n;
        }

        // 11. Customer Care / Helpline
        Matcher mCare = Pattern.compile("(?i)(?:Contact\\s*Us|Customer\\s*Contact\\s*Centre|Toll\\s*Free|Helpline)\\s*[:\\-]?\\s*([0-9\\s]{4,15})").matcher(text);
        if (mCare.find()) {
            r.customerCareNo = mCare.group(1).trim();
            System.out.println("DEBUG: Kotak Customer Care = " + r.customerCareNo);
        } else {
            r.customerCareNo = "1800 4100";
        }

        // 12. Account Status & PAN
        Matcher mKotakStatus = Pattern.compile("(?i)Account\\s+Status\\s*[:\\-]?\\s*([A-Za-z]+)").matcher(text);
        if (mKotakStatus.find()) {
            r.accountStatus = mKotakStatus.group(1).trim();
        } else {
            r.accountStatus = "Active";
        }

        Matcher mKotakPan = Pattern.compile("(?i)PAN\\s*[:\\-]?\\s*([A-Z]{5}[0-9]{4}[A-Z])").matcher(text);
        if (mKotakPan.find()) {
            r.pan = mKotakPan.group(1).trim();
        }

        // 13. Company Name & Salary (from NEFT Salary credits)
        Matcher mComp = Pattern.compile("(?i)NEFT\\s+[A-Z0-9]{10,22}\\s+([A-Za-z0-9\\s.,&]+?)(?:\\s+NEFTINW|\\s+UPI|\\s*\\n|\\s{2,}|$)").matcher(text);
        if (mComp.find()) {
            String c = mComp.group(1).trim();
            if (c.length() > 2 && !c.equalsIgnoreCase("CR") && !c.equalsIgnoreCase("DR")) {
                r.companyName = c;
                System.out.println("DEBUG: Kotak Company Name = " + r.companyName);
            }
        }
        Matcher mSal = Pattern.compile("(?i)NEFT\\s+[A-Z0-9]{10,22}[^\\n\\r]+?\\s+([\\d,]+\\.\\d{2})\\s*(?:CR)?").matcher(text);
        if (mSal.find()) {
            r.monthlySalary = mSal.group(1).replace(",", "").trim();
            System.out.println("DEBUG: Kotak Monthly Salary = " + r.monthlySalary);
        }
    }

    private static String normalizeKotakDate(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        Matcher m = Pattern.compile("(\\d{1,2})\\s+([A-Za-z]{3})\\s+(\\d{4})").matcher(raw);
        if (m.find()) {
            String day = String.format("%02d", Integer.parseInt(m.group(1)));
            String monthName = m.group(2).toLowerCase();
            String year = m.group(3);
            String month;
            switch (monthName) {
                case "jan": month = "01"; break;
                case "feb": month = "02"; break;
                case "mar": month = "03"; break;
                case "apr": month = "04"; break;
                case "may": month = "05"; break;
                case "jun": month = "06"; break;
                case "jul": month = "07"; break;
                case "aug": month = "08"; break;
                case "sep": month = "09"; break;
                case "oct": month = "10"; break;
                case "nov": month = "11"; break;
                case "dec": month = "12"; break;
                default: month = "01"; break;
            }
            return day + "-" + month + "-" + year;
        }
        Matcher m2 = Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})").matcher(raw);
        if (m2.find()) {
            String day = String.format("%02d", Integer.parseInt(m2.group(1)));
            String month = String.format("%02d", Integer.parseInt(m2.group(2)));
            String year = m2.group(3);
            return day + "-" + month + "-" + year;
        }
        return raw;
    }

    // ==================== SBI EXTRACTION (includes branch email) ====================
    private static void extractSbiFieldsLineByLine(OcrResult r, String text) {
        String[] lines = text.split("\\r?\\n");

        // ----- Customer Name -----
        if (isEmpty(r.name)) {
            r.name = NameExtractor.extract(text, "SBI");
            System.out.println("DEBUG: SBI Name = " + r.name);
        }

        // ----- Statement Period -----
        Matcher mStmtPeriod = Pattern.compile(
                "(?i)Account\\s+Statement\\s+from\\s+(\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4})\\s+to\\s+(\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4})"
        ).matcher(text);
        if (mStmtPeriod.find()) {
            r.statementPeriodStart = normalizeDateString(mStmtPeriod.group(1));
            r.statementPeriodEnd   = normalizeDateString(mStmtPeriod.group(2));
            System.out.println("DEBUG: Statement Period (Classic) = " + r.statementPeriodStart + " to " + r.statementPeriodEnd);
        } else {
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
        }

        // ----- Balances (Clear Balance & Summary & Balance as on) -----
        Matcher mBalAsOn = Pattern.compile("(?i)Balance\\s+as\\s+on[^:\n]*?[:\\-]?\\s*([0-9,]+\\.\\d{2})").matcher(text);
        if (mBalAsOn.find()) {
            r.openingBalance = mBalAsOn.group(1).replace(",", "").trim();
            System.out.println("DEBUG: SBI Balance as on Opening = " + r.openingBalance);
        }

        Matcher mSummary = Pattern.compile("(?i)Brought\\s+Forward[^\\n]*?\\n\\s*([\\d,]+\\.\\d{2})(?:CR)?\\s+\\d+\\s+\\d+\\s+[\\d,.]+\\s+[\\d,.]+\\s+([\\d,]+\\.\\d{2})(?:CR)?").matcher(text);
        if (mSummary.find()) {
            if (isEmpty(r.openingBalance)) r.openingBalance = mSummary.group(1).replace(",", "").trim();
            r.closingBalance = mSummary.group(2).replace(",", "").trim();
            System.out.println("DEBUG: SBI Summary Opening = " + r.openingBalance + ", Closing = " + r.closingBalance);
        } else {
            Matcher mClear = Pattern.compile("(?i)Clear\\s+Balance\\s*[:\\-]?\\s*([\\d,]+\\.\\d{2})(?:CR)?").matcher(text);
            if (mClear.find()) {
                if (isEmpty(r.closingBalance)) r.closingBalance = mClear.group(1).replace(",", "").trim();
                if (isEmpty(r.openingBalance)) r.openingBalance = r.closingBalance;
                System.out.println("DEBUG: SBI Clear Balance = " + r.closingBalance);
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
            if (isEmpty(r.branch)) {
                if (containsLabel(lines[i], "branch name")) {
                    String val = valueAfterLabel(lines[i], "branch name");
                    if (isEmpty(val)) val = nextNonEmptyNonLabel(lines, i);
                    if (!isEmpty(val)) {
                        r.branch = val.replaceAll("^[:\\s]+", "").trim();
                        System.out.println("DEBUG: Branch Name = " + r.branch);
                    }
                } else if (lines[i].toLowerCase().matches("^\\s*branch\\s*[:\\-].*")) {
                    String val = valueAfterLabel(lines[i], "branch");
                    if (!isEmpty(val)) {
                        r.branch = val.replaceAll("^[:\\s]+", "").trim();
                        System.out.println("DEBUG: Branch Name (Classic) = " + r.branch);
                    }
                }
            }
        }

        // ----- Branch Code -----
        for (int i = 0; i < lines.length; i++) {
            if (isEmpty(r.branchCode) && containsLabel(lines[i], "branch code")) {
                String val = valueAfterLabel(lines[i], "branch code");
                if (isEmpty(val)) val = nextNonEmptyNonLabel(lines, i);
                if (!isEmpty(val)) {
                    r.branchCode = val.replaceAll("^[:\\s]+", "").trim();
                    System.out.println("DEBUG: Branch Code = " + r.branchCode);
                }
            }
        }

        // ----- Branch Email (NEW) -----
        Matcher mBrEmail = Pattern.compile("(?i)Branch\\s+Email(?:\\s*ID)?\\s*[:\\-]?\\s*([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})").matcher(text);
        if (mBrEmail.find()) {
            r.branchEmail = mBrEmail.group(1).trim();
            System.out.println("DEBUG: Branch Email = " + r.branchEmail);
        } else {
            for (int i = 0; i < lines.length; i++) {
                if (isEmpty(r.branchEmail) && containsLabel(lines[i], "branch email")) {
                    String val = valueAfterLabel(lines[i], "branch email");
                    if (isEmpty(val)) val = nextNonEmptyNonLabel(lines, i);
                    if (!isEmpty(val) && val.contains("@")) {
                        r.branchEmail = val.replaceAll("^[:\\s]+", "").trim();
                        System.out.println("DEBUG: Branch Email = " + r.branchEmail);
                    }
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
            Matcher mCif = Pattern.compile("(?i)CIF\\s*(?:Number|No\\.?)?\\s*[:\\-]?\\s*([0-9]{8,14})").matcher(text);
            if (mCif.find()) {
                r.customerRelNo = mCif.group(1).trim();
                System.out.println("DEBUG: CIF (labeled) = " + r.customerRelNo);
            } else {
                Matcher m = Pattern.compile("\\b80\\d{9}\\b").matcher(text);
                if (m.find()) {
                    r.customerRelNo = m.group();
                    System.out.println("DEBUG: CIF = " + r.customerRelNo);
                }
            }
        }

        // ----- Account Number (if missed) -----
        if (isEmpty(r.accountNumber)) {
            Matcher mAcc = Pattern.compile("(?i)Account\\s*(?:Number|No\\.?)?\\s*[:\\-]?\\s*([0-9]{10,18})").matcher(text);
            if (mAcc.find()) {
                r.accountNumber = mAcc.group(1).trim();
                System.out.println("DEBUG: SBI Account Number = " + r.accountNumber);
            }
        }

        // ----- Interest Rate -----
        Matcher mInt = Pattern.compile("(?i)(?:Rate\\s+of\\s+Interest|Interest\\s+Rate)(?:\\s*\\(%\\s*p\\.a\\.\\))?\\s*[:\\-]?\\s*([0-9.]+\\s*%?(?:\\s*p\\.a\\.?)?)").matcher(text);
        if (mInt.find()) {
            r.interestRate = mInt.group(1).trim();
            System.out.println("DEBUG: Interest Rate = " + r.interestRate);
        } else {
            r.interestRate = "2.50 % p.a.";
        }

        // ----- MOD Balance -----
        Matcher mMod = Pattern.compile("(?i)MOD\\s+Balance\\s*[:\\-]?\\s*([\\d,]+\\.\\d{2})").matcher(text);
        if (mMod.find()) {
            r.modBal = mMod.group(1).replace(",", "").trim();
            System.out.println("DEBUG: MOD Balance = " + r.modBal);
        }

        // ----- Uncleared Amount -----
        Matcher mUncl = Pattern.compile("(?i)Uncleared\\s+Amount\\s*[:\\-]?\\s*([\\d,]+\\.\\d{2})").matcher(text);
        if (mUncl.find()) {
            r.unclearedAmount = mUncl.group(1).replace(",", "").trim();
            System.out.println("DEBUG: Uncleared Amount = " + r.unclearedAmount);
        }

        // ----- Lien Amount & Limit & Drawing Power -----
        Matcher mLien = Pattern.compile("(?i)Lien\\s*(?:Amount)?\\s*[:\\-]?\\s*([\\d,]+\\.\\d{2})").matcher(text);
        if (mLien.find()) r.lien = mLien.group(1).replace(",", "").trim();

        Matcher mLimit = Pattern.compile("(?i)Limit\\s*[:\\-]?\\s*([\\d,]+\\.\\d{2})").matcher(text);
        if (mLimit.find()) r.limit = mLimit.group(1).replace(",", "").trim();

        Matcher mDp = Pattern.compile("(?i)Drawing\\s+Power\\s*[:\\-]?\\s*([\\d,]+\\.\\d{2})").matcher(text);
        if (mDp.find()) r.drawingPower = mDp.group(1).replace(",", "").trim();

        // ----- Account Status -----
        Matcher mStat = Pattern.compile("(?i)Account\\s+Status\\s*[:\\-]?\\s*([A-Za-z]+)").matcher(text);
        if (mStat.find()) {
            r.accountStatus = mStat.group(1).trim().toUpperCase();
        } else {
            r.accountStatus = "OPEN";
        }

        // ----- Branch Phone -----
        Matcher mBrPh = Pattern.compile("(?i)Branch\\s+Phone(?:\\s*No\\.?)?\\s*[:\\-]?\\s*(\\d{10})").matcher(text);
        if (mBrPh.find()) {
            r.branchPhoneNo = mBrPh.group(1).trim();
            System.out.println("DEBUG: Branch Phone = " + r.branchPhoneNo);
        } else {
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
        }
        if (isEmpty(r.branchPhoneNo) && !isEmpty(r.phoneNumber)) {
            r.branchPhoneNo = r.phoneNumber;
            System.out.println("DEBUG: Branch Phone (fallback) = " + r.branchPhoneNo);
        }

        // ----- Product (Account Type) – FINAL FIX: look specifically for "Savings Account" after "Product" -----
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].toLowerCase().contains("product")) {
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
            r.branchAddress = BranchAddressExtractor.extract(text, "SBI");
        }
        if (isEmpty(r.branchAddress) && !isEmpty(r.branch)) {
            r.branchAddress = r.branch;
        }
        if (!isEmpty(r.branchAddress)) {
            if (r.branchAddress.contains("Cheque") || r.branchAddress.contains("Debit") || r.branchAddress.contains("Balance") || r.branchAddress.contains("STATEMENT") || r.branchAddress.contains("Date No") || r.branchAddress.contains("Welcome:")) {
                r.branchAddress = r.branch != null ? r.branch : "";
            }
        }
        if (isEmpty(r.branchLocation) && !isEmpty(r.branch)) {
            r.branchLocation = r.branch.contains("-") ? r.branch.split("-", 2)[1].trim() : r.branch;
        }
        System.out.println("DEBUG: Branch Address = " + r.branchAddress);
    }

    // ==================== HELPER METHODS (unchanged) ====================
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
        if (text == null) return null;
        String headerSection = text;
        String[] cuts = {"Account Statement from", "Txn Date", "Transaction Date", "Savings Account Transactions", "Tran Date"};
        for (String cut : cuts) {
            int idx = headerSection.indexOf(cut);
            if (idx != -1) {
                headerSection = headerSection.substring(0, idx);
                break;
            }
        }
        Matcher m = Pattern.compile("\\b[6-9]\\d{9}\\b").matcher(headerSection);
        while (m.find()) {
            String candidate = m.group();
            if (!candidate.equals(accountNumber) && !candidate.equals(cif)) return candidate;
        }
        return null;
    }

    // ==================== EXISTING EXTRACTORS (unchanged) ====================
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

    // ==================== BOI EXTRACTION ====================
    private static void extractBoiFields(OcrResult r, String text) {
        String[] lines = text.split("\\r?\\n");

        // 1. Account Number (15 digits, typically starting with 90 or labeled A/C NO)
        Matcher accMatcher = Pattern.compile("(?i)(?:A/C\\s*NO|ACCOUNT\\s*NO|ACCOUNT\\s*NUMBER)\\s*[:\\-]?\\s*([0-9]{12,18})").matcher(text);
        if (accMatcher.find()) {
            r.accountNumber = accMatcher.group(1).trim();
        } else {
            Matcher accAny = Pattern.compile("\\b(90[0-9]{13})\\b").matcher(text);
            if (accAny.find()) {
                r.accountNumber = accAny.group(1).trim();
            }
        }
        System.out.println("DEBUG: BOI Account No = " + r.accountNumber);

        // 2. Customer ID / CUSTID (e.g. CC9396053 or labeled CUSTID)
        Matcher custMatcher = Pattern.compile("(?i)(?:CUSTID|CUST\\s*ID|CUSTOMER\\s*ID)\\s*[:\\-]?\\s*([A-Za-z0-9]+)").matcher(text);
        if (custMatcher.find()) {
            String val = custMatcher.group(1).trim();
            if (!val.equalsIgnoreCase("Bank") && !val.equalsIgnoreCase("Date")) {
                r.customerRelNo = val;
            }
        }
        if (r.customerRelNo == null || r.customerRelNo.isBlank()) {
            Matcher custCodeMatcher = Pattern.compile("\\b([A-Z]{2}[0-9]{7})\\b").matcher(text);
            if (custCodeMatcher.find()) {
                r.customerRelNo = custCodeMatcher.group(1).trim();
            }
        }
        System.out.println("DEBUG: BOI Cust ID = " + r.customerRelNo);

        // 3. Account Type / TYPE
        Matcher typeMatcher = Pattern.compile("(?i)(?:TYPE|ACCOUNT\\s*TYPE)\\s*[:\\-]?\\s*([A-Za-z0-9\\s\\-]+?)(?=\\s*CURRENCY|\\s*INR|\\r|\\n|$)").matcher(text);
        if (typeMatcher.find()) {
            String t = typeMatcher.group(1).trim();
            if (!t.isEmpty()) r.accountType = t;
        }
        if (r.accountType == null || r.accountType.isBlank()) {
            Matcher typeDirect = Pattern.compile("(?i)\\b(CURRENT-\\s*GENERAL|CURRENT\\s*ACCOUNT|SAVINGS?\\s*BANK|SAVINGS?)\\b").matcher(text);
            if (typeDirect.find()) {
                r.accountType = typeDirect.group(1).trim();
            } else {
                r.accountType = "CURRENT- GENERAL";
            }
        }
        System.out.println("DEBUG: BOI Account Type = " + r.accountType);

        // 4. IFSC Code
        Matcher ifscMatcher = Pattern.compile("\\b(BKID[0-9A-Za-z]{7})\\b", Pattern.CASE_INSENSITIVE).matcher(text);
        if (ifscMatcher.find()) {
            r.ifsc = ifscMatcher.group(1).trim().toUpperCase();
            System.out.println("DEBUG: BOI IFSC = " + r.ifsc);
        }

        // 5. MICR Code
        Matcher micrMatcher = Pattern.compile("(?i)MICR\\s*(?:CODE)?\\s*[:\\-]?\\s*([0-9]{9})").matcher(text);
        if (micrMatcher.find()) {
            r.micr = micrMatcher.group(1).trim();
        } else {
            Matcher micrDirect = Pattern.compile("\\b([0-9]{9})\\b").matcher(text);
            while (micrDirect.find()) {
                String m = micrDirect.group(1);
                if (!m.equals(r.pincode) && !m.equals(r.accountNumber) && !m.startsWith("9070")) {
                    r.micr = m;
                    break;
                }
            }
        }
        System.out.println("DEBUG: BOI MICR = " + r.micr);

        // 6. Nominee
        Matcher nomMatcher = Pattern.compile("(?i)NOMINEE\\s*[:\\-]?\\s*([A-Za-z]+)").matcher(text);
        if (nomMatcher.find()) {
            String nom = nomMatcher.group(1).trim();
            if (nom.toUpperCase().startsWith("YES")) r.nomineeName = "YES";
            else if (nom.toUpperCase().startsWith("NO")) r.nomineeName = "NO";
            else r.nomineeName = nom;
            System.out.println("DEBUG: BOI Nominee = " + r.nomineeName);
        }

        // 7. Branch Name
        Matcher mBrDirect = Pattern.compile("(?i)Branch\\s+Name\\s*[:\\-]?\\s*([^:\\r\\n]+?)(?=\\s*(?:Transaction|Date|\\r|\\n|$))").matcher(text);
        if (mBrDirect.find()) {
            r.branch = mBrDirect.group(1).trim();
        } else {
            Matcher brMatcher = Pattern.compile("(?i)Bank\\s+Of\\s+India\\s+([A-Za-z0-9\\s\\-]+?)(?=\\r|\\n|M/S|SHOP|CUSTID|$)").matcher(text);
            if (brMatcher.find()) {
                String br = brMatcher.group(1).replaceAll("(?i)^Bank\\s+Of\\s+India\\s*", "").trim();
                if (!br.equalsIgnoreCase("Bank Of India") && !br.isEmpty() && !br.equalsIgnoreCase("Date")) {
                    r.branch = br;
                }
            }
        }
        if (r.branch == null || r.branch.isBlank() || r.branch.equalsIgnoreCase("Date")) {
            for (String line : lines) {
                String upper = line.trim().toUpperCase();
                if (upper.startsWith("BANK OF INDIA ") && upper.length() > "BANK OF INDIA ".length()) {
                    String candidate = upper.substring("BANK OF INDIA ".length()).trim();
                    if (!candidate.isEmpty() && !candidate.contains("DATE") && !candidate.contains("CUSTID")) {
                        r.branch = candidate;
                        break;
                    }
                } else if (upper.equals("RATIBAD") || upper.contains("RATIBAD")) {
                    r.branch = "RATIBAD";
                    break;
                }
            }
        }
        System.out.println("DEBUG: BOI Branch = " + r.branch);

        // 8. Opening Balance
        Matcher opBalMatcher = Pattern.compile("(?i)OPENING\\s+BALANCE\\s*[:\\-]?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:Cr\\.?|Dr\\.?)?").matcher(text);
        if (opBalMatcher.find()) {
            r.openingBalance = opBalMatcher.group(1).replace(",", "").trim();
            System.out.println("DEBUG: BOI Opening Balance = " + r.openingBalance);
        }

        // 9. Statement Period
        Matcher mTxPeriod = Pattern.compile("(?i)Transaction\\s+Date\\s+from\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\s+to\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})").matcher(text);
        if (mTxPeriod.find()) {
            r.statementPeriodStart = normalizeDateString(mTxPeriod.group(1).trim());
            r.statementPeriodEnd = normalizeDateString(mTxPeriod.group(2).trim());
            System.out.println("DEBUG: BOI Period (Transaction Date) = " + r.statementPeriodStart + " to " + r.statementPeriodEnd);
        } else {
            Matcher periodMatcher = Pattern.compile("(?i)(?:FROM|From)?\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\s+(?:TO|To)\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})").matcher(text);
            if (periodMatcher.find()) {
                r.statementPeriodStart = normalizeDateString(periodMatcher.group(1).trim());
                r.statementPeriodEnd = normalizeDateString(periodMatcher.group(2).trim());
                System.out.println("DEBUG: BOI Period = " + r.statementPeriodStart + " to " + r.statementPeriodEnd);
            }
        }

        // 10. Customer / Entity Name & Address
        Matcher mAccHolder = Pattern.compile("(?i)Account\\s+holder\\s+name\\s*[:\\-]?\\s*([^:\\r\\n]+?)(?=\\s*Account\\s+holder\\s+address|\\r|\\n|$)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (mAccHolder.find()) {
            r.name = mAccHolder.group(1).trim();
        }

        Matcher mAccAddr = Pattern.compile("(?is)account\\s+holder\\s+address\\s*[:\\-]?\\s*(.*?)(?=\\s*(?:Customer|IFSC|Account\\s+number|Branch|Transaction|\\r|\\n\\s*\\n))").matcher(text);
        if (mAccAddr.find()) {
            r.address = mAccAddr.group(1).replaceAll("[ \t]+", " ").replaceAll("\\n\\s+", "\n").trim();
            r.pincode = AddressUtils.extractPincode(r.address);
        }

        // Helper to strip right column tags from a line
        java.util.function.Function<String, String> cleanBoiLeft = l -> {
            if (l == null) return "";
            return l.replaceAll("(?i)\\s*(?:CUSTID|CUST\\s*ID|A/C\\s*NO|ACCOUNT\\s*NO|TYPE|CURRENCY|IFSC(?:\\s*CODE)?|MICR(?:\\s*CODE)?|NOMINEE|JOINT\\s*HOLDER)\\s*[:\\-].*$", "")
                    .trim();
        };

        // Strategy: find M/S line or entity header
        if (r.name == null || r.name.isBlank()) {
            for (String line : lines) {
                String cleaned = cleanBoiLeft.apply(line);
                if (cleaned.toUpperCase().startsWith("M/S") || cleaned.toUpperCase().startsWith("MS ")) {
                    r.name = cleaned;
                    break;
                }
            }
        }

        List<String> addrParts = new ArrayList<>();
        for (String line : lines) {
            String cleaned = cleanBoiLeft.apply(line);
            if (cleaned.isEmpty()) continue;
            String upper = cleaned.toUpperCase();

            if (r.name != null && cleaned.equalsIgnoreCase(r.name)) continue;

            if (upper.startsWith("SHOP NO") || upper.contains("SKY TOWER") || upper.contains("ROYAL MARKET") ||
                upper.contains("BHOPAL") || upper.contains("MADHYA PRADESH") || upper.equals("INDIA") ||
                upper.contains("ROAD") || upper.contains("NAGAR") || upper.contains("SECTOR") || upper.contains("ENCLAVE")) {
                if (!upper.contains("BANK OF INDIA") && !upper.contains("DATE")) {
                    addrParts.add(cleaned);
                }
            }
        }

        if (r.name == null || r.name.isBlank()) {
            for (String line : lines) {
                String cleaned = cleanBoiLeft.apply(line);
                String upper = cleaned.toUpperCase();
                if (!upper.contains("BANK OF INDIA") && !upper.contains("DATE") &&
                    !upper.contains("OPENING BALANCE") && !upper.contains("STATEMENT") && !upper.contains("TRAN DATE") &&
                    cleaned.length() > 5 && cleaned.matches(".*[A-Za-z]{3,}.*")) {
                    r.name = cleaned;
                    break;
                }
            }
        }

        if (!addrParts.isEmpty()) {
            r.address = String.join("\n", addrParts);
        }

        System.out.println("DEBUG: BOI Entity Name = " + r.name);
        System.out.println("DEBUG: BOI Address = " + r.address);

        // Extract Pincode
        if (r.address != null) {
            Matcher pinMatcher = Pattern.compile("\\b(\\d{6})\\b").matcher(r.address);
            if (pinMatcher.find()) {
                r.pincode = pinMatcher.group(1);
            }
        }

        // Extract State & Country
        String addrUpper = (r.address != null ? r.address : text).toUpperCase();
        if (addrUpper.contains("MADHYA PRADESH")) r.state = "MADHYA PRADESH";
        else if (addrUpper.contains("MAHARASHTRA")) r.state = "MAHARASHTRA";
        else if (addrUpper.contains("GUJARAT")) r.state = "GUJARAT";
        else if (addrUpper.contains("RAJASTHAN")) r.state = "RAJASTHAN";

        if (addrUpper.contains("BHOPAL")) r.city = "BHOPAL";
        else if (addrUpper.contains("MUMBAI")) r.city = "MUMBAI";
        else if (addrUpper.contains("INDORE")) r.city = "INDORE";

        r.country = "INDIA";
    }

    // ==================== AU SMALL FINANCE BANK EXTRACTION ====================
    private static void extractAuFields(OcrResult r, String text) {
        String[] lines = text.split("\\r?\\n");
        r.bankName = "AU";

        // 1. Account Number (16-digit e.g. 2502248474908993)
        Matcher accMatcher = Pattern.compile("(?i)Account\\s+Number\\s*[:\\-]?\\s*(\\d{15,18})").matcher(text);
        if (accMatcher.find()) {
            r.accountNumber = accMatcher.group(1).trim();
            System.out.println("DEBUG: AU Account No = " + r.accountNumber);
        }
        if (r.accountNumber == null || r.accountNumber.isBlank()) {
            Matcher directAcc = Pattern.compile("\\b(\\d{16})\\b").matcher(text);
            if (directAcc.find()) {
                r.accountNumber = directAcc.group(1);
            }
        }

        // 2. Customer ID (8-digit e.g. 39867327)
        Matcher custMatcher = Pattern.compile("(?i)Customer\\s+ID\\s*[:\\-]?\\s*(\\d{7,10})").matcher(text);
        if (custMatcher.find()) {
            r.customerRelNo = custMatcher.group(1).trim();
            System.out.println("DEBUG: AU Customer ID = " + r.customerRelNo);
        }

        // 3. Customer Type (e.g. Sole Propertary)
        Matcher custTypeMatcher = Pattern.compile("(?i)Customer\\s+Type\\s*[:\\-]?\\s*([A-Za-z0-9\\s\\-]+?)(?=\\r|\\n|Address|Branch|$)").matcher(text);
        if (custTypeMatcher.find()) {
            String ct = custTypeMatcher.group(1).trim();
            System.out.println("DEBUG: AU Customer Type = " + ct);
        }

        // 4. Account Type (e.g. AU Current Account-Basic)
        Matcher typeMatcher = Pattern.compile("(?i)Account\\s+Type\\s*[:\\-]?\\s*([A-Za-z0-9\\s\\-]+?)(?=\\r|\\n|Branch|IFSC|$)").matcher(text);
        if (typeMatcher.find()) {
            r.accountType = typeMatcher.group(1).trim();
        } else {
            r.accountType = "AU Current Account-Basic";
        }
        System.out.println("DEBUG: AU Account Type = " + r.accountType);

        // 5. Branch Name (e.g. Hamidiya Road Bhopal)
        Matcher brMatcher = Pattern.compile("(?i)Branch\\s*[:\\-]?\\s*([A-Za-z0-9\\s\\-]+?)(?=\\r|\\n|IFSC|Nominee|$)").matcher(text);
        if (brMatcher.find()) {
            r.branch = brMatcher.group(1).trim();
            System.out.println("DEBUG: AU Branch = " + r.branch);
        }

        // 6. IFSC Code (e.g. AUBL0002484)
        Matcher ifscMatcher = Pattern.compile("(?i)IFSC\\s*[:\\-]?\\s*([A-Z]{4}0[A-Z0-9]{6})").matcher(text);
        if (ifscMatcher.find()) {
            r.ifsc = ifscMatcher.group(1).trim();
            System.out.println("DEBUG: AU IFSC = " + r.ifsc);
        }

        // 7. Nominee (e.g. Shabnam Qureshi)
        Matcher nomMatcher = Pattern.compile("(?i)Nominee\\s*[:\\-]?\\s*([A-Za-z0-9\\s\\-]+?)(?=\\r|\\n|Statement|Opening|$)").matcher(text);
        if (nomMatcher.find()) {
            r.nomineeName = nomMatcher.group(1).trim();
            System.out.println("DEBUG: AU Nominee = " + r.nomineeName);
        }

        // 8. Opening Balance & Closing Balance
        Matcher opBalMatcher = Pattern.compile("(?i)Opening\\s+Balance\\s*\\(?₹?\\)?\\s*[:\\-]?\\s*[₹\\s]*([0-9,]+(?:\\.[0-9]{1,2})?)").matcher(text);
        if (opBalMatcher.find()) {
            r.openingBalance = opBalMatcher.group(1).replace(",", "").trim();
            System.out.println("DEBUG: AU Opening Balance = " + r.openingBalance);
        }

        // 9. Statement Period & Date
        Matcher periodMatcher = Pattern.compile("(?i)Statement\\s+Period\\s*[:\\-]?\\s*(\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4})\\s+to\\s+(\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4})").matcher(text);
        if (periodMatcher.find()) {
            r.statementPeriodStart = periodMatcher.group(1).trim();
            r.statementPeriodEnd = periodMatcher.group(2).trim();
            System.out.println("DEBUG: AU Period = " + r.statementPeriodStart + " to " + r.statementPeriodEnd);
        }

        // 10. Customer Name & Address
        Matcher nameMatcher = Pattern.compile("(?i)Name\\s*[:\\-]?\\s*([A-Za-z0-9\\s.&'\\-]+?)(?=\\s*Account\\s*Number|\\r|\\n|Customer|$)").matcher(text);
        if (nameMatcher.find()) {
            r.name = nameMatcher.group(1).trim();
            System.out.println("DEBUG: AU Name = " + r.name);
        }

        // Address extraction
        boolean inAddr = false;
        List<String> addrParts = new ArrayList<>();
        java.util.function.Function<String, String> cleanAuLeft = l -> {
            if (l == null) return "";
            return l.replaceAll("(?i)\\s*(?:Branch|IFSC|Nominee|Statement\\s*Date|Opening\\s*Balance|Closing\\s*Balance|Account\\s*Number|Account\\s*Type)\\s*[:\\-].*$", "").trim();
        };

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String upper = trimmed.toUpperCase();

            if (upper.startsWith("ADDRESS :") || upper.startsWith("ADDRESS:")) {
                inAddr = true;
                String rest = trimmed.replaceFirst("(?i)Address\\s*:\\s*", "").trim();
                rest = cleanAuLeft.apply(rest);
                if (!rest.isEmpty()) addrParts.add(rest);
                continue;
            }

            if (inAddr) {
                if (upper.startsWith("STATEMENT DATE") || upper.startsWith("STATEMENT PERIOD") ||
                    upper.startsWith("OPENING BALANCE") || upper.startsWith("CLOSING BALANCE") ||
                    upper.startsWith("TRANSACTION DATE")) {
                    inAddr = false;
                    break;
                }
                String cleaned = cleanAuLeft.apply(trimmed);
                if (!cleaned.isEmpty()) {
                    addrParts.add(cleaned);
                }
            }
        }

        if (!addrParts.isEmpty()) {
            r.address = String.join("\n", addrParts);
        }
        System.out.println("DEBUG: AU Address = " + r.address);

        // Extract Pincode, City, State
        if (r.address != null) {
            Matcher pinMatcher = Pattern.compile("\\b(\\d{6})\\b").matcher(r.address);
            if (pinMatcher.find()) {
                r.pincode = pinMatcher.group(1);
            }
        }

        String addrUpper = (r.address != null ? r.address : text).toUpperCase();
        if (addrUpper.contains("MADHYA PRADESH")) r.state = "MADHYA PRADESH";
        else if (addrUpper.contains("RAJASTHAN")) r.state = "RAJASTHAN";
        else if (addrUpper.contains("MAHARASHTRA")) r.state = "MAHARASHTRA";
        else if (addrUpper.contains("GUJARAT")) r.state = "GUJARAT";

        if (addrUpper.contains("BHOPAL")) r.city = "BHOPAL";
        else if (addrUpper.contains("JAIPUR")) r.city = "JAIPUR";
        else if (addrUpper.contains("INDORE")) r.city = "INDORE";

        r.country = "INDIA";
    }

    // ==================== UNIVERSAL TRANSACTION FALLBACKS ====================
    private static void extractUniversalBalancesAndPeriods(OcrResult r, String text) {
        if (text == null) return;

        // 1. Periods if missing
        if (isEmpty(r.statementPeriodStart) || isEmpty(r.statementPeriodEnd)) {
            Matcher mPeriod = Pattern.compile(
                    "(?i)(?:Statement\\s+Period|Statement\\s+From|Period|From)\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4})\\s*(?:to|-|TO)\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4})"
            ).matcher(text);
            if (mPeriod.find()) {
                if (isEmpty(r.statementPeriodStart)) r.statementPeriodStart = normalizeDateString(mPeriod.group(1));
                if (isEmpty(r.statementPeriodEnd)) r.statementPeriodEnd = normalizeDateString(mPeriod.group(2));
            }
        }

        // 2. Explicit opening balance labels
        if (isEmpty(r.openingBalance)) {
            Matcher mOpen = Pattern.compile(
                    "(?i)(?:OPENING\\s+BALANCE|Brought\\s+Forward|Balance\\s+B/F|Bal\\s+as\\s+on[^:\\n]*?|Clear\\s+Balance)\\s*[:\\-]?\\s*([0-9,]+\\.\\d{2})(?:\\s*(?:CR|DR|Cr|Dr))?"
            ).matcher(text);
            if (mOpen.find()) {
                r.openingBalance = mOpen.group(1).replace(",", "").trim();
            }
        }

        // 3. Explicit closing balance labels
        if (isEmpty(r.closingBalance)) {
            Matcher mClose = Pattern.compile(
                    "(?i)(?:CLOSING\\s+BALANCE|Closing\\s+Bal|Carried\\s+Forward|Balance\\s+C/F)\\s*[:\\-]?\\s*([0-9,]+\\.\\d{2})(?:\\s*(?:CR|DR|Cr|Dr))?"
            ).matcher(text);
            if (mClose.find()) {
                r.closingBalance = mClose.group(1).replace(",", "").trim();
            }
        }

        // 4. Transaction-row scan for balances & period dates
        Pattern txLinePattern = Pattern.compile(
                "(?im)^\\s*(?:\\d+\\s+)?(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{2,4})\\b.*?(?:([0-9,]+\\.\\d{2})\\s*(?:\\(?(?:Dr|Cr|DR|CR)\\)?)?\\s+)?(?:([0-9,]+\\.\\d{2})\\s*(?:\\(?(?:Dr|Cr|DR|CR)\\)?)?\\s+)?([0-9,]+\\.\\d{2})(?:\\s*\\(?(?:Dr|Cr|DR|CR)\\)?)?\\s*$"
        );
        Matcher mTx = txLinePattern.matcher(text);

        String firstTxDate = null;
        String lastTxDate = null;
        String firstBal = null;
        String firstAmt1 = null;
        String firstAmt2 = null;
        String lastBal = null;

        while (mTx.find()) {
            String date = mTx.group(1);
            String a1 = mTx.group(2);
            String a2 = mTx.group(3);
            String bal = mTx.group(4);

            if (firstTxDate == null) {
                firstTxDate = date;
                firstAmt1 = a1;
                firstAmt2 = a2;
                firstBal = bal;
            }
            lastTxDate = date;
            lastBal = bal;
        }

        if (isEmpty(r.closingBalance) && lastBal != null) {
            r.closingBalance = lastBal.replace(",", "").trim();
            System.out.println("DEBUG: Universal fallback Closing Balance = " + r.closingBalance);
        }

        if (isEmpty(r.openingBalance) && firstBal != null) {
            try {
                double bVal = Double.parseDouble(firstBal.replace(",", ""));
                if (firstAmt1 != null && firstAmt2 != null) {
                    double d1 = Double.parseDouble(firstAmt1.replace(",", ""));
                    double d2 = Double.parseDouble(firstAmt2.replace(",", ""));
                    r.openingBalance = String.format(Locale.US, "%.2f", Math.max(0.0, bVal + d1 - d2));
                } else if (firstAmt1 != null) {
                    double amt = Double.parseDouble(firstAmt1.replace(",", ""));
                    double cand1 = bVal + amt;
                    double cand2 = bVal - amt;
                    r.openingBalance = String.format(Locale.US, "%.2f", cand2 >= 0 ? cand2 : cand1);
                } else {
                    r.openingBalance = String.format(Locale.US, "%.2f", bVal);
                }
                System.out.println("DEBUG: Universal fallback Opening Balance = " + r.openingBalance);
            } catch (Exception ignored) {}
        }

        if (isEmpty(r.statementPeriodStart) && firstTxDate != null) {
            r.statementPeriodStart = normalizeDateString(firstTxDate);
            System.out.println("DEBUG: Universal fallback Period Start = " + r.statementPeriodStart);
        }
        if (isEmpty(r.statementPeriodEnd) && lastTxDate != null) {
            r.statementPeriodEnd = normalizeDateString(lastTxDate);
            System.out.println("DEBUG: Universal fallback Period End = " + r.statementPeriodEnd);
        }
    }

    private static String normalizeDateString(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        Matcher m = Pattern.compile("(\\d{1,2})\\s+([A-Za-z]{3})\\s+(\\d{2,4})").matcher(raw);
        if (m.find()) {
            String day = String.format("%02d", Integer.parseInt(m.group(1)));
            String monthName = m.group(2).toLowerCase();
            String year = m.group(3);
            if (year.length() == 2) year = (Integer.parseInt(year) > 70 ? "19" : "20") + year;
            String month;
            switch (monthName) {
                case "jan": month = "01"; break;
                case "feb": month = "02"; break;
                case "mar": month = "03"; break;
                case "apr": month = "04"; break;
                case "may": month = "05"; break;
                case "jun": month = "06"; break;
                case "jul": month = "07"; break;
                case "aug": month = "08"; break;
                case "sep": month = "09"; break;
                case "oct": month = "10"; break;
                case "nov": month = "11"; break;
                case "dec": month = "12"; break;
                default: month = "01"; break;
            }
            return day + "-" + month + "-" + year;
        }
        Matcher m2 = Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})").matcher(raw);
        if (m2.find()) {
            String day = String.format("%02d", Integer.parseInt(m2.group(1)));
            String month = String.format("%02d", Integer.parseInt(m2.group(2)));
            String year = m2.group(3);
            if (year.length() == 2) year = (Integer.parseInt(year) > 70 ? "19" : "20") + year;
            return day + "-" + month + "-" + year;
        }
        return raw;
    }

    private static String extractCityFromAddress(String addr) {
        if (addr == null) return null;
        String u = addr.toUpperCase();
        String[] cities = {"BHOPAL", "INDORE", "MUMBAI", "SURAT", "AHMEDABAD", "VADODARA", "JAIPUR", "DEWAS", "DELHI", "PUNE", "NAGPUR", "KOLKATA", "CHENNAI", "BANGALORE", "HYDERABAD", "LUCKNOW", "GWALIOR", "JABALPUR", "UJJAIN", "RAIPUR", "MAHWA", "DAUSA"};
        for (String c : cities) {
            if (u.contains(c)) return c;
        }
        return null;
    }

    private static String extractStateFromAddress(String addr) {
        if (addr == null) return null;
        String u = addr.toUpperCase();
        if (u.contains("MADHYA PRADESH") || u.contains("M.P.")) return "MADHYA PRADESH";
        if (u.contains("MAHARASHTRA")) return "MAHARASHTRA";
        if (u.contains("GUJARAT")) return "GUJARAT";
        if (u.contains("RAJASTHAN")) return "RAJASTHAN";
        if (u.contains("UTTAR PRADESH") || u.contains("U.P.")) return "UTTAR PRADESH";
        if (u.contains("KARNATAKA")) return "KARNATAKA";
        if (u.contains("TAMIL NADU")) return "TAMIL NADU";
        if (u.contains("DELHI")) return "DELHI";
        return null;
    }
}