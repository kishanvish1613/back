package com.user.pdfservice.template.kotaknew;

import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.Transaction;
import com.user.pdfservice.template.BankPdfTemplate;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

@SuppressWarnings("deprecation")
public class KotakNewTemplate implements BankPdfTemplate {

    private final float TXNROW_H = 10.68f;
    private final float LINE_H = 7.68f;

    private static final float DESC_WIDTH = 149.47f;
    private static final float DESC_X = 119.19f;

    private final float PAGE1_TXN_START = 438.14f;

    private final float[] TXN_X = {
            39f,
            72.82f,
            119.19f,
            274.66f,
            391.17f,
            460.22f,
            529.27f,
    };
    private float RIGHT_BAL;
    private float RIGHT_DR;
    private float RIGHT_CR;
    private Map<String, BufferedImage> extractedImages = new LinkedHashMap<>();
    private Statement stmt;
    private double CLOSING_BAL;

    private final String[] notices = new String[] {
            "RBI mandates Positive Pay for high-value cheques from Jan 1, 2021. Customers must submit cheque details via Net/Mobile Banking or at the branch on the day of issuance or before handing it to the beneficiary. For more details, visit www.kotak.bank.in.",
            "From October 4, 2025, same-day cheque clearing will be implemented across all banks. Cheques will be credited or debited within a few hours of issuance.",
            "Complimentary insurance cover on Kotak Debit Cards (linked to Saving and Current accounts) will be discontinued w.e.f. July 20, 2025. All claims will be accepted until July 20, 2025, as per the existing process. Salary account holders may view their insurance covers under Debit Card Services in the Cards & FASTag section on the web portal. For any queries related to Debit Card insurance, write to dc.insurance@kotak.com or visit https://www.kotak.bank.in/en/personal-banking /cards/debit-cards/debit-card-services/insurnace-on-debit-card.html",
            "TDS is applicable from time to time as per the Income Tax Act, 2025 (IT Act) and Income Tax Rules, 2026. Currently, TDS is deducted when the aggregate interest paid / credited across all FDs, RDs exceeds the threshold limit of Rs. 50,000/- for residents and Rs. 1,00,000/- for resident senior citizens in a financial year (FY) for a customer.",
            "You can avail a Tax Deduction at Source (TDS) exemption on the interest earned on your existing or new Fixed Deposits for the current financial year by submitting Form 121 (earlier Form 15G/15H). This exemption is available if you are eligible and the total interest earned on your bank deposits in a financial year exceeds the TDS exemption threshold. If the total interest earned on your bank deposits is below the TDS exemption threshold, submission of Form 121 is not required. A fresh Form 121 must be submitted at the beginning of each financial year for Fixed Deposits and Recurring Deposits.",
            "RBI, vide its circular DOR.CRE.REC.23/21.08.008/2022-23 dated April 19, 2022, has issued guidelines pertaining to the opening and maintenance of Current Account(s) of customers who have availed various credit facilities from the banking system. The term \"banking system\" refers to Scheduled Commercial Banks and Payments Banks. Banks (whether lending banks or otherwise) are required to monitor all Current Account, Overdraft, and Cash Credit accounts on a regular basis, at least on a half-yearly frequency. This monitoring must specifically consider the aggregate exposure of the banking system to the borrower vis-à-vis the individual bank's share in that exposure, in order to ensure compliance with the said instructions. Detailed guidelines are available in the aforementioned circular.",
            "Deposits of up to ₹5,00,000 per depositor are fully insured by the Deposit Insurance and Credit Guarantee Corporation, under the Deposit Insurance Scheme.",
            "Keep your account active for uninterrupted access to your funds: Inoperative accounts can be easily reactivated by submitting a signed request along with valid KYC documents. For details, please click https://www.kotak.bank.in/en/reach-us.html",
            "Registering a nominee is strongly recommended: A nominee can help your family access funds lying in your inoperative account smoothly. In the event of the account holder's demise, the nominee may visit our nearest branch with the required documents for verification and settlement of claims.",
            "Goods and Services Tax (GST), at the applicable rate of 18%, is levied on relevant service charges.",
            "Dear Customer, any inaccurate, incomplete or false disclosure of statement of financial transaction or reportable account by you would lead to penal consequence on the Bank under applicable law. The Bank shall be entitled to recover from you such amount levied due to such inaccuracy, incompleteness or false disclosure. You will indemnify the Bank in respect of all or any liabilities incurred by Bank, by reason of any of the information or particulars given by you, being incorrect or false or being suppressed or omitted.",
            "Please note: This statement/ advice should not be construed as a Tax Invoice under the Goods and Services Tax Act."
    };

