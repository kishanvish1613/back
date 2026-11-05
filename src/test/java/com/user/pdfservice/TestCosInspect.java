package com.user.pdfservice;

import com.user.pdfservice.model.Statement;
import com.user.pdfservice.model.StatementMeta;
import com.user.pdfservice.model.StatementUserDetails;
import com.user.pdfservice.model.Transaction;
import com.user.pdfservice.template.sbinew.SbiNewTemplate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class TestCosInspect {

    @Test
    public void testRenderPage2() throws Exception {
        Statement stmt = new Statement();
        stmt.details = new StatementUserDetails();
        stmt.details.name = "Mr. Ved Prakash Patel";
        stmt.details.accountNumber = "40973913131";
        stmt.details.customerRelNo = "90955580461";
        stmt.details.ifsc = "SBIN0061102";
        stmt.details.micr = "462002115";
        stmt.details.branch = "WAGON COACH FACTORY";
        stmt.details.branchPhoneNo = "9926587917";
        stmt.details.branchAddress = "GOSWAMI COMPLEX, KRISHNA NAGAR, COACH FACTORY ROAD, BHOPAL";
        stmt.details.address = "S/O: Shyam Sundar, 138/1, gali no 03, Krishna nagar, Bhopal";
        stmt.details.startingBalance = 17514.71;
        stmt.details.ckycr = "50046100545797";

        stmt.meta = new StatementMeta();
        stmt.meta.statementPeriodStart = "2025-08-01T00:00:00Z";
        stmt.meta.statementPeriodEnd = "2025-08-31T00:00:00Z";
        stmt.meta.password = "40973913131";

        stmt.transactions = new ArrayList<>();

        Transaction t1 = new Transaction();
        t1.date = "2025-08-21T00:00:00Z";
        t1.description = "WDL TFR\nUPI/DR/246387136005/BURGER KING/ICIC/burgerking@/Paym\n0097690162095 AT 16450\nROHIT NAGAR";
        t1.reference = "-";
        t1.debit = 103.42;
        t1.credit = 0;
        t1.balance = 17411.29;
        stmt.transactions.add(t1);

        Transaction t2 = new Transaction();
        t2.date = "2025-08-21T00:00:00Z";
        t2.description = "UPI/Cashback/Reward/710023687989/Cashback";
        t2.reference = "UPI-710023687989";
        t2.debit = 0;
        t2.credit = 5.00;
        t2.balance = 17416.29;
        stmt.transactions.add(t2);

        Transaction t3 = new Transaction();
        t3.date = "2025-08-21T00:00:00Z";
        t3.description = "UPI/Cashback/Reward/181154080829/Cashback";
        t3.reference = "UPI-181154080829";
        t3.debit = 0;
        t3.credit = 1.00;
        t3.balance = 17417.29;
        stmt.transactions.add(t3);

        Transaction t4 = new Transaction();
        t4.date = "2025-08-22T00:00:00Z";
        t4.description = "DEP TFR\nUPI/CR/886454812556/SHALINI SINGH /MAHB/1994226196/Paym\n0097733162093 AT 16450\nROHIT NAGAR";
        t4.reference = "-";
        t4.debit = 0;
        t4.credit = 152.00;
        t4.balance = 17569.29;
        stmt.transactions.add(t4);

        Transaction t5 = new Transaction();
        t5.date = "2025-08-25T00:00:00Z";
        t5.description = "WDL TFR\nUPI/DR/378592987641/Ramakant Sharma/CBIN/paytmqrSKB/Paym\n0097695162091 AT 16450\nROHIT NAGAR";
        t5.reference = "-";
        t5.debit = 120.00;
        t5.credit = 0;
        t5.balance = 17212.29;
        stmt.transactions.add(t5);

        Transaction t6 = new Transaction();
        t6.date = "2025-08-21T00:00:00Z";
        t6.description = "UPI/Cashback/Reward/\n539680223655/Cashback";
        t6.reference = "UPI-5396802230";
        t6.debit = 0;
        t6.credit = 5.00;
        t6.balance = 17381.71;
        stmt.transactions.add(t6);

        SbiNewTemplate template = new SbiNewTemplate();
        byte[] pdfBytes = template.generate(stmt);

        try (PDDocument doc = PDDocument.load(pdfBytes, "40973913131")) {
            PDFRenderer renderer = new PDFRenderer(doc);
            if (doc.getNumberOfPages() > 1) {
                BufferedImage image2 = renderer.renderImageWithDPI(1, 150);
                File file2 = new File("C:/Users/real/.gemini/antigravity/brain/ffddd075-61d9-416b-98f0-31880d7be51c/sbinew_page2_perfect.png");
                ImageIO.write(image2, "PNG", file2);
                System.out.println("Rendered page 2 to: " + file2.getAbsolutePath());
            }
        }
    }
}
