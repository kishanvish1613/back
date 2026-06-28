package com.user.pdfservice.ocr.extractors;

import java.util.regex.*;

public class BranchExtractor {

    public static String extract(String text, String bank) {
        
        Matcher branchName = Pattern.compile(
            "BRANCH\\s+NAME\\s*(?:[:\\-]\\s*)?\\s*([^\\r\\n]{3,100})",
            Pattern.CASE_INSENSITIVE
        ).matcher(text);
        
        if (branchName.find()) {
            return clean(branchName.group(1));
        }
        
        Matcher branch = Pattern.compile(
            "BRANCH\\s*(?!CODE\\b)\\s*(?:[:\\-]\\s*)?\\s*([^\\r\\n]{3,100})",
            Pattern.CASE_INSENSITIVE
        ).matcher(text);
        
        if (branch.find()) {
            return clean(branch.group(1));
        }

        if ("KOTAK".equals(bank)) {
            Matcher kotak = Pattern.compile(
                    "KOTAK\\s+MAHINDRA\\s+BANK\\s*\\n([A-Za-z\\s]{3,40})",
                    Pattern.CASE_INSENSITIVE).matcher(text);

            if (kotak.find()) {
                return clean(kotak.group(1));
            }
        }

        return null;
    }

    private static String clean(String s) {
        return s
                .replaceAll("\\s{2,}", " ")
                .replaceAll("\\b(BRANCH|BANK)\\b", "")
                .trim();
    }
}