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

package astex.design;

import astex.*;
import java.util.*;

public class ActiveSite {
    /**
     * Handle an active site command.
     */
    public static void handleCommand(MoleculeViewer mv, Arguments args){
	DynamicArray superstar  = (DynamicArray)args.get("-superstar");
	DynamicArray lipophiles = (DynamicArray)args.get("-lipophile");
	DynamicArray asp        = (DynamicArray)args.get("-asp");
	DynamicArray spheres    = (DynamicArray)args.get("-spheres");
	DynamicArray pass       = (DynamicArray)args.get("-pass");

	exclusion  = (DynamicArray)args.get("-exclusion");

	//long initialMemory = Runtime.getRuntime().totalMemory();

	setupExclusion();

	if(superstar != null){
	    generateSuperstarMap(mv, args, superstar);
	}else if(asp != null){
	    generateAspMap(mv, args, asp);
	}else if(lipophiles != null){
	    //generateLipophileMap(mv, args, superstar);
	}else if(spheres != null){
	    generateTangentSpheres(mv, args, spheres);
	}else if(pass != null){
	    Molecule mol = PASS.generatePASS(args, pass);
	    mv.addMolecule(mol);
	}

	System.gc();

	//long finalMemory = Runtime.getRuntime().totalMemory();

	//Log.info("memory used %10d", (finalMemory - initialMemory));
    }

    private static void setupExclusion(){
	if(exclusion != null){
	    // get the exclusion atoms
	    // create the lattice object
	    lattice = new Lattice(4.1);

	    // and put them in it
	    int exclusionCount = exclusion.size();

	    for(int i = 0; i < exclusionCount; i++){
		Atom atom = (Atom)exclusion.get(i);
		lattice.add(i, atom.x, atom.y, atom.z);
	    }
	}
    }

    private static Lattice lattice = null;
    private static DynamicArray exclusion = null;

    /** Arbitrary size for radius table. */
    private static final int MaxElements   = 200;

    /** The atomic number based radius table. */
    private static double vdwRadii[]       = new double[MaxElements];

    /** Various constants used in the superstar procedure. */
    private static double dtol             = 0.4;
    private static double rOffset          = 0.5;
    private static double rmsdWarningLevel = 0.1;
    private static double rmsdFailLevel    = 0.5;

    /** The hash of probe molecule names for this superstar map. */
    private static Hashtable probeMolecules     = null;

    /** Set up the radii from the superstar property file. */
    private static void setupRadii(){
	for(int r = 0; r < MaxElements; r++){
	    double rvdw = Settings.getDouble("superstar", "radius." + r, 1.4);
	    vdwRadii[r] = rvdw;
	    //System.out.println("look for " + radiusEntry);
	    //System.out.println("got " + vdwRadii[r]);
	}

	rOffset = Settings.getDouble("superstar", "radius.offset");

	System.out.println("radiusOffset is " + rOffset);

	rmsdWarningLevel = Settings.getDouble("superstar", "rmsd.warning");
	
	rmsdFailLevel = Settings.getDouble("superstar", "rmsd.fail");

	Log.info("rmsd fail level is %.1f", rmsdFailLevel);

	dtol = Settings.getDouble("superstar", "radius.dtol");

	Log.info("dtol is %.1f", dtol);
    }
    
    private static Hashtable mappings = null;

    /**
     * Generate a simplified superstar map.
     */
    public static void generateSuperstarMap(MoleculeViewer mv,
					    Arguments args,
					    DynamicArray superstar){
	// The prefix for molecule names
	String molNamePrefix = args.getString("-prefix", "superstar");
	String type          = args.getString("-type", null);
        MoleculeRenderer mr  = mv.getMoleculeRenderer();

	if(molNamePrefix == null){
	    molNamePrefix = args.getString("-superstarprefix", "superstar");
	}

	// if we have no type we must return
	if(type == null){
	    Log.error("no map type defined");
	    return;
	}

	// make sure we have the superstar properties
	// before we do anything else.
	setupRadii();

	// hash for the istr molecules
	probeMolecules = new Hashtable();
	
	// clear the visit flags on the superstar atoms.
	int atomCount = superstar.size();

	for(int a = 0; a < atomCount; a++){
	    Atom atom = (Atom)superstar.get(a);
	    atom.setTemporarilySelected(false);
	}

	// generate the map name
	String mapName = molNamePrefix + "_" + type;

	// create a map and set it up
	astex.Map map = mr.getMap(mapName);
	boolean newMap = false;

	if(map == null){
	    Log.info("creating new map " + mapName);
	    map = astex.Map.createSimpleMap();
	    newMap = true;
	}

	map.setName(mapName);
	map.setFile(mapName);


	// initialise the map
	initialiseMap(mv, args, mapName, map, superstar);

	scatterPlotCount = 0;

	for(int groups = 0; groups < 1000; groups++){
	    String groupLabel = "group." + groups;

	    String groupName = Settings.getString("superstar", groupLabel);

	    //Log.info("returned groupName " + groupName);

	    if(groupName != null){
		String groupTypeLabel = groupName + ".type";
		String groupType = Settings.getString("superstar",
						      groupTypeLabel);

		if(type.equals(groupType)){
		    Log.info(groupName);

		    processSuperstarGroup(mv, args, superstar,
					  groupName, molNamePrefix, map);
		}
	    }
	}

	finaliseMap(mv, args, mapName, map);

        if(newMap){
            mr.addMap(map);
        }

	probeMolecules = null;
	exclusion      = null;
	mappings       = null;
	lattice        = null;
    }

    /** The space in which we contribute the small map. */
    private static float scatterPlot[] = null;

    //private static final double gridBorder = 5.0;

