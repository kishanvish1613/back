package com.user.pdfservice;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.File;

public class KotakPage4DumpTest {
    @Test
    void dumpPage4Text() throws Exception {
        try (PDDocument doc = PDDocument.load(new File("kotaknew.pdf"))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(4);
            stripper.setEndPage(4);
            System.out.println("=== FULL TEXT OF ORIGINAL PAGE 4 ===");
            System.out.println(stripper.getText(doc));
        }
    }
}

