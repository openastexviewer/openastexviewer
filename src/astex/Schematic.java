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
 * Class for creating schematic diagrams.
 */
import java.util.*;

public class Schematic {
    /** Create a schematic. */
    public static Tmesh create(Arguments args, MoleculeRenderer mr, DynamicArray atoms){
	String name = args.getString("-name", "defaultSchematic");
	Tmesh tm = new Tmesh();
	tm.setName(name);

	AtomIterator iterator = mr.getAtomIterator();
	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    atom.setTemporarilySelected(false);
	}

	int atomCount = atoms.size();
	
	for(int a = 0; a < atomCount; a++){
	    Atom atom = (Atom)atoms.get(a);
	    atom.setTemporarilySelected(true);
	}

	try {
	    int moleculeCount = mr.getMoleculeCount();

	    for(int i = 0; i < moleculeCount; i++){
		Molecule m = mr.getMolecule(i);
		for(int c = 0; c < m.getChainCount(); c++){
		    Chain chain = (Chain)m.getChain(c);

		    chainSchematic(args, tm, chain);
		}
	    }

	    // generate exact normals for the schematic
	    // using the current ones as approximations.
	    tm.recalculateNormals();

	    //System.out.println("tm " + tm);
	}catch(Exception e){
	    e.printStackTrace();
	}