    /** Set up the map. */
    private static astex.Map initialiseMap(MoleculeViewer mv,
					   Arguments args, 
					   String mapName,
                                           astex.Map map,
					   DynamicArray superstarAtoms){
	MoleculeRenderer mr = mv.getMoleculeRenderer();
	int atomCount       = superstarAtoms.size();

	if(atomCount == 0){
	    // no atoms so do nothing.
	    return null;
	}

        // set of atoms over which field is defined
        DynamicArray boxAtoms = (DynamicArray)args.get("-boxatoms");

        if(boxAtoms == null || boxAtoms.size() == 0){
            boxAtoms = superstarAtoms;
        }

	// spacing for the map
	double spacing    = args.getDouble("-mapspacing", 0.5);
	double gridBorder = args.getDouble("-border", 5.0);
	double volume     = spacing * spacing * spacing;

	double xmin =  1.e10, ymin =  1.e10, zmin =  1.e10;
	double xmax = -1.e10, ymax = -1.e10, zmax = -1.e10;
	
	int contourColour = Color32.white;

        int boxAtomCount = boxAtoms.size();

	for(int a = 0; a < boxAtomCount; a++){
	    Atom atom = (Atom)boxAtoms.get(a);

	    if(a == 0){
		int element = atom.getElement();
		if(element == PeriodicTable.NITROGEN){
		    contourColour = Color32.blue;
		}else if(element == PeriodicTable.OXYGEN){
		    contourColour = Color32.red;
		}else if(element == PeriodicTable.CARBON){
		    contourColour = Color32.gray;
		}
	    }
	    if(atom.x > xmax) xmax = atom.x;
	    if(atom.x < xmin) xmin = atom.x;
	    if(atom.y > ymax) ymax = atom.y;
	    if(atom.y < ymin) ymin = atom.y;
	    if(atom.z > zmax) zmax = atom.z;
	    if(atom.z < zmin) zmin = atom.z;
	}

	xmin -= gridBorder;
	ymin -= gridBorder;
	zmin -= gridBorder;
	xmax += gridBorder;
	ymax += gridBorder;
	zmax += gridBorder;

	map.origin.x = xmin;
	map.origin.y = ymin;
	map.origin.z = zmin;

	map.spacing.x = spacing;
	map.spacing.y = spacing;
	map.spacing.z = spacing;

	map.ngrid[0] = 1 + (int)(0.5 + (xmax - xmin)/spacing);
	map.ngrid[1] = 1 + (int)(0.5 + (ymax - ymin)/spacing);
	map.ngrid[2] = 1 + (int)(0.5 + (zmax - zmin)/spacing);

	//System.out.println("grid size " + map.ngrid[0] +
	//		   " " + map.ngrid[1] + " " + map.ngrid[2]);
	
	int gridPoints = map.ngrid[0] * map.ngrid[1] * map.ngrid[2];

	map.data = new float[gridPoints];

	scatterPlot = new float[gridPoints];

	// the map will accumulate probabilities and
	// so needs setting to 1.0 initially
	for(int i = 0; i < gridPoints; i++){
	    map.data[i] = 1.0f;
	}

	//initialiseNonCentralGroupValues(map, mol);

	return map;
    }

    /** Set up the contour levels etc. */
    private static void finaliseMap(MoleculeViewer mv,
				    Arguments args,
				    String mapName,
				    astex.Map map){
	MoleculeRenderer mr = mv.getMoleculeRenderer();
	int gridPoints      = map.ngrid[0] * map.ngrid[1] * map.ngrid[2];

	double fmin    =  1.e10;
	double fmax    = -1.e10;
	double logfmin =  1.e10;
	double logfmax = -1.e10;

	int oneCount = 0;

	for(int i = 0; i < gridPoints; i++){
	    double v = map.data[i];

	    if(v < fmin) fmin = v;
	    if(v > fmax) fmax = v;

	    // clear out the remaining 1.0's
	    if(map.data[i] == 1.0f){
		oneCount++;
		map.data[i] = 0.0f;
	    }else if(map.data[i] != 0.0f){
		map.data[i] = (float)Math.log(map.data[i]);

		if(map.data[i] < logfmin){
		    logfmin = map.data[i];
		}

		if(map.data[i] > logfmax){
		    logfmax = map.data[i];
		}
	    }

	    //map.data[i] *= 0.1f;

	}

	// now set zero to the minimum log
	// value (as Math.ln(0.0f) = inf
	for(int i = 0; i < gridPoints; i++){
	    if(map.data[i] == 0.0f){
		map.data[i] = (float)logfmin;
	    }
	}

	//Log.info("grid points %5d", gridPoints);
	//Log.info("set to 1.0  %5d", oneCount);

	Log.info("minimum value    %8.2f", fmin);
	Log.info("maximum value    %8.2f", fmax);

	Log.info("minimum log value %8.3f", logfmin);
	Log.info("maximum log value %8.3f", logfmax);

	double startLevel = 4.5;

	String colors[] = {
	    "red",
	    "orange",
	    "yellow",
	};

	if(mapName.indexOf("donor") != -1){
	    startLevel = 2.5;
	    colors[0] = "blue";
	    colors[1] = "0x5ad2ff";
	    colors[2] = "cyan";
	}else if(mapName.indexOf("ali") != -1){
	    startLevel = 4.5;
	    colors[0] = "green";
	    colors[1] = "0x69ff69";
	    colors[2] = "0xc3ffc3";
	}else if(mapName.indexOf("aro") != -1){
	    startLevel = 4.5;
	    colors[0] = "brown";
	    colors[1] = "0xb41e00";
	    colors[2] = "0xd24b00";
	}

	String command = null;
	String commandPrefix = null;

	for(int i = 0; i < map.MaximumContourLevels; i++){
            map.setContourLevel(i, startLevel + i);
            map.setContourDisplayed(i, true);
            map.setContourStyle(i, astex.Map.Lines);
            map.setContourColor(i, Color32.getColorFromName(colors[i]));
            /*
              commandPrefix = "map " + mapName + " contour " + i + " ";
              command = commandPrefix + FILE.sprint("%.2f;", startLevel + i);
              mr.executeInternal(command);
              command = commandPrefix + "on;";
              mr.executeInternal(command);
              command = commandPrefix + "wire;";
              mr.executeInternal(command);
              command = commandPrefix + colors[i] + ";";
              mr.executeInternal(command);
            */
	}
    }


