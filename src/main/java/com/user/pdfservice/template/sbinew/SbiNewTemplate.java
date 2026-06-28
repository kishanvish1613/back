package com.user.pdfservice.template.sbinew;

import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.Transaction;
import com.user.pdfservice.template.BankPdfTemplate;
import com.user.pdfservice.template.sbinew.XObjectFactory.RawFormDef;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

@SuppressWarnings("deprecation")
public class SbiNewTemplate implements BankPdfTemplate {

    private Map<String, BufferedImage> extractedImages = new LinkedHashMap<>();
    private final PDType1Font font = PDType1Font.HELVETICA;
    private Statement stmt;
    private PageCursor pc;
    private PDPageContentStream cs;

    private Map<String, PDImageXObject> cachedXObjects = new HashMap<>();

    private static final float LINE_GAP = 9.31f;
    private static final float TEXT_TOP_PADDING = 10.5f;

    private static final float X_DATE = 27.48f;
    private static final float X_POST_DATE = 82.48f;
    private static final float X_DETAILS = 138f;

    static final float X_REF_COL = 280f;
    static final float W_REF_COL = 50f;

    private static final float X_DEBIT_COL = 330f;
    private static final float W_DEBIT_COL = 80f;

    private static final float X_CREDIT_COL = 410f;
    private static final float W_CREDIT_COL = 80f;

    private static final float X_BALANCE_COL = 490f;
    private static final float W_BALANCE_COL = 85f;

    private static final float PAGE1_TXN_Y = 315f;
    private static final float PAGE_TXN_Y = 785f;

    private static final int MAX_NARRATION_LINES = 6;

    record Column(float x, float width) {}

    static final Column[] COLS = {
            new Column(20f, 55f),
            new Column(75f, 55f),
            new Column(130f, 150f),
            new Column(280f, 50f),
            new Column(330f, 80f),
            new Column(410f, 80f),
            new Column(490f, 85f),
    };

