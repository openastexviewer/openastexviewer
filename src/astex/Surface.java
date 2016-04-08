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

/* Copyright Astex Technology Ltd. 1999-2001 */

/*
 * 17-05-01 mjh
 *	created
 */

/**
 * A class for generating three dimensional surfaces.
 *
 * @author Mike Hartshorn
 */
public class Surface {
    /** The default spacing for soft object grid. */
    private static double minimumSpacing = 0.25;

    /** The actual spacing of the grid. */
    private static double spacing = 0.0;

    /** The desired grid spacing. */
    private static double desiredGridSpacing = 0.5;

    /** The maximum size of the grid. */
    private static int maximumGridSize = 100;

    /** The actual size of the grid. */
    private static int gx, gy, gz;

    /** The actual grid. */
    private static float grid[] = null;

    /** The extent of the grid. */
    private static double gminx, gminy, gminz;

    /** The extent of the grid. */
    private static double gmaxx, gmaxy, gmaxz;

    /** The coordinates of the grid points. */
    private static double gridx[] = null;
    private static double gridy[] = null;
    private static double gridz[] = null;

    private static int visible[] = null;

    /** Reordered vertex list. */
    private static int reordered[] = null;

    /** The probe radius. */
    private static double rp = 1.5;

    /** The maximum radius we saw. */
    private static double maxRadius = 0.0;

    /** The number of probe positions. */
    private static int np = 40;

    /** Should we produce debug info? */
    private static boolean debugFlag = false;

    /** Print a debuggin message. */
    public static void debug(String s){
	if(debugFlag){
	    System.out.println(s);
	}
    }

    /** Turn debugging on/off. */
    public static void setDebug(boolean d){
	debugFlag = d;
    }

    /** Set the probe radius. */
    public static void setProbeRadius(double radius){
	rp = radius;
    }

    /** Set the minimum grid spacing. */
    public static void setMinimumSpacing(double s){
	minimumSpacing = s;
    }

    /** Set the maximum grid size. */
    public static void setMaximumGridsize(int gs){
	maximumGridSize = gs;
    }

    /** The x-coordinate of the atoms. */
    private static double ax[] = null;

    /** The y-coordinate of the atoms. */
    private static double ay[] = null;

    /** The z-coordinate of the atoms. */
    private static double az[] = null;

    /** The radii of the atoms. */
    private static double ar[] = null;

    /** The radii of the atoms squared. */
    private static double ar2[] = null;

    /** The list of selectd atoms. */
    private static int selected[] = null;

    /** The number of atoms. */
    private static int atomCount = 0;

    /** The number of neighbour atoms. */
    private static int neighbourCount = 0;

    /** The list of neighbour atoms. */
    private static int neighbours[] = null;

    /** The total number of torus points that were used. */
    private static int torusPoints = 0;

    /** The lattice object for neighbour calculations. */
    private static Lattice l = null;