    /** Calculate grid boundary. */
    private static void gridBoundary(astex.Map map,
				     Point3d p, double r,
				     int gmin[], int gmax[]){
	int xp = (int)(0.5 + (p.x - map.origin.x)/map.spacing.x);
	int yp = (int)(0.5 + (p.y - map.origin.y)/map.spacing.y);
	int zp = (int)(0.5 + (p.z - map.origin.z)/map.spacing.z);
	
	int gx = 3 + (int)(r / map.spacing.x);
	int gy = 3 + (int)(r / map.spacing.y);
	int gz = 3 + (int)(r / map.spacing.z);

	gmin[0] = xp - gx;
	gmin[1] = yp - gy;
	gmin[2] = zp - gz;

	gmax[0] = xp + gx;
	gmax[1] = yp + gy;
	gmax[2] = zp + gz;

	for(int i = 0; i < 3; i++){
	    if(gmin[i] < 0)             gmin[i] = 0;
	    if(gmax[i] >= map.ngrid[i]) gmax[i] = map.ngrid[i];
	}
    }

    /** Calculate coordinates of grid point. */
    private static void gridPoint(astex.Map map,
				  int i, int j, int k,
				  Point3d p){
	p.x = map.origin.x + i * map.spacing.x;
	p.y = map.origin.y + j * map.spacing.y;
	p.z = map.origin.z + k * map.spacing.z;
    }

    /** Calculate the grid index for the grid point. */
    private static int gridIndex(astex.Map map, int i, int j, int k){
	return i + j * map.ngrid[0] + k * map.ngrid[0] * map.ngrid[1];
    }


    /** Clear the values within non central group atoms to zero. */
    private static void initialiseNonCentralGroupValues(astex.Map map,
							Molecule mol){
	int gmin[] = new int[3];
	int gmax[] = new int[3];

	int atomCount = mol.getAtomCount();
	Point3d gp    = new Point3d();
	int zeroCount = 0;
	for(int a = 0; a < atomCount; a++){
	    Atom atom = mol.getAtom(a);

	    if(atom.isTemporarilySelected() == false){
		// it was never in a central group.
		double r = vdwRadii[atom.getElement()] - dtol;
		double r2 = r * r;
		gridBoundary(map, atom, r, gmin, gmax);

		for(int i = gmin[0]; i < gmax[0]; i++){
		    for(int j = gmin[1]; j < gmax[1]; j++){
			for(int k = gmin[2]; k < gmax[2]; k++){
			    gridPoint(map, i, j, k, gp);

			    if(atom.distanceSq(gp) < r2){
				int index = gridIndex(map, i, j, k);
				map.data[index] = 0.0f;
				zeroCount++;
			    }
			}
		    }
		}
	    }
	}

	//Log.info("zeroCount %d", zeroCount);
    }

    /** Prepare the current scatter plot mask. */
    private static void prepareScatterPlotRegion(astex.Map map,
						 float scatterPlot[],
						 double probeRadius,
						 DynamicArray centralAtoms,
						 int gmin[], int gmax[]){
	// grid min, max for each atom
	int gamin[] = new int[3];
	int gamax[] = new int[3];

	for(int i = 0; i < 3; i++){
	    gmin[i] = Integer.MAX_VALUE;
	    gmax[i] = 0;
	}

	if(centralAtoms == null){
	    Log.error("centralAtoms was null");
	    return;
	}

	//double rp = vdwRadii[atom.getElement()];
	double rp = probeRadius;
	int centralAtomCount = centralAtoms.size();

	//Log.info("centralAtomCount %d", centralAtomCount);

	for(int c = 0; c < centralAtomCount; c++){
	    Atom catom = (Atom)centralAtoms.get(c);
	    double rc = vdwRadii[catom.getElement()];
	    double r = rp + rc + rOffset;

	    //Log.info("total radius %4.2f", r);

	    gridBoundary(map, catom, r, gamin, gamax);

	    for(int i = 0; i < 3; i++){
		if(gamin[i] < gmin[i]) gmin[i] = gamin[i];
		if(gamax[i] > gmax[i]) gmax[i] = gamax[i];
	    }
	}

	Point3d gp = new Point3d();

	for(int i = gmin[0]; i < gmax[0]; i++){
	    for(int j = gmin[1]; j < gmax[1]; j++){
		for(int k = gmin[2]; k < gmax[2]; k++){
		    int index = gridIndex(map, i, j, k);

		    scatterPlot[index] = 1.0f;

		    gridPoint(map, i, j, k, gp);

		    for(int c = 0; c < centralAtomCount; c++){
			Atom catom = (Atom)centralAtoms.get(c);
			double rc = vdwRadii[catom.getElement()];
			//double r = rp + rc + rOffset;
			double r = rc - dtol;
			
			if(gp.distanceSq(catom) < r*r){
			    scatterPlot[index] = 0.0f;
			    break;
			}
		    }
		}
	    }
	}

	//System.out.println("gmin " + gmin[0] + " " + gmin[1] + " " + gmin[2]);
	//System.out.println("gmax " + gmax[0] + " " + gmax[1] + " " + gmax[2]);
    }

    /** Multiply in the partial scatter plot. */
    private static void multiplyScatterPlot(astex.Map map,
					    float scatterPlot[],
					    int gmin[], int gmax[]){
	double spmin =  1.e10;
	double spmax = -1.e10;
	double mmin =  1.e10;
	double mmax = -1.e10;
	
	if(false){
	    for(int i = gmin[0]; i < gmax[0]; i++){
		for(int j = gmin[1]; j < gmax[1]; j++){
		    for(int k = gmin[2]; k < gmax[2]; k++){
			int index = gridIndex(map, i, j, k);
			double v = scatterPlot[index];
			
			FILE.out.print("%4.1f ", v);
		    }
		    FILE.out.print("\n");
		}
		FILE.out.print("\n");
		FILE.out.print("\n");
	    }
	}

	for(int i = gmin[0]; i < gmax[0]; i++){
	    for(int j = gmin[1]; j < gmax[1]; j++){
		for(int k = gmin[2]; k < gmax[2]; k++){
		    int index = gridIndex(map, i, j, k);
		    double v = scatterPlot[index];

		    if(v < spmin) spmin = v;
		    if(v > spmax) spmax = v;

		    map.data[index] *= v;

		    if(map.data[index] > mmax) mmax = map.data[index];
		    if(map.data[index] < mmin) mmin = map.data[index];
		}
	    }
	}

	//Log.info("scatter plot min %6.2f", spmin);
	//Log.info("scatter plot max %6.2f", spmax);

	//Log.info("map region   min %6.2f", mmin);
	//Log.info("map region   max %6.2f", mmax);
    }

