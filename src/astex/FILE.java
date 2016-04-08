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

/* Copyright Astex Technology Ltd. 1999 */

/*
 * 08-08-03 mjh
 *	add in the sprint functionality for formatting and returning
 *	numbers internally as a string.
 * 14-03-03 mjh
 *	add rudimentary code to handle scientific notation in
 *	floating point input numbers.
 * 14-02-03 pw
 *	merge Format class in for high performance output formatting.
 * 02-08-02 mjh
 *	look for .gz versions of files first, to work around
 *	the shortcomings of microsoft web servers that cannot
 *	handle two file extensions correctly such as .pdb.gz
 * 13-08-02 mjh
 *	add internal field splitting to avoid overhead of String
 *	creation with the old method. To get the fields, you had
 *	to get the current line as a string and then tokenise that
 *	to give you an array of strings. The current method should
 *	be much faster.
 * 24-01-99 mjh
 *	fix serious bug in fill() that caused infinite loop with Mac
 *	style line endings (\r).
 * 23-12-99 mjh
 *	add method to load a properties file
 * 22-12-99 mjh
 *	FILE.open() will now try and load from a jar file if one is
 *	present. Netscape is ridiculously fussy about what you can
 *	load from a jar file. lib, dat and many other extensions are
 *	expressly forbidden when running as an applet. properties is just
 *	about the only one that works.
 * 30-11-99 mjh
 *	make FILE.open() determine if the resource is a file or URL
 *	and open it appropriately. make open(URL) use URLConnection's
 *	so that it can take advantage of cached files.
 * 11-11-99 mjh
 *	work around fact that java bytes are signed, but ascii isn't
 *	i.e. read() returns b & 255. Didn't cause problems with regular
 *	text files but hoses binary files e.g. maps. No particular impact
 *	on performance
 * 29-10-99 mjh
 *	removed code for original implementation of unget buffer
 * 28-10-99 mjh
 *	created
 */

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.net.*;
import java.awt.*;
import java.applet.*;

/**
 * A class which implements a high performance byte based input
 * stream.
 * 
 * Buffering is performed internally. This class is NOT thread safe.
 */
public class FILE extends InputStream {

    /**
     * Input and output member variables.
     */

    /**  The size of the input buffer we will use. */
    private static final int bufferSize = 2048;

    /** The buffer into which we will read the input. */
    private byte buffer[] = new byte[bufferSize + 1];

    /** The number of characters currently in the buffer. */
    private int charactersInBuffer = 1;

    /** 
     * The last exception that was thrown by 
     * a FILE.open() call or FILE.write(). 
     */
    private static Exception exception = null;

    /**
     * Input specific variable.
     */
    /** The InputStream that this object will read from. */
    private InputStream inputStream = null;
 
    /** The next character from the buffer that we will return. */
    private int nextCharacter = 1;

    /** The constant that represents the end of file. */
    public static int EOF = -1;
    
    /** Have fields been determined for the current line. */
    private boolean fieldsDetermined = false;

    /** The number of fields on the current line. */
    private int fieldCount = 0;

    /** The maximum number of fields we can have. */
    private static int MaxFields = 1024;

    /** The start position of each field on the current line. */
    private int fieldStartPosition[] = new int[MaxFields];

    /** The stop position of each field on the current line. */
    private int fieldLength[] = new int[MaxFields];

    /** The document base if there is one. */
    private static URL documentBase = null;

    /** The code base if there is one. */
    private static URL codeBase = null;

    /** Should we output debug information. */
    private static boolean debug = false;

    /** Should we try and open resources from files. */
    private static boolean tryFiles = true;
    
    /** The length of the buffer used for storing a line of input. */
    private static int lineBufferSize = 1024;

    /** A buffer for storing the current line in the input. */
    private byte lineBuffer[] = new byte[lineBufferSize];

    /** The length of the current line. */
    private int lineLength = 0;

    /**
     * Output specific variables.
     */
    
    /** Wrapper allowing write to stdout and stderr. */
    public static FILE out = new FILE(System.out);
    public static FILE err = new FILE(System.err);
    public static FILE in  = new FILE(System.in);
    public static FILE spr = new FILE();
 
    /** Output stuff. */
    private OutputStream outputStream = null;

    /** eol identifier. */
    private int  eolIdentifier = UNIX;
    private byte eol0 = (byte)'\n';
    private byte eol1 = 0;
    private int  eolLength = 1;

    public static final int UNIX = 0;
    public static final int PC   = 1;
    public static final int MAC  = 2;

    /* Variables for formatting. */
    private int width = 0;
    private int precision = -1;
    private byte pre[]  = new byte[bufferSize];
    private byte post[] = new byte[bufferSize];
    private int  preCount  = 0;
    private int  postCount = 0;
    private boolean leading_zeroes = false;
    private boolean show_plus = false;
    private boolean alternate = false;
    private boolean show_space = false;
    private boolean left_align = false;
    private char fmt = ' '; 

    /** Temporary byte buffer. */
    private byte tb[] = new byte[2048];
    
    /** Number of bytes in temporary byte buffer. */
    private int ntb      = 0;
    private int ntbStart = 0;
    private int ntbE     = 0;

    /** 
     * autoflush variable. If output is stderr 
     * or stdout is set true else false. 
     */
    private boolean autoFlush = false;

    /** Arrays of bytes for different ouput types. */
    private static final byte baseo[] = {(byte)'0', (byte)'1', (byte)'2', (byte)'3', 
					 (byte)'4', (byte)'5', (byte)'6', (byte)'7'};

    private static final byte basex[] = {(byte)'0', (byte)'1', (byte)'2', (byte)'3', 
					 (byte)'4', (byte)'5', (byte)'6', (byte)'7', 
					 (byte)'8', (byte)'9', (byte)'a', (byte)'b',
					 (byte)'c', (byte)'d', (byte)'e', (byte)'f'};

    private static final byte baseX[] = {(byte)'0', (byte)'1', (byte)'2', (byte)'3', 
					 (byte)'4', (byte)'5', (byte)'6', (byte)'7', 
					 (byte)'8', (byte)'9', (byte)'A', (byte)'B',
					 (byte)'C', (byte)'D', (byte)'E', (byte)'F'};
    
    private static final byte based[] = {(byte)'0', (byte)'1', (byte)'2', (byte)'3', 
					 (byte)'4', (byte)'5', (byte)'6', (byte)'7',
					 (byte)'8', (byte)'9'};

    private static final byte baseb[]  = {(byte)'0', (byte)'1'};
   
    /** Load of constants for character bytes. */
    private static final byte bSpace = (byte)' ';
    private static final byte bPlus  = (byte)'+';
    private static final byte bMinus = (byte)'-';
    private static final byte b0     = (byte)'0';
    private static final byte bx     = (byte)'x';
    private static final byte bX     = (byte)'X';
    private static final byte bDot   = (byte)'.';
    private static final byte be     = (byte)'e';
    private static final byte bE     = (byte)'E';
    
    /** Stores the previous format. */
    private String prevFormat = null;

