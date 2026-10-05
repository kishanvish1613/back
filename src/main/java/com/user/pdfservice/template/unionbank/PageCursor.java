package com.user.pdfservice.template.unionbank;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.IOException;

public class PageCursor {
    private final PDDocument doc;
    private final Fonts fonts;
    private PDPage page;
    private PDPageContentStream cs;
    private float y;
    private int pageNumber = 0;

    public PageCursor(PDDocument doc, Fonts fonts, float topY) throws IOException {
        this.doc = doc;
        this.fonts = fonts;
        newPage(topY);
    }

    private void newPage(float topY) throws IOException {
        if (cs != null) cs.close();
        page = new PDPage(PDRectangle.A4); // 595 x 842 — matches the template geometry
        doc.addPage(page);
        cs = new PDPageContentStream(doc, page);
        this.y = topY;
        pageNumber++;
    }

    public void newPage() throws IOException {
        newPage(page.getMediaBox().getHeight() - 40);
    }

    public PDPageContentStream cs() { return cs; }
    public PDPage page() { return page; }
    public float y() { return y; }
    public void setY(float y) { this.y = y; }
    public int pageNumber() { return pageNumber; }
    public PDDocument getDocument() { return doc; }   // added to expose doc

    public void close() throws IOException {
        if (cs != null) cs.close();
    }
}