    /** The number of grid points. */
    private static int gridOffset = 2;

    private static int gmin[] = new int[3];
    private static int gmax[] = new int[3];

    private static int    included[] = new int[1000];
    private static double contrib[]  = new double[1000];

    /** Trim the atoms that are in the superstar molecule. */
    private static void mapSuperstarMolecule(MoleculeViewer mv,
					     Arguments args,
					     DynamicArray centralAtoms,
					     DynamicArray scatterPlotAtoms,
					     double probeRadius,
					     astex.Map map){
	int ninc = 0;

	double expConst = 2.*0.5*0.5;

	// form volume
	double volume = map.spacing.x * map.spacing.y * map.spacing.z;

	prepareScatterPlotRegion(map, scatterPlot,
				 probeRadius, centralAtoms,
				 gmin, gmax);
	
	int atomCount = scatterPlotAtoms.size();

	Point3d pp = new Point3d();

	for(int a = 0; a < atomCount; a++){
	    Atom atom = (Atom)scatterPlotAtoms.get(a);
	    double d = 1.0 / (atom.getBFactor() * volume);

	    //Log.info("contribution %f", d);

	    int xp = (int)(0.5 + (atom.x - map.origin.x)/map.spacing.x);
	    int yp = (int)(0.5 + (atom.y - map.origin.y)/map.spacing.y);
	    int zp = (int)(0.5 + (atom.z - map.origin.z)/map.spacing.z);

	    if(xp < gmin[0] || xp >= gmax[0] ||
	       yp < gmin[1] || yp >= gmax[1] ||
	       zp < gmin[2] || zp >= gmax[2]){
		//System.out.println("probe out of grid...");
		//System.out.println("xp " + xp + " yp " + yp + " zp " + zp);
                continue;
	    }

	    int slot = gridIndex(map, xp, yp, zp);

	    if(true){
		int bxmin = Math.max(xp - gridOffset, 0);
		int bxmax = Math.min(xp + gridOffset, map.ngrid[0]);
		int bymin = Math.max(yp - gridOffset, 0);
		int bymax = Math.min(yp + gridOffset, map.ngrid[1]);
		int bzmin = Math.max(zp - gridOffset, 0);
		int bzmax = Math.min(zp + gridOffset, map.ngrid[2]);

		ninc = 0;

		for(int i = bxmin; i < bxmax; i++){
		    for(int j = bymin; j < bymax; j++){
			for(int k = bzmin; k < bzmax; k++){
			    gridPoint(map, i, j, k, pp);
			    double r2 = pp.distanceSq(atom);
			    if(r2 < 1.0){
				int index = gridIndex(map, i, j, k);
				double v = Math.exp(-r2/expConst);
				//FILE.out.print("v %8.3f\n", v);
				included[ninc] = index;
				contrib[ninc] = v;
				ninc++;
			    }
			}
		    }
		}

		// calculate normalisation factor
		double norm = 0.0;

		for(int i = 0; i < ninc; i++){
		    norm += contrib[i] * contrib[i];
		}

		norm = Math.sqrt(norm);

		for(int i = 0; i < ninc; i++){
		    int index = included[i];
		    //if(scatterPlot[index] != 1.0f){
			scatterPlot[index] += d * contrib[i] / norm;
			//}
		}
	    }else{
		//Log.info("contribution %5.3f", d);
		
		//map.data[slot] += d;
		scatterPlot[slot] += d;
	    }
	}
	
	multiplyScatterPlot(map, scatterPlot, gmin, gmax);
    }

