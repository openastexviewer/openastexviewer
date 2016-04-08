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

public class ViewCommand {
    private static int    defaultWidth     = 400;
    private static int    defaultHeight    = 300;
    private static int    defaultSample    = 1;
    public  static int    stepMultiple     = 1;
    private static String defaultImage     = "movie/image_%04d.bmp";
    private static boolean defaultCompress = false;

    private static int tempBuffer[] = null;

    /** Execute a view command. */
    public static void execute(MoleculeViewer mv, Arguments args){
	MoleculeRenderer mr      = mv.getMoleculeRenderer();

	double zrot              = args.getDouble("-rotatez",
						  Double.NEGATIVE_INFINITY);
	Object antialiasString   = args.get("-antialias");
	Object realSpheres       = args.get("-realspheres");
	Object renderDebugString = args.get("-renderdebug");
	String image             = args.getString("-writeimage", null);

	if(args.get("-defaultwidth") != null){
	    defaultWidth = args.getInteger("-defaultwidth", 400);
	}

	if(args.get("-defaultheight") != null){
	    defaultHeight = args.getInteger("-defaultheight", 300);
	}

	if(args.get("-defaultsample") != null){
	    defaultSample = args.getInteger("-defaultsample", 1);
	}

	stepMultiple = args.getInteger("-stepmultiple", 1);

	if(args.get("-defaultimage") != null){
	    defaultImage = (String)args.get("-defaultimage");
	}

	if(args.get("-defaultcompress") != null){
	    defaultCompress = args.getBoolean("-defaultcompress", false);
	}

	if(args.get("-ambient") != null){
	    int amb = args.getInteger("-ambient", 64);
	    mr.renderer.setAmbient(Color32.pack(amb, amb, amb));
	}

	if(args.get("-normalcutoff") != null){
	    double c = args.getDouble("-normalcutoff", 0.07);
	    mr.renderer.setCartoonNormalCutoff(c);
	}

	if(args.get("-lightingmodel") != null){
	    String model = args.getString("-lightingmodel", "normal");

	    if(model.equals("normal")){
		mr.renderer.setLightingModel(Renderer.DefaultLightingModel);
	    }else if(model.equals("cartoon")){
		mr.renderer.setLightingModel(Renderer.CartoonLightingModel);
	    }else{
		Log.error("unknown lighting model: " + model);
	    }
	}

        if(args.get("-gradient") != null){
            mr.renderer.setGradient(args.getBoolean("-gradient", false));
        }

        if(args.get("-gradienttop") != null){
            mr.renderer.setGradientTop(args.getColor("-gradienttop", Color32.black));
        }

        if(args.get("-gradientbottom") != null){
            mr.renderer.setGradientBottom(args.getColor("-gradientbottom", Color32.black));
        }

	if(args.get("-wuantialias") != null){
	    mr.renderer.wuAntiAlias = args.getBoolean("-wuantialias", false);
	    System.out.println("wu " + mr.renderer.wuAntiAlias);
	}

	if(image != null){
	    int width        = args.getInteger("-width", -1);
	    int height       = args.getInteger("-height", -1);
	    int sample       = args.getInteger("-sample", -1);
	    int multiple     = args.getInteger("-multiple", -1);
	    boolean compress = args.getBoolean("-compress", false);
	    int oldWidth  = -1;
	    int oldHeight = -1;

	    if(image.equals("default")){
		image = defaultImage;
	    }

	    if(multiple > 0){
		width = mr.renderer.pixelWidth * multiple;
		height = mr.renderer.pixelHeight * multiple;
	    }

	    if(width == -1 && height == -1){
		width = defaultWidth;
		height = defaultHeight;
	    }

	    if(sample == -1){
		sample = defaultSample;
	    }

	    int previousSample = mr.renderer.getSamples();

	    width *= sample;
	    height *= sample;

	    if(width != -1 && height != -1){
		oldWidth  = mr.renderer.pixelWidth;
		oldHeight = mr.renderer.pixelHeight;
		System.out.println("Image size " + width + "x" + height);
		double mb = (width*height*8)/(1024.0*1024.0);
		FILE.out.print("Approximate memory use %.1fMb\n", mb);
		mv.resetAwtImage();
		mr.renderer.setSamples(sample);
		mr.renderer.setSize(width, height);
		mr.paint();
	    }

	    if(compress == false && defaultCompress == true){
		compress = true;
	    }

	    if(sample != 1){
		doAntialias(mr.renderer.pbuffer,
			    mr.renderer.pixelWidth, mr.renderer.pixelHeight,
			    sample);
		ImageIO.write(image, tempBuffer,
			      mr.renderer.pixelWidth/sample,
			      mr.renderer.pixelHeight/sample, compress);
	    }else{
		ImageIO.write(image, mr.renderer.pbuffer,
			      mr.renderer.pixelWidth, mr.renderer.pixelHeight,
			      compress);
	    }

	    System.out.println("done.");

	    if(oldHeight != -1 || oldWidth != -1){
		mr.renderer.setSize(oldWidth, oldHeight);
		// need to force the awtimage to reflect
		// the (possibly) new pixel buffer
		// ... this is a mess
		mv.resetAwtImage();
		mr.paint();
	    }

	    mr.renderer.setSamples(previousSample);
	}

	if(zrot != Double.NEGATIVE_INFINITY){
	    //Log.info("zrot %.1f", zrot);
	    mr.renderer.rotateY(zrot);
	}

	if(args.defined("-wrapangle")){
            mr.renderer.setWrapAngle(Math.PI * args.getDouble("-wrapangle", 90.0)/180.0);
	}

	if(args.defined("-aagamma")){
            mr.renderer.setDrawGamma(args.getDouble("-aagamma", 2.35));
	}

	if(args.defined("-powfactor")){
            mr.renderer.setPowFactor(args.getDouble("-powfactor", 1.0));
	}

	if(antialiasString != null){
	    boolean antialias = args.getBoolean("-antialias", false);
	    mr.renderer.setAntiAlias(antialias);
	}

	if(realSpheres != null){
	    boolean spheres = args.getBoolean("-realspheres", false);

	    mr.renderer.analyticalSpheres = spheres;
	}

	if(renderDebugString != null){
	    boolean renderDebug = args.getBoolean("-renderdebug", false);

	    mr.renderer.debug = renderDebug;
	}

	if(args.get("-solidfonts") != null){
	    mr.hersheyFonts = args.getBoolean("-solidfonts", false);
	}

	if(args.get("-shadows") != null){
	    mr.shadows = args.getBoolean("-shadows", false);
	}

	if(args.get("-background") != null){
	    String colorName = args.getString("-background", "white");
	    int color = Color32.getColorFromName(colorName);
	    mr.renderer.setBackgroundColor(color);
	}

	if(args.get("-fog") != null){
	    boolean f = args.getBoolean("-fog", false);
	    mr.renderer.depthcue = f;
	}

	mv.dirtyRepaint();
    }

    public static void doAntialias(int pbuffer[], int w, int h, int sample){
	int wa = w / sample;
	int ha = h / sample;
	int pa = wa * ha;

	if(tempBuffer == null || tempBuffer.length < pa){
	    tempBuffer = new int[pa];
	}

	int indexa = 0;
	int sample2 = sample * sample;

	for(int j = 0; j < ha; j++){
	    for(int i = 0; i < wa; i++){
		int r = 0;
		int g = 0;
		int b = 0;

		for(int iys = 0; iys < sample; iys++){
		    int iy = j * sample + iys;
		    for(int ixs = 0; ixs < sample; ixs++){
			int ix = i * sample + ixs;

			int index =  ix + w * iy;

			int rgb = pbuffer[index];

			int rp  = (rgb >> 16) & 0xff;
			int gp  = (rgb >> 8) & 0xff;
			int bp  = (rgb & 0xff);

			r += rp;
			g += gp;
			b += bp;
		    }
		}

		r /= sample2;
		g /= sample2;
		b /= sample2;

		tempBuffer[indexa] = Color32.pack(r, g, b);

		indexa++;
	    }
	}
    }
}
