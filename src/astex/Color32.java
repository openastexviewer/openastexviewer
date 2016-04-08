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

/**
 * Manipulate packed colors.
 */
import java.util.*;
import java.awt.*;

public class Color32 {
    /** Alpha bits. */
    public static final int Alpha = 0xFF000000;

    /** Red bits. */
    public static final int Red = 0xFF0000;

    /** Green bits. */
    public static final int Green = 0xFF00;

    /** Blue bits. */
    public static final int Blue = 0xFF;

    public static int getComponent(int c, int comp){
	if(comp == 0) return getRed(c);
	if(comp == 1) return getGreen(c);
	if(comp == 2) return getBlue(c);

	return 0;
    }

    /** Returns the red component. */
    public static int getRed(int c){
	return (c & Red) >> 16;
    }

    /** Returns the green component. */
    public static int getGreen(int c){
	return (c & Green) >> 8;
    }

    /** Returns the blue component. */
    public static int getBlue(int c){
	return c & Blue;
    }

    public static int getIntensity(int c){
	//print("color ", c);

	int r = getRed(c);
	int g = getGreen(c);
	int b = getBlue(c);

	int intensity = (int)((r+g+b)/3.0);

	return intensity;
    }

    public static int getMaximumIntensity(int c){
	//print("color ", c);

	int r = getRed(c);
	int g = getGreen(c);
	int b = getBlue(c);

	int max = r;

	if(g > max){
	    max = g;
	}
	if(b > max){
	    max = b;
	}

	//System.out.println("getIntensity " + intensity);

	return max;
    }

    public static int getGrayScale(int c){
	int r = getRed(c);
	int g = getGreen(c);
	int b = getBlue(c);

	int luma = (int)(r*0.3 + g*0.59+ b*0.11);
	if(luma > 255) luma = 255;

	return pack(luma, luma, luma);
    }

    /** Clamp components to 0,255 and return rgb color. */
    public static int getClampColor(int r, int g, int b){
	return Alpha|(clamp(r)<<16)|(clamp(g)<<8)|clamp(b);
    }

    /** Clamp number to range specified. */
    public static final int clamp(int num) {
	if(num < 0) return 0;
	else if(num > 255) return 255;
	else return num;
    }

    public static final int scale(int c, int factor) {
	if (factor == 255) return c;
	if (factor == 0) return 0;
	int r = (c>>16)&0xff;
	int g = (c>>8)&0xff;
	int b = c&0xff;

	r = (r * factor)>>8;
	g = (g * factor)>>8;
	b = (b * factor)>>8;

	return (r<<16)|(g<<8)|b;
    }

    public static final int add(int color1, int color2) {
	int r = (color1 & Red) + (color2 & Red);
	int g = (color1 & Green) + (color2 & Green);
	int b = (color1 & Blue) + (color2 & Blue);
	if (r > Red  ) r = Red;
	if (g > Green) g = Green;
	if (b > Blue ) b = Blue;
	return (r|g|b);
    }

    public static final int multiply(int c1, int c2) {
	int c1r = (c1 >> 16) & 0xff;
	int c1g = (c1 >> 8) & 0xff;
	int c1b = (c1 >> 0) & 0xff;
	int c2r = (c2 >> 16) & 0xff;
	int c2g = (c2 >> 8) & 0xff;
	int c2b = (c2 >> 0) & 0xff;
	
	// the mutliply means we need to scale down by 256
	// equivalent to right shift 8 bits
	c2r = (c1r * c2r) >> 8;
	c2g = (c1g * c2g) >> 8;
	c2b = (c1b * c2b) >> 8;
	
	return (c2r << 16) | (c2g <<  8) | (c2b);
    }

    /** multiply by intensity between 0 and 255 and specularise. */
    public static final int intensitySpecularise(int c1, int intensity, int specular) {

	c1 = scale(c1, intensity);

	return add(c1, specular);
    }

