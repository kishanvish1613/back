package com.user.pdfservice.template.hdfc;

import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.Transaction;
import com.user.pdfservice.template.BankPdfTemplate;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.desktop.PrintFilesEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.jbig2.segments.GenericRefinementRegion.Template;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import tools.jackson.databind.node.BooleanNode;

@SuppressWarnings("deprecation")
public class HdfcTemplate implements BankPdfTemplate {

    private Map<String, BufferedImage> extractedImages = new LinkedHashMap<>();
    private final PDFont times = PDType1Font.TIMES_ROMAN;
    private final PDFont times_bold = PDType1Font.TIMES_BOLD;
    private final PDFont helvetica = PDType1Font.HELVETICA;
    private Statement stmt;

    private PDPageContentStream cs;
    PDResources resources = new PDResources();

    static final float PAGE_WIDTH = 638f;
    static final float PAGE_HEIGHT = 842f;

    static final float TABLE_TOP_Y_PAGE_1 = 612.553f;
    static final float TABLE_TOP_Y = 614.553f;
    static final float TABLE_BOTTOM_Y_PAGE_1 = 78.373f;
    static final float TABLE_BOTTOM_Y = 63.587f;
    static final float FULL_BG_HEIGHT = 533.614f; // 612.553 - 78.939

    static final float TOP_PADDING = 10.464f;

    static final float ROW_H_SMALL = 17.2f;
    static final float ROW_H_BIG = 34.4f;

    static final float START_ROW_Y = 594.939f; // header bottom

    static final float SUMMARY_HEIGHT = 190f; // safe measured height
    static final float SUMMARY_TITLE_OFFSET = 0f;

    private float RIGHT_DB;
    private float RIGHT_CR;
    private float RIGHT_BAL;

    private StringBuilder pageBuffer;

    private int pageno = 1;
    private int lineno;
    private int txnno;
    
    private String infoTemplate;

