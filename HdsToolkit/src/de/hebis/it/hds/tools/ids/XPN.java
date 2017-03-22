/*
 * Copyright 2014-2017 by HeBIS (www.hebis.de).
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
 * Static tools to convert, test and compare the ids of the Pica library systems.<br/>
 * - IPN = internal production number (one to eight digits)<br/>
 * - PPN = pica production number (the IPN plus one character as check digit)<br/>
 * TODO extend for new ids with 10(+1) digits
 * 
 * @author 2014 Uwe
 * @version 2017-03-22 uh revised
 */
public class XPN {
   static final Logger LOG = LogManager.getLogger(XPN.class);

   /**
    * Converts a PPN to a IPN
    * 
    * @param ppn The PPN to convert
    * @return The IPN (= stripped PPN, no check digit no leading zeros)
    */
   public static String ppnToIpn(String ppn) {
      return ppn.substring(0, ppn.length() - 1).replaceFirst("0*", "");
   }

   /**
    * Converts a PPN to a IPN
    * 
    * @param ppn The PPN to convert
    * @return The IPN
    */
   public static int ppnToInt(String ppn) {
      return Integer.valueOf(ppnToIpn(ppn));
   }

   /**
    * Converts a IPN to a PPN
    * 
    * @param ipn The IPN to convert
    * @return The computed PPN
    */
   public static String ipnToPpn(int ipn) {
      return ipnToPpn(String.valueOf(ipn));
   }

   /**
    * Converts a IPN to a PPN
    * 
    * @param ipn The IPN to convert
    * @return The computed PPN
    */
   public static String ipnToPpn(String ipn) {
      StringBuilder ppn = new StringBuilder();
      int len = ipn.length();
      if (len > 8) {
         LOG.error("Es werden bis jetzt nur 9-Stellige PPNs unterstützt: " + ipn);
         return "0";
      }
      char checkdigit = computeCheckDigit(ipn); // Prüfziffer ermitteln
      ppn.append("00000000".substring(len, 8)); // führende Nullen ergänzen
      ppn.append(ipn); // Die eigentliche Kennung 'IPN'
      ppn.append(checkdigit);
      return ppn.toString();
   }

   /**
    * Validates a PPN (length and check digit).
    * 
    * @param testppn The PPN to validate
    * @return The validated PPN it is formal correct, NULL if not.
    */
   public static String checkPpn(String testppn) {
      String newppn = ipnToPpn(ppnToIpn(testppn));
      return (newppn.equals(testppn.trim().toUpperCase())) ? newppn : null;
   }

   /**
    * Calculate the check digit (mod-11)
    * 
    * @param ipn The number which needs a check digit
    * @return The computed check digit [0..9X]
    */
   public static char computeCheckDigit(String ipn) {
      int checksum = 0;
      int weight = 2;
      for (int i = ipn.length() - 1; i >= 0; --i) {
         int val = ipn.charAt(i) - '0';
         checksum += val * weight++;
      }
      checksum = 11 - (checksum % 11);
      if (checksum == 11) return '0';
      return (checksum == 10) ? 'X' : (char) ('0' + checksum);
   }

   /**
    * Simple Tests
    * 
    * @param args
    */
   public static void main(String[] args) {
      System.out.println(ipnToPpn("1"));
      System.out.println(ppnToIpn("00019"));
      System.out.println(ipnToPpn("35571202"));
      System.out.println("#######");
      System.out.println(ipnToPpn("21050201")); // 0
      System.out.println(ipnToPpn("21050206")); // 1
      System.out.println(ipnToPpn("21050200")); // 2
      System.out.println(ipnToPpn("21050219")); // 3
      System.out.println(ipnToPpn("35552097")); // 4
      System.out.println(ipnToPpn("21050204")); // 5
      System.out.println(ipnToPpn("21050209")); // 6
      System.out.println(ipnToPpn("21050203")); // 7
      System.out.println(ipnToPpn("21050208")); // 8
      System.out.println(ipnToPpn("13771932")); // 9
      System.out.println(ipnToPpn("21050207")); // X
   }

}
