package com.user.pdfservice;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class BoiPdfVisualRenderTest {

    @Test
    public void renderPages() throws Exception {
        File pdfFile = new File("test_boi_current_generated.pdf");
        if (!pdfFile.exists()) {
            System.out.println("Skipping visual render test as test_boi_current_generated.pdf does not exist.");
            return;
        }
        File artifactsDir = new File("target/test-render-output");
        artifactsDir.mkdirs();

        try (PDDocument doc = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, 150);
                File out = new File(artifactsDir, "boi_current_page_" + (i + 1) + ".png");
                ImageIO.write(img, "png", out);
                System.out.println("Saved rendered page " + (i + 1) + " to: " + out.getAbsolutePath());
            }
        }
    }
}