    /** sets the autoFlush flag. */
    public void setautoFlush(boolean f){
	autoFlush = f;
    }

    /** Return a formatted double as String. */
    public static String sprint(String f, double d){

        String s;

        spr.parseFormat(f); 
        spr.format(d);
        s = spr.createString(); 

        return s;
    }

    /** Return a formatted long as String. */
    public static String sprint(String f, long d){

        String s;

        spr.parseFormat(f); 
        spr.format(d);
        s = spr.createString();
        
        return s;
    }

    /** Return a formatted String as String. */
    public static String sprint(String f, String d){

        String s;

        spr.parseFormat(f); 
        spr.format(d);
        s = spr.createString();
        
        return s;
    }

    /** Copies all data to the main buffer. */
    private String createString(){
        byte localBuffer[] = new byte[bufferSize];
        byte localtb[] = tb;

        int nc = 0;
        int n = preCount + postCount + width + (ntb - ntbStart - ntbE); 
        String s = null;

        // Do preformatting.
        for(int i = 0; i < preCount; i++){
            localBuffer[nc++] = pre[i];
        }

        if(!left_align){
            // Pad first.
            nc = repeat(bSpace, width - (ntb - ntbStart) - ntbE, 
                        localBuffer, nc);

            // Copy the number.
            for(int i = ntb-1; i >= ntbStart; i--){
                localBuffer[nc++] = localtb[i];
            }
            
            for(int i = ntbE-1; i >= 0; i--){
                localBuffer[nc++] = localtb[i];
            } 

        } else {
            // Copy the number.
            for(int i = ntb-1; i >= ntbStart; i--){
                localBuffer[nc++] = localtb[i];
            }
            
            for(int i = ntbE-1; i >= 0; i--){
                localBuffer[nc++] = localtb[i];
            } 
            
            // Pad second.
            nc = repeat(bSpace, width - (ntb - ntbStart) - ntbE, 
                        localBuffer, nc);
            
        }

        // Post-format.
        for(int i = 0; i < postCount; i++){
            localBuffer[nc++] = post[i];
        }
       
        return new String(localBuffer, 0, 0, nc);
    }   

    /** Print a char. */
    public void print(char c){
	flushIfFull(1);

	buffer[charactersInBuffer++] = (byte)c;
    }

    /** Quick print function for floats/doubles. */
    public void print(int w, int p, double d){
	
	width = w;
	precision = p;
	leading_zeroes = false;
	show_plus = false;
	alternate = false;
	show_space = false;
	left_align = false;
	fmt = 'f'; 
	preCount = 0; 
	postCount = 0;

	format(d);
	copyAll();	
    }
    
    /** Quick print function for ints/long */
    public void print(int w, long d){

	width = w;
	precision = -1;
	leading_zeroes = false;
	show_plus = false;
	alternate = false;
	show_space = false;
	left_align = false;
	fmt = 'd'; 
	preCount = 0;
	postCount = 0;

	format(d);
	copyAll();	
    }
    
    /** Print function for 3 floats/doubles. */
    public void print(String f, double d1, double d2, double d3){
   
	/* Parse the format string. */
	if(prevFormat != f){
	    parseFormat(f);
	    f.intern();
	    prevFormat = f;
	}
	
	format(d1);
	copyAll();

	format(d2);
	copyAll();

	format(d3);
	copyAll();
    }

    /** Print function for 3 ints/longs. */
    public void print(String f, long d1, long d2, long d3){
   	
	/* Parse the format string. */
	if(prevFormat != f){
	    parseFormat(f);
	    f.intern();
	    prevFormat = f;
	}
	
	format(d1);
	copyAll();

	format(d2);
	copyAll();

	format(d3);
	copyAll();
    }

    /** print function. No formatting. */
    public void print(String s){

	int len = s.length();
	byte b  = 0; 
	
	flushIfFull(len);
	for(int i = 0; i < len; i++){
	    b = (byte)s.charAt(i);
	    buffer[charactersInBuffer++] = b;
	}
	if(autoFlush) flush();
    }

    /** println function. No formatting adds eol. */
    public void println(String s){
	
	int len = s.length();
	byte b  = 0; 

	flushIfFull(len + eolLength);
	for(int i = 0; i < len; i++){
	    b = (byte)s.charAt(i);
	    buffer[charactersInBuffer++] = b;
	}
	eol();
	if(autoFlush) flush();
    }

    /** Print a formatted String. */
    public void print(String f, String s){
	
	if(prevFormat != f){
	    parseFormat(f);
	    f.intern();
	    prevFormat = f;
	}
	format(s);
	if(autoFlush) flush();
    }

    /** Print a formatted char. */
    public void print(String f, char c){
	
	if(prevFormat != f){
	    parseFormat(f);
	    f.intern();
	    prevFormat = f;
	}
	format(c);
	if(autoFlush) flush();
    }

    /** Print a formatted integer. */
    public void print(String f, long d){
	
	if(prevFormat != f){
	    parseFormat(f);
	    f.intern();
	    prevFormat = f;
	}	
	format(d);
	copyAll();	
    }
       
    /** Print a formatted double. */
    public void print(String f, double d){
	
	if(prevFormat != f){
	    parseFormat(f);
	    f.intern();
	    prevFormat = f;
	}

	format(d);
	copyAll();	
    }
   
    /** Format the String argument. */
    public void format(String s){
	
	if (fmt != 's')
	    throw new java.lang.IllegalArgumentException();

	ntb = 0;
	ntbStart = 0;

	/* Here we can copy directly in the main buffer. */
	ntbE = s.length();

        //System.out.println("format preCount  " + preCount);
        //System.out.println("format postCount " + postCount);
        //System.out.println("format width     " + width);
        //System.out.println("format ntbE      " + ntbE);
 
	//int n   = preCount + postCount + width - ntbE;
	int n   = preCount + postCount + width + ntbE;
	
	flushIfFull(n);
		
	preFormat();
	if(!left_align){
	    pad();
	    copyString(s, ntbE);
	} else {
	    copyString(s, ntbE);
	    pad();
	}
	postFormat();
    }

    /** Format integer. */
    public void format(long d){

	int s = 0; /* sign of the number. */

	/* Reinitialise counters. */
	ntb = 0;
	ntbStart = 0; 
	ntbE = 0;
	
	/* If format is d or i set sign value. */
	if(fmt == 'd' || fmt == 'i'){
	    if(d < 0) s = -1; 
	    else s = 1;
	}
       
	if(fmt == 'd' || fmt == 'i')
	    convert(d, based, 10);	
	else if (fmt == 'o')
	    convert(d, baseo, 8);
	else if (fmt == 'x')
	    convert(d, basex, 16);
	else if (fmt == 'X')
	    convert(d, baseX, 16);
	else throw new java.lang.IllegalArgumentException();
	
	/* Add the sign character bytes. */
	sign(s);
    }

