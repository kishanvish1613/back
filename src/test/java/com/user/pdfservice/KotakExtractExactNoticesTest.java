package com.user.pdfservice;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class KotakExtractExactNoticesTest {
    @Test
    void extractExactNotices() throws Exception {
        try (PDDocument doc = PDDocument.load(new File("kotaknew.pdf"))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(4);
            stripper.setEndPage(4);
            String text = stripper.getText(doc);
            String[] lines = text.split("\\r?\\n");
            
            System.out.println("=== LINES DUMP ===");
            for (int i = 0; i < lines.length; i++) {
                System.out.printf("[%02d] %s%n", i, lines[i]);
            }
        }
    }
}