    @Override
    public byte[] generate(Statement statement) {
        this.stmt = statement;
        if (stmt.transactions != null && !stmt.transactions.isEmpty()) {
            Transaction first = stmt.transactions.get(0);
            double computedStarting = first.balance - (first.credit - first.debit);
            stmt.details.startingBalance = Math.round(computedStarting * 100.0) / 100.0;
            CLOSING_BAL = Math.max(0, stmt.transactions.get(stmt.transactions.size() - 1).balance);
        } else {
            CLOSING_BAL = Math.max(0, stmt.details.startingBalance);
        }

        try (PDDocument doc = new PDDocument()) {
            // PDF version 1.5
            doc.getDocument().setVersion(1.5f);

            PDDocumentInformation info = new PDDocumentInformation();
            info.setProducer("OpenPDF 2.0.3");
            Calendar now = Calendar.getInstance();
            info.setCreationDate(now);
            doc.setDocumentInformation(info);

            PDDocument original = PDDocument.load(new File("kotaknew.pdf"));
            extractImagesFromPage(original, 0, extractedImages);
            extractImagesFromPage(original, 2, extractedImages);
            extractImagesFromPage(original, 3, extractedImages);

            PDRectangle A4_EXACT = new PDRectangle(595f, 842f);
            PDPage page = new PDPage(A4_EXACT);
            doc.addPage(page);

            Fonts fonts = Fonts.load(doc);

            RIGHT_BAL = 535f + (fonts.regular.getStringWidth("464.93") / 1000f) * 7.68f;
            RIGHT_CR = 465.95f + (fonts.regular.getStringWidth("400.00") / 1000f) * 7.68f;
            RIGHT_DR = 391.17f + (fonts.regular.getStringWidth("2,900.00") / 1000f) * 7.68f;

            PDPageContentStream cs = new PDPageContentStream(doc, page, AppendMode.APPEND, false);

            PDResources res = page.getResources();
            if (res == null) {
                res = new PDResources();
                page.setResources(res);
            }

            var first = extractedImages.entrySet().iterator().next();
            PDImageXObject ximg = LosslessFactory.createFromImage(doc, first.getValue());
            page.getResources().put(COSName.getPDFName(first.getKey()), ximg);

            cs.appendRawCommands("q\n");
            cs.appendRawCommands("q 595 0 0 74.85 0 767.15 cm /img0 Do Q\n");
            cs.appendRawCommands("Q\n");
            cs.appendRawCommands("q\n");

            renderHeader(cs, fonts);
            renderTransactionTableHeader(cs, fonts, 487.5f);
            renderOpeningBalance(cs, fonts);
            cs.close();

            PageCursor pc = new PageCursor(doc, fonts, page, PAGE1_TXN_START);
            drawAllTransactions(stmt.transactions, pc, fonts);
            pc.nextPageWithImages(extractedImages, 1, 6);
            drawSummaryPage(pc, fonts);
            pc.nextPageWithImages(extractedImages, 7, 8);
            drawLastPage(pc, fonts);
            pc.close();

            drawHeaderAndFooter(doc, fonts);

            // Force catalog version to 1.5
            doc.getDocumentCatalog().getCOSObject().setName(COSName.VERSION, "1.5");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private static void extractImagesFromPage(PDDocument doc, int pageIndex, Map<String, BufferedImage> out) throws IOException {
        PDPage page = doc.getPage(pageIndex);
        PDResources resources = page.getResources();
        if (resources == null) return;
        for (COSName name : resources.getXObjectNames()) {
            PDXObject xobj = resources.getXObject(name);
            if (xobj instanceof PDImageXObject img) {
                out.putIfAbsent(name.getName(), img.getImage());
            }
        }
    }

    // =========================================================================
    // PAGE 1: HEADER & USER ACCOUNT DETAILS
    // =========================================================================
    private void renderHeader(PDPageContentStream cs, Fonts fonts) throws Exception {
        /* =========================================================
         * "Account Statement" Title & Period Range
         * • Title: "Account Statement" at Y = 724.50 (25pt SemiBold Black)
         * • Period: "01 Jan 2026 - 09 Jan 2026" at Y = 708.00 (11pt Regular Black)
         * ========================================================= */
        cs.beginText();
        cs.appendRawCommands("36 762 Td\n");
        cs.appendRawCommands("0 -37.5 Td\n");
        cs.setFont(fonts.semiBold, 25);
        cs.setNonStrokingColor(0f, 0f, 0f); // Black
        cs.showText("Account Statement");
        cs.setNonStrokingColor(0f);
        cs.appendRawCommands("0 0 Td\n");
        cs.appendRawCommands("0 -16.5 Td\n");
        String startDate = TemplateUtils.formatDateDMMMYYYY(stmt.meta.statementPeriodStart);
        String endDate = TemplateUtils.formatDateDMMMYYYY(stmt.meta.statementPeriodEnd);
        cs.setFont(fonts.regular, 11);
        cs.setNonStrokingColor(0f, 0f, 0f); // Black
        cs.showText(startDate + " - " + endDate);
        cs.setNonStrokingColor(0f);
        cs.appendRawCommands("0 0 Td\n");
        cs.appendRawCommands("0 -185.5 Td\n");
        cs.endText();
        renderHeaderInfo(cs, fonts);
    }

    private void renderHeaderInfo(PDPageContentStream cs, Fonts fonts) throws Exception {
        String customerName = (stmt.details.fullName != null && !stmt.details.fullName.isBlank())
                ? stmt.details.fullName
                : (stmt.details.name != null ? stmt.details.name : "");

        /* =========================================================
         * LEFT COLUMN: CUSTOMER INFORMATION
         * • Customer Name: X = 38, Y = 660 (14pt SemiBold Black)
         * • CRN Number:    X = 38, Y = 643.5 (11pt Regular Gray 0.35f)
         * • Address Lines: X = 38, starts at Y = 610.50 (9pt Regular Black, lineH = 14pt)
         * • MICR / IFSC:   X = 38, Y = 524.50 (9pt Gray labels + 9pt Black values)
         * ========================================================= */
        // Customer Name
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 38 660 Tm\n");
        cs.setFont(fonts.semiBold, 14);
        cs.setNonStrokingColor(0f, 0f, 0f); // Black
        cs.showText(customerName);
        cs.endText();

        // CRN Number
        String crnRaw = (stmt.details.crn != null && !stmt.details.crn.isBlank())
                ? stmt.details.crn
                : (stmt.details.customerRelNo != null ? stmt.details.customerRelNo : "");
        String crnDisplay = crnRaw;
        if (!crnRaw.isBlank() && !crnRaw.startsWith("xxxxxx") && crnRaw.length() >= 6) {
            crnDisplay = "xxxxxx" + crnRaw.substring(crnRaw.length() - 3);
        }
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 38 643.5 Tm\n");
        cs.setFont(fonts.regular, 11);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f); // Medium Gray
        cs.showText("CRN " + crnDisplay);
        cs.endText();

        // Wrapped Address Lines (Starts at Y=610.50, max width 270)
        float maxAddrWidth = 270f;
        List<String> wrappedAddrLines = new ArrayList<>();
        if (stmt.details.address != null && !stmt.details.address.isBlank()) {
            String rawAddr = stmt.details.address.trim();
            String[] parts = rawAddr.split("\\r?\\n");
            if (parts.length == 1 && rawAddr.contains(",")) {
                parts = rawAddr.split("\\s*,\\s*");
            }
            for (String part : parts) {
                part = part.trim();
                if (!part.isEmpty()) {
                    wrappedAddrLines.addAll(wrapTextByWidth(part, fonts.regular, 9f, maxAddrWidth));
                }
            }
        }
        float addrY = 610.50f;
        float addrLineH = 14.0f;
        for (String line : wrappedAddrLines) {
            if (addrY < 538f) break; // keep above MICR line
            cs.beginText();
            cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 38 %.2f Tm\n", addrY));
            cs.setFont(fonts.regular, 9);
            cs.setNonStrokingColor(0f, 0f, 0f);
            cs.showText(line);
            cs.endText();
            addrY -= addrLineH;
        }

        // MICR & IFSC Code
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 38 524.5 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f);
        cs.showText("MICR ");
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(stmt.details.micr != null ? stmt.details.micr : "");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f);
        cs.showText("   IFSC Code ");
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(stmt.details.ifsc != null ? stmt.details.ifsc : "");
        cs.endText();