    /** Format double. */
    public void format(double x) {

	byte localtb[] = tb;

	/* Reinitialise counters. */
	ntb = 0;
	ntbStart = 0;
	ntbE = 0;

	int s = 1; /* Set sign to positive. */

	/* Handle + and - Infinity. */
	if(Double.isInfinite(x)){
	    if(x == Double.NEGATIVE_INFINITY) s = -1;
	    else s = 1;
	    
	    localtb[ntb++] = (byte)'f';
	    localtb[ntb++] = (byte)'n';
	    localtb[ntb++] = (byte)'I';
	    ntbStart = ntbE;
	    sign(s);
	    return;
	}
	
	/* Handle Nan. */
	if(Double.isNaN(x)){
	    localtb[ntb++] = (byte)'N';
	    localtb[ntb++] = (byte)'a';
	    localtb[ntb++] = (byte)'N';
	    ntbStart = ntbE;
	    return;
	}

	/* Set default precision and sign. */
	if(precision < 0) precision = 6;
	if(x < 0){ x = -x; s = -1; };

	if(fmt == 'f'){
	    fixedFormat(x);
	} else if(fmt == 'e' || fmt == 'E' || fmt == 'g' || fmt == 'G'){
	    expFormat(x);
	} else throw new java.lang.IllegalArgumentException();
	
	
	/* Need this here if we round to infinity. */
	if(Double.isInfinite(x)){
	    localtb[ntb++] = (byte)'f';
	    localtb[ntb++] = (byte)'n';
	    localtb[ntb++] = (byte)'I';
	    ntbStart = ntbE;
	    sign(s);
	    return;
	} else {
	    sign(s); /* Sort out the sign. */
	}
    }

    /** Format the char argument. */
    public void format(char c){
	
	byte localBuffer[] = buffer;

	if (fmt != 'c')
	    throw new java.lang.IllegalArgumentException();

	/* Initialise counters. */
	ntb = 0;
	ntbStart = 0;

	/* Here we can copy directly in the main buffer. */
	ntbE = 1; 
	int n = preCount + postCount + width - ntbE;
	
	flushIfFull(n);
		
	preFormat();
	if(!left_align){
	    pad();
	    buffer[charactersInBuffer++] = (byte)c;
	} else {
	    buffer[charactersInBuffer++] = (byte)c;
	    pad();
	}
	postFormat();
    }

    /** Copies all data to the main buffer. */
    private void copyAll(){
	
	int n = preCount + postCount + width + (ntb - ntbStart - ntbE);	

	/* Check limits of buffer if full, then flush. */
	flushIfFull(n);

	preFormat();
	if(!left_align){
	    pad();
	    copyNumber();
	} else {
	    copyNumber();
	    pad();
	}
	postFormat();
	
	/* If autoFlush is true. Flush the buffer. */
	if(autoFlush) flush();
    }	

    /** Copies the number from the tb temporary array into
       the main buffer. Need to do this backwards. */
    private void copyNumber(){

	byte localBuffer[] = buffer;
	byte localtb[] = tb;

	for(int i = ntb-1; i >= ntbStart; i--){
	    localBuffer[charactersInBuffer++] = localtb[i];
	}

	for(int i = ntbE-1; i >= 0; i--){
	    localBuffer[charactersInBuffer++] = localtb[i];
	} 

    }

    /** Copy preformat segment into main buffer. */
    private void preFormat(){
	byte localBuffer[] = buffer;

	for(int i = 0; i < preCount; i++){
	    localBuffer[charactersInBuffer++] = pre[i];
	}
    }
    
    /** Copy postformat segment into main buffer. */
    private void postFormat(){
	byte localBuffer[] = buffer;
	
	for(int i = 0; i < postCount; i++){
	    localBuffer[charactersInBuffer++] = post[i];
	}
    }

    /** Create padded segment in main buffer. */
    private void pad(){
	charactersInBuffer = repeat(bSpace, width - (ntb - ntbStart) - ntbE, 
				    buffer, charactersInBuffer);
    }

    /** Copies repeated characters to supplied buffer. */
    private int repeat(byte b, int n, byte bArray[], int nb){
	if(n <= 0) return nb;
	
	for (int i = 0; i < n; i++){
	    bArray[nb++] = b;
	}
	return nb;
    }

    /** Copies the content of a String directly
	to the main buffer. */
    private void copyString(String s, int len){
	for(int i = 0; i < len; i++){
	    buffer[charactersInBuffer++] = (byte)s.charAt(i);
	}
    }

    /** Outputs fixed format numbers. */
    private void fixedFormat(double d){
	
	byte localtb[] = tb; 

	/* Sort out whether to remove trailing balnks. */
	boolean removeTrailing = (fmt == 'G' || fmt == 'g') && !alternate;
	
	/* Remove trailing zeroes and decimal point. */
	if(d > 0x7FFFFFFFFFFFFFFFL){ expFormat(d); return; }
	
	/* Handle precision zero stuff as sepecial case. */
	if(precision == 0){ 
	    if(!removeTrailing){
		localtb[ntb++] = bDot;
	    }
	    convert((long)(d + 0.5), based, 10);
	    ntbStart = 0;
	    ntbE     = 0;
	    return;
	}
	
	/* Get parts of the number. */
	long whole = (long)d;
	double fr  = d - whole;

	if(fr >= 1 || fr < 0){ expFormat(d); return; }
	
	double factor = 1;
	int    leading_zeroes = 0;
	
	for (int i = 1; i <= precision && factor <= 0x7FFFFFFFFFFFFFFFL; i++)  {
	    factor *= 10;
	    leading_zeroes++; 
	}
	
	long l = (long) (factor * fr + 0.5);
	if (l >= factor) { l = 0; whole++; }

	/* Convert and add the fractional part to tb. */
	convert(l, based, 10);

	/* Add any leading zeros */
	int realLeadingZeros = precision - ntb + ntbE;
	for(int i = 0; i < realLeadingZeros; i++){
	    localtb[ntb++] = b0;
	}
	
	/* Add the decimal point. */
	localtb[ntb++] = bDot;

	/* Reset nbStart and remove trailing zeros if set. */
	ntbStart = ntbE;
	if(removeTrailing){
	    while(ntbStart < ntb && localtb[ntbStart] == b0) ntbStart++;
	    if(ntbStart < ntb && localtb[ntbStart] == bDot) ntbStart++;   
	}

	/* Convert and add the whole part to tb. */
	convert(whole, based, 10);
    }
    
    /** Handle epxonential format numbers. */
    private void expFormat(double d){
	
	int e = 0;
	double dd = d;
	double factor = 1;
	
	byte localtb[] = tb;

	if(d != 0){
	    while(dd > 10){ e++; factor /= 10; dd = dd / 10; }
	    while(dd < 1){ e--; factor *= 10; dd = dd * 10; }
	}
	
	if((fmt == 'g' || fmt == 'G') && e >= -4 && e < precision){ 
	    fixedFormat(d); return;
	}

	/* Need to add the power bit first. */
	if(e >= 0){
	    convert(e, based, 10);
	    /* Add on required number of zeros. */
	    if(ntb < 3){
		for(int i = ntb; i < 3; i++){
		    localtb[ntb++] = b0;
		}
	    }
	    localtb[ntb++] = bPlus;
	}
	else{
	    convert(-e, based, 10);
	    /* Add on required number of zeros. */
	    if(ntb < 3){
		for(int i = ntb; i < 3; i++){
		    localtb[ntb++] = b0;
		}
	    }
	    localtb[ntb++] = bMinus;
	}
	
	/* Add the e, or E. */
	if(fmt == 'e' || fmt == 'g') localtb[ntb++] = be;
	else localtb[ntb++] = bE;
	
	/* Record the position we got. */
	ntbE = ntb;
	
	/* Now add the floating point bit. */
	d = d * factor;
	fixedFormat(d);
    }

