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

/*
  PeriodicTable.java

  Data and utility functions for periodic table
*/

import java.io.Serializable;
import java.util.*;
import java.io.*;

public class PeriodicTable {
    public static final AtomicElement elements[] = new AtomicElement[] {
	new AtomicElement(  0, "h+",    0.00000, 15,  0,  0,  0,  0,  0),
	new AtomicElement(  1, "H",     1.00797,  1,  0,  0,  0,  8,  0),
	new AtomicElement(  2, "He",    4.00260, 15,  0,  0,  0,  8, 17),
	new AtomicElement(  3, "Li",    6.93900,  1,  0,  0,  0,  7,  0),
	new AtomicElement(  4, "Be",    9.01220,  2,  0,  0,  0,  7,  1),
	new AtomicElement(  5, "B",    10.81100,  3,  0,  0,  0,  7, 12),
	new AtomicElement(  6, "C",    12.01115,  4,  0,  0,  0,  7, 13),
	new AtomicElement(  7, "N",    14.00670,  3,  5,  0,  0,  7, 14),
	new AtomicElement(  8, "O",    15.99940,  2,  0,  0,  0,  7, 15),
	new AtomicElement(  9, "F",    18.99840,  1,  0,  0,  0,  7, 16),
	new AtomicElement( 10, "Ne",   20.18300, 15,  0,  0,  0,  7, 17),
	new AtomicElement( 11, "Na",   22.98980,  1,  0,  0,  0,  6,  0),
	new AtomicElement( 12, "Mg",   24.31200,  2,  0,  0,  0,  6,  1),
	new AtomicElement( 13, "Al",   26.98150,  3,  0,  0,  0,  6, 12),
	new AtomicElement( 14, "Si",   28.08600,  4,  0,  0,  0,  6, 13),
	new AtomicElement( 15, "P",    30.97380,  3,  5,  0,  0,  6, 14),
	new AtomicElement( 16, "S",    32.06400,  2,  4,  6,  0,  6, 15),
	new AtomicElement( 17, "Cl",   35.45300,  1,  3,  5,  7,  6, 16),
	new AtomicElement( 18, "Ar",   39.94800, 15,  0,  0,  0,  6, 17),
	new AtomicElement( 19, "K",    39.10200,  1,  0,  0,  0,  5,  0),
	new AtomicElement( 20, "Ca",   40.08000,  2,  0,  0,  0,  5,  1),
	new AtomicElement( 21, "Sc",   44.95600,  3,  0,  0,  0,  5,  2),
	new AtomicElement( 22, "Ti",   47.90000,  3,  4,  0,  0,  5,  3),
	new AtomicElement( 23, "V",    50.94200,  2,  3,  4,  5,  5,  4),
	new AtomicElement( 24, "Cr",   51.99600,  2,  3,  6,  0,  5,  5),
	new AtomicElement( 25, "Mn",   54.93800,  2,  3,  4,  6,  5,  6),
	new AtomicElement( 26, "Fe",   55.84700,  2,  3,  4,  6,  5,  7),
	new AtomicElement( 27, "Co",   58.93320,  2,  3,  0,  0,  5,  8),
	new AtomicElement( 28, "Ni",   58.71000,  2,  3,  0,  0,  5,  9),
	new AtomicElement( 29, "Cu",   63.54600,  1,  2,  0,  0,  5, 10),
	new AtomicElement( 30, "Zn",   65.37000,  2,  0,  0,  0,  5, 11),
	new AtomicElement( 31, "Ga",   69.72000,  3,  0,  0,  0,  5, 12),
	new AtomicElement( 32, "Ge",   72.59000,  2,  4,  0,  0,  5, 13),
	new AtomicElement( 33, "As",   74.92160,  3,  5,  0,  0,  5, 14),
	new AtomicElement( 34, "Se",   78.96000,  2,  4,  6,  0,  5, 15),
	new AtomicElement( 35, "Br",   79.90400,  1,  3,  5,  7,  5, 16),
	new AtomicElement( 36, "Kr",   83.80000, 15,  0,  0,  0,  5, 17),
	new AtomicElement( 37, "Rb",   85.47000,  1,  0,  0,  0,  4,  0),
	new AtomicElement( 38, "Sr",   87.62000,  2,  0,  0,  0,  4,  1),
	new AtomicElement( 39, "Y",    88.90500,  3,  0,  0,  0,  4,  2),
	new AtomicElement( 40, "Zr",   91.22000,  4,  0,  0,  0,  4,  3),
	new AtomicElement( 41, "Nb",   92.90600,  3,  5,  0,  0,  4,  4),
	new AtomicElement( 42, "Mo",   95.94000,  3,  4,  5,  6,  4,  5),
	new AtomicElement( 43, "Tc",   98.90620,  7,  0,  0,  0,  4,  6),
	new AtomicElement( 44, "Ru",  101.07000,  2,  3,  4,  6,  4,  7),
	new AtomicElement( 45, "Rh",  102.90500,  2,  3,  4,  0,  4,  8),
	new AtomicElement( 46, "Pd",  106.40000,  2,  4,  0,  0,  4,  9),
	new AtomicElement( 47, "Ag",  107.86800,  1,  0,  0,  0,  4, 10),
	new AtomicElement( 48, "Cd",  112.40000,  2,  0,  0,  0,  4, 11),
	new AtomicElement( 49, "In",  114.82000,  3,  0,  0,  0,  4, 12),
	new AtomicElement( 50, "Sn",  118.69000,  2,  4,  0,  0,  4, 13),
	new AtomicElement( 51, "Sb",  121.75000,  3,  5,  0,  0,  4, 14),
	new AtomicElement( 52, "Te",  127.60000,  2,  4,  6,  0,  4, 15),
	new AtomicElement( 53, "I",   126.90440,  1,  3,  5,  7,  4, 16),
	new AtomicElement( 54, "Xe",  131.30000, 15,  0,  0,  0,  4, 17),
	new AtomicElement( 55, "Cs",  132.90500,  1,  0,  0,  0,  3,  0),
	new AtomicElement( 56, "Ba",  137.33000,  2,  0,  0,  0,  3,  1),
	new AtomicElement( 57, "La",  138.91000,  3,  0,  0,  0,  1,  2),
	new AtomicElement( 58, "Ce",  140.12000,  3,  4,  0,  0,  1,  3),
	new AtomicElement( 59, "Pr",  140.90700,  3,  4,  0,  0,  1,  4),
	new AtomicElement( 60, "Nd",  144.24000,  3,  0,  0,  0,  1,  5),
	new AtomicElement( 61, "Pm",  145.00000,  3,  0,  0,  0,  1,  6),
	new AtomicElement( 62, "Sm",  150.35000,  2,  3,  0,  0,  1,  7),
	new AtomicElement( 63, "Eu",  151.96000,  2,  3,  0,  0,  1,  8),
	new AtomicElement( 64, "Gd",  157.25000,  3,  0,  0,  0,  1,  9),
	new AtomicElement( 65, "Tb",  158.92400,  3,  4,  0,  0,  1, 10),
	new AtomicElement( 66, "Dy",  162.50000,  3,  0,  0,  0,  1, 11),
	new AtomicElement( 67, "Ho",  164.93000,  3,  0,  0,  0,  1, 12),
	new AtomicElement( 68, "Er",  167.26000,  3,  0,  0,  0,  1, 13),
	new AtomicElement( 69, "Tm",  168.93400,  2,  3,  0,  0,  1, 14),
	new AtomicElement( 70, "Yb",  173.04000,  2,  3,  0,  0,  1, 15),
	new AtomicElement( 71, "Lu",  174.97000,  3,  0,  0,  0,  1, 16),
	new AtomicElement( 72, "Hf",  178.49000,  4,  0,  0,  0,  3,  3),
	new AtomicElement( 73, "Ta",  180.94800,  5,  0,  0,  0,  3,  4),
	new AtomicElement( 74, "W",   183.85000,  3,  4,  5,  6,  3,  5),
	new AtomicElement( 75, "Re",  186.20000,  2,  4,  6,  7,  3,  6),
	new AtomicElement( 76, "Os",  190.20000,  2,  3,  4,  6,  3,  7),
	new AtomicElement( 77, "Ir",  192.20000,  2,  3,  4,  6,  3,  8),
	new AtomicElement( 78, "Pt",  195.09000,  2,  4,  0,  0,  3,  9),
	new AtomicElement( 79, "Au",  196.96700,  1,  3,  0,  0,  3, 10),
	new AtomicElement( 80, "Hg",  200.59000,  1,  2,  0,  0,  3, 11),
	new AtomicElement( 81, "Tl",  204.37000,  1,  3,  0,  0,  3, 12),
	new AtomicElement( 82, "Pb",  207.19000,  2,  4,  0,  0,  3, 13),
	new AtomicElement( 83, "Bi",  208.98000,  3,  5,  0,  0,  3, 14),
	new AtomicElement( 84, "Po",  209.00000,  2,  4,  0,  0,  3, 15),
	new AtomicElement( 85, "At",  210.00000,  1,  3,  5,  7,  3, 16),
	new AtomicElement( 86, "Rn",  222.00000, 15,  0,  0,  0,  3, 17),
	new AtomicElement( 87, "Fr",  223.00000,  1,  0,  0,  0,  2,  0),
	new AtomicElement( 88, "Ra",  226.03000,  2,  0,  0,  0,  2,  1),
	new AtomicElement( 89, "Ac",  227.00000,  3,  0,  0,  0,  0,  2),
	new AtomicElement( 90, "Th",  232.03800,  3,  4,  0,  0,  0,  3),
	new AtomicElement( 91, "Pa",  231.04000,  3,  4,  5,  0,  0,  4),
	new AtomicElement( 92, "U",   238.03000,  3,  4,  5,  6,  0,  5),
	new AtomicElement( 93, "Np",  237.05000,  3,  4,  5,  6,  0,  6),
	new AtomicElement( 94, "Pu",  244.00000,  3,  4,  5,  6,  0,  7),
	new AtomicElement( 95, "Am",  243.00000,  3,  4,  5,  6,  0,  8),
	new AtomicElement( 96, "Cm",  247.00000,  3,  0,  0,  0,  0,  9),
	new AtomicElement( 97, "Bk",  247.00000,  3,  4,  0,  0,  0, 10),
	new AtomicElement( 98, "Cf",  251.00000,  3,  0,  0,  0,  0, 11),
	new AtomicElement( 99, "Es",  254.00000,  3,  0,  0,  0,  0, 12),
	new AtomicElement(100, "Fm",  257.00000,  3,  0,  0,  0,  0, 13),
	new AtomicElement(101, "Md",  258.00000,  3,  0,  0,  0,  0, 14),
	new AtomicElement(102, "No",  259.00000,  1,  0,  0,  0,  0, 15),
	new AtomicElement(103, "Lr",  260.00000,  1,  0,  0,  0,  0, 16),
	new AtomicElement(104, "D",     2.01400,  1,  0,  0,  0,  7,  0),
	new AtomicElement(105, "T",     3.01605,  1,  0,  0,  0,  7,  1),
	new AtomicElement(106, "R",     0.00000,  0,  0,  0,  0,  6,  0),
	new AtomicElement(107, "X",     0.00000,  0,  0,  0,  0,  6,  1),
	new AtomicElement(108, "Gly",  57.04765,  2,  0,  0,  0,  5,  0),
	new AtomicElement(109, "Ala",  71.07474,  2,  0,  0,  0,  5,  1),
	new AtomicElement(110, "Val",  99.12892,  2,  0,  0,  0,  5,  2),
	new AtomicElement(111, "Leu", 113.15601,  2,  0,  0,  0,  5,  3),
	new AtomicElement(112, "Ile", 113.15601,  2,  0,  0,  0,  5,  4),
	new AtomicElement(113, "Ser",  87.07414,  2,  0,  0,  0,  5,  5),
	new AtomicElement(114, "Thr", 101.10123,  2,  0,  0,  0,  5,  6),
	new AtomicElement(115, "Asp", 115.08469,  2,  0,  0,  0,  5,  7),
	new AtomicElement(116, "Asn", 114.09996,  2,  0,  0,  0,  5,  8),
	new AtomicElement(117, "Glu", 129.11178,  2,  0,  0,  0,  5,  9),
	new AtomicElement(118, "Gln", 128.12705,  2,  0,  0,  0,  5, 10),
	new AtomicElement(119, "Lys", 128.17068,  2,  0,  0,  0,  5, 11),
	new AtomicElement(120, "Hyl", 144.17008,  2,  0,  0,  0,  4,  0),
	new AtomicElement(121, "His", 137.13753,  2,  0,  0,  0,  4,  1),
	new AtomicElement(122, "Arg", 156.18408,  2,  0,  0,  0,  4,  2),
	new AtomicElement(123, "Phe", 147.17352,  2,  0,  0,  0,  4,  3),
	new AtomicElement(124, "Tyr", 163.17292,  2,  0,  0,  0,  4,  4),
	new AtomicElement(125, "Trp", 186.21049,  2,  0,  0,  0,  4,  5),
	new AtomicElement(126, "Thy", 758.85682,  2,  0,  0,  0,  4,  6),
	new AtomicElement(127, "Cys", 103.13874,  2,  0,  0,  0,  4,  7),
	new AtomicElement(128, "Cst", 222.28154,  2,  0,  0,  0,  4,  8),
	new AtomicElement(129, "Met", 131.19292,  2,  0,  0,  0,  4,  9),
	new AtomicElement(130, "Pro",  97.11298,  2,  0,  0,  0,  4, 10),
	new AtomicElement(131, "Hyp", 113.11238,  2,  0,  0,  0,  4, 11),
	new AtomicElement(132, "H+",    1.00797, 15,  0,  0,  0,  8,  0),
	new AtomicElement(133, "H2",    2.01594, 15,  0,  0,  0,  8,  1),
    };