        /* =========================================================
         * RIGHT COLUMN: ACCOUNT METADATA (X = 347.5)
         * • Account No.:        Y = 665 (Label Gray + Value SemiBold Black)
         * • Account Type:       Y = 649 (Label Gray + Value SemiBold Black)
         * • Branch:             Y = 633 (Label Gray + Value SemiBold Black)
         * • Account Status:     Y = 595 (Label Gray + Value SemiBold Black)
         * • Nominee Registered: Y = 575 (Label Gray + Value SemiBold Black)
         * • Currency:           Y = 545 (Label Gray + Value SemiBold Black)
         * • TO SHIFT ALL RIGHT ITEMS UP/DOWN: Adjust individual Y values.
         * ========================================================= */
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 347.5 665 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f);
        cs.showText("Account No. ");
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(stmt.details.accountNumber != null ? stmt.details.accountNumber : "");
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 347.5 649 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f);
        cs.showText("Account Type ");
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(stmt.details.accountType != null && !stmt.details.accountType.isBlank() ? stmt.details.accountType : "Savings");
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 347.5 633 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f);
        cs.showText("Branch ");
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(stmt.details.branch != null ? stmt.details.branch : "");
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 347.5 595 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f);
        cs.showText("Account Status ");
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("Active");
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 347.5 575 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f);
        cs.showText("Nominee Registered ");
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        String nomineeVal = (stmt.details.nomineeName != null && !stmt.details.nomineeName.isBlank() && !stmt.details.nomineeName.equalsIgnoreCase("No")) ? "Yes" : "No";
        cs.showText(nomineeVal);
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 347.5 545 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f);
        cs.showText("Currency ");
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("INDIAN RUPEE");
        cs.endText();
    }

    // ----------------- TRANSACTION TABLE HEADER -----------------
    /* =========================================================
     * TRANSACTION TABLE HEADER (Page 1 Top Y = 487.5f)
     * • Red Bar: X = 36 to 559, Height = 25 (Kotak Red)
     * • Gray Column Header: Height = 25 (Header Gray)
     * • 7 Columns: #, Date, Description, Chq/Ref. No., Withdrawal, Deposit, Balance
     * ========================================================= */
    private void renderTransactionTableHeader(PDPageContentStream cs, Fonts fonts, float topY) throws Exception {
        float tableLeft = 36f;
        float tableRight = 559f;
        float redBarHeight = 25f;
        float headerRowHeight = 25f;
        float headerRowY = topY - headerRowHeight;
        float[] colWidths = { 31.18f, 49f, 155.47f, 80.19f, 69.05f, 69.05f, 69.05f };
        float[] colTextX = { 44f, 75.18f, 124.19f, 279.66f, 354.85f, 428.9f, 497.95f };

        // Red title bar
        cs.setNonStrokingColor(0.92941f, 0.1098f, 0.14118f); // Kotak Red
        cs.addRect(tableLeft, topY, tableRight - tableLeft, redBarHeight);
        cs.fill();
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 222.79 " + (topY + 7.5f) + " Tm\n");
        cs.setFont(fonts.regular, 12);
        cs.setNonStrokingColor(1f, 1f, 1f); // White
        cs.showText("Savings Account Transactions");
        cs.setNonStrokingColor(0f);
        cs.endText();

        // Grey column header background
        cs.setNonStrokingColor(0.62353f, 0.63137f, 0.64314f);
        float x = tableLeft;
        for (float w : colWidths) {
            cs.addRect(x, headerRowY, w, headerRowHeight);
            cs.fill();
            x += w;
        }

        // Vertical white separators
        cs.setLineWidth(0.5f);
        cs.setLineCapStyle(2);
        cs.setStrokingColor(1f, 1f, 1f);
        x = tableLeft;
        for (float w : colWidths) {
            x += w;
            cs.moveTo(x, headerRowY);
            cs.lineTo(x, headerRowY + headerRowHeight);
            cs.stroke();
        }

        String[] COL_HEADERS = { "#", "Date", "Description", "Chq/Ref. No.", "Withdrawal (Dr.)", "Deposit (Cr.)", "Balance" };
        for (int i = 0; i < colTextX.length; i++) {
            cs.beginText();
            cs.appendRawCommands("1 0 0 1 " + colTextX[i] + " " + (topY - 17f) + " Tm\n");
            cs.setFont(fonts.regular, 9);
            cs.setNonStrokingColor(1f, 1f, 1f);
            cs.showText(COL_HEADERS[i]);
            cs.setNonStrokingColor(0);
            cs.endText();
        }
    }

    private void renderOpeningBalance(PDPageContentStream cs, Fonts fonts) throws Exception {
        Object[] r = TemplateUtils.formatBalanceAndRightAlignX(stmt.details.startingBalance, fonts.regular, 7.68f, RIGHT_BAL);
        String fAmt = (String) r[0];
        float x1 = (float) r[1];
        String[] values = { "-", "-", "Opening Balance", "-", "-", "-", fAmt };
        float[] x = { 39f, 70.18f, 119.19f, 274.66f, 415.51f, 484.56f, x1 };
        final float y = 451.82f;
        for (int i = 0; i < x.length; i++) {
            cs.beginText();
            cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", x[i], y));
            cs.setFont(fonts.regular, 7.68f);
            cs.setNonStrokingColor(0f, 0f, 0f);
            cs.showText(values[i]);
            cs.endText();
        }
        cs.setLineWidth(0.5f);
        cs.setLineCapStyle(2);
        cs.setStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.moveTo(36f, 448.82f);
        cs.lineTo(559f, 448.82f);
        cs.stroke();
    }

    // ----------------- TRANSACTIONS -----------------
    private float renderTransaction(int idx, Transaction txn, PageCursor pc, Fonts fonts) throws Exception {
        float y = pc.y();
        String[] tvals = { String.valueOf(idx), TemplateUtils.formatDateDMMMYYYY(txn.date), txn.description };
        float yBefore = y;
        int descLines = countWrappedLines(fonts.regular, 7.68f, txn.description, DESC_WIDTH);
        float available = y - 36f;
        int linesThatFit = (int) Math.floor(available / LINE_H);
        linesThatFit = Math.max(1, Math.min(linesThatFit, descLines));
        renderTransactionInfo(txn, yBefore, pc, fonts);
        for (int i = 0; i < tvals.length; i++) {
            float x = TXN_X[i];
            if (i != 2) {
                PDPageContentStream cs = pc.cs();
                cs.beginText();
                cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", x, yBefore));
                cs.setFont(fonts.regular, 7.68f);
                cs.showText(tvals[i]);
                cs.setNonStrokingColor(0f, 0f, 0f);
                cs.endText();
            } else {
                y = renderWrappedDescription(pc, fonts.regular, 7.68f, txn.description, DESC_X, y, linesThatFit);
            }
        }
        PDPageContentStream cs = pc.cs();
        cs.setLineWidth(0.5f);
        cs.setLineCapStyle(2);
        cs.setStrokingColor(0.62353f, 0.63137f, 0.64314f);
        y -= 3f;
        cs.moveTo(36f, y);
        cs.lineTo(559f, y);
        cs.stroke();
        return y;
    }

    private void renderTransactionInfo(Transaction txn, float yBefore, PageCursor pc, Fonts fonts) throws Exception {
        PDPageContentStream cs = pc.cs();

        cs.beginText();
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", TXN_X[3], yBefore));
        cs.setFont(fonts.regular, 7.68f);
        cs.showText(txn.reference);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.endText();

        boolean isDebit = txn.credit == 0;
        double amt = (isDebit ? txn.debit : txn.credit);
        // Clamp amount for display
        Object[] r = TemplateUtils.formatBalanceAndRightAlignX(
                Math.max(0, amt),
                fonts.regular, 7.68f,
                (isDebit ? RIGHT_DR : RIGHT_CR)
        );
        String fAmt = (String) r[0];
        float x1 = (float) r[1];
        cs.beginText();
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", x1, yBefore));
        cs.setFont(fonts.regular, 7.68f);
        cs.showText(fAmt);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.endText();

        amt = txn.balance;
        r = TemplateUtils.formatBalanceAndRightAlignX(
                Math.max(0, amt),
                fonts.regular, 7.68f, RIGHT_BAL
        );
        fAmt = (String) r[0];
        x1 = (float) r[1];
        cs.beginText();
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", x1, yBefore));
        cs.setFont(fonts.regular, 7.68f);
        cs.showText(fAmt);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.endText();
    }

    public static List<String> wrapDescriptionLines(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;
        List<String> tokens = new ArrayList<>();
        for (String word : text.trim().split("\\s+")) {
            if (word.isEmpty()) continue;
            if (word.contains("/")) {
                String[] parts = word.split("(?<=/)");
                for (String p : parts) {
                    if (!p.isEmpty()) tokens.add(p);
                }
            } else {
                tokens.add(word);
            }
        }

        StringBuilder line = new StringBuilder();
        for (String token : tokens) {
            String test = line.length() == 0 ? token : (line.toString().endsWith("/") ? line + token : line + " " + token);
            float width = (font.getStringWidth(test) / 1000f) * fontSize;
            if (width <= maxWidth) {
                line.setLength(0);
                line.append(test);
            } else {
                if (line.length() > 0) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                float tokWidth = (font.getStringWidth(token) / 1000f) * fontSize;
                if (tokWidth <= maxWidth) {
                    line.append(token);
                } else {
                    String rem = token;
                    while ((font.getStringWidth(rem) / 1000f) * fontSize > maxWidth) {
                        int cut = rem.length();
                        while (cut > 1 && (font.getStringWidth(rem.substring(0, cut)) / 1000f) * fontSize > maxWidth) {
                            cut--;
                        }
                        lines.add(rem.substring(0, cut));
                        rem = rem.substring(cut);
                    }
                    line.append(rem);
                }
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    private float renderWrappedDescription(PageCursor pc, PDFont font, float fontSize, String text, float startX, float startY, int maxLinesOnPage) throws Exception {
        PDPageContentStream cs = pc.cs();
        List<String> lines = wrapDescriptionLines(text, font, fontSize, DESC_WIDTH);
        float y = startY;

        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                y -= LINE_H;
            }
            cs.beginText();
            cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", startX, y));
            cs.setFont(font, fontSize);
            cs.setNonStrokingColor(0f, 0f, 0f);
            cs.showText(lines.get(i));
            cs.setNonStrokingColor(0f);
            cs.endText();
        }
        return y;
    }

    public static List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 <= maxWidth) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) lines.add(currentLine.toString());
        return lines;
    }

    // ---------- NEW helper: wrap by actual rendered pixel width ----------
    private List<String> wrapTextByWidth(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String[] words = text.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            float width = (font.getStringWidth(test) / 1000f) * fontSize;
            if (width > maxWidth && line.length() > 0) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(test);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private int countWrappedLines(PDFont font, float fontSize, String text, float maxWidth) throws Exception {
        return Math.max(1, wrapDescriptionLines(text, font, fontSize, maxWidth).size());
    }

    private final void drawAllTransactions(List<Transaction> txns, PageCursor pc, Fonts fonts) throws Exception {
        int i = 1;
        for (Transaction txn : txns) {
            int descLines = countWrappedLines(fonts.regular, 7.68f, txn.description, DESC_WIDTH);
            float neededHeight = Math.max(1, descLines) * LINE_H + 6f;
            pc.ensureSpace(neededHeight);
            pc.y(renderTransaction(i++, txn, pc, fonts));
            pc.moveDown(TXNROW_H);
        }
    }

    // ----------------- SUMMARY PAGE (with three-column footer) -----------------
    // =========================================================================
    // PAGE 3: SUMMARY PAGE (Account Summary, End of Statement, Ad, Contact, Remember Box)
    // -------------------------------------------------------------------------
    // PDF COORDINATE SYSTEM GUIDE:
    // • (0, 0) is at the BOTTOM-LEFT of the page. Total Height = 842, Width = 595.
    // • Y-Axis: Increasing Y moves UP (towards top). Decreasing Y moves DOWN (towards bottom).
    // • X-Axis: Increasing X moves RIGHT. Decreasing X moves LEFT.
    // • Colors: setNonStrokingColor(R, G, B) from 0.0f (0) to 1.0f (255).
    //   - Kotak Red: (0.92941f, 0.1098f, 0.14118f) [#ED1C24]
    //   - Dark Header Gray: (0.62353f, 0.63137f, 0.64314f) [#9FA1A4]
    //   - Medium Label Gray: (0.35f, 0.35f, 0.35f) [#595959]
    //   - Light Box Gray: (0.91765f, 0.92157f, 0.92549f) [#EAEBED]
    //   - Pure Black: (0f, 0f, 0f)
    // =========================================================================
    private final void drawSummaryPage(PageCursor pc, Fonts fonts) throws IOException {
        PDPageContentStream cs = pc.cs();

        /* =========================================================
         * 1. RED SECTION HEADER BAR ("Account Summary")
         * • X = 36 (Left margin)
         * • Y = 737 (Bottom of red bar; increase to move UP, decrease to move DOWN)
         * • Width = 523 (Spans from X=36 to X=559)
         * • Height = 25 (Top of red bar is at 737 + 25 = 762)
         * • Color: Kotak Red (0.92941f, 0.1098f, 0.14118f)
         * ========================================================= */
        cs.saveGraphicsState();
        cs.setNonStrokingColor(0.92941f, 0.1098f, 0.14118f); // Kotak Red
        cs.addRect(36, 737, 523, 25);
        cs.fill();
        cs.restoreGraphicsState();

        /* Title text inside Red Bar: "Account Summary" (White, 12pt) */
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 251.42 744.5 Tm\n"); // X=251.42 (centered), Y=744.5 (vertical center in bar)
        cs.setFont(fonts.regular, 12);
        cs.setNonStrokingColor(1f, 1f, 1f); // White text
        cs.showText("Account Summary");
        cs.endText();

        /* =========================================================
         * 2. COLUMN HEADER BACKGROUNDS (Gray banner below red bar)
         * • Y = 704 (Height = 33, top connects to 737)
         * • Column 1 (Particulars): X=36, Width=237.73
         * • Column 2 (Opening Balance): X=273.73, Width=142.64
         * • Column 3 (Closing Balance): X=416.36, Width=142.64
         * • Color: Dark Header Gray (0.62353f, 0.63137f, 0.64314f)
         * ========================================================= */
        cs.saveGraphicsState();
        cs.setNonStrokingColor(0.62353f, 0.63137f, 0.64314f); // Header Gray
        cs.addRect(36, 704, 237.73f, 33);
        cs.addRect(273.73f, 704, 142.64f, 33);
        cs.addRect(416.36f, 704, 142.64f, 33);
        cs.fill();
        cs.restoreGraphicsState();

        /* White vertical separator lines between column headers */
        cs.setLineWidth(0.5f);
        cs.setStrokingColor(1f, 1f, 1f); // White divider lines
        cs.moveTo(273.73f, 704);
        cs.lineTo(273.73f, 737);
        cs.stroke();
        cs.moveTo(416.36f, 704);
        cs.lineTo(416.36f, 737);
        cs.stroke();
        cs.moveTo(559, 704);
        cs.lineTo(559, 737);
        cs.stroke();

        /* Header Labels inside Gray Bar (White text, 9pt, Y=716) */
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 48 716 Tm\n"); // Particulars label
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.showText("   Particulars");
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 285.73 716 Tm\n"); // Opening Balance label
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.showText("Opening Balance");
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 428.36 716 Tm\n"); // Closing Balance label
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.showText("Closing Balance");
        cs.endText();

        /* =========================================================
         * 3. SUMMARY DATA ROW ("Savings Account (SA):", Balances)
         * • Y = 693.32 (Text baseline; increase to move UP, decrease to move DOWN)
         * • Bottom border line: Y = 690.32
         * • Text Color: Pure Black (0f, 0f, 0f), 7.68pt
         * ========================================================= */
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 39 693.32 Tm\n");
        cs.setFont(fonts.regular, 7.68f);
        cs.setNonStrokingColor(0f, 0f, 0f); // Pure black
        cs.showText("   Savings Account (SA):");
        cs.endText();

        // Right alignment bounds for Opening & Closing balances
        float OPENING_RIGHT = 359.64f + 26.7264f; // Right edge boundary for opening balance
        float CLOSING_RIGHT = 502.27f + 26.7264f; // Right edge boundary for closing balance

        Object[] r = TemplateUtils.formatBalanceAndRightAlignX(
                stmt.details.startingBalance,
                fonts.regular, 7.68f, OPENING_RIGHT
        );
        String fAmt = (String) r[0];
        float x1 = (float) r[1];
        cs.beginText();
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f 693.32 Tm\n", x1));
        cs.setFont(fonts.regular, 7.68f);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(fAmt);
        cs.endText();

        r = TemplateUtils.formatBalanceAndRightAlignX(
                Math.max(0, CLOSING_BAL),
                fonts.regular, 7.68f, CLOSING_RIGHT
        );
        fAmt = (String) r[0];
        x1 = (float) r[1];
        cs.beginText();
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f 693.32 Tm\n", x1));
        cs.setFont(fonts.regular, 7.68f);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(fAmt);
        cs.endText();

        // Bottom gray dividing line below the summary row
        cs.setLineWidth(0.5f);
        cs.setLineCapStyle(2);
        cs.setStrokingColor(0.62353f, 0.63137f, 0.64314f);
        float y = 690.32f;
        cs.moveTo(36f, y);
        cs.lineTo(559f, y);
        cs.stroke();

        /* =========================================================
         * 4. "END OF STATEMENT" TEXT BLOCK (4 Centered Lines)
         * • centerPage = 297.5f (Page horizontal midpoint)
         * • Line 1 ("End of Statement"): Y = 616.32 (12pt font)
         * • Line 2 ("Any discrepancy..."): Y = 600.32 (9.12pt font)
         * • Line 3 ("one month..."): Y = 584.32 (9.12pt font)
         * • Line 4 ("This is a system generated..."): Y = 568.32 (9.12pt font)
         * • TO ADJUST GAP ABOVE: Change 616.32 to higher value (moves UP) or lower value (moves DOWN).
         *   Keep 16pt step difference between lines (e.g. 616.32 -> 600.32 -> 584.32 -> 568.32).
         * ========================================================= */
        float centerPage = 297.5f;

        String eos1 = "End of Statement";
        float eos1X = centerAlign(eos1, centerPage, fonts.regular, 12);
        cs.beginText();
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f 616.32 Tm\n", eos1X));
        cs.setFont(fonts.regular, 12);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(eos1);
        cs.endText();

        String eos2 = "Any discrepancy in the statement should be brought to the notice of Kotak Mahindra Bank Ltd. within";
        float eos2X = centerAlign(eos2, centerPage, fonts.regular, 9.12f);
        cs.beginText();
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f 600.32 Tm\n", eos2X));
        cs.setFont(fonts.regular, 9.12f);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(eos2);
        cs.endText();

        String eos3 = "one month from the date of receipt of the statement.";
        float eos3X = centerAlign(eos3, centerPage, fonts.regular, 9.12f);
        cs.beginText();
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f 584.32 Tm\n", eos3X));
        cs.setFont(fonts.regular, 9.12f);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(eos3);
        cs.endText();

        String eos4 = "This is a system generated report and does not require signature & stamp.";
        float eos4X = centerAlign(eos4, centerPage, fonts.regular, 9.12f);
        cs.beginText();
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f 568.32 Tm\n", eos4X));
        cs.setFont(fonts.regular, 9.12f);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(eos4);
        cs.endText();

        /* =========================================================
         * 5. AD BANNER IMAGE ("Hausla hai toh ho jayega")
         * • Transformation matrix: [Width 0 0 Height X Y cm /img1 Do]
         * • Width = 508.67, Height = 84.00
         * • X = 43.17 (Left offset)
         * • Y = 364.32 (Bottom of image; Top of image is at 364.32 + 84 = 448.32)
         * • TO MOVE AD UP: Increase 364.32 (e.g. 380.00).
         * • TO MOVE AD DOWN: Decrease 364.32 (e.g. 350.00).
         * ========================================================= */
        cs.appendRawCommands("q 508.67 0 0 84 43.17 457 cm /img1 Do Q\n");

        /* =========================================================
         * 6. ASSISTANCE SECTION (Red Bar + 3-Column Contact Details)
         * ---------------------------------------------------------
         * 💡 TO MOVE THIS ENTIRE SECTION UP OR DOWN:
         * Just change the single variable below (assistBaseY):
         *   • Increase assistBaseY (e.g. 250f, 260f) -> Moves ENTIRE section UP
         *   • Decrease assistBaseY (e.g. 220f, 210f) -> Moves ENTIRE section DOWN
         * All elements (Red Bar, Title, Icons, Vertical Dividers,
         * Contact Us, Address, Phone Number) will move together automatically!
         * ========================================================= */
        float assistBaseY = 245f; // Default: 238f (Bottom Y of red bar)

        // Red Header Bar ("For assistance, reach out to us at:")
        cs.saveGraphicsState();
        cs.setNonStrokingColor(0.92941f, 0.1098f, 0.14118f); // Kotak Red
        cs.addRect(36f, assistBaseY, 523f, 25f);
        cs.fill();
        cs.restoreGraphicsState();

        cs.beginText();
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 214.19 %.2f Tm\n", assistBaseY + 7.5f));
        cs.setFont(fonts.regular, 12);
        cs.setNonStrokingColor(1f, 1f, 1f); // White
        cs.showText("For assistance, reach out to us at:");
        cs.endText();

        // 3-Column Layout Geometry (Calculated from assistBaseY)
        float midLeftX     = 184f; // divider 1
        float midRightX    = 416f; // divider 2

        float col1Center   = 110f;          // Contact Us center (36 + 184)/2
        float col2Center   = 300f;          // Branch Address center (184 + 416)/2
        float col3Center   = 487.5f;        // Branch Phone center (416 + 559)/2

        // Vertical dividers (between columns)
        float divTop = assistBaseY - 15f;    // 223f when assistBaseY=238f
        float divBottom = assistBaseY - 76f; // 162f when assistBaseY=238f
        cs.setLineWidth(1f);
        cs.setStrokingColor(0.62353f, 0.63137f, 0.64314f); // Gray divider lines
        cs.moveTo(midLeftX, divTop);
        cs.lineTo(midLeftX, divBottom);
        cs.stroke();
        cs.moveTo(midRightX, divTop);
        cs.lineTo(midRightX, divBottom);
        cs.stroke();

        float iconSize = 21f;                // 21x21 pt icon size
        float iconY = assistBaseY - 33f;     // 205f when assistBaseY=238f

        // ---- Column 1: Contact Us (Phone icon /img3) ----
        cs.appendRawCommands(String.format(Locale.US,
                "q %.2f 0 0 %.2f %.2f %.2f cm /img3 Do Q\n", iconSize, iconSize,
                col1Center - iconSize / 2f, iconY));

        cs.beginText();
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        String t1 = "Contact Us";
        float t1X = centerAlign(t1, col1Center, fonts.semiBold, 9);
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", t1X, assistBaseY - 45f));
        cs.showText(t1);
        cs.endText();

        cs.beginText();
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        String num1 = "1800 4100";
        float num1X = centerAlign(num1, col1Center, fonts.regular, 9);
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", num1X, assistBaseY - 57f));
        cs.showText(num1);
        cs.endText();

        cs.beginText();
        cs.setFont(fonts.regular, 7);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f); // Medium Gray note
        cs.setNonStrokingColor(0f, 0f, 0f); // Pure Black
        String note = "(Toll-free number)";
        float noteX = centerAlign(note, col1Center, fonts.regular, 7);
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", noteX, assistBaseY - 69f));
        cs.showText(note);
        cs.endText();

        // ---- Column 2: Branch Address (Bank icon /img5) ----
        cs.appendRawCommands(String.format(Locale.US,
                "q %.2f 0 0 %.2f %.2f %.2f cm /img5 Do Q\n", iconSize, iconSize,
                col2Center - iconSize / 2f, iconY));

        cs.beginText();
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        String t2 = "Branch Address";
        float t2X = centerAlign(t2, col2Center, fonts.semiBold, 9);
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", t2X, assistBaseY - 45f));
        cs.showText(t2);
        cs.endText();

        float addrMaxWidth = 216f; // Max wrapping width for branch address
        String rawAddr = (stmt.details.branchAddress != null && !stmt.details.branchAddress.isBlank())
                ? stmt.details.branchAddress.replaceAll("\\s*\\R\\s*", " ").trim()
                : "Kotak Mahindra Bank Ltd, Ground Floor, Mezzanine Floor, Hotel Blue Star, 62 Hamedia Road, Near Sangam Cinema, Bhopal, Bhopal-462001, Madhya Pradesh, India";
        float brFontSize = 7.5f;
        float lineHeight = 8.5f; // Line spacing between wrapped address lines
        List<String> addrLines = wrapTextByWidth(rawAddr, fonts.regular, brFontSize, addrMaxWidth);
        float addrY = assistBaseY - 57f; // Starting Y for address line 1 (181f when assistBaseY=238f)
        for (int i = 0; i < addrLines.size(); i++) {
            String line = addrLines.get(i);
            float lineX = centerAlign(line, col2Center, fonts.regular, brFontSize);
            cs.beginText();
            cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", lineX, addrY - i * lineHeight));
            cs.setFont(fonts.regular, brFontSize);
            cs.setNonStrokingColor(0f, 0f, 0f);
            cs.showText(line);
            cs.endText();
        }

        // ---- Column 3: Branch Phone Number (Phone icon /img3) ----
        cs.appendRawCommands(String.format(Locale.US,
                "q %.2f 0 0 %.2f %.2f %.2f cm /img3 Do Q\n", iconSize, iconSize,
                col3Center - iconSize / 2f, iconY));

        cs.beginText();
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        String t3 = "Branch Phone Number";
        float t3X = centerAlign(t3, col3Center, fonts.semiBold, 9);
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", t3X, assistBaseY - 45f));
        cs.showText(t3);
        cs.endText();

        cs.beginText();
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        String phone = (stmt.details.branchPhoneNo != null && !stmt.details.branchPhoneNo.isBlank() && !stmt.details.branchPhoneNo.equals(stmt.details.accountNumber))
                ? stmt.details.branchPhoneNo
                : "9522262613";
        float phoneX = centerAlign(phone, col3Center, fonts.regular, 9);
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", phoneX, assistBaseY - 57f));
        cs.showText(phone);
        cs.endText();

        /* =========================================================
         * 8. "REMEMBER!" BOX (Light Gray Rounded Container + QR Code)
         * • Y Span: Y = 86f (Bottom) to Y = 156f (Top) -> Total Height = 70f
         * • X Span: X = 36f to X = 559f
         * • Background Color: Light Gray (0.91765f, 0.92157f, 0.92549f) [#EAEBED]
         * • Divider line: X = 384.67f from Y=95f to Y=146f
         * • "Remember!" Title: Y = 133 (12pt SemiBold Kotak Red)
         * • Text lines: Y = 115, Y = 97 (12pt Regular Black)
         * • QR Code Image (/img6): X = 399.84, Y = 98.4, Size = 45.6x45.6
         * • QR Labels ("Scan for...", "more safe...", "banking tips"): Y = 129, 119, 109 (10pt Regular Black)
         * ========================================================= */
        cs.saveGraphicsState();
        cs.setNonStrokingColor(0.91765f, 0.92157f, 0.92549f); // Light Box Background
        cs.moveTo(46f, 86f);
        cs.lineTo(549f, 86f);
        cs.curveTo(554.52f, 86f, 559f, 90.48f, 559f, 96f);
        cs.lineTo(559f, 146f);
        cs.curveTo(559f, 151.52f, 554.52f, 156f, 549f, 156f);
        cs.lineTo(46f, 156f);
        cs.curveTo(40.48f, 156f, 36f, 151.52f, 36f, 146f);
        cs.lineTo(36f, 96f);
        cs.curveTo(36f, 90.48f, 40.48f, 86f, 46f, 86f);
        cs.fill();
        cs.restoreGraphicsState();

        // Vertical divider line inside Remember box (separating text from QR code)
        cs.saveGraphicsState();
        cs.setLineCapStyle(2);
        cs.setLineWidth(1f);
        cs.setStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.moveTo(384.67f, 95f);
        cs.lineTo(384.67f, 146f);
        cs.stroke();
        cs.restoreGraphicsState();

        // Remember! Title
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 51 133 Tm\n");
        cs.setFont(fonts.semiBold, 12);
        cs.setNonStrokingColor(0.92941f, 0.1098f, 0.14118f); // Kotak Red
        cs.showText("   Remember!");
        cs.endText();

        // Line 1: "Never share personal/sensitive..."
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 51 115 Tm\n");
        cs.setFont(fonts.regular, 12);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("   Never share personal/sensitive information like PIN, CVV, OTP");
        cs.endText();

        // Line 2: "or passwords with anyone."
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 51 97 Tm\n");
        cs.setFont(fonts.regular, 12);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("   or passwords with anyone.");
        cs.endText();

        // QR Code Image (/img6)
        cs.appendRawCommands("q 45.6 0 0 45.6 399.84 98.4 cm /img6 Do Q\n");

        // QR Code Side Text ("Scan for more safe banking tips")
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 458.61 129 Tm\n");
        cs.setFont(fonts.regular, 10);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("Scan for");
        cs.appendRawCommands("1 0 0 1 458.61 119 Tm\n");
        cs.showText("more safe");
        cs.appendRawCommands("1 0 0 1 458.61 109 Tm\n");
        cs.showText("banking tips");
        cs.endText();

        /* =========================================================
         * 9. LEGAL FOOTER (CIN line & Registered Office line)
         * • Divider line: Y = 76f (X from 36 to 559)
         * • CIN Line: Y = 61f (9.12pt Regular Medium Gray)
         * • Registered Office Line: Y = 43f (9.12pt Regular Medium Gray)
         * ========================================================= */
        cs.setStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.setLineWidth(1f);
        cs.moveTo(36f, 76f);
        cs.lineTo(559f, 76f);
        cs.stroke();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 168.66 61 Tm\n");
        cs.setFont(fonts.regular, 9.12f);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f); // Medium Gray
        cs.setNonStrokingColor(0f, 0f, 0f); // Pure Black
        cs.showText("Kotak Mahindra Bank Ltd. | CIN: L65110MH1985PLC038137");
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 63.75 43 Tm\n");
        cs.setFont(fonts.regular, 9.12f);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f); // Medium Gray
        cs.setNonStrokingColor(0f, 0f, 0f); // Pure Black
        cs.showText("Registered Office: 27 BKC, C 27, G Block, Bandra Kurla Complex, Bandra (E), Mumbai - 400 051. www.kotak.bank.in");
        cs.endText();
    }

    private float centerAlign(String text, float center, PDFont font, float fontSize) throws IOException {
        float textWidth = (font.getStringWidth(text) / 1000f) * fontSize;
        return center - (textWidth / 2f);
    }

    // =========================================================================
    // PAGE 4: LAST PAGE (Important Information Notices & Commonly Used Narrations)
    // -------------------------------------------------------------------------
    private final void drawLastPage(PageCursor pc, Fonts fonts) throws IOException {
        PDPageContentStream cs = pc.cs();

        /* =========================================================
         * 1. RED HEADER BAR ("Important Information")
         * • X = 36, Y = 737, Width = 523, Height = 25 (Top is at 762)
         * • Color: Kotak Red (0.92941f, 0.1098f, 0.14118f) [#ED1C24]
         * • Title Text: X = 240.74 (centered at X=297.5), Y = 744.5 (12pt White)
         * ========================================================= */
        cs.saveGraphicsState();
        cs.setNonStrokingColor(0.92941f, 0.1098f, 0.14118f); // Kotak Red
        cs.addRect(36f, 737f, 523f, 25f);
        cs.fill();
        cs.restoreGraphicsState();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 240.74 744.5 Tm\n");
        cs.setFont(fonts.regular, 12);
        cs.setNonStrokingColor(1f, 1f, 1f); // White text
        cs.showText("Important Information");
        cs.endText();

        /* =========================================================
         * 2. STANDALONE ROUNDED BORDER CONTAINER BOX (Encasing Notices)
         * • Clean gap of ~27pt (~36px) below Red Banner 1
         * • X Span: X = 36f to X = 559f (Width = 523f)
         * • Y Span: Y = 310f (Bottom of box) to Y = 710f (Top of box) -> Height = 400f
         * • 4 Rounded Corners (radius = 11pt)
         * • Stroke Color: Gray border (0.62353f, 0.63137f, 0.64314f), LineWidth = 0.5f
         * • Fill Color: Pure White background (1f, 1f, 1f)
         * ========================================================= */
        cs.saveGraphicsState();
        cs.setStrokingColor(0.62353f, 0.63137f, 0.64314f); // Gray Border
        cs.setNonStrokingColor(1f, 1f, 1f); // White Fill
        cs.setLineWidth(0.5f);
        cs.moveTo(47f, 310f);
        cs.lineTo(548f, 310f);
        cs.curveTo(554.08f, 310f, 559f, 314.92f, 559f, 321f);
        cs.lineTo(559f, 699f);
        cs.curveTo(559f, 705.08f, 554.08f, 710f, 548f, 710f);
        cs.lineTo(47f, 710f);
        cs.curveTo(40.92f, 710f, 36f, 705.08f, 36f, 699f);
        cs.lineTo(36f, 321f);
        cs.curveTo(36f, 314.92f, 40.92f, 310f, 47f, 310f);
        cs.fillAndStroke();
        cs.restoreGraphicsState();

        /* =========================================================
         * 3. 12 BULLET NOTICES (Evenly distributed to cover all space inside container box)
         * • textX = 66.0f (Left start of text)
         * • bulletIconX = 49.0f (Bullet round dot X position)
         * • maxTextW = 482f (Line wrapping boundary width utilizing full box width)
         * • fontSize = 7.15f (Clear font size)
         * • lineH = 8.0f (Comfortable line height)
         * • regularGap = 9.0f, largerGap = 13.0f (before bullets 11 & 12)
         * ========================================================= */
        float curY = 696f;
        float textX = 66.0f;        // Left offset for text
        float bulletIconX = 49.0f;  // Left offset for bullet dot icon
        float maxTextW = 482f;      // Text wrap width across full box
        float fontSize = 7.15f;     // Notice font size
        float lineH = 8.0f;         // Line height
        float regularGap = 9.0f;    // Gap between standard bullet points
        float largerGap = 13.0f;    // Gap before disclaimers (bullets 11 & 12)

        for (int i = 0; i < notices.length; i++) {
            // Draw bullet dot icon (4x4 points)
            cs.appendRawCommands(String.format(Locale.US, "q 4 0 0 4 %.2f %.2f cm /img8 Do Q\n", bulletIconX, curY - 1.32f));

            List<String> wrappedLines = wrapTextByWidth(notices[i], fonts.regular, fontSize, maxTextW);
            for (String line : wrappedLines) {
                cs.beginText();
                cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", textX, curY));
                cs.setFont(fonts.regular, fontSize);
                cs.setNonStrokingColor(0f, 0f, 0f); // Pure black
                cs.showText(line);
                cs.endText();
                curY -= lineH; // Move down for next line
            }
            // Gap after bullet item
            float gap = (i >= 9) ? largerGap : regularGap;
            curY -= gap;
        }

        /* =========================================================
         * 4. "COMMONLY USED NARRATIONS" RED HEADER BAR
         * • Clean gap of ~29pt (~39px) below Rounded Box (310f -> 281f)
         * • X = 36f, Y = 256f (Bottom of red bar), Width = 523f, Height = 25f (Top is at 281f)
         * • Color: Kotak Red (0.92941f, 0.1098f, 0.14118f) [#ED1C24]
         * • Text: X = 228.27 (centered), Y = 263.5f (12pt White)
         * ========================================================= */
        cs.saveGraphicsState();
        cs.setNonStrokingColor(0.92941f, 0.1098f, 0.14118f); // Kotak Red
        cs.addRect(36f, 256f, 523f, 25f);
        cs.fill();
        cs.restoreGraphicsState();

        cs.beginText();
        cs.setFont(fonts.regular, 12);
        cs.setNonStrokingColor(1f, 1f, 1f); // White
        cs.appendRawCommands("1 0 0 1 228.27 263.5 Tm\n");
        cs.showText("Commonly Used Narrations");
        cs.endText();

        /* =========================================================
         * 5. NARRATIONS TABLE GRID (13 Rows x 2 Columns)
         * • Sits immediately flush below Red Banner 2 (at Y = 256f)
         * • rowH = 13.54f
         * • rowY = 256f - rowH = 242.46f (Top row Y; steps down by 13.54f per row down to ~80f)
         * • Left Column Box:  X = 36f, Width = 261.5f, Height = 13.54f
         * • Right Column Box: X = 297.5f, Width = 261.5f, Height = 13.54f
         * • Border Color: Gray (0.62353f, 0.63137f, 0.64314f), LineWidth = 0.5f
         * • Text baseline textY2: Starts at 245.46f, steps down by 13.54f
         * • Left text at X = 39f, Right text at X = 300.5f (7.0pt Regular Black)
         * ========================================================= */
        float rowH = 13.54f;
        float rowY = 256f - rowH; // 242.46f

        cs.saveGraphicsState();
        cs.setLineCapStyle(2);
        cs.setLineWidth(0.5f);
        cs.setStrokingColor(0.62353f, 0.63137f, 0.64314f); // Grid Border Gray
        for (int i = 0; i < 13; i++) {
            cs.addRect(36f, rowY - (i * rowH), 261.5f, rowH); // Left cell border
            cs.stroke();
            cs.addRect(297.5f, rowY - (i * rowH), 261.5f, rowH); // Right cell border
            cs.stroke();
        }
        cs.restoreGraphicsState();

        List<String[]> transactionCodes = List.of(
                new String[] {"AP - Autopay for Billpay", "Netcard - Netc@rd transaction"},
                new String[] {"ATL - ATM withdrawal done from other bank ATM machine", "OS - Online Shopping transaction"},
                new String[] {"ATW - ATM withdrawal done from Kotak ATM machine", "OT - Online Trading transaction via Payment Gateway"},
                new String[] {"BP - Bill Pay transaction", "PB - Transaction done through Phone Banking (IVR)"},
                new String[] {"CDM - Kotak Cash Deposit Machine", "PCI/PCD - POS transaction"},
                new String[] {"CMS - Cash Management Service", "RTGS - Real Time Gross Settlement"},
                new String[] {"IB - Transaction done on Kotak Net Banking", "UPI - Unified Payment Interface"},
                new String[] {"IMPS - Immediate Payment Service", "VISACCPAY - Visa Credit Card Payment"},
                new String[] {"IMT - Instant Money Transfer", "VMT - VISA Money Transfer"},
                new String[] {"KB - Billpay transaction via Keya Chatbot", "WB - Billpay transaction via WhatsApp Banking"},
                new String[] {"MB - Transaction done on Mobile banking", "Int. Pd. - Interest credited on your savings account balance"},
                new String[] {"NACH - National Automated Clearing House", "Sweep transfer to - Booking new Term Deposit"},
                new String[] {"NEFT - National Electronic Funds Transfer", "Sweep transfer from - Broken existing Term Deposit"}
        );

        double textY2 = 245.46;
        for (String[] pair : transactionCodes) {
            // Left column abbreviation text
            cs.beginText();
            cs.setTextMatrix(1, 0, 0, 1, 39f, textY2);
            cs.setFont(fonts.regular, 7.0f);
            cs.setNonStrokingColor(0f, 0f, 0f); // Black
            cs.showText(pair[0]);
            cs.endText();

            // Right column abbreviation text
            cs.beginText();
            cs.setTextMatrix(1, 0, 0, 1, 300.5f, textY2);
            cs.setFont(fonts.regular, 7.0f);
            cs.setNonStrokingColor(0f, 0f, 0f); // Black
            cs.showText(pair[1]);
            cs.endText();

            textY2 -= rowH; // Move down to next row
        }
    }

    // =========================================================================
    // MULTI-PAGE TOP HEADER & BOTTOM FOOTER (Appears on Pages 2, 3, 4, etc.)
    // =========================================================================
    private final void drawHeaderAndFooter(PDDocument doc, Fonts fonts) throws IOException {
        int totalPages = doc.getNumberOfPages();
        for (int i = 0; i < totalPages; i++) {
            PDPage page = doc.getPage(i);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                if (i > 0) {
                    drawHeader(cs, fonts); // Draw top customer details on pages 2+
                }
                drawFooter(cs, fonts, i + 1, totalPages); // Draw page footer on all pages
            }
        }
    }

    /* =========================================================
     * TOP CUSTOMER DETAILS BLOCK (Above table on Pages 2, 3, 4)
     * • Line 1 (Customer Name): X = 36, Y = 821 (9pt SemiBold Pure Black)
     * • Line 2 (Account No.):   X = 36, Y = 809 ("Account No. " Gray 9pt + Number Black 9pt)
     * • Line 3 (Period):        X = 36, Y = 797 ("Account Statement " Gray 9pt + Dates Black 9pt)
     * • TO MOVE BLOCK UP/DOWN: Adjust Y=821, 809, 797 (maintain 12pt difference).
     * ========================================================= */
    private void drawHeader(PDPageContentStream cs, Fonts fonts) throws IOException {
        String name = (stmt.details.fullName != null && !stmt.details.fullName.isBlank())
                ? stmt.details.fullName
                : (stmt.details.name != null ? stmt.details.name : "");
        String accNo = stmt.details.accountNumber != null ? stmt.details.accountNumber : "";
        String startDate = TemplateUtils.formatDateDMMMYYYY(stmt.meta.statementPeriodStart);
        String endDate = TemplateUtils.formatDateDMMMYYYY(stmt.meta.statementPeriodEnd);

        // Line 1: Customer Name (UPPERCASE)
        cs.beginText();
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f); // Pure Black
        cs.appendRawCommands("1 0 0 1 36 821 Tm\n");
        cs.showText(name.toUpperCase());
        cs.endText();

        // Line 2: "Account No." (Gray) + Value (Black)
        cs.beginText();
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f); // Medium Gray label
        cs.appendRawCommands("1 0 0 1 36 809 Tm\n");
        cs.showText("Account No. ");
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f); // Black value
        cs.showText(accNo);
        cs.endText();

        // Line 3: "Account Statement" (Gray) + Period (Black)
        cs.beginText();
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f); // Medium Gray label
        cs.appendRawCommands("1 0 0 1 36 797 Tm\n");
        cs.showText("Account Statement ");
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f); // Black value
        cs.showText(startDate + " - " + endDate);
        cs.endText();
    }

    /* =========================================================
     * BOTTOM PAGE FOOTER
     * • Left ("Statement Generated on..."): X = 36, Y = 16 (9.6pt Regular Gray 0.40f)
     * • Right ("Page X of Y"): X = 518.99, Y = 16 (10pt Regular Gray 0.40f)
     * ========================================================= */
    private void drawFooter(PDPageContentStream cs, Fonts fonts, int page, int totalPages) throws IOException {
        String date = TemplateUtils.formatDateTimeDMMMYYYYHHMM(stmt.meta.generatedAt);
        cs.beginText();
        cs.setFont(fonts.regular, 9.6f);
        cs.setNonStrokingColor(0.40f, 0.40f, 0.40f);
        cs.appendRawCommands("1 0 0 1 36 16 Tm\n");
        cs.showText("Statement Generated on " + date);
        cs.endText();

        cs.beginText();
        cs.setFont(fonts.regular, 10);
        cs.setNonStrokingColor(0.40f, 0.40f, 0.40f);
        cs.appendRawCommands("1 0 0 1 518.99 16 Tm\n");
        cs.showText("Page " + page + " of " + totalPages);
        cs.endText();
    }
}