    /** Converts a long to a stream of bytes for the provided base. 
	Note that it fills the temporary buffer in reverse. */
    private void convert(long x, byte b[], long base){

	long posx;
	byte localtb[] = tb;

	if(x == Long.MIN_VALUE){
	    localtb[ntb++] = b[8]; localtb[ntb++] = b[0]; localtb[ntb++] = b[8]; 
	    localtb[ntb++] = b[5]; localtb[ntb++] = b[7]; localtb[ntb++] = b[7];
	    localtb[ntb++] = b[4]; localtb[ntb++] = b[5]; localtb[ntb++] = b[8];
	    localtb[ntb++] = b[6]; localtb[ntb++] = b[3]; localtb[ntb++] = b[0];
	    localtb[ntb++] = b[2]; localtb[ntb++] = b[7]; localtb[ntb++] = b[3]; 
	    localtb[ntb++] = b[3]; localtb[ntb++] = b[2]; localtb[ntb++] = b[2];
	    localtb[ntb++] = b[9];
	    return;
	}

	if(x < 0) posx = -x;
	else posx = x;

	if(posx == 0){
	    localtb[ntb++] = b[0];
	    return;
	}

	int m;

	while (posx != 0) {
	    m = (int)(posx % base);
	    posx /= base;
	    localtb[ntb++] = b[m];
	}
    }
    
    /** Sorts the sign out and adds it to the temporary buffer. */
    private void sign(int s){
	
	int  signLength = 0;
	byte signByte1  = bSpace;
	byte signByte2  = bSpace;

	byte localtb[] = tb;

	if(s < 0){ signLength = 1; signByte1 = bMinus; }
	else if(s > 0){
	    if(show_plus){ signLength = 1; signByte1 = bPlus; }
	    else if(show_space){ signLength = 1; signByte1 = bSpace; }
	} else {
	    if(fmt == 'o' && alternate && ntb > 0 && localtb[ntb-1] != b0)
		{ signLength = 1; signByte1 = b0; }
	    else if (fmt == 'x' && alternate)
		{ signLength = 2; signByte2 = b0; signByte1 = bx; }
	    else if (fmt == 'X' && alternate)
		{ signLength = 2; signByte2 = b0; signByte1 = bX; }
	}
       

	/* Now sort out the precision and leading zeros. */
	int w = 0;
	if(leading_zeroes) w = width;
	else if((fmt == 'd' || fmt == 'i' || fmt == 'x' || fmt == 'X' || fmt == 'o') 
		&& precision > 0) w = precision;
	
	/* Add leading zeros. */
	ntb = repeat(b0, w - (ntb - ntbStart) - ntbE - signLength, localtb, ntb);
	
	/* Add the sign. */
	if(signLength > 0){
	    localtb[ntb++] = signByte1;
	    if(signLength > 1){
	    	localtb[ntb++] = signByte2;
	    }
	}
    }
    
    /** Parses the format string. */
    private void parseFormat(String s){

	width = 0;
	precision = -1;
	leading_zeroes = false;
	show_plus = false;
	alternate = false;
	show_space = false;
	left_align = false;
	fmt = ' '; 
	preCount = 0;
	postCount = 0;

	/* sets the format of the output. */
	int state = 0; 
	int length = s.length();
	int parse_state = 0; 
	
	/* 
	 * The parse states are listed here:
	 * 0 = prefix 
	 * 1 = flags
	 * 2 = width
	 * 3 = precision
	 * 4 = format
	 * 5 = end
	 */

	int i = 0;
	while(parse_state == 0){
	    char ci = s.charAt(i);
	    if(i >= length) parse_state = 5;
	    else if(ci == '%'){
		if(i < length - 1){
		    if(s.charAt(i + 1) == '%'){
			pre[preCount++] = (byte)'%';
			i++;
		    } else {
			parse_state = 1;
		    }
		} else {
		    throw new java.lang.IllegalArgumentException();
		}
	    } else {
		pre[preCount++] = (byte)ci;
	    }
	    i++;
	}
	while(parse_state == 1){
	    char ci = s.charAt(i);
	    if(i >= length) parse_state = 5;
	    else if(ci == ' ') show_space = true;
	    else if(ci == '-') left_align = true; 
	    else if(ci == '+') show_plus = true;
	    else if(ci == '0') leading_zeroes = true;
	    else if(ci == '#') alternate = true;
	    else { parse_state = 2; i--; }
	    i++;
	}      
	while(parse_state == 2){
	    char ci = s.charAt(i);
	    if(i >= length) parse_state = 5;
	    else if('0' <= ci && ci <= '9'){
		width = width * 10 + ci - '0';
		i++;
	    } else if(ci == '.'){
		parse_state = 3;
		precision = 0;
		i++;
	    } else 
		parse_state = 4;            
	}
	while(parse_state == 3){
	    char ci = s.charAt(i);
	    if(i >= length){
		parse_state = 5;
	    } else if('0' <= ci && ci <= '9'){
		precision = precision * 10 + ci - '0';
		i++;
	    } else { 
		parse_state = 4;                  
	    }
	}
	if(parse_state == 4){
	    if(i >= length) parse_state = 5;
	    else fmt = s.charAt(i);
	    i++;
	}
	if(i < length){
	    for(int j = i; j < length; j++){
		post[postCount++] = (byte)s.charAt(j);
	    }
	}
    }

    /** Sets the default end of line character. */
    private void setDefaultEolIdentifier(){

	String eolString = System.getProperty("line.separator");
	
	if(eolString.equals("\n")){
	    setEolIdentifier(UNIX);
	} else if (eolString.equals("\r\n")){
	    setEolIdentifier(PC);
	} else if (eolString.equals("\r")){
	    setEolIdentifier(MAC);
	} else {
	    setEolIdentifier(UNIX);
	}
    }

    /** Set the output stream. */
    public void setOutputStream(OutputStream os){
	outputStream = os;
    }

    /** Set the eond of line characters explicitily. */
    public void setEolIdentifier(int opsys){

	eolIdentifier = opsys;

	switch(opsys){
	case UNIX:	    
	    eolLength = 1;
	    eol0 = (byte)'\n';
	    break;
	case PC:
	    eolLength = 2;
	    eol0 = (byte)'\r';
	    eol1 = (byte)'\n';	    
	    break;
	case MAC:
	    eolLength = 1;
	    eol0 = (byte)'\r';
	    break;
	default:
	    eolLength = 1;
	    eolIdentifier = UNIX;
	    eol0 = (byte)'\n';
	    break;
	}
	
    }

