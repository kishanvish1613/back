package com.user.pdfservice.template.boi;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;

@SuppressWarnings("deprecation")
public class PageCursor {

    private final PDDocument doc;
    private final PDType1Font font;

    private PDPage page;
    private PDPageContentStream cs;
    private float y;

    private final float NEW_PAGE_HEADER_START = 737.5f;
    private final float NEW_PAGE_TXN_START = 701.32f;
    private int pgn = 1;

    public PageCursor(
        PDDocument doc,
        PDType1Font font,
        PDPage startPage,
        float startY
    ) throws Exception {
        this.doc = doc;
        this.font = font;
        this.page = startPage;
        this.y = startY;

        this.cs = new PDPageContentStream(doc, page, AppendMode.APPEND, false);
    }

    public PDPageContentStream cs() {
        return cs;
    }
    
    public PDPage page() {
        return page;
    }

    public PDDocument doc() {
        return doc;
    }
    
    public float y() {
        return y;
    }

    public void y(float newY) {
        this.y = newY;
    }

    public void moveDown(float dy) {
        y -= dy;
    }

    public void ensureSpace(float requiredHeight) throws Exception {
        if (y - requiredHeight < 36f) {
            nextPage();
        }
    }

    public void nextPage() throws Exception {
        cs.close();

        PDRectangle A4_EXACT = new PDRectangle(595f, 842f);
        page = new PDPage(A4_EXACT);
        doc.addPage(page);

        cs = new PDPageContentStream(doc, page, AppendMode.APPEND, true);

        y = NEW_PAGE_TXN_START;
    }

    public void nextPageWithImages(
        Map<String, BufferedImage> extractedImages,
        int start,
        int end
    ) throws Exception {
        cs.close();
        PDRectangle A4_EXACT = new PDRectangle(595f, 842f);
        page = new PDPage(A4_EXACT);
        doc.addPage(page);
        cs = new PDPageContentStream(doc, page, AppendMode.APPEND, true);

        PDResources resources = page.getResources();
        if (resources == null) {
            resources = new PDResources();
            page.setResources(resources);
        }

        for (int i = start; i <= end; i++) {
            String key = "img" + i;

            var img = extractedImages.get(key);
            if (img == null) continue; // skip if missing
            
            PDImageXObject ximg = LosslessFactory.createFromImage(doc, img);
            COSName cosName = COSName.getPDFName(key);
            
            resources.put(cosName, ximg);
        }

        if (start == 1 && end == 6) {
            addLink(
                page,
                43.17f,
                364.32f,
                551.83f,
                448.32f,
                "https://www.kotak.bank.in/en/home.html"
            );
        } else if (start == 7 && end == 8) {
            addLink(
                page,
                352.53f,
                537.12f,
                380.7f,
                544.8f,
                "https://rbi.org.in/scripts/bs_viewcontent.aspx?Id=4235"
            );
            addLink(
                page,
                348.39f,
                529.44f,
                410.68f,
                537.12f,
                "https://rbi.org.in/scripts/Fema.aspx"
            );
            addLink(
                page,
                439.81f,
                529.44f,
                492.2f,
                537.12f,
                "https://rbi.org.in/scripts/bs_viewcontent.aspx?Id=4080"
            );
        }

        y = NEW_PAGE_TXN_START;
    }

    private static void addLink(
        PDPage page,
        float llx,
        float lly,
        float urx,
        float ury,
        String url
    ) throws IOException {
        PDAnnotationLink link = new PDAnnotationLink();

        PDRectangle rect = new PDRectangle();
        rect.setLowerLeftX(llx);
        rect.setLowerLeftY(lly);
        rect.setUpperRightX(urx);
        rect.setUpperRightY(ury);
        link.setRectangle(rect);

        PDActionURI action = new PDActionURI();
        action.setURI(url);
        link.setAction(action);
        link.setBorderStyle(null);
        link.setColor(
            new PDColor(new float[] { 0f, 0f, 1f }, PDDeviceRGB.INSTANCE)
        );
        link.setPrinted(true);

        page.getAnnotations().add(link);
    }

    public void close() throws Exception {
        cs.close();
    }

}