    /** Create a soft object surface. */
    public static Tmesh connolly(DynamicArray atoms,
				 double gridSpacing, boolean solid){
	desiredGridSpacing = gridSpacing;

	atomCount = atoms.size();
	
	ax = new double[atomCount];
	ay = new double[atomCount];
	az = new double[atomCount];
	ar = new double[atomCount];
	ar2 = new double[atomCount];
	selected = new int[atomCount];
	// there can never be any more than atomCount
	// worth of neighbours
	neighbours = new int[atomCount];

	// gather all of the coordinates and radii

	int selectionCount = 0;
	maxRadius = 0.0;

	for(int a = 0; a < atomCount; a++){
	    Atom atom = (Atom)atoms.get(a);
	    ar[a] = atom.getVDWRadius() + rp;
	    if(ar[a] > maxRadius){
		maxRadius = ar[a];
	    }
	    ar2[a] = ar[a] * ar[a];
	    ax[a] = atom.getX();
	    ay[a] = atom.getY();
	    az[a] = atom.getZ();
	    if(atom.isSelected()){
		selected[a] = 1;
		selectionCount++;
	    }else{
		selected[a] = 0;
	    }
	}

	FILE.out.print("maximum solvent extended radius %.2f\n", maxRadius);

	l = new Lattice(2.01 * maxRadius);

	for(int a = 0; a < atomCount; a++){
	    l.add(a, ax[a], ay[a], az[a]);
	}

	// if nothing is selected then we select all
	// of the atoms and surface them...

	if(selectionCount == 0){
	    for(int a = 0; a < atomCount; a++){
		selected[a] = 1;
	    }

	    selectionCount = atomCount;
	}

	// now we need to grow the atoms that we surface
	// out by one layer to prevent strange artefacts
	// at the surface boundary.
	if(selectionCount != atomCount){
	    for(int a = 0; a < atomCount; a++){
		if(selected[a] == 0){
		    for(int b = 0; b < atomCount; b++){
			if(selected[b] == 1 &&
			   distance2(ax[a], ay[a], az[a],
				     ax[b], ay[b], az[b]) <
			   (ar[a]+ar[b])*(ar[a]+ar[b])){
			    selected[a] = 2;
			    break;
			}
		    }
		}
	    }
	}

	initialiseGrid(solid ? minimumSpacing : minimumSpacing * 2.5);

	long then, now;

	then = System.currentTimeMillis();

	projectPoints();

	debug("Point projection " + (System.currentTimeMillis() - then));

	then = System.currentTimeMillis();

	//torusPoints = 0;

	projectTorii();

	debug("Torus projection " + (System.currentTimeMillis() - then));

	//debug("Torus points " + torusPoints);

	// fix up the grid points that were outside
	// the solvent accessible surface
	int gridPointCount = gx * gy * gz;
	float gr[] = grid;

	for(int i = 0; i < gridPointCount; i++){
	    if(gr[i] < 0.0){
		gr[i] = (float)0.0;
	    }
	}

	// fix
	//GraphicalObject surface = GraphicalObject.create();
	Tmesh surface = new Tmesh();
	if(solid){
	    surface.style = Tmesh.TRIANGLES;
	}else{
	    surface.style = Tmesh.LINES;
	}

	then = System.currentTimeMillis();

	// contour at a level equal to the probe radius.
	// we are defining a surface at this distance from
	// the solvent extended surface.

	March.generateTriangles = solid;
	March.surface(grid, gx, gy, gz, (float)rp, false, surface);

	debug("Contour         " + (System.currentTimeMillis() - then));

	// fix
	//int pointCount = surface.pointCount;
	int pointCount = surface.np;

	for(int i = 0; i < pointCount; i++){
	    surface.x[i] *= spacing; surface.x[i] += gminx;
	    surface.y[i] *= spacing; surface.y[i] += gminy;
	    surface.z[i] *= spacing; surface.z[i] += gminz;
	}

	if(selectionCount != atomCount){
	    clipSurface(surface, solid);
	}

	if(!solid){
	    // ditch any normals or texture
	    // storage that we may have if it is a line object
	    surface.nx = null;
	    surface.ny = null;
	    surface.nz = null;
	    surface.u = null;
	    surface.v = null;
	}

	if(solid){
	    debug("points " + surface.np + " triangles " + surface.nt);

	    //fixNormals(surface);
	}

	// fix
	//debug("surface has " + surface.lineCount + " lines");

	return surface;
    }