    /** Handle a superstar group definition. */
    private static void processSuperstarGroup(MoleculeViewer mv,
					      Arguments args,
					      DynamicArray superstar,
					      String groupName,
					      String molPrefix,
					      astex.Map map){
	MoleculeRenderer mr = mv.getMoleculeRenderer();
	String typeLabel  = groupName + ".type";
	String istr       = groupName + ".istr";
	String scaleLabel = groupName + ".scale";

	String istrName   = Settings.getString("superstar", istr);
	String typeString = Settings.getString("superstar", typeLabel);

	boolean keepScatterPlots = args.getBoolean("-scatterplots", false);

	if(typeString == null){
	    Log.error("no type for group " + groupName);
	    Log.error("fitting abandoned");
	    return;
	}

	// generate the molecule name from prefix and group type
	String moleculeName = molPrefix + "_" + typeString;

	Molecule superstarMol = null;

	if(keepScatterPlots){
	    if(superstarMol == null){
		// remove any old ones of that name...
		mr.removeMoleculeByName(moleculeName);
		
		superstarMol = new Molecule();
		superstarMol.setName(moleculeName);
		superstarMol.setMoleculeType(Molecule.SkeletonMolecule);
		mv.addMolecule(superstarMol);
		Log.info("creating " + moleculeName);
	    }
	}
	
	// look for the istr molecule 
	Molecule istrMol = (Molecule)probeMolecules.get(istrName);

	if(istrMol == null){
	    istrMol = MoleculeIO.read(istrName);
	    if(istrMol == null){
		Log.error("couldn't load " + istrName);
		return;
	    }

	    //Log.info("loading " + istrName);

	    boolean keepCarbons = false;

	    if(typeString.indexOf("lipo") != -1){
		keepCarbons = true;
	    }

	    markUnwantedAtoms(istrMol, keepCarbons);

	    probeMolecules.put(istrName, istrMol);
	}

	double plotScale = 1.0;

	String scaleString = Settings.getString("superstar", scaleLabel);

	if(scaleString != null){
	    plotScale = FILE.readDouble(scaleString);
	    Log.info("plotScale %5.2f", plotScale);
	}

	StringArray pdbMap = new StringArray();
	StringArray istrMap = new StringArray();
	DynamicArray pdbAtoms = new DynamicArray();
	DynamicArray istrAtoms = new DynamicArray();

	// look for the mappings
	for(int i = 0; i < 1000; i++){
	    String mapString = groupName + "." + i;

	    String namePairString = Settings.getString("superstar", mapString);

	    // absence of the next name pair indicates
	    // end of name pairs
	    if(namePairString == null){
		break;
	    }

	    String namePairs[] = FILE.split(namePairString, ",");

	    pdbMap.add(namePairs[0]);
	    istrMap.add(namePairs[1]);
	}

	for(int i = 0; i < istrMap.size(); i++){
	    String idLabel = istrMap.get(i);
	    int id = FILE.readInteger(idLabel);

	    Atom a = istrMol.getAtomWithId(id);

	    istrAtoms.add(a);
	}

	int mapCount = pdbMap.size();

	if(mapCount == 0){
	    Log.error("no mappings for " + groupName);
	    return;
	}

	String atomSelection[] = FILE.split(pdbMap.get(0), ".");

	if(atomSelection[0].equals("*")){
	    atomSelection[0] = null;
	}

	int atomCount = superstar.size();

	for(int a = 0; a < atomCount; a++){
	    Atom atom = (Atom)superstar.get(a);
	    Residue residue = atom.getResidue();

	    if(atom.getAtomLabel().equals(atomSelection[1]) &&
	       (atomSelection[0] == null ||
		residue.getName().equals(atomSelection[0]))){
		pdbAtoms.removeAllElements();

		pdbAtoms.add(atom);

		for(int match = 1; match < pdbMap.size(); match++){
		    String s = pdbMap.get(match);
		    Atom matchAtom = residue.findAtom(s);
		    
		    if(matchAtom == null){
			System.out.println("couldn't find match atom " + s +
					   " for " + residue);
			break;
		    }else{
			pdbAtoms.add(matchAtom);
			s = istrMap.get(match);
		    }
		}

		if(pdbAtoms.size() == pdbMap.size()){
		    fitSuperstarGroup(mv, args, pdbAtoms, istrAtoms,
				      superstarMol, istrMol, plotScale, map);
		}
	    }
	}
    }

    /** Mark those atoms we don't want in a molecule. */
    private static void markUnwantedAtoms(Molecule mol, boolean keepCarbons){
	int centralAtomCount = mol.getCentralAtomCount();
	int istrCount        = mol.getAtomCount();

	int kept = 0;

	// skip the central group atoms
	for(int i = centralAtomCount; i < istrCount; i++){
	    Atom a = mol.getAtom(i);
	    int elementa = a.getElement();

	    a.setOccupancy(-1.0);

	    if((keepCarbons || elementa != PeriodicTable.CARBON) &&
	       elementa != PeriodicTable.HYDROGEN){
		double rc = vdwRadii[elementa];

		for(int j = 0; j < centralAtomCount; j++){
		    Atom catom = mol.getAtom(j);
		    int celement = catom.getElement();
		    double rr = vdwRadii[celement];
		    double rcheck = rc + rr + rOffset;

		    if(a.distanceSq(catom) < rcheck*rcheck){
			// record that this was an active atom
			a.setOccupancy(1.0);
			kept++;
			break;
		    }
		}
	    }
	}
    }

    private static int scatterPlotCount = 0;

    private static DoubleArray x = new DoubleArray();
    private static DoubleArray y = new DoubleArray();
    private static DoubleArray z = new DoubleArray();
    private static DoubleArray xp = new DoubleArray();
    private static DoubleArray yp = new DoubleArray();
    private static DoubleArray zp = new DoubleArray();

    private static IntArray neighbours  = new IntArray();

    private static DynamicArray atomCache = new DynamicArray();

