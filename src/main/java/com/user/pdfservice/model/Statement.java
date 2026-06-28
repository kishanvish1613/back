package com.user.pdfservice.model;

import java.util.List;

public class Statement {
  public String id;
  public StatementUserDetails details;
  public StatementMeta meta;
  public List<Transaction> transactions;
}