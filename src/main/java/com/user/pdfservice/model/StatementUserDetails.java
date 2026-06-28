package com.user.pdfservice.model;

public class StatementUserDetails {
  public String title;
  public String name;
  public String fullName;        // used by some templates
  public String accountNumber;
  public String ifsc;
  public double startingBalance;

  public String address;
  public String city;
  public String state;
  public String pincode;
  public String branch;
  public String branchAddress;
  public String phoneNumber;
  public String email;

  public String micr;
  public String branchPhoneNo;
  public String customerRelNo;
  public String pan;
  public String cykr;
  public String ckycr;          // ← added (CKYCR number / "Not Available")
  public String accountType;    // already present
  public String password;       // if needed for templates
  public String branchLocation; // branch location (e.g., "BAWADIYA KALAN, BHOPAL")
}