    /**
     * Maintain the versions of the file we are about to for writing.
     */
    private static boolean shuffleVersions(String file){
	File f = new File(file);

	if(f.exists() == false){
	    return true;
	}else{
	    int version = 99;

	    boolean found = false;

	    do {
		String fileVersion = file + FILE.sprint("_%02d", version);
		File vf = new File(fileVersion);

		if(vf.exists()){
		    found = true;
		}else{
		    version--;
		}

	    } while(!found && version >= 0);

	    version++;

	    String fileVersion = file + FILE.sprint("_%02d", version);
	    //String fileVersion = prefix + FILE.sprint(".%02d", version) + suffix;

	    File newf = new File(fileVersion);

	    if(f.renameTo(newf) == false){
		return false;
	    }
	}
	
	return true;
    }

    /** Opens outputstream for writing. */
    public static FILE write(String file){
	// see if we could shuffle the files
	// if not we can't open the file
	if(shuffleVersions(file) == false){
	    return null;
	}

	try {
	    FileOutputStream fileOutputStream = new FileOutputStream(file);
	    FILE output = new FILE();
	    output.outputStream = fileOutputStream;
	    output.setCharactersInBuffer(0);
	    return output;
	} catch(Exception e){
	    setException(e);
	    return null;
	}
       
    }

    /** Adds end of line. */
    private void eol(){
	buffer[charactersInBuffer++] = eol0;
	if(eolLength == 2){
	    buffer[charactersInBuffer++] = eol1;
	}
	
    }

    /** Flush buffer if next lot of output is too long. */
    private void flushIfFull(int len){
	
	if(len + charactersInBuffer >= bufferSize){
	    flush();
	} 
    }

    /** Flush buffer. */
    private void flush(){
	
	if(charactersInBuffer > 0){
	    try {
		outputStream.write(buffer, 0, charactersInBuffer);
		charactersInBuffer = 0;
	    } catch(Exception e){
		System.out.println("Error flushing outputStream " + e);
	    }
	}
    }

    /** Constructor for stdout stderr output. */
    public FILE(OutputStream os){
	outputStream = os;
	setCharactersInBuffer(0);
	autoFlush = true;
    }
    
    /** Destructor just flushes the buffer. */
    protected void finalize(){
	if(outputStream != null){
	    flush();
	}
    }

    public static void setTryFiles(boolean t){
	tryFiles = t;
    }

    public static boolean getTryFiles(){
	return tryFiles;
    }

    /** Set the debug state. */
    public static void setDebug(boolean state){
	debug = state;
    }

    /** Set the docuemnt base. */
    public static void setDocumentBase(URL url){
	if(debug){
	    System.out.println("setDocumentBase to " + url);
	}
	documentBase = url;
    }

    /** Set the docuemnt base. */
    public static void setCodeBase(URL url){
	if(debug){
	    System.out.println("setCodeBase to " + url);
	}
	codeBase = url;
    }

    /** Set the exception. */
    private static void setException(Exception e){
	exception = e;
    }

    /**
     * Get the exception (if any) that was thrown when FILE.open()
     * was called.
     */
    public static Exception getException(){
	return exception;
    }
	
    /** Set the input stream. */
    private void setInputStream(InputStream is){
	inputStream = is;
    }

    /** Get the input stream associated with this file. */
    public InputStream getInputStream(){
	return inputStream;
    }

    public void setCharactersInBuffer(int i){
	charactersInBuffer = i;
    }

    /**
     * Make the default constructor private to ensure this class
     * cannot be instantiated directly.
     */
    private FILE(){
    }

    /**
     * Constructor that will take an InputStream and create a FILE
     * object that will read from it.
     */
    public FILE(InputStream is){
	setInputStream(is);
	setException(null);
    }

    /**
     * Fill the input buffer.
     */
    private void fill(){
	int charactersRead;

	// put the last character from the buffer into position 0,
	// so that unget will work when we unget the first character
	// after a buffer fill

	buffer[0] = buffer[charactersInBuffer - 1];

	try {
	    do {
		charactersRead = inputStream.read(buffer, 1, bufferSize);
	    } while (charactersRead == 0);
			
	    //System.out.println("fill() returned " + charactersRead +
	    //				   " characters");

	    if (charactersRead > 0) {
		charactersInBuffer = charactersRead + 1;
		nextCharacter = 1;
	    }
	}catch(Exception e){
	    setException(e);
	}
    }
	
    /**
     * Read a single character.
     */
    public int read(){
			
	if (nextCharacter >= charactersInBuffer) {
	    //System.out.print("before fill() nextCharacter " + nextCharacter);
	    //System.out.println(" charactersInBuffer " + charactersInBuffer);
			
	    fill();

	    //System.out.print("after fill() nextCharacter " + nextCharacter);
	    //System.out.println(" charactersInBuffer " + charactersInBuffer);

	    if (nextCharacter >= charactersInBuffer){
		//System.out.println("trying to return EOF");
		return EOF;
	    }
	}

	// convert to unsigned int
	return buffer[nextCharacter++] & 255; 
    }

    /** Skip some bytes. */
    public int skip(int byteCount){
	if(nextCharacter + byteCount < charactersInBuffer){
	    nextCharacter += byteCount;
	}else{
	    while(byteCount-- > 0){
		int b = read();
		if(b == EOF){
		    return EOF;
		}
	    }
	}

	return 0;
    }

    /**
     * unget a single character.
     */
    public void ungetc(int c){
	if(nextCharacter > 0){
	    nextCharacter--;
	}
    }


    /**
     * Return the length of the current line.
     */
    public int getLineLength(){
	return lineLength;
    }

    /**
     * Advance the FILE object to the next line.
     *
     * A return value of EOF indicates that there are no more lines.
     */
    public boolean nextLine(){
	fieldsDetermined = false;

	int ch = 0;

	lineLength = 0;
		
	do {
	    ch = read();

	    // all of the special cases are less than '\r'
	    // this saves us 3 comparisons per character
            // tab is less than \r (thank you paul mortenson)
	    if(ch <= '\r' && ch != '\t'){

		//if(ch == '\n'){
		//	System.out.println("\\n");
		//}else if(ch == '\r'){
		//	System.out.println("\\r");
		//}else if(ch == EOF){
		//	System.out.println("EOF");
		//}

		if( ch == '\n' ) {
		    return true;
		} else if( ch == '\r' ) {
		    ch = read();

		    //if(ch == '\n'){
		    //	System.out.println("After \\r \\n");
		    //}else if(ch == '\r'){
		    //	System.out.println("After \\r \\r");
		    //}else if(ch == EOF){
		    //	System.out.println("After \\r EOF");
		    //}


		    if( ch != '\n' && ch != EOF){
			ungetc(ch);
		    }else{
			//System.out.println("\\n after \\r");
		    }

		    return true;
		} else if( ch == EOF ) {
		    // if we see EOF and the line has some contents
		    // we need to return those contents and
		    // give the real EOF next time.

		    //System.out.println("EOF seen: lineLength " + lineLength);
		    return lineLength > 0;
		}
	    } else{
		lineBuffer[lineLength++] = (byte)ch;
	    }
	} while( lineLength < lineBufferSize);
		
	/* skip to the end of the line! */

	do {
	    ch = read();
	} while( (ch != '\n') && (ch != '\r') && (ch != EOF) );
		
	if( ch == '\r' ) {
	    ch = read();
	    if( ch != '\n' ){
		ungetc(ch);
	    }
	}

	return true;
    }