    @Override
    public byte[] generate(Statement statement) {
        this.stmt = statement;

        // ✅ LOCAL page counter – always starts at 1
        final int[] pageNo = {1};

        try (PDDocument doc = new PDDocument()) {
            if (stmt.meta.password == null) {
                throw new Error("No Password Provided");
            }

            PDDocumentInformation info = new PDDocumentInformation();
            info.setProducer("OpenPDF 1.3.32");
            info.setCreator("JasperReports Library version 6.21.2-8434a0bd7c3bbc37cbf916f2968d35e4b165821a");
            Calendar now = Calendar.getInstance();
            info.setCreationDate(now);
            doc.setDocumentInformation(info);

            COSDictionary catalogDict = doc.getDocumentCatalog().getCOSObject();
            catalogDict.setName("Trapped", "False");

            PDDocument original = PDDocument.load(new File("sbinew.pdf"));
            extractImagesFromPage(original, 2, extractedImages);
            for (Map.Entry<String, BufferedImage> entry : extractedImages.entrySet()) {
                PDImageXObject ximg = LosslessFactory.createFromImage(doc, entry.getValue());
                cachedXObjects.put(entry.getKey(), ximg);
            }
            original.close();

            PDRectangle A4_EXACT = new PDRectangle(595f, 842f);
            PDPage page = new PDPage(A4_EXACT);
            doc.addPage(page);

            PDResources pageRes = page.getResources();
            if (pageRes == null) {
                pageRes = new PDResources();
                page.setResources(pageRes);
            }
            pageRes.put(COSName.getPDFName("F1"), font);

            attachPage1FormXObjects(doc, page);

            pc = new PageCursor(doc, font, page, 0f, cachedXObjects);
            cs = pc.cs();

            String template = load("/sbinewstreams/headerinfo.pdfops");
            String branchCode = stmt.details.ifsc.substring(6);

            float name_right_edge = 485.18f + (font.getStringWidth("Ved Prakash Patel") / 1000f) * 10f;
            float name_x = AlignXtoRight(stmt.details.name, font, name_right_edge, 10f);

            String[] baddr = stmt.details.branchAddress.toUpperCase().split("\n");
            String[] addr = stmt.details.address.split("\n");
            String addrfont = addr.length == 3 ? "9" : "10";
            StringBuilder sb = new StringBuilder(
                    "1 0 0 1 67 680.65 Tm\n" +
                            "/F1 " + addrfont + " Tf\n" +
                            "0 0 0 rg\n" +
                            "(" + pdfEscapeLiteral(addr[0]) + ")Tj\n" +
                            "0 g\n");
            float addr_y = 670.17f;
            for (int i = 1; i < addr.length; i++) {
                sb.append(String.format(Locale.ROOT,
                        "1 0 0 1 67 %.2f Tm\n0 0 0 rg\n(%s)Tj\n0 g\n",
                        addr_y, pdfEscapeLiteral(addr[i])));
                addr_y -= 10.48f;
            }

            Map<String, Object> fields = Map.ofEntries(
                    Map.entry("NAME", stmt.details.name),
                    Map.entry("NAME_X", name_x),
                    Map.entry("ACCOUNT_NO", stmt.details.accountNumber),
                    Map.entry("IFSC", stmt.details.ifsc),
                    Map.entry("MICR", stmt.details.micr),
                    Map.entry("CIF", stmt.details.customerRelNo),
                    Map.entry("BRANCH_NAME", stmt.details.branch),
                    Map.entry("BRANCH_PHONE", stmt.details.branchPhoneNo),
                    Map.entry("BRANCH_CODE", branchCode),
                    Map.entry("OPENING_BAL", formatIndianAmount(stmt.details.startingBalance) + "CR"),
                    Map.entry("BADDRFONT", baddr.length == 3 ? "8.5" : "10"),
                    Map.entry("EMAIL", stmt.details.email == null ? "test@gmail.com" : stmt.details.email),
                    Map.entry("ADDRSTREAM", sb.toString()),
                    Map.entry("BADDRL1", baddr[0]),
                    Map.entry("BADDRL2", baddr[1]),
                    Map.entry("BADDRL3", 2 < baddr.length ? baddr[2] : ""),
                    Map.entry("END_DATE", formatIsoInstantDate(stmt.meta.statementPeriodEnd)),
                    Map.entry("START_DATE", formatIsoInstantDate(stmt.meta.statementPeriodStart)),
                    Map.entry("CKYCR", stmt.details.ckycr != null ? stmt.details.ckycr : "Not Available"),
                    Map.entry("ACCOUNT_TYPE", stmt.details.accountType != null ? stmt.details.accountType : "Savings Account"),
                    Map.entry("GEN_DATE", formatCurrentDate())
            );

            String rendered = renderPdfOps(template, fields);
            cs.appendRawCommands(rendered + "\n");

            drawTableHeader(pc, true);

            float y = 315f;
            for (Transaction tx : stmt.transactions) {
                String dr = tx.debit == 0 ? "-" : formatIndianAmount(tx.debit);
                String cr = tx.credit == 0 ? "-" : formatIndianAmount(tx.credit);
                double safeBalance = Math.max(0.0, tx.balance);
                y = drawTransactionRow(
                        y,
                        formatIsoInstantDate2(tx.date),
                        buildNarrationLines(tx.description),
                        dr,
                        cr,
                        formatIndianAmount(safeBalance),
                        pageNo      // pass counter
                );
            }
            y = drawTableFooterAt(y);
            y -= 47f;
            drawStatementSummaryAt(pc, y, pageNo);
            drawPageNo(pageNo);          // first page number
            cs.close();

            doc.getDocumentCatalog().setViewerPreferences(null);

            // Encryption
            doc.getDocument().setVersion(1.5f);
            AccessPermission ap = new AccessPermission();
            ap.setCanPrint(true);
            ap.setCanExtractContent(true);
            ap.setCanModify(false);
            StandardProtectionPolicy policy = new StandardProtectionPolicy(
                    stmt.meta.password, stmt.meta.password, ap);
            policy.setEncryptionKeyLength(128);
            policy.setPreferAES(true);
            doc.protect(policy);
            doc.getDocumentCatalog().getCOSObject().setName(COSName.VERSION, "1.5");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    // ---------- Helpers that use pageNo ----------

    private void drawPageNo(int[] pageNo) throws IOException {
        String stream = """
                1 0 0 1 0 0 cm
                BT
                1 0 0 1 313 78.72 Tm
                /F1 10 Tf
                0 0 0 rg
                (%d)Tj
                0 g
                ET
                1 0 0 1 0 0 cm
                [] 0 d
                2 J
                1 0 0 1 0 0 cm
                BT
                1 0 0 1 269 78.72 Tm
                /F1 10 Tf
                0 0 0 rg
                (Page no.)Tj
                0 g
                ET
                1 0 0 1 0 0 cm
                [] 0 d
                2 J
                """;
        cs.appendRawCommands(String.format(Locale.ROOT, stream, pageNo[0]++) + "\n");
    }

    private float drawTransactionRow(float rowTopY, String date, List<String> narrationLines,
                                     String debit, String credit, String balance,
                                     int[] pageNo) throws Exception {
        int lineCount = Math.min(narrationLines.size(), 6);
        float rowHeight = rowHeightForLines(lineCount);
        float rowBottomY = rowTopY - rowHeight;

        if (rowBottomY < 124f) {
            drawTableFooterAt(rowTopY);
            drawPageNo(pageNo);                     // draw current page number before new page
            pc.nextPage();
            cs = pc.cs();
            cs.appendRawCommands("""
                    q
                    BT
                    36 806 Td
                    ET
                    Q
                    2 J
                    BT
                    1 0 0 1 0 842 Tm
                    /F1 10 Tf
                    ()Tj
                    ET
                    """);
            drawTableHeader(pc, false);
            rowTopY = PAGE_TXN_Y;
            rowBottomY = rowTopY - rowHeight;
        }

        drawCell(20f, rowTopY, 55f, rowHeight, true);
        drawText(X_DATE, rowTopY - 18f, date);
        drawCell(75f, rowTopY, 55f, rowHeight, true);
        drawText(X_POST_DATE, rowTopY - 18f, date);
        drawCell(130f, rowTopY, 150f, rowHeight, true);

        float firstLineY;
        if (lineCount <= 2) {
            float textBlockHeight = (lineCount - 1) * LINE_GAP;
            float centerY = rowTopY - (rowHeight / 2f);
            firstLineY = centerY + (textBlockHeight / 2f) - 3.6f;
        } else {
            firstLineY = rowTopY - TEXT_TOP_PADDING;
        }
        for (int i = 0; i < lineCount; i++) {
            drawText(X_DETAILS, firstLineY - (i * LINE_GAP), narrationLines.get(i));
        }

        drawCell(X_REF_COL, rowTopY, W_REF_COL, rowHeight, true);
        drawText(centerAlignedX("-", X_REF_COL, W_REF_COL, 8f), rowTopY - 17f, "-");

        drawCell(X_DEBIT_COL, rowTopY, W_DEBIT_COL, rowHeight, true);
        String debitText = (debit == null || debit.isBlank()) ? "-" : debit;
        drawText(centerAlignedX(debitText, X_DEBIT_COL, W_DEBIT_COL, 8f), rowTopY - 17f, debitText);

        drawCell(X_CREDIT_COL, rowTopY, W_CREDIT_COL, rowHeight, true);
        String creditText = (credit == null || credit.isBlank()) ? "-" : credit;
        drawText(centerAlignedX(creditText, X_CREDIT_COL, W_CREDIT_COL, 8f), rowTopY - 17f, creditText);

        drawCell(X_BALANCE_COL, rowTopY, W_BALANCE_COL, rowHeight, true);
        drawText(centerAlignedX(balance, X_BALANCE_COL, W_BALANCE_COL, 8f), rowTopY - 17f, balance);

        return rowBottomY;
    }

    private float drawStatementSummaryAt(PageCursor pc, float topY, int[] pageNo) throws Exception {
        float summaryHeight = 136.43f;
        if (topY - summaryHeight < 124f) {
            drawPageNo(pageNo);                     // draw current page number before new page
            pc.nextPage();
            cs = pc.cs();
            topY = 822f;
        }
        PDPage page = pc.page();
        PDDocument doc = pc.doc();
        var first = cachedXObjects.entrySet().iterator().next();
        PDImageXObject ximg = first.getValue();
        page.getResources().put(COSName.getPDFName(first.getKey()), ximg);

        final float BASE_Y = 348f;
        final float BLOCK_HEIGHT = 87f;
        float deltaY = topY - BASE_Y;
        float bottomY = topY - BLOCK_HEIGHT;

        float BF_X_COL = 20f, BF_W_COL = 99.5f;
        float DR_X_COL = 119.5f, DR_W_COL = 58f;
        float CR_X_COL = 177.5f, CR_W_COL = 52f;
        float TD_X_COL = 229.5f, TD_W_COL = 101f;
        float TC_X_COL = 330.5f, TC_W_COL = 100f;
        float CB_X_COL = 430.5f, CB_W_COL = 144.5f;

        int drCount = 0, crCount = 0;
        double totalDebits = 0.0, totalCredits = 0.0;
        for (Transaction tx : stmt.transactions) {
            if (tx.debit > 0) { drCount++; totalDebits += tx.debit; }
            if (tx.credit > 0) { crCount++; totalCredits += tx.credit; }
        }

        String bfText = formatIndianAmount(stmt.details.startingBalance) + "CR";
        String drText = String.valueOf(drCount);
        String crText = String.valueOf(crCount);
        String tdText = formatIndianAmount(totalDebits);
        String tcText = formatIndianAmount(totalCredits);
        Transaction lastTx = stmt.transactions.get(stmt.transactions.size() - 1);
        String closingText = formatIndianAmount(Math.max(0.0, lastTx.balance)) + "CR";

        Map<String, Object> sumFields = Map.ofEntries(
                Map.entry("BROUGHT_FORWARD", bfText),
                Map.entry("BF_X", centerAlignedX(bfText, BF_X_COL, BF_W_COL, 10f)),
                Map.entry("DR_COUNT", drText),
                Map.entry("DR_X", centerAlignedX(drText, DR_X_COL, DR_W_COL, 10f)),
                Map.entry("CR_COUNT", crText),
                Map.entry("CR_X", centerAlignedX(crText, CR_X_COL, CR_W_COL, 10f)),
                Map.entry("TOTAL_DEBITS", tdText),
                Map.entry("TD_X", centerAlignedX(tdText, TD_X_COL, TD_W_COL, 10f)),
                Map.entry("TOTAL_CREDITS", tcText),
                Map.entry("TC_X", centerAlignedX(tcText, TC_X_COL, TC_W_COL, 10f)),
                Map.entry("CLOSING_BAL", closingText),
                Map.entry("CB_X", centerAlignedX(closingText, CB_X_COL, CB_W_COL, 10f)),
                Map.entry("SUMMARY_RANGE",
                        formatIsoInstantDate(stmt.meta.statementPeriodStart) +
                                " To " + formatIsoInstantDate(stmt.meta.statementPeriodEnd))
        );

        String summaryTemplate = load("/sbinewstreams/summary.pdfops");
        String rendered = renderPdfOps(summaryTemplate, sumFields);
        cs.appendRawCommands(String.format(Locale.ROOT,
                "q\n1 0 0 1 0 %.2f cm\n%s\nQ\n", deltaY, rendered));
        return bottomY;
    }

    // ---------- All other methods unchanged ----------

    private static String formatCurrentDate() {
        return DateTimeFormatter.ofPattern("dd-MM-yyyy")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
    }

    private float drawTableFooterAt(float rowTopY) throws IOException {
        float baseY = 124f;
        float deltaY = rowTopY - baseY;
        float footerHeight = 30.25f;
        float footerBottomY = rowTopY - footerHeight;
        cs.appendRawCommands(String.format(Locale.ROOT,
                "q\n1 0 0 1 0 %.2f cm\n%s\nQ\n",
                deltaY, load("/sbinewstreams/tablefooter.pdfops")));
        return footerBottomY;
    }

    private List<String> buildNarrationLines(String narration) {
        if (narration == null || narration.isBlank()) return List.of();
        List<String> lines = new ArrayList<>();
        String[] hardLines = narration.strip().split("\\r?\\n");
        for (String hardLine : hardLines) {
            hardLine = hardLine.trim().replaceAll("[ \\t]+", " ");
            List<String> wrapped;
            try {
                wrapped = wrapTextToWidth(hardLine, font, 8f, 150f);
            } catch (IOException e) {
                wrapped = List.of(hardLine.split(" "));
            }
            for (String line : wrapped) {
                if (lines.size() >= MAX_NARRATION_LINES) break;
                lines.add(line);
            }
        }
        while (lines.size() > MAX_NARRATION_LINES) lines.remove(lines.size() - 1);
        return lines;
    }

    private static List<String> wrapTextToWidth(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> result = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float width = (font.getStringWidth(testLine) / 1000f) * fontSize;
            if (width > maxWidth && currentLine.length() > 0) {
                result.add(currentLine.toString());
                currentLine.setLength(0);
                currentLine.append(word);
            } else {
                currentLine.setLength(0);
                currentLine.append(testLine);
            }
        }
        if (currentLine.length() > 0) result.add(currentLine.toString());
        return result;
    }

    private void drawCell(float x, float topY, float width, float height, boolean drawBottom) throws IOException {
        float bottomY = topY - height;
        cs.appendRawCommands(String.format(Locale.ROOT,
                "1 1 1 rg\n%.2f %.2f %.2f -%.2f re\nf\n", x, topY, width, height));
        cs.appendRawCommands(String.format(Locale.ROOT,
                """
                0.5 w 0 J 0.72157 0.72157 0.72157 RG [] 0 d
                %.2f %.2f m %.2f %.2f l S
                %.2f %.2f m %.2f %.2f l S
                %s
                %.2f %.2f m %.2f %.2f l S
                """,
                x - 0.25f, topY, x + width + 0.25f, topY,
                x, topY + 0.25f, x, bottomY - 0.25f,
                drawBottom ? String.format(Locale.ROOT,
                        "%.2f %.2f m %.2f %.2f l S\n",
                        x - 0.25f, bottomY, x + width + 0.25f, bottomY) : "",
                x + width, topY + 0.25f, x + width, bottomY - 0.25f));
    }

    private void drawText(float x, float y, String text) throws IOException {
        cs.appendRawCommands(String.format(Locale.ROOT,
                "BT\n1 0 0 1 %.2f %.2f Tm\n/F1 8 Tf\n0 0 0 rg\n(%s)Tj\nET\n",
                x, y, pdfEscapeLiteral(text)));
    }

    private void drawTableHeader(PageCursor pc, boolean firstPage) throws IOException {
        if (firstPage) {
            String template = load("/sbinewstreams/tableheader1.pdfops");
            cs.appendRawCommands(template + "\n");
        } else {
            PDPage page = pc.page();
            PDDocument doc = pc.doc();

            List<RawFormDef> defs = List.of(
                    new RawFormDef("Xf24", new PDRectangle(0, 0, 43, 8), load("/sbinewstreams/xf24.pdfops")),
                    new RawFormDef("Xf23", new PDRectangle(0, 0, 49, 8), load("/sbinewstreams/xf23.pdfops")),
                    new RawFormDef("Xf18", new PDRectangle(0, 0, 34, 37), load("/sbinewstreams/xf18.pdfops")),
                    new RawFormDef("Xf27", new PDRectangle(0, 0, 35, 8), load("/sbinewstreams/xf27.pdfops")),
                    new RawFormDef("Xf26", new PDRectangle(0, 0, 32, 8), load("/sbinewstreams/xf26.pdfops")),
                    new RawFormDef("Xf25", new PDRectangle(0, 0, 31, 8), load("/sbinewstreams/xf25.pdfops"))
            );
            List<RawFormDef> ch = List.of(
                    new RawFormDef("Xf12", new PDRectangle(0, 0, 49, 8), load("/sbinewstreams/xf12.pdfops")),
                    new RawFormDef("Xf21", new PDRectangle(0, 0, 35, 8), load("/sbinewstreams/xf21.pdfops")),
                    new RawFormDef("Xf19", new PDRectangle(0, 0, 32, 8), load("/sbinewstreams/xf19.pdfops")),
                    new RawFormDef("Xf16", new PDRectangle(0, 0, 31, 8), load("/sbinewstreams/xf16.pdfops")),
                    new RawFormDef("Xf14", new PDRectangle(0, 0, 43, 8), load("/sbinewstreams/xf14.pdfops"))
            );

            Map<String, PDFormXObject> forms = XObjectFactory.createRawForms(doc, defs);
            Map<String, PDFormXObject> childs = XObjectFactory.createRawForms(doc, ch);

            PDFormXObject xfparent = forms.get("Xf24");
            PDFormXObject xfchild = childs.get("Xf14");
            PDResources res = xfparent.getResources();
            if (res == null) { res = new PDResources(); xfparent.setResources(res); }
            res.put(COSName.getPDFName("Xf14"), xfchild);

            xfparent = forms.get("Xf23");
            xfchild = childs.get("Xf12");
            res = xfparent.getResources();
            if (res == null) { res = new PDResources(); xfparent.setResources(res); }
            res.put(COSName.getPDFName("Xf12"), xfchild);

            xfparent = forms.get("Xf27");
            xfchild = childs.get("Xf21");
            res = xfparent.getResources();
            if (res == null) { res = new PDResources(); xfparent.setResources(res); }
            res.put(COSName.getPDFName("Xf21"), xfchild);

            xfparent = forms.get("Xf26");
            xfchild = childs.get("Xf19");
            res = xfparent.getResources();
            if (res == null) { res = new PDResources(); xfparent.setResources(res); }
            res.put(COSName.getPDFName("Xf19"), xfchild);

            xfparent = forms.get("Xf25");
            xfchild = childs.get("Xf16");
            res = xfparent.getResources();
            if (res == null) { res = new PDResources(); xfparent.setResources(res); }
            res.put(COSName.getPDFName("Xf16"), xfchild);

            XObjectFactory.attachFormsToPage(page, forms);
            String template = load("/sbinewstreams/tableheader2.pdfops");
            cs.appendRawCommands(template + "\n");
        }
    }

    private static float AlignXtoRight(String text, PDType1Font font, float rightEdge, float fontSize) throws IOException {
        float textWidth = (font.getStringWidth(text) / 1000f) * fontSize;
        return rightEdge - textWidth;
    }

    private float centerAlignedX(String text, float colX, float colWidth, float fontSize) throws IOException {
        float textWidth = (font.getStringWidth(text) / 1000f) * fontSize;
        return colX + (colWidth - textWidth) / 2f;
    }

    private static String formatIndianAmount(double value) {
        boolean negative = value < 0;
        value = Math.abs(value);
        String s = String.format(Locale.ROOT, "%.2f", value);
        int dot = s.indexOf('.');
        String intPart = dot >= 0 ? s.substring(0, dot) : s;
        String fracPart = dot >= 0 ? s.substring(dot) : ".00";
        String grouped = groupIndian(intPart);
        return (negative ? "-" : "") + grouped + fracPart;
    }

    private static String groupIndian(String digits) {
        int len = digits.length();
        if (len <= 3) return digits;
        String last3 = digits.substring(len - 3);
        String rest = digits.substring(0, len - 3);
        StringBuilder sb = new StringBuilder();
        int i = rest.length();
        while (i > 0) {
            int start = Math.max(0, i - 2);
            if (sb.length() > 0) sb.insert(0, ',');
            sb.insert(0, rest.substring(start, i));
            i = start;
        }
        sb.append(',').append(last3);
        return sb.toString();
    }

    private static String formatIsoInstantDate(String isoInstant) {
        return DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ROOT).withZone(ZoneOffset.UTC).format(Instant.parse(isoInstant));
    }

