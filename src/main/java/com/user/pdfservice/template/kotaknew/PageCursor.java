package com.user.pdfservice.template.kotaknew;

import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.*;

import java.io.IOException;
import java.util.Map;

public class PageCursor {

    private final Document doc;
    private final PdfWriter writer;
    private final Fonts fonts;
    private final PdfTemplate tpl;
    private final KotakCanvas canvas;
    private float y;

    private final float NEW_PAGE_HEADER_START = 737.5f;
    private final float NEW_PAGE_TXN_START = 701.32f;
    private int pgn = 1;

    private Runnable pageHeaderRenderer;
    private Runnable pageFooterRenderer;

    public PageCursor(
            Document doc,
            PdfWriter writer,
            Fonts fonts,
            PdfTemplate tpl,
            KotakCanvas canvas,
            float startY
    ) {
        this.doc = doc;
        this.writer = writer;
        this.fonts = fonts;
        this.tpl = tpl;
        this.canvas = canvas;
        this.y = startY;
    }

    public void setPageHeaderRenderer(Runnable pageHeaderRenderer) {
        this.pageHeaderRenderer = pageHeaderRenderer;
    }

    public void setPageFooterRenderer(Runnable pageFooterRenderer) {
        this.pageFooterRenderer = pageFooterRenderer;
    }

    public KotakCanvas cs() {
        return canvas;
    }

    public PdfWriter writer() {
        return writer;
    }

    public float y() {
        return y;
    }

    public void y(float newY) {
        this.y = newY;
    }

    public void moveDown(float dy) {
        y -= dy;
    }

    public int getPageNumber() {
        return pgn;
    }

    public void ensureSpace(float requiredHeight) throws Exception {
        if (y - requiredHeight < 36f) {
            nextPage();
            renderTransactionTableHeader();
        }
    }

    public void nextPage() throws Exception {
        if (pageFooterRenderer != null) {
            pageFooterRenderer.run();
        }
        doc.newPage();
        pgn++;
        if (pageHeaderRenderer != null) {
            pageHeaderRenderer.run();
        }
        y = NEW_PAGE_TXN_START;
    }

    public void nextPageWithImages(
            Map<String, Image> extractedImages,
            int start,
            int end
    ) throws Exception {
        if (pageFooterRenderer != null) {
            pageFooterRenderer.run();
        }
        doc.newPage();
        pgn++;

        if (pageHeaderRenderer != null) {
            pageHeaderRenderer.run();
        }

        y = NEW_PAGE_TXN_START;
    }

    public void close() throws Exception {
        if (pageFooterRenderer != null) {
            pageFooterRenderer.run();
        }
    }

    public void renderTransactionTableHeader() throws Exception {
        float tableLeft = 36f;
        float tableRight = 559f;

        float redBarHeight = 25f;
        float headerRowHeight = 25f;

        float topY = NEW_PAGE_HEADER_START;
        float headerRowY = topY - headerRowHeight;

        float[] colWidths = {
                31.18f,
                49f,
                155.47f,
                80.19f,
                69.05f,
                69.05f,
                69.05f,
        };

        float[] colTextX = {
                44f,
                75.18f,
                124.19f,
                279.66f,
                354.85f,
                428.9f,
                497.95f,
        };

        canvas.setNonStrokingColor(0.92941f, 0.1098f, 0.14118f);
        canvas.addRect(tableLeft, topY, tableRight - tableLeft, redBarHeight);
        canvas.fill();

        canvas.beginText();
        canvas.appendRawCommands("1 0 0 1 222.79 " + (topY + 7.5f) + " Tm\n");
        canvas.setFont(fonts.regular, 12);
        canvas.setNonStrokingColor(1f, 1f, 1f);
        canvas.showText("Savings Account Transactions");
        canvas.setNonStrokingColor(0f);
        canvas.endText();

        canvas.setNonStrokingColor(0.62353f, 0.63137f, 0.64314f);

        float x = tableLeft;
        for (float w : colWidths) {
            canvas.addRect(x, headerRowY, w, headerRowHeight);
            canvas.fill();
            x += w;
        }

        canvas.setLineWidth(0.5f);
        canvas.setLineCapStyle(2);
        canvas.setStrokingColor(1f, 1f, 1f);

        x = tableLeft;
        for (float w : colWidths) {
            x += w;
            canvas.moveTo(x, headerRowY);
            canvas.lineTo(x, headerRowY + headerRowHeight);
            canvas.stroke();
        }

        String[] COL_HEADERS = {
                "#",
                "Date",
                "Description",
                "Chq/Ref. No.",
                "Withdrawal (Dr.)",
                "Deposit (Cr.)",
                "Balance",
        };

        for (int i = 0; i < colTextX.length; i++) {
            canvas.beginText();
            canvas.appendRawCommands(
                    "1 0 0 1 " + colTextX[i] + " " + (topY - 17f) + " Tm\n"
            );
            canvas.setFont(fonts.regular, 9);
            canvas.setNonStrokingColor(1f, 1f, 1f);
            canvas.showText(COL_HEADERS[i]);
            canvas.setNonStrokingColor(0);
            canvas.endText();
        }
    }
}