    /**
     * Provide a method that will return the current line
     * as a String. This has the same functionality as the BufferedReader
     * readLine() method. There is no need to use this at all.
     */
    public String readLine(){
	if(nextLine()){
	    return getCurrentLineAsString();
	}else{
	    return null;
	}
    }

    /**
     * Return the current line as a String.
     */
    public String getCurrentLineAsString(){
	return new String(lineBuffer, 0, lineLength);
    }

    /**
     * Return the specified character from the current line.
     */
    public char getChar(int offset){
	if(offset < 0 || offset >= lineLength){
	    return (char)EOF;
	}else{
	    return (char)lineBuffer[offset];
	}
    }

    /**
     * Does the current line contain the specified string at the specified
     * location.
     */
    public boolean currentLineContains(String string, int offset){
	int stringLength = string.length();

	if(stringLength + offset > lineLength){
	    return false;
	}else{
	    for(int i = 0; i < stringLength; i++){
		if(string.charAt(i) != lineBuffer[i + offset]){
		    return false;
		}
	    }

	    return true;
	}
    }

    /**
     * Return a substring of the current line.
     */
    public String getSubstring(int offset, int length){
	if(lineLength <= offset){
	    return "";
	}else if(offset + length > lineLength){
	    return new String(lineBuffer, offset, lineLength - offset);
	}else{
	    return new String(lineBuffer, offset, length);
	}
    }

    /**
     * Read an integer from the current line buffer.
     */
    public int readInteger(int offset, int length){
	int end = offset + length;

	// make sure we don't read past the end of the line.
	if(end > lineLength){
	    end = lineLength;
	}

	boolean negative = false;
	int currentPosition = offset;

	for(/* nothing */; currentPosition < end; currentPosition++){

	    byte b = lineBuffer[currentPosition];

	    if(b == ' ' || b == '+'){
		continue;
	    }else if(b == '-'){
		negative = true;
	    }else{
		break;
	    }
	}

	int total = 0;

	for(/* nothing */; currentPosition < end; currentPosition++){
	    byte b = lineBuffer[currentPosition];

	    if(b >= '0' && b <= '9'){
		total = (10 * total) + (b - '0'); 
	    }else{
		break;
	    }
	}

	if(negative){
	    return -total;
	}else{
	    return total;
	}
    }

    /**
     * Specific method for reading a 3 column integer.
     */
    public int readInteger3(int offset){
	// not yet implemented
	return readInteger(offset, 3);
    }

    /**
     * Read a double precision number from the specified region.
     */
    public double readDouble(int offset, int length){
	int end = offset + length;

	// make sure we don't read past the end of the line.
	if(end > lineLength){
	    end = lineLength;
	}

	boolean negative = false;
	int currentPosition = offset;
	long total = 0;
	boolean dotSeen = false;
	int decimalPlaces = 0;
	boolean exponentSeen = false;
	boolean exponentNegative = false;
	int exponentTotal = 0;

	for(/* nothing */; currentPosition < end; currentPosition++){

	    byte b = lineBuffer[currentPosition];

	    if(b >= '0' && b <= '9'){
		if(exponentSeen){
		    exponentTotal = exponentTotal * 10 + (b - '0');
		}else{
		    total = total * 10 + (b - '0');
		
		    if(dotSeen){
			decimalPlaces++;
		    }
		}
	    }else if(b == ' ' || b =='+'){
		continue;
	    }else if(b == '-'){
		if(exponentSeen){
		    exponentNegative = true;
		}else{
		    negative = true;
		}
	    }else if(b == '.'){
		dotSeen = true;
	    }else if(b == 'e' || b == 'E'){
		exponentSeen = true;
	    }else{
		break;
	    }
	}

	// fix up sign on exponentTotal
	if(exponentSeen && exponentNegative){
	    exponentTotal = -exponentTotal;
	}
	
	// now turn the integer into a double by taking into
	// account the number of decimal places that were seen.

	double result = (double)total;

	decimalPlaces -= exponentTotal;

	switch(decimalPlaces){
	case 1: result *= 0.1; break;
	case 2: result *= 0.01; break;
	case 3: result *= 0.001; break;
	case 4: result *= 0.0001; break;
	case 5: result *= 0.00001; break;
	case 6: result *= 0.000001; break;
	case 7: result *= 0.0000001; break;
	case 8: result *= 0.00000001; break;
	case 9: result *= 0.000000001; break;
	    // anything else and we have to do it manually
	default:
	    if(decimalPlaces > 0){
		while(decimalPlaces-- > 0){
		    result *= 0.1;
		}
	    }else{
		while(decimalPlaces++ < 0){
		    result *= 10.0;
		}
	    }
	}

	if(negative){
	    return -result;
	}else{
	    return result;
	}
    }

    /** Split the line into fields on white space characters. */
    private void determineFields(){
	int len = getLineLength();
	boolean inField = false;
	int currentLen = 0;

	fieldCount = 0;

	for(int i = 0; i < len; i++){
	    byte c = lineBuffer[i];
	    if(c != ' ' && c != '\t'){
		if(inField){
		    currentLen++;
		}else{
		    inField = true;
		    fieldStartPosition[fieldCount] = i;
		    currentLen = 1;
		    fieldCount++;
		}
	    }else{
		if(inField){
		    fieldLength[fieldCount-1] = currentLen;
		    inField = false;
		}
	    }
	}

	// if the field goes up to the end of line we
	// need to terminate the field.
	if(inField){
	    fieldLength[fieldCount-1] = currentLen;
	}

	fieldsDetermined = true;
    }

    /** Return the number of fields on the current line. */
    public int getFieldCount(){
	if(fieldsDetermined == false){
	    determineFields();
	}

	return fieldCount;
    }

    /** Get the start position of the field. */
    public int getFieldStart(int f){
	if(fieldsDetermined == false){
	    determineFields();
	}

	if(f >= fieldCount|| f < 0){
	    return -1;
	}else{
	    return fieldStartPosition[f];
	}
    }
	
    /** Get the start position of the field. */
    public int getFieldLength(int f){
	if(fieldsDetermined == false){
	    determineFields();
	}

	if(f >= fieldCount|| f < 0){
	    return 0;
	}else{
	    return fieldLength[f];
	}
    }

