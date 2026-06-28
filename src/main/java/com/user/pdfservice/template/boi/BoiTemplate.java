package com.user.pdfservice.template.boi;

import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.Transaction;
import com.user.pdfservice.template.BankPdfTemplate;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
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
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
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

@SuppressWarnings("deprecation")
public class BoiTemplate implements BankPdfTemplate {

    private Map<String, BufferedImage> extractedImages = new LinkedHashMap<>();
    private final PDType1Font font1 = PDType1Font.HELVETICA_BOLD;
    private final PDType1Font font2 = PDType1Font.HELVETICA;
    private PDType0Font font3;
    private Statement stmt;
    private PDPageContentStream cs;

    static final float X_REF_COL = 280f;
    static final float W_REF_COL = 50f;

    private final float ROW1H = 56.56f;
    private final float ROW2H = 74.85f;

    private int pageno = 1;

    @Override
    public byte[] generate(Statement statement) throws Exception {
        this.stmt = statement;

        try (PDDocument doc = new PDDocument()) {
            
            if(stmt.meta.password == null){
                stmt.meta.password = "";
                // throw new Error("No Password Provided");
            }
            
            AccessPermission ap = new AccessPermission();
            ap.setReadOnly();
            
            StandardProtectionPolicy spp =
                    new StandardProtectionPolicy(
                            stmt.meta.password,   // owner password
                            stmt.meta.password,    // user password
                            ap
                    );
            
            spp.setEncryptionKeyLength(128);
            spp.setPreferAES(true);
            
            doc.protect(spp);

            PDDocumentInformation info = new PDDocumentInformation();
            info.setProducer(
                "iText® Core 7.2.5 (AGPL version), pdfHTML 4.0.5 (AGPL version) ©2000-2023 iText Group NV"
            );
            Calendar now = Calendar.getInstance();
            info.setCreationDate(now);
            info.setModificationDate(now);
            info.setTitle("Document");
            doc.setDocumentInformation(info);

            PDDocument original = PDDocument.load(new File("boi.pdf"));
            extractImagesFromPage(original, 0, extractedImages);
            BufferedImage logoImg = extractedImages.get("Im1");
            PDImageXObject logo = LosslessFactory.createFromImage(doc, logoImg);

            PDType0Font cloned = cloneEmbeddedFontNative(
                original,
                doc,
                "NotoSans-Regular"
            );

            PDPage page = new PDPage(new PDRectangle(1550f, 1900f));
            doc.addPage(page);

            PDResources resources = new PDResources();
            resources.put(COSName.getPDFName("F1"), font1);
            resources.put(COSName.getPDFName("F2"), font2);
            resources.put(COSName.getPDFName("F3"), cloned);
            page.setResources(resources);

            font3 = cloned;
            pageno = 1;

            cs = new PDPageContentStream(doc, page);
            
            String[] addr = stmt.details.address.split("\n");
            List<String> name = wrapText(stmt.details.name, 26);
            
            boolean multiheader = name.size() == 2 || addr.length == 3;
            
            String template, template2;
            Map<String, Object> fields;
            if(multiheader){
                template = load("/boistreams/headerinfoBIG.pdfops");
                template2 = load("/boistreams/headerinfo2BIG.pdfops");
                // float namey = 448.5f;
                fields = Map.ofEntries(
                    Map.entry("NAME1", name.get(0)),
                    Map.entry("NAME2", name.size() > 1 ? name.get(1) : ""),
                    Map.entry("CUSTOMER_ID", stmt.details.customerRelNo),
                    Map.entry("ACCOUNT_NO", stmt.details.accountNumber),
                    Map.entry("ADDR1", addr.length > 0 ? addr[0] : ""),
                    Map.entry("ADDR2", addr.length > 1 ? addr[1] : ""),
                    Map.entry("ADDR3", addr.length > 2 ? addr[2] : ""),
                    Map.entry("IFSC", stmt.details.ifsc),
                    Map.entry("BRANCH_NAME", stmt.details.branch),
                    Map.entry("DATE2", formatIsoInstantDate2(stmt.meta.generatedAt))
                );
            } else{
                template = load("/boistreams/headerinfo.pdfops");
                template2 = load("/boistreams/headerinfo2.pdfops");
                fields = Map.ofEntries(
                    Map.entry("NAME", stmt.details.name),
                    Map.entry("CUSTOMER_ID", stmt.details.customerRelNo),
                    Map.entry("ACCOUNT_NO", stmt.details.accountNumber),
                    Map.entry("ADDR1", (addr.length > 0 ? addr[0] : "")),
                    Map.entry("ADDR2", (addr.length > 1 ? addr[1] : "")),
                    Map.entry("IFSC", stmt.details.ifsc),
                    Map.entry("BRANCH_NAME", stmt.details.branch),
                    Map.entry("DATE2", formatIsoInstantDate2(stmt.meta.generatedAt))
                );
            }
            
            Map<String, Object> fields2 = Map.ofEntries(
                Map.entry("START_DATE", formatIsoInstantDate(stmt.meta.statementPeriodStart)),
                Map.entry("END_DATE", formatIsoInstantDate(stmt.meta.statementPeriodEnd))
            );

            String rendered = renderPdfOps(template, fields);
            cs.appendRawCommands(rendered + "\n");
            
            float bottomMargin = 60f;
            int i = 1;
            float grid_top, grid_bottom;
            if(multiheader){
                grid_top = 1109.54f;
                grid_bottom = 1052.01f;
            } else {
                grid_top = 1131.72f;
                grid_bottom = 1075.32f;
            }
            cs.appendRawCommands("Q\n");
            // cs.appendRawCommands("Q\n");
            cs.appendRawCommands("Q\n");
            cs.appendRawCommands(buildHeaderGrid(grid_top, grid_bottom));

            StringBuilder gridBuffer = new StringBuilder();
            float y = multiheader ? 1052.01f : 1075.32f;
            // cs.appendRawCommands(buildRowSeparator(y));

            float table_top = y;

            for (Transaction tx : stmt.transactions) {
                boolean twoLine =
                    tx.description != null && tx.description.length() > 40;
                float rowHeight = twoLine ? ROW2H : ROW1H;

                // ---- page break ----
                if (y - rowHeight < bottomMargin) {
                    if(pageno == 1){
                        cs.saveGraphicsState();
                        cs.drawImage(logo, 1089, 1664.5f, 336, 135);
                        cs.restoreGraphicsState();
            
                        rendered = renderPdfOps(template2, fields2);
                        cs.appendRawCommands(rendered + "\n");
                    }
                    // new page
                    cs.appendRawCommands(gridBuffer.toString());
                    cs.appendRawCommands(buildVerticalGrid(table_top, y));
                    gridBuffer.setLength(0); // clear buffer
                    cs.close();

                    PDPage newPage = new PDPage(new PDRectangle(1550f, 1900f));
                    doc.addPage(newPage);
                    newPage.setResources(page.getResources()); // reuse fonts/resources

                    cs = new PDPageContentStream(doc, newPage);
                    y = 1777.06f;
                    table_top = y;
                    gridBuffer.append(buildRowSeparator(y));
                    pageno++;
                }

                drawTransactionRow(i++, tx, y);
                y -= rowHeight;
                gridBuffer.append(buildRowSeparator(y));
            }

            cs.appendRawCommands(buildVerticalGrid(table_top, y));
            cs.appendRawCommands(gridBuffer.toString());
            
            final float FOOTER_HEIGHT = 924.62f - 841.89f + 20f;
            
            if(y - FOOTER_HEIGHT < bottomMargin){
                cs.close();
                
                PDPage newPage = new PDPage(new PDRectangle(1550f, 1900f));
                doc.addPage(newPage);
                newPage.setResources(page.getResources()); // reuse fonts/resources

                cs = new PDPageContentStream(doc, newPage);
                y = 1777.06f;
            }

            drawFooterBlock(y, bottomMargin);

            cs.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.getDocument().setVersion(1.7f);
            COSDictionary catalog = doc.getDocumentCatalog().getCOSObject();
            catalog.removeItem(COSName.VERSION);
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }
    
    public static List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
    
        StringBuilder currentLine = new StringBuilder();
    
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 <= maxWidth) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }
    
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
    
        return lines;
    }

    void drawFooterBlock(
        float lastRowBottomY,
        float bottomMargin
    ) throws IOException {
        final float FOOTER_GAP = 37.65f;

        float footerStartY = lastRowBottomY - FOOTER_GAP;

        float noteY = footerStartY;
        float line1Y = footerStartY - (924.62f - 893.64f); // 30.98
        float line2Y = footerStartY - (924.62f - 867.76f); // 56.86
        float line3Y = footerStartY - (924.62f - 841.89f); // 82.73

        // NOTE:
        cs.beginText();
        cs.setFont(font1, 14.32f);
        cs.newLineAtOffset(114.75f, noteY);
        cs.showText("NOTE:");
        cs.endText();

        // Line 1
        cs.beginText();
        cs.setFont(font2, 17.25f);
        cs.newLineAtOffset(114.75f, line1Y);
        cs.showText(
            "Any discrepancy in the account statement should be notified to the bank within period of 30 days of generation of statement. It will be treated that the entries/contents of"
        );
        cs.endText();

        // Line 2
        cs.beginText();
        cs.setFont(font2, 17.25f);
        cs.newLineAtOffset(114.75f, line2Y);
        cs.showText(
            "this statement are checked and found correct by you, if no such complaint is made within the period stated above. Please do not share your ATM, Card details, PIN, OTP"
        );
        cs.endText();

        // Line 3
        cs.beginText();
        cs.setFont(font2, 17.25f);
        cs.newLineAtOffset(114.75f, line3Y);
        cs.showText(
            "and Passwords with anyone else. Bank never asks for such details."
        );
        cs.endText();
    }

    String buildVerticalGrid(float top, float bottom) {
        StringBuilder g = new StringBuilder();

        float[] xs = {
            115.88f,
            218.24f,
            354.73f,
            809.68f,
            1003.04f,
            1196.40f,
            1423.88f,
        };

        for (float x : xs) {
            g
                .append("q 0 0 0 RG 2.25 w ")
                .append(x)
                .append(" ")
                .append(top)
                .append(" m ")
                .append(x)
                .append(" ")
                .append(bottom)
                .append(" l S Q\n");
        }

        return g.toString();
    }

    String buildRowSeparator(float y) {
        StringBuilder g = new StringBuilder();

        // single horizontal line across full table width
        g
            .append("q 0 0 0 RG 2.25 w ")
            .append("114.75 ")
            .append(y)
            .append(" m ")
            .append("1425 ")
            .append(y)
            .append(" l S Q\n");

        return g.toString();
    }

    String buildHeaderGrid(float top, float bottom) {
        StringBuilder g = new StringBuilder();

        // float top = 1131.72f;
        // float bottom = 1075.32f;

        // verticals
        g
            .append("q 0 0 0 RG 2.25 w 115.88 ")
            .append(top)
            .append(" m 115.88 ")
            .append(bottom)
            .append(" l S Q\n");
        g
            .append("q 0 0 0 RG 2.25 w 218.24 ")
            .append(top)
            .append(" m 218.24 ")
            .append(bottom)
            .append(" l S Q\n");
        g
            .append("q 0 0 0 RG 2.25 w 354.73 ")
            .append(top)
            .append(" m 354.73 ")
            .append(bottom)
            .append(" l S Q\n");
        g
            .append("q 0 0 0 RG 2.25 w 809.68 ")
            .append(top)
            .append(" m 809.68 ")
            .append(bottom)
            .append(" l S Q\n");
        g
            .append("q 0 0 0 RG 2.25 w 1003.04 ")
            .append(top)
            .append(" m 1003.04 ")
            .append(bottom)
            .append(" l S Q\n");
        g
            .append("q 0 0 0 RG 2.25 w 1196.40 ")
            .append(top)
            .append(" m 1196.40 ")
            .append(bottom)
            .append(" l S Q\n");
        g
            .append("q 0 0 0 RG 2.25 w 1423.88 ")
            .append(top)
            .append(" m 1423.88 ")
            .append(bottom)
            .append(" l S Q\n");

        // horizontals
        g
            .append("q 0 0 0 RG 2.25 w 114.75 ")
            .append(top)
            .append(" m 1425 ")
            .append(top)
            .append(" l S Q\n");
        g
            .append("q 0 0 0 RG 2.25 w 114.75 ")
            .append(bottom)
            .append(" m 1425 ")
            .append(bottom)
            .append(" l S Q\n");

        return g.toString();
    }

    String buildTransactionRowGrid(float y, float h) {
        StringBuilder g = new StringBuilder();

        // verticals
        g
            .append("q 0 0 0 RG 2.25 w 115.88 ")
            .append(y + h)
            .append(" m 115.88 ")
            .append(y)
            .append(" l S Q\n");
        g
            .append("q 0 0 0 RG 2.25 w 218.24 ")
            .append(y + h)
            .append(" m 218.24 ")
            .append(y)
            .append(" l S Q\n");
        g
            .append("q 0 0 0 RG 2.25 w 354.73 ")
            .append(y + h)
            .append(" m 354.73 ")
            .append(y)
            .append(" l S Q\n");
        g
            .append("q 0 0 0 RG 2.25 w 809.68 ")
            .append(y + h)
            .append(" m 809.68 ")
            .append(y)
            .append(" l S Q\n");
        g
            .append("q 0 0 0 RG 2.25 w 1003.04 ")
            .append(y + h)
            .append(" m 1003.04 ")
            .append(y)
            .append(" l S Q\n");
        g
            .append("q 0 0 0 RG 2.25 w 1196.40 ")
            .append(y + h)
            .append(" m 1196.40 ")
            .append(y)
            .append(" l S Q\n");
        g
            .append("q 0 0 0 RG 2.25 w 1423.88 ")
            .append(y + h)
            .append(" m 1423.88 ")
            .append(y)
            .append(" l S Q\n");

        // horizontals (top + bottom)
        g
            .append("q 0 0 0 RG 2.25 w 114.75 ")
            .append(y + h)
            .append(" m 1425 ")
            .append(y + h)
            .append(" l S Q\n");
        g
            .append("q 0 0 0 RG 2.25 w 114.75 ")
            .append(y)
            .append(" m 1425 ")
            .append(y)
            .append(" l S Q\n");

        return g.toString();
    }

    void drawTransactionRow(int i, Transaction tx, float y) throws IOException {
        boolean twoLine =
            tx.description != null && tx.description.length() > 40;

        float h = twoLine ? ROW2H : ROW1H;

        float rowBottom = y - h;
        float yoffset = twoLine ? 42.66f : 24.37f;

        // Sr No
        drawCell(
            cs,
            117f,
            rowBottom,
            100.12f,
            h,
            String.valueOf(i),
            19.5f,
            "F2",
            15f,
            yoffset
        );

        // Date
        drawCell(
            cs,
            219.37f,
            rowBottom,
            134.24f,
            h,
            formatIsoInstantDate(tx.date),
            19.5f,
            "F2",
            15f,
            yoffset
        );

        // Remarks
        if (twoLine) {
            String[] lines = splitDescription(tx.description);

            drawCell(
                cs,
                355.85f,
                rowBottom,
                452.71f,
                h,
                lines[0],
                19.5f,
                "F2",
                15f,
                42f
            );
            drawCell(
                cs,
                355.85f,
                rowBottom,
                452.71f,
                h,
                lines[1],
                19.5f,
                "F2",
                15f,
                20f
            );
        } else {
            drawCell(
                cs,
                355.85f,
                rowBottom,
                452.71f,
                h,
                safe(tx.description),
                19.5f,
                "F2",
                15f,
                yoffset
            );
        }

        String db = tx.debit == 0 ? " " : formatIndianAmount(tx.debit);
        String cr = tx.credit == 0 ? " " : formatIndianAmount(tx.credit);
        String bal = formatIndianAmount(tx.balance);

        float offDb = alignTextRightInCell(db, font2, 19.5f, 191.11f, 15f);
        float offCr = alignTextRightInCell(cr, font2, 19.5f, 191.11f, 15f);
        float offBal =
            alignTextRightInCell(bal, font3, 19.5f, 225.23f, 15f) - 16.22f;

        drawCell(
            cs,
            810.81f,
            rowBottom,
            191.11f,
            h,
            db,
            19.5f,
            "F2",
            offDb,
            yoffset
        );
        drawCell(
            cs,
            1004.17f,
            rowBottom,
            191.11f,
            h,
            cr,
            19.5f,
            "F2",
            offCr,
            yoffset
        );
        drawCell(
            cs,
            1197.52f,
            rowBottom,
            225.23f,
            h,
            bal,
            19.5f,
            "F3",
            offBal,
            yoffset - 3.66f
        );
    }

    private static float alignTextRightInCell(
        String text,
        PDFont font,
        float fontSize,
        float cellWidth,
        float paddingRight
    ) throws IOException {
        if (text == null || text.isEmpty()) {
            return cellWidth - paddingRight;
        }
        float textWidth = (font.getStringWidth(text) / 1000f) * fontSize;
        return (cellWidth - textWidth) - paddingRight;
    }

    void drawCell(
        PDPageContentStream cs,
        float x,
        float y,
        float w,
        float h,
        String text,
        float fontSize,
        String fontAlias,
        float textXOffset,
        float textYOffset
    ) throws IOException {
        // Cell state
        cs.appendRawCommands("q\n");
        cs.appendRawCommands(x + " " + y + " " + w + " " + h + " re\n");
        cs.appendRawCommands("W\n");
        cs.appendRawCommands("n\n");
        cs.appendRawCommands("q\n");

        // Text
        if (fontAlias.equals("F3")) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(0x07);
            baos.write(0x47);
            baos.write(0x00);
            baos.write(0x62);
            byte[] encoded = font3.encode(text);
            baos.write(encoded);

            byte[] glyphBytes = baos.toByteArray();

            // force hex COSString
            COSString cosStr = new COSString(glyphBytes);
            cosStr.setForceHexForm(true);
            cs.beginText();
            cs.setFont(font3, fontSize);
            cs.appendRawCommands(
                (x + textXOffset) + " " + (y + textYOffset) + " Td\n"
            );
            // cs.showText(text);
            cs.appendRawCommands(
                "<" + cosStr.toHexString().toLowerCase() + ">Tj\n"
            );
            cs.endText();
        } else {
            cs.appendRawCommands("BT\n");
            cs.appendRawCommands("/" + fontAlias + " " + fontSize + " Tf\n");
            cs.appendRawCommands(
                (x + textXOffset) + " " + (y + textYOffset) + " Td\n"
            );
            cs.appendRawCommands("(" + escape(text) + ")Tj\n");
            cs.appendRawCommands("ET\n");
        }

        // Restore state
        cs.appendRawCommands("Q\n");
        cs.appendRawCommands("Q\n");
    }

    String safe(String s) {
        return s == null ? "" : s;
    }

    String escape(String text) {
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
    
                    COSStream clonedStream = dstDoc.getDocument().createCOSStream();
    
                    // ---- COPY FONT FILE STREAM (SAFE) ----
                    InputStream is = t0
                        .getDescendantFont()
                        .getFontDescriptor()
                        .getFontFile2()
                        .createInputStream();
    
                    OutputStream os = clonedStream.createOutputStream();
    
                    try {
                        is.transferTo(os);
                    } finally {
                        is.close();
                        os.close();
                    }
    
                    clonedDict.setItem(COSName.FONT_FILE2, clonedStream);
    
                    // ---- RETURN CLONED FONT ----
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

        String last3 = digits.substring(len - 3);
        String rest = digits.substring(0, len - 3);

        StringBuilder sb = new StringBuilder();

        int i = rest.length();
        while (i > 0) {
            int start = Math.max(0, i - 2);
            if (sb.length() > 0) {
                sb.insert(0, ',');
            }
            sb.insert(0, rest.substring(start, i));
            i = start;
        }

        sb.append(',').append(last3);
        return sb.toString();
    }

    private static String formatIsoInstantDate(String isoInstant) {
        Instant instant = Instant.parse(isoInstant);
        return DateTimeFormatter.ofPattern("dd-MM-yyyy")
            .withZone(ZoneOffset.UTC)
            .format(instant);
    }
    
    private static String formatIsoInstantDate2(String isoInstant) {
        Instant instant = Instant.parse(isoInstant);
        return DateTimeFormatter.ofPattern("dd/MM/yyyy")
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
            if (v instanceof Number n) {
                replacement = String.format(
                    Locale.ROOT,
                    "%.2f",
                    n.doubleValue()
                );
            } else if ("ADDRSTREAM".equals(e.getKey())) {
                replacement = v.toString();
            } else {
                replacement = pdfEscapeLiteral(v.toString());
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
        try (var is = BoiTemplate.class.getResourceAsStream(path)) {
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
                // store once (avoid duplicates)
                out.putIfAbsent(name.getName(), img.getImage());
            }
        }
    }
}
