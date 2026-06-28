package com.user.pdfservice.template.sbi;

import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;

public class Fonts {

  public final Font title;
  public final Font header;
  public final Font body;
  public final Font small;

  public Fonts() throws Exception {

    BaseFont helvetica = BaseFont.createFont(
        BaseFont.HELVETICA,
        BaseFont.WINANSI,
        BaseFont.NOT_EMBEDDED
    );

    BaseFont helveticaBold = BaseFont.createFont(
        BaseFont.HELVETICA_BOLD,
        BaseFont.WINANSI,
        BaseFont.NOT_EMBEDDED
    );

    this.title  = new Font(helvetica, 12);
    this.header = new Font(helveticaBold, 10);
    this.body   = new Font(helvetica, 9);
    this.small  = new Font(helvetica, 8);
  }
}