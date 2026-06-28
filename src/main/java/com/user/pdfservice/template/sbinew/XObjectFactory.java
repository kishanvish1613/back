package com.user.pdfservice.template.sbinew;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;

public final class XObjectFactory {

    private XObjectFactory() {}

    public static PDFormXObject createFormXObject(
        PDDocument doc,
        PDRectangle bbox,
        AffineTransform formMatrix, // <-- IMPORTANT
        Consumer<PDPageContentStream> streamWriter,
        Consumer<COSDictionary> dictMutator
    ) throws IOException {
        PDFormXObject form = new PDFormXObject(doc);

        // Required
        form.setBBox(bbox);

        // Optional Form-local CTM
        if (formMatrix != null) {
            form.setMatrix(formMatrix);
        }

        // Write the content stream
        try (
            OutputStream out = form.getCOSObject().createOutputStream();
            PDPageContentStream cs = new PDPageContentStream(doc, form, out);
        ) {
            streamWriter.accept(cs);
        }

        if (dictMutator != null) {
            dictMutator.accept(form.getCOSObject());
        }

        return form;
    }

    public record RawFormDef(
        String name, // e.g. "Xf3"
        PDRectangle bbox, // e.g. [0 0 16 19]
        String stream // raw PDF operators
    ) {}

    public static Map<String, PDFormXObject> createRawForms(
        PDDocument doc,
        List<RawFormDef> defs
    ) throws IOException {
        Map<String, PDFormXObject> forms = new LinkedHashMap<>();

        for (RawFormDef def : defs) {
            PDFormXObject form = new PDFormXObject(doc);
            form.setBBox(def.bbox());

            try (OutputStream out = form.getCOSObject().createOutputStream()) {
                out.write(def.stream().getBytes(StandardCharsets.US_ASCII));
            }

            forms.put(def.name(), form);
        }

        return forms;
    }

    public static void attachFormsToPage(
        PDPage page,
        Map<String, PDFormXObject> forms
    ) {
        PDResources res = page.getResources();
        if (res == null) {
            res = new PDResources();
            page.setResources(res);
        }

        for (var e : forms.entrySet()) {
            res.put(COSName.getPDFName(e.getKey()), e.getValue());
        }
    }
}
