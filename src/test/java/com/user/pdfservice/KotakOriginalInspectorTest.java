package com.user.pdfservice;

import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.DrawObject;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class KotakOriginalInspectorTest {

    @Test
    void inspectOriginalPdf() throws Exception {
        File file = new File("kotaknew.pdf");
        if (!file.exists()) {
            System.out.println("kotaknew.pdf not found in root");
            return;
        }

        try (PDDocument doc = PDDocument.load(file)) {
            System.out.println("=== TOTAL PAGES: " + doc.getNumberOfPages() + " ===");

            for (int p = 0; p < doc.getNumberOfPages(); p++) {
                System.out.println("\n----------------- PAGE " + (p + 1) + " -----------------");
                final int pageNum = p + 1;

                PDFTextStripper stripper = new PDFTextStripper() {
                    @Override
                    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
                        if (!textPositions.isEmpty()) {
                            TextPosition first = textPositions.get(0);
                            System.out.printf("P%d Text: [%s] at X=%.2f, Y=%.2f (from top=%.2f), font=%s, size=%.2f%n",
                                    pageNum,
                                    text.trim(),
                                    first.getXDirAdj(),
                                    first.getYDirAdj(), // from top
                                    842f - first.getYDirAdj(), // Y from bottom
                                    first.getFont().getName(),
                                    first.getFontSizeInPt()
                            );
                        }
                    }
                };
                stripper.setStartPage(p + 1);
                stripper.setEndPage(p + 1);
                stripper.getText(doc);
            }
        }
    }
}

