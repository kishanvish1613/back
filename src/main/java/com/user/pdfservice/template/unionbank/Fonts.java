package com.user.pdfservice.template.unionbank;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import java.io.InputStream;

public class Fonts {
    public final PDFont regular;
    public final PDFont bold;

    public Fonts(PDDocument doc) throws Exception {
        // Using the fonts you already have in src/main/resources/fonts/
        InputStream regularStream = getClass().getResourceAsStream("/fonts/Helvetica.ttf");
        InputStream boldStream = getClass().getResourceAsStream("/fonts/SourceSans3-SemiBold.ttf");
        this.regular = PDType0Font.load(doc, regularStream);
        this.bold = PDType0Font.load(doc, boldStream);
    }
}