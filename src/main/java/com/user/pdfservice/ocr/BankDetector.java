package com.user.pdfservice.ocr;

import java.util.Map;

public final class BankDetector {

    private static final Map<String, String> IFSC_PREFIX_TO_BANK = Map.ofEntries(
            Map.entry("SBIN", "SBI"),
            Map.entry("UTIB", "AXIS"),
            Map.entry("KKBK", "KOTAKNEW"),
            Map.entry("BKID", "BOI"),
            Map.entry("HDFC", "HDFC"),
            Map.entry("UBIN", "UNIONBANK"),
            Map.entry("AUBL", "AU")
    );

    public static String detectFromIFSC(String ifsc) {
        if (ifsc == null || ifsc.length() < 4)
            return null;
        return IFSC_PREFIX_TO_BANK.get(ifsc.substring(0, 4).toUpperCase());
    }

    public static String detectFromText(String text) {
        if (text == null || text.isBlank())
            return null;
        String t = text.toUpperCase();

        // 1. High-confidence multi-word brand signatures
        if (t.contains("AU SMALL FINANCE BANK") || t.contains("AUBANK.IN") || t.contains("AU CURRENT ACCOUNT") || t.contains("AU SMALL FINANCE"))
            return "AU";
        if (t.contains("UNION BANK OF INDIA") || t.contains("UNIONBANKOFINDIA") || t.contains("UNION BANK"))
            return "UNIONBANK";
        if (t.contains("BANK OF INDIA") || t.contains("BANKOFINDIA.CO.IN") || t.contains("RELATIONSHIP BEYOND BANKING"))
            return "BOI";
        if (t.contains("HDFC BANK") || t.contains("HDFCBANK.COM") || t.contains("WE UNDERSTAND YOUR WORLD") || t.contains("BIZ PRO PLUS ACCOUNT"))
            return "HDFC";
        if (t.contains("KOTAK MAHINDRA BANK") || t.contains("KOTAK.BANK.IN") || t.contains("KOTAK811") || t.contains("HAUSLA HAI TOH HO JAYEGA"))
            return "KOTAKNEW";
        if (t.contains("STATE BANK OF INDIA") || t.contains("ONLINESBI.COM") || t.contains("ONLINESBI.SBM"))
            return "SBI";
        if (t.contains("AXIS BANK") || t.contains("AXISBANK.COM") || t.contains("AXIS BANK LTD"))
            return "AXIS";

        return null;
    }

    public static String detect(String ifsc, String text) {
        // Priority 1: High-confidence text brand signature from header
        String bankFromText = detectFromText(text);
        if (bankFromText != null) return bankFromText;

        // Priority 2: Header IFSC code
        String bankFromIfsc = detectFromIFSC(ifsc);
        if (bankFromIfsc != null) return bankFromIfsc;

        return null;
    }
}