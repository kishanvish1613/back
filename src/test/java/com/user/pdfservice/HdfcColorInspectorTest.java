package com.user.pdfservice;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

public class HdfcColorInspectorTest {

    @Test
    void inspectColors() throws Exception {
        File f = new File("C:/Users/real/.gemini/antigravity/brain/5e191c5a-8270-4075-8fa4-b9ceab0cc3cf/.user_uploaded/media_1790159153469.png");
        if (!f.exists()) {
            System.out.println("File not found: " + f.getAbsolutePath());
            return;
        }
        BufferedImage img = ImageIO.read(f);
        int w = img.getWidth();
        int h = img.getHeight();
        System.out.println("Image Dimensions: " + w + " x " + h);

        // Sample at table header (approx 29% height)
        int headerY = (int) (h * 0.285);
        // Sample at table data row (approx 45% height)
        int dataY = (int) (h * 0.45);

        float[] xPcts = {0.08f, 0.25f, 0.46f, 0.58f, 0.67f, 0.78f, 0.90f};
        String[] colNames = {"Date", "Narration", "Chq/Ref", "Value Dt", "Withdrawal", "Deposit", "Closing Bal"};

        System.out.println("=== HEADER ROW COLORS ===");
        for (int i = 0; i < xPcts.length; i++) {
            int x = (int) (w * xPcts[i]);
            Color c = new Color(img.getRGB(x, headerY));
            System.out.printf("Col %d (%s) at (%d, %d): R=%d, G=%d, B=%d [Hex: #%02X%02X%02X]%n",
                    i + 1, colNames[i], x, headerY, c.getRed(), c.getGreen(), c.getBlue(), c.getRed(), c.getGreen(), c.getBlue());
        }

        System.out.println("=== DATA ROW COLORS ===");
        for (int i = 0; i < xPcts.length; i++) {
            int x = (int) (w * xPcts[i]);
            Color c = new Color(img.getRGB(x, dataY));
            System.out.printf("Col %d (%s) at (%d, %d): R=%d, G=%d, B=%d [Hex: #%02X%02X%02X]%n",
                    i + 1, colNames[i], x, dataY, c.getRed(), c.getGreen(), c.getBlue(), c.getRed(), c.getGreen(), c.getBlue());
        }
    }
}