    /* standard element name/element no. constants */
    public static final int UNKNOWN       = 0;
    public static final int HYDROGEN      = 1;
    public static final int HELIUM        = 2;
    public static final int LITHIUM       = 3;
    public static final int BERYLLIUM     = 4;
    public static final int BORON         = 5;
    public static final int CARBON        = 6;
    public static final int NITROGEN      = 7;
    public static final int OXYGEN        = 8;
    public static final int FLUORINE      = 9;
    public static final int NEON          = 10;
    public static final int SODIUM        = 11;
    public static final int MAGNESIUM     = 12;
    public static final int ALUMINUM      = 13;
    public static final int SILICON       = 14;
    public static final int PHOSPHORUS    = 15;
    public static final int SULPHUR       = 16;
    public static final int CHLORINE      = 17;
    public static final int ARGON         = 18;
    public static final int POTASSIUM     = 19;
    public static final int CALCIUM       = 20;
    public static final int SCANDIUM      = 21;
    public static final int TITANIUM      = 22;
    public static final int VANADIUM      = 23;
    public static final int CHROMIUM      = 24;
    public static final int MANGANESE     = 25;
    public static final int IRON          = 26;
    public static final int COBALT        = 27;
    public static final int NICKEL        = 28;
    public static final int COPPER        = 29;
    public static final int ZINC          = 30;
    public static final int GALLIUM       = 31;
    public static final int GERMANIUM     = 32;
    public static final int ARSENIC       = 33;
    public static final int SELENIUM      = 34;
    public static final int BROMINE       = 35;
    public static final int KRYPTON       = 36;
    public static final int RUBIDIUM      = 37;
    public static final int STRONTIUM     = 38;
    public static final int YTTRIUM       = 39;
    public static final int ZIRCONIUM     = 40;
    public static final int NIOBIUM       = 41;
    public static final int MOLYBDENUM    = 42;
    public static final int TECHNETIUM    = 43;
    public static final int RUTHENIUM     = 44;
    public static final int RHODIUM       = 45;
    public static final int PALLADIUM     = 46;
    public static final int SILVER        = 47;
    public static final int CADMIUM       = 48;
    public static final int INDIUM        = 49;
    public static final int TIN           = 50;
    public static final int ANTIMONY      = 51;
    public static final int TELLURIUM     = 52;
    public static final int IODINE        = 53;
    public static final int XENON         = 54;
    public static final int CESIUM        = 55;
    public static final int BARIUM        = 56;
    public static final int LANTHANUM     = 57;
    public static final int CERIUM        = 58;
    public static final int PRASEODYMIUM  = 59;
    public static final int NEODYMIUM     = 60;
    public static final int PROMETHIUM    = 61;
    public static final int SAMARIUM      = 62;
    public static final int EUROPIUM      = 63;
    public static final int GADOLINIUM    = 64;
    public static final int TERBIUM       = 65;
    public static final int DYSPROSIUM    = 66;
    public static final int HOLMIUM       = 67;
    public static final int ERBIUM        = 68;
    public static final int THULIUM       = 69;
    public static final int YTTERBIUM     = 70;
    public static final int LUTETIUM      = 71;
    public static final int HAFNIUM       = 72;
    public static final int TANTALUM      = 73;
    public static final int WOLFRAM       = 74;
    public static final int RHENIUM       = 75;
    public static final int OSMIUM        = 76;
    public static final int IRIDIUM       = 77;
    public static final int PLATINUM      = 78;
    public static final int GOLD          = 79;
    public static final int MERCURY       = 80;
    public static final int THALLIUM      = 81;
    public static final int LEAD          = 82;
    public static final int BISMUTH       = 83;
    public static final int POLONIUM      = 84;
    public static final int ASTATINE      = 85;
    public static final int RADON         = 86;
    public static final int FRANCIUM      = 87;
    public static final int RADIUM        = 88;
    public static final int ACTINIUM      = 89;
    public static final int THORIUM       = 90;
    public static final int PROTACTINIUM  = 91;
    public static final int URANIUM       = 92;
    public static final int NEPTUNIUM     = 93;
    public static final int PLUTONIUM     = 94;
    public static final int AMERICIUM     = 95;
    public static final int CURIUM        = 96;
    public static final int BERKELIUM     = 97;
    public static final int CALIFORNIUM   = 98;
    public static final int EINSTEINIUM   = 99;
    public static final int FERMIUM       = 100;
    public static final int MENDELEVIUM   = 101;
    public static final int NOBELIUM      = 102;
    public static final int LAWRENCIUM    = 103;
    public static final int UNQ           = 104;
    public static final int UNP           = 105;

