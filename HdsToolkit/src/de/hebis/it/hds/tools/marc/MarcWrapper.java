/*
 * Copyright 2016, 2017 by HeBIS (www.hebis.de).
 * 
 * This file is part of HeBIS HdsToolkit.
 *
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the code.  If not, see http://www.gnu.org/licenses/agpl>.
 */
package de.hebis.it.hds.tools.marc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marc4j.MarcPermissiveStreamReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlWriter;
import org.marc4j.converter.CharConverter;
import org.marc4j.marc.Record;
import org.marc4j.marc.VariableField;

/**
 * Addon to Marc4j to work with also with Strings (instead of character streams, java.io.*).<br>
 * 
 * @author 2016-05-01 Uwe Reh, HeBIS-IT
 * @version 2016-09-26 uh changed all methods to 'static'.
 * @version 2017-03-22 uh revised
 */
public class MarcWrapper {
   static final Logger  LOG     = LogManager.getLogger(MarcWrapper.class);
   // RegEx-Pattern für die Methode normalizeUnicode(String) vordefinieren
   static final Pattern pattern = Pattern.compile("(\\\\u(\\w{4}))|(#(29|30|31);)");

   /**
    * Prepare a converter for the use in a {@link MarcWriter}.<br/>
    * The converter uses {@link Normalizer#normalize(CharSequence, java.text.Normalizer.Form)} to get {@link Normalizer.Form.NFC} 
    *
    */
   private static class NormalizeToNFC extends CharConverter {
      @Override
      public String convert(char data[]) {
         return Normalizer.normalize(new String(data), Normalizer.Form.NFC);
      }
   }

   /**
    * Writes a record to a String. (@see org.marc4j.MarcStreamWriter)
    * 
    * @param marc The record to write
    * @return The String representation of the record (ISO 2709), or 'NULL' in case of an error.
    */
   public static String marcToString(Record marc) {
      try {
         ByteArrayOutputStream b = new ByteArrayOutputStream();
         MarcWriter marcwriter = new MarcStreamWriter(b, "UTF-8", true);
         marcwriter.setConverter(new NormalizeToNFC());
         marcwriter.write(marc);
         marcwriter.close();
         return b.toString("UTF-8");
      } catch (UnsupportedEncodingException e) {
         e.printStackTrace();
      }
      return null;
   }

   /**
    * A simple replacement for {@link Record}.toString() 
    * 
    * @param marc The record to pretty print
    * @return A readable representation of the record, or "" in case of an error.
    */
   public static String marcToText(Record marc) {
      StringBuilder out = new StringBuilder();
      for (Object obj : marc.getVariableFields()) {
         VariableField feld = (VariableField) obj;
         out.append(feld.toString());
         out.append('\n');
      }
      return out.toString();
   }

   /**
    * Reads a record from a string
    * 
    * @param data A string repesentating the record (ISO 2709).
    * @return The first record found in the string. Or NULL, if no record could be found.
    */
   public static Record string2Marc(String data) {
      try {
         String normalizedData = normalizeUnicode(data);
         InputStream dataStream = new ByteArrayInputStream(normalizedData.getBytes("UTF-8"));
         MarcPermissiveStreamReader mr = new MarcPermissiveStreamReader(dataStream, true, true);
         return mr.hasNext() ? mr.next() : null;
      } catch (Exception e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      }
   }

   /**
    * Weird filter for encoded characters and control characters.<br/>
    * TODO 'get rid of'
    * 
    * @param string The raw string
    * @return The filtered string
    */
   private static String normalizeUnicode(String string) {
      Matcher matcher = pattern.matcher(string);
      StringBuffer result = new StringBuffer();
      int prevEnd = 0;
      while (matcher.find()) {
         result.append(string.substring(prevEnd, matcher.start()));
         result.append(getChar(matcher.group()));
         prevEnd = matcher.end();
      }
      result.append(string.substring(prevEnd));
      string = result.toString();
      return string;
   }

   /**
    * Helper to {@link #normalizeUnicode(String)}
    * @param charCodePoint A string with 'suspect' data
    * @return a replacement for the input.
    */
   private static String getChar(String charCodePoint) {
      int charNum;
      if (charCodePoint.startsWith("\\u")) {
         charNum = Integer.parseInt(charCodePoint.substring(2), 16);
         // Das konvertierte Zeichen mit Platzhalter ausgeben, weil sich sonst die Feldlänge im Marc ändert.
         return "" + (char) charNum + "    ";
      }
      // Steuerzeichen
      charNum = Integer.parseInt(charCodePoint.substring(1, 3));
      return "" + (char) charNum;
   }

   /**
    * Converts a record in ISO 2709 to marcXML
    * 
    * @param raw The record as string. (formated according ISO 2709)
    * @return The record in XML representation
    */
   public static String string2XML(String raw) {
      return marc2XML(string2Marc(raw));
   }

   /**
    * Converts a record in marcXML to ISO 2709
    * 
    * @param marc The record as XML.
    * @return The record formated according ISO 2709
    */
   public static String marc2XML(Record marc) {
      if (marc == null) return null;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      MarcXmlWriter xmlWriter = new MarcXmlWriter(baos, true);
      xmlWriter.write(marc);
      xmlWriter.close();
      String ret = "";
      try {
         ret = baos.toString("UTF-8");
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException("Should never Happens.", e);
      }
      int startPos = ret.indexOf("<record>") - 2;
      int endPos = ret.indexOf("</collection>") - 1;
      return ret.substring(startPos, endPos);
   }

}
