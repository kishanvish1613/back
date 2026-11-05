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

    private Tesseract tesseract;

    public PdfOcrService() {
        try {
            this.tesseract = new Tesseract();
            String tessPath = System.getenv("TESSDATA_PREFIX");
            if (tessPath == null || tessPath.isBlank() || !new java.io.File(tessPath).exists()) {
                String[] possiblePaths = {
                    "C:\\Program Files\\Tesseract-OCR\\tessdata",
                    "C:\\Program Files (x86)\\Tesseract-OCR\\tessdata",
                    System.getProperty("user.home") + "\\AppData\\Local\\Tesseract-OCR\\tessdata"
                };
                for (String p : possiblePaths) {
                    if (new java.io.File(p).exists()) {
                        tessPath = p;
                        break;
                    }
                }
            }
            if (tessPath != null && new java.io.File(tessPath).exists()) {
                this.tesseract.setDatapath(tessPath);
            }
            this.tesseract.setLanguage("eng");
            this.tesseract.setOcrEngineMode(1); // LSTM
            this.tesseract.setPageSegMode(3);   // Auto
        } catch (Throwable t) {
            System.err.println("Warning: Tesseract initialization deferred/skipped: " + t.getMessage());
            this.tesseract = null;
        }
    }

    /* ===================== PDF OCR ===================== */

    public String extractTextFromPdf(byte[] pdfBytes, String password) throws Exception {

        try (PDDocument document = PDDocument.load(pdfBytes, password)) {
            int totalPages = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            String directText;
            if (totalPages <= 5) {
                stripper.setStartPage(1);
                stripper.setEndPage(totalPages);
                directText = stripper.getText(document);
            } else {
                stripper.setStartPage(1);
                stripper.setEndPage(3);
                String headerText = stripper.getText(document);

                PDFTextStripper lastStripper = new PDFTextStripper();
                lastStripper.setSortByPosition(true);
                lastStripper.setStartPage(Math.max(4, totalPages - 1));
                lastStripper.setEndPage(totalPages);
                String lastPageText = lastStripper.getText(document);

                directText = headerText + "\n--- LAST PAGE ---\n" + lastPageText;
            }
            if (directText != null && directText.trim().length() > 50) {
                return directText;
            }

            // OCR fallback for scanned image PDFs
            if (tesseract == null) {
                return directText != null ? directText : "";
            }

            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder result = new StringBuilder();

            Set<Integer> pagesToOcr = new LinkedHashSet<>();
            pagesToOcr.add(0); // first page
            if (totalPages > 1) {
                pagesToOcr.add(totalPages - 1); // last page
            }

            for (int pageIndex : pagesToOcr) {
                try {
                    BufferedImage image = renderer.renderImageWithDPI(pageIndex, 300);
                    BufferedImage processed = preprocessImage(image);
                    String text = tesseract.doOCR(processed);

                    result.append("\n--- Page ")
                          .append(pageIndex + 1)
                          .append(" ---\n")
                          .append(text);
                } catch (Throwable ocrEx) {
                    System.err.println("Warning: OCR on page " + (pageIndex + 1) + " failed: " + ocrEx.getMessage());
                }
            }

            return result.toString();
        }
    }

    /* ===================== IMAGE OCR ===================== */

    public String extractTextFromImage(byte[] imageBytes) throws Exception {
        if (tesseract == null) {
            throw new IllegalStateException("Tesseract OCR engine is not configured on the server");
        }

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
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