    private static void fitSuperstarGroup(MoleculeViewer mv,
					  Arguments args,
					  DynamicArray pdbAtoms,
					  DynamicArray istrAtoms,
					  Molecule superstarMol,
					  Molecule istrMol,
					  double plotScale,
					  astex.Map map){

	//Integer scatterHash = null;
	//DynamicArray scatterAtoms = null;

	DynamicArray scatterPlotAtoms = new DynamicArray();

	int nfit = pdbAtoms.size();

	x.removeAllElements();
	y.removeAllElements();
	z.removeAllElements();
	xp.removeAllElements();
	yp.removeAllElements();
	zp.removeAllElements();

	for(int i = 0; i < nfit; i++){

	    //System.out.println("atom " + i);
	    Atom a = (Atom)pdbAtoms.get(i);
	    //System.out.println("pdbAtom " + a);
	    x.add(a.x);
	    y.add(a.y);
	    z.add(a.z);

	    // mark the pdb atom as having been in a central group
	    a.setTemporarilySelected(true);

	    a = (Atom)istrAtoms.get(i);
	    //System.out.println("istrAtom " + a);
	    xp.add(a.x);
	    yp.add(a.y);
	    zp.add(a.z);

	}

	Matrix rot = new Matrix();

	double rmsd = astex.Fit.fit(x.getArray(),
				    y.getArray(),
				    z.getArray(),
				    xp.getArray(),
				    yp.getArray(),
				    zp.getArray(), nfit, rot);

	if(rmsd > rmsdWarningLevel){
	    Atom baseAtom = (Atom)pdbAtoms.get(0);
	    Residue res = baseAtom.getResidue();

	    Log.warn("residue " + res + " rmsd=%5.2f", rmsd);

	    if(rmsd > rmsdFailLevel){
		Log.error("fitting abandoned");
		return;
	    }
	}

	Point3d p            = new Point3d();
	int centralAtomCount = istrMol.getCentralAtomCount();
	int istrCount        = istrMol.getAtomCount();
	boolean addit        = true;
	Atom cacheHit        = null;

	int cacheHits        = 0;

	int latticeCache     = 0;
	int latticeMiss      = 0;

	int boxx             = Integer.MIN_VALUE;
	int boxy             = Integer.MIN_VALUE;
	int boxz             = Integer.MIN_VALUE;

	double probeRadius   = 0.0;

	// skip the central group atoms
	for(int i = centralAtomCount; i < istrCount; i++){
	    Atom a = istrMol.getAtom(i);
	    int elementa = a.getElement();
	    double ra    = vdwRadii[elementa];
	    probeRadius  = ra;

	    // occupancy > 0.0 indicates it is an
	    // atom we want to proceed with
	    if(a.getOccupancy() > 0.0){
		p.set(a);
		
		rot.transform(p);
		
		addit = true;
		
		// if there were exclusion atoms
		// check them for collisions
		if(exclusion != null){
		    if(cacheHit != null){
			int elementc = cacheHit.getElement();
			double rc    = vdwRadii[elementc];
			double rvdw  = (ra + rc) - 2.0 * dtol;

			if(cacheHit.distanceSq(p) < rvdw*rvdw){
			    addit = false;
			    cacheHits++;
			}else{
			    cacheHit = null;
			}
		    }

		    if(addit == true){
			// check for whether we are looking
			// for neighbours in the same cell
			// as before.
			int pboxx = lattice.BOX(p.x);
			int pboxy = lattice.BOX(p.y);
			int pboxz = lattice.BOX(p.z);

			if(pboxx != boxx ||
			   pboxy != boxy ||
			   pboxz != boxz){
			    neighbours.removeAllElements();
			    lattice.getPossibleNeighbours(-1, p.x, p.y, p.z,
							  neighbours, true);
			    boxx = pboxx;
			    boxy = pboxy;
			    boxz = pboxz;
			    latticeCache++;
			}else{
			    latticeMiss++;
			}

			int ncount = neighbours.size();
		    
			for(int n = 0; n < ncount; n++){
			    int id             = neighbours.get(n);
			    Atom atomNeighbour = (Atom)exclusion.get(id);
			    int elementn       = atomNeighbour.getElement();
			    double rn          = vdwRadii[elementn];
			    // allow closer approach to allow for hbonding
			    double rvdw        = (ra + rn) - 2.0 * dtol;
			    boolean ignore     = false;
			
			    if(p.distanceSq(atomNeighbour) < rvdw*rvdw){
				// but we shouldn't allow collisions
				// with the actual fit atoms
				// of the central group itself
				for(int j = 0; j < nfit; j++){
				    Atom pdbAtom = (Atom)pdbAtoms.get(j);
				    if(pdbAtom == atomNeighbour){
					ignore = true;
					break;
				    }
				}
			    
				if(!ignore){
				    addit = false;
				    cacheHit = atomNeighbour;
				    break;
				}
			    }
			}
		    }
		}

		// atom survived clipping by neighbours
		// so add it to the scatter plot
		if(addit){
		    /*
		    if(scatterHash == null){
			scatterHash = new Integer(scatterPlotCount);
			scatterAtoms = new DynamicArray();
			mappings.put(scatterHash, scatterAtoms);
			for(int ma = 0; ma < nfit; ma++){
			    scatterAtoms.add(pdbAtoms.get(ma));
			}
		    }
		    */

		    // this should only be non-null if
		    // we are keeping scatterplots
		    if(superstarMol != null){
			//System.out.println("adding atom");
			Atom newAtom = superstarMol.addAtom();
			newAtom.setElement(a.getElement());
			newAtom.set(p);
			// multiply in the plot scale
			// at this point
			newAtom.setBFactor(a.getBFactor() / plotScale);
			newAtom.setCharge(scatterPlotCount);
		    }

		    // hmm, duplicate atom creation here
		    Atom atom = Atom.create();
		    atom.setElement(a.getElement());
		    atom.set(p);
		    atom.setBFactor(a.getBFactor() / plotScale);
		    atom.setCharge(scatterPlotCount);

		    scatterPlotAtoms.add(atom);
		}
	    }
	}

	//Log.info("cacheHits    %5d", cacheHits);
	//Log.info("latticeCache %5d", latticeCache);
	//Log.info("latticeMiss  %5d", latticeMiss);

	//Log.info("atoms in scatterplot %d", scatterPlotAtoms.size());

	mapSuperstarMolecule(mv, args, pdbAtoms,
			     scatterPlotAtoms, probeRadius, map);

	// push the atoms from the scatter plot back
	// into the central atom cache
	int scatterPlotCount = scatterPlotAtoms.size();

	for(int i = 0; i < scatterPlotCount; i++){
	    Atom a = (Atom)scatterPlotAtoms.get(i);
	    a.release();
	}

	scatterPlotAtoms = null;

	scatterPlotCount++;
    }

    private static double xs[][] = {{0.0, 0.0, 0.0, 0.0},
				    {0.0, 0.0, 0.0, 0.0},
				    {0.0, 0.0, 0.0, 0.0},
    };
	
    private static double rs[] = {0.0, 0.0, 0.0, 0.0};

    private static final double xe[] = new double[4];

    private static final Atom atomQuad[] = new Atom[4];

