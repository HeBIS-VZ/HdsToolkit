/*
 * Copyright 2013-2017 by HeBIS (www.hebis.de).
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
package de.hebis.it.hds.tools.ids;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Simple data type for a International Standard Book Number (ISBN-10 + ISBN-13)
 * 
 * @author Uwe 2013-01-09
 * @version 2017-03-23 uh revised and moved to toolkit
 * 
 */
public class ISBN {
   static final Logger LOG     = LogManager.getLogger(ISBN.class);
   String              isbnRaw = null;
   String              isbn10  = null;
   String              isbn13  = null;

   /**
    * Constructs new ISBN
    * 
    * @param isbnString A text, which should contain a ISBN.<br>
    * The instance holds the first found ISBN in the Text.
    */
   public ISBN(String isbnString) {
      if (isbnString == null) throw new NumberFormatException("ISBN: the Parameter is mandatory.");
      isbnRaw = getClean(isbnString);
      switch (isbnRaw.length()) {
         case 10:
            isbn10 = rebuild(isbnRaw);
            if (isbn10 != null) isbn13 = expand();
            break;
         case 13:
            isbn13 = rebuild(isbnRaw);
            if (isbn13 != null) isbn10 = shrink();
            break;
         default:
            LOG.warn("No propper ISBN found in: " + isbnString);
      }
   }

   /**
    * Factory method that builds new ISBN
    * 
    * @param isbnString A text, which should contain a ISBN
    * @return A new ISBN build from the first found 'ISBN' in the Text
    */
   public static ISBN valueOf(String isbnString) {
      return new ISBN(isbnString);
   }

   /**
    * Extracting the most likely ISBN from a given Text.<br>
    * 1. extract all hyphens ('-')<br>
    * 2. replace all invalid characters with a space<br>
    * 3. split the text in parts<br>
    * 4. return the first part which has the length of 10 or 13<br>
    * 5. if no isbn with exact length is found, return the part with the nearest length<br>
    * 
    * @param isbnText Text containing a ISBN
    * @return Most likely (ISBN) part of the given Text (Hyphens are striped)
    */
   private static String getClean(String isbnText) {
      int offset = Integer.MAX_VALUE - 14;
      int pos = 0;
      String tmp = isbnText.replace("-", "").toUpperCase().replaceAll("[^\\dX]", " ");
      String[] parts = tmp.trim().split(" ");
      for (int i = 0; i < parts.length; i++) {
         int len = parts[i].length();
         if ((len == 10) || (len == 13)) return parts[i];
         if ((len > 10 - offset) && (len < 13 + offset)) {
            pos = i; // remember position
            // the next parts have to be closer
            if (len > 13) offset = len - 13 - 1;
            else if (len < 10) offset = 10 - len - 1;
         }
      }
      return parts[pos];
   }

   /**
    * Is the ISBN syntactically correct.
    *
    * @return TRUE, if the found ISBN is valid.
    */
   public boolean isValid() {
      if (isbnRaw.equals(isbn10) || isbnRaw.equals(isbn13)) return true;
      return false;
   }

   /**
    * Get the found (potential invalid) ISBN.
    *
    * @return The stripped ISBN found in the original text
    */
   @Override
   public String toString() {
      return isbnRaw;
   }

   /**
    * Get as ISBN-10
    *
    * @return Short ISBN with recomputed check digit. Or NULL if the ISBN isn't valid
    */
   public String toString10() {
      return isbn10;
   }

   /**
    * Get as ISBN-13
    *
    * @return Long ISBN with recomputed check digit. Or NULL if the ISBN isn't valid
    */
   public String toString13() {
      return isbn13;
   }

   /**
    * Converter ISBN-10 to ISBN-13
    *
    * @return The long Format of the ISBN or NULL if the ISBN isn't valid.
    */
   private String expand() {
      if (isbn10 == null) return null;
      String tmp = "978" + isbn10;
      return tmp.substring(0, tmp.length() - 1) + computeCheckDigit(tmp);
   }

   /**
    * Converter ISBN-13 to ISBN-10
    *
    * @return The long Format of the ISBN or NULL if the ISBN isn't valid.
    */
   private String shrink() {
      if (isbn13 == null) return null;
      String tmp = isbn13.substring(3, 12);
      return tmp + computeCheckDigit(tmp);
   }

   /**
    * Strip checkdigit and build isbn with newly computed checkdigit.
    *
    * @param rawIsbn ISBN to process
    * @return ISBN with new checkdigit
    */
   private String rebuild(String rawIsbn) {
      return rawIsbn.substring(0, rawIsbn.length() - 1) + computeCheckDigit(rawIsbn);
   }

   /**
    * Recomputes the check digit
    * 
    * @param teststring the original ISBN
    * @return The computed check digit or '#' if the length of the input is wrong
    */
   private static char computeCheckDigit(String teststring) {
      int sum = 0;
      int rest = 0;
      int digit;
      switch (teststring.length()) {
         case 9:
         case 10: // mod 11
            for (int i = 1; i < 10; i++) {
               digit = (teststring.charAt(i - 1) - '0');
               sum += digit * i;
            }
            rest = sum % 11;
            return (char) ((rest == 10) ? 'X' : rest + '0');
         case 13: // mod 10
            for (int i = 0; i < 12; i++) {
               digit = (teststring.charAt(i) - '0');
               sum += (i % 2 == 0) ? digit * 1 : digit * 3;
            }
            rest = 10 - (sum % 10);
            return (char) ((rest == 0) ? '0' : rest + '0');
         default:
            LOG.warn("Method is defined for ISBN-10 or ISBN-13 only.");
            return '#';
      }
   }

   /**
    * Poor man's unit test
    * 
    * @param args All Parameters are ignored
    */
   public static void main(String[] args) {
      // Tests
      doit("bla 999-11 qay 978-3-7657-2781-8 Preis:22,33", "Valid ISBN-13 with noise.");
      doit("bla 999-11 qay 978-3-7657- Preis:22,33", "Invalid ISBN.");
      doit("978-3-7657-2781-0", "ISBN-13 whith with wrong check digit.");
      doit("88-14-05814-8", "ISBN-10.");
      doit("041534638X", "ISBN-10.");
      doit("9780415346382", "ISBN-13.");
   }

   private static void doit(String test, String msg) {
      System.out.println("\n" + msg + ": " + test);
      ISBN me = ISBN.valueOf(test);
      System.out.println("--> " + me.toString());
      System.out.println("--> ___" + me.toString10());
      System.out.println("--> " + me.toString13());
   }

}