    /** Make better normals for the surface. */
    public static void fixNormals(Tmesh surface){
	l = new Lattice((maxRadius - rp) * 1.05);

	for(int a = 0; a < atomCount; a++){
	    l.add(a, ax[a], ay[a], az[a]);
	}

	IntArray neighbours = new IntArray();

	int pointCount = surface.np;

	for(int p = 0; p < pointCount; p++){
	    neighbours.removeAllElements();
	    double px = surface.x[p];
	    double py = surface.y[p];
	    double pz = surface.z[p];

	    l.getPossibleNeighbours(-1, px, py, pz, neighbours, true);

	    int neighbourCount = neighbours.size();
	    int total = 0;

	    for(int n = 0; n < neighbourCount; n++){
		int a = neighbours.get(n);
		double d = distance(ax[a], ay[a], az[a], px, py, pz);

		if(Math.abs(d - (ar[a] - rp)) < 0.0005){
		    total++;
		    // this point is on surface of this atom
		    double nx = px - ax[a];
		    double ny = py - ay[a];
		    double nz = pz - az[a];
		    double len = Math.sqrt(nx*nx + ny*ny + nz*nz);
		    nx /= len;
		    ny /= len;
		    nz /= len;

		    surface.nx[p] = (float)nx;
		    surface.ny[p] = (float)ny;
		    surface.nz[p] = (float)nz;
		}
	    }

	    if(total > 1){
		System.out.println("point " + p + " on surface of " + total + " atoms");
	    }
	}
    }

    public static void clipSurface(Tmesh surface, boolean solid){
	int pointCount = surface.np;
	long then = System.currentTimeMillis();

	lastClip = -1;
	
	// gather the atoms with selected = 1
	neighbourCount = 0;
	for(int a = 0; a < atomCount; a++){
	    if(selected[a] == 1){
		neighbours[neighbourCount++] = a;
	    }
	}

	// finally clip to the surface atoms.
	    
	if(visible == null || visible.length < pointCount){
	    visible = new int[pointCount];
	}
	    
	debug("before compaction points " + surface.np + " triangles " + surface.nt);
	
	then = System.currentTimeMillis();
	
	int distanceComparisons = 0;
	
	double aax[] = ax;
	double aay[] = ay;
	double aaz[] = az;
	
	for(int i = 0; i < pointCount; i++){
	    int vis = 0;
	    double x = surface.x[i], y = surface.y[i], z = surface.z[i];
	    
	    // check the atom that clipped the last point
	    if(lastClip != -1){
		distanceComparisons++;
		double dx = aax[lastClip] - x;
		double dy = aay[lastClip] - y;
		double dz = aaz[lastClip] - z;
		double d2 = dx*dx + dy*dy + dz*dz;
		if(d2 < ar2[lastClip]){
		    vis = 1;
		}else{
		    lastClip = -1;
		}
	    }

	    if(vis == 0){
		//for(int a = 0; a < atomCount; a++){
		//if(selected[a] == 1){
		//  Atom atom = (Atom)atoms.get(a);
		for(int aa = 0; aa < neighbourCount; aa++){
		    int a = neighbours[aa];
		    double dx = aax[a] - x;
		    double dy = aay[a] - y;
		    double dz = aaz[a] - z;
		    double d2 = dx*dx + dy*dy + dz*dz;
		    distanceComparisons++;
		    if(d2 < ar2[a]){
			vis = 1;
			lastClip = a;
			break;
		    }
		}
	    }
		
	    visible[i] = vis;
	}

	debug("distanceComparisons " + distanceComparisons);
	debug("visibility         " + (System.currentTimeMillis() - then));



	then = System.currentTimeMillis();

	int newLines = 0;
	
	if(solid){
	    for(int i = 0; i < surface.nt; i++){
		int v0 = surface.t0[i];
		int v1 = surface.t1[i];
		int v2 = surface.t2[i];
		
		if(visible[v0] == 1 && visible[v1] == 1 &&
		   visible[v2] == 1){
		    surface.t0[newLines] = v0;
		    surface.t1[newLines] = v1;
		    surface.t2[newLines] = v2;
		    surface.tcolor[newLines] = surface.tcolor[i];
		    newLines++;
		}
	    }
	}else{
	    for(int i = 0; i < surface.nt; i++){
		int v0 = surface.t0[i];
		int v1 = surface.t1[i];
		
		if(visible[v0] == 1 && visible[v1] == 1){
		    surface.t0[newLines] = v0;
		    surface.t1[newLines] = v1;
		    surface.tcolor[newLines] = surface.tcolor[i];
		    newLines++;
		}
	    }
	}
	
	debug("line compaction         " + (System.currentTimeMillis() - then));
	
	surface.nt = newLines;
	
	int newPoints = 0;
	
	if(reordered == null || reordered.length < pointCount){
	    reordered = new int[pointCount];
	}
	
	then = System.currentTimeMillis();
	
	for(int i = 0; i < surface.np; i++){
	    if(visible[i] == 1){
		surface.x[newPoints] = surface.x[i];
		surface.y[newPoints] = surface.y[i];
		surface.z[newPoints] = surface.z[i];
		if(solid){
		    surface.nx[newPoints] = surface.nx[i];
		    surface.ny[newPoints] = surface.ny[i];
		    surface.nz[newPoints] = surface.nz[i];
		}
		surface.vcolor[newPoints] = surface.vcolor[i];
		reordered[i] = newPoints;
		newPoints++;
	    }
	}
	
	System.out.println("newPoints " + newPoints);
	
	surface.np = newPoints;
	
	for(int i = 0; i < surface.nt; i++){
	    surface.t0[i] = reordered[surface.t0[i]];
	    surface.t1[i] = reordered[surface.t1[i]];
	    if(solid){
		surface.t2[i] = reordered[surface.t2[i]];
	    }
	}
	
	debug("point compaction         " + (System.currentTimeMillis() - then));
	
	
	//debug("post process     " + (System.currentTimeMillis() - then));
	
    }

