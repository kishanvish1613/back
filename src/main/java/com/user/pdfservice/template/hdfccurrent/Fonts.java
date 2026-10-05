package com.user.pdfservice.template.hdfccurrent;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class Fonts {
    public final PDFont regular;
    public final PDFont bold;
    public final PDFont italic;
    public final PDFont boldItalic;

    public Fonts(PDDocument doc) {
        this.regular = PDType1Font.TIMES_ROMAN;
        this.bold = PDType1Font.TIMES_BOLD;
        this.italic = PDType1Font.TIMES_ITALIC;
        this.boldItalic = PDType1Font.TIMES_BOLD_ITALIC;
    }
}


