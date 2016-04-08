/*
 * This file is part of OpenAstexViewer.
 *
 * OpenAstexViewer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenAstexViewer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OpenAstexViewer.  If not, see <http://www.gnu.org/licenses/>.
 */

package astex;

// CRC32 calculation.
// This software is in the public domain.
//

import java.math.*;

/**
 * <P> Calculates the CRC32 - 32 bit Cyclical Redundancy Check
 * <P> This check is used in numerous systems to verify the integrity
 * of information.  It's also used as a hashing function.  Unlike a regular
 * checksum, it's sensitive to the order of the characters.
 * It produces a 32 bit (Java <CODE>int</CODE>.
 * <P>
 * This Java programme was translated from a C version I had written.
 * <P> This software is in the public domain.
 *
 * <P>
 * When calculating the CRC32 over a number of strings or byte arrays
 * the previously calculated CRC is passed to the next call.  In this
 * way the CRC is built up over a number of items, including a mix of
 * strings and byte arrays.
 * <P>
 * <PRE>
 * CRC32 crc = new CRC32();
 * int crcCalc = crc.crc32("Hello World");
 * crcCalc = crc.crc32("How are you?", crcCalc);
 * crcCalc = crc.crc32("I'm feeling really good, how about you?", crcCalc);
 * </PRE>
 * The line <CODE>int crcCalc = crc.crc32("Hello World");</CODE> is equivalent
 * to <CODE>int crcCalc = crc.crc32("Hello World", -1);</CODE>.
 * When starting a new CRC calculation the "previous crc" is set to
 * 0xFFFFFFFF (or -1).
 * <P>
 * The table only needs to be built once.  You may use it to generate
 * many CRC's.
 * <CODE>
 *
 * @author Michael Lecuyer (mjl@theorem.com)
 *
 * @version 1.1 August 11, 1998
 *
 */
public class CRC32
{
   static int CRCTable[] = null;   // CRC Lookup table

   /**
   * Tests CRC32.
   * <BR>Eg: <SAMP> java CRC32 "Howdy, I'm A Cowboy"
   *
   * @param args the string used to calculate the CRC32
   */
   public static void main(String args[])
   {
      if (args.length == 0)
      {
         System.out.println("Usage CRC32 [string to calculate CRC32]");
         System.exit(1);
      }

      System.out.println("CRC for [" + args[0] + "] is " + new CRC32().crc32(args[0]));
   }

   private static int crc;  // currently calculated crc (used in conversion to byte array)

   /**
   * Constructor constructs the lookup table.
   * 
   */
   CRC32()
   {
      buildCRCTable();     
   }

   /**
    * Just build a plain old fashioned table based on good, old fashioned
    * values like the CRC32_POLYNOMIAL.  The table is of a fixed size.
    */
   private static void buildCRCTable()
   {
      final int CRC32_POLYNOMIAL = 0xEDB88320;

      int i, j;
      int crc;

      CRCTable = new int[256];

      for (i = 0; i <= 255; i++)
      {
         crc = i;
         for (j = 8; j > 0; j--)
            if ((crc & 1) == 1)
               crc = (crc >>> 1) ^ CRC32_POLYNOMIAL;
            else
               crc >>>= 1;
         CRCTable[i] = crc;
      }
   }

   /**
    * Convenience mithod for generating a CRC from a single <CODE>String</CODE>.
    *
    * @param buffer string to generate the CRC32 
    *
    * @return 32 bit CRC
    */
   public static int crc32(String buffer)
   {
      return crc32(buffer, 0xFFFFFFFF);
   }
   
   /**
    * Convenience method for generating a CRC from a <CODE>byte</CODE> array.
    *
    * @param buffer byte array to generate the CRC32 
    *
    * @return 32 bit CRC
    */
   public static int crc32(byte buffer[])
   {
      return crc32(buffer, 0xFFFFFFFF);
   }
   
   /**
    * Convenience method for generating a CRC from a series of <CODE>String</CODE>'s.
    *
    * @param buffer string to generate the CRC32 
    * @param crc previously generated CRC32.
    *
    * @return 32 bit CRC
    */
   public static int crc32(String buffer, int crc)
   {
      return crc32(buffer.getBytes(), crc);
   }

   /**
    * Convenience method for generating a CRC from a series of <CODE>byte</CODE> arrays.
    *
    * @param buffer byte array to generate the CRC32 
    * @param crc previously generated CRC32.
    *
    * @return 32 bit CRC
    */
   public static int crc32(byte buffer[], int crc)
   {
      return crc32(buffer, 0, buffer.length, crc);
   }

   /**
    * General CRC generation function.
    *
    * @param buffer byte array to generate the CRC32 
    * @param start byte start position 
    * @param count number of byte's to include in CRC calculation 
    * @param crc previously generated CRC32.
    *
    * @return 32 bit CRC
    */
   public static int crc32(byte buffer[], int start, int count, int lastcrc)
   {
      int temp1, temp2;
      int i = start;

      if(CRCTable == null){
	  buildCRCTable();
      }

      crc = lastcrc;

      while (count-- != 0)
      {
         temp1 = crc >>> 8;
         temp2 = CRCTable[(crc ^ buffer[i++]) & 0xFF];
         crc = temp1 ^ temp2;
      }

      return crc;
   }

   /**
    * General CRC generation function.
    *
    * @param buffer byte array to generate the CRC32 
    * @param start byte start position 
    * @param count number of byte's to include in CRC calculation 
    * @param crc previously generated CRC32.
    *
    * @return 32 bit CRC
    */
    public static int crc32(int buffer[], int start, int count, int lastcrc) {
	int temp1, temp2;
	int i = start;

	if(CRCTable == null){
	    buildCRCTable();
	}

	crc = lastcrc;
	
	while (count-- != 0) {
	    int w = buffer[i++];
	    
	    for(int b = 0; b < 4; b++){
		int bb = (w & 0xff);
		w >>= 8;
		temp1 = crc >>> 8;
		temp2 = CRCTable[(crc ^ bb) & 0xFF];
		crc = temp1 ^ temp2;
	    }
	}

	return crc;
    }
}