    /**
     * Generate all spheres tangent to 4 spheres in the atom list.
     * The exclusion list is used to remove clashing spheres.
     */
    public static void generateTangentSpheres(MoleculeViewer mv,
					      Arguments args,
					      DynamicArray atoms){
	double max              = args.getDouble("-maxradius", 3.0);
	double min              = args.getDouble("-minradius", 1.5);
	double minAcceptedAngle = args.getDouble("-minangle", 1.5);
	String molName          = args.getString("-molecule", "spheres");

	int atomCount = atoms.size();
	int quadCount = 0;
	int sphereCount = 0;
	Point3d p = new Point3d();
		
	Molecule sphereMol = new Molecule();
	sphereMol.setName(molName);
	sphereMol.setMoleculeType(Molecule.SkeletonMolecule);
	mv.addMolecule(sphereMol);
	Log.info("creating molecule " + molName);

	double maxAngle = 0.0;

	for(int a0 = 0; a0 < atomCount; a0++){
	    Atom atom0 = (Atom)atoms.get(a0);
	    xs[0][0] = atom0.x; xs[1][0] = atom0.y; xs[2][0] = atom0.z;
	    rs[0] = atom0.getVDWRadius();
	    atomQuad[0] = atom0;

	    for(int a1 = a0+1; a1 < atomCount; a1++){
		Atom atom1 = (Atom)atoms.get(a1);
		xs[0][1] = atom1.x; xs[1][1] = atom1.y; xs[2][1] = atom1.z;
		rs[1] = atom1.getVDWRadius();
		atomQuad[1] = atom1;

		double max01 = max + rs[0] + rs[1];

		if(atom0.distanceSq(atom1) < max01*max01){
		    for(int a2 = a1+1; a2 < atomCount; a2++){
			Atom atom2 = (Atom)atoms.get(a2);
			xs[0][2] = atom2.x;
			xs[1][2] = atom2.y;
			xs[2][2] = atom2.z;
			rs[2] = atom2.getVDWRadius();
			atomQuad[2] = atom2;

			double max02 = max + rs[0] + rs[2];
			double max12 = max + rs[1] + rs[2];

			if(atom0.distanceSq(atom2) < (max02*max02) &&
			   atom1.distanceSq(atom2) < (max12*max12)){
			    for(int a3 = a2+1; a3 < atomCount; a3++){
				Atom atom3 = (Atom)atoms.get(a3);
				rs[3] = atom3.getVDWRadius();
				atomQuad[3] = atom3;

				double max03 = max + rs[0] + rs[3];
				double max13 = max + rs[1] + rs[3];
				double max23 = max + rs[2] + rs[3];

				if(atom0.distanceSq(atom3) < (max03*max03) &&
				   atom1.distanceSq(atom3) < (max13*max13) &&
				   atom2.distanceSq(atom3) < (max23*max23)){
				    xs[0][3] = atom3.x;
				    xs[1][3] = atom3.y;
				    xs[2][3] = atom3.z;

				    boolean success = Apollo.tangentSphere(xs, rs, xe);

				    if(success && xe[3] < max && xe[3] > min){
					
					neighbours.removeAllElements();
					lattice.getPossibleNeighbours(-1, xe[0], xe[1], xe[2],
								      neighbours, true);
					
					p.set(xe[0], xe[1], xe[2]);
					
					boolean addit = true;
					
					int ncount = neighbours.size();
					
					for(int n = 0; n < ncount; n++){
					    int id = neighbours.get(n);
					    Atom aa = (Atom)exclusion.get(id);
					    // skip the atoms that define the sphere.
					    if(aa == atom0 || aa == atom1 ||
					       aa == atom2 || aa == atom3){
						continue;
					    }else{
						double r = xe[3] + aa.getVDWRadius();
						if(p.distanceSq(aa) < r*r){
						    addit = false;
						    break;
						}
					    }
					}
					
					if(addit){
					    // check the angles
					    maxAngle = 0.0;

					    for(int s0 = 0; s0 < 4; s0++){
						for(int s1 = s0+1; s1 < 4; s1++){
						    double angle = Point3d.angle(atomQuad[s0],
										 p,
										 atomQuad[s1]);
						    angle = angle * 180.0/Math.PI;
						    if(angle > maxAngle){
							maxAngle = angle;
						    }
						}
					    }

					    if(maxAngle > minAcceptedAngle){
						Atom newAtom = sphereMol.addAtom();
						newAtom.set(p);
						newAtom.setVDWRadius(xe[3]);
						newAtom.setElement(PeriodicTable.UNKNOWN);
						sphereCount++;
					    }
					}
				    }
				    
				    quadCount++;
				}
			    }
			}
		    }
		}
	    }
	}

	Log.info("quadCount   %d", quadCount);
	Log.info("sphereCount %d", sphereCount);
    }

    /** Generate an Astex Statistical Potential map. */
    public static void generateAspMap(MoleculeViewer mv,
				      Arguments args,
				      DynamicArray aspAtoms){
	String aspPrefix = args.getString("-prefix", "asp");
	String probeName = args.getString("-type", "C.3");
	double maxd      = args.getDouble("-maxdistance", 6.0);
	MoleculeRenderer mr = mv.getMoleculeRenderer();

	// initialise the map
	String mapName = aspPrefix + "_" + probeName;
	
	// find where the maps are stored
	String location = Settings.getString("asp", "location");

	if(location == null){
	    Log.error("no location setting for asp pmf's");
	    return;
	}

	Log.info("location "+ location);

	// create a map and set it up
	astex.Map map = mr.getMap(mapName);
	boolean newMap = false;

	if(map == null){
	    Log.info("creating new map " + mapName);
	    map = astex.Map.createSimpleMap();
	    newMap = true;
	}

	map.setName(mapName);
	map.setFile(mapName);


	initialiseMap(mv, args, mapName, map, aspAtoms);

	// build the lattice datastructure
	// but actually put the exclusion set into it
	int exclusionCount = exclusion.size();

	Lattice aspLattice = new Lattice(6.0);

	for(int i = 0; i < exclusionCount; i++){
	    Atom aspAtom = (Atom)exclusion.get(i);
	    aspLattice.add(i, aspAtom.x, aspAtom.y, aspAtom.z);
	}

	// build the type information
	StringArray types = new StringArray();
	Hashtable pmfs = new Hashtable();

	for(int i = 0; i < exclusionCount; i++){
	    Atom atom = (Atom)exclusion.get(i);
	    Residue res = atom.getResidue();
	    String description = res.getName() + "." + atom.getAtomLabel();
	    //Log.info("description " + description);

	    String type = Settings.getString("asp", description);

	    if(type == null){
		//Log.warn("no type for " + description);

		description = "*." + atom.getAtomLabel();
		//Log.warn("checking " + description);

		type = Settings.getString("asp", description);

		if(type == null){
		    Log.warn("no type for " + description);
		    Log.warn("residue " + res.getName());
		    Log.warn("defaulting to C.3");
		    type = "C.3";
		}
	    }

	    String pmf = type + "_" + probeName;

	    if(pmfs.get(pmf) == null){
		loadPmf(pmfs, location, pmf);
	    }

	    types.add(pmf);
	}

	// now go through each atom, and sum the field
	for(int i = 0; i < exclusionCount; i++){
	    Atom atom = (Atom)exclusion.get(i);
	    incorporatePotential(map, i, atom, types, pmfs, maxd);
	}
	
	int gridPoints = map.ngrid[0] * map.ngrid[1] * map.ngrid[2];

	double min =  1.e10;
	double max = -1.e10;

	for(int i = 0; i < gridPoints; i++){
	    map.data[i] = -map.data[i];
	    if(map.data[i] > max) max = map.data[i];
	    if(map.data[i] < min) min = map.data[i];
	}

	Log.info("map min %f", min);
	Log.info("map max %f", max);

	min = (double)Math.rint(min);

	Log.info("round min %f", min);


	String command = null;
	String commandPrefix = null;
	double startLevel = max - 3.0;

	String colors[] = new String[3];

	colors[0] = "green";
	colors[1] = "'0x69ff69'";
	colors[2] = "'0xc3ffc3'";

	for(int i = 0; i < map.MaximumContourLevels; i++){
            map.setContourLevel(i, startLevel - 2*i);
            map.setContourDisplayed(i, true);
            map.setContourStyle(i, astex.Map.Lines);
            map.setContourColor(i, Color32.getColorFromName(colors[i]));
	    /*
              commandPrefix = "map " + mapName + " contour " + i + " ";
              command = commandPrefix + FILE.sprint("%.2f;", startLevel - 2*i);
              mr.executeInternal(command);
              command = commandPrefix + "on;";
              mr.executeInternal(command);
              command = commandPrefix + "wire;";
              mr.executeInternal(command);
              command = commandPrefix + colors[i] + ";";
              mr.executeInternal(command);
            */
	}

        if(newMap){
            mr.addMap(map);
        }
    }