    /**
     * Symbols used for displaying valence states of atoms.
     */
    public static final String valenceSymbols[] = {
	"(O)",  "(I)",   "(II)",   "(III)",  "(IV)", // 0-4
	"(V)",  "(VI)",  "(VII)",  "(VIII)", "(IX)", // 5-9
	"(X)",  "(XI)",  "(XII)",  "(XIII)", "(XIV)",	// 10-14
    };

    /**
     * Symbols used for displaying radical states of atoms.
     * For radical state 2, we should probably use unicode Middle dot
     * "\u00B7"
     */
    public static final String radicalSymbols[] = {
	// 0   1    2    3
	"", ":", "." /*"\u00B7"*/, "^^",
    };

    /**
     * Symbols used for displaying attachment points.
     */
    public static final String attachmentLabels[] = {
	"!", "*", "*\"", "?"
    };


    /**
     * Find the atomic number (atom type) given a symbol.
     */
    public static int getElementFromSymbol( String symbol ) {
	int elementType = UNKNOWN;


	// ok, based on length of label let's look it up...

	if( symbol.length() == 1 ) {
	    char c1 = symbol.charAt(0);
	    switch(c1){
	    case 'C': return CARBON;
	    case 'N': return NITROGEN;
	    case 'O': return OXYGEN;
	    case 'P': return PHOSPHORUS;
	    case 'S': return SULPHUR;
	    case 'H': return HYDROGEN;
	    }
	}

	// still have not figured out type, so we must
	//			search through the table...

	for(int pass = 0; pass < 2; pass++){
	    for (int i = 1; i < elements.length; i++) {
		if(elements[i].symbol.equals(symbol)){
		    return elements[i].atomicNumber;
		}
	    }
	    
	    // some cofactors have A as the first letter of the name.
	    // this really confuses astexviewer so remove the leading a.
	    if(symbol.startsWith("A")){
		symbol = " " + symbol.substring(1);
	    }
	}

	return UNKNOWN;
    }

