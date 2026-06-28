package com.user.pdfservice.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.InputStream;

public final class SampleStatements {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .registerModule(new JavaTimeModule());

  private SampleStatements() {}

  public static Statement kotak() {
    return load("/templates/sample.json");
  }

  private static Statement load(String path) {
    try (InputStream is =
             SampleStatements.class.getResourceAsStream(path)) {

      if (is == null) {
        throw new IllegalStateException("Sample statement not found: " + path);
      }

      return MAPPER.readValue(is, Statement.class);

    } catch (Exception e) {
      throw new RuntimeException("Failed to load sample statement: " + path, e);
    }
  }
}