package com.user.pdfservice.template.axis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

public final class PdfHeaderAdder {

    private static final float TOP_MARGIN = 5f;

    private PdfHeaderAdder() {
    }

    /**
     * Adds a palette-based PNG header to every page.
     * Preserves Indexed /DeviceRGB.
     */
    public static byte[] addHeader(byte[] pdfBytes, String headerPng)
            throws Exception {

        PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfStamper stamper = new PdfStamper(reader, out);

        // Load PNG bytes directly
        InputStream is = PdfHeaderAdder.class.getResourceAsStream(headerPng);
        if (is == null) {
            throw new IllegalStateException(
                    "Header image not found: " + headerPng);
        }

        byte[] pngBytes = is.readAllBytes();

        Image header = Image.getInstance(pngBytes);

        float scalePercent = 70f; // change this value

        header.scalePercent(scalePercent);

        for (int i = 1; i <= 1; i++) {
            Rectangle pageSize = reader.getPageSize(i);

            // Center horizontally
            float x = (pageSize.getWidth()
                    - header.getScaledWidth() - 16) / 2f;

            // Position at top
            float y = pageSize.getHeight()
                    - header.getScaledHeight()
                    - TOP_MARGIN;

            PdfContentByte canvas = stamper.getOverContent(i);

            header.setAbsolutePosition(x, y);
            canvas.addImage(header);
        }

        stamper.close();
        reader.close();

        return out.toByteArray();
    }
}