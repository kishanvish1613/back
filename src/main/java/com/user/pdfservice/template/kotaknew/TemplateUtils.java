package com.user.pdfservice.template.kotaknew;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.font.PDFont;

public final class TemplateUtils {

    private TemplateUtils() {}

    private static final DateTimeFormatter DATE_D_MMM_YYYY =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH).withZone(ZoneOffset.UTC);

    public static String formatDateDMMMYYYY(String isoUtc) {
        if (isoUtc == null || isoUtc.isBlank()) return "";
        return DATE_D_MMM_YYYY.format(Instant.parse(isoUtc));
    }

    private static final DateTimeFormatter D_MMM_YYYY_HH_MM =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)
                    .withZone(ZoneId.of("Asia/Kolkata"));

    public static String formatDateTimeDMMMYYYYHHMM(String isoUtc) {
        if (isoUtc == null || isoUtc.isBlank()) return "";
        return D_MMM_YYYY_HH_MM.format(Instant.parse(isoUtc));
    }

    public static String formatDateDMMMYYYY(Instant instant) {
        if (instant == null) return "";
        return DATE_D_MMM_YYYY.format(instant);
    }

    public static String safe(String value) {
        return value == null ? "" : value;
    }

    // ---------- Correct Indian Number Grouping ----------
    /**
     * Formats a double value with proper Indian grouping (e.g. 1,23,12,345.67).
     */
    public static String formatIndianAmount(double value) {
        boolean negative = value < 0;
        value = Math.abs(value);
        String s = String.format(Locale.ROOT, "%.2f", value);
        int dot = s.indexOf('.');
        String intPart = dot >= 0 ? s.substring(0, dot) : s;
        String fracPart = dot >= 0 ? s.substring(dot) : ".00";
        String grouped = groupIndian(intPart);
        return (negative ? "-" : "") + grouped + fracPart;
    }

    /**
     * Groups a digit‑only string in Indian numbering (last 3, then groups of 2).
     * Example: "123456" -> "1,23,456", "12345678" -> "1,23,45,678"
     */
    private static String groupIndian(String digits) {
        int len = digits.length();
        if (len <= 3) return digits;
        String last3 = digits.substring(len - 3);
        String rest = digits.substring(0, len - 3);
        StringBuilder sb = new StringBuilder();
        int i = rest.length();
        while (i > 0) {
            int start = Math.max(0, i - 2);
            if (sb.length() > 0) sb.insert(0, ',');
            sb.insert(0, rest.substring(start, i));
            i = start;
        }
        sb.append(',').append(last3);
        return sb.toString();
    }

    // ---------- Alignment helper that uses Indian grouping ----------
    /**
     * Returns a formatted Indian‑amount string and its right‑aligned x position.
     */
    public static Object[] formatBalanceAndRightAlignX(
            double balance,
            PDFont font,
            float fontSize,
            float rightEdge
    ) throws IOException {
        String formatted = formatIndianAmount(balance);
        float textWidth = (font.getStringWidth(formatted) / 1000f) * fontSize;
        float x = rightEdge - textWidth;
        return new Object[] { formatted, x };
    }

    /**
     * Convenience – formats a double to Indian grouping (no alignment).
     */
    public static String formatAmount(double value) {
        if (value == 0) return "";
        return formatIndianAmount(value);
    }

    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return "";
        return "XXXXXX" + accountNumber.substring(accountNumber.length() - 4);
    }
}