    @Override
    public byte[] generate(Statement statement) throws Exception {
        this.stmt = statement;
        RIGHT_CR = 530.187f + (times.getStringWidth("30.00") / 1000f) * 8f;
        RIGHT_DB = 434.235f + (times.getStringWidth("300,000.00") / 1000f) * 8f;
        RIGHT_BAL = 612.705f + (times.getStringWidth("1.41") / 1000f) * 8f;

        try (PDDocument doc = new PDDocument()) {
            pageBuffer = new StringBuilder();
            pageno = 1;
            lineno = 1;
            txnno = 1;
            buildInfo();

            doc.getDocument().setVersion(1.3f);
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            PDDocumentInformation info = new PDDocumentInformation();
            info.setProducer("3¬Õø");
            doc.setDocumentInformation(info);

            // AccessPermission ap = new AccessPermission();
            // ap.setReadOnly();
            // StandardProtectionPolicy spp = new StandardProtectionPolicy(
            //     "",
            //     "",
            //     ap
            // );
            // spp.setEncryptionKeyLength(40);
            // spp.setPreferAES(false);
            // doc.protect(spp);

            PDDocument original = PDDocument.load(new File("hdfc.pdf"));
            extractImagesFromPage(original, 0, extractedImages);

            PDPage page = new PDPage(new PDRectangle(638f, 842f));
            doc.addPage(page);

            BufferedImage logo = extractedImages.get("Im1");
            PDImageXObject ximg = LosslessFactory.createFromImage(doc, logo);
            resources.put(COSName.getPDFName("Im1"), ximg);
            resources.put(COSName.getPDFName("F1"), helvetica);
            resources.put(COSName.getPDFName("F5"), times);
            resources.put(COSName.getPDFName("F7"), times_bold);

            page.setResources(resources);

            cs = new PDPageContentStream(
                doc,
                page,
                PDPageContentStream.AppendMode.OVERWRITE,
                false
            );
            
            float y = drawTransactions(doc);
            y += 17.2f - 6.8f;
            
            y = drawSummaryStream(doc, y);
            y -= 7.6f;
            drawTableBackground(cs, TABLE_TOP_Y, y);
            cs.appendRawCommands(pageBuffer.toString());

            // y = TABLE_TOP_Y - (lineno - 1) * 17.2f;
            // if(y - SUMMARY_HEIGHT < TABLE_BOTTOM_Y){
            //     drawTableBackground(cs, TABLE_TOP_Y, y);
            //     if (!pageBuffer.isEmpty()) {
            //         cs.appendRawCommands(pageBuffer.toString());
            //         pageBuffer.setLength(0);
            //     }
            //     String template = load("/hdfcstreams/info.pdfops");
            //     cs.appendRawCommands(template + "\n");
            //     y = newPage(doc);
            // }
            

            // String template = load("/hdfcstreams/info.pdfops");
            // cs.appendRawCommands(template + "\n");

            // y = drawSummaryStream(doc, y);
            // System.err.println(y);
            // drawTableBackground(cs, TABLE_TOP_Y, y - 7.302f);
            // cs.appendRawCommands(pageBuffer.toString());
            // pageBuffer.setLength(0);
            
            cs.appendRawCommands(infoTemplate);

            cs.close();
            doc.save(out);

            // Encryption
            byte[] pdfBytes = out.toByteArray();

            PdfReader reader = new PdfReader(pdfBytes);
            ByteArrayOutputStream encOut = new ByteArrayOutputStream();

            PdfStamper stamper = new PdfStamper(reader, encOut);

            stamper.setEncryption(
                new byte[0], // user password
                new byte[0], // owner password
                PdfWriter.AllowPrinting,
                PdfWriter.STANDARD_ENCRYPTION_40
            );

            stamper.close();
            reader.close();

            return encOut.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }
    
    private void buildInfo() throws IOException {
        String template = load("/hdfcstreams/info.pdfops");
        String branchCode = stmt.details.ifsc.substring(stmt.details.ifsc.length() - 4);
        
        String name = stmt.details.name;
        String designation = "";
        Matcher m = Pattern.compile("^(.*?)\\.(.*)$").matcher(name);
        
        if(m.find()){
            designation = m.group(1);
            name = m.group(2);
        }
        
        String nameReplace = (designation != "" ? "(" + designation + ")" + " -1000.0 " : "") + format(name);
        
        String[] addr = stmt.details.address.toUpperCase().split("\\n");
        String addr1 = addr.length > 0 ? format(addr[0]) : "(.)";
        String addr2 = addr.length > 1 ? format(addr[1]) : "(.)";
        String addr3 = addr.length > 2 ? format(addr[2]) : "(.)";
        String addr4 = addr.length > 3 ? format(addr[3]) : "(.)";
        String addr5 = addr.length > 4 ? format(addr[4]) : "(.)";
        
        String[] baddr = stmt.details.branchAddress.toUpperCase().split("\n");
        String baddr1 = baddr[0] != null ? format(baddr[0]) : "(.)";
        String baddr2 = (baddr.length > 1 && baddr[1] != null) ? format(baddr[1]) : "(.)";
        String baddr3 = (baddr.length > 2 && baddr[2] != null) ? format(baddr[2]) : "(.)";
        
        Map<String, Object> fields = Map.ofEntries(
            Map.entry("NAME", nameReplace),
            Map.entry("CUSTOMER_ID", stmt.details.customerRelNo),
            Map.entry("ACCOUNT_NO", stmt.details.accountNumber),
            Map.entry("ADDR1", addr1),
            Map.entry("ADDR2", addr2),
            Map.entry("ADDR3", addr3),
            Map.entry("ADDR4", addr4),
            Map.entry("ADDR5", addr5),
            Map.entry("BADDR1", baddr1),
            Map.entry("BADDR2", baddr2),
            Map.entry("BADDR3", baddr3),
            Map.entry("IFSC", stmt.details.ifsc),
            Map.entry("PAGE_NO", pageno),
            Map.entry("EMAIL", stmt.details.email),
            Map.entry("BRANCH_CODE", branchCode),
            Map.entry("BRANCH", format(stmt.details.branch.toUpperCase())),
            Map.entry("CITY", format(stmt.details.city)),
            Map.entry("STATE", format(stmt.details.state)),
            Map.entry("START_DATE", formatIsoInstantDate(stmt.meta.statementPeriodStart)),
            Map.entry("END_DATE", formatIsoInstantDate(stmt.meta.statementPeriodEnd)),
            Map.entry("MICR", stmt.details.micr)
        );
        
        infoTemplate = renderPdfOps(template, fields);
    }
    
    private static String format(String text) {
        if(text == null) return "";
        float spaceAdjust = -250.0f;
        String[] words = text.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            sb.append("(").append(escape(words[i])).append(")");
            if (i < words.length - 1) {
                sb.append(" ").append(spaceAdjust).append(" ");
            }
        }
        return sb.toString();
    }

