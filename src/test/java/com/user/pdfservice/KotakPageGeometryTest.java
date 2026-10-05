package com.user.pdfservice;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class KotakPageGeometryTest {

    @Test
    void inspectPage3And4Geometry() throws Exception {
        File file = new File("kotaknew.pdf");
        if (!file.exists()) return;

        try (PDDocument doc = PDDocument.load(file)) {
            System.out.println("=== PAGE 3 & 4 DETAILED GEOMETRY ===");

            for (int p : new int[]{2, 3}) { // Page 3 (index 2) and Page 4 (index 3)
                System.out.println("\n=================== PAGE " + (p + 1) + " ===================");
                PDPage page = doc.getPage(p);

                PDFTextStripper stripper = new PDFTextStripper() {
                    @Override
                    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
                        if (!textPositions.isEmpty()) {
                            TextPosition first = textPositions.get(0);
                            float yBottom = 842f - first.getYDirAdj();
                            System.out.printf("P%d [%s] at X=%.2f, Y_bottom=%.2f, (Y_top=%.2f), font=%s, size=%.2f%n",
                                    (p + 1), text.trim(), first.getXDirAdj(), yBottom, first.getYDirAdj(),
                                    first.getFont().getName(), first.getFontSizeInPt());
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

