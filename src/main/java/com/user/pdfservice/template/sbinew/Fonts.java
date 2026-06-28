package com.user.pdfservice.template.sbinew;

import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

/**
 * Central font holder for Kotak PDFs.
 * Keeps font logic out of templates.
 */
public class Fonts {

    public final PDType0Font regular;
    public final PDType0Font semiBold;

    public final PDType1Font helvetica;

    private Fonts(PDType0Font regular, PDType0Font semiBold) {
        this.semiBold = semiBold;
        this.regular = regular;
        this.helvetica = PDType1Font.HELVETICA;
    }

    public static Fonts load(PDDocument doc) {
        return new Fonts(
                loadFont(doc, "/fonts/SourceSans3-Regular.ttf"),
                loadFont(doc, "/fonts/SourceSans3-SemiBold.ttf"));
    }

    private static PDType0Font loadFont(PDDocument doc, String path) {
        try (InputStream is = Fonts.class.getResourceAsStream(path)) {

            if (is == null) {
                throw new IllegalStateException("Font not found: " + path);
            }

            // true = subset → CID TrueType → Identity-H
            return PDType0Font.load(doc, is, true);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load font: " + path, e);
        }
    }
}