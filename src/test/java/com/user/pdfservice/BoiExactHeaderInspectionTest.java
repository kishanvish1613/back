package com.user.pdfservice;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class BoiExactHeaderInspectionTest {

    @Test
    public void inspectHeader() throws Exception {
        File pdfFile = new File("src/main/resources/test/boi.pdf");
        if (!pdfFile.exists()) {
            pdfFile = new File("boi.pdf");
        }
        if (!pdfFile.exists()) return;
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void processTextPosition(TextPosition text) {
                    if (text.getYDirAdj() < 400.0f) { // only header region
                        System.out.printf("x=%.2f, y=%.2f, fontSize=%.2fpt, font=%s, text='%s'%n",
                                text.getXDirAdj(), text.getYDirAdj(), text.getFontSizeInPt(),
                                text.getFont().getName(), text.getUnicode());
                    }
                    super.processTextPosition(text);
                }
            };
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            stripper.getText(doc);
        }
    }
}

