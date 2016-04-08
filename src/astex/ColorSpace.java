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

import java.awt.*;
import java.awt.image.*;

public class ColorSpace extends Canvas {
    int width = -1;
    int height = -1;
    int pixels[] = null;

    /** The image that we will use. */
    private Image awtImage = null;
    
    /** The pixel buffer that will produce the image. */
    MemoryImageSource memoryImageSource = null;

    private double hsv[] = new double[3];
    private double rgb[] = new double[3];
    private int irgb[] = new int[3];

    public void paint(Graphics gr){
	if(bounds().width != width ||
	   bounds().height != height){
	    width = bounds().width;
	    height = bounds().height;

	    pixels = new int[width*height];

	    memoryImageSource =
		new MemoryImageSource(width, height,
				      new DirectColorModel(32, 0xff0000,
				      		   0xff00, 0xff),
				      pixels,
				      0, width);
	    memoryImageSource.setAnimated(true);
	    //memoryImageSource.setFullBufferUpdates(true);
	    awtImage = createImage(memoryImageSource);

	    int p = 0;

	    for(int j = 0; j < height; j++){
		for(int i = 0; i < width; i++){
                    //if((i%16) == 0 && (j%16) == 0){
                    //  System.out.println("<button width=\"12\" height=\"12\" background=\"0x" + Integer.toHexString(0xff000000 | getRGB(i,j)).substring(2, 8) + "\"/>");
                    //}

		    pixels[p++] = getRGB(i, j);
		}
	    }
	}

	memoryImageSource.newPixels();

	//awtImage.flush();

	gr.drawImage(awtImage, 0, 0, null);
    }

    public int getRGB(int i, int j){
	int h2 = height/2;
	int w = width;

	hsv[0] = 360*i/w;
	if(j < h2){
	    hsv[2] = j/(double)h2;
	    hsv[1] = 1.;
	}else if(j == height/2){
	    hsv[1] = 1.0;
	    hsv[2] = 1.0;
	}else{
	    hsv[1] = 1. - (j-h2)/(double)h2;
	    hsv[2] = 1.;
	}

	Color32.hsv2rgb(hsv, rgb);

	for(int cc = 0; cc < 3; cc++){
	    irgb[cc] = 15*(int)(17 * rgb[cc]);
	}

	return 0xff000000 | Color32.pack(irgb[0], irgb[1], irgb[2]);
    }

    /**
     * Methods below here implement the GIMP watercolor chooser.
     * Its nice, but you only get fully saturated colours.
     */
    public void paint2(Graphics gr){
	if(bounds().width != width ||
	   bounds().height != height){
	    width = bounds().width;
	    height = bounds().height;

	    pixels = new int[width*height];

	    memoryImageSource =
		new MemoryImageSource(width, height,
				      new DirectColorModel(32, 0xff0000,
				      		   0xff00, 0xff),
				      pixels,
				      0, width);
	    memoryImageSource.setAnimated(true);
	    awtImage = createImage(memoryImageSource);

	    int p = 0;

	    double r  = 0.0, g  = 0.0, b  = 0.0;
	    double dr = 0.0, dg = 0.0, db = 0.0;

	    for(int j = 0; j < height; j++){
		r = calc (0, j, 0);
		g = calc (0, j, 120);
		b = calc (0, j, 240);

		dr = calc (1, j, 0) - r;
		dg = calc (1, j, 120) - g;
		db = calc (1, j, 240) - b;

		for(int i = 0; i < width; i++){
		    //for (x = 0; x < IMAGE_SIZE; x++) {
		    int ir = Color32.clamp ((int) r);
		    int ig = Color32.clamp ((int) g);
		    int ib = Color32.clamp ((int) b);
		    
		    int c = Color32.pack(ir,ig,ib);

		    r += dr;
		    g += dg;
		    b += db;
                    
		    pixels[p++] = c;
		}
	    }
	}

	memoryImageSource.newPixels();

	//awtImage.flush();

	gr.drawImage(awtImage, 0, 0, null);
    }

    public double calc (double x, double y, double angle) {
	double s, c;

	s = 1.6 * Math.sin (angle * Math.PI / 180) * 256.0 / width;
	c = 1.6 * Math.cos (angle * Math.PI / 180) * 256.0 / width;

	return 128 + (x - (width >> 1)) * c - (y - (width >> 1)) * s;
    }

    public int getRGB2(int i, int j){
	if(i < 0) i = 0;
	if(j < 0) j = 0;
	if(i >= width) i = width - 1;
	if(j >= height) j = height - 1;

	return pixels[i + j * width];
    }

    public Dimension getPreferredSize(){
        return new Dimension(200, 200);
    }

    public static void main(String args[]){
	Frame f = new Frame();
	f.add(new ColorSpace());

	f.pack();
	f.show();
    }
}
