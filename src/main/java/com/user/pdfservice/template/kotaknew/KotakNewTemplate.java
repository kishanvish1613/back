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
            "RBI mandates Positive Pay for high-value cheques from Jan 1, 2021. Customers must submit cheque details via Net/Mobile Banking or at the branch on\n" +
                    "the day of issuance or before handing it to the beneficiary. For more details, visit www.kotak.bank.in.",
            "From October 4, 2025, same-day cheque clearing will be implemented across all banks. Cheques will be credited or debited within a few hours of\n" +
                    "issuance.",
            "Complimentary insurance cover on Kotak Debit Cards (linked to Saving and Current accounts) will be discontinued w.e.f. July 20, 2025. All claims will be\n" +
                    "accepted until July 20, 2025, as per the existing process. Salary account holders may view their insurance covers under Debit Card Services in the Cards\n" +
                    "& FASTag section on the web portal. For any queries related to Debit Card insurance, write to dc.insurance@kotak.com or visit\n" +
                    "https://www.kotak.bank.in/en/personal-banking/cards/debit-cards/debit-card-services/insurnace-on-debit-card.html",
            "In order to avail TDS exemption (if eligible) on existing/new Fixed Deposits for the Financial Year 2023–24, fresh Form 15G (15H for senior citizens) must\n" +
                    "be submitted by furnishing the details of all Fixed Deposits held, at the earliest. Form 15G/15H can now be submitted conveniently through Net Banking\n" +
                    "as well. From FY 2019–20, the TDS exemption threshold is increased to Rs. 50,000 (Rs. 1,00,000 for senior citizens); hence, filing of 15G/15H is not\n" +
                    "required up to Rs. 50,000 (Rs. 1,00,000 for senior citizens) of the aggregate interest earned on bank deposits in a financial year. Please ignore this\n" +
                    "message if already submitted.",
            "RBI guidelines on unauthorised foreign exchange transactions dated April 24, 2024, state that unauthorised entities offering foreign exchange (forex)\n" +
                    "trading facilities to Indian residents with promises of disproportionate/exorbitant returns are providing options to remit/deposit funds in Rupees for\n" +
                    "undertaking unauthorised forex transactions using various digital channels. Refer to the 'Alert List' containing names of such entities (this list is\n" +
                    "indicative and not exhaustive). Vigilance and caution are advised. Liaison must be with 'Authorised Persons' and on 'Authorised ETPs' for\n" +
                    "processing/routing any forex transaction.",
            "As per the RBI Master Direction for Credit and Debit Card Issuance (RBI/2022-23/92 DoR.AUT.REC. No.27/24.01.041/2022-23), issuance of debit cards on\n" +
                    "Overdraft accounts is not permissible.",
            "RBI, vide its circular DOR.CRE.REC.23/21.08.008/2022-23 dated April 19, 2022, has issued guidelines pertaining to the opening and maintenance of\n" +
                    "Current Account(s) of customers who have availed various credit facilities from the banking system. The term \"banking system\" refers to Scheduled\n" +
                    "Commercial Banks and Payments Banks. Banks (whether lending banks or otherwise) are required to monitor all Current Account, Overdraft, and Cash\n" +
                    "Credit accounts on a regular basis, at least on a half-yearly frequency. This monitoring must specifically consider the aggregate exposure of the banking\n" +
                    "system to the borrower vis-à-vis the individual bank's share in that exposure, in order to ensure compliance with the said instructions. Detailed\n" +
                    "guidelines are available in the aforementioned circular.",
            "Deposits of up to ₹5,00,000 per depositor are fully insured by the Deposit Insurance and Credit Guarantee Corporation, under the Deposit Insurance\n" +
                    "Scheme.",
            "Goods and Services Tax (GST), at the applicable rate of 18%, is levied on relevant service charges.",
            "Please note: This statement/ advice should not be construed as a Tax Invoice under the Goods and Services Tax Act.",
    };

    @Override
    public byte[] generate(Statement statement) {
        this.stmt = statement;
        // Clamp closing balance to never be negative
        CLOSING_BAL = Math.max(0, stmt.transactions.get(stmt.transactions.size() - 1).balance);

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

    // ----------------- HEADER -----------------
    private void renderHeader(PDPageContentStream cs, Fonts fonts) throws Exception {
        cs.beginText();
        cs.appendRawCommands("36 762 Td\n");
        cs.appendRawCommands("0 -37.5 Td\n");
        cs.setFont(fonts.semiBold, 25);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("Account Statement");
        cs.setNonStrokingColor(0f);
        cs.appendRawCommands("0 0 Td\n");
        cs.appendRawCommands("0 -16.5 Td\n");
        String startDate = TemplateUtils.formatDateDMMMYYYY(stmt.meta.statementPeriodStart);
        String endDate = TemplateUtils.formatDateDMMMYYYY(stmt.meta.statementPeriodEnd);
        cs.setFont(fonts.regular, 11);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(startDate + " - " + endDate);
        cs.setNonStrokingColor(0f);
        cs.appendRawCommands("0 0 Td\n");
        cs.appendRawCommands("0 -185.5 Td\n");
        cs.endText();
        renderHeaderInfo(cs, fonts);
    }

    private void renderHeaderInfo(PDPageContentStream cs, Fonts fonts) throws Exception {
        // Name
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 38 660 Tm\n");
        cs.setFont(fonts.semiBold, 14);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(stmt.details.name);
        cs.setNonStrokingColor(0f);
        cs.endText();

        String crn = "xxxxxx" + stmt.details.customerRelNo.substring(6);
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 38 643.5 Tm\n");
        cs.setFont(fonts.regular, 11);
        cs.setNonStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.showText("CRN " + crn);
        cs.setNonStrokingColor(0f);
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 38 625.5 Tm\n");
        cs.setFont(fonts.helvetica, 12);
        cs.showText("");
        cs.endText();

        String[] addrlines = stmt.details.address.split("\\n");
        String[] ADDR_TMS = {
                "1 0 0 1 38 610.5 Tm\n",
                "1 0 0 1 38 595.5 Tm\n",
                "1 0 0 1 38 580.5 Tm\n",
                "1 0 0 1 38 565.5 Tm\n",
                "1 0 0 1 38 550.5 Tm\n",
        };
        cs.beginText();
        for (int i = 0; i < addrlines.length; i++) {
            cs.appendRawCommands(ADDR_TMS[i]);
            cs.setFont(fonts.regular, 9);
            cs.setNonStrokingColor(0f, 0f, 0f);
            cs.showText(addrlines[i]);
            cs.setNonStrokingColor(0f);
        }
        cs.endText();

        // MICR / IFSC
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 38 524.5 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.showText("MICR ");
        cs.setNonStrokingColor(0f);
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(stmt.details.micr);
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.showText("   IFSC Code ");
        cs.setNonStrokingColor(0f);
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(stmt.details.ifsc);
        cs.setNonStrokingColor(0f);
        cs.endText();

        // Right column
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 347.5 665 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.showText("Account No. ");
        cs.setNonStrokingColor(0f);
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(stmt.details.accountNumber);
        cs.setNonStrokingColor(0f);
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 347.5 649 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.showText("Account Type  ");
        cs.setNonStrokingColor(0f);
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(stmt.details.accountType);
        cs.setNonStrokingColor(0f);
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 347.5 633 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.showText("Branch ");
        cs.setNonStrokingColor(0f);
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(stmt.details.branch);
        cs.setNonStrokingColor(0f);
        cs.appendRawCommands("1 0 0 1 347.5 617 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.showText("Branch Phone Number ");
        cs.setNonStrokingColor(0f);
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(stmt.details.branchPhoneNo);
        cs.setNonStrokingColor(0f);
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 347.5 591 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.showText("Account Status ");
        cs.setNonStrokingColor(0f);
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("Active");
        cs.setNonStrokingColor(0f);
        cs.appendRawCommands("1 0 0 1 347.5 575 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.showText("Nominee Registered ");
        cs.setNonStrokingColor(0f);
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("Yes");
        cs.setNonStrokingColor(0f);
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 347.5 549 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.showText("Currency ");
        cs.setNonStrokingColor(0f);
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("INDIAN RUPEE");
        cs.setNonStrokingColor(0f);
        cs.endText();
    }

    // ----------------- TRANSACTION TABLE HEADER -----------------
    private void renderTransactionTableHeader(PDPageContentStream cs, Fonts fonts, float topY) throws Exception {
        float tableLeft = 36f;
        float tableRight = 559f;
        float redBarHeight = 25f;
        float headerRowHeight = 25f;
        float headerRowY = topY - headerRowHeight;
        float[] colWidths = { 31.18f, 49f, 155.47f, 80.19f, 69.05f, 69.05f, 69.05f };
        float[] colTextX = { 44f, 75.18f, 124.19f, 279.66f, 354.85f, 428.9f, 497.95f };

        // Red title bar
        cs.setNonStrokingColor(0.92941f, 0.1098f, 0.14118f);
        cs.addRect(tableLeft, topY, tableRight - tableLeft, redBarHeight);
        cs.fill();
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 222.79 " + (topY + 7.5f) + " Tm\n");
        cs.setFont(fonts.regular, 12);
        cs.setNonStrokingColor(1f, 1f, 1f);
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
        // Clamp amount for display (should already be ≥0, but safety)
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
                Math.max(0, amt),   // Clamp balance display
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

    private float renderWrappedDescription(PageCursor pc, PDFont font, float fontSize, String text, float startX, float startY, int maxLinesOnPage) throws Exception {
        PDPageContentStream cs = pc.cs();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        float y = startY;
        int renderedLines = 0;

        for (String word : words) {
            String testLine = line.length() == 0 ? word : line + " " + word;
            float width = (font.getStringWidth(testLine) / 1000f) * fontSize;
            if (width > DESC_WIDTH) {
                if (renderedLines == maxLinesOnPage) {
                    pc.nextPage();
                    pc.renderTransactionTableHeader();
                    cs = pc.cs();
                    y = pc.y();
                    renderedLines = 0;
                }
                cs.beginText();
                cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", startX, y));
                cs.setFont(font, fontSize);
                cs.showText(line.toString());
                cs.endText();
                y -= LINE_H;
                renderedLines++;
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(testLine);
            }
        }

        if (line.length() > 0) {
            if (renderedLines == maxLinesOnPage) {
                cs.setLineWidth(0.5f);
                cs.setLineCapStyle(2);
                cs.setStrokingColor(0.62353f, 0.63137f, 0.64314f);
                y = 36f;
                cs.moveTo(36f, y);
                cs.lineTo(559f, y);
                cs.stroke();
                pc.nextPage();
                pc.renderTransactionTableHeader();
                cs = pc.cs();
                y = pc.y();
            }
            cs.beginText();
            cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm\n", startX, y));
            cs.setFont(font, fontSize);
            cs.showText(line.toString());
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

    private int countWrappedLines(PDFont font, float fontSize, String text, float maxWidth) throws Exception {
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        int lines = 1;
        for (String word : words) {
            String testLine = line.length() == 0 ? word : line + " " + word;
            float width = (font.getStringWidth(testLine) / 1000f) * fontSize;
            if (width > maxWidth) {
                lines++;
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(testLine);
            }
        }
        return lines;
    }

    private final void drawAllTransactions(List<Transaction> txns, PageCursor pc, Fonts fonts) throws Exception {
        int i = 1;
        float y = PAGE1_TXN_START;
        for (Transaction txn : txns) {
            pc.ensureSpace(3f);
            pc.y(renderTransaction(i++, txn, pc, fonts));
            pc.moveDown(TXNROW_H);
        }
    }

    // ----------------- SUMMARY PAGE (with clamped closing balance) -----------------
    private final void drawSummaryPage(PageCursor pc, Fonts fonts) throws IOException {
        PDPageContentStream cs = pc.cs();

        /* =========================
         * TOP SUMMARY TEXT BLOCK
         * ========================= */
        cs.saveGraphicsState();
        cs.beginText();
        cs.appendRawCommands("36 762 Td\n");
        cs.appendRawCommands("0 -25 Td\n");
        cs.appendRawCommands("0 -46.68 Td\n");
        cs.appendRawCommands("0 -20 Td\n");
        cs.setFont(fonts.helvetica, 12);
        cs.showText(" ");
        cs.appendRawCommands("0 -18 Td\n");
        cs.showText(" ");
        cs.appendRawCommands("0 -20 Td\n");
        cs.showText(" ");
        cs.appendRawCommands("0 0 Td\n");
        cs.appendRawCommands("216.6 -16 Td\n");
        cs.setFont(fonts.regular, 12);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("End of Statement");
        cs.setNonStrokingColor(0f);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(" ");
        cs.setNonStrokingColor(0f);
        cs.appendRawCommands("-216.6 0 Td\n");
        cs.appendRawCommands("68.56 -16 Td\n");
        cs.setFont(fonts.regular, 9.12f);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("Any discrepancy in the statement should be brought to the notice of Kotak Mahindra Bank Ltd. within");
        cs.setNonStrokingColor(0f);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(" ");
        cs.setNonStrokingColor(0f);
        cs.appendRawCommands("-68.56 0 Td\n");
        cs.appendRawCommands("160.27 -16 Td\n");
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("one month from the date of receipt of the statement.");
        cs.setNonStrokingColor(0f);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(" ");
        cs.setNonStrokingColor(0f);
        cs.appendRawCommands("-160.27 0 Td\n");
        cs.appendRawCommands("120.53 -16 Td\n");
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("This is a system generated report and does not require signature & stamp.");
        cs.setNonStrokingColor(0f);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(" ");
        cs.setNonStrokingColor(0f);
        cs.appendRawCommands("-120.53 0 Td\n");
        cs.appendRawCommands("0 -204 Td\n");
        cs.endText();
        cs.restoreGraphicsState();

        /* =========================
         * RED SECTION HEADER BAR
         * ========================= */
        cs.saveGraphicsState();
        cs.setNonStrokingColor(0.92941f, 0.1098f, 0.14118f);
        cs.addRect(36, 737, 523, 25);
        cs.fill();
        cs.restoreGraphicsState();

        /* =========================
         * RED BAR TITLE
         * ========================= */
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 251.42 744.5 Tm\n");
        cs.setFont(fonts.regular, 12);
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.showText("Account Summary");
        cs.setNonStrokingColor(0f);
        cs.endText();

        /* =========================
         * COLUMN HEADER BACKGROUNDS
         * ========================= */
        cs.saveGraphicsState();
        cs.setNonStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.addRect(36, 704, 237.73f, 33);
        cs.addRect(273.73f, 704, 142.64f, 33);
        cs.addRect(416.36f, 704, 142.64f, 33);
        cs.fill();
        cs.restoreGraphicsState();

        /* =========================
         * COLUMN SEPARATORS
         * ========================= */
        cs.setLineWidth(0.5f);
        cs.setStrokingColor(1f, 1f, 1f);
        cs.moveTo(273.73f, 704);
        cs.lineTo(273.73f, 737);
        cs.stroke();
        cs.moveTo(416.36f, 704);
        cs.lineTo(416.36f, 737);
        cs.stroke();
        cs.moveTo(559, 704);
        cs.lineTo(559, 737);
        cs.stroke();

        /* =========================
         * COLUMN TITLES
         * ========================= */
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 48 716 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.showText("   Particulars");
        cs.setNonStrokingColor(0f);
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 285.73 716 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.showText("Opening Balance");
        cs.setNonStrokingColor(0f);
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 428.36 716 Tm\n");
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.showText("Closing Balance");
        cs.setNonStrokingColor(0f);
        cs.endText();

        /* =========================
         * SAMPLE ROW
         * ========================= */
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 39 693.32 Tm\n");
        cs.setFont(fonts.regular, 7.68f);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("   Savings Account (SA):");
        cs.setNonStrokingColor(0f);
        cs.endText();

        float OPENING_RIGHT = 359.64f + 26.7264f;
        float CLOSING_RIGHT = 502.27f + 26.7264f;

        Object[] r = TemplateUtils.formatBalanceAndRightAlignX(
                stmt.details.startingBalance,
                fonts.regular, 7.68f, OPENING_RIGHT
        );
        String fAmt = (String) r[0];
        float x1 = (float) r[1];
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 359.64 693.32 Tm\n");
        cs.setFont(fonts.regular, 7.68f);
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f 693.32 Tm\n", x1));
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(fAmt);
        cs.setNonStrokingColor(0f);
        cs.endText();

        r = TemplateUtils.formatBalanceAndRightAlignX(
                Math.max(0, CLOSING_BAL),   // clamp closing balance display
                fonts.regular, 7.68f, CLOSING_RIGHT
        );
        fAmt = (String) r[0];
        x1 = (float) r[1];
        cs.beginText();
        cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 %.2f 693.32 Tm\n", x1));
        cs.setFont(fonts.regular, 7.68f);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(fAmt);
        cs.setNonStrokingColor(0f);
        cs.endText();

        cs.setLineWidth(0.5f);
        cs.setLineCapStyle(2);
        cs.setStrokingColor(0.62353f, 0.63137f, 0.64314f);

        float y = 693.32f;
        y -= 3f;
        cs.moveTo(36f, y);
        cs.lineTo(559f, y);
        cs.stroke();

        // Image and footer sections unchanged
        cs.appendRawCommands("q 508.67 0 0 84 43.17 364.32 cm /img1 Do Q\n");
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 43.17 364.32 Tm\n");
        cs.appendRawCommands("508.67 0\n");
        cs.appendRawCommands("-508.67 0\n");
        cs.endText();

        cs.saveGraphicsState();
        cs.setNonStrokingColor(0.92941f, 0.1098f, 0.14118f);
        cs.addRect(36f, 238f, 523f, 25f);
        cs.fill();
        cs.restoreGraphicsState();

        cs.saveGraphicsState();
        cs.setLineCapStyle(2);
        cs.appendRawCommands("0 G\n");
        cs.restoreGraphicsState();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 214.19 245.5 Tm\n");
        cs.setFont(fonts.regular, 12);
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.showText("For assistance, reach out to us at:");
        cs.setNonStrokingColor(0f);
        cs.endText();

        cs.saveGraphicsState();
        cs.restoreGraphicsState();
        cs.saveGraphicsState();
        cs.setLineCapStyle(2);
        cs.appendRawCommands("0 G\n");
        cs.appendRawCommands("1 w\n");
        cs.appendRawCommands("0.62353 0.63137 0.64314 RG\n185.43 167 m\n185.43 223 l\nS\n0 G\n");
        cs.restoreGraphicsState();

        cs.appendRawCommands("q 21 0 0 21 100.21 205 cm /img3 Do Q\n");
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 100.21 205 Tm\n");
        cs.appendRawCommands("21 0 Td\n-21 0 Td\n");
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 89.92 193 Tm\n");
        cs.setFont(fonts.regular, 9.12f);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("Contact Us");
        cs.setNonStrokingColor(0f);
        cs.appendRawCommands("1 0 0 1 83.96 181 Tm\n");
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("1800 266 0811");
        cs.setNonStrokingColor(0f);
        cs.appendRawCommands("1 0 0 1 63.77 169 Tm\n");
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("(local call charges apply)");
        cs.setNonStrokingColor(0f);
        cs.endText();

        cs.appendRawCommands("q 21 0 0 21 361.71 205 cm /img5 Do Q\n");
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 361.71 205 Tm\n");
        cs.appendRawCommands("21 0 Td\n-21 0 Td\n");
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 342.87 193 Tm\n");
        cs.setFont(fonts.regular, 9f);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("Branch Address");
        cs.setNonStrokingColor(0f);
        String branchAddress = stmt.details.branchAddress.replaceAll("\\s*\\R\\s*", " ");
        List<String> addr = wrapText(branchAddress, 100);
        float C_ADDR1 = 201.32f + (fonts.regular.getStringWidth("1st Floor, Shanti Point, Surat - Cloth Market (ring Road).,Surat, Surat-395002, Gujarat, India,")/1000f * 9f)/2;
        float C_ADDR2 = 349.85f + (fonts.regular.getStringWidth("9099091998")/1000f * 9f)/2;
        float addr1x = centerAlign(addr.get(0), C_ADDR1, fonts.regular, 9f);
        cs.appendRawCommands(String.format("1 0 0 1 %.2f 181 Tm\n", addr1x));
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText(addr.get(0));
        cs.setNonStrokingColor(0f);
        if(addr.size() > 1){
            float addr2x = centerAlign(addr.get(1), C_ADDR2, fonts.regular, 9f);
            cs.appendRawCommands(String.format("1 0 0 1 %.2f 169 Tm\n", addr2x));
            cs.setNonStrokingColor(0f, 0f, 0f);
            cs.showText(addr.get(1));
            cs.setNonStrokingColor(0f);
            cs.endText();
        } else {
            cs.endText();
        }

        // Rounded rectangle background
        cs.saveGraphicsState();
        cs.setNonStrokingColor(0.91765f, 0.92157f, 0.92549f);
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
        cs.saveGraphicsState();
        cs.restoreGraphicsState();
        cs.saveGraphicsState();
        cs.setLineCapStyle(2);
        cs.appendRawCommands("0 G\n");
        cs.setLineWidth(1f);
        cs.setStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.moveTo(384.67f, 95f);
        cs.lineTo(384.67f, 146f);
        cs.stroke();
        cs.appendRawCommands("0 G\n");
        cs.restoreGraphicsState();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 51 133 Tm\n");
        cs.setFont(fonts.semiBold, 12);
        cs.setNonStrokingColor(0.92941f, 0.1098f, 0.14118f);
        cs.showText("   Remember!");
        cs.setNonStrokingColor(0f);
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 51 115 Tm \n");
        cs.setFont(fonts.regular, 12);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("   Never share personal/sensitive information like PIN, CVV, OTP");
        cs.setNonStrokingColor(0f);
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 51 97 Tm\n");
        cs.setFont(fonts.regular, 12);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("   or passwords with anyone.");
        cs.setNonStrokingColor(0f);
        cs.endText();

        cs.appendRawCommands("q 45.6 0 0 45.6 399.84 98.4 cm /img6 Do Q\n");
        cs.beginText();
        cs.appendRawCommands("1 0 0 1 399.84 98.4 Tm\n");
        cs.appendRawCommands("45.6 0 Td\n-45.6 0 Td\n");
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 458.61 129 Tm\n");
        cs.setFont(fonts.regular, 10);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("Scan for");
        cs.setNonStrokingColor(0f);
        cs.appendRawCommands("1 0 0 1 458.61 119 Tm\n");
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("more safe");
        cs.setNonStrokingColor(0f);
        cs.appendRawCommands("1 0 0 1 458.61 109 Tm\n");
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("banking tips");
        cs.setNonStrokingColor(0f);
        cs.endText();

        cs.setStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.setLineWidth(1f);
        cs.moveTo(36f, 76f);
        cs.lineTo(559f, 76f);
        cs.stroke();
        cs.saveGraphicsState();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 168.66 61 Tm\n");
        cs.setFont(fonts.regular, 9.12f);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("Kotak Mahindra Bank Ltd. | CIN: L65110MH1985PLC038137");
        cs.setNonStrokingColor(0f);
        cs.endText();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 63.75 43 Tm\n");
        cs.setFont(fonts.regular, 9.12f);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.showText("Registered Office: 27 BKC, C 27, G Block, Bandra Kurla Complex, Bandra (E), Mumbai - 400 051. www.kotak.bank.in");
        cs.setNonStrokingColor(0f);
        cs.endText();
        cs.restoreGraphicsState();
    }

    private float centerAlign(String text, float center, PDFont font, float fontSize) throws IOException {
        float textWidth = (font.getStringWidth(text) / 1000f) * fontSize;
        return center - (textWidth / 2f);
    }

    // ----------------- LAST PAGE -----------------
    private final void drawLastPage(PageCursor pc, Fonts fonts) throws IOException {
        PDPageContentStream cs = pc.cs();

        cs.saveGraphicsState();
        cs.setStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.moveTo(46f, 342f);
        cs.lineTo(549f, 342f);
        cs.curveTo(554.52f, 342f, 559f, 346.48f, 559f, 352f);
        cs.lineTo(559f, 712f);
        cs.curveTo(559f, 717.52f, 554.52f, 722f, 549f, 722f);
        cs.lineTo(46f, 722f);
        cs.curveTo(40.48f, 722f, 36f, 717.52f, 36f, 712f);
        cs.lineTo(36f, 352f);
        cs.curveTo(36f, 346.48f, 40.48f, 342f, 46f, 342f);
        cs.fillAndStroke();
        cs.restoreGraphicsState();

        cs.beginText();
        cs.appendRawCommands("36 762 Td\n");
        cs.appendRawCommands("0 -25 Td\n");
        cs.appendRawCommands("0 -16 Td\n");
        cs.setFont(fonts.helvetica, 12);
        cs.showText(" ");
        cs.appendRawCommands("0 0 Td\n");
        cs.appendRawCommands("0 -16 Td\n");
        cs.appendRawCommands("0 0 Td\n");
        cs.appendRawCommands("0 -343.04 Td\n");
        cs.appendRawCommands("0 -16 Td\n");
        cs.setFont(fonts.helvetica, 12);
        cs.showText(" ");
        cs.appendRawCommands("0 0 Td\n");
        cs.appendRawCommands("0 -16 Td\n");
        cs.showText(" ");
        cs.appendRawCommands("0 0 Td\n");
        cs.appendRawCommands("0 -16 Td\n");
        cs.showText(" ");
        cs.appendRawCommands("0 0 Td\n");
        cs.appendRawCommands("0 -16 Td\n");
        cs.appendRawCommands("0 0 Td\n");
        cs.appendRawCommands("0 -25 Td\n");
        cs.appendRawCommands("0 -177.84 Td\n");
        cs.endText();

        cs.saveGraphicsState();
        cs.setNonStrokingColor(0.92941f, 0.1098f, 0.14118f);
        cs.addRect(36, 737, 523, 25);
        cs.fill();
        cs.restoreGraphicsState();

        cs.beginText();
        cs.appendRawCommands("1 0 0 1 240.74 744.5 Tm\n");
        cs.setFont(fonts.regular, 12);
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.showText("Important Information");
        cs.appendRawCommands("0 g\n");
        cs.endText();

        float textY = 692.32f;
        float[] noticesY = {
                691f, 665.64f, 640.28f, 599.56f, 551.16f, 502.76f,
                477.4f, 421.32f, 395.96f, 371.96f,
        };

        for (int i = 0; i < notices.length; i++) {
            cs.appendRawCommands(String.format(Locale.US, "q 4 0 0 4 49.53 %.2f cm /img8 Do Q\n", noticesY[i]));
            cs.beginText();
            cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 68.53 %.2f Tm\n", noticesY[i]));
            cs.appendRawCommands("4 0 Td\n");
            cs.appendRawCommands("-4 0 Td\n");
            cs.endText();

            if (i == 4) {
                cs.setStrokingColor(0f, 0f, 0f);
                cs.setLineWidth(0.5f);
                cs.moveTo(352.53f, 536.12f);
                cs.lineTo(380.7f, 536.12f);
                cs.stroke();
                cs.setStrokingColor(0f);
                cs.setLineWidth(1f);
                cs.setStrokingColor(0f, 0f, 0f);
                cs.setLineWidth(0.5f);
                cs.moveTo(348.39f, 528.44f);
                cs.lineTo(410.68f, 528.44f);
                cs.stroke();
                cs.setStrokingColor(0f);
                cs.setLineWidth(1f);
                cs.setStrokingColor(0f, 0f, 0f);
                cs.setLineWidth(0.5f);
                cs.moveTo(439.81f, 528.44f);
                cs.lineTo(492.2f, 528.44f);
                cs.stroke();
                cs.setStrokingColor(0f);
                cs.setLineWidth(1f);
            }

            String[] lines = notices[i].split("\\n+\\s*");
            textY = noticesY[i] + 1.32f;
            cs.beginText();
            for (int j = 0; j < lines.length; j++) {
                String line = lines[j];
                cs.appendRawCommands(String.format(Locale.US, "1 0 0 1 68.53 %.2f Tm\n", textY));
                if (j == 0) cs.setFont(fonts.regular, 7.68f);
                cs.setNonStrokingColor(0f, 0f, 0f);
                if (i == 4 && j == 2) {
                    String[] l = line.split("Alert List");
                    cs.showText(l[0]);
                    cs.setNonStrokingColor(0f);
                    cs.setNonStrokingColor(0f, 0f, 0f);
                    cs.showText("Alert List");
                    cs.setNonStrokingColor(0f);
                    cs.setNonStrokingColor(0f, 0f, 0f);
                    cs.showText(l[1]);
                } else if (i == 4 && j == 3) {
                    String text = line;
                    String marker1 = "Authorised Persons";
                    String marker2 = "Authorised ETPs";
                    List<String> parts = new ArrayList<>();
                    int i1 = text.indexOf(marker1);
                    int i2 = text.indexOf(marker2);
                    if (i1 != -1 && i2 != -1 && i2 > i1) {
                        parts.add(text.substring(0, i1).trim());
                        parts.add(text.substring(i1 + marker1.length(), i2).trim());
                        parts.add(text.substring(i2 + marker2.length()).trim());
                    } else {
                        parts.add(text);
                    }
                    cs.showText(parts.get(0));
                    cs.setNonStrokingColor(0f);
                    cs.setNonStrokingColor(0f, 0f, 0f);
                    cs.showText(marker1);
                    cs.setNonStrokingColor(0f);
                    cs.setNonStrokingColor(0f, 0f, 0f);
                    cs.showText(parts.get(1));
                    cs.setNonStrokingColor(0f);
                    cs.setNonStrokingColor(0f, 0f, 0f);
                    cs.showText(marker2);
                    cs.setNonStrokingColor(0f);
                    cs.setNonStrokingColor(0f, 0f, 0f);
                    cs.showText(parts.get(2));
                } else {
                    cs.showText(line);
                }
                cs.setNonStrokingColor(0f);
                textY -= LINE_H;
            }
            cs.endText();
        }

        cs.saveGraphicsState();
        cs.setNonStrokingColor(0.92941f, 0.1098f, 0.14118f);
        cs.addRect(36, 272.96f, 523, 25);
        cs.fill();
        cs.restoreGraphicsState();

        cs.beginText();
        cs.setFont(fonts.regular, 12);
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.appendRawCommands("1 0 0 1 228.27 280.46 Tm\n");
        cs.showText("Commonly Used Narrations");
        cs.appendRawCommands("0 g\n");
        cs.endText();

        // Narrative table
        cs.saveGraphicsState();
        cs.setLineCapStyle(2);
        cs.setStrokingColor(0f);
        cs.setLineWidth(0.5f);
        cs.setStrokingColor(0.62353f, 0.63137f, 0.64314f);
        float rowY = 259.28f;
        for (int i = 0; i < 13; i++) {
            cs.addRect(36f, rowY, 261.5f, 13.68f);
            cs.stroke();
            cs.addRect(297.5f, rowY, 261.5f, 13.68f);
            cs.stroke();
            rowY -= 13.68f;
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

        double textY2 = 262.28;
        for (String[] pair : transactionCodes) {
            cs.beginText();
            cs.setTextMatrix(1, 0, 0, 1, 39f, textY2);
            cs.setFont(fonts.regular, 7.68f);
            cs.setNonStrokingColor(0f, 0f, 0f);
            cs.showText(pair[0]);
            cs.setNonStrokingColor(0f);
            cs.endText();
            cs.beginText();
            cs.setTextMatrix(1, 0, 0, 1, 300.5f, textY2);
            cs.setFont(fonts.regular, 7.68f);
            cs.setNonStrokingColor(0f, 0f, 0f);
            cs.showText(pair[1]);
            cs.setNonStrokingColor(0f);
            cs.endText();
            textY2 -= 13.68f;
        }
    }

    private final void drawHeaderAndFooter(PDDocument doc, Fonts fonts) throws IOException {
        int totalPages = doc.getNumberOfPages();
        for (int i = 0; i < totalPages; i++) {
            PDPage page = doc.getPage(i);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                if (i > 0) {
                    drawHeader(cs, fonts);
                }
                drawFooter(cs, fonts, i + 1, totalPages);
            }
        }
    }

    private void drawHeader(PDPageContentStream cs, Fonts fonts) throws IOException {
        cs.beginText();
        cs.setFont(fonts.regular, 9);
        cs.appendRawCommands("1 0 0 1 36 821 Tm\n");
        cs.showText(stmt.details.name.toUpperCase());
        cs.endText();
        cs.beginText();
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.appendRawCommands("1 0 0 1 36 809 Tm\n");
        cs.showText("Account No. ");
        cs.endText();
        cs.beginText();
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.appendRawCommands("1 0 0 1 84 809 Tm\n");
        cs.showText(stmt.details.accountNumber);
        cs.endText();
        cs.beginText();
        cs.setFont(fonts.regular, 9);
        cs.setNonStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.appendRawCommands("1 0 0 1 36 797 Tm\n");
        cs.showText("Account Statement ");
        cs.endText();
        String startDate = TemplateUtils.formatDateDMMMYYYY(stmt.meta.statementPeriodStart);
        String endDate = TemplateUtils.formatDateDMMMYYYY(stmt.meta.statementPeriodEnd);
        cs.beginText();
        cs.setFont(fonts.semiBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.appendRawCommands("1 0 0 1 111 797 Tm\n");
        cs.showText(startDate + " - " + endDate);
        cs.endText();
    }

    private void drawFooter(PDPageContentStream cs, Fonts fonts, int page, int totalPages) throws IOException {
        String date = TemplateUtils.formatDateTimeDMMMYYYYHHMM(stmt.meta.generatedAt);
        cs.beginText();
        cs.setFont(fonts.regular, 9.6f);
        cs.setNonStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.appendRawCommands("1 0 0 1 36 16 Tm\n");
        cs.showText("Statement Generated on " + date);
        cs.endText();
        cs.beginText();
        cs.setFont(fonts.regular, 10);
        cs.setNonStrokingColor(0.62353f, 0.63137f, 0.64314f);
        cs.appendRawCommands("1 0 0 1 518.99 16 Tm\n");
        cs.showText("Page " + page + " of " + totalPages);
        cs.endText();
    }
}