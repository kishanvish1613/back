package com.user.pdfservice.template.kotak;

import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.Transaction;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class KotakHtmlBuilder {

  // Output format used in PDF (Kotak-style)
  private KotakHtmlBuilder() {
    // utility class
  }

  public static String build(Statement stmt) {
    String template = loadTemplate("templates/kotak.html");

    Summary s = computeSummary(stmt.transactions);

    return template
        .replace("{{NAME}}", stmt.details.name)
        .replace("{{ACCOUNT_NO}}", stmt.details.accountNumber)
        .replace("{{IFSC}}", stmt.details.ifsc)
        .replace("{{BRANCH}}", stmt.details.branch)
        .replace("{{BRANCH_PHONE_NO}}", stmt.details.branchPhoneNo)
        .replace("{{BRANCH_ADDRESS}}", esc(stmt.details.branchAddress).replaceAll("\n", "<br/>"))
        .replace("{{MICR}}", stmt.details.micr)
        .replace("{{CUSTOMER_REL_NO}}", stmt.details.customerRelNo)
        .replace("{{CUSTOMER_ADDRESS}}",
            Arrays.stream(stmt.details.address.split("\\r?\\n"))
                .filter(line -> !line.trim().isEmpty())
                .map(line -> "<p>" + line + "</p>")
                .collect(Collectors.joining()))
        .replace("{{STATEMENT_START_DATE}}", formatDate2(stmt.meta.statementPeriodStart))
        .replace("{{STATEMENT_END_DATE}}", formatDate2(stmt.meta.statementPeriodEnd))
        .replace("{{OPENING_BALANCE}}", formatCr(s.openingBalance))
        .replace("{{TOTAL_DEBIT}}", formatDr(s.totalDebit))
        .replace("{{TOTAL_CREDIT}}", formatCr(s.totalCredit))
        .replace("{{CLOSING_BALANCE}}", formatCr(s.closingBalance))
        .replace("{{DEBIT_COUNT}}", String.valueOf(s.debitCount))
        .replace("{{CREDIT_COUNT}}", String.valueOf(s.creditCount))
        .replace("{{TRANSACTIONS_TABLE}}",
            buildTransactionRows(stmt.transactions));
  }
  
  private static String esc(String s) {
      if (s == null) return "";
      return s
          .replace("&", "&amp;")
          .replace("<", "&lt;")
          .replace(">", "&gt;")
          .replace("\"", "&quot;")
          .replace("'", "&apos;");
  }

  private static String loadTemplate(String path) {
    try (InputStream in = KotakHtmlBuilder.class
        .getClassLoader()
        .getResourceAsStream(path)) {

      if (in == null) {
        throw new IllegalStateException("Template not found: " + path);
      }

      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load HTML template", e);
    }
  }

  // ===================== TRANSACTIONS =====================

  public static String buildTransactionRows(List<Transaction> txns) {
    StringBuilder sb = new StringBuilder();

    for (Transaction t : txns) {
      sb.append("<tr>")
          .append("<td class=\"nowrap\">")
          .append(formatDate(t.date))
          .append("</td>")
          .append("<td>")
          .append(escapeHtml(t.description))
          .append("</td>")
          .append("<td class=\"nowrap\">")
          .append(escapeHtml(t.reference))
          .append("</td>")
          .append("<td class=\"nowrap\">")
          .append(formatAmount(t))
          .append("</td>")
          .append("<td class=\"nowrap\">")
          .append(formatBalance(t))
          .append("</td>")
          .append("</tr>");
    }

    return sb.toString();
  }

  // ===================== HELPERS =====================

  private static String formatDate(String rawDate) {
    try {
      Instant instant = Instant.parse(rawDate);

      DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("dd-MM-yy")
          .withZone(ZoneOffset.UTC);
      return outputFmt.format(instant);
    } catch (Exception e) {
      // fallback – never break PDF generation
      return rawDate;
    }
  }

  private static String formatDate2(String rawDate) {
    try {
      Instant instant = Instant.parse(rawDate);

      DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy")
          .withZone(ZoneOffset.UTC);
      return outputFmt.format(instant);
    } catch (Exception e) {
      // fallback – never break PDF generation
      return rawDate;
    }
  }

  private static String formatAmount(Transaction t) {
    if (t.debit > 0) {
      return String.format(Locale.ENGLISH, "%,.2f(Dr)", t.debit);
    }
    if (t.credit > 0) {
      return String.format(Locale.ENGLISH, "%,.2f(Cr)", t.credit);
    }
    return "";
  }

  private static String formatBalance(Transaction t) {
    return String.format(Locale.ENGLISH, "%,.2f(Cr)", t.balance);
  }

  // Prevent HTML injection / layout break
  private static String escapeHtml(String s) {
    if (s == null)
      return "";
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }

  private static Summary computeSummary(List<Transaction> txns) {

    if (txns == null || txns.isEmpty()) {
      return new Summary(0, 0, 0, 0, 0, 0);
    }

    double totalDebit = 0;
    double totalCredit = 0;
    int debitCount = 0;
    int creditCount = 0;

    for (Transaction t : txns) {
      if (t.debit > 0) {
        totalDebit += t.debit;
        debitCount++;
      }
      if (t.credit > 0) {
        totalCredit += t.credit;
        creditCount++;
      }
    }

    Transaction first = txns.get(0);
    Transaction last = txns.get(txns.size() - 1);

    double openingBalance = first.balance - first.credit + first.debit;

    double closingBalance = last.balance;

    return new Summary(
        openingBalance,
        totalDebit,
        totalCredit,
        closingBalance,
        debitCount,
        creditCount);
  }

  private static String formatCr(double amt) {
    return String.format(Locale.ENGLISH, "%,.2f(Cr)", amt);
  }

  private static String formatDr(double amt) {
    return String.format(Locale.ENGLISH, "%,.2f(Dr)", amt);
  }

  private static final class Summary {
    double openingBalance;
    double totalDebit;
    double totalCredit;
    double closingBalance;
    int debitCount;
    int creditCount;

    Summary(
        double openingBalance,
        double totalDebit,
        double totalCredit,
        double closingBalance,
        int debitCount,
        int creditCount) {
      this.openingBalance = openingBalance;
      this.totalDebit = totalDebit;
      this.totalCredit = totalCredit;
      this.closingBalance = closingBalance;
      this.debitCount = debitCount;
      this.creditCount = creditCount;
    }
  }

}