    private static String formatIsoInstantDate2(String isoInstant) {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT).withZone(ZoneOffset.UTC).format(Instant.parse(isoInstant));
    }

    private static String renderPdfOps(String template, Map<String, ?> values) {
        String result = template;
        for (var e : values.entrySet()) {
            String key = "{{" + e.getKey() + "}}";
            Object v = e.getValue();
            if (!result.contains(key)) throw new IllegalArgumentException("Placeholder not found: " + key);
            String replacement;
            if (v instanceof Number n) {
                replacement = String.format(Locale.ROOT, "%.2f", n.doubleValue());
            } else if ("ADDRSTREAM".equals(e.getKey())) {
                replacement = v.toString();
            } else {
                replacement = pdfEscapeLiteral(v.toString());
            }
            result = result.replace(key, replacement);
        }
        if (result.contains("{{")) throw new IllegalStateException("Unresolved placeholders in PDF ops");
        return result;
    }

    private static String pdfEscapeLiteral(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '(' -> sb.append("\\(");
                case ')' -> sb.append("\\)");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String load(String path) throws IOException {
        try (var is = SbiNewTemplate.class.getResourceAsStream(path)) {
            if (is == null) throw new IOException("Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.US_ASCII);
        }
    }

    private void attachPage1FormXObjects(PDDocument doc, PDPage page) throws IOException {
        List<RawFormDef> defs = List.of(
                new RawFormDef("Xf3", new PDRectangle(0, 0, 16, 19), load("/sbinewstreams/xf3.pdfops")),
                new RawFormDef("Xf13", new PDRectangle(0, 0, 49, 8), load("/sbinewstreams/xf13.pdfops")),
                new RawFormDef("Xf4", new PDRectangle(0, 0, 16, 16), load("/sbinewstreams/xf4.pdfops")),
                new RawFormDef("Xf1", new PDRectangle(0, 0, 20, 20), load("/sbinewstreams/xf1.pdfops")),
                new RawFormDef("Xf11", new PDRectangle(0, 0, 93, 32), load("/sbinewstreams/xf11.pdfops")),
                new RawFormDef("Xf22", new PDRectangle(0, 0, 35, 8), load("/sbinewstreams/xf22.pdfops")),
                new RawFormDef("Xf2", new PDRectangle(0, 0, 20, 20), load("/sbinewstreams/xf2.pdfops")),
                new RawFormDef("Xf10", new PDRectangle(0, 0, 20, 20), load("/sbinewstreams/xf10.pdfops")),
                new RawFormDef("Xf20", new PDRectangle(0, 0, 32, 8), load("/sbinewstreams/xf20.pdfops")),
                new RawFormDef("Xf9", new PDRectangle(0, 0, 13, 15), load("/sbinewstreams/xf9.pdfops")),
                new RawFormDef("Xf7", new PDRectangle(0, 0, 18, 13), load("/sbinewstreams/xf7.pdfops")),
                new RawFormDef("Xf8", new PDRectangle(0, 0, 117, 23), load("/sbinewstreams/xf8.pdfops")),
                new RawFormDef("Xf5", new PDRectangle(0, 0, 16, 16), load("/sbinewstreams/xf5.pdfops")),
                new RawFormDef("Xf6", new PDRectangle(0, 0, 14, 14), load("/sbinewstreams/xf6.pdfops")),
                new RawFormDef("Xf18", new PDRectangle(0, 0, 34, 37), load("/sbinewstreams/xf18.pdfops")),
                new RawFormDef("Xf17", new PDRectangle(0, 0, 31, 8), load("/sbinewstreams/xf17.pdfops")),
                new RawFormDef("Xf15", new PDRectangle(0, 0, 43, 8), load("/sbinewstreams/xf15.pdfops"))
        );
        List<RawFormDef> ch = List.of(
                new RawFormDef("Xf12", new PDRectangle(0, 0, 49, 8), load("/sbinewstreams/xf12.pdfops")),
                new RawFormDef("Xf21", new PDRectangle(0, 0, 35, 8), load("/sbinewstreams/xf21.pdfops")),
                new RawFormDef("Xf19", new PDRectangle(0, 0, 32, 8), load("/sbinewstreams/xf19.pdfops")),
                new RawFormDef("Xf16", new PDRectangle(0, 0, 31, 8), load("/sbinewstreams/xf16.pdfops")),
                new RawFormDef("Xf14", new PDRectangle(0, 0, 43, 8), load("/sbinewstreams/xf14.pdfops"))
        );

        Map<String, PDFormXObject> forms = XObjectFactory.createRawForms(doc, defs);
        Map<String, PDFormXObject> childs = XObjectFactory.createRawForms(doc, ch);

        PDFormXObject xfparent = forms.get("Xf13");
        PDFormXObject xfchild = childs.get("Xf12");
        PDResources res = xfparent.getResources();
        if (res == null) { res = new PDResources(); xfparent.setResources(res); }
        res.put(COSName.getPDFName("Xf12"), xfchild);

        xfparent = forms.get("Xf22");
        xfchild = childs.get("Xf21");
        res = xfparent.getResources();
        if (res == null) { res = new PDResources(); xfparent.setResources(res); }
        res.put(COSName.getPDFName("Xf21"), xfchild);

        xfparent = forms.get("Xf20");
        xfchild = childs.get("Xf19");
        res = xfparent.getResources();
        if (res == null) { res = new PDResources(); xfparent.setResources(res); }
        res.put(COSName.getPDFName("Xf19"), xfchild);

        xfparent = forms.get("Xf17");
        xfchild = childs.get("Xf16");
        res = xfparent.getResources();
        if (res == null) { res = new PDResources(); xfparent.setResources(res); }
        res.put(COSName.getPDFName("Xf16"), xfchild);

        xfparent = forms.get("Xf15");
        xfchild = childs.get("Xf14");
        res = xfparent.getResources();
        if (res == null) { res = new PDResources(); xfparent.setResources(res); }
        res.put(COSName.getPDFName("Xf14"), xfchild);

        XObjectFactory.attachFormsToPage(page, forms);
    }

    private static float rowHeightForLines(int lines) {
        return switch (lines) {
            case 1, 2 -> 30f;
            case 3 -> 31f;
            case 4 -> 41f;
            case 5 -> 50f;
            default -> 59f;
        };
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
}