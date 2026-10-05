package com.user.pdfservice;

import com.user.pdfservice.template.kotaknew.Fonts;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.File;

public class KotakFontWidthTest {
    @Test
    void testFontWidths() throws Exception {
        try (PDDocument doc = PDDocument.load(new File("kotaknew.pdf"))) {
            Fonts fonts = Fonts.load(doc);
            String line1 = "RBI mandates Positive Pay for high-value cheques from Jan 1, 2021. Customers must submit cheque details via Net/Mobile Banking or at the branch on";
            float w = (fonts.regular.getStringWidth(line1) / 1000f) * 7.0f;
            System.out.println("Line 1 text width at 7.0pt = " + w);

            // Let's check line heights and gaps in original
            // P4 bullet 1: from top = 692.32
            // P4 bullet 2: from top = 666.96 (delta = 25.36) -> 2 lines of 7.68 + gap of 10.0!
            // P4 bullet 3: from top = 641.60 (delta = 25.36) -> 2 lines of 7.68 + gap of 10.0!
            // P4 bullet 4: from top = 600.88 (delta = 40.72) -> 4 lines of 7.68 + gap of 10.0!
            System.out.println("Standard line height = 7.68f, gap between bullets = 10.0f");
        }
    }
}

