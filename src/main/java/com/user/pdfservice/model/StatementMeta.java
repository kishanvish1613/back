package com.user.pdfservice.model;

public class StatementMeta {
  public String generatedAt;
  public BankTemplate template;
  public String userType; // salaried | selfEmployed
  public String configHash;
  public long seed;
  public String statementPeriodStart;
  public String statementPeriodEnd;
  
  public String password;
}