    // blend alpha's worth of c1 and 255-alpha's worth of c2
    // I'm sure there are faster ways of doing this...
    public static final int blend(int c1, int c2, int alpha) {
	if (alpha==0) return c2;
	if (alpha==255) return c1;

	int c1r = (c1 >> 16) & 0xff;
	int c1g = (c1 >> 8) & 0xff;
	int c1b = (c1 >> 0) & 0xff;
	int c2r = (c2 >> 16) & 0xff;
	int c2g = (c2 >> 8) & 0xff;
	int c2b = (c2 >> 0) & 0xff;
	
	int r = (alpha*(c1r) + (255-alpha)*(c2r))>>8;
	int g = (alpha*(c1g) + (255-alpha)*(c2g))>>8;
	int b = (alpha*(c1b) + (255-alpha)*(c2b))>>8;

	return Alpha|(r<<16)|(g<<8)|b;
    }

    /** Interpolate two colors. (frac*c1 + (1.-frac)*c2)*/
    public static int blend(int c1, int c2, double frac){
	return blend(c1, c2, (int)(255 * frac));
    }

    /** Convert hsv format colour to rgb. */
    public static void hsv2rgb (double hsv[], double rgb[]){
	int HueQuadrant;
	double HueLocal, Diff, m, n, k;

	HueLocal = hsv[0];
	while (HueLocal >= 360.0)  HueLocal = HueLocal - 360.0;
	HueLocal = HueLocal / 60.0;
	HueQuadrant = (int)HueLocal;
	Diff = HueLocal - HueQuadrant;
	m = hsv[2] * (1.0 - hsv[1]);
	n = hsv[2] * (1.0 - hsv[1] * Diff);
	k = hsv[2] * (1.0 - hsv[1] * (1.0 - Diff));

	switch(HueQuadrant){
	case 0: rgb[0] = hsv[2]; rgb[1] = k;  rgb[2] = m; break;
	case 1: rgb[0] = n;  rgb[1] = hsv[2]; rgb[2] = m; break;
	case 2: rgb[0] = m;  rgb[1] = hsv[2]; rgb[2] = k; break;
	case 3: rgb[0] = m;  rgb[1] = n;  rgb[2] = hsv[2]; break;
	case 4: rgb[0] = k;  rgb[1] = m;  rgb[2] = hsv[2]; break;
	case 5: rgb[0] = hsv[2]; rgb[1] = m;  rgb[2] = n; break;
	}

	// clamp to range 0.0-1.0
	// not sure if this is actually necesarry.
	for(int i = 0; i < 3; i++){
	    if(rgb[i] < 0.0) rgb[i] = 0.0;
	    else if(rgb[i] > 1.0) rgb[i] = 1.0;
	}
    }

    /** Pack r,g,b triple into a single int. */
    public static final int pack(int r, int g, int b) {
	return Alpha | r << 16 | g << 8 | b;
    }

