package com.user.pdfservice.ocr.extractors;

import java.util.regex.*;

public class IfscExtractor {

    public static String extract(String text) {

        String normalized = text.toUpperCase()
                .replace('O', '0');
                
        Matcher m1 = Pattern.compile("\\bHDFC[0-9]{7}").matcher(normalized);
        if(m1.find()) return m1.group();

        Pattern p = Pattern.compile("\\b[A-Z]{4}0[A-Z0-9]{6}\\b");
        Matcher m = p.matcher(normalized);

        return m.find() ? m.group() : null;
    }
}