    /** Project the points inside the atoms onto the surface. */
    public static void projectPoints(){
	float gr[] = grid;

	IntArray possibleNeighbours = new IntArray();

	for(int a = 0; a < atomCount; a++){
	    double ra = ar[a];
	    double r2 = ra * ra;
	    double aax = ax[a];
	    double aay = ay[a];
	    double aaz = az[a];

	    neighbourCount = 0;

	    if(selected[a] > 0){
		possibleNeighbours.removeAllElements();

		l.getPossibleNeighbours(a, aax, aay, aaz, possibleNeighbours, true);

		int possibleNeighbourCount = possibleNeighbours.size();

		for(int p = 0; p < possibleNeighbourCount; p++){
		    // XXX can reduce the number of torii we generate here
		    // XXX only need to consider pairs of selected atoms once..
		    int b = possibleNeighbours.get(p);

		    double rb = ar[b];
		    if(distance2(aax, aay, aaz, ax[b], ay[b], az[b]) <
		       (ra + rb) * (ra + rb)){
			neighbours[neighbourCount++] = b;
		    }
		}

		// number of grid points covered by atom.
		int ng = 1 + (int)(ra / spacing);

		// grid point of atom center.
		int iax = (int)(0.5 + ((aax - gminx) / spacing));
		int iay = (int)(0.5 + ((aay - gminy) / spacing));
		int iaz = (int)(0.5 + ((aaz - gminz) / spacing));

		// force grid point ranges to lie in grid.
		int minx = iax - ng; if(minx < 0) minx = 0;
		int maxx = iax + ng; if(maxx > gx) maxx = gx;
		int miny = iay - ng; if(miny < 0) miny = 0;
		int maxy = iay + ng; if(maxy > gy) maxy = gy;
		int minz = iaz - ng; if(minz < 0) minz = 0;
		int maxz = iaz + ng; if(maxz > gz) maxz = gz;

		lastClip = -1;

		for(int iz = minz; iz < maxz; iz++){
		    double dz = gridz[iz] - aaz;
		    int zoffset = gx*gy*iz;

		    for(int iy = miny; iy < maxy; iy++){
			double dy = gridy[iy] - aay;
			double dzy2 = dz*dz + dy*dy;
			int yzoffset = zoffset + gx*iy;

			for(int ix = minx; ix < maxx; ix++){
			    double dx = gridx[ix] - aax;
			    double d2 = dzy2 + dx*dx;

			    if(d2 < r2){
				int idx = ix + yzoffset;
				double current = gr[idx];

				// if the current value is less than zero
				// we didn't visit this yet
				// mark it as inside the solvent accessible
				// surface by making it positive
				if(current < 0.0){
				    current = -current;
				    gr[idx] = (float)current;
				}

				// project onto surface of sphere
				// dx is the relative vector, spx will
				// be projection of point onto surface
				double d = Math.sqrt(d2);
				double ap = ra / d;
				double spx = dx * ap;
				double spy = dy * ap;
				double spz = dz * ap;

				spx += aax; spy += aay; spz += aaz;

				// check and see if this point is within
				// another atom
				if(obscured(spx, spy, spz, a, -1) == -1){
				    double dd = ra - d;
				
				    if(dd < current){
					gr[idx] = (float)dd;
				    }
				}
			    }
			}
		    }
		}
	    }
	}
    }

