package com.user.pdfservice.template.kotak;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public final class PdfHeaderAdder {

    private static final float HEADER_HEIGHT = 31.2f; // px/pt – must match your design

    private PdfHeaderAdder() {}

    /**
     * Adds a header image to every page of the PDF.
     *
     * @param pdfBytes   original PDF bytes
     * @param headerPng  classpath path, e.g. "/templates/kotak_header.png"
     */
    public static byte[] addHeader(byte[] pdfBytes, String headerPng) throws Exception {

        try (
            PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes));
            InputStream headerStream = PdfHeaderAdder.class.getResourceAsStream(headerPng)
        ) {
            if (headerStream == null) {
                throw new IllegalStateException("Header image not found: " + headerPng);
            }

            PDImageXObject headerImage =
                PDImageXObject.createFromByteArray(
                    document,
                    headerStream.readAllBytes(),
                    "header"
                );

            for (PDPage page : document.getPages()) {
                PDRectangle pageSize = page.getMediaBox();

                float pageWidth = pageSize.getWidth();
                float pageHeight = pageSize.getHeight();

                try (PDPageContentStream cs =
                    new PDPageContentStream(
                        document,
                        page,
                        AppendMode.APPEND,
                        true,
                        true
                    )) {

                    cs.drawImage(
                        headerImage,
                        0,                              // x
                        pageHeight - HEADER_HEIGHT - 4,     // y (top of page)
                        pageWidth,                      // stretch full width
                        HEADER_HEIGHT                   // fixed height
                    );
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
