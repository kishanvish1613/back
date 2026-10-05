package com.user.pdfservice.template.hdfccurrent;

import com.user.pdfservice.model.Statement;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Header {

    private final Statement stmt;
    private final Fonts fonts;
    private final PDDocument doc;
    private PDImageXObject logoImage;

    public Header(Statement stmt, Fonts fonts, PDDocument doc) {
        this.stmt = stmt;
        this.fonts = fonts;
        this.doc = doc;
        loadLogo();
    }

    private void loadLogo() {
        try {
            InputStream is = getClass().getResourceAsStream("/templates/hdfc_logo.png");
            if (is != null) {
                BufferedImage bimg = ImageIO.read(is);
                if (bimg != null) {
                    this.logoImage = LosslessFactory.createFromImage(doc, bimg);
                    return;
                }
            }
            File f = new File("src/main/resources/templates/hdfc_logo.png");
            if (f.exists()) {
                BufferedImage bimg = ImageIO.read(f);
                if (bimg != null) {
                    this.logoImage = LosslessFactory.createFromImage(doc, bimg);
                }
            }
        } catch (Exception ignored) {}
    }

    public void draw(PDPageContentStream cs, int pageNum) throws IOException {
        // 1. Top Page Number: "Page No .: 1" (x = 292.034 pt, y = 816.583 pt)
        TemplateUtils.drawText(cs, "Page No .: " + pageNum, 292.034f, 816.583f, fonts.regular, 8.0f, 0f, 0f, 0f);

        // 2. HDFC Bank Logo (x = 28.346 pt, y = 774.674 pt, W = 145 pt, H = 32 pt)
        if (logoImage != null) {
            cs.drawImage(logoImage, 28.346f, 774.674f, 145.00f, 32.00f);
        }

        // 3. Customer Address Box (Outer rect: x = 28.346 pt, y = 650.836 pt, W = 283.464 pt, H = 109.982 pt)
        cs.saveGraphicsState();
        cs.setLineWidth(1.133f);
        cs.setStrokingColor(0f, 0f, 0f);
        cs.addRect(28.346f, 650.836f, 283.464f, 109.982f);
        cs.stroke();
        cs.restoreGraphicsState();

        // Customer details inside box (Left: 34.016 pt, top baseline at y = 744.918 pt, pitch = 11.634 pt)
        String customerName = "M/S. TALHA ENTERPRISES";
        if (stmt.details != null) {
            if (stmt.details.fullName != null && !stmt.details.fullName.isBlank()) {
                customerName = stmt.details.fullName.trim();
            } else if (stmt.details.name != null && !stmt.details.name.isBlank()) {
                customerName = stmt.details.name.trim();
            }
        }

        float textX = 34.016f;
        float curY = 744.918f;
        float addrPitch = 11.634f;

        TemplateUtils.drawText(cs, customerName.toUpperCase(), textX, curY, fonts.bold, 8.0f, 0f, 0f, 0f);
        curY -= addrPitch;

        // Address lines
        String rawAddress = (stmt.details != null && stmt.details.address != null && !stmt.details.address.isBlank())
                ? stmt.details.address
                : "NA SHOP NO 03 BHARATPUR ROAD\nNEAR KHANDALWAL HOTEL NA MAHWA\nDAUSA RAJASTHAN 321608 KESARBAG\nMAHWA 321608\nRAJASTHAN INDIA";

        String[] addrParts = rawAddress.split("\\r?\\n");
        for (String part : addrParts) {
            if (part == null || part.isBlank()) continue;
            List<String> wrapped = TemplateUtils.wrapText(part.trim().toUpperCase(), fonts.regular, 8.0f, 270f);
            for (String line : wrapped) {
                if (curY < 672f) break;
                TemplateUtils.drawText(cs, line, textX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
                curY -= addrPitch;
            }
        }

        // Joint Holders line (inside box)
        TemplateUtils.drawText(cs, "JOINT HOLDERS :", textX, 660.0f, fonts.regular, 8.0f, 0f, 0f, 0f);

        // Nomination line (plain text OUTSIDE the box: x = 34.015 pt, y = 642.870 pt)
        String nomination = (stmt.details != null && stmt.details.nomineeName != null && !stmt.details.nomineeName.isBlank())
                ? stmt.details.nomineeName
                : "Registered";
        TemplateUtils.drawText(cs, "Nomination : " + nomination, 34.015f, 642.870f, fonts.regular, 8.0f, 0f, 0f, 0f);

        // 4. Branch & Account Metadata Grid (Right side: Origin x = 340.157 pt)
        drawBranchMeta(cs);

        // 5. Statement Period & Title Bar (y = 620.193 pt / 619.895 pt)
        String startDate = (stmt.meta != null) ? TemplateUtils.formatDateDDMMYYYY(stmt.meta.statementPeriodStart) : "01/01/2026";
        String endDate = (stmt.meta != null) ? TemplateUtils.formatDateDDMMYYYY(stmt.meta.statementPeriodEnd) : "17/08/2026";
        if (startDate.isEmpty()) startDate = "01/01/2026";
        if (endDate.isEmpty()) endDate = "17/08/2026";

        TemplateUtils.drawText(cs, "From : " + startDate + "    To : " + endDate, 34.015f, 620.193f, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, "Statement of account", 340.157f, 619.895f, fonts.regular, 12.0f, 0f, 0f, 0f);
    }

    private void drawBranchMeta(PDPageContentStream cs) throws IOException {
        String branch = (stmt.details != null && stmt.details.branch != null && !stmt.details.branch.isBlank())
                ? stmt.details.branch.trim() : "MAHWA";
        String brAddr = (stmt.details != null && stmt.details.branchAddress != null && !stmt.details.branchAddress.isBlank())
                ? stmt.details.branchAddress.trim() : "HDFC BANK LTD,\nJAIPUR ROAD ,MAHWA DIST DAUSA\nRAJASTHAN";
        String city = (stmt.details != null && stmt.details.city != null && !stmt.details.city.isBlank())
                ? stmt.details.city.trim() : "MAHWA 321608";
        String state = (stmt.details != null && stmt.details.state != null && !stmt.details.state.isBlank())
                ? stmt.details.state.trim() : "RAJASTHAN";
        String phone = (stmt.details != null && stmt.details.branchPhoneNo != null && !stmt.details.branchPhoneNo.isBlank())
                ? stmt.details.branchPhoneNo.trim() : "18002600/18001600";
        String email = (stmt.details != null && stmt.details.email != null && !stmt.details.email.isBlank())
                ? stmt.details.email.trim() : "AARISKHAN219545@GMAIL.COM";
        String custId = (stmt.details != null && stmt.details.customerRelNo != null && !stmt.details.customerRelNo.isBlank())
                ? stmt.details.customerRelNo.trim() : "338318140";
        String accNo = (stmt.details != null && stmt.details.accountNumber != null && !stmt.details.accountNumber.isBlank())
                ? stmt.details.accountNumber.trim() : "50200115012821";
        String ifsc = (stmt.details != null && stmt.details.ifsc != null && !stmt.details.ifsc.isBlank())
                ? stmt.details.ifsc.trim() : "HDFC0009689";
        String micr = (stmt.details != null && stmt.details.micr != null && !stmt.details.micr.isBlank())
                ? stmt.details.micr.trim() : "321240004";
        String brCode = (stmt.details != null && stmt.details.branchCode != null && !stmt.details.branchCode.isBlank())
                ? stmt.details.branchCode.trim() : "9689";
        String accType = (stmt.details != null && stmt.details.accountType != null && !stmt.details.accountType.isBlank())
                ? stmt.details.accountType.trim() : "BIZ PRO PLUS ACCOUNT(1482)";
        String openDate = "12/01/2026";
        String status = "Regular";

        float keyX = 340.157f;
        float colonX = 410.00f;
        float valX = 418.00f;
        float curY = 780.00f;
        float step = 8.700f; // Exact 8.70pt pitch per row

        // Line 1: Account Branch
        TemplateUtils.drawText(cs, "Account Branch", keyX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, ":", colonX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, branch, valX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        curY -= step;

        // Line 2+: Branch Address lines
        TemplateUtils.drawText(cs, "Address", keyX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, ":", colonX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        String[] bParts = brAddr.split("\\r?\\n");
        for (int i = 0; i < bParts.length; i++) {
            TemplateUtils.drawText(cs, bParts[i].trim(), valX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
            curY -= step;
        }

        // City
        TemplateUtils.drawText(cs, "City", keyX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, ":", colonX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, city, valX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        curY -= step;

        // State
        TemplateUtils.drawText(cs, "State", keyX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, ":", colonX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, state, valX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        curY -= step;

        // Phone no.
        TemplateUtils.drawText(cs, "Phone no.", keyX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, ":", colonX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, phone, valX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        curY -= step;

        // OD Limit
        TemplateUtils.drawText(cs, "OD Limit", keyX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, ":", colonX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, "0.00", valX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        curY -= step;

        // Currency
        TemplateUtils.drawText(cs, "Currency", keyX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, ":", colonX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, "INR", valX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        curY -= step;

        // Email
        TemplateUtils.drawText(cs, "Email", keyX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, ":", colonX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, email, valX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        curY -= step;

        // Cust ID
        TemplateUtils.drawText(cs, "Cust ID", keyX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, ":", colonX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, custId, valX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        curY -= step;

        // Account No
        TemplateUtils.drawText(cs, "Account No", keyX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, ":", colonX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, accNo + "  OTHER", valX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        curY -= step;

        // A/C Open Date
        TemplateUtils.drawText(cs, "A/C Open Date", keyX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, ":", colonX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, openDate, valX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        curY -= step;

        // Account Status
        TemplateUtils.drawText(cs, "Account Status", keyX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, ":", colonX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, status, valX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        curY -= step;

        // RTGS/NEFT IFSC & MICR
        TemplateUtils.drawText(cs, "RTGS/NEFT IFSC", keyX, curY, fonts.regular, 7.3f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, ":", colonX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, ifsc, valX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, "MICR : " + micr, 520.0f, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        curY -= step;

        // Branch Code
        TemplateUtils.drawText(cs, "Branch Code", keyX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, ":", colonX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, brCode, valX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        curY -= step;

        // Account Type
        TemplateUtils.drawText(cs, "Account Type", keyX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, ":", colonX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
        TemplateUtils.drawText(cs, accType, valX, curY, fonts.regular, 8.0f, 0f, 0f, 0f);
    }
}


