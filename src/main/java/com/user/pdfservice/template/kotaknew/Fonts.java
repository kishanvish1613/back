package com.user.pdfservice.template.kotaknew;

import com.lowagie.text.pdf.BaseFont;
import java.io.InputStream;

/**
 * Central font holder for Kotak PDFs.
 * Loads SourceSans3 and Helvetica for native OpenPDF generation.
 */
public class Fonts {

    public final BaseFont regular;
    public final BaseFont semiBold;
    public final BaseFont helvetica;

    private Fonts(BaseFont regular, BaseFont semiBold, BaseFont helvetica) {
        this.regular = regular;
        this.semiBold = semiBold;
        this.helvetica = helvetica;
    }

    public static Fonts load() {
        try {
            BaseFont regular = loadFont("/fonts/SourceSans3-Regular.ttf", "SourceSans3-Regular.ttf");
            BaseFont semiBold = loadFont("/fonts/SourceSans3-SemiBold.ttf", "SourceSans3-SemiBold.ttf");
            BaseFont helvetica = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            return new Fonts(regular, semiBold, helvetica);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load fonts", e);
        }
    }

    private static BaseFont loadFont(String resourcePath, String name) {
        try (InputStream is = Fonts.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Font resource not found: " + resourcePath);
            }
            byte[] bytes = is.readAllBytes();
            return BaseFont.createFont(name, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load font " + resourcePath, e);
        }
    }
}
