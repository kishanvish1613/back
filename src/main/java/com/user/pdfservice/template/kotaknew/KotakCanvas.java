package com.user.pdfservice.template.kotaknew;

import com.lowagie.text.Image;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;

/**
 * Adapter wrapping OpenPDF's PdfContentByte to provide drawing operations
 * matching the Kotak statement coordinate layout.
 */
public class KotakCanvas {

    private PdfContentByte cb;

    public KotakCanvas(PdfContentByte cb) {
        this.cb = cb;
    }

    public void setContentByte(PdfContentByte cb) {
        this.cb = cb;
    }

    public PdfContentByte getPdfContentByte() {
        return cb;
    }

    public void beginText() {
        cb.beginText();
    }

    public void endText() {
        cb.endText();
    }

    public void setFont(BaseFont font, float size) {
        cb.setFontAndSize(font, size);
    }

    public void showText(String text) {
        if (text != null) {
            cb.showText(text);
        }
    }

    public void setTextMatrix(double a, double b, double c, double d, double e, double f) {
        cb.setTextMatrix((float) a, (float) b, (float) c, (float) d, (float) e, (float) f);
    }

    public void setTextMatrix(double e, double f) {
        cb.setTextMatrix((float) e, (float) f);
    }

    public void setNonStrokingColor(float gray) {
        cb.setGrayFill(gray);
    }

    public void setNonStrokingColor(int gray) {
        cb.setGrayFill((float) gray);
    }

    public void setNonStrokingColor(float r, float g, float b) {
        cb.setRGBColorFillF(r, g, b);
    }

    public void setStrokingColor(float gray) {
        cb.setGrayStroke(gray);
    }

    public void setStrokingColor(int gray) {
        cb.setGrayStroke((float) gray);
    }

    public void setStrokingColor(float r, float g, float b) {
        cb.setRGBColorStrokeF(r, g, b);
    }

    public void addRect(float x, float y, float w, float h) {
        cb.rectangle(x, y, w, h);
    }

    public void fill() {
        cb.fill();
    }

    public void stroke() {
        cb.stroke();
    }

    public void fillAndStroke() {
        cb.fillStroke();
    }

    public void moveTo(float x, float y) {
        cb.moveTo(x, y);
    }

    public void lineTo(float x, float y) {
        cb.lineTo(x, y);
    }

    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        cb.curveTo(x1, y1, x2, y2, x3, y3);
    }

    public void setLineWidth(float w) {
        cb.setLineWidth(w);
    }

    public void setLineCapStyle(int cap) {
        cb.setLineCap(cap);
    }

    public void appendRawCommands(String raw) {
        if (raw != null) {
            cb.setLiteral(raw);
        }
    }

    public void saveGraphicsState() {
        cb.saveState();
    }

    public void restoreGraphicsState() {
        cb.restoreState();
    }

    public void setGState(PdfGState gs) {
        if (gs != null) {
            cb.setGState(gs);
        }
    }

    public void drawImage(Image img, float x, float y, float w, float h) throws Exception {
        if (img != null) {
            img.setAbsolutePosition(x, y);
            img.scaleAbsolute(w, h);
            cb.addImage(img);
        }
    }

    public void drawImage(Image img, float a, float b, float c, float d, float e, float f) throws Exception {
        if (img != null) {
            cb.addImage(img, a, b, c, d, e, f);
        }
    }

    public void close() {
        // No-op for PdfContentByte
    }
}
