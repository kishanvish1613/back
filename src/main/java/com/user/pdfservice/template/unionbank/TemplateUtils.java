package com.user.pdfservice.template.unionbank;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TemplateUtils {

    public static void drawText(PDPageContentStream cs, String text, float x, float y, PDFont font, float size) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    public static List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null) return lines;
        String line = "";
        for (String word : text.split("\\s+")) {
            if (word.isEmpty()) continue;
            String withWord = line.isEmpty() ? word : line + " " + word;
            if (textWidth(withWord, font, fontSize) <= maxWidth) {
                line = withWord;
                continue;
            }
            // Word does not fit on the current line.
            if (!line.isEmpty()) { lines.add(line); line = ""; }
            if (textWidth(word, font, fontSize) <= maxWidth) {
                line = word;
                continue;
            }
            // Token wider than the column (UPI/NEFT narration): break at natural
            // '/' and '-' boundaries; only char-break a segment that is still too wide.
            for (String seg : splitSegments(word)) {
                String test = line.isEmpty() ? seg : line + seg;
                if (textWidth(test, font, fontSize) <= maxWidth) {
                    line = test;
                } else {
                    if (!line.isEmpty()) { lines.add(line); line = ""; }
                    while (textWidth(seg, font, fontSize) > maxWidth) {
                        int cut = seg.length();
                        while (cut > 1 && textWidth(seg.substring(0, cut), font, fontSize) > maxWidth) cut--;
                        lines.add(seg.substring(0, cut));
                        seg = seg.substring(cut);
                    }
                    line = seg;
                }
            }
        }
        if (!line.isEmpty()) lines.add(line);
        return lines;
    }

    /** Splits a token so each '/' or '-' starts a new segment (delimiter kept with the following text). */
    private static List<String> splitSegments(String word) {
        List<String> segs = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if ((c == '/' || c == '-') && cur.length() > 0) {
                segs.add(cur.toString());
                cur.setLength(0);
            }
            cur.append(c);
        }
        if (cur.length() > 0) segs.add(cur.toString());
        return segs;
    }

    private static float textWidth(String s, PDFont font, float size) throws IOException {
        if (s.isEmpty()) return 0;
        return font.getStringWidth(s) / 1000 * size;
    }

    public static float getTextWidth(String text, PDFont font, float size) throws IOException {
        return font.getStringWidth(text) / 1000 * size;
    }
}