    /** Project the points inside the atoms onto the surface. */
    public static void projectTorii(){

	IntArray possibleNeighbours = new IntArray();

	for(int a = 0; a < atomCount; a++){
	    double r1 = ar[a];
	    double aax = ax[a];
	    double aay = ay[a];
	    double aaz = az[a];
	    neighbourCount = 0;

	    if(selected[a] > 0){
		possibleNeighbours.removeAllElements();

		l.getPossibleNeighbours(a, aax, aay, aaz, possibleNeighbours, true);

		int possibleNeighbourCount = possibleNeighbours.size();

		for(int p = 0; p < possibleNeighbourCount; p++){
		    // XXX can reduce the number of torii we generate here
		    // XXX only need to consider pairs of selected atoms once..
		    int b = possibleNeighbours.get(p);

		    if(selected[b] > 0 && a != b){
			double r12 = r1 + ar[b];
			double dx = aax - ax[b];
			double dy = aay - ay[b];
			double dz = aaz - az[b];
			if((dx*dx + dy*dy + dz*dz) < r12*r12){
			    neighbours[neighbourCount++] = b;
			}
		    }
		}

		for(int b = 0; b < neighbourCount; b++){
		    // the two atoms are close enough together
		    // to form a torus.
		    if(a < neighbours[b]){
			projectTorus(a, neighbours[b]);
		    }
		}
	    }
	}
    }

    private static Point3d atom1 = new Point3d();
    private static Point3d atom2 = new Point3d();
    private static Point3d mid   = new Point3d();
    private static Point3d n1    = new Point3d();
    private static Point3d n2    = new Point3d();

    private static double cosTable[] = null;
    private static double sinTable[] = null;

