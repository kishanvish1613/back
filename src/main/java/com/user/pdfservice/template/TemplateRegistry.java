package com.user.pdfservice.template;

import com.user.pdfservice.model.BankTemplate;
import com.user.pdfservice.template.axis.AxisTemplate;
import com.user.pdfservice.template.boi.BoiTemplate;
import com.user.pdfservice.template.hdfc.HdfcTemplate;
import com.user.pdfservice.template.kotak.KotakTemplate;
import com.user.pdfservice.template.kotaknew.KotakNewTemplate;
import com.user.pdfservice.template.sbi.SbiTemplate;
import com.user.pdfservice.template.sbi2.SbiTemplate2;
import com.user.pdfservice.template.sbinew.SbiNewTemplate;

import java.util.EnumMap;
import java.util.Map;

public class TemplateRegistry {

  private static final Map<BankTemplate, BankPdfTemplate> MAP =
      new EnumMap<>(BankTemplate.class);

  static {
    MAP.put(BankTemplate.SBI, new SbiTemplate());
    MAP.put(BankTemplate.SBI2, new SbiTemplate2());
    MAP.put(BankTemplate.KOTAK, new KotakTemplate());
    MAP.put(BankTemplate.KOTAKNEW, new KotakNewTemplate());
    MAP.put(BankTemplate.AXIS, new AxisTemplate());
    MAP.put(BankTemplate.SBINEW, new SbiNewTemplate());
    MAP.put(BankTemplate.BOI, new BoiTemplate());
    MAP.put(BankTemplate.HDFC, new HdfcTemplate());
  }

  public static BankPdfTemplate get(BankTemplate template) {
    return MAP.get(template);
  }
}