package com.user.pdfservice.ocr.utils;

import java.util.regex.*;

public class AddressUtils {

    public static String extractPincode(String address) {
        if (address == null) return null;
        Matcher m = Pattern.compile("\\b\\d{6}\\b").matcher(address);
        return m.find() ? m.group() : null;
    }
}