package com.user.pdfservice.template.axis;

import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;

public final class Fonts {

    public final Font helvetica;
    public final Font timesBold;
    public final Font timesRoman;

    public Fonts() throws Exception {

        // Helvetica (Type 1, WinAnsi, NOT embedded)
        BaseFont helveticaBase = BaseFont.createFont(
                BaseFont.HELVETICA,
                BaseFont.WINANSI,
                BaseFont.NOT_EMBEDDED
        );

        // Times-Bold (Type 1, WinAnsi, NOT embedded)
        BaseFont timesBoldBase = BaseFont.createFont(
                BaseFont.TIMES_BOLD,
                BaseFont.WINANSI,
                BaseFont.NOT_EMBEDDED
        );

        // Times-Roman (Type 1, WinAnsi, NOT embedded)
        BaseFont timesRomanBase = BaseFont.createFont(
                BaseFont.TIMES_ROMAN,
                BaseFont.WINANSI,
                BaseFont.NOT_EMBEDDED
        );

        // Sizes chosen to match banking PDFs
        this.helvetica  = new Font(helveticaBase, 12);
        this.timesBold  = new Font(timesBoldBase, 10);
        this.timesRoman = new Font(timesRomanBase, 9);
    }
}