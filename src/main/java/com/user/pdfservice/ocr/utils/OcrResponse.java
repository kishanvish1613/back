package com.user.pdfservice.ocr.utils;

import com.user.pdfservice.ocr.OcrResult;

public class OcrResponse {
    public boolean encrypted;
    public String error;
    public OcrResult result;

    public static OcrResponse encrypted(String message) {
        OcrResponse r = new OcrResponse();
        r.encrypted = true;
        r.error = message;
        r.result = null;
        return r;
    }

    public static OcrResponse success(OcrResult data){
        OcrResponse r = new OcrResponse();
        r.encrypted = false;
        r.result = data;
        return r;
    }
}