    /** Project the points of a torus onto the grid. */
    public static void projectTorus(int a, int b){
	double r1 = ar[a];
	double r2 = ar[b];
	double dx = ax[b] - ax[a];
	double dy = ay[b] - ay[a];
	double dz = az[b] - az[a];
	double d = Math.sqrt(dx*dx + dy*dy + dz*dz);

	double cosA = (r1 * r1 + d * d - r2 * r2) / (2.0 * r1 * d);

	// distance to mid point is
	double dmp = r1 * cosA;

	float gr[] = grid;

	atom1.set(ax[a], ay[a], az[a]);
	atom2.set(ax[b], ay[b], az[b]);
	
	Point3d.unitVector(mid, atom1, atom2);
	Point3d.normalToLine(mid, n1);
	Point3d.cross(n2, mid, n1);

	double r = Math.sqrt(r1 * r1 - dmp * dmp);

	n1.scale(r);
	n2.scale(r);

	mid.scale(dmp);
	mid.add(atom1);

	double step = 2. * Math.PI / np;
	double theta = 0.0;

	// build sin,cos lookup tables
	if(cosTable == null){
	    cosTable = new double[np];
	    sinTable = new double[np];
	    for(int j = 0; j < np; j++){
		cosTable[j] = Math.cos(theta);
		sinTable[j] = Math.sin(theta);
		theta += step;
	    }

	}

	lastClip = -1;

	for(int i = 0; i < np; i++){
	    //double cost = Math.cos(theta);
	    //double sint = Math.sin(theta);
	    double cost = cosTable[i];
	    double sint = sinTable[i];
	    double px = mid.x + cost*n1.x + sint*n2.x;
	    double py = mid.y + cost*n1.y + sint*n2.y;
	    double pz = mid.z + cost*n1.z + sint*n2.z;

	    if(obscured(px, py, pz, a, b) == -1){
		//torusPoints++;

		int ng = 4 + (int)((rp / spacing));
	    
		int iax = (int)(0.5 + ((px - gminx) / spacing));
		int iay = (int)(0.5 + ((py - gminy) / spacing));
		int iaz = (int)(0.5 + ((pz - gminz) / spacing));

		int minx = iax - ng; if(minx < 0) minx = 0;
		int maxx = iax + ng; if(maxx > gx) maxx = gx;
		int miny = iay - ng; if(miny < 0) miny = 0;
		int maxy = iay + ng; if(maxy > gy) maxy = gy;
		int minz = iaz - ng; if(minz < 0) minz = 0;
		int maxz = iaz + ng; if(maxz > gz) maxz = gz;
		
		for(int iz = minz; iz < maxz; iz++){
		    int zoffset = gx*gy*iz;
		    dz = pz - gridz[iz];

		    for(int iy = miny; iy < maxy; iy++){
			int yzoffset = zoffset + gx*iy;
			dy = py - gridy[iy];
			double dzy2 = dz*dz + dy*dy;

			for(int ix = minx; ix < maxx; ix++){
			    dx = px - gridx[ix];
			    // calcuate the square of the distance.
			    double d2 = dzy2 + dx*dx;

			    int idx = ix + yzoffset;
			    double current = gr[idx];

			    // compare againt the square of the grid value
			    // to avoid the square root unless we really
			    // want to store it...
			    if(current > 0.0 && d2 < (current*current)){
				gr[idx] = (float)Math.sqrt(d2);
			    }
			}
		    }
		}
	    }
	}
    }