	return tm;
    }

    /** The points for the various objects. */
    private static Point3d guides[] = null;
    private static Point3d tangents[] = null;
    private static Point3d width[] = null;
    private static Point3d thick[] = null;
    private static Point3d tmp[] = null;
    private static Point3d t[] = null;
    private static Point3d tout[] = null;
    private static Point3d c[] = null;
    private static Point3d h[] = null;
    private static Point3d rv[] = null;
    private static int type[] = null;
    private static int colors[] = null;
    private static int resids[] = null;
    private static boolean widthInitialised[] = null;
    
    private static int guideCount = 0;
    private static int residCount = 0;

    private static void ensureCapacity(Chain chain){
	int resCount = chain.getResidueCount();
	if(guides == null ||
	   resCount > guides.length){
	    guides = new Point3d[resCount];
	    tangents = new Point3d[resCount];
	    width = new Point3d[resCount];
	    thick = new Point3d[resCount];
	    tmp = new Point3d[resCount];
	    c = new Point3d[resCount];
	    h = new Point3d[resCount];
	    t = new Point3d[resCount];
	    tout = new Point3d[resCount];
	    rv = new Point3d[resCount];
	    type = new int[resCount];
	    colors = new int[resCount];
	    resids = new int[resCount];
	    widthInitialised = new boolean[resCount];
	    //System.out.println("allocating space for " +
	    //	       resCount + " residues");

	    for(int i = 0; i < resCount; i++){
		guides[i] = new Point3d();
		tangents[i] = new Point3d();
		width[i] = new Point3d();
		thick[i] = new Point3d();
		tmp[i] = new Point3d();
		c[i] = new Point3d();
		t[i] = new Point3d();
		tout[i] = new Point3d();
		h[i] = new Point3d();
		rv[i] = new Point3d();
	    }
	}
    }

    /** Add schematic objects for this chain. */
    public static void chainSchematic(Arguments args, Tmesh tm , Chain chain){
	//System.out.println("#### chainSchematic");

	int minResidues = args.getInteger("-minchainresidues", 5);
	
	if(chain.getResidueCount() < minResidues) return;

	//System.out.println("before ensureCapacity");

	ensureCapacity(chain);

	//System.out.println("after ensureCapacity");

	tm.setColorStyle(Tmesh.TriangleColor);

	guideCount = 0;

	// store the calpha positions and secondary structure types
	for(int r = 0; r < chain.getResidueCount(); r++){
	    Residue res = (Residue)chain.getResidue(r);
	    Atom CA = res.getAtom("CA");
	    if(CA != null && CA.isTemporarilySelected()){
		
		// if there is C and O for this res
		// use it to define the width vector
		Atom C = res.getAtom("C");
		Atom O = res.getAtom("O");

		if(O != null && C != null){
		    width[guideCount] = Point3d.unitVector(C, O);
		    widthInitialised[guideCount] = true;
		}else{
		    widthInitialised[guideCount] = false;
		}
		colors[guideCount] = CA.getColor();
		guides[guideCount].set(CA);
		type[guideCount] = res.getSecondaryStructure();
		guideCount++;
	    }
	}

        // MLV 17/09/07 DNA/RNA schematics
	for(int r = 0; r < chain.getResidueCount(); r++){
	    Residue res = (Residue)chain.getResidue(r);
	    Atom C5 = res.getAtom("C5*");
	    if(C5 != null && C5.isTemporarilySelected()){
		widthInitialised[guideCount] = false;
		colors[guideCount] = C5.getColor();
		guides[guideCount].set(C5);
		type[guideCount] = res.Sheet;
		guideCount++;
	    }
	}

	//System.out.println("guideCount " + guideCount);

	if(guideCount < 2){
	    return;
	}

	// calculate the tangents
	for(int r = 0; r < guideCount; r++){
	    int before = r - 1;
	    int after = r + 1;

	    if(r == 0){
		before = r;
	    }else if(r == guideCount - 1){
		after = guideCount - 1;
	    }

	    tangents[r].zero();
	    tangents[r].subtract(guides[before]);
	    tangents[r].add(guides[after]);
	    tangents[r].normalise();
	}

	// calculate out of plane normals
	for(int r = 1; r < guideCount - 1; r++){
	    if(widthInitialised[r] == false){
		Point3d ab = Point3d.unitVector(guides[r-1], guides[r]);
		Point3d bc = Point3d.unitVector(guides[r], guides[r+1]);
		Point3d.cross(width[r], ab, bc);
	    }
	}

	// assign end points.
	if(guideCount > 2){
	    if(widthInitialised[0] == false){
		width[0].set(width[1]);
	    }
	    if(widthInitialised[guideCount - 1] == false){
		width[guideCount-1].set(width[guideCount-2]);
	    }
	}

	if(false){
	    // Correct direction swaps.
	    for(int r = 1; r < guideCount; r++){
		if(width[r].dot(width[r-1]) < 0.0){
		    width[r].negate();
		}
	    }
	}

	// now go through the chain and generate
	// the bits and pieces that make up the schematic

	boolean allTube = args.getBoolean("-alltube", false);

	if(allTube){
	    //System.out.println("alltube is true guideCount "+guideCount);
	    //System.out.println("resids length "+ resids.length);
	    residCount = 0;

	    for(int r = 0; r < guideCount; r++){
		
		resids[residCount] = r;
		residCount++;
	    }

	    //System.out.println("alltube is true residCount "+residCount);
	    tube(args, tm);
	}else{

	    residCount = 0;

	    // Sheet.
	    for(int r = 0; r < guideCount; r++){
		
		if(type[r] == Residue.Sheet){
		    resids[residCount] = r;
		    residCount++;
		}else{
		    if(residCount > 0){
			arrow(args, tm);
			residCount = 0;
		    }
		}
	    }
	    
	    if(residCount > 0){
		arrow(args, tm);
		residCount = 0;
	    }
	    
	    // Helix.
	    for(int r = 0; r < guideCount; r++){
		if(type[r] == Residue.Helix){
		    resids[residCount] = r;
		    residCount++;
		}else{
		    if(residCount > 0){
			ribbon(args, tm);
			residCount = 0;
		    }
		}
	    }
	    
	    if(residCount > 0){
		ribbon(args, tm);
		residCount = 0;
	    }
	    
	    // Coil.
	    for(int r = 0; r < guideCount; r++){
		if((r > 0 && type[r] != Residue.Coil &&
		    type[r - 1] == Residue.Coil) ||
		   (r < guideCount - 1 && type[r] != Residue.Coil
		    && type[r + 1] == Residue.Coil) ||
		   type[r] == Residue.Coil){
		    
		    if(residCount > 0 &&
		       guides[r].distance(guides[resids[residCount - 1]]) > 4.2){
			tube(args, tm);
			residCount = 0;
		    }
		    
		    resids[residCount] = r;
		    residCount++;
		    
		    if(r > 0 && type[r] != Residue.Coil &&
		       type[r - 1] == Residue.Coil){
			tube(args, tm);
			residCount = 0;
		    }
		    
		}else{
		    if(residCount > 0){
			tube(args, tm);
			residCount = 0;
		    }
		}
	    }

	    if(residCount > 0){
		tube(args, tm);
		residCount = 0;
	    }
	    
	    for(int r = 1; r < guideCount; r++){
		if((type[r] == Residue.Sheet && type[r-1] == Residue.Helix) ||
		   (type[r-1] == Residue.Sheet && type[r] == Residue.Helix)){
		    resids[0] = r - 1;
		    resids[1] = r;
		    residCount = 2;
		    tube(args, tm);
		    residCount = 0;
		}
	    }
	}
    }

    // offset vectors
    private static Point3d wptp = new Point3d();
    private static Point3d wptm = new Point3d();
    private static Point3d wmtp = new Point3d();
    private static Point3d wmtm = new Point3d();

    private static Point3d wm   = new Point3d();
    private static Point3d wp   = new Point3d();
    private static Point3d ep   = new Point3d();
    private static Point3d en   = new Point3d();

    private static Point3d wmlast   = new Point3d();
    private static Point3d wplast   = new Point3d();

    private static Point3d p     = new Point3d();
    private static Point3d wint  = new Point3d();
    private static Point3d tint  = new Point3d();
    private static Point3d tnext = new Point3d();

    private static Point3d v1 = new Point3d();
    private static Point3d v2 = new Point3d();
    
    private static int lastv[] = new int[8];
    private static int v[] = new int[8];
    // points on end of strip...
    private static int hv[] = new int[4];

    // rotation constants for helix vectors
    // (from molscript paper)
    private static final double COSA = 0.8480;
    private static final double COSB = 0.9816;
    private static final double SINA = 0.5299;
    private static final double SINB = 0.1908;

    /** Add ribbon to graphical object. */
    public static void ribbon(Arguments args, Tmesh tm){
	int quality           = args.getInteger("-quality", 1);
	double tangent_length = args.getDouble("-ribbontangent", 5.0);
	int splinePoints      = args.getInteger("-ribbonpoints", 8);
	double aWidth         = 0.5 * args.getDouble("-ribbonwidth", 2.2);
	double minWidth       = 0.5 * args.getDouble("-ribbonminwidth", 0.4);
	double aThick         = 0.5 * args.getDouble("-ribbonthickness", 0.15);
	boolean cylinders     = args.getBoolean("-ribboncylinders", false);
	boolean helixBack     = args.getBoolean("-helixback", false);
	boolean ellipse       = args.getBoolean("-ribbonellipse", false);
	int ellipsePoints     = args.getInteger("-ribbonellipsepoints", 12);
        boolean colorBySS     = args.getBoolean("-colorbyss", false);
        int color             = args.getColor("-ribboncolor", Color32.red);

	splinePoints *= quality;
	ellipsePoints *= quality;

	int helixColor = Color32.red;
	
	int rfirst = resids[0];
	int rlast = resids[residCount - 1];

	Point3d plast = new Point3d();
	Point3d p = new Point3d();
	Point3d pnext = new Point3d();

	int currentEllipse[] = null;
	int lastEllipse[] = null;
	double cosTheta[] = null;
	double sinTheta[] = null;

	if(ellipse){
	    currentEllipse = new int[ellipsePoints];
	    lastEllipse = new int[ellipsePoints];

	    // build the trig lookup table
	    sinTheta = new double[ellipsePoints];
	    cosTheta = new double[ellipsePoints];

	    double step = 2.*Math.PI/ellipsePoints;
	    double theta = 0.0;

	    for(int iep = 0; iep < ellipsePoints; iep++){
		// skew the points to the areas around the ellipse
		double component = 0.0;
		if(theta < Math.PI){
		    component = -Math.cos(theta) + 1.0;
		}else{
		    component =  Math.cos(theta) + 3.0;
		}
		// factor of two comes from the component already.
		double realTheta = 0.5 * component * Math.PI;
		sinTheta[iep] = Math.sin(realTheta);
		cosTheta[iep] = Math.cos(realTheta);
		theta += step;
	    }
	}

	boolean fudgeFirst = false;
	boolean fudgeLast  = false;

	for(int i = 0; i < residCount ; i++){
	    int r = resids[i];
	    
	    if(r == 0){
		fudgeFirst = true;
	    }else if(r == guideCount - 1){
		fudgeLast = true;
	    }else{
		Point3d ab = Point3d.unitVector(guides[r-1], guides[r]);
		Point3d bc = Point3d.unitVector(guides[r], guides[r+1]);
		Point3d.cross(width[r], ab, bc);
	    }
	}

	if(fudgeFirst){
	    width[rfirst].set(width[rfirst+1]);
	}
	if(fudgeLast){
	    width[rlast].set(width[rlast-1]);
	}

	// rotate vectors slightly as in molscript
	// to get correct shape.
	for(int i = 0; i < residCount; i++){
	    int r = resids[i];
	    
	    for(int j = 0; j < 3; j++){
		t[r].set(j, COSB * tangents[r].get(j) - SINB * width[r].get(j));
		h[r].set(j, COSA * width[r].get(j) + SINA * tangents[r].get(j));
	    }

	    t[r].normalise();
	    h[r].normalise();
	    Point3d.cross(tout[r], t[r], h[r]);
	}

	boolean first = true;
	boolean firstVertices = true;
	int backColor = Color32.cyan;

	for(int i = 0; i < residCount - 1; i++){
	    int r = resids[i];
	    double w = aWidth;
	    double at = aThick;

	    int nsp = splinePoints;

	    if(i == residCount - 2){
		nsp = splinePoints + 1;
	    }

	    for(int sp = 0; sp < nsp; sp++){
		double tp = (double)sp/(double)splinePoints;
		double tnext = (double)(sp+1)/(double)splinePoints;

		// new color interpolation scheme...
		// blame joe
                if(!colorBySS){
                    color = Color32.blend(colors[r], colors[r+1], 1. - tp);
                }

		if(i == 0){
		    w = minWidth + tp * (aWidth - minWidth);
		    at = minWidth + tp * ( aThick - minWidth);
		}else if(i == residCount - 2){
		    w = minWidth + (1. - tp) * (aWidth - minWidth);
		    at = minWidth + (1. - tp) * ( aThick - minWidth);
		}

		interpolate(wint, h[r], h[r+1], tp);

		hermite_single(guides[r], guides[r+1],
			       t[r], tangent_length,
			       t[r+1], tangent_length,
			       tp, p);

		hermite_single(guides[r], guides[r+1],
			       t[r], tangent_length,
			       t[r+1], tangent_length,
			       tnext, pnext);

		interpolate(tint, tout[r], tout[r+1], tp);

		if(ellipse){
		    for(int iep = 0; iep < ellipsePoints; iep++){
			double ct = cosTheta[iep];
			double st = sinTheta[iep];

			for(int c = 0; c < 3; c++){
			    ep.set(c, p.get(c) + ct * wint.get(c) * w + st * tint.get(c) * at);
			    // normal to ellipse, just swap components
			    en.set(c, ct * wint.get(c) * at + st * tint.get(c) * w);
			}

			en.normalise();

			currentEllipse[iep] = tm.addPoint(ep.x, ep.y, ep.z, en.x, en.y, en.z, 0.0, 0.0);
			//tm.addSphere(ep.x, ep.y, ep.z, 0.02, Color32.yellow);
		    }

		    if(!first){
			// connect up the triangles
			for(int iep = 0; iep < ellipsePoints; iep++){
			    int inp = iep + 1;
			    if(inp == ellipsePoints){
				inp = 0;
			    }

			    tm.addTriangle(currentEllipse[iep], lastEllipse[iep], currentEllipse[inp],
					   color);
			    tm.addTriangle(lastEllipse[iep], currentEllipse[inp], lastEllipse[inp],
					   color);
			}
		    }

		    for(int iep = 0; iep < ellipsePoints; iep++){
			lastEllipse[iep] = currentEllipse[iep];
		    }

		}else{

		    // calculate perimeter of arrow cross section
		    for(int c = 0; c < 3; c++){
			wptp.set(c, p.get(c) + wint.get(c) * w + tint.get(c) * aThick);
			wptm.set(c, p.get(c) + wint.get(c) * w - tint.get(c) * aThick);
			wmtp.set(c, p.get(c) - wint.get(c) * w + tint.get(c) * aThick);
			wmtm.set(c, p.get(c) - wint.get(c) * w - tint.get(c) * aThick);

			wp.set(c, p.get(c) + wint.get(c) * w);
			wm.set(c, p.get(c) - wint.get(c) * w);
		    }
		    
		    // add points for perimeter
		    // each point twice with different normal
		    v[0] = tm.addPoint(wptm.x, wptm.y, wptm.z, -tint.x, -tint.y, -tint.z, 0.0, 0.0);
		    v[1] = tm.addPoint(wmtm.x, wmtm.y, wmtm.z, -tint.x, -tint.y, -tint.z, 0.0, 0.0);
		    v[2] = tm.addPoint(wmtp.x, wmtp.y, wmtp.z, tint.x, tint.y, tint.z, 0.0, 0.0);
		    v[3] = tm.addPoint(wptp.x, wptp.y, wptp.z, tint.x, tint.y, tint.z, 0.0, 0.0);
		    
		    if(!cylinders){
			v[4] = tm.addPoint(wmtm.x, wmtm.y, wmtm.z, -wint.x, -wint.y, -wint.z, 0.0, 0.0);
			v[5] = tm.addPoint(wmtp.x, wmtp.y, wmtp.z, -wint.x, -wint.y, -wint.z, 0.0, 0.0);
			v[6] = tm.addPoint(wptp.x, wptp.y, wptp.z, wint.x, wint.y, wint.z, 0.0, 0.0);
			v[7] = tm.addPoint(wptm.x, wptm.y, wptm.z, wint.x, wint.y, wint.z, 0.0, 0.0);
		    }

		    if(!firstVertices){
			int end = 2;
			if(!cylinders){
			    end = 4;
			}
			for(int k = 0; k < end; k++){
			    int tcolor = color;
			    if(k == 0 && helixBack){
				tcolor = Color32.gray;
				//tcolor = backColor;
			    }else{
				tcolor = color;
			    }
			//if(k == 1 || k == 3){
			    tm.addTriangle(v[0+2*k], v[1+2*k], lastv[0+2*k], tcolor);
			    tm.addTriangle(v[1+2*k], lastv[0+2*k], lastv[1+2*k], tcolor);
			    //if(!cylinders){
			    //tm.addTriangle(v[0+k], v[1+k], lastv[0+k], color);
			    //tm.addTriangle(v[1+k], lastv[0+k], lastv[1+k], color);
			    //}
			}
			
			double cylRadius = aThick * 1.5;

			if(cylinders){
			    tm.addCylinder(wm.x, wm.y, wm.z,
					   wmlast.x, wmlast.y, wmlast.z,
					   cylRadius, color, color);
			    tm.addCylinder(wp.x, wp.y, wp.z,
					   wplast.x, wplast.y, wplast.z,
					   cylRadius, color, color);
			}
		    }
		
		    // remember points on last perimeter
		    for(int j = 0; j < 8; j++){
			lastv[j] = v[j];
		    }
		}

		wmlast.set(wm);
		wplast.set(wp);
		
		firstVertices = false;
		//}
		
		plast.set(p);
		
		first = false;
	    }

	}	

    }

    private static Point3d spline[] = null;
    private static int splineColor[] = null;

    private static void ensureSplineCapacity(int n){
	if(spline == null || spline.length < n){
	    spline = new Point3d[n];
	    splineColor = new int[n];
	}

	for(int i = 0; i < n; i++){
	    spline[i] = new Point3d();
	}
    }

    /** Add a tube to the object. */
    public static void tube(Arguments args, Tmesh tm){
	int quality      = args.getInteger("-quality", 1);
	int smooth       = args.getInteger("-tubesmoothing", 1);
	int splinePoints = args.getInteger("-tubepoints", 4);
	int perimPoints  = args.getInteger("-tubeperimeter", 8);
	double radius    = args.getDouble("-tuberadius", 0.2);
	double rTaper    = args.getDouble("-tubetaperradius", 0.1);
	double tangent   = args.getDouble("-tubetangent", 2.);
	boolean taper    = args.getBoolean("-tubetaper", false);
        boolean colorBySS   = args.getBoolean("-colorbyss", false);
        int color             = args.getColor("-tubecolor", Color32.white);

	splinePoints *= quality;
	perimPoints *= quality;

	//System.out.println("hello from tube "+residCount);

	ensureSplineCapacity(residCount * splinePoints);

	int rfirst = resids[0];
	int rlast = resids[residCount - 1];

	DoubleArray radii = null;

	// apply guide point smoothing.
	for(int iteration = 0; iteration < smooth; iteration++){
	    for(int i = 1; i < residCount - 1; i++){
		int r = resids[i];
		tmp[r].set(guides[r-1]);
		tmp[r].add(guides[r+1]);
		tmp[r].scale(0.5);
		tmp[r].add(guides[r]);
		tmp[r].scale(0.5);
	    }

	    for(int i = 1; i < residCount - 1; i++){
		int r = resids[i];
		guides[r].set(tmp[r]);
	    }
	}

	// calculate the tangents
	for(int r = 1; r < guideCount-1; r++){
	    int before = r - 1;
	    int after = r + 1;

	    if(r == 0){
		before = r;
	    }else if(r == guideCount - 1){
		after = guideCount - 1;
	    }

	    tangents[r].zero();
	    tangents[r].subtract(guides[before]);
	    tangents[r].add(guides[after]);
	    tangents[r].normalise();
	}

	boolean fudgeFirst = false;
	boolean fudgeLast  = false;

	for(int i = 0; i < residCount ; i++){
	    int r = resids[i];
	    
	    if(r == 0){
		fudgeFirst = true;
	    }else if(r == guideCount - 1){
		fudgeLast = true;
	    }else{
		Point3d ab = Point3d.unitVector(guides[r-1], guides[r]);
		Point3d bc = Point3d.unitVector(guides[r], guides[r+1]);
		Point3d.cross(width[r], ab, bc);
	    }
	}

	if(residCount == 2){
	    Point3d.normalToLine(guides[rfirst], width[rfirst]);
	    Point3d.normalToLine(guides[rlast], width[rlast]);
	}else{
	    if(fudgeFirst){
		width[rfirst].set(width[rfirst+1]);
	    }
	    if(fudgeLast){
		width[rlast].set(width[rlast-1]);
	    }
	}

	for(int i = 1; i < residCount; i++){
	    int r = resids[i];
	    if(width[r].dot(width[r-1]) < 0.0){
		width[r].negate();
	    }
	}

	for(int i = 0; i < residCount; i++){
	    int r = resids[i];

	    Point3d.cross(thick[r], tangents[r], width[r]);	    
	}	    

	for(int i = 1; i < residCount; i++){
	    int r = resids[i];
	    if(thick[r].dot(thick[r-1]) < 0.0){
		thick[r].negate();
	    }
	}

	int nsp = 0;

	boolean first = true;

	Point3d pos = new Point3d();

	int pnew[] = new int[perimPoints];
	int plast[] = new int[perimPoints];

	if(taper){
	    radii = new DoubleArray();
	}

	//System.out.println("building trig");

	// build the trig lookup table
	double sinTheta[] = new double[perimPoints];
	double cosTheta[] = new double[perimPoints];

	for(int iep = 0; iep < perimPoints; iep++){
	    double theta = 2.* Math.PI*(double)iep/(double)perimPoints;

	    sinTheta[iep] = Math.sin(theta);
	    cosTheta[iep] = Math.cos(theta);
	}

	for(int i = 0; i < residCount - 1; i++){
	    int r = resids[i];
	    int sp1 = splinePoints;

	    if(i == residCount - 2){
		sp1 = splinePoints + 1;
	    }

	    for(int sp = 0; sp < sp1; sp++){
		double t = (double)sp/(double)(splinePoints);

		// if we aren't smoothing force
		// spline points to lie near curved sections.
		if(smooth == 0){
		    if(t < 0.5){
			t = t * t;
		    }else{
			t = 1.0 - t;
			t = t * t;

			t = 1.0 - t;
		    }
		}

		if(taper){
		    double rr = radius;
		    if(i == 0){
			if(t < 0.5){
			    rr = rTaper + 2.*t * (radius - rTaper);
			}
		    }else if(i == residCount - 2){
			if(t > 0.5){
			    rr = rTaper + 2.*(1.0 - t) * (radius - rTaper);
			}
		    }

		    radii.add(rr);
		}

		// new color interpolation scheme...
		// blame joe
                if(!colorBySS){
                    color = Color32.blend(colors[r], colors[r+1], 1. - t);
                }

		splineColor[nsp] = color;

		hermite_single(guides[r], guides[r+1],
			       tangents[r], tangent,
			       tangents[r+1], tangent,
			       t, spline[nsp]);

		nsp++;
	    }
	}

	//System.out.println("building points");

	Point3d alast = null;
	Point3d blast = null;

	for(int isp = 0; isp < nsp; isp++){
	    Point3d p   = spline[isp];
	    Point3d a   = null;
	    Point3d b   = null;

	    if(alast == null){
		if(isp == 0){
		    Point3d dir = Point3d.unitVector(spline[0], spline[1]);
		    a = Point3d.normalToLine(dir);
		    b = a.cross(dir);
		}else{
		    Point3d ab = Point3d.unitVector(spline[isp-1], spline[isp]);
		    Point3d bc = Point3d.unitVector(spline[isp], spline[isp+1]);
		    Point3d dir = Point3d.unitVector(spline[isp-1], spline[isp+1]);
		    a = ab.cross(bc);
		    b = a.cross(dir);
		}
	    }else{
		Point3d dir = null;

		if(isp == nsp - 1){
		    dir = Point3d.unitVector(spline[isp-1], spline[isp]);
		}else{
		    dir = Point3d.unitVector(spline[isp-1], spline[isp+1]);
		}

		b = dir.cross(alast);
		a = b.cross(dir);

		if(a.dot(alast) < 0.0) a.negate();
		if(b.dot(blast) < 0.0) b.negate();
	    }

	    double rr = radius;

	    if(taper){
		rr = radii.get(isp);
	    }

	    color = splineColor[isp];
	    
	    for(int ip = 0; ip < perimPoints; ip++){
		//double theta = 2. * Math.PI * (double)ip/(double)perimPoints;
		//double costheta = Math.cos(theta);
		//double sintheta = Math.sin(theta);
		//double theta = 2. * Math.PI * (double)ip/(double)perimPoints;
		double costheta = cosTheta[ip];
		double sintheta = sinTheta[ip];

		for(int j = 0; j < 3; j++){
		    pos.set(j, a.get(j) * costheta + b.get(j) * sintheta);
		}
		
		pnew[ip] = tm.addPoint(p.x + pos.x * rr,
				       p.y + pos.y * rr,
				       p.z + pos.z * rr,
				       pos.x, pos.y, pos.z,
				       0.0, 0.0);
	    }
	    
	    if(!first){
		for(int ip = 0; ip < perimPoints; ip++){
		    int pnext = (ip + 1) % perimPoints;
		    tm.addTriangle(pnew[ip], plast[ip], pnew[pnext], color);
		    tm.addTriangle(plast[ip], pnew[pnext], plast[pnext], color);
		}
		
	    }else{
		double ptmp[] = new double[3];
		double ntmp[] = new double[3];
		int face[] = new int[perimPoints];
		for(int ip = 0; ip < perimPoints; ip++){
		    tm.getVertex(pnew[ip], ptmp, ntmp);
		    face[ip] = tm.addPoint(ptmp[0], ptmp[1], ptmp[2],
					   -tangents[rfirst].x,
					   -tangents[rfirst].y,
					   -tangents[rfirst].z,
					   0.0, 0.0);
		}
		
		for(int ip = 2; ip < perimPoints; ip++){
		    tm.addTriangle(face[0], face[ip-1], face[ip], color);
		}
	    }
	    
	    
	    for(int ip = 0; ip < perimPoints; ip++){
		plast[ip] = pnew[ip];
	    }
	    
	    first = false;
	    alast = a;
	    blast = b;
	}

	//System.out.println("blocking faces");

	// block off the last face.
	double ptmp[] = new double[3];
	double ntmp[] = new double[3];
	int face[] = new int[perimPoints];
	for(int p = 0; p < perimPoints; p++){
	    tm.getVertex(pnew[p], ptmp, ntmp);
	    face[p] = tm.addPoint(ptmp[0], ptmp[1], ptmp[2],
				  tangents[rlast].x,
				  tangents[rlast].y,
				  tangents[rlast].z,
				  0.0, 0.0);
	}
	
	for(int p = 2; p < perimPoints; p++){
	    tm.addTriangle(face[0], face[p-1], face[p], color);
	}

	//System.out.println("pointCount " + tm.np);
	
	/*
	for(int i = 0; i < nsp - 1; i++){
	    tm.addCylinder(spline[i].x, spline[i].y, spline[i].z,
			   spline[i+1].x, spline[i+1].y, spline[i+1].z,
			   radius, splineColor[i], splineColor[i]);
	}
	*/
    }

    /** Add an arrow to the object. */
    public static void arrow(Arguments args, Tmesh tm){
	int quality       =       args.getInteger("-quality", 1);
	int smooth        =       args.getInteger("-arrowsmoothing", 3);
	int splinePoints  =       args.getInteger("-arrowpoints", 4);
	double aHeadWidth = 0.5 * args.getDouble("-arrowheadwidth", 3.6);
	double tangent    =       args.getDouble("-arrowtangent", 2.0);
	double aWidth     = 0.5 * args.getDouble("-arrowwidth", 2.2);
	double aThick     = 0.5 * args.getDouble("-arrowthickness", 0.5);
        boolean colorBySS =       args.getBoolean("-colorbyss", false);
        int color         =       args.getColor("-arrowcolor", Color32.yellow);

	splinePoints *= quality;

	if(residCount <= 1) return;
	
	if(false){
	    for(int i = 0; i < residCount-1; i++){

		Point3d p1 = guides[resids[i]];
		Point3d p2 = guides[resids[i+1]];
		tm.addCylinder(p1.x, p1.y, p1.z,
			       p2.x, p2.y, p2.z,
			       0.2, Color32.white, Color32.white);
	    }
	}

	/*
	for(int i = 1; i < residCount - 1; i++){
	    int r = resids[i];
	    Point3d ab = Point3d.unitVector(guides[r-1], guides[r]);
	    Point3d bc = Point3d.unitVector(guides[r], guides[r+1]);
	    Point3d dir = Point3d.unitVector(guides[r-1], guides[r+1]);
	    Point3d bob = new Point3d();
	    Point3d.cross(bob, width[r], dir);
	    Point3d.cross(width[r], bob, dir);
	}

	int rfirst = resids[0];
	int rlast  = resids[residCount-1];

	width[rfirst].set(width[rfirst+1]);
	width[rlast-2].set(width[rlast-1]);
	*/

	// correct direction swaps
	for(int i = 1; i < residCount; i++){
	    int r = resids[i];

	    if(width[r].dot(width[r-1]) < 0.0){
		width[r].negate();
	    }
	}

	// smooth out of plane normals

	//for(int iteration = 0; iteration < smooth; iteration++){
	for(int iteration = 0; iteration < 1; iteration++){
	    for(int i = 0; i < residCount; i++){
		int r = resids[i];
		tmp[r].set(width[r]);
		if(i != 0){
		    tmp[r].add(width[r-1]);
		}else if(i < residCount - 1){
		    tmp[r].add(width[r+1]);
		}
		tmp[r].normalise();
	    }

	    // and reinstall them...
	    for(int i = 0; i < residCount; i++){
		int r = resids[i];
		width[r].set(tmp[r]);
	    }
	}

	// apply guide point smoothing.
	for(int iteration = 0; iteration < smooth; iteration++){
	    for(int i = 1; i < residCount - 1; i++){
		int r = resids[i];

		tmp[r].set(guides[r-1]);
		tmp[r].add(guides[r+1]);
		tmp[r].scale(0.5);
		tmp[r].add(guides[r]);
		tmp[r].scale(0.5);
	    }

	    for(int i = 1; i < residCount - 1; i++){
		int r = resids[i];
		guides[r].set(tmp[r]);
	    }
	}

	Point3d ref = new Point3d();

	// Generate in CA-CA-CA plane normal
	for(int i = 0; i < residCount; i++){
	    int r = resids[i];
	    int before = r - 1;
	    int after = r + 1;
	    if(i == 0){
		before = r;
	    }else if(i == residCount - 1){
		after = r;
	    }
	    Point3d ab = Point3d.unitVector(guides[before], guides[after]);
	    Point3d.cross(thick[r], ab, width[r]);

	    // recalculate the tangent vectors to reflect smoothing.
	    tangents[r].set(ab);


	    // try and reorthogonalise the width vectors
	    Point3d.cross(ref, width[r], ab);
	    Point3d.cross(width[r], ref, ab);
	    width[r].normalise();

	    Point3d.cross(thick[r], ab, width[r]);
	}

	// Correct direction swaps.
	for(int i = 1; i < residCount; i++){
	    int r = resids[i];
	    if(thick[r].dot(thick[r-1]) < 0.0){
		thick[r].negate();
	    }
	}

	if(residCount > 2){
	    int last = resids[residCount - 1];
	    int prevLast = resids[residCount - 2];
	    width[last].set(width[prevLast]);
	    thick[last].set(thick[prevLast]);
	}

	boolean first = true;

	for(int i = 0; i < residCount - 1; i++){
	    int r = resids[i];
	    double w = aWidth;

	    for(int sp = 0; sp < splinePoints; sp++){
		double t = 0.0;
		if(i == residCount - 3){
		    if(sp == splinePoints - 1){
			t = 0.9;
		    }else{
			t = (double)sp/(double)(splinePoints - 1);
		    }
		}else if(i == residCount - 2){
		    t = (double)sp/(double)(splinePoints - 1);
		}else{
		    t = (double)sp/(double)(splinePoints);
		}

		/*
		if(t <= 0.5){
		    color = colors[r];
		}else{
		    color = colors[r+1];
		}
		*/

		// new color interpolation scheme...
		// blame joe
                if(!colorBySS){
                    color = Color32.blend(colors[r], colors[r+1], 1. - t);
                }

		hermite_single(guides[r], guides[r+1],
			       tangents[r], tangent,
			       tangents[r+1], tangent,
			       t, p);

		// on arrow tip
		// scale width according to distance along vector.
		// as hermite interpolation is not uniform
		if(i == residCount - 2){
		    w = p.distance(guides[r+1])/guides[r+1].distance(guides[r]);
		    w *= aHeadWidth;
		}

		interpolate(wint, width[r], width[r+1], t);
		interpolate(tint, thick[r], thick[r+1], t);

		// calculate perimeter of arrow cross section
		for(int c = 0; c < 3; c++){
		    wptp.set(c, p.get(c) + wint.get(c) * w + tint.get(c) * aThick);
		    wptm.set(c, p.get(c) + wint.get(c) * w - tint.get(c) * aThick);
		    wmtp.set(c, p.get(c) - wint.get(c) * w + tint.get(c) * aThick);
		    wmtm.set(c, p.get(c) - wint.get(c) * w - tint.get(c) * aThick);
		}

		// block off start of arrow
		if(first){
		    double nx = -tangents[r].x;
		    double ny = -tangents[r].y;
		    double nz = -tangents[r].z;
		    v[0] = tm.addPoint(wptp.x, wptp.y, wptp.z, nx, ny, nz, 0.0, 0.0);
		    v[1] = tm.addPoint(wptm.x, wptm.y, wptm.z, nx, ny, nz, 0.0, 0.0);
		    v[2] = tm.addPoint(wmtp.x, wmtp.y, wmtp.z, nx, ny, nz, 0.0, 0.0);
		    v[3] = tm.addPoint(wmtm.x, wmtm.y, wmtm.z, nx, ny, nz, 0.0, 0.0);
		    tm.addTriangle(v[0], v[1], v[2], color);
		    tm.addTriangle(v[1], v[2], v[3], color);
		}

		// back of arrow...
		if(i == residCount - 2 && sp == 0){
		    double nx = -tangents[r].x;
		    double ny = -tangents[r].y;
		    double nz = -tangents[r].z;
		    v[0] = tm.addPoint(wptp.x, wptp.y, wptp.z, nx, ny, nz, 0.0, 0.0);
		    v[1] = tm.addPoint(wptm.x, wptm.y, wptm.z, nx, ny, nz, 0.0, 0.0);
		    v[2] = tm.addPoint(wmtp.x, wmtp.y, wmtp.z, nx, ny, nz, 0.0, 0.0);
		    v[3] = tm.addPoint(wmtm.x, wmtm.y, wmtm.z, nx, ny, nz, 0.0, 0.0);

		    if(residCount > 2){
			// one part of back facing tip
			tm.addTriangle(v[0], v[1], hv[1], color);
			tm.addTriangle(v[0], hv[0], hv[1], color);
			// second part of back facing tip
			tm.addTriangle(v[2], v[3], hv[3], color);
			tm.addTriangle(v[2], hv[2], hv[3], color);
		    }else{
			tm.addTriangle(v[0], v[1], v[2], color);
			tm.addTriangle(v[0], v[2], v[3], color);
		    }
		}

		// last perimeter of main part of arrow...
		if(i == residCount - 3 && sp == splinePoints - 1){
		    double nx = -tangents[r].x;
		    double ny = -tangents[r].y;
		    double nz = -tangents[r].z;

		    hv[0] = tm.addPoint(wptp.x, wptp.y, wptp.z, nx, ny, nz, 0.0, 0.0);
		    hv[1] = tm.addPoint(wptm.x, wptm.y, wptm.z, nx, ny, nz, 0.0, 0.0);
		    hv[2] = tm.addPoint(wmtp.x, wmtp.y, wmtp.z, nx, ny, nz, 0.0, 0.0);
		    hv[3] = tm.addPoint(wmtm.x, wmtm.y, wmtm.z, nx, ny, nz, 0.0, 0.0);
		    // triangles are added on first
		    // perimeter of arrow tip.
		}

		// add points for perimeter
		// each point twice with different normal
		v[0] = tm.addPoint(wptp.x, wptp.y, wptp.z, wint.x, wint.y, wint.z, 0.0, 0.0);
		v[1] = tm.addPoint(wptm.x, wptm.y, wptm.z, wint.x, wint.y, wint.z, 0.0, 0.0);

		v[2] = tm.addPoint(wptm.x, wptm.y, wptm.z, -tint.x, -tint.y, -tint.z, 0.0, 0.0);
		v[3] = tm.addPoint(wmtm.x, wmtm.y, wmtm.z, -tint.x, -tint.y, -tint.z, 0.0, 0.0);

		v[4] = tm.addPoint(wmtm.x, wmtm.y, wmtm.z, -wint.x, -wint.y, -wint.z, 0.0, 0.0);
		v[5] = tm.addPoint(wmtp.x, wmtp.y, wmtp.z, -wint.x, -wint.y, -wint.z, 0.0, 0.0);

		v[6] = tm.addPoint(wmtp.x, wmtp.y, wmtp.z, tint.x, tint.y, tint.z, 0.0, 0.0);
		v[7] = tm.addPoint(wptp.x, wptp.y, wptp.z, tint.x, tint.y, tint.z, 0.0, 0.0);

		if(!first){
		    // add triangles from this perimeter to last
		    if(i == residCount - 2 && sp == 0){
			for(int k = 1; k < 4; k+=2){
			    tm.addTriangle(v[0+2*k], v[1+2*k], lastv[0+2*k], color);
			    tm.addTriangle(v[1+2*k], lastv[0+2*k], lastv[1+2*k], color);
			}
		    }else{
			for(int k = 0; k < 4; k++){
			    tm.addTriangle(v[0+2*k], v[1+2*k], lastv[0+2*k], color);
			    tm.addTriangle(v[1+2*k], lastv[0+2*k], lastv[1+2*k], color);
			}
		    }
		}
		
		// remember points on last perimeter
		for(int j = 0; j < 8; j++){
		    lastv[j] = v[j];
		}
		
		first = false;
	    }
	}
    }

    /** Linear interoplation between p1 and p2. */
    public static void interpolate(Point3d s,
				   Point3d p1, Point3d p2,
				   double t){
	s.x = p1.x + t * (p2.x - p1.x);
	s.y = p1.y + t * (p2.y - p1.y);
	s.z = p1.z + t * (p2.z - p1.z);
	s.normalise();
    }

    /** Interpolate hermite spline. */
    public static void hermite_single(Point3d P1, Point3d P2,
				      Point3d T1, double T1len,
				      Point3d T2, double T2len,
				      double s, Point3d p){
	double h1 =  2*s*s*s - 3*s*s + 1;          // calculate basis function 1
	double h2 = -2*s*s*s + 3*s*s;              // calculate basis function 2
	double h3 =    s*s*s - 2*s*s + s;          // calculate basis function 3
	double h4 =    s*s*s -   s*s;              // calculate basis function 4

	h3 *= T1len;
	h4 *= T2len;

	p.x = h1*P1.x + h2*P2.x+ h3*T1.x + h4*T2.x;
	p.y = h1*P1.y + h2*P2.y+ h3*T1.y + h4*T2.y;
	p.z = h1*P1.z + h2*P2.z+ h3*T1.z + h4*T2.z;
    }
}
