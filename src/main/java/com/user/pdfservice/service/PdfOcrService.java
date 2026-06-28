package com.user.pdfservice.service;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import net.sourceforge.tess4j.Tesseract;

@Service
public class PdfOcrService {

    private static final String TESSDATA_PATH =
            "C:\\Program Files\\Tesseract-OCR\\tessdata";

    private final Tesseract tesseract;

    public PdfOcrService() {
        this.tesseract = new Tesseract();
        this.tesseract.setDatapath(TESSDATA_PATH);
        this.tesseract.setLanguage("eng");

        this.tesseract.setOcrEngineMode(1); // LSTM
        this.tesseract.setPageSegMode(3);   // Auto
    }

    /* ===================== PDF OCR ===================== */

    public String extractTextFromPdf(byte[] pdfBytes, String password) throws Exception {

        try (PDDocument document = PDDocument.load(pdfBytes, password)) {

            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();
            int endPage = 1;
            if(totalPages > 1) endPage += 1;
            if(totalPages > 2) endPage += 1;

            stripper.setStartPage(1);
            stripper.setEndPage(endPage);

            String directText = stripper.getText(document);
            if (directText != null && directText.trim().length() > 200) {
                return directText;
            }

            // OCR fallback
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder result = new StringBuilder();

            Set<Integer> pagesToOcr = new LinkedHashSet<>();
            pagesToOcr.add(0); // first page

            if (totalPages > 1) {
                pagesToOcr.add(totalPages - 1); // last page
            } else if(totalPages > 2){
                pagesToOcr.add(totalPages - 2);
            }

            for (int pageIndex : pagesToOcr) {
                BufferedImage image =
                        renderer.renderImageWithDPI(pageIndex, 300);

                BufferedImage processed = preprocessImage(image);
                String text = tesseract.doOCR(processed);

                result.append("\n--- Page ")
                      .append(pageIndex + 1)
                      .append(" ---\n")
                      .append(text);
            }

            return result.toString();
        }
    }

    /* ===================== IMAGE OCR ===================== */

    public String extractTextFromImage(byte[] imageBytes) throws Exception {

        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(imageBytes));

        if (image == null) {
            throw new IllegalArgumentException("Invalid image file");
        }

        BufferedImage processed = preprocessImage(image);
        return tesseract.doOCR(processed);
    }

    /* ===================== PREPROCESSING ===================== */

    private BufferedImage preprocessImage(BufferedImage src) {
        BufferedImage gray = new BufferedImage(
                src.getWidth(),
                src.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );

        Graphics2D g = gray.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        return gray;
    }
    
    public static boolean isEncrypted(InputStream inputStream, String password) {
        try (PDDocument document = PDDocument.load(inputStream)) {
            return false;
        } catch (InvalidPasswordException e) {
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read PDF", e);
        }
    }

}