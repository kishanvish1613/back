package com.user.pdfservice.ocr.utils;

import java.util.regex.*;

public class RegexUtils {

    public static String first(String text, String regex) {
        Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        return m.find() ? m.group() : null;
    }
}
