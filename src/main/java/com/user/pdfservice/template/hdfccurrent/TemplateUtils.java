package com.user.pdfservice.template.hdfccurrent;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TemplateUtils {

    private static final DateTimeFormatter DD_MM_YY = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.ENGLISH);
    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter D_MMM_YYYY = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    public static void drawText(PDPageContentStream cs, String text, float x, float y, PDFont font, float size) throws IOException {
        drawText(cs, text, x, y, font, size, 0f, 0f, 0f);
    }

    public static void drawText(PDPageContentStream cs, String text, float x, float y, PDFont font, float size, float r, float g, float b) throws IOException {
        if (text == null || text.isEmpty()) return;
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(r, g, b);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    public static void drawRightAlignedText(PDPageContentStream cs, String text, float rightX, float y, PDFont font, float size) throws IOException {
        drawRightAlignedText(cs, text, rightX, y, font, size, 0f, 0f, 0f);
    }

    public static void drawRightAlignedText(PDPageContentStream cs, String text, float rightX, float y, PDFont font, float size, float r, float g, float b) throws IOException {
        if (text == null || text.isEmpty()) return;
        float width = getTextWidth(text, font, size);
        drawText(cs, text, rightX - width, y, font, size, r, g, b);
    }

    public static void drawCenteredText(PDPageContentStream cs, String text, float centerX, float y, PDFont font, float size) throws IOException {
        drawCenteredText(cs, text, centerX, y, font, size, 0f, 0f, 0f);
    }

    public static void drawCenteredText(PDPageContentStream cs, String text, float centerX, float y, PDFont font, float size, float r, float g, float b) throws IOException {
        if (text == null || text.isEmpty()) return;
        float width = getTextWidth(text, font, size);
        drawText(cs, text, centerX - (width / 2f), y, font, size, r, g, b);
    }

    public static float getTextWidth(String text, PDFont font, float size) {
        if (text == null || text.isEmpty()) return 0;
        try {
            return (font.getStringWidth(text) / 1000f) * size;
        } catch (IOException e) {
            return text.length() * (size * 0.5f);
        }
    }

    public static String formatIndianCurrency(double amount) {
        boolean isNegative = amount < 0;
        double absAmt = Math.abs(amount);
        long integerPart = (long) absAmt;
        int fractionPart = (int) Math.round((absAmt - integerPart) * 100);
        if (fractionPart == 100) {
            integerPart += 1;
            fractionPart = 0;
        }

        String intStr = String.valueOf(integerPart);
        StringBuilder result = new StringBuilder();
        int len = intStr.length();

        if (len <= 3) {
            result.append(intStr);
        } else {
            String lastThree = intStr.substring(len - 3);
            String remaining = intStr.substring(0, len - 3);
            StringBuilder remFormatted = new StringBuilder();
            int count = 0;
            for (int i = remaining.length() - 1; i >= 0; i--) {
                remFormatted.insert(0, remaining.charAt(i));
                count++;
                if (count % 2 == 0 && i > 0) {
                    remFormatted.insert(0, ',');
                }
            }
            result.append(remFormatted).append(',').append(lastThree);
        }

        result.append(String.format(Locale.US, ".%02d", fractionPart));
        if (isNegative) {
            result.insert(0, "-");
        }
        return result.toString();
    }

    public static String formatDateDDMMYY(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) return "";
        try {
            if (rawDate.length() >= 10 && rawDate.charAt(4) == '-' && rawDate.charAt(7) == '-') {
                LocalDate ld = LocalDate.parse(rawDate.substring(0, 10));
                return ld.format(DD_MM_YY);
            }
            if (rawDate.contains("/")) {
                String[] parts = rawDate.split("/");
                if (parts.length == 3) {
                    int d = Integer.parseInt(parts[0]);
                    int m = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    if (y > 2000) y -= 2000;
                    return String.format(Locale.US, "%02d/%02d/%02d", d, m, y);
                }
            }
            Instant inst = Instant.parse(rawDate);
            LocalDate date = inst.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
            return date.format(DD_MM_YY);
        } catch (Exception e) {
            return rawDate;
        }
    }

    public static String formatDateDDMMYYYY(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) return "";
        try {
            if (rawDate.length() >= 10 && rawDate.charAt(4) == '-' && rawDate.charAt(7) == '-') {
                LocalDate ld = LocalDate.parse(rawDate.substring(0, 10));
                return ld.format(DD_MM_YYYY);
            }
            if (rawDate.contains("/")) {
                String[] parts = rawDate.split("/");
                if (parts.length == 3) {
                    int d = Integer.parseInt(parts[0]);
                    int m = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    if (y < 100) y += 2000;
                    return String.format(Locale.US, "%02d/%02d/%04d", d, m, y);
                }
            }
            Instant inst = Instant.parse(rawDate);
            LocalDate date = inst.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
            return date.format(DD_MM_YYYY);
        } catch (Exception e) {
            return rawDate;
        }
    }

    public static String formatDateDMMMYYYY(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return LocalDate.now().format(D_MMM_YYYY);
        }
        try {
            if (rawDate.length() >= 10 && rawDate.charAt(4) == '-' && rawDate.charAt(7) == '-') {
                LocalDate ld = LocalDate.parse(rawDate.substring(0, 10));
                return ld.format(D_MMM_YYYY);
            }
            Instant inst = Instant.parse(rawDate);
            LocalDate date = inst.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
            return date.format(D_MMM_YYYY);
        } catch (Exception e) {
            return rawDate;
        }
    }

    public static List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;

        String[] words = text.trim().split("\\s+");
        String currentLine = "";

        for (String word : words) {
            if (word.isEmpty()) continue;
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (getTextWidth(testLine, font, fontSize) <= maxWidth) {
                currentLine = testLine;
            } else {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine);
                    currentLine = "";
                }
                if (getTextWidth(word, font, fontSize) <= maxWidth) {
                    currentLine = word;
                } else {
                    // Break token at slash or hyphen boundaries
                    List<String> segments = splitSegments(word);
                    for (String seg : segments) {
                        String testSeg = currentLine.isEmpty() ? seg : currentLine + seg;
                        if (getTextWidth(testSeg, font, fontSize) <= maxWidth) {
                            currentLine = testSeg;
                        } else {
                            if (!currentLine.isEmpty()) {
                                lines.add(currentLine);
                                currentLine = "";
                            }
                            while (getTextWidth(seg, font, fontSize) > maxWidth) {
                                int cut = seg.length();
                                while (cut > 1 && getTextWidth(seg.substring(0, cut), font, fontSize) > maxWidth) {
                                    cut--;
                                }
                                lines.add(seg.substring(0, cut));
                                seg = seg.substring(cut);
                            }
                            currentLine = seg;
                        }
                    }
                }
            }
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine);
        }
        return lines;
    }

    private static List<String> splitSegments(String word) {
        List<String> segs = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if ((c == '/' || c == '-' || c == '@') && cur.length() > 0) {
                segs.add(cur.toString());
                cur.setLength(0);
            }
            cur.append(c);
        }
        if (cur.length() > 0) segs.add(cur.toString());
        return segs;
    }
}

