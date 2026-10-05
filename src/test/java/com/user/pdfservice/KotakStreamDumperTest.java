package com.user.pdfservice;

import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class KotakStreamDumperTest {

    @Test
    void dumpStreamOps() throws Exception {
        File file = new File("kotaknew.pdf");
        if (!file.exists()) return;

        try (PDDocument doc = PDDocument.load(file)) {
            System.out.println("Total pages in original: " + doc.getNumberOfPages());
            for (int p = 0; p < doc.getNumberOfPages(); p++) {
                final int pageNum = p + 1;
                System.out.println("\n\n=================== PAGE " + pageNum + " ===================");
                PDPage page = doc.getPage(p);

                PDFGraphicsStreamEngine engine = new PDFGraphicsStreamEngine(page) {
                    @Override
                    public void drawImage(PDImage pdImage) throws IOException {
                        System.out.printf("P%d DRAW IMAGE: %dx%d at CTM: %s%n",
                                pageNum, pdImage.getWidth(), pdImage.getHeight(), getGraphicsState().getCurrentTransformationMatrix());
                    }

                    @Override
                    public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) throws IOException {
                        System.out.printf("P%d RECT: (%.2f, %.2f) -> (%.2f, %.2f) -> (%.2f, %.2f) -> (%.2f, %.2f)%n",
                                pageNum, p0.getX(), p0.getY(), p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY());
                    }

                    @Override
                    public void clip(int windingRule) throws IOException {}

                    @Override
                    public void moveTo(float x, float y) throws IOException {
                        System.out.printf("P%d moveTo: (%.2f, %.2f)%n", pageNum, x, y);
                    }

                    @Override
                    public void lineTo(float x, float y) throws IOException {
                        System.out.printf("P%d lineTo: (%.2f, %.2f)%n", pageNum, x, y);
                    }

                    @Override
                    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) throws IOException {
                        System.out.printf("P%d curveTo: (%.2f, %.2f), (%.2f, %.2f), (%.2f, %.2f)%n", pageNum, x1, y1, x2, y2, x3, y3);
                    }

                    @Override
                    public Point2D getCurrentPoint() throws IOException {
                        return new Point2D.Float(0, 0);
                    }

                    @Override
                    public void closePath() throws IOException {
                        System.out.printf("P%d closePath%n", pageNum);
                    }

                    @Override
                    public void endPath() throws IOException {}

                    @Override
                    public void strokePath() throws IOException {
                        System.out.printf("P%d strokePath (color=%s, width=%.2f)%n", pageNum, getGraphicsState().getStrokingColor(), getGraphicsState().getLineWidth());
                    }

                    @Override
                    public void fillPath(int windingRule) throws IOException {
                        System.out.printf("P%d fillPath (color=%s)%n", pageNum, getGraphicsState().getNonStrokingColor());
                    }

                    @Override
                    public void fillAndStrokePath(int windingRule) throws IOException {
                        System.out.printf("P%d fillAndStrokePath%n", pageNum);
                    }

                    @Override
                    public void shadingFill(COSName shadingName) throws IOException {}
                };
                engine.processPage(page);

                System.out.println("--- TEXT ON PAGE " + pageNum + " ---");
                PDFTextStripper stripper = new PDFTextStripper() {
                    @Override
                    protected void processTextPosition(TextPosition text) {
                        System.out.printf("P%d TEXT: '%s' at (x=%.2f, y=%.2f, w=%.2f, h=%.2f, size=%.2f, font=%s)%n",
                                pageNum, text.getUnicode(), text.getXDirAdj(), text.getYDirAdj(), text.getWidthDirAdj(), text.getHeightDir(), text.getFontSizeInPt(), text.getFont().getName());
                        super.processTextPosition(text);
                    }
                };
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);
                stripper.getText(doc);
            }
        }
    }
}

