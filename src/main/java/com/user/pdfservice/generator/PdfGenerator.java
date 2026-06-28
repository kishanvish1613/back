package com.user.pdfservice.generator;

import com.user.pdfservice.model.Statement;
import com.user.pdfservice.template.BankPdfTemplate;
import com.user.pdfservice.template.TemplateRegistry;

import java.security.SecureRandom;

public class PdfGenerator {

  private static final SecureRandom RANDOM = new SecureRandom();

  /**
   * Generate the PDF for the given statement.
   */
  public static byte[] generate(Statement stmt) throws Exception {
    BankPdfTemplate template = TemplateRegistry.get(stmt.meta.template);
    if (template == null) {
      throw new IllegalArgumentException(
              "No PDF template registered for bank: " + stmt.meta.template
      );
    }
    return template.generate(stmt);
  }

  /**
   * Generate a random filename like 1782333107871d7ximqicmet.pdf
   * (timestamp + random mixed‑case string, length 8‑16)
   */
  public static String generateTimestampFilename() {
    String timestamp = String.valueOf(System.currentTimeMillis());
    int len = 8 + RANDOM.nextInt(9); // random length 8‑16
    String random = generateRandomString(len);
    return timestamp + random + ".pdf";
  }

  /**
   * Generate a random filename with bank prefix (kept for potential other uses).
   */
  public static String generateRandomFilenameWithBank(String bankName) {
    String timestamp = String.valueOf(System.currentTimeMillis());
    String random = generateRandomString(8);
    String safeBank = bankName.replaceAll("[^a-zA-Z0-9]", "");
    return safeBank + "_" + timestamp + "_" + random + ".pdf";
  }

  public static String generateRandomFilename() {
    String timestamp = String.valueOf(System.currentTimeMillis());
    String random = generateRandomString(8);
    return timestamp + "_" + random + ".pdf";
  }

  /**
   * Generate a random string with MIXED CASE (lowercase + uppercase + digits).
   */
  private static String generateRandomString(int length) {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
    }
    return sb.toString();
  }
}