    public float drawSummaryStream(PDDocument doc, float startY) throws IOException {
    
        final float X = 28.346f;
        final float W = 602.359f;
    
        final float GAP1 = 70.040f;
        final float GAP2 = 57.664f;
        final float PADDING = 6.8f;
    
        float y = startY;
    
        StringBuilder bgBuffer = new StringBuilder();
        StringBuilder contentBuffer = new StringBuilder();
    
        float blockTop = y;
    
        // ========= SECTION 1 =========
        float y1 = drawSection1Buffered(y, contentBuffer);
    
        if (y1 < TABLE_BOTTOM_Y) {
            contentBuffer.setLength(0);
    
            y = newPage(doc, y);
            bgBuffer.setLength(0);
            contentBuffer.setLength(0);
            blockTop = y;
    
            y1 = drawSection1Buffered(y, contentBuffer);
        }
        
        bgBuffer.append(
            "q\n" +
            "0.50196 g\n" +                 // gray color
            "28.346 " + (y + 0.566f) + " 602.359 0.566 re f\n" +  // x y width height
            "Q\n"
        );
    
        y = y1;
    
        // ========= GAP 1 =========
        if (y - GAP1 < TABLE_BOTTOM_Y) {
            float h = blockTop - y + PADDING;
    
            bgBuffer.append(
                "q\n1 g 1 G\n" +
                X + " " + blockTop + " " + W + " -" + h + " re b\nQ\n"
            );
    
            pageBuffer.append(bgBuffer);
            pageBuffer.append(contentBuffer);
    
            y = newPage(doc, y - 7.8f);
            bgBuffer.setLength(0);
            contentBuffer.setLength(0);
            blockTop = y;
        } else {
            y -= GAP1;
        }
    
        // ========= CHECK SECTION 3 FIT =========
        float testY3 = y - GAP2;            // gap2
        testY3 -= 17.2f * 2;                // approx height of section3 text
    
        boolean section3Fits = testY3 >= TABLE_BOTTOM_Y;
    
        // ========= IF SECTION 3 FITS =========
        if (section3Fits) {
    
            // Gap2 on same page
            y -= GAP2;
    
            // Section3 on same page
            float y3 = drawSection3Buffered(y, contentBuffer);
    
            float finalHeight = blockTop - y3 + PADDING;
    
            bgBuffer.append(
                "q\n1 g 1 G\n" +
                X + " " + blockTop + " " + W + " -" + finalHeight + " re b\nQ\n"
            );
    
            pageBuffer.append(bgBuffer);
            pageBuffer.append(contentBuffer);
    
            return y3;
        }
    
        // ========= ELSE → NEW PAGE FOR GAP2 + SECTION3 =========
    
        // draw what we have so far (section1 + gap1)
        float partialHeight = blockTop - y + PADDING;
    
        bgBuffer.append(
            "q\n1 g 1 G\n" +
            X + " " + blockTop + " " + W + " -" + partialHeight + " re b\nQ\n"
        );
    
        pageBuffer.append(bgBuffer);
        pageBuffer.append(contentBuffer);
    
        // ---- new page ----
        y = newPage(doc, y - 7.5f);
    
        bgBuffer.setLength(0);
        contentBuffer.setLength(0);
        blockTop = y;
    
        // Gap2 on new page
        y -= GAP2;
    
        // Section3 on new page
        float y3 = drawSection3Buffered(y, contentBuffer);
    
        float finalHeight = blockTop - y3 + PADDING;
    
        bgBuffer.append(
            "q\n1 g 1 G\n" +
            X + " " + blockTop + " " + W + " -" + finalHeight + " re b\nQ\n"
        );
    
        pageBuffer.append(bgBuffer);
        pageBuffer.append(contentBuffer);
    
        return y3;
    }


