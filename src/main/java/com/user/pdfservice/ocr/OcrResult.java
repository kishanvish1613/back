package com.user.pdfservice.ocr;

public class OcrResult {

  public String name;
  public String accountNumber;
  public String ifsc;
  public String micr;
  public String ckycr;
  public String customerRelNo;
  public String bankName;

  public String branch;                  // Branch Name (e.g., "ROHIT NAGAR")
  public String branchLocation;          // Branch Location (e.g., "BAWADIYA KALAN, BHOPAL")
  public String branchAddress;           // Full address (without location line)
  public String branchPhoneNo;
  public String address;
  public String pincode;
  public String accountType;
  public String pan;
  public String city;
  public String state;

  public String phoneNumber;
  public String email;

  public String openingBalance;
  public String accountOpenDate;
  public String nomineeName;
  public String statementPeriodStart;
  public String statementPeriodEnd;

  public String rawText;
}