    /** Calculate the distance between two points. */
    public static double distance(double xa, double ya, double za,
				  double xb, double yb, double zb){
	double dx = xa - xb;
	double dy = ya - yb;
	double dz = za - zb;

	return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    /** Calucate the sqaured distance between two points. */
    public static double distance2(double xa, double ya, double za,
				   double xb, double yb, double zb){
	double dx = xa - xb;
	double dy = ya - yb;
	double dz = za - zb;

	return (dx*dx + dy*dy + dz*dz);
    }

    /** The last atom that clipped a point. */
    public static int lastClip = -1;

    /**
     * Is the point within one of the atoms in the list.
     * Caches the last atom that clipped a point, as
     * this often clips the next point.
     * a and b are omitted from the check.
     *
     * Return value of -1 indicates that the point
     * was not obscured by any atom in the neighbour list.
     */
    public static int obscured(double x, double y, double z,
			       int a, int b){
	if(lastClip != -1){
	    double r = ar[lastClip];
	    double dx = ax[lastClip] - x;
	    double dy = ay[lastClip] - y;
	    double dz = az[lastClip] - z;
	    double d2 = dx*dx + dy*dy + dz*dz;
	    if(d2 < ar2[lastClip] &&
	       lastClip != a && lastClip != b){
		return lastClip;
	    }else{
		lastClip = -1;
	    }
	}

	for(int ia = 0; ia < neighbourCount; ia++){
	    int i = neighbours[ia];
	    double r = ar[i];
	    double dx = ax[i] - x;
	    double dy = ay[i] - y;
	    double dz = az[i] - z;
	    double d2 = dx*dx + dy*dy + dz*dz;

	    if(d2 < ar2[i] && i != a && i != b){
		lastClip = i;
		return lastClip;
	    }
	}

	lastClip = -1;
	return lastClip;
    }

    /** Find the size of the atoms that we will surface. */
    public static void initialiseGrid(double minSpacing){

	// figure out the size of the box containing
	// the selected atoms.

	gminx = gminy = gminz =  1.e10;
	gmaxx = gmaxy = gmaxz = -1.e10;

	for(int a = 0; a < atomCount; a++){
	    if(selected[a] == 1){
		// allow for the radius of the atoms.
		if(ax[a] - ar[a] < gminx) gminx = ax[a] - ar[a];
		if(ay[a] - ar[a] < gminy) gminy = ay[a] - ar[a];
		if(az[a] - ar[a] < gminz) gminz = az[a] - ar[a];
		if(ax[a] + ar[a] > gmaxx) gmaxx = ax[a] + ar[a];
		if(ay[a] + ar[a] > gmaxy) gmaxy = ay[a] + ar[a];
		if(az[a] + ar[a] > gmaxz) gmaxz = az[a] + ar[a];
	    }
	}

	double gridBorder = 0.2;

	// add in a border
	gminx -= gridBorder;
	gminy -= gridBorder;
	gminz -= gridBorder;
	gmaxx += gridBorder;
	gmaxy += gridBorder;
	gmaxz += gridBorder;

	debug("min " + gminx + " " + gminy + " " + gminz);
	debug("max " + gmaxx + " " + gmaxy + " " + gmaxz);

	double maxe = gmaxx - gminx;
	if(gmaxy - gminy > maxe) maxe = gmaxy - gminy;
	if(gmaxz - gminz > maxe) maxe = gmaxz - gminz;

	int biggestGrid = (int)(maxe/desiredGridSpacing);

	if(biggestGrid > maximumGridSize){
	    biggestGrid = maximumGridSize;
	    spacing = maxe / biggestGrid;
	}else{
	    spacing = desiredGridSpacing;
	}

	// force to the minimum spacing that we will allow

	if(spacing < minSpacing){
	    spacing = minSpacing;
	}

	debug("spacing " + spacing);

	gx = 1 + (int)((gmaxx - gminx)/spacing);
	gy = 1 + (int)((gmaxy - gminy)/spacing);
	gz = 1 + (int)((gmaxz - gminz)/spacing);

	debug("gx " + gx + " gy " + gy + " gz " + gz);
	
	int gridPointCount = gx*gy*gz;

	grid = new float[gridPointCount];

	float gr[] = grid;

	for(int i = 0; i < gridPointCount; i++){
	    gr[i] = (float)-1001.0;
	}

	gridx = new double[gx];
	gridy = new double[gy];
	gridz = new double[gz];

	for(int i = 0; i < gx; i++) gridx[i] = gminx + i * spacing;
	for(int i = 0; i < gy; i++) gridy[i] = gminy + i * spacing;
	for(int i = 0; i < gz; i++) gridz[i] = gminz + i * spacing;
    }

    /* Generate dot spheres. */
    private static DynamicArray dots = new DynamicArray();
    private static DynamicArray triangles = new DynamicArray();

    private static void addSpherePoint(Point3d p){
	dots.add(new Point3d(p));
    }
    
    private static void addTriangle(int i, int j, int k){
	int tri[] = new int[3];
	tri[0] = i;
	tri[1] = j;
	tri[2] = k;
	triangles.add(tri);
    }

    private static int findSpherePoint(Point3d a, Point3d b){
	Point3d mid = Point3d.mid(a, b);
	double len = mid.length();
	mid.scale(1./len);

	int dotCount = dots.size();

	for(int d = 0; d < dotCount; d++){
	    Point3d p = (Point3d)dots.get(d);
	    if(p.distanceSq(mid) < 0.0001){
		return d;
	    }
	}

	// not there so add it

	dots.add(mid);

	return dots.size() - 1;
    }

    /** Initialise the dot sphere structures. */
    public static void sphereGen(int subDivisions){
	dots.removeAllElements();
	triangles.removeAllElements();

	addSpherePoint(new Point3d(1.0, 0.0, 0.0));
	addSpherePoint(new Point3d(0.0, 1.0, 0.0));
	addSpherePoint(new Point3d(0.0, 0.0, 1.0));
	addSpherePoint(new Point3d(-1.0, 0.0, 0.0));
	addSpherePoint(new Point3d(0.0, -1.0, 0.0));
	addSpherePoint(new Point3d(0.0, 0.0, -1.0));
	
	addTriangle(0, 1, 2);
	addTriangle(0, 1, 5);
	addTriangle(0, 2, 4);
	addTriangle(0, 4, 5);
	addTriangle(1, 2, 3);
	addTriangle(1, 3, 5);
	addTriangle(2, 3, 4);
	addTriangle(3, 4, 5);

	int firstTriangle = 0;

	for(int sub = 0; sub < subDivisions; sub++){
	    int triCount = triangles.size();

	    for(int t = firstTriangle; t < triCount; t++){
		int tri[] = (int[])triangles.get(t);
		Point3d v0 = (Point3d)dots.get(tri[0]);
		Point3d v1 = (Point3d)dots.get(tri[1]);
		Point3d v2 = (Point3d)dots.get(tri[2]);

		int mid01 = findSpherePoint(v0, v1);
		int mid12 = findSpherePoint(v1, v2);
		int mid20 = findSpherePoint(v2, v0);

		addTriangle(mid01, mid12, mid20);
		addTriangle(tri[0], mid01, mid20);
		addTriangle(tri[1], mid01, mid12);
		addTriangle(tri[2], mid12, mid20);
	    }

	    firstTriangle = triCount;
	}
    }
    
    /** Generate a dot surface. */

    public static synchronized
	Tmesh dotSurface(DynamicArray selectedAtoms,
			 int subDivisions){

	sphereGen(subDivisions);
	int dotCount = dots.size();
	Point3d dot = new Point3d();

	// fix
	//GraphicalObject ds = GraphicalObject.create();
	Tmesh ds = new Tmesh();
	ds.style = Tmesh.DOTS;

	int atomCount = selectedAtoms.size();
	neighbours = new int[atomCount];

	for(int a = 0; a < atomCount; a++){
	    Atom atom = (Atom)selectedAtoms.get(a);
	    double ra = atom.getVDWRadius();
	    int atomColor = atom.getColor();

	    neighbourCount = 0;

	    for(int b = 0; b < atomCount; b++){
		// XXX can reduce the number of torii we generate here
		// XXX only need to consider pairs of selected atoms once..
		Atom atom2 = (Atom)selectedAtoms.get(b);
		if(a != b){
		    double rb = atom2.getVDWRadius();
		    if(atom.distanceSq(atom2) < (ra + rb) * (ra + rb)){
			neighbours[neighbourCount++] = b;
		    }
		}
	    }
	    


	    double r = atom.getVDWRadius();

	    Atom lastClippingAtom = null;

	    for(int i = 0; i < dotCount; i++){
		boolean clipped = false;
		dot.set((Point3d)dots.get(i));
		dot.scale(r);
		dot.add(atom);
		
		if(lastClippingAtom != null){
		    double lastr = lastClippingAtom.getVDWRadius();
		    if(lastClippingAtom.distanceSq(dot) < lastr*lastr){
			clipped = true;
		    }else{
			lastClippingAtom = null;
		    }
		}
		
		if(clipped == false){
		    for(int b = 0; b < neighbourCount; b++){
			Atom atom2 = (Atom)selectedAtoms.get(neighbours[b]);
			double r2 = atom2.getVDWRadius();
			if(atom2.distanceSq(dot) < r2*r2){
			    lastClippingAtom = atom2;
			    clipped = true;
			}
		    }
		}
		
		// fix
		if(!clipped){
		    ds.addPoint(dot.x, dot.y, dot.z, atomColor);
		}
	    }
	}

	//ds.setColor(Renderer.yellow);

	return ds;
    }
}
