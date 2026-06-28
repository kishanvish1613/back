package com.user.pdfservice.template;

import com.user.pdfservice.model.Statement;

public interface BankPdfTemplate {
  byte[] generate(Statement stmt) throws Exception;
}