    private float drawSection1Buffered(float startY, StringBuilder buf) throws IOException {
    
        float y = startY;
    
        y -= 27.526f;
    
        buf.append("BT\n/F7 10 Tf\n1 0 0 1 68.031 ").append(y)
           .append(" Tm [(STATEMENT) -250 (SUMMARY) -500 (:-)] TJ\nET\n");
    
        y -= 10.458f;
    
        buf.append("BT\n/F7 8 Tf\n1 0 0 1 132.346 ").append(y)
           .append(" Tm [(Opening) -250 (Balance) -12375.75 (Dr) -250 (Count) -4775.25 (Cr) -250 (Count) -3949.875 (Debits) -6800.0 (Credits)] TJ\nET\n");
    
        buf.append("BT\n1 0 0 1 572.057 ").append(y)
           .append(" Tm [(Closing) -250 (Bal)] TJ\nET\n");
    
        y -= 11.104f;
        
        int drc = 0;
        int crc = 0;
        float debits = 0f;
        float credits = 0f;
        
        for(Transaction tx : stmt.transactions){
            if(tx.debit == 0) crc++;
            else drc++;
            
            debits += tx.debit;
            credits += tx.credit;
        }
        
        float FS = 8f; // font size
        
        float C_OPENING = 143.574f + ((times.getStringWidth("299,971.41") / 1000f) * FS) / 2f;
        float C_DR_CNT   = C_OPENING + ((times.getStringWidth("299,971.41") / 1000f) * FS)/2f
                         + 15070.75f/1000f * FS
                         + ((times.getStringWidth("647") / 1000f) * FS) / 2f;
        
        float C_CR_CNT   = C_DR_CNT
                        + ((times.getStringWidth("647") / 1000f) * FS) / 2f
                         + 7358.25f/1000f * FS
                         + ((times.getStringWidth("114") / 1000f) * FS) / 2f;
        
        float C_DEBITS   = C_CR_CNT
                        + ((times.getStringWidth("114") / 1000f) * FS) / 2f
                         + 3727.375f/1000f * FS
                         + ((times.getStringWidth("42,726,877.23") / 1000f) * FS) / 2f;
        
        float C_CREDITS  = C_DEBITS
                        + ((times.getStringWidth("42,726,877.23") / 1000f) * FS) / 2f
                         + 3994.0f/1000f * FS
                         + ((times.getStringWidth("42,427,597.19") / 1000f) * FS) / 2f;
        
        float C_CLOSING  = C_CREDITS
                        + ((times.getStringWidth("42,427,597.19") / 1000f) * FS) / 2f
                         + 5494.0f/1000f * FS
                         + ((times.getStringWidth("691.37") / 1000f) * FS) / 2f;
                         
        
        String v1 = formatIndianAmount(stmt.transactions.get(0).balance);
        String v2 = Integer.toString(drc);
        String v3 = Integer.toString(crc);
        String v4 = formatIndianAmount(debits);
        String v5 = formatIndianAmount(credits);
        String v6 = formatIndianAmount(stmt.transactions.get(stmt.transactions.size() - 1).balance);
        
        float x1 = centerAlign(v1, C_OPENING, times, FS);
        float w1 = (times.getStringWidth(v1) / 1000f) * FS;
        
        float x2 = centerAlign(v2, C_DR_CNT, times, FS);
        float w2 = (times.getStringWidth(v2) / 1000f) * FS;
        
        float x3 = centerAlign(v3, C_CR_CNT, times, FS);
        float w3 = (times.getStringWidth(v3) / 1000f) * FS;
        
        float x4 = centerAlign(v4, C_DEBITS, times, FS);
        float w4 = (times.getStringWidth(v4) / 1000f) * FS;
        
        float x5 = centerAlign(v5, C_CREDITS, times, FS);
        float w5 = (times.getStringWidth(v5) / 1000f) * FS;
        
        float x6 = centerAlign(v6, C_CLOSING, times, FS);
        float w6 = (times.getStringWidth(v6) / 1000f) * FS;
        
        float tj12 = -(x2 - (x1 + w1)) * 1000f / FS;
        float tj23 = -(x3 - (x2 + w2)) * 1000f / FS;
        float tj34 = -(x4 - (x3 + w3)) * 1000f / FS;
        float tj45 = -(x5 - (x4 + w4)) * 1000f / FS;
        float tj56 = -(x6 - (x5 + w5)) * 1000f / FS;
        
        buf.append("BT\n/F5 8 Tf\n")
           .append("1 0 0 1 ").append(x1).append(" ").append(y).append(" Tm ")
           .append("[(").append(v1).append(") ")
           .append(tj12).append(" (").append(v2).append(") ")
           .append(tj23).append(" (").append(v3).append(") ")
           .append(tj34).append(" (").append(v4).append(") ")
           .append(tj45).append(" (").append(v5).append(") ")
           .append(tj56).append(" (").append(v6).append(")] TJ\nET\n");

        
        y -= 58.344f;
        
        String generatedOn = format(formatIsoInstantDate3(stmt.meta.generatedAt));
    
        buf.append("BT\n/F7 8 Tf\n1 0 0 1 76.134 ").append(y)
           .append(" Tm [(Generated) -250 (On:) -250 " + generatedOn + " ] TJ\nET\n");
    
        buf.append("BT\n1 0 0 1 262.427 ").append(y)
           .append(" Tm [(Generated) -250 (By:) -250 (" + stmt.details.customerRelNo + ")] TJ\nET\n");
           
        String branchCode = stmt.details.ifsc.substring(stmt.details.ifsc.length() - 4);
    
        buf.append("BT\n1 0 0 1 441.217 ").append(y)
           .append(" Tm [(Requesting) -250 (Branch) -250 (Code:) -250 (" + branchCode + ")] TJ\nET\n");
    
        return y;
    }
    