    /** Get the field as a string. */
    public String getField(int f){
	if(fieldsDetermined == false){
	    determineFields();
	}

	if(f >= fieldCount|| f < 0){
	    return null;
	}else{
	    int start = getFieldStart(f);
	    int len = getFieldLength(f);
	    return getSubstring(start, len);
	}
    }

    /** Get the specified character from the field. */
    public byte getFieldChar(int f, int c){
	if(fieldsDetermined == false){
	    determineFields();
	}

	if(f >= fieldCount|| f < 0){
	    System.out.println("invalid field: " + f);
	    return (byte)-1;
	}else{
	    if(c >= 0 && c < getFieldLength(f)){
		return lineBuffer[getFieldStart(f) + c];
	    }else{
		System.out.println("invalid char: field " + f + " char " + c);
		return (byte)-1;
	    }
	}
    }

    /**
     * Try and make path relative to current directory.
     */
    public static String getRelativePath(String path){

	try {
	    File currentDir = new File(".");
	    String currentPath = null;
	    currentPath = currentDir.getCanonicalPath();

	    if(currentPath != null){
		// get canonical path of file, to
		// fix case problems with drive letters in Windows.
		File pathFile = new File(path);
		String canonicalPath = pathFile.getCanonicalPath();

		if(canonicalPath.startsWith(currentPath)){
		    String relativePath = canonicalPath.substring(currentPath.length());

		    // hmm, unix doesn't have trailing / on 
		    // current dir canonical path, so remove any
		    // that are left on the front of the path here.
		    while(relativePath.startsWith("/") ||
                          relativePath.startsWith("\\")){
			relativePath = relativePath.substring(1);
		    }

		    return relativePath;
		}else{
		    return path;
		}
	    }
	}catch(Exception e){
	    return path;
	}

	return path;
    }
	
    /**
     * Return a FILE object for reading the specified resource.
     *
     * If the resource looks like it is a URL name (begins with
     * something like 'http:' or 'ftp:') then an attempt will
     * be made to open it as a URL. Otherwise the resource will
     * be opened as a file.
     */
    public static FILE open(String resource){
	FILE input = null;

	if(debug){
            System.out.println("FILE.open tryfiles=" + tryFiles);
	    System.out.println("FILE.open resource=" + resource);
	}

	if(input == null &&
	   (resource.endsWith(".properties") ||
	    resource.endsWith(".gif") ||
	    resource.endsWith(".jpg") ||
	    resource.endsWith(".jpeg"))){
	    // instantiate a file so we can use
	    // getResourceAsStream()
	    // one last chance it might be in a jar file
	    InputStream inputStream = null;
			
	    if(debug){
		System.out.println("attempt to open as absolute resource");
	    }
	    inputStream = openResource("/" + resource);
 
	    if(inputStream == null){
		if(debug){
		    System.out.println("attempt to open as relative resource");
		}
		inputStream = openResource(resource);
	    }

	    if(inputStream != null){
		if(debug){
		    System.out.println("opened as resource");
		}
		input = new FILE(inputStream);
	    }
	}

	if(input == null && documentBase != null){
	    if(debug){
		System.out.println("documentBase " + documentBase);
	    }

	    try {
		URL url = new URL(documentBase, resource);
		if(debug){
		    System.out.println("attempting to open url " + url);
		}
		input = open(url);
		if(debug && input != null){
		    System.out.println("url successfully opened");
		}
	    }catch(Exception e){
		if(debug){
		    System.out.println("**** URL exception " + e);
		}
	    }
	}

	if(input == null && codeBase != null){
	    if(debug){
		System.out.println("codeBase " + codeBase);
	    }

	    try {
		URL url = new URL(codeBase, resource);
		if(debug){
		    System.out.println("attempting to open url " + url);
		}
		input = open(url);
		if(debug && input != null){
		    System.out.println("url successfully opened");
		}
	    }catch(Exception e){
		if(debug){
		    System.out.println("**** URL exception " + e);
		}
	    }
	}

	if(input == null && stringIsURL(resource)){
	    try {
		if(debug){
		    System.out.println("attempting to open as absolute url");
		}
		input = openURL(resource);
		if(debug && input != null){
		    System.out.println("successfully opened as absolute url");
		}
	    }catch(Exception e){
		input = null;
		if(debug){
		    System.out.println("**** absolute url exception " + e);
		}
	    }
	}
		
	if(input == null && tryFiles == true){
	    try {
		if(debug){
		    System.out.println("attempting to open as file");
		}
		input = openFile(resource);
		if(debug && input != null){
		    System.out.println("successfully opened as file");
		}
	    }catch(Exception e){
		input = null;
		if(debug){
		    System.out.println("**** open file exception " + e);
		}
	    }
	}
		
	if(input != null){
	    if(debug){
		System.out.println("opened as resource");
	    }
	}

	if(resource.endsWith(".gz")){
	    try {
		GZIPInputStream gis =
		    new GZIPInputStream(input.inputStream);

		input.inputStream = gis;
	    }catch(Exception e){
		input = null;
		if(debug){
		    System.out.println("failed to open gzip'ed inputStream");
		    System.out.println("" + e);
		}
	    }
	}

	// finally try and see if there is a resource with
	// .gz extension that corresponds to the resource
	// we were asked to open
	// i.e. we were asked to open 1abc.pdb, but it
	// didn't exist. Does 1abc.pdb.gz exist instead?
	if(input == null && resource.endsWith(".gz") == false){
	    input = open(resource + ".gz");

	    if(input != null){
		return input;
	    }
	}

	return input;
    }

    /** Try and open the file as a resource from a jar file. */
    private static InputStream openResource(String resource){
	// netscape requires that you must get resources in this
	// fashion. getSystemResourceAsStream will always fail.
	// this method will also fail if we try and open
	// a file that has an extension that isn't in the approved
	// list... in a word - ridiculous.
	FILE dummy = new FILE();

	InputStream inputStream = null;

	try {
	    inputStream = 
		dummy.getClass().getResourceAsStream(resource);
	}catch(Exception e){
	}

	return inputStream;
    }

    /** Does the resource look like it is a URL? */
    public static boolean stringIsURL(String resource){
	int length = resource.length();
	int i = 0;

	for(/* nothing */; i < length - 3; i++){
	    char c = resource.charAt(i);
	    if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')){
		continue;
	    }else if(c == ':'){
		return true;
	    }
	}

