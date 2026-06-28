package com.user.pdfservice.ocr.utils;

public class TextNormalizer {

  public static String normalize(String text) {
    if (text == null) return null;

    return text
      .replace('\u00A0', ' ')
      .replaceAll("[ \t]+", " ")
      .replaceAll("\\n{2,}", "\n")
      .trim();
  }

  public static String deNoise(String text) {
    return text
      .replaceAll("[^\\x20-\\x7E\\n]", "")
      .trim();
  }
}