    private static Point3d p = new Point3d();

    /** Add in the potential of an atom. */
    public static void incorporatePotential(astex.Map map, int iatom,
					    Atom atom, StringArray types,
					    Hashtable pmfs, double maxd){
	String type = types.get(iatom);
	DoubleArray pmf = (DoubleArray)pmfs.get(type);

	if(pmf == null){
	    Log.error("couldn't find pmf for " + type);
	    return;
	}

	double maxd2 = maxd * maxd;

	int nx = map.ngrid[0];
	int ny = map.ngrid[1];
	int nz = map.ngrid[2];

	double ax = atom.x - map.origin.x;
	double ay = atom.y - map.origin.y;
	double az = atom.z - map.origin.z;

	int gxmin =     (int)((ax - maxd)/map.spacing.x);
	int gymin =     (int)((ay - maxd)/map.spacing.y);
	int gzmin =     (int)((az - maxd)/map.spacing.z);

	if(gxmin >= nx || gymin >= ny || gzmin >= nz) return;

	if(gxmin < 0) gxmin = 0;
	if(gymin < 0) gymin = 0;
	if(gzmin < 0) gzmin = 0;

	int gxmax = 1 + (int)((ax + maxd)/map.spacing.x);
	int gymax = 1 + (int)((ay + maxd)/map.spacing.y);
	int gzmax = 1 + (int)((az + maxd)/map.spacing.z);

	if(gxmax < 0 || gymax < 0 || gzmax < 0) return;

	if(gxmax >= nx) gxmax = nx - 1;
	if(gymax >= ny) gymax = ny - 1;
	if(gzmax >= nz) gzmax = nz - 1;

	Point3d gp = new Point3d();
	int pmfSize = pmf.size();
	double pmfData[] = pmf.getArray();

	for(int iz = gzmin; iz <= gzmax; iz++){
	    gp.z = map.origin.z + iz * map.spacing.z;
	    for(int iy = gymin; iy <= gymax; iy++){
		gp.y = map.origin.y + iy * map.spacing.y;
		for(int ix = gxmin; ix <= gxmax; ix++){
		    gp.x = map.origin.x + ix * map.spacing.x;

		    int gridPoint = ix + iy * nx + iz * nx * ny;

		    double d2 = gp.distanceSq(atom);

		    if(d2 < maxd2){
			double d = Math.sqrt(d2);
			int bin = (int)(0.5 + (d / 0.1));

			if(bin < pmfSize){
			    map.data[gridPoint] += pmfData[bin];
			}
		    }
		}
	    }
	}
    }

    /** Evaluate asp potential of a point. */
    public static double evaluatePotential(Point3d gp,
					   IntArray neighbours,
					   String probeName,
					   StringArray types,
					   Hashtable pmfs){
	double aspScore = 0.0;
	int neighbourCount = neighbours.size();

	//System.out.println("neighbourcount " + neighbourCount);

	for(int i = 0; i < neighbourCount; i++){
	    int iatom = neighbours.get(i);
	    String type = types.get(iatom);
	    Atom atom = (Atom)exclusion.get(iatom);

	    DoubleArray pmf = (DoubleArray)pmfs.get(type);

	    if(pmf == null){
		Log.error("no pmf for " + type);
		continue;
	    }

	    double d = gp.distance(atom);
	    if(d < 6.0){
		int bin = (int)(d/0.1);

		if(bin < pmf.size()){
		    aspScore += pmf.get(bin);
		}else{
		    //System.out.println("bin " + bin + " max " + pmf.size() + " d " + d);
		}
	    }

	}

	//System.out.println("aspScore " + aspScore);

	return aspScore;
    }

    /** Try and load the pmf. */
    public static void loadPmf(Hashtable pmfs, String location, String pmf){
	String filename = location + "/" + pmf + ".pmf";
	DoubleArray values = new DoubleArray();

	//Log.info("loading filename " + filename);

	FILE f = FILE.open(filename);

	if(f == null){
	    Log.error("couldn't load " + filename);
	    return;
	}

	while(f.nextLine()){
	    double v = f.getDouble(1);
	    //Log.info("value %7.2f", v);
	    values.add(v);
	}

	f.close();

	pmfs.put(pmf, values);
    }

}