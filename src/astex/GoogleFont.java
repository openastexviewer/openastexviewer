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

/**
 * Prepare an image of a string that looks like
 * the rather groovy outlined font that Google maps
 * uses.
 * 
 * Needs an AWT component to do the offscreen font rendering.
 */
public class GoogleFont {
    /** We need a component for making images. */
    private static Component component = null; 

    public static void setComponent(Component c){
        component = c;
        //component.setSize(maxWidth, maxHeight);
    }

    private static Font imageFont = null;
    private static Image image = null;
    private static PixelGrabber pg = null;

    private static int maxWidth  = 200;
    private static int maxHeight =  80;

    private static final int matteColor = 0x00ffff;
    private static int pix[] = null;
    private static int impix[] = null;
    private static int finalpix[] = null;

    public static int[] makeFontImage(String s, int fg, int bg, int size[]){

        if(component == null){
            Frame frame = new Frame();
            frame.addNotify();

            component = (Component)frame;
            
            if(component == null){
                throw new Error("makeFontImage: you must call setComponent first");
            }
        }

        if(imageFont == null){
            imageFont = new Font("Arial", Font.PLAIN, 20);
        }
        
        Image image = component.createImage(maxWidth, maxHeight);
        //Image image = Toolkit.getDefaultToolkit().createImage((ImageProducer)component);

        Graphics gr = image.getGraphics();

        if(true){
            // there are approximately 128,345 different
            // bugs associated with offscreen images.
            // do not under any circumstances attempt to 
            // change the current color, or specify the background
            // or foreground color for the parent component.
            // this silently does absolutely nothing under jdk1.1.8.
            // you will use the colors that you are given and you
            // will not moan about it
            gr.setFont(imageFont);
            gr.drawString(s, 10, maxHeight - 10);
        }

        if(pix == null){
            pix = new int[maxWidth * maxHeight];
        }

        // pixelgrabber is one shot for some infathomable reason
        pg = new PixelGrabber(image, 0, 0, maxWidth, maxHeight, pix, 0, maxWidth);

        try {
            pg.grabPixels();
        }catch(Exception e){
            throw new Error("makeFontImage: interrupted grabbing pixels");
        }


        Color foreground = component.getForeground();
        int actualfg = foreground.getRGB() & 0xffffff;
        //print.f("foreground " + actualfg);

        // now get the boundary of the font.
        int xmin = maxWidth;
        int xmax = 0;
        int ymin = maxHeight;
        int ymax = 0;

        int pixel = 0;

        for(int y = 0; y < maxHeight; y++){
            for(int x = 0; x < maxWidth; x++){
                int p = pix[pixel++] & 0xffffff;

                if(p == actualfg){
                    if(x < xmin) xmin = x;
                    if(y < ymin) ymin = y;
                    if(x > xmax) xmax = x;
                    if(y > ymax) ymax = y;
                }
            }
        }

        int border = 8;

        if(impix == null){
            impix = new int[(maxWidth + 2 * border) * (maxHeight + 2 * border)];
        }

        int w = xmax - xmin + 2 * border;
        int h = ymax - ymin + 2 * border;

        if(w % 2 == 1) w += 1;
        if(h % 2 == 1) h += 1;

        int pixelCount = w * h;

        for(int p = 0; p < pixelCount; p++){
            impix[p] = matteColor;
        }

        if(false){
            print.f("w " + w);
            print.f("h " + h);
            print.f("pixelCount " +pixelCount);
            
            print.f("xmin %3d ", xmin);
            print.f("xmax %3d ", xmax);
            print.f("ymin %3d ", ymin);
            print.f("ymax %3d\n", ymax);
        }

        // copy text into centered new image
        for(int y = ymin; y <= ymax; y++){
            for(int x = xmin; x <= xmax; x++){
                int oldp = x + maxWidth * y;
                int newp = (x-xmin) + border + (((y-ymin) + border) * w);

                if((pix[oldp] & 0xffffff) == actualfg){
                    impix[newp] = fg;
                }
            }
        }

        int actualBorder = 4;
        int actualBorder2 = actualBorder * actualBorder;
        int region = actualBorder + 2;

        // fill in the background border around the lettering
        if(true){
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                int newp = x + y * w;
                int p = impix[newp];

                if(p == fg){
                    for(int y2 = y - region; y2 <= y + region; y2++){
                        for(int x2 = x - region; x2 <= x + region; x2++){
                            if(y2 >= 0 && y2 < h && x2 >= 0 && x2 < w){
                                int refp = x2 + y2 * w;
                                int p2 = impix[refp];
                                
                                if(p2 != fg){
                                    int dx = x2 - x;
                                    int dy = y2 - y;
                                    if(dx*dx + dy*dy <= actualBorder2){
                                        impix[refp] = bg;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        if(finalpix == null){
            finalpix = new int[impix.length/4];
        }

        // now supersample
        for(int y = 0; y < h; y += 2){
            for(int x = 0; x < w; x += 2){
                int a = 0;
                int r = 0;
                int g = 0;
                int b = 0;
                int matteCount = 0;

                for(int xoff = 0; xoff < 2; xoff++){
                    for(int yoff = 0; yoff < 2; yoff++){
                        int poff = (x + xoff) + ((y + yoff)*w);
                        int p = impix[poff];

                        if(p == matteColor){
                            matteCount++;
                        }else{
                            r += (p & 0xff0000) >> 16;
                            g += (p & 0xff00) >> 8;
                            b += (p & 0xff);
                        }
                    }
                }

                r >>= 2;
                g >>= 2;
                b >>= 2;

                switch(matteCount){
                case 4: a = 0; break;
                case 3: a = 64; break;
                case 2: a = 128; break;
                case 1: a = 192; break;
                case 0: a = 255; break;
                default: a = 255; break;
                }

                int dest = (x/2) + (y/2) * w/2;

                finalpix[dest] = ((a&255)<< 24) | ((r&255)<<16) | ((g&255)<<8) | (b&255);
            }
        }

        //size[0] = maxWidth;
        //size[1] = maxHeight;
        //size[0] = w/2;
        //size[1] = h/2;
        size[0] = w/2;
        size[1] = h/2;

        //print.f("about to return pix component=" + component);

        impix = null;
        pix = null;

        return finalpix;
    }
}