package com.user.pdfservice.template.boi;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.font.PDFont;

public final class TemplateUtils {

    private TemplateUtils() {}

    private static final DateTimeFormatter DATE_D_MMM_YYYY =
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH).withZone(
            ZoneOffset.UTC
        );

    public static String formatDateDMMMYYYY(String isoUtc) {
        if (isoUtc == null || isoUtc.isBlank()) {
            return "";
        }
        return DATE_D_MMM_YYYY.format(Instant.parse(isoUtc));
    }

    private static final DateTimeFormatter D_MMM_YYYY_HH_MM =
        DateTimeFormatter.ofPattern(
            "d MMM yyyy, HH:mm",
            Locale.ENGLISH
        ).withZone(ZoneId.of("Asia/Kolkata")); // or UTC if required

    public static String formatDateTimeDMMMYYYYHHMM(String isoUtc) {
        if (isoUtc == null || isoUtc.isBlank()) {
            return "";
        }
        return D_MMM_YYYY_HH_MM.format(Instant.parse(isoUtc));
    }

    public static String formatDateDMMMYYYY(Instant instant) {
        if (instant == null) {
            return "";
        }
        return DATE_D_MMM_YYYY.format(instant);
    }

    public static String safe(String value) {
        return value == null ? "" : value;
    }

    public static String formatAmount(double value) {
        if (value == 0) return "";

        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .toPlainString();
    }

    public static Object[] formatBalanceAndRightAlignX(
        double balance,
        PDFont font,
        float fontSize,
        float rightEdge
    ) throws IOException {
        DecimalFormat df = new DecimalFormat("##,##,##0.00");
        String formatted = df.format(balance);

        float textWidth = (font.getStringWidth(formatted) / 1000f) * fontSize;

        float x = rightEdge - textWidth;

        return new Object[] { formatted, x };
    }

    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "";
        }
        return "XXXXXX" + accountNumber.substring(accountNumber.length() - 4);
    }
}
