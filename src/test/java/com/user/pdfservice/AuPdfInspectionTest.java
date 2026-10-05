package com.user.pdfservice;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AuPdfInspectionTest {

    @Test
    public void inspectAuPdf() throws Exception {
        File pdfFile = new File("C:/Users/real/.gemini/antigravity/brain/5e191c5a-8270-4075-8fa4-b9ceab0cc3cf/.user_uploaded/media_1790250362396.pdf");
        if (!pdfFile.exists()) {
            System.out.println("File not found: " + pdfFile.getAbsolutePath());
            return;
        }

        try (PDDocument doc = PDDocument.load(pdfFile)) {
            System.out.println("Page count = " + doc.getNumberOfPages());
            PDPage page1 = doc.getPage(0);
            System.out.println("Page 1 MediaBox = " + page1.getMediaBox());
            System.out.println("Page 1 CropBox = " + page1.getCropBox());

            // Extract page 1 image
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage page1Img = renderer.renderImageWithDPI(0, 300);

            // Save rendered page 1
            File p1Out = new File("au_sample_page_1.png");
            ImageIO.write(page1Img, "PNG", p1Out);
            System.out.println("Saved page 1 image to " + p1Out.getAbsolutePath());

            // Crop logo from top right
            // Page is 595 x 842 pt -> at 300 DPI: scale = 300 / 72 = 4.1667
            // Logo is at top right: x ~ 440 to 580 pt, y ~ 5 to 45 pt
            int imgW = page1Img.getWidth();
            int imgH = page1Img.getHeight();
            float scale = (float) imgW / page1.getMediaBox().getWidth();

            int logoX = (int) (440 * scale);
            int logoY = (int) (5 * scale);
            int logoW = (int) ((575 - 440) * scale);
            int logoH = (int) (45 * scale);

            if (logoX + logoW <= imgW && logoY + logoH <= imgH) {
                BufferedImage logoImg = page1Img.getSubimage(logoX, logoY, logoW, logoH);
                File logoOut = new File("src/main/resources/templates/au_logo.png");
                logoOut.getParentFile().mkdirs();
                ImageIO.write(logoImg, "PNG", logoOut);
                System.out.println("Extracted AU logo to " + logoOut.getAbsolutePath());
            }

            // Inspect text positions
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
                    for (TextPosition tp : textPositions) {
                        if (tp.getYDirAdj() < 260) {
                            System.out.printf("POS: x=%.2f, y=%.2f, w=%.2f, h=%.2f, font=%s, size=%.2f | Text: %s\n",
                                    tp.getXDirAdj(), tp.getYDirAdj(), tp.getWidthDirAdj(), tp.getHeightDir(),
                                    tp.getFont().getName(), tp.getFontSizeInPt(), tp.getUnicode());
                        }
                    }
                    super.writeString(text, textPositions);
                }
            };
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            String text = stripper.getText(doc);
            System.out.println("=== PAGE 1 TEXT ===");
            System.out.println(text);
        }
    }
}

