package com.user.pdfservice;

import com.user.pdfservice.ocr.OcrProcessingService;
import com.user.pdfservice.ocr.OcrResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AllBanksOcrVerificationTest {

    @Test
    public void testAuBankOcr() {
        String auText = """
                ACCOUNT STATEMENT
                Name : M S PHARMA
                Customer ID : 39867327
                Customer Type : Sole Propertary
                Address : Shop No/plot No Shop No G-70
                Ground, Floor Vardhman City Plaza
                Dawa, Bazar, Hamidia Road Tehsil
                Huzur
                Bhopal - 462001, Madhya Pradesh -
                India
                Account Number : 2502248474908993
                Account Type : AU Current Account-Basic
                Branch : Hamidiya Road Bhopal
                IFSC : AUBL0002484
                Nominee : Shabnam Qureshi
                Statement Date : 11 Aug 2026
                Statement Period : 01 Jan 2026 to 11 Aug 2026
                Opening Balance(₹) : 85,482.15
                Closing Balance(₹) : 5,366.45
                """;

        OcrResult r = OcrProcessingService.process(auText);
        assertEquals("AU", r.bankName);
        assertEquals("M S PHARMA", r.name);
        assertEquals("2502248474908993", r.accountNumber);
        assertEquals("39867327", r.customerRelNo);
        assertEquals("AU Current Account-Basic", r.accountType);
        assertEquals("Hamidiya Road Bhopal", r.branch);
        assertEquals("AUBL0002484", r.ifsc);
        assertEquals("Shabnam Qureshi", r.nomineeName);
        assertEquals("85482.15", r.openingBalance);
        assertEquals("01 Jan 2026", r.statementPeriodStart);
        assertEquals("11 Aug 2026", r.statementPeriodEnd);
        assertTrue(r.address.contains("Vardhman City Plaza"));
        assertEquals("462001", r.pincode);
    }

    @Test
    public void testBoiBankOcr() {
        String boiText = """
                Bank Of India
                Bank Of India RATIBAD
                Date 23-03-2026
                M/S SHIVAY ORTHO CARE CLINIC PHYSIOTHERAPY &
                SHOP NO 6
                SKY TOWER ROYAL MARKET
                BHOPAL-462001
                MADHYA PRADESH
                INDIA
                JOINT HOLDER: NOMINEE : YES
                OPENING BALANCE: 501.50Cr.
                CUSTID : CC9396053
                A/C NO : 907020110000340
                TYPE : CURRENT- GENERAL
                CURRENCY : INR
                IFSC CODE : BKID0009070
                MICR CODE : 462013018
                Statement of Account 907020110000340 FROM 01-02-2025 TO 31-01-2026
                """;

        OcrResult r = OcrProcessingService.process(boiText);
        assertEquals("BOI", r.bankName);
        assertTrue(r.name.contains("SHIVAY ORTHO CARE"));
        assertEquals("907020110000340", r.accountNumber);
        assertEquals("CC9396053", r.customerRelNo);
        assertEquals("BKID0009070", r.ifsc);
        assertEquals("462013018", r.micr);
        assertEquals("YES", r.nomineeName);
        assertEquals("RATIBAD", r.branch);
        assertEquals("501.50", r.openingBalance);
        assertEquals("01-02-2025", r.statementPeriodStart);
        assertEquals("31-01-2026", r.statementPeriodEnd);
    }

    @Test
    public void testHdfcCurrentOcr() {
        String hdfcText = """
                Page No .: 1
                M/S. TALHA ENTERPRISES
                NA SHOP NO 03 BHARATPUR ROAD
                NEAR KHANDALWAL HOTEL NA MAHWA
                DAUSA RAJASTHAN 321608 KESARBAG
                MAHWA 321608
                RAJASTHAN INDIA
                JOINT HOLDERS :
                Nomination : Registered
                From : 01/01/2026 To : 17/08/2026
                Statement of account
                Account Branch : MAHWA
                Address : HDFC BANK LTD, JAIPUR ROAD ,MAHWA DIST DAUSA RAJASTHAN
                City : MAHWA 321608
                State : RAJASTHAN
                Phone no. : 18002600/18001600
                OD Limit : 0.00
                Currency : INR
                Email : AARISKHAN219545@GMAIL.COM
                Cust ID : 338318140
                Account No : 50200115012821 OTHER
                A/C Open Date : 12/01/2026
                Account Status : Regular
                RTGS/NEFT IFSC: HDFC0009689 MICR : 321240004
                Branch Code : 9689
                Account Type : BIZ PRO PLUS ACCOUNT(1482)
                """;

        OcrResult r = OcrProcessingService.process(hdfcText);
        assertEquals("HDFCCURRENT", r.bankName);
        assertEquals("M/S. TALHA ENTERPRISES", r.name);
        assertEquals("50200115012821", r.accountNumber);
        assertEquals("338318140", r.customerRelNo);
        assertEquals("BIZ PRO PLUS ACCOUNT(1482)", r.accountType);
        assertEquals("MAHWA", r.branch);
        assertEquals("HDFC0009689", r.ifsc);
        assertEquals("321240004", r.micr);
        assertEquals("Registered", r.nomineeName);
        assertEquals("01/01/2026", r.statementPeriodStart);
        assertEquals("17/08/2026", r.statementPeriodEnd);
        assertEquals("aariskhan219545@gmail.com", r.email);
    }

    @Test
    public void testKotakOcr() {
        String kotakText = """
                Account Statement
                01 Sep 2026 - 30 Nov 2026
                Kishan Vishwakarma
                CRN xxxxxx478
                118 Semra Kala Purushottam Nagar B
                Purushottam Nagar Semra Kala B
                Bhopal - 462010
                Madhya Pradesh - India
                MICR 462485007 IFSC Code KKBK0005891
                Account No. 9947281077
                Account Type Savings
                Branch Bhopal - Hamedia Road
                Account Status Active
                Nominee Registered No
                Currency INDIAN RUPEE
                """;

        OcrResult r = OcrProcessingService.process(kotakText);
        assertEquals("KOTAKNEW", r.bankName);
        assertEquals("Kishan Vishwakarma", r.name);
        assertEquals("9947281077", r.accountNumber);
        assertEquals("xxxxxx478", r.customerRelNo);
        assertEquals("Savings", r.accountType);
        assertEquals("Bhopal - Hamedia Road", r.branch);
        assertEquals("KKBK0005891", r.ifsc);
        assertEquals("462485007", r.micr);
        assertEquals("01-09-2026", r.statementPeriodStart);
        assertEquals("30-11-2026", r.statementPeriodEnd);
    }

    @Test
    public void testUnionBankOcr() {
        String unionText = """
                Statement of Account
                JAI HIND CARRIER SERVICES
                C/O JAI HIND CARRIER SERVICES
                A 201 URVASHI APARTMENT
                BH SAMTA FLATS SUBHANPURA
                City VADODARA
                State GUJARAT
                Country INDIA
                Zip 390023
                Mobile No 919601783479
                E-mail jaihindcarrierservices@gmail.com
                Statement Date : 11/06/2026 11:48
                Union Bank of India
                Branch VADODARA - PBB
                Customer Id 1000151959
                Account No 510101001126333
                Account Currency INR
                Account Type Current Account
                MICR Code 390026039
                IFSC Code UBIN0906166
                CKYC Number 80037687902360
                Statement Period From -01/12/2025 To 11/06/2026
                """;

        OcrResult r = OcrProcessingService.process(unionText);
        assertEquals("UNIONBANK", r.bankName);
        assertEquals("JAI HIND CARRIER SERVICES", r.name);
        assertEquals("510101001126333", r.accountNumber);
        assertEquals("1000151959", r.customerRelNo);
        assertEquals("Current Account", r.accountType);
        assertEquals("VADODARA - PBB", r.branch);
        assertEquals("UBIN0906166", r.ifsc);
        assertEquals("390026039", r.micr);
        assertEquals("80037687902360", r.ckycr);
        assertEquals("01/12/2025", r.statementPeriodStart);
        assertEquals("11/06/2026", r.statementPeriodEnd);
        assertEquals("919601783479", r.phoneNumber);
        assertEquals("jaihindcarrierservices@gmail.com", r.email);
    }
}