    /** Get element from characters from pdb file. */
    public static int getElementFromSymbol(char e0, char e1){
	int elementType = UNKNOWN;


	// ok, based on length of label let's look it up...

	if(e0 == ' '){
	    switch(e1){
	    case 'C': return CARBON;
	    case 'N': return NITROGEN;
	    case 'O': return OXYGEN;
	    case 'P': return PHOSPHORUS;
	    case 'S': return SULPHUR;
	    case 'H': return HYDROGEN;
	    }
	}

	String symbol = null;
	char buf[]    = null;

	if(e0 == ' '){
	    buf = new char[1];
	    buf[0] = e1;
	}else{
	    buf = new char[2];
	    buf[0] = e0;
	    buf[1] = Character.toLowerCase(e1);
	}
	
	symbol = new String(buf);

	return getElementFromSymbol(symbol);
    }

    /** returns the symbol for the specified atom type */
    public static String getAtomSymbolFromElement(int num ) {
	String sym = null;

	switch (num) {
	case UNKNOWN:
	    sym = "?";
	    break;
	default:
	    if (num > 0 && num < elements.length) {
		sym = elements[num].symbol;
	    } else {
		sym = "";
	    }
	    break;
	}
	return sym;
    }

    /** Decode element string to atomic number. */
    public static int getElement(char c0, char c1) {
        switch(c0){
        case 'A':
            switch(c1){
            case 'c': return  89;
            case 'g': return  47;
            case 'l': return  13;
            case 'm': return  95;
            case 'r': return  18;
            case 's': return  33;
            case 't': return  85;
            case 'u': return  79;
            default:  return   0;
            }
        case 'B':
            switch(c1){
            case ' ':
            case 0:   return   5;
            case 'a': return  56;
            case 'e': return   4;
            case 'i': return  83;
            case 'k': return  97;
            case 'r': return  35;
            default:  return   0;
            }
        case 'C':
            switch(c1){
            case ' ':
            case 0:   return   6;
            case 'a': return  20;
            case 'd': return  48;
            case 'e': return  58;
            case 'f': return  98;
            case 'l': return  17;
            case 'm': return  96;
            case 'o': return  27;
            case 'r': return  24;
            case 's': return  55;
            case 'u': return  29;
            default:  return   0;
            }
        case 'D':
            switch(c1){
            case 'y': return  66;
            default:  return   0;
            }
        case 'E':
            switch(c1){
            case 'r': return  68;
            case 's': return  99;
            case 'u': return  63;
            default:  return   0;
            }
        case 'F':
            switch(c1){
            case ' ':
            case 0:   return   9;
            case 'e': return  26;
            case 'm': return 100;
            case 'r': return  87;
            default:  return   0;
            }
        case 'G':
            switch(c1){
            case 'a': return  31;
            case 'd': return  64;
            case 'e': return  32;
            default:  return   0;
            }
        case 'H':
            switch(c1){
            case ' ':
            case 0:   return   1;
            case 'e': return   2;
            case 'f': return  72;
            case 'g': return  80;
            case 'o': return  67;
            default:  return   0;
            }
        case 'I':
            switch(c1){
            case ' ':
            case 0:   return  53;
            case 'n': return  49;
            case 'r': return  77;
            default:  return   0;
            }
        case 'K':
            switch(c1){
            case ' ':
            case 0:   return  19;
            case 'r': return  36;
            default:  return   0;
            }
        case 'L':
            switch(c1){
            case 'a': return  57;
            case 'i': return   3;
            case 'r': return 103;
            case 'u': return  71;
            default:  return   0;
            }
        case 'M':
            switch(c1){
            case 'd': return 101;
            case 'g': return  12;
            case 'n': return  25;
            case 'o': return  42;
            default:  return   0;
            }
        case 'N':
            switch(c1){
            case ' ':
            case 0:   return   7;
            case 'a': return  11;
            case 'b': return  41;
            case 'd': return  60;
            case 'e': return  10;
            case 'i': return  28;
            case 'o': return 102;
            case 'p': return  93;
            default:  return   0;
            }
        case 'O':
            switch(c1){
            case ' ':
            case 0:   return   8;
            case 's': return  76;
            default:  return   0;
            }
        case 'P':
            switch(c1){
            case ' ':
            case 0:   return  15;
            case 'a': return  91;
            case 'b': return  82;
            case 'd': return  46;
            case 'm': return  61;
            case 'o': return  84;
            case 'r': return  59;
            case 't': return  78;
            case 'u': return  94;
            default:  return   0;
            }
        case 'R':
            switch(c1){
            case 'a': return  88;
            case 'b': return  37;
            case 'e': return  75;
            case 'h': return  45;
            case 'n': return  86;
            case 'u': return  44;
            default:  return   0;
            }
        case 'S':
            switch(c1){
            case ' ':
            case 0:   return  16;
            case 'b': return  51;
            case 'c': return  21;
            case 'e': return  34;
            case 'i': return  14;
            case 'm': return  62;
            case 'n': return  50;
            case 'r': return  38;
            default:  return   0;
            }
        case 'T':
            switch(c1){
            case 'a': return  73;
            case 'b': return  65;
            case 'c': return  43;
            case 'e': return  52;
            case 'h': return  90;
            case 'i': return  22;
            case 'l': return  81;
            case 'm': return  69;
            default:  return   0;
            }
        case 'U':
            switch(c1){
            case ' ':
            case 0:   return  92;
            default:  return   0;
            }
        case 'V':
            switch(c1){
            case ' ':
            case 0:   return  23;
            default:  return   0;
            }
        case 'W':
            switch(c1){
            case ' ':
            case 0:   return  74;
            default:  return   0;
            }
        case 'X':
            switch(c1){
            case 'e': return  54;
            default:  return   0;
            }
        case 'Y':
            switch(c1){
            case ' ':
            case 0:   return  39;
            case 'b': return  70;
            default:  return   0;
            }
        case 'Z':
            switch(c1){
            case 'n': return  30;
            case 'r': return  40;
            default:  return   0;
            }
        }

        return 0;
    }

