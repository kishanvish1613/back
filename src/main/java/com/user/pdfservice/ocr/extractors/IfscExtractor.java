package com.user.pdfservice.ocr.extractors;

import java.util.regex.*;

public class IfscExtractor {

    public static String extract(String text) {
        if (text == null || text.isBlank()) return null;

        String normalized = text.replaceAll("\r\n", "\n").replaceAll("\r", "\n");

        // 1. Prioritize explicit labeled IFSC in header
        Matcher labeled = Pattern.compile("(?i)(?:RTGS/NEFT\\s+)?IFSC(?:\\s*CODE)?\\s*[:\\-]?\\s*([A-Z]{4}0[A-Z0-9]{6})").matcher(normalized);
        if (labeled.find()) {
            return labeled.group(1).toUpperCase().replace('O', '0');
        }

        // 2. Generic IFSC code format
        Matcher generic = Pattern.compile("\\b([A-Z]{4}0[A-Z0-9]{6})\\b").matcher(normalized);
        if (generic.find()) {
            return generic.group(1).toUpperCase().replace('O', '0');
        }

        return null;
    }
}