    private float centerAlign(String text, float center, PDFont font, float fontSize) throws IOException {
        float textWidth = (font.getStringWidth(text) / 1000f) * fontSize;
        return center - (textWidth / 2f);
    }
    
    private float drawSection3Buffered(float startY, StringBuilder buf) {
    
        float y = startY;
    
        buf.append("BT\n/F5 8 Tf\n0 g\n")
           .append("1 0 0 1 474.801 ").append(y)
           .append(" Tm [(This) -250 (is) -250 (a) -250 (computer) -250 (generated) -250 (statement) -250 (and) -250 (does)] TJ\n");
    
        y -= 17.2f;
    
        buf.append("1 0 0 1 474.801 ").append(y)
           .append(" Tm [(not) -250 (require) -250 (signature.)] TJ\nET\n");
           
        return y;
    }

    private float drawTransactions(PDDocument doc) throws IOException {
        float y = START_ROW_Y;
        float tableTop = pageno == 1 ? TABLE_TOP_Y_PAGE_1 : TABLE_BOTTOM_Y;

        drawTableBackground(cs, tableTop, TABLE_BOTTOM_Y_PAGE_1);
        String template = load("/hdfcstreams/tableheader.pdfops");
        cs.appendRawCommands(template + "\n");

        for (Transaction tx : stmt.transactions) {
            String date = formatIsoInstantDate2(tx.date);
            String debit = tx.debit == 0 ? "" : formatIndianAmount(tx.debit);
            String credit = tx.credit == 0 ? "" : formatIndianAmount(tx.credit);
            String balance = formatIndianAmount(tx.balance);

            y = drawTxnRow(
                doc,
                y,
                date,
                tx.description,
                tx.reference,
                date,
                debit,
                credit,
                balance
            );
            txnno++;
        }
        
        return y;
    }

    private static void drawTableBackground(
        PDPageContentStream cs,
        float tableTopY,
        float tableBottomY
    ) throws IOException {
        float bgHeight = tableTopY - tableBottomY;

        StringBuilder sb = new StringBuilder();

        // blue border background
        sb
            .append("q\n")
            .append("0.90196 1 1 rg\n")
            .append("0.90196 1 1 RG\n")
            .append("28.346 ")
            .append(tableTopY)
            .append(" 602.359 -")
            .append(bgHeight - 0.5f)
            .append(" re b\n")
            .append("Q\n");

        // top border
        sb
            .append("q\n")
            .append("0.50196 g\n")
            .append("28.346 ")
            .append(tableTopY)
            .append(" 602.359 0.566 re f\n")
            .append("Q\n");

        // bottom border
        sb
            .append("q\n")
            .append("0.50196 g\n")
            .append("28.346 ")
            .append(tableTopY - bgHeight)
            .append(" 602.359 0.566 re f\n")
            .append("Q\n");

        // left border
        sb
            .append("q\n")
            .append("0.50196 g\n")
            .append("27.78 ")
            .append(tableTopY - bgHeight)
            .append(" 0.566 ")
            .append(bgHeight)
            .append(" re f\n")
            .append("Q\n");

        // right border
        sb
            .append("q\n")
            .append("0.50196 g\n")
            .append("630.705 ")
            .append(tableTopY - bgHeight)
            .append(" 0.566 ")
            .append(bgHeight)
            .append(" re f\n")
            .append("Q\n");

        cs.appendRawCommands(sb.toString());
    }