    public static String getSymbol(int el){
        return symbols[el];
    }

    /** Element symbols ordered by atomic number. */
    private static String symbols[] = {
        "*",  "H",  "He", "Li", "Be", "B",  "C",  "N",  "O",  "F",
        "Ne", "Na", "Mg", "Al", "Si", "P",  "S",  "Cl", "Ar", "K",
        "Ca", "Sc", "Ti", "V",  "Cr", "Mn", "Fe", "Co", "Ni", "Cu",
        "Zn", "Ga", "Ge", "As", "Se", "Br", "Kr", "Rb", "Sr", "Y",
        "Zr", "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", "Ag", "Cd", "In",
        "Sn", "Sb", "Te", "I",  "Xe", "Cs", "Ba", "La", "Ce", "Pr",
        "Nd", "Pm", "Sm", "Eu", "Gd", "Tb", "Dy", "Ho", "Er", "Tm",
        "Yb", "Lu", "Hf", "Ta", "W",  "Re", "Os", "Ir", "Pt", "Au",
        "Hg", "Tl", "Pb", "Bi", "Po", "At", "Rn", "Fr", "Ra", "Ac",
        "Th", "Pa", "U",  "Np", "Pu", "Am", "Cm", "Bk", "Cf", "Es",
        "Fm", "Md", "No", "Lr",
    };


}