	return false;
    }

    /**
     * Create a FILE object to read from the specified File object.
     */
    public static FILE open(File file){
	try {
	    FileInputStream fileInputStream = new FileInputStream(file);

	    return new FILE(fileInputStream);
	}catch(Exception e){
	    setException(e);

            if(debug){
                System.out.println("open(File): " + e);
            }

	    return null;
	}
    }

    /**
     * Create a FILE object to read from the specified filename.
     */
    public static FILE openFile(String filename){
	try {
            FILE f = null;
	    File file = new File(filename);

            if(file.isAbsolute()){
                f = open(file);
            }else{
                if(documentBase != null){
                    String docBase = documentBase.toString();
                    if(docBase.startsWith("file:/")){
                        docBase = docBase.substring(6);
                        file = new File(docBase, filename);

                        f = open(file);
                    }
                }

                if(f == null && codeBase != null){
                    String cBase = codeBase.toString();
                    if(cBase.startsWith("file:/")){
                        cBase = cBase.substring(6);
                        file = new File(cBase, filename);

                        f = open(file);
                    }
                }

                if(f == null){
                    file = new File(filename);

                    f = open(file);
                }
            }            
                
	    return f;
	}catch(Exception e){
	    setException(e);

            if(debug){
                System.out.println("openFile: " + e);
            }
	    return null;
	}
    }

    /**
     * Create a FILE object to read from the specified urlname.
     */
    public static FILE openURL(String urlname){
	try {
	    URL url = new URL(urlname);

	    return open(url);
	}catch(Exception e){
	    setException(e);

	    return null;
	}
    }

    /**
     * Create a FILE object to read from the specified URL.
     */
    public static FILE open(URL url){
	try {
	    // open the url via a URLConnection object
	    if(false){
		URLConnection urlConnection = url.openConnection();
		System.out.println("open(URL) URLConnection " + urlConnection);
		// this allows us to use cached copies of the data
		urlConnection.setUseCaches(true);
	    }

	    InputStream urlInputStream = url.openStream();
	    //urlConnection.getInputStream();

	    //System.out.println("open(URL) urlInputStream " + urlInputStream);

	    return new FILE(urlInputStream);
	}catch(Exception e){
            if(debug){
                System.out.println("open(URL) exception " + e);
            }
	    setException(e);

	    return null;
	}
    }

    /**
     * Close a file object.
     *
     * This also closes the underlying InputStream.
     * Returns true if there is a problem closing the FILE.
     */
    public void close(){

	if(inputStream != null){

	    try {
		inputStream.close();
		
		//return false;
	    }catch(Exception e){
		setException(e);
		
		//return true;
	    }
	} 

	if(outputStream != null){

	    try {
		flush();
		outputStream.close();
		
		//return false;
	    }catch(Exception e){
		setException(e);
		
		//return true;
	    }
	}
    }

    /** Convenience method to load a properties file. */
    public static Properties loadProperties(String resource){
	Properties properties = new Properties();

	FILE propertyStream = open(resource);

	if(debug){
	    System.out.println("loadProperties from " + propertyStream);
	}

	if(propertyStream != null){
	    try {
		properties.load(propertyStream);
	    }catch(Exception e){
	    }finally{
                propertyStream.close();
            }
	}

	return properties;
    }

    /* Some utility methods for decoding numbers. */

    /** Synonym for readIntegerFromField. */
    public int getInteger(int f){
	return readIntegerFromField(f);
    }

    public float getFloat(int f){
	return (float)getDouble(f);
    }

    /** Synonym for readDoubleFromField. */
    public double getDouble(int f){
	return readDoubleFromField(f);
    }

    /** Read an integer from the specified field. */
    public int readIntegerFromField(int f){
	if(fieldsDetermined == false){
	    determineFields();
	}

	if(f < 0 || f >= fieldCount){
	    System.out.println("readIntegerFromField: attempt " +
			       "to read from field " + f);
	    return 0;
	}else{
	    int start = getFieldStart(f);
	    int len = getFieldLength(f);

	    return readInteger(start, len);
	}
    }

    /** Read a double from the specified field. */
    public double readDoubleFromField(int f){
	if(fieldsDetermined == false){
	    determineFields();
	}

	if(f < 0 || f >= fieldCount){
	    System.out.println("readDoubleFromField: attempt " +
			       "to read from field " + f);
	    return 0.0;
	}else{
	    int start = getFieldStart(f);
	    int len = getFieldLength(f);

	    return readDouble(start, len);
	}
    }

    /** Read an integer from the specified string. */
    public static int readInteger(String string){
	int stringLength = string.length();
	int value = 0;
	boolean negative = false;

	for(int i = 0; i < stringLength; i++){
	    char c = string.charAt(i);

	    if(c >= '0' && c <= '9'){
		value = (10 * value) + (c - '0');
	    }else if(c == '-'){
		negative = true;
	    }
	}

	if(negative){
	    return -value;
	}else{
	    return value;
	}
    }

    /** Read a double value from a string. */
    public static double readDouble(String string){
	try {
	    // try to convert to double..
	    return Double.valueOf(string.trim()).doubleValue();
	} catch (Exception e) {
	    return 0.0;
	}
    }

    /** Convenience methods for splitting strings into fields. */
    public static String[] split(String string){
	return split(string, null);
    }
		
    /** Convenience methods for splitting strings into fields. */
    public static String[] split(String string, String separators){
	StringTokenizer tokenizer = null;

	if(separators == null){
	    tokenizer = new StringTokenizer(string);
	}else{
	    tokenizer = new StringTokenizer(string, separators);
	}

	int fieldCount = tokenizer.countTokens();
		
	String fields[] = new String[fieldCount];

	int field = 0;

	while(tokenizer.hasMoreElements()){
	    fields[field++] = tokenizer.nextToken();
	}

	return fields;
    }

    /**
     * Test program for the FILE class.
     */
    public static void main(String args[]){
	if(args.length > 0){
	    FILE f = FILE.write(args[0]);
	    
	    if(f != null){
		f.print("hello\n");
		
		f.close();
	    }else{
		System.out.println("couldn't open " + args[0]);
	    }
	}

	
	if(false){
	    if(args.length > 0){
		if(true){
		    FILE file = FILE.open(args[0]);
		    
		    while(file.nextLine()){
			String line = file.getCurrentLineAsString();
			System.out.println(line + "|");
			int nf = file.getFieldCount();
			System.out.println("field count "+ nf);
			for(int i = 0; i < nf; i++){
			    String f = file.getField(i);
			    
			    System.out.print(i + "|" + f + "| ");
			    System.out.print(" = " +
					     file.readDoubleFromField(i) + " ");
			}

			System.out.println("");
		    
		    }

		    file.close();
		}else{
		    int iterations = 10;
		    long totalTime = 0;
		    
		    for(int i = 0; i < iterations; i++){
			FILE file = FILE.openFile(args[0]);
			
			long startTime = System.currentTimeMillis();
			
			System.out.println("read " + readFrom(file) +
					   " bytes from " + args[0]);

			long stopTime = System.currentTimeMillis();
			
			totalTime += (stopTime - startTime);
		    }

		    System.out.println("average read time = " +
				       (double)totalTime/(double)iterations +"ms");
		}
	    }else{
		System.out.println("usage: java FILE file.name");
	    }
	}
    }

    /**
     * Read the data from the specified FILE object.
     */
    public static int readFrom(FILE file){
	int bytesRead = 0;

	//String line = null;
	//while((line = file.readLine()) != null){
	//	System.out.println("<" + line + ">");
	//}

	while(file.read() != FILE.EOF){
	    bytesRead++;
	}

	//while(file.nextLine()){
	//	System.out.println("length " + file.getLineLength());
	//	bytesRead++;
	//}

	return bytesRead;
    }

    
}