    public float drawTxnRow(
        PDDocument doc,
        float prevBottomY, // bottom of PREVIOUS row
        String date,
        String narration,
        String refNo,
        String valueDate,
        String debit,
        String credit,
        String balance
    ) throws IOException {
        int pageCapacity = (pageno == 1 ? 30 : 32);

        if ((pageno == 1 && lineno > 30) || (pageno != 1 && lineno > 32)) {
            prevBottomY = newPage(doc, TABLE_BOTTOM_Y);
        }

        // ---- layout constants ----
        final float gray = 0.50196f;
        final float lineW = 0.566f;
        final float lineStep = 17.20f; // line spacing

        float basePadding = 0f;
        if (pageBuffer != null && pageBuffer.isEmpty()) {
            basePadding = TOP_PADDING;
        } else {
            basePadding = 0f;
        }

        // column X positions
        final float xDate = 33.681f;
        final float xNarr = 68.031f;
        final float xRef = 292.598f;
        final float xVal = 362.499f;
        final float xDr = alignTextRight(debit, RIGHT_DB, times, 8f);
        final float xCr = alignTextRight(credit, RIGHT_CR, times, 8f);
        final float xBal = alignTextRight(balance, RIGHT_BAL, times, 8f);

        final float[] gridXs = {
            67.465f,
            254.551f,
            356.598f,
            396.283f,
            474.235f,
            552.187f,
        };

        // ---- break narration into lines ----
        List<String> lines = breakLines(narration, 32); // tuned to column width
        int maxLinesFit = pageCapacity - lineno + 1;

        List<String> drawLines;
        List<String> remainingLines = null;

        if (lines.size() > maxLinesFit) {
            drawLines = lines.subList(0, maxLinesFit);
            remainingLines = new ArrayList<>(
                lines.subList(maxLinesFit, lines.size())
            );
        } else {
            drawLines = lines;
        }

        int lineCount = drawLines.size();
        float rowHeight = (lineCount * lineStep) + basePadding;
        float currentBottomY = prevBottomY - rowHeight;
        float drawBottom = currentBottomY;

        lineno += lineCount;
        if (txnno == stmt.transactions.size()) {
            currentBottomY = prevBottomY - (lineCount * lineStep);
            drawBottom = TABLE_TOP_Y - (lineno - 1) * 17.2f;
        }

        boolean pageBreakAfter =
            (pageno == 1 && (lineno + lineCount >= 30)) ||
            (pageno != 1 && (lineno + lineCount >= 32));

        if (pageBreakAfter) {
            float tableBottom = (pageno == 1
                ? TABLE_BOTTOM_Y_PAGE_1
                : TABLE_BOTTOM_Y);
            drawBottom = tableBottom;
            rowHeight = prevBottomY - tableBottom;
        }

        // if(remainingLines != null && !remainingLines.isEmpty()){
        //     rowHeight = prevBottomY - TABLE_BOTTOM_Y;
        // }

        // ---- compute text start Y (top of row minus padding) ----
        float textY = prevBottomY - basePadding;

        StringBuilder sb = new StringBuilder();

        // ---- grid vertical separators ----
        for (float gx : gridXs) {
            sb
                .append("q\n")
                .append(gray)
                .append(" g\n")
                .append(gx)
                .append(" ")
                .append(drawBottom)
                .append(" ")
                .append(lineW)
                .append(" ")
                .append(rowHeight)
                .append(" re f\n")
                .append("Q\n");
        }

        // ---- text ----
        sb
            .append("BT\n")
            .append("/F5 8.0 Tf\n")
            .append("0 g\n")
            .append("0 Tr\n");

        // Date
        sb
            .append("1 0 0 1 ")
            .append(xDate)
            .append(" ")
            .append(textY)
            .append(" Tm ")
            .append(toTJ(date, -250))
            .append(" TJ\n");

        // Narration (multi-line)
        float narrY = textY;
        for (String line : drawLines) {
            sb
                .append("1 0 0 1 ")
                .append(xNarr)
                .append(" ")
                .append(narrY)
                .append(" Tm ")
                .append(toTJ(line, -250))
                .append(" TJ\n");
            narrY -= lineStep;
        }

        // Ref
        sb
            .append("1 0 0 1 ")
            .append(xRef)
            .append(" ")
            .append(textY)
            .append(" Tm ")
            .append(toTJ(refNo, -250))
            .append(" TJ\n");

        // Value date
        sb
            .append("1 0 0 1 ")
            .append(xVal)
            .append(" ")
            .append(textY)
            .append(" Tm ")
            .append(toTJ(valueDate, -250))
            .append(" TJ\n");

        // Debit
        if (debit != null && !debit.isEmpty()) {
            sb
                .append("1 0 0 1 ")
                .append(xDr)
                .append(" ")
                .append(textY)
                .append(" Tm ")
                .append(toTJ(debit, -250))
                .append(" TJ\n");
        }

        // Credit
        if (credit != null && !credit.isEmpty()) {
            sb
                .append("1 0 0 1 ")
                .append(xCr)
                .append(" ")
                .append(textY)
                .append(" Tm ")
                .append(toTJ(credit, -250))
                .append(" TJ\n");
        }

        // Balance
        sb
            .append("1 0 0 1 ")
            .append(xBal)
            .append(" ")
            .append(textY)
            .append(" Tm ")
            .append(toTJ(balance, -250))
            .append(" TJ\n");

        sb.append("ET\n");

        pageBuffer.append(sb.toString());

        if ((remainingLines != null && !remainingLines.isEmpty())) {
            currentBottomY = newPage(doc, TABLE_BOTTOM_Y);
            currentBottomY = drawTxnRow(
                doc,
                currentBottomY,
                "",
                String.join(" ", remainingLines),
                "",
                "",
                "",
                "",
                ""
            );
        }

        return currentBottomY;
    }

