package com.user.pdfservice.ocr;

import java.util.Map;

public final class BankDetector {

    private static final Map<String, String> IFSC_PREFIX_TO_BANK = Map.ofEntries(
            Map.entry("SBIN", "SBI"),
            Map.entry("UTIB", "AXIS"),
            Map.entry("KKBK", "KOTAK"),
            Map.entry("BKID", "BOI"),
            Map.entry("HDFC", "HDFC")
    );

    public static String detectFromIFSC(String ifsc) {
        if (ifsc == null || ifsc.length() < 4)
            return null;
        return IFSC_PREFIX_TO_BANK.get(ifsc.substring(0, 4));
    }

    public static String detectFromText(String text) {
        if (text == null)
            return null;
        String t = text.toUpperCase();

        if (t.contains("STATE BANK OF INDIA") || t.contains("SBI"))
            return "SBI";
        if (t.contains("AXIS BANK"))
            return "AXIS";
        if (t.contains("KOTAK"))
            return "KOTAK";

        return null;
    }

}
