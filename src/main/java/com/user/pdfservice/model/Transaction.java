package com.user.pdfservice.model;

public class Transaction {
  public String id;
  public String date;
  public String description;
  public String reference;
  public double debit;
  public double credit;
  public double balance;

  public String valueDate;

  // Union Bank statement code columns
  public String tranId;   // Tran Id-1  e.g. S47792753
  public String utr;      // UTR Number e.g. "Sender No:BARBZ25335514191" or "-"
  public String instrId;  // Instr. ID  mostly blank, occasionally a clearing number
}