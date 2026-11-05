package com.user.pdfservice;

import com.user.pdfservice.template.kotaknew.Fonts;
import org.junit.jupiter.api.Test;

public class KotakFontWidthTest {
    @Test
    void testFontWidths() throws Exception {
        Fonts fonts = Fonts.load();
        String line1 = "RBI mandates Positive Pay for high-value cheques from Jan 1, 2021. Customers must submit cheque details via Net/Mobile Banking or at the branch on";
        float w = fonts.regular.getWidthPoint(line1, 7.0f);
        System.out.println("Line 1 text width at 7.0pt = " + w);

        System.out.println("Standard line height = 7.68f, gap between bullets = 10.0f");
    }
}