    private float newPage(PDDocument doc, float tableBottom) throws IOException {
        if (!pageBuffer.isEmpty()) {
            if (pageno != 1) drawTableBackground(
                cs,
                TABLE_TOP_Y,
                tableBottom
            );
            cs.appendRawCommands(pageBuffer.toString());
            pageBuffer = new StringBuilder();
        }
        cs.appendRawCommands(infoTemplate);
        cs.close();

        // New Page
        lineno = 1;
        PDPage newPage = new PDPage(new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT));
        doc.addPage(newPage);
        newPage.setResources(resources);
        cs = new PDPageContentStream(
            doc,
            newPage,
            PDPageContentStream.AppendMode.OVERWRITE,
            false
        );

        pageno++;
        buildInfo();

        return TABLE_TOP_Y;
    }

    private static List<String> breakLines(String text, int maxCharsPerLine) {
        List<String> lines = new ArrayList<>();
    
        if (text == null || text.trim().isEmpty()) {
            lines.add(""); // keep 1 empty line for layout consistency
            return lines;
        }
    
        String[] words = text.toUpperCase().trim().split("\\s+");
        StringBuilder line = new StringBuilder();
    
        for (String w : words) {
    
            // Hard-break very long words
            while (w.length() > maxCharsPerLine) {
                if (line.length() > 0) {
                    lines.add(line.toString().trim());
                    line.setLength(0);
                }
                lines.add(w.substring(0, maxCharsPerLine));
                w = w.substring(maxCharsPerLine);
            }
    
            // Normal wrap
            if (line.length() + w.length() + 1 > maxCharsPerLine) {
                if (line.length() > 0) {
                    lines.add(line.toString().trim());
                }
                line.setLength(0);
            }
    
            line.append(w).append(" ");
        }
    
        if (line.length() > 0) {
            lines.add(line.toString().trim());
        }
    
        // absolute safety
        if (lines.isEmpty()) {
            lines.add("");
        }
    
        return lines;
    }

    private static String toTJ(String text, float spaceAdjust) {
        String[] words = text.trim().split("\\s+");
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < words.length; i++) {
            sb.append("(").append(escape(words[i])).append(")");
            if (i < words.length - 1) {
                sb.append(" ").append(spaceAdjust).append(" ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static float alignTextRight(
        String text,
        float RIGHT_EDGE,
        PDFont font,
        float fontSize
    ) throws IOException {
        float textWidth = (font.getStringWidth(text) / 1000f) * fontSize;
        return RIGHT_EDGE - textWidth;
    }

    String safe(String s) {
        return s == null ? "" : s;
    }

    private static String escape(String text) {
        if (text == null) return "";
        return text
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("\n", " ")
            .replace("\r", " ");
    }

    String[] splitDescription(String desc) {
        if (desc == null) return new String[] { "", "" };

        desc = desc.trim();

        if (desc.length() <= 40) {
            return new String[] { desc, "" };
        }

        // Prefer split on slash (UPI style)
        int cut = desc.indexOf('/', 30);
        if (cut == -1) cut = desc.indexOf(' ', 30);
        if (cut == -1) cut = 40;

        String line1 = desc.substring(0, cut).trim();
        String line2 = desc.substring(cut).trim();

        // Hard safety trim
        if (line2.length() > 45) {
            line2 = line2.substring(0, 45);
        }

        return new String[] { line1, line2 };
    }

    public static PDType0Font cloneEmbeddedFontNative(
        PDDocument srcDoc,
        PDDocument dstDoc,
        String fontNameContains
    ) throws IOException {
        PDPage page = srcDoc.getPage(0);
        PDResources res = page.getResources();

        for (COSName key : res.getFontNames()) {
            PDFont f = res.getFont(key);

            if (f instanceof PDType0Font t0) {
                if (t0.getName().contains(fontNameContains)) {
                    COSDictionary fontDict = t0.getCOSObject();
                    COSDictionary clonedDict = new COSDictionary(fontDict);

                    COSStream clonedStream = dstDoc
                        .getDocument()
                        .createCOSStream();
                    clonedStream.setItem(
                        COSName.LENGTH,
                        fontDict.getItem(COSName.LENGTH)
                    );

                    try (
                        InputStream is = t0
                            .getDescendantFont()
                            .getFontDescriptor()
                            .getFontFile2()
                            .createInputStream();
                        var os = clonedStream.createOutputStream()
                    ) {
                        is.transferTo(os);
                    }

                    clonedDict.setItem(COSName.FONT_FILE2, clonedStream);

                    return new PDType0Font(clonedDict);
                }
            }
        }

        throw new IOException("Font not found");
    }

    private static String formatIndianAmount(double value) {
        boolean negative = value < 0;
        value = Math.abs(value);

        // Format to plain 2-decimal string
        String s = String.format(Locale.ROOT, "%.2f", value);

        int dot = s.indexOf('.');
        String intPart = dot >= 0 ? s.substring(0, dot) : s;
        String fracPart = dot >= 0 ? s.substring(dot) : ".00";

        String grouped = groupIndian(intPart);

        return (negative ? "-" : "") + grouped + fracPart;
    }

    private static String groupIndian(String digits) {
        int len = digits.length();
        if (len <= 3) {
            return digits;
        }
    
        StringBuilder sb = new StringBuilder();
        int i = len;
    
        // group in 3s instead of 2–2–3
        while (i > 0) {
            int start = Math.max(0, i - 3);
            if (sb.length() > 0) {
                sb.insert(0, ',');
            }
            sb.insert(0, digits.substring(start, i));
            i = start;
        }
    
        return sb.toString();
    }

    private static String formatIsoInstantDate(String isoInstant) {
        Instant instant = Instant.parse(isoInstant);
        return DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneOffset.UTC)
            .format(instant);
    }

    private static String formatIsoInstantDate2(String isoInstant) {
        Instant instant = Instant.parse(isoInstant);
        return DateTimeFormatter.ofPattern("dd/MM/yy")
            .withZone(ZoneOffset.UTC)
            .format(instant);
    }

    private static String formatIsoInstantDate3(String isoInstant) {
        Instant instant = Instant.parse(isoInstant);
        return DateTimeFormatter.ofPattern("dd-MMM-yyy HH:mm:ss")
            .withZone(ZoneOffset.UTC)
            .format(instant);
    }

    private static String renderPdfOps(String template, Map<String, ?> values) {
        String result = template;

        for (var e : values.entrySet()) {
            String key = "{{" + e.getKey() + "}}";
            Object v = e.getValue();

            if (!result.contains(key)) {
                throw new IllegalArgumentException(
                    "Placeholder not found: " + key
                );
            }

            String replacement;
            if ("ADDRSTREAM".equals(e.getKey())) {
                replacement = v.toString();
            } else {
                replacement = v.toString();
            }

            result = result.replace(key, replacement);
        }

        if (result.contains("{{")) {
            throw new IllegalStateException(
                "Unresolved placeholders in PDF ops"
            );
        }

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
        try (var is = HdfcTemplate.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.US_ASCII);
        }
    }

    private static void extractImagesFromPage(
        PDDocument doc,
        int pageIndex,
        Map<String, BufferedImage> out
    ) throws IOException {
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
