package com.user.pdfservice.controller;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.user.pdfservice.ocr.OcrProcessingService;
import com.user.pdfservice.ocr.OcrResult;
import com.user.pdfservice.ocr.utils.OcrResponse;
import com.user.pdfservice.service.PdfOcrService;

@RestController
@CrossOrigin(value = "*")
@RequestMapping("/ocr")
public class OcrController {

    private final PdfOcrService ocrService;

    public OcrController(PdfOcrService ocrService) {
        this.ocrService = ocrService;
    }

    /**
     * OCR a PDF or image file.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OcrResponse> ocrPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false) String password) throws Exception {

        System.out.println(">>> Processing OCR for file: " + file.getOriginalFilename());
        System.out.println(">>> File size: " + file.getBytes().length + " bytes");

        String text;
        String name = file.getOriginalFilename().toLowerCase();
        byte[] bytes = file.getBytes();

        try {
            if (name.endsWith(".pdf")) {
                System.out.println(">>> Detected PDF file format");
                boolean encrypted = PdfOcrService.isEncrypted(new ByteArrayInputStream(bytes), password);

                if (encrypted && password == null) {
                    System.out.println(">>> PDF is password protected - no password provided");
                    OcrResponse r = OcrResponse.encrypted("PDF is Password Protected");
                    return ResponseEntity.ok(r);
                }

                text = ocrService.extractTextFromPdf(bytes, password);
            } else {
                text = ocrService.extractTextFromImage(bytes);
            }

            System.out.println(">>> Text extraction completed - " + text.length() + " characters extracted");

            OcrResult result = OcrProcessingService.process(text);
            result.rawText = ""; // clear raw text for response size
            OcrResponse r = OcrResponse.success(result);

            System.out.println(">>> OCR processing completed successfully");
            return ResponseEntity.ok(r);

        } catch (Exception e) {
            System.err.println(">>> OCR processing error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Preview OCR on a test PDF.
     */
    @GetMapping("/preview")
    public ResponseEntity<OcrResponse> previewOcr() throws Exception {
        ClassPathResource pdfResource = new ClassPathResource("test/axisog.pdf");
        byte[] pdf = pdfResource.getInputStream().readAllBytes();

        boolean encrypted = PdfOcrService.isEncrypted(new ByteArrayInputStream(pdf), null);
        if (encrypted) {
            OcrResponse r = OcrResponse.encrypted("PDF is Password Protected");
            return ResponseEntity.ok(r);
        }

        String text = ocrService.extractTextFromPdf(pdf, null);
        OcrResult result = OcrProcessingService.process(text);
        OcrResponse r = OcrResponse.success(result);
        return ResponseEntity.ok(r);
    }

    /**
     * Health check.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OCR Service is running and operational");
    }

    /**
     * Supported formats and constraints.
     */
    @GetMapping("/formats")
    public ResponseEntity<Map<String, Object>> getSupportedFormats() {
        Map<String, Object> info = new HashMap<>();
        info.put("pdf", true);
        info.put("images", true);
        info.put("maxFileSize", "50MB");
        info.put("encryptedPdfSupport", true);
        info.put("description", "Supports PDF and common image formats");
        return ResponseEntity.ok(info);
    }

    /**
     * Service info.
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> getServiceInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("version", "2.0");
        info.put("service", "Bank Statement OCR Service");
        info.put("capabilities", "PDF text extraction, image OCR, encrypted PDF support");
        return ResponseEntity.ok(info);
    }
}