    /** Convert rgb to hsv. */
    public static void rgb2hsv (double rgb[], double hsv[]){
	double x, Rtemp, Gtemp, Btemp;

	hsv[2] = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));

	x = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));

	hsv[1] = (hsv[2] - x) / hsv[2];

	Rtemp = (hsv[2] - rgb[0]) * 60 / (hsv[2] - x);
	Gtemp = (hsv[2] - rgb[1]) * 60 / (hsv[2] - x);
	Btemp = (hsv[2] - rgb[2]) * 60 / (hsv[2] - x);

	if(rgb[0] == hsv[2]){
	    if (rgb[1] == x)
		hsv[0] = 300 + Btemp;
	    else
		hsv[0] = 60 - Gtemp;
	}else if(rgb[1] == hsv[2]){
	    if (rgb[2] == x)
		hsv[0] = 60 + Rtemp;
	    else
		hsv[0] = 180 - Btemp;
	}else{
	    if (rgb[0] == x)
		hsv[0] = 180 + Gtemp;
	    else
		hsv[0] = 300 - Rtemp;
	}
    }

    /** Working space for rgb conversions. */
    private static double rgbtmp[] = new double[3];

    /** Convert hsv to packed int color. */
    public static int hsv2packed(double hsv[]){
	int r, g, b;

	hsv2rgb(hsv, rgbtmp);

	r = (int)(rgbtmp[0] * 255.0);
	g = (int)(rgbtmp[1] * 255.0);
	b = (int)(rgbtmp[2] * 255.0);

	int c = pack(r, g, b);

	//print("rgb=", c);

	return c;
    }

    /** Convert a packed rgb number to hsv. */
    public static void packed2hsv(int rgb, double hsv[]){
	int r = getRed(rgb);
	int g = getGreen(rgb);
	int b = getBlue(rgb);

	rgbtmp[0] = r / 255.0;
	rgbtmp[1] = g / 255.0;
	rgbtmp[2] = b / 255.0;

	rgb2hsv(rgbtmp, hsv);
    }

    /** Print a colour representation. */
    public static final void print(String s, int c){
	print(s, getRed(c), getGreen(c), getBlue(c));
    }

    /** Print a colour representation. */
    public static final void print(String s, int r, int g, int b){
	FILE.out.print(s);
	FILE.out.print(" [%03d", r);
	FILE.out.print(",%03d", g);
	FILE.out.print(",%03d]\n", b);
    }

    public static String format(int c){
	return "'0x" +
	    FILE.sprint("%02x", getRed(c)) +
	    FILE.sprint("%02x", getGreen(c)) +
	    FILE.sprint("%02x'", getBlue(c));
    }

    public static String formatNoQuotes(int c){
	return "0x" +
	    FILE.sprint("%02x", getRed(c)) +
	    FILE.sprint("%02x", getGreen(c)) +
	    FILE.sprint("%02x", getBlue(c));
    }

    /* Some internal colors. */
    public static final int snow       = pack(255, 250, 250);
    public static final int gainsboro  = pack(220, 220, 220);
    public static final int linen      = pack(250, 240, 230);
    public static final int bisque     = pack(255, 228, 196);
    public static final int moccasin   = pack(255, 228, 181);
    public static final int cornsilk   = pack(255, 248, 220);
    public static final int ivory      = pack(255, 255, 240);
    public static final int seashell   = pack(255, 245, 238);
    public static final int honeydew   = pack(240, 255, 240);
    public static final int azure      = pack(240, 255, 255);
    public static final int lavender   = pack(230, 230, 250);
    public static final int white      = pack(255, 255, 255);
    public static final int black      = pack(  0,   0,   0);
    public static final int navy       = pack(  0,   0, 128);
    public static final int blue       = pack(  0,   0, 255);
    public static final int turquoise  = pack( 64, 224, 208);
    public static final int cyan       = pack(  0, 255, 255);
    public static final int aquamarine = pack(127, 255, 212);
    public static final int green      = pack(  0, 255,   0);
    public static final int chartreuse = pack(127, 255,   0);
    public static final int khaki      = pack(240, 230, 140);
    public static final int yellow     = pack(255, 255,   0);
    public static final int gold       = pack(255, 215,   0);
    public static final int goldenrod  = pack(218, 165,  32);
    public static final int sienna     = pack(160,  82,  45);
    public static final int peru       = pack(205, 133,  63);
    public static final int burlywood  = pack(222, 184, 135);
    public static final int beige      = pack(245, 245, 220);
    public static final int wheat      = pack(245, 222, 179);
    public static final int tan        = pack(210, 180, 140);
    public static final int chocolate  = pack(210, 105,  30);
    public static final int firebrick  = pack(178,  34,  34);
    public static final int brown      = pack(165,  42,  42);
    public static final int salmon     = pack(250, 128, 114);
    public static final int orange     = pack(255, 165,   0);
    public static final int coral      = pack(255, 127,  80);
    public static final int tomato     = pack(255,  99,  71);
    public static final int red        = pack(255,   0,   0);
    public static final int pink       = pack(255, 192, 203);
    public static final int maroon     = pack(176,  48,  96);
    public static final int magenta    = pack(255,   0, 255);
    public static final int violet     = pack(238, 130, 238);
    public static final int plum       = pack(221, 160, 221);
    public static final int orchid     = pack(218, 112, 214);
    public static final int purple     = pack(160,  32, 240);
    public static final int thistle    = pack(216, 191, 216);
    public static final int grey       = pack(190, 190, 190);
    public static final int gray       = grey;
    public static final int rwb0       = pack( 255,   0,   0 ); // rwb0
    public static final int rwb1       = pack( 255, 115, 115 ); // rwb1
    public static final int rwb2       = pack( 255, 170, 170 ); // rwb2
    public static final int rwb3       = pack( 255, 215, 215 ); // rwb3
    public static final int rwb4       = pack( 215, 215, 255 ); // rwb4
    public static final int rwb5       = pack( 170, 170, 255 ); // rwb5
    public static final int rwb6       = pack( 115, 115, 255 ); // rwb6
    public static final int rwb7       = pack(   0,   0, 255 ); // rwb7

    public static final int undefinedColor = white;

    /** Hashtable for storing color name -> rgb values. */
    private static Hashtable colorHash = null;

    /** Valid digits for a hex format number. */
    private static String hexDigits = "0123456789abcdefABCDEFx";

    /** Return packed color value from string name. */
    public static int getColorFromName(String colorName){
	int len = colorName.length();
	int color = undefinedColor;

	if(colorName.indexOf(",") != -1){
	    String components[] = FILE.split(colorName, ",");
	    if(components.length == 3){
		int r = FILE.readInteger(components[0]);
		int g = FILE.readInteger(components[1]);
		int b = FILE.readInteger(components[2]);
		color = Color32.getClampColor(r, g, b);
	    }else{
		Log.error("color should be \"r,g,b\" not: " + colorName);
	    }

	    return color;
	}

	boolean allHex = true;

	for(int i = 0; i < len; i++){
	    char c = colorName.charAt(i);
	    if(hexDigits.indexOf(c) == -1){
		allHex = false;
		break;
	    }
	}

	if(allHex){
	    int start = -1;
	    int group_mult = 256;
	    int char_mult = 16;

	    if(len == 8 &&
	       colorName.charAt(0) == '0' &&
	       colorName.charAt(1) == 'x'){
		start = 2;
		group_mult = 16;
		char_mult = 1;
	    }else if(len == 6){
		start = 0;
		group_mult = 16;
		char_mult = 1;
	    }else if(len == 7){
		start = 1;
	    }else if(len == 4){
		start = 1;
		group_mult = 16;
		char_mult = 1;
	    }else if(len == 3){
		start = 0;
	    }

	    if(start == -1){
		System.out.println("illegal color format: " + colorName);
		return undefinedColor;
	    }

	    int total = 0;

	    for(int i = start; i < len; i++){
		int c = colorName.charAt(i);
		if(c >= '0' && c <= '9'){
		    total = group_mult * total + (c - '0') * char_mult;
		}else if(c >= 'a' && c <= 'f'){
		    total = group_mult * total + (c - 'a' + 10) * char_mult;
		}else if(c >= 'A' && c <= 'F'){
		    total = group_mult * total + (c - 'A' + 10) * char_mult;
		}else{
		    System.out.println("getColorIndex: illegal char: " +
				       (char)c);
		}
	    }

	    color = total;
	}else{
	    ensureColorHashDefined();
	    colorName = colorName.toLowerCase();

	    Integer colorValue = (Integer)colorHash.get(colorName);

	    if(colorValue != null){
		color = colorValue.intValue();
	    }else{
		System.out.println("couldn't find color " + colorName);

		color = undefinedColor;
	    }
	}

	return color;
    }

    /** Return AWT color of this. */
    public static Color getAWTColor(int color){
	Color awtColor =
	    new Color(getRed(color), getGreen(color), getBlue(color));

	return awtColor;
    }

    /** Make sure the color hash is defined and initialised. */
    private static void ensureColorHashDefined(){
	if(colorHash == null){
	    colorHash = new Hashtable();

	    FILE f = FILE.open("color.properties");

	    while(f.nextLine()){
		if(f.getChar(0) != '#'){
		    int fc = f.getFieldCount();
		    if(fc != 4){
			System.out.println("color.properties: invalid color definition");
			System.out.println(f.getCurrentLineAsString());
		    }else{
			String name = f.getField(0);
			name = name.toLowerCase();
			Integer colorValue =
			    new Integer(pack(f.getInteger(1),
					     f.getInteger(2),
					     f.getInteger(3)));
			colorHash.put(name, colorValue);
		    }
		}else{
		    System.out.println(f.getCurrentLineAsString());
		}
	    }

	    f.close();
	}

	return;
    }
}
