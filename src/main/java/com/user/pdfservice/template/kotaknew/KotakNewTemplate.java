package com.user.pdfservice.template.kotaknew;

import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.*;
import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.Transaction;
import com.user.pdfservice.template.BankPdfTemplate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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
    private Map<String, Image> extractedImages = new LinkedHashMap<>();
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

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 0, 0, 0, 0);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            writer.setPdfVersion(PdfWriter.VERSION_1_5);
            writer.setFullCompression();

            // Password encryption (Mode B)
            String password = null;
            if (stmt.meta != null && stmt.meta.password != null && !stmt.meta.password.trim().isEmpty()) {
                password = stmt.meta.password.trim();
            } else if (stmt.details != null && stmt.details.password != null && !stmt.details.password.trim().isEmpty()) {
                password = stmt.details.password.trim();
            }

            if (password != null) {
                byte[] userPwd = password.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                int permissions = PdfWriter.AllowPrinting | PdfWriter.AllowCopy | PdfWriter.AllowScreenReaders;
                writer.setEncryption(userPwd, null, permissions, PdfWriter.ENCRYPTION_AES_128);
            }

            doc.open();

            // Set OpenPDF metadata
            writer.getInfo().put(PdfName.PRODUCER, new PdfString("OpenPDF 2.0.3"));
            writer.getInfo().put(PdfName.CREATIONDATE, new PdfDate());
            writer.getInfo().remove(PdfName.CREATOR);
            writer.getInfo().remove(PdfName.MODDATE);

            PdfContentByte cb = writer.getDirectContent();
            KotakCanvas canvas = new KotakCanvas(cb);

            // Obj 1: Template for Page Count (Form XObject [0 0 30 16])
            PdfTemplate tpl = cb.createTemplate(30, 16);

            // Load Fonts & register them as Obj 2, 3, 4
            Fonts fonts = Fonts.load();
            cb.setFontAndSize(fonts.semiBold, 12);
            cb.setFontAndSize(fonts.regular, 10);
            cb.setFontAndSize(fonts.helvetica, 10);

            RIGHT_BAL = 535f + fonts.regular.getWidthPoint("464.93", 7.68f);
            RIGHT_CR = 465.95f + fonts.regular.getWidthPoint("400.00", 7.68f);
            RIGHT_DR = 391.17f + fonts.regular.getWidthPoint("2,900.00", 7.68f);

            // Load raw images
            this.extractedImages = loadRawImages();

            // Page 1: Top logo banner -> Obj 5: img0
            Image img0 = extractedImages.get("img0");
            if (img0 != null) {
                canvas.drawImage(img0, 595f, 0f, 0f, 74.85f, 0f, 767.15f);
            }

            PageCursor pc = new PageCursor(doc, writer, fonts, tpl, canvas, PAGE1_TXN_START);
            pc.setPageHeaderRenderer(() -> {
                try {
                    drawHeader(canvas, fonts);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            pc.setPageFooterRenderer(() -> {
                try {
                    drawFooter(canvas, fonts, pc.getPageNumber(), tpl);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            // Render Page 1
            renderHeader(canvas, fonts);
            renderTransactionTableHeader(canvas, fonts, 487.5f);
            renderOpeningBalance(canvas, fonts);

            // Transactions across pages
            drawAllTransactions(stmt.transactions, pc, fonts);

            // Summary Page (Page 9 in 10-page doc)
            pc.nextPageWithImages(extractedImages, 1, 6);
            drawSummaryPage(pc, fonts);

            // Notices Page (Page 10 in 10-page doc)
            pc.nextPageWithImages(extractedImages, 7, 8);
            drawLastPage(pc, fonts);

            // Finish cursor (triggers last footer)
            pc.close();

            // Populate template with total page count
            tpl.beginText();
            tpl.setFontAndSize(fonts.regular, 10);
            tpl.showText(String.valueOf(pc.getPageNumber()));
            tpl.endText();

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private static Map<String, Image> loadRawImages() {
        File file = new File("account-bank.pdf");
        if (!file.exists()) {
            file = new File("src/main/resources/templates/kotak/account-bank.pdf");
        }
        try {
            PdfReader reader;
            if (file.exists()) {
                reader = new PdfReader(file.getAbsolutePath());
            } else {
                InputStream is = KotakNewTemplate.class.getResourceAsStream("/templates/kotak/account-bank.pdf");
                if (is == null) {
                    throw new IllegalStateException("account-bank.pdf template not found");
                }
                reader = new PdfReader(is);
            }
            try {
                Map<String, Image> map = new LinkedHashMap<>();

                // Obj 5: img0
                PRStream s5 = (PRStream) reader.getPdfObject(5);
                Image img0 = Image.getInstance(PdfReader.getStreamBytesRaw(s5));
                map.put("img0", img0);

                // Obj 24: img1
                PRStream s24 = (PRStream) reader.getPdfObject(24);
                Image img1 = Image.getInstance(PdfReader.getStreamBytesRaw(s24));
                map.put("img1", img1);

                // Obj 25 & 26: img2 & img3
                PRStream s25 = (PRStream) reader.getPdfObject(25);
                Image img2 = Image.getInstance(200, 200, 1, 8, PdfReader.getStreamBytes(s25));
                img2.makeMask();
                map.put("img2", img2);

                PRStream s26 = (PRStream) reader.getPdfObject(26);
                Image img3 = Image.getInstance(200, 200, 3, 8, PdfReader.getStreamBytes(s26));
                img3.setImageMask(img2);
                map.put("img3", img3);

                // Obj 27 & 28: img4 & img5
                PRStream s27 = (PRStream) reader.getPdfObject(27);
                Image img4 = Image.getInstance(200, 200, 1, 8, PdfReader.getStreamBytes(s27));
                img4.makeMask();
                map.put("img4", img4);

                PRStream s28 = (PRStream) reader.getPdfObject(28);
                Image img5 = Image.getInstance(200, 200, 3, 8, PdfReader.getStreamBytes(s28));
                img5.setImageMask(img4);
                map.put("img5", img5);

                // Obj 29 & 30: ICC Profile & img6 (QR code)
                PRStream s29 = (PRStream) reader.getPdfObject(29);
                PRStream s30 = (PRStream) reader.getPdfObject(30);
                Image img6 = Image.getInstance(PdfReader.getStreamBytesRaw(s30));
                if (s29 != null) {
                    try {
                        byte[] iccBytes = PdfReader.getStreamBytes(s29);
                        java.awt.color.ICC_Profile icc = java.awt.color.ICC_Profile.getInstance(iccBytes);
                        img6.tagICC(icc);
                    } catch (Exception ignored) {}
                }
                map.put("img6", img6);

                // Obj 34 & 35: img7 & img8
                PRStream s34 = (PRStream) reader.getPdfObject(34);
                Image img7 = Image.getInstance(100, 100, 1, 8, PdfReader.getStreamBytes(s34));
                img7.makeMask();
                map.put("img7", img7);

                PRStream s35 = (PRStream) reader.getPdfObject(35);
                Image img8 = Image.getInstance(100, 100, 3, 8, PdfReader.getStreamBytes(s35));
                img8.setImageMask(img7);
                map.put("img8", img8);

                return map;
            } finally {
                reader.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load raw images for Kotak template", e);
        }
    }

    // =========================================================================
    // PAGE 1: HEADER & USER ACCOUNT DETAILS
    // =========================================================================
    private void renderHeader(KotakCanvas cs, Fonts fonts) throws Exception {
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

    private void renderHeaderInfo(KotakCanvas cs, Fonts fonts) throws Exception {
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
         * • Branch Phone:       Y = 617 (Label Gray + Value SemiBold Black)
         * • Account Status:     Y = 591 (Label Gray + Value SemiBold Black)
         * • Nominee Registered: Y = 575 (Label Gray + Value SemiBold Black)
         * • Currency:           Y = 549 (Label Gray + Value SemiBold Black)
         * ========================================================= */
        // Account No.
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 347.5 665 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f);
        cs.showText("Account No. ");
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(stmt.details.accountNumber != null ? stmt.details.accountNumber : "");
        cs.endText();

        // Account Type
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 347.5 649 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f);
        cs.showText("Account Type  ");
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        String accType = stmt.details.accountType != null ? stmt.details.accountType : "Savings";
        cs.showText(accType);
        cs.endText();

        // Branch
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 347.5 633 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f);
        cs.showText("Branch ");
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(stmt.details.branchAddress != null ? stmt.details.branchAddress : "");
        cs.endText();

        // Branch Phone Number
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 347.5 617 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f);
        cs.showText("Branch Phone Number ");
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        String bPhone = (stmt.details.branchPhoneNo != null && !stmt.details.branchPhoneNo.isBlank() && !stmt.details.branchPhoneNo.equals(stmt.details.accountNumber))
                ? stmt.details.branchPhoneNo
                : "9522262613";
        cs.showText(bPhone);
        cs.endText();

        // Account Status
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 347.5 591 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f);
        cs.showText("Account Status ");
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("Active");
        cs.endText();

        // Nominee Registered
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 347.5 575 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f);
        cs.showText("Nominee Registered ");
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("Yes");
        cs.endText();

        // Currency
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 347.5 549 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f);
        cs.showText("Currency ");
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("INDIAN RUPEE");
        cs.endText();
    }

    // =========================================================================
    // TRANSACTION TABLE HEADER & OPENING BALANCE
    // =========================================================================
    private void renderTransactionTableHeader(KotakCanvas cs, Fonts fonts, float topY) throws Exception {
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

    private void renderOpeningBalance(KotakCanvas cs, Fonts fonts) throws Exception {
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
                KotakCanvas cs = pc.cs();
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
        KotakCanvas cs = pc.cs();
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
        KotakCanvas cs = pc.cs();

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

    public static List<String> wrapDescriptionLines(String text, BaseFont font, float fontSize, float maxWidth) {
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
            float width = font.getWidthPoint(test, fontSize);
            if (width <= maxWidth) {
                line.setLength(0);
                line.append(test);
            } else {
                if (line.length() > 0) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                float tokWidth = font.getWidthPoint(token, fontSize);
                if (tokWidth <= maxWidth) {
                    line.append(token);
                } else {
                    String rem = token;
                    while (font.getWidthPoint(rem, fontSize) > maxWidth) {
                        int cut = rem.length();
                        while (cut > 1 && font.getWidthPoint(rem.substring(0, cut), fontSize) > maxWidth) {
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

    private float renderWrappedDescription(PageCursor pc, BaseFont font, float fontSize, String text, float startX, float startY, int maxLinesOnPage) throws Exception {
        KotakCanvas cs = pc.cs();
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

    private List<String> wrapTextByWidth(String text, BaseFont font, float fontSize, float maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            float width = font.getWidthPoint(test, fontSize);
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

    private int countWrappedLines(BaseFont font, float fontSize, String text, float maxWidth) {
        return Math.max(1, wrapDescriptionLines(text, font, fontSize, maxWidth).size());
    }

    private final void drawAllTransactions(List<Transaction> txns, PageCursor pc, Fonts fonts) throws Exception {
        if (txns == null || txns.isEmpty()) return;

        for (int i = 0; i < txns.size(); i++) {
            Transaction t = txns.get(i);
            int lines = countWrappedLines(fonts.regular, 7.68f, t.description != null ? t.description : "", DESC_WIDTH);
            float rowHeight = TXNROW_H + (lines - 1) * LINE_H;

            pc.ensureSpace(rowHeight);

            float yBefore = pc.y();
            renderTransactionInfo(t, yBefore, pc, fonts);

            String[] tvals = { String.valueOf(i + 1), TemplateUtils.formatDateDMMMYYYY(t.date), t.description };
            for (int col = 0; col < tvals.length; col++) {
                float x = TXN_X[col];
                if (col != 2) {
                    KotakCanvas cs = pc.cs();
                    cs.beginText();
                    cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", x, yBefore));
                    cs.setFont(fonts.regular, 7.68f);
                    cs.showText(tvals[col]);
                    cs.setNonStrokingColor(0f, 0f, 0f);
                    cs.endText();
                } else {
                    renderWrappedDescription(pc, fonts.regular, 7.68f, t.description, DESC_X, yBefore, lines);
                }
            }

            KotakCanvas cs = pc.cs();
            cs.setLineWidth(0.5f);
            cs.setLineCapStyle(2);
            cs.setStrokingColor(0.62353f, 0.63137f, 0.64314f);
            float sepY = yBefore - (lines - 1) * LINE_H - 3f;
            cs.moveTo(36f, sepY);
            cs.lineTo(559f, sepY);
            cs.stroke();

            pc.moveDown(rowHeight);
        }
    }

    // =========================================================================
    // PAGE 9: ACCOUNT SUMMARY & ASSISTANCE PAGE
    // =========================================================================
    private final void drawSummaryPage(PageCursor pc, Fonts fonts) throws Exception {
        KotakCanvas cs = pc.cs();

        /* =========================================================
         * 1. RED SECTION HEADER BAR ("Account Summary")
         * • X = 36 (Left margin), Y = 737, Width = 523, Height = 25
         * ========================================================= */
        cs.saveGraphicsState();
        cs.setNonStrokingColor(0.92941f, 0.1098f, 0.14118f); // Kotak Red
        cs.addRect(36, 737, 523, 25);
        cs.fill();
        cs.restoreGraphicsState();

        /* Title text inside Red Bar: "Account Summary" (White, 12pt) */
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 251.42 744.5 Tm\n");
        cs.setFont(fonts.regular, 12);
        cs.setNonStrokingColor(1f, 1f, 1f); // White text
        cs.showText("Account Summary");
        cs.endText();

        /* =========================================================
         * 2. COLUMN HEADER BACKGROUNDS (Gray banner below red bar)
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
        cs.appendRawCommands("1 0 0 1 48 716 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.showText("   Particulars");
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 285.73 716 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.showText("Opening Balance");
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 428.36 716 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.showText("Closing Balance");
        cs.endText();

        /* =========================================================
         * 3. SUMMARY DATA ROW ("Savings Account (SA):", Balances)
         * ========================================================= */
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 39 693.32 Tm\n");
        cs.setFont(fonts.regular, 7.68f);
        cs.setNonStrokingColor(0f, 0f, 0f); // Pure black
        cs.showText("   Savings Account (SA):");
        cs.endText();

        // Right alignment bounds for Opening & Closing balances
        float OPENING_RIGHT = 359.64f + 26.7264f;
        float CLOSING_RIGHT = 502.27f + 26.7264f;

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
         * 5. AD BANNER IMAGE -> Obj 24: img1
         * ========================================================= */
        Image img1 = extractedImages.get("img1");
        if (img1 != null) {
            cs.drawImage(img1, 523f, 0f, 0f, 86.31f, 36f, 434.01f);
        }

        /* =========================================================
         * 6. ASSISTANCE SECTION (Red Bar + 3-Column Contact Details)
         * ========================================================= */
        float assistBaseY = 245f;

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

        // 3-Column Layout Geometry
        float midLeftX     = 184f; // divider 1
        float midRightX    = 416f; // divider 2

        float col1Center   = 110f;          // Contact Us center
        float col2Center   = 300f;          // Branch Address center
        float col3Center   = 487.5f;        // Branch Phone center

        // Vertical dividers
        float divTop = assistBaseY - 15f;
        float divBottom = assistBaseY - 76f;
        cs.setLineWidth(1f);
        cs.setStrokingColor(0.62353f, 0.63137f, 0.64314f); // Gray divider lines
        cs.moveTo(midLeftX, divTop);
        cs.lineTo(midLeftX, divBottom);
        cs.stroke();
        cs.moveTo(midRightX, divTop);
        cs.lineTo(midRightX, divBottom);
        cs.stroke();

        float iconSize = 21f;
        float iconY = assistBaseY - 33f;

        // ---- Column 1: Contact Us (Phone icon -> Obj 25: smask img2, Obj 26: img3) ----
        Image img3 = extractedImages.get("img3");
        if (img3 != null) {
            cs.drawImage(img3, iconSize, 0f, 0f, iconSize, col1Center - iconSize / 2f, iconY);
        }

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
        cs.setNonStrokingColor(0f, 0f, 0f);
        String note = "(Toll-free number)";
        float noteX = centerAlign(note, col1Center, fonts.regular, 7);
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", noteX, assistBaseY - 69f));
        cs.showText(note);
        cs.endText();

        // ---- Column 2: Branch Address (Bank icon -> Obj 27: smask img4, Obj 28: img5) ----
        Image img5 = extractedImages.get("img5");
        if (img5 != null) {
            cs.drawImage(img5, iconSize, 0f, 0f, iconSize, col2Center - iconSize / 2f, iconY);
        }

        cs.beginText();
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        String t2 = "Branch Address";
        float t2X = centerAlign(t2, col2Center, fonts.semiBold, 9);
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", t2X, assistBaseY - 45f));
        cs.showText(t2);
        cs.endText();

        float addrMaxWidth = 216f;
        String bAddress = (stmt.details.branchAddress != null && !stmt.details.branchAddress.isBlank())
                ? stmt.details.branchAddress
                : "27 BKC, C 27, G Block, Bandra Kurla Complex, Bandra (E), Mumbai";
        List<String> brLines = wrapTextByWidth(bAddress, fonts.regular, 7.5f, addrMaxWidth);
        float brFontSize = (brLines.size() > 3) ? 6.5f : 7.5f;
        float lineHeight = (brLines.size() > 3) ? 8.5f : 9.5f;
        if (brFontSize != 7.5f) {
            brLines = wrapTextByWidth(bAddress, fonts.regular, brFontSize, addrMaxWidth);
        }
        float addrY = assistBaseY - 57f;
        for (int i = 0; i < Math.min(brLines.size(), 3); i++) {
            String line = brLines.get(i);
            float lineX = centerAlign(line, col2Center, fonts.regular, brFontSize);
            cs.beginText();
            cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", lineX, addrY - i * lineHeight));
            cs.setFont(fonts.regular, brFontSize);
            cs.setNonStrokingColor(0f, 0f, 0f);
            cs.showText(line);
            cs.endText();
        }

        // ---- Column 3: Branch Phone Number (Phone icon /img3 reused) ----
        if (img3 != null) {
            cs.drawImage(img3, iconSize, 0f, 0f, iconSize, col3Center - iconSize / 2f, iconY);
        }

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
         * 7. "REMEMBER!" BOX (Light Gray Rounded Container + QR Code)
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

        // Vertical divider line inside Remember box
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

        // QR Code Image -> Obj 29 (ICC profile), Obj 30 (img6)
        Image img6 = extractedImages.get("img6");
        if (img6 != null) {
            cs.drawImage(img6, 45.6f, 0f, 0f, 45.6f, 399.84f, 98.4f);
        }

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
         * 8. LEGAL FOOTER (CIN line & Registered Office line)
         * ========================================================= */
        cs.setStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.setLineWidth(1f);
        cs.moveTo(36f, 76f);
        cs.lineTo(559f, 76f);
        cs.stroke();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 168.66 61 Tm\n");
        cs.setFont(fonts.regular, 9.12f);
        cs.setNonStrokingColor(0f, 0f, 0f); // Pure Black
        cs.showText("Kotak Mahindra Bank Ltd. | CIN: L65110MH1985PLC038137");
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 63.75 43 Tm\n");
        cs.setFont(fonts.regular, 9.12f);
        cs.setNonStrokingColor(0f, 0f, 0f); // Pure Black
        cs.showText("Registered Office: 27 BKC, C 27, G Block, Bandra Kurla Complex, Bandra (E), Mumbai - 400 051. www.kotak.bank.in");
        cs.endText();

        // Obj 31: Link Annotation on Page 9
        PdfAction act9 = new PdfAction("https://www.kotak.bank.in/en/home.html");
        PdfAnnotation annot9 = new PdfAnnotation(pc.writer(), 36f, 434.01f, 559f, 520.32f, act9);
        annot9.put(PdfName.SUBTYPE, PdfName.LINK);
        annot9.put(PdfName.BORDER, new PdfBorderArray(0, 0, 0));
        pc.writer().addAnnotation(annot9);
    }

    private float centerAlign(String text, float center, BaseFont font, float fontSize) {
        float textWidth = font.getWidthPoint(text, fontSize);
        return center - (textWidth / 2f);
    }

    // =========================================================================
    // PAGE 10: IMPORTANT INFORMATION & NOTICES PAGE
    // =========================================================================
    private final void drawLastPage(PageCursor pc, Fonts fonts) throws Exception {
        KotakCanvas cs = pc.cs();

        /* =========================================================
         * 1. RED HEADER BAR ("Important Information")
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
         * 3. 12 BULLET NOTICES -> Obj 34 (smask img7), Obj 35 (img8)
         * ========================================================= */
        float curY = 696f;
        float textX = 66.0f;
        float bulletIconX = 49.0f;
        float maxTextW = 482f;
        float fontSize = 7.15f;
        float lineH = 8.0f;
        float regularGap = 9.0f;
        float largerGap = 13.0f;

        Image img8 = extractedImages.get("img8");

        for (int i = 0; i < notices.length; i++) {
            // Draw bullet dot icon (4x4 points) -> Obj 34 & 35
            if (img8 != null) {
                cs.drawImage(img8, 4f, 0f, 0f, 4f, bulletIconX, curY - 1.32f);
            }

            List<String> wrappedLines = wrapTextByWidth(notices[i], fonts.regular, fontSize, maxTextW);
            for (String line : wrappedLines) {
                cs.beginText();
                cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", textX, curY));
                cs.setFont(fonts.regular, fontSize);
                cs.setNonStrokingColor(0f, 0f, 0f); // Pure black
                cs.showText(line);
                cs.endText();
                curY -= lineH;
            }
            float gap = (i >= 9) ? largerGap : regularGap;
            curY -= gap;
        }

        /* =========================================================
         * 4. "COMMONLY USED NARRATIONS" RED HEADER BAR
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

            textY2 -= rowH;
        }

        // Obj 36: ExtGState
        PdfGState gs = new PdfGState();
        gs.setFillOpacity(1.0f);
        cs.setGState(gs);

        // Obj 37: Link Annotation on Page 10
        PdfAction act10 = new PdfAction("https://www.kotak.bank.in/en/reach-us.html");
        PdfAnnotation annot10 = new PdfAnnotation(pc.writer(), 230.98f, 430.32f, 376.07f, 438f, act10);
        annot10.put(PdfName.SUBTYPE, PdfName.LINK);
        annot10.put(PdfName.BORDER, new PdfBorderArray(0, 0, 0));
        pc.writer().addAnnotation(annot10);
    }

    // =========================================================================
    // MULTI-PAGE TOP HEADER & BOTTOM FOOTER
    // =========================================================================
    private void drawHeader(KotakCanvas cs, Fonts fonts) {
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

    private void drawFooter(KotakCanvas cs, Fonts fonts, int page, PdfTemplate tpl) {
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
        cs.showText("Page " + page + " of ");
        cs.endText();

        float textWidth = fonts.regular.getWidthPoint("Page " + page + " of ", 10f);
        cs.getPdfContentByte().addTemplate(tpl, 518.99f + textWidth, 16f);
    }
}
