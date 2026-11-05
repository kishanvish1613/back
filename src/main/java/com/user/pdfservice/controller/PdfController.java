package com.user.pdfservice.controller;

import com.user.pdfservice.generator.PdfGenerator;
import com.user.pdfservice.model.SampleStatements;
import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.entity.StatementRecord;
import com.user.pdfservice.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

@RestController
@CrossOrigin(origins = "*", exposedHeaders = "Content-Disposition")
public class PdfController {

  private final ObjectMapper mapper = new ObjectMapper();
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
  private final AdminService adminService;

  public PdfController(AdminService adminService) {
    this.adminService = adminService;
  }

  /**
   * Generate a bank statement PDF (possibly password‑protected) and return it
   * with an appropriate filename.
   */
  @PostMapping("/pdf")
  public ResponseEntity<byte[]> createPdf(@RequestBody Statement statement, HttpServletRequest request) throws Exception {

    System.out.println(">>> Generating PDF for template: " + statement.meta.template);
    System.out.println(">>> Statement opening balance: " + statement.details.startingBalance);

    // If Admin enabled storing normal user data, record this generated statement into MongoDB
    try {
      if (adminService.isStoreUserDataEnabled()) {
        String username = (String) request.getAttribute("username");
        if (username == null || username.trim().isEmpty()) {
          username = "Normal User";
        }

        StatementRecord record = new StatementRecord();
        record.setUsername(username);
        record.setBankTemplate(statement.meta != null && statement.meta.template != null ? statement.meta.template.toString() : "UNKNOWN");
        record.setAccountNumber(statement.details != null ? statement.details.accountNumber : "");
        record.setCustomerName(statement.details != null ? statement.details.name : "");
        double openingBal = statement.details != null ? statement.details.startingBalance : 0.0;
        double closingBal = openingBal;
        int totalTxns = 0;
        if (statement.transactions != null && !statement.transactions.isEmpty()) {
          totalTxns = statement.transactions.size();
          closingBal = statement.transactions.get(totalTxns - 1).balance;
        }

        record.setOpeningBalance(openingBal);
        record.setClosingBalance(closingBal);
        record.setTotalTransactions(totalTxns);
        record.setStartDate(statement.meta != null ? statement.meta.statementPeriodStart : "");
        record.setEndDate(statement.meta != null ? statement.meta.statementPeriodEnd : "");
        record.setGeneratedAt(LocalDateTime.now());
        record.setStatementJson(mapper.writeValueAsString(statement));

        adminService.saveStatementRecord(record);
        System.out.println(">>> [Audit] Archived statement record for user: " + username);
      }
    } catch (Exception ex) {
      System.err.println(">>> [Audit] Warning: could not store statement record: " + ex.getMessage());
    }

    // 🔒 If template handles its own encryption (e.g. BOI with exact JasperReports RC4 metadata), keep password
    boolean templateHandlesEncryption = (statement.meta.template != null && (statement.meta.template.toString().equalsIgnoreCase("BOI") || statement.meta.template.toString().equalsIgnoreCase("SBINEW") || statement.meta.template.toString().equalsIgnoreCase("KOTAKNEW")));
    String lockPassword = statement.meta.password;
    if (!templateHandlesEncryption) {
      statement.meta.password = "";
    }

    byte[] pdf = PdfGenerator.generate(statement);

    // Now apply our own encryption if a lock password was requested and template did not handle it
    if (!templateHandlesEncryption && lockPassword != null && !lockPassword.isEmpty()) {
      System.out.println(">>> Encrypting PDF with password: " + lockPassword);
      pdf = encryptPdf(pdf, lockPassword);
    }

    // Choose filename based on bank template
    String templateStr = statement.meta.template.toString();
    String filename;

    if (templateStr.toUpperCase().startsWith("SBI")) {
      filename = PdfGenerator.generateTimestampFilename();
    } else if (templateStr.equalsIgnoreCase("KOTAKNEW")) {
      filename = buildKotakNewFilename(statement);
    } else {
      filename = PdfGenerator.generateTimestampFilename();
    }

    System.out.println(">>> Generated filename: " + filename);

    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + filename + "\"")
            .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(pdf.length))
            .cacheControl(CacheControl.noCache().noStore().mustRevalidate())
            .header(HttpHeaders.PRAGMA, "no-cache")
            .header(HttpHeaders.EXPIRES, "0")
            .body(pdf);
  }

  /**
   * Preview a Kotak statement inline.
   */
  @GetMapping(value = "/pdf/preview", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> preview() throws Exception {
    Statement stmt = SampleStatements.kotak();
    byte[] pdf = PdfGenerator.generate(stmt);
    String filename = PdfGenerator.generateTimestampFilename();

    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .cacheControl(CacheControl.noCache().noStore().mustRevalidate())
            .header(HttpHeaders.PRAGMA, "no-cache")
            .header(HttpHeaders.EXPIRES, "0")
            .body(pdf);
  }

  @GetMapping("/pdf/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("PDF Service is running");
  }

  @GetMapping("/pdf/templates")
  public ResponseEntity<List<String>> getSupportedTemplates() {
    List<String> templates = Arrays.asList(
            "UNIONBANK", "SBINEW", "SBI", "SBI2", "KOTAKNEW", "KOTAK", "AXIS", "BOI", "HDFC"
    );
    return ResponseEntity.ok(templates);
  }

  // ===================== PRIVATE HELPERS =====================

  /**
   * Encrypt a PDF byte array with a user password using OpenPDF (with PDFBox fallback).
   */
  private byte[] encryptPdf(byte[] pdfBytes, String password) throws Exception {
    try (PDDocument document = PDDocument.load(pdfBytes)) {
      float version = document.getVersion();
      AccessPermission ap = new AccessPermission();
      StandardProtectionPolicy spp = new StandardProtectionPolicy(password, password, ap);
      spp.setEncryptionKeyLength(128);
      spp.setPermissions(ap);
      document.protect(spp);

      if (version >= 1.5f) {
        document.getDocument().setVersion(version);
        if (document.getDocumentCatalog() != null) {
          document.getDocumentCatalog().setVersion(String.valueOf(version));
        }
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      document.save(baos);
      return baos.toByteArray();
    }
  }

  /**
   * Build a KotakNew filename: AccountStatement_dd-MMM-yyyy_dd-MMM-yyyy.pdf
   */
  private String buildKotakNewFilename(Statement stmt) {
    String startStr = stmt.meta.statementPeriodStart;
    String endStr = stmt.meta.statementPeriodEnd;

    LocalDate start = parseDate(startStr);
    LocalDate end = parseDate(endStr);

    return "AccountStatement_" + start.format(DATE_FORMAT) + "_" + end.format(DATE_FORMAT) + ".pdf";
  }

  private LocalDate parseDate(String dateStr) {
    if (dateStr == null) return LocalDate.now();
    try {
      return LocalDate.parse(dateStr);
    } catch (DateTimeParseException e) {
      return LocalDate.now();
    }
  }
}