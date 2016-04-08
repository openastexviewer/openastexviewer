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

public class ShadowCache {
    /** Sphere cache for shadows. */
    public static FloatArray scachex = new FloatArray();
    public static FloatArray scachey = new FloatArray();
    public static FloatArray scachez = new FloatArray();
    public static FloatArray scacher = new FloatArray();

    /** Cylinder cache for shadows. */
    public static FloatArray ccachex0 = new FloatArray();
    public static FloatArray ccachey0 = new FloatArray();
    public static FloatArray ccachez0 = new FloatArray();
    public static FloatArray ccachex1 = new FloatArray();
    public static FloatArray ccachey1 = new FloatArray();
    public static FloatArray ccachez1 = new FloatArray();
    public static FloatArray ccacher  = new FloatArray();

    /** Triangle cache for shadows. */
    public static FloatArray tcachex0 = new FloatArray();
    public static FloatArray tcachey0 = new FloatArray();
    public static FloatArray tcachez0 = new FloatArray();
    public static FloatArray tcachex1 = new FloatArray();
    public static FloatArray tcachey1 = new FloatArray();
    public static FloatArray tcachez1 = new FloatArray();
    public static FloatArray tcachex2 = new FloatArray();
    public static FloatArray tcachey2 = new FloatArray();
    public static FloatArray tcachez2 = new FloatArray();
    //public static IntArray   ttransp  = new IntArray();

    public static FloatArray tcen2dx  = new FloatArray();
    public static FloatArray tcen2dy  = new FloatArray();
    public static FloatArray tcenx    = new FloatArray();
    public static FloatArray tceny    = new FloatArray();
    public static FloatArray tcenz    = new FloatArray();
    public static FloatArray tcenr    = new FloatArray();

    /** References to the triangle array contents. */
    public static float tx0[] = null;
    public static float ty0[] = null;
    public static float tz0[] = null;
    public static float tx1[] = null;
    public static float ty1[] = null;
    public static float tz1[] = null;
    public static float tx2[] = null;
    public static float ty2[] = null;
    public static float tz2[] = null;

    /** References to the triangle bounding sphere info. */
    public static float tc2x[] = null;
    public static float tc2y[] = null;
    public static float tcx[] = null;
    public static float tcy[] = null;
    public static float tcz[] = null;
    public static float tcr[] = null;

    /** References for sphere centers. */
    public static float scx[] = null;
    public static float scy[] = null;
    public static float scz[] = null;
    public static float scr[] = null;

    /** Cache and occlusion lists. */
    public static IntArray sphereShadowCacheList    = new IntArray();
    public static IntArray sphereOcclusionCacheList = new IntArray();
    public static IntArray cylinderShadowCacheList  = new IntArray();
    public static IntArray triangleShadowCacheList  = new IntArray();
    public static IntArray initialList              = new IntArray();

    /** Overall scale factor in the renderer. */
    public static double overallScale = 1.0;

    /**
     * Prepare shadow cache list for triangle, by making bounding
     * volume a sphere that encloses the whole triangle.
     */
    public static void prepareTriangleCacheList(double x0, double y0, double z0,
						double x1, double y1, double z1,
						double x2, double y2, double z2,
						boolean targetIsTransparent){
	boundingSphereTriangle(x0, y0, z0, x1, y1, z1, x2, y2, z2, cbs);

	prepareSphereCacheList(cbs[0], cbs[1], cbs[2], cbs[3], targetIsTransparent);
    }

    /** Bounding sphere. */
    private static double cbs[] = new double[4];

    /**
     * Prepare shadow cache list for cylinder, by making bounding
     * volume a sphere that encloses the whole cylinder.
     */
    public static void prepareCylinderCacheList(double c0x, double c0y, double c0z,
						double c1x, double c1y, double c1z,
						double r){
	boundingSphereCylinder(c0x, c0y, c0z,
			       c1x, c1y, c1z, r,
			       cbs);

	prepareSphereCacheList(cbs[0], cbs[1], cbs[2], cbs[3], false);
    }

    /** Prepare bounding sphere that encloses cylinder. */
    public static void boundingSphereCylinder(double c0x, double c0y, double c0z,
					      double c1x, double c1y, double c1z,
					      double r, double bs[]){
	bs[0] = 0.5 * (c0x + c1x);
	bs[1] = 0.5 * (c0y + c1y);
	bs[2] = 0.5 * (c0z + c1z);
	double dmx = c0x - c1x;
	double dmy = c0y - c1y;
	double dmz = c0z - c1z;
	bs[3] = 0.5 * Math.sqrt(dmx*dmx + dmy*dmy + dmz*dmz) + r;
    }

    /** Prepare bounding sphere that encloses cylinder. */
    public static void boundingSphereTriangle(double x0, double y0, double z0,
					      double x1, double y1, double z1,
					      double x2, double y2, double z2,
					      double bs[]){
	bs[0] = (x0 + x1 + x2)/3.0;
	bs[1] = (y0 + y1 + y2)/3.0;
	bs[2] = (z0 + z1 + z2)/3.0;
	double dmx = x0 - bs[0];
	double dmy = y0 - bs[1];
	double dmz = z0 - bs[2];
	double r = 0.0;
	double rad = Math.sqrt(dmx*dmx + dmy*dmy + dmz*dmz);
	dmx = x1 - bs[0];
	dmy = y1 - bs[1];
	dmz = z1 - bs[2];
	r = Math.sqrt(dmx*dmx + dmy*dmy + dmz*dmz);
	if(r > rad) rad = r;
	dmx = x2 - bs[0];
	dmy = y2 - bs[1];
	dmz = z2 - bs[2];
	r = Math.sqrt(dmx*dmx + dmy*dmy + dmz*dmz);
	if(r > rad) rad = r;

	bs[3] = rad;
    }

    /**
     * Prepare a list of objects that may obscure points on _this_ sphere.
     * As we have scan line coherence, the cache list can be reused many
     * times for a given sphere.
     */
    public static void prepareSphereCacheList(double sx, double sy, double sz, double sr,
					      boolean transparent){
	int sphereCount = scachex.size();

	double x = sx * lightx.x + sy * lightx.y + sz * lightx.z;
	double y = sx * lighty.x + sy * lighty.y + sz * lighty.z;

	if(sphereCount > 0){
	    sphereOcclusionCacheList.removeAllElements();
	    sphereShadowCacheList.removeAllElements();
	    initialList.removeAllElements();

	    sphereGrid.getPossibleNeighbours(-1, x, y,
					     sr + sphereGrid.getSpacing(),
					     initialList, true);
	    
	    int initialSize = initialList.size();

	    for(int j = 0; j < initialSize; j++){
		int i = initialList.get(j);

		// look up triangle center and radius
		double r   = sr + scr[i];
	    
		double s2px = sx - scx[i];
		double s2py = sy - scy[i];
		double s2pz = sz - scz[i];
		
		double dot = s2px*light.x + s2py*light.y + s2pz*light.z;
		
		if(dot <= 0.0 /* &&
		   (Math.abs(s2px) > 1.e-3 ||
		    Math.abs(s2py) > 1.e-3 ||
		    Math.abs(s2pz) > 1.e-3)
		   */){
		    // could shadow us
		    // project intersphere vector onto 2d coordinate frame
		    double projx = lightx.x * s2px + lightx.y * s2py + lightx.z * s2pz;
		    double projy = lighty.x * s2px + lighty.y * s2py + lighty.z * s2pz;
		    
		    if(projx < r && projy < r){
			if(projx * projx + projy * projy < r * r){
			    // ok, it was between us and the light
			    // and the sphere perimeters overlap
			    sphereShadowCacheList.add(i);
			}
		    }
		}
	    }

	    //FILE.out.print("initial %5d ", initialSize);
	    //FILE.out.print("final %5d\n", sphereShadowCacheList.size());
	}

	// prepare the cylinder overlap
	int cylinderCount = ccachex0.size();

	if(cylinderCount > 0){
	    cylinderShadowCacheList.removeAllElements();

	    initialList.removeAllElements();

	    cylinderGrid.getPossibleNeighbours(-1, x, y,
					       sr + cylinderGrid.getSpacing(), initialList, true);
	    
	    int initialSize = initialList.size();

	    for(int j = 0; j < initialSize; j++){
		int i = initialList.get(j);

		boundingSphereCylinder(ccachex0.get(i), ccachey0.get(i), ccachez0.get(i),
				       ccachex1.get(i), ccachey1.get(i), ccachez1.get(i),
				       ccacher.get(i),
				       cbs);

		double r   = sr + cbs[3];
		double r2  = r * r;

		double s2px = sx - cbs[0];
		double s2py = sy - cbs[1];
		double s2pz = sz - cbs[2];
		double dot = s2px*light.x + s2py*light.y + s2pz*light.z;

		if(dot <= 0.0){
		    // could shadow us
		    // project intersphere vector onto 2d coordinate frame
		    double projx = lightx.x * s2px + lightx.y * s2py + lightx.z * s2pz;
		    double projy = lighty.x * s2px + lighty.y * s2py + lighty.z * s2pz;

		    if(projx < r && projy < r){
			if(projx * projx + projy * projy < r2){
			    // ok, it was between us and the light
			    // and the sphere perimeters overlap
			    cylinderShadowCacheList.add(i);
			}
		    }
		}
	    }
	}

	// two stages
	// first get spheres that could possibly overlap in the
	// grid structure
	// two remove those that don't overlap

	if(tcachex0.size() > 0){
	    triangleShadowCacheList.removeAllElements();
	    initialList.removeAllElements();

	    triangleGrid.getPossibleNeighbours(-1, x, y, sr + triangleGrid.getSpacing(), initialList, true);
	    
	    int initialSize = initialList.size();

	    for(int j = 0; j < initialSize; j++){
		int i = initialList.get(j);

		// look up triangle center and radius
		double r   = sr + tcr[i];
	    
		double s2px = sx - tcx[i];
		double s2py = sy - tcy[i];
		double s2pz = sz - tcz[i];
		
		double dot = s2px*light.x + s2py*light.y + s2pz*light.z;
		
		if(dot <= 0.0 &&
		   (Math.abs(s2px) > 1.e-3 ||
		    Math.abs(s2py) > 1.e-3 ||
		    Math.abs(s2pz) > 1.e-3)){
		    // could shadow us
		    // project intersphere vector onto 2d coordinate frame
		    double projx = lightx.x * s2px + lightx.y * s2py + lightx.z * s2pz;
		    double projy = lighty.x * s2px + lighty.y * s2py + lighty.z * s2pz;
		    
		    if(projx < r && projy < r){
			if(projx * projx + projy * projy < r * r){
			    // ok, it was between us and the light
			    // and the sphere perimeters overlap
			    //int t = ttransp.get(i);
			    //if((transparent == true) ||
			    //   (transparent == false && t == 255)){
				triangleShadowCacheList.add(i);
				//}
			}
		    }
		}
	    }

	    //FILE.out.print("initial %5d ", initialSize);
	    //FILE.out.print("final %5d\n", triangleShadowCacheList.size());
	}

	//FILE.out.print("triangle cache %d", triangleShadowCacheList.size());
	//FILE.out.print("[%d]\n", tcachex0.size());

	//FILE.out.print("cylinder shadow cache    %5d\n", cylinderShadowCacheList.size());
	//FILE.out.print("shadow cache    %5d\n", sphereShadowCacheList.size());
	//FILE.out.print("occlusion cache %5d\n", sphereOcclusionCacheList.size());
    }

    /** Add a sphere to the cache list. */
    public static void addSphereToCacheList(double x, double y, double z, double r){
	scachex.add((float)x);
	scachey.add((float)y);
	scachez.add((float)z);
	scacher.add((float)r);
    }

    /** Add a cylinder to the cache list. */
    public static void addCylinderToCacheList(double x0, double y0, double z0,
					      double x1, double y1, double z1,
					      double r){
	ccachex0.add((float)x0);
	ccachey0.add((float)y0);
	ccachez0.add((float)z0);
	ccachex1.add((float)x1);
	ccachey1.add((float)y1);
	ccachez1.add((float)z1);
	ccacher.add((float)r);
    }

    /** Add a cylinder to the cache list. */
    public static void addTriangleToCacheList(double x0, double y0, double z0,
					      double x1, double y1, double z1,
					      double x2, double y2, double z2,
					      int transparency){
	/*
	FILE.out.print("%d\n", tcachex0.size());
	FILE.out.print("%5.1f,", x0);
	FILE.out.print("%5.1f,", y0);
	FILE.out.print("%5.1f\n", z0);
	FILE.out.print("%5.1f,", x1);
	FILE.out.print("%5.1f,", y1);
	FILE.out.print("%5.1f\n", z1);
	FILE.out.print("%5.1f,", x2);
	FILE.out.print("%5.1f,", y2);
	FILE.out.print("%5.1f\n", z2);
	*/

	tcachex0.add((float)x0);
	tcachey0.add((float)y0);
	tcachez0.add((float)z0);
	tcachex1.add((float)x1);
	tcachey1.add((float)y1);
	tcachez1.add((float)z1);
	tcachex2.add((float)x2);
	tcachey2.add((float)y2);
	tcachez2.add((float)z2);
	//ttransp.add(transparency);
    }

    /** Cylinder endpoints */
    private static double c0[] = new double[3];
    private static double c1[] = new double[3];
    private static double c2[] = new double[3];

    /** ray endpoints */
    private static double ray0[] = new double[3];
    private static double ray1[] = new double[3];

    /** Intersection points. */
    private static double rint[] = new double[3];
    private static double nint[] = new double[3];

    /** Intersection parameters for triangle. */
    private static double tuv[] = new double[3];

    private static double eye[] = new double[3];
    private static double eyedir[]  = new double[3];

    private static double cent[] = new double[3];

    /**
     * Is the surface at this point self shadowing.
     * i.e. does the normal point away from the light.
     */
    public static boolean selfShadowed(double nx, double ny, double nz, double tol){
	//if(nx*light.x + ny*light.y + nz*light.z < 0.0){
	if(nx*light.x + ny*light.y + nz*light.z < tol){
	    return true;
	}

	return false;
    }

    /** project surface point. */
    private static double px = 0.0;
    private static double py = 0.0;

    /** Is this point shadowed by stuff in the shadow cache. */
    public static boolean pointShadowed(double x, double y, double z){
	int sphereCacheCount = sphereShadowCacheList.size();

	// shift point towards light to handle
	// self intersections more gracefully.
	x += 1. * light.x;
	y += 1. * light.y;
	z += 1. * light.z;

	// point on light coordinate system
	px = lightx.x * x + lightx.y * y + lightx.z * z;
	py = lighty.x * x + lighty.y * y + lighty.z * z;

	ray0[0] = x;
	ray0[1] = y;
	ray0[2] = z;
	ray1[0] = x + 100000. * light.x;
	ray1[1] = y + 100000. * light.y;
	ray1[2] = z + 100000. * light.z;

	// check caches
	if(lastObscuringSphere != -1){
	    if(obscuredBySphere(lastObscuringSphere, x, y, z)){
		return true;
	    }else{
		lastObscuringSphere = -1;
	    }
	}

	if(lastObscuringCylinder != -1){
	    if(obscuredByCylinder(lastObscuringCylinder)){
		return true;
	    }else{
		lastObscuringCylinder = -1;
	    }
	}

	int slist[] = sphereShadowCacheList.getArray();

	// ok now check lists
	for(int j = 0; j < sphereCacheCount; j++){
	    int i = slist[j];
	    if(obscuredBySphere(i, x, y, z)){
		lastObscuringSphere = i;
		return true;
	    }
	}

	int cylinderCacheCount = cylinderShadowCacheList.size();

	for(int j = 0; j < cylinderCacheCount; j++){
	    int i = cylinderShadowCacheList.get(j);
	    if(obscuredByCylinder(i)){
		lastObscuringCylinder = i;
		return true;
	    }
	}

	light.normalise();

	//reform ray1 as the direction to the light

	// offset ray further to try and reduce self
	// shadowing for triangles
	//ray0[2] += 20.0 / overallScale;
	// no longer needed

	ray1[0] = light.x;
	ray1[1] = light.y;
	ray1[2] = light.z;

	// check last triangle, if not still obscuring clear it
	if(lastObscuringTriangle != -1){
	    if(obscuredByTriangle(lastObscuringTriangle)){
		return true;
	    }else{
		lastObscuringTriangle = -1;
	    }
	}

	int triangleCacheCount = triangleShadowCacheList.size();

	/*
	FILE.out.print("p %6.2f,", ray0[0]);
	FILE.out.print("%6.2f,", ray0[1]);
	FILE.out.print("%6.2f\n", ray0[2]);
	*/

	for(int j = 0; j < triangleCacheCount; j++){
	    int i = triangleShadowCacheList.get(j);
	    if(obscuredByTriangle(i)){
		lastObscuringTriangle = i;
		return true;
	    }
	}

	return false;
    }

    private static int lastObscuringTriangle = -1;

    private static boolean obscuredByTriangle(int i){
	double dx = px - tc2x[i];
	double dy = py - tc2y[i];
	double r  = tcr[i];

	if(dx*dx + dy*dy > r*r){
	    return false;
	}

	/*
	c0[0] = tcachex0.get(i);
	c0[1] = tcachey0.get(i);
	c0[2] = tcachez0.get(i);
	c1[0] = tcachex1.get(i);
	c1[1] = tcachey1.get(i);
	c1[2] = tcachez1.get(i);
	c2[0] = tcachex2.get(i);
	c2[1] = tcachey2.get(i);
	c2[2] = tcachez2.get(i);
	*/
	c0[0] = tx0[i];
	c0[1] = ty0[i];
	c0[2] = tz0[i];
	c1[0] = tx1[i];
	c1[1] = ty1[i];
	c1[2] = tz1[i];
	c2[0] = tx2[i];
	c2[1] = ty2[i];
	c2[2] = tz2[i];
  
	if(intersect_triangle(ray0, ray1, c0, c1, c2, tuv) == 1 &&
	   tuv[0] >= 0.0){
	    return true;
	}

	return false;
    }

    private static int lastObscuringSphere = -1;

    public static boolean obscuredBySphere(int i, double x, double y, double z){
	double s2px = x - scx[i];
	double s2py = y - scy[i];
	double s2pz = z - scz[i];
	double dot  = s2px * light.x + s2py * light.y + s2pz * light.z;
	
	if(dot < 0.0){
	    // this sphere is between us and infinite light.
	    double projx = lightx.x * s2px + lightx.y * s2py + lightx.z * s2pz;
	    double r = scr[i];
	    if(projx < r){
		double projy = lighty.x * s2px + lighty.y * s2py + lighty.z * s2pz;
		if(projy < r && projx * projx + projy * projy < r * r){
		    return true;
		}
	    }
	}
	
	return false;
    }

    private static int lastObscuringCylinder = -1;

    private static boolean obscuredByCylinder(int i){
	c0[0] = ccachex0.get(i);
	c0[1] = ccachey0.get(i);
	c0[2] = ccachez0.get(i);
	c1[0] = ccachex1.get(i);
	c1[1] = ccachey1.get(i);
	c1[2] = ccachez1.get(i);
	
	if(astex.anasurface.AnaSurface.intersect(ray0, ray1, c0, c1, rint, nint) < ccacher.get(i)){
	    return true;
	}

	return false;
    }


    /** Is this point in another sphere. */
    public static boolean pointInSphere(double x, double y, double z){
	int sphereCount = sphereOcclusionCacheList.size();

	for(int i = 0; i < sphereCount; i++){
	    int j = sphereOcclusionCacheList.get(i);
	    double dx = x - scachex.get(j);
	    double dy = y - scachey.get(j);
	    double dz = z - scachez.get(j);
	    double r2 = scacher.get(j);

	    if(dx*dx + dy*dy + dz*dz < r2 * r2){
		return true;
	    }
	}

	return false;
    }

    /** Clear out the shadow data structures. */
    public static void clearShadowCaches(){
	//Log.info("%d spheres cleared from cache", ShadowCache.scachex.size());
	scachex.removeAllElements();
	scachey.removeAllElements();
	scachez.removeAllElements();
	scacher.removeAllElements();

	ccachex0.removeAllElements();
	ccachey0.removeAllElements();
	ccachez0.removeAllElements();
	ccachex1.removeAllElements();
	ccachey1.removeAllElements();
	ccachez1.removeAllElements();
	ccacher.removeAllElements();

	tcachex0.removeAllElements();
	tcachey0.removeAllElements();
	tcachez0.removeAllElements();
	tcachex1.removeAllElements();
	tcachey1.removeAllElements();
	tcachez1.removeAllElements();
	tcachex2.removeAllElements();
	tcachey2.removeAllElements();
	tcachez2.removeAllElements();
	//ttransp.removeAllElements();

	tcen2dx.removeAllElements();
	tcen2dy.removeAllElements();
	tcenx.removeAllElements();
	tceny.removeAllElements();
	tcenz.removeAllElements();
	tcenr.removeAllElements();
    }

    private static Point3d light = new Point3d();
    private static Point3d lightx = null;
    private static Point3d lighty = null;

    /** Set up the shadow cache data structures for renderering. */
    public static void setupShadowCaches(Light l0, double ovs){
	//Log.info("%d spheres in cache", ShadowCache.scachex.size());
	light.x = l0.pos[0];
	// light y needs to be negative to correct for 
	// on screen orientation
	light.y = -l0.pos[1];
	light.z = l0.pos[2];
	light.normalise();

	lightx = Point3d.normalToLine(light);
	lightx.normalise();
	lighty = lightx.cross(light);
	lighty.normalise();

	tx0 = tcachex0.getArray();
	ty0 = tcachey0.getArray();
	tz0 = tcachez0.getArray();
	tx1 = tcachex1.getArray();
	ty1 = tcachey1.getArray();
	tz1 = tcachez1.getArray();
	tx2 = tcachex2.getArray();
	ty2 = tcachey2.getArray();
	tz2 = tcachez2.getArray();

	prepareTriangleGrid();
	prepareSphereGrid();
	prepareCylinderGrid();

	lastObscuringSphere   = -1;
	lastObscuringCylinder = -1;
	lastObscuringTriangle = -1;

	overallScale = ovs;

	tc2x = tcen2dx.getArray();
	tc2y = tcen2dy.getArray();
	tcx = tcenx.getArray();
	tcy = tceny.getArray();
	tcz = tcenz.getArray();
	tcr = tcenr.getArray();

	scx = scachex.getArray();
	scy = scachey.getArray();
	scz = scachez.getArray();
	scr = scacher.getArray();

	//System.out.println("lightx.light " + lightx.dot(light));
	//System.out.println("lighty.light " + lighty.dot(light));
    }

    private static NeighbourGrid2D triangleGrid = new NeighbourGrid2D();
    private static NeighbourGrid2D sphereGrid   = new NeighbourGrid2D();
    private static NeighbourGrid2D cylinderGrid = new NeighbourGrid2D();

    private static void prepareTriangleGrid(){
	int triangleCount = tcachex0.size();

	if(triangleCount == 0) return;

	double xmin =  1.e10;
	double ymin =  1.e10;
	double xmax = -1.e10;
	double ymax = -1.e10;
	double rmax = 0.0;
	
	for(int i = 0; i < triangleCount; i++){
	    boundingSphereTriangle(tx0[i], ty0[i], tz0[i],
				   tx1[i], ty1[i], tz1[i],
				   tx2[i], ty2[i], tz2[i],
				   cbs);

	    // capture centre
	    tcenx.add((float)cbs[0]);
	    tceny.add((float)cbs[1]);
	    tcenz.add((float)cbs[2]);
	    tcenr.add((float)cbs[3]);

	    // project tri center onto light orthonormal set
	    double x = cbs[0] * lightx.x + cbs[1] * lightx.y + cbs[2] * lightx.z;
	    double y = cbs[0] * lighty.x + cbs[1] * lighty.y + cbs[2] * lighty.z;

	    // capture projected centre
	    tcen2dx.add((float)x);
	    tcen2dy.add((float)y);

	    // can do this as we add triangles in principle
	    // gather extents
	    if(x < xmin) xmin = x;
	    if(x > xmax) xmax = x;
	    if(y < ymin) ymin = y;
	    if(y > ymax) ymax = y;
	    if(cbs[3] > rmax) rmax = cbs[3];
	}

	if(false){
	    FILE.out.print("xmin %6.1f ", xmin);
	    FILE.out.print("xmax %6.1f\n", xmax);
	    FILE.out.print("ymin %6.1f ", ymin);
	    FILE.out.print("ymax %6.1f\n", ymax);
	    FILE.out.print("rmax %6.1f\n", rmax);
	}

	// make the bounding box for the grid.
	triangleGrid.reset(xmin - 0.1, ymin - 0.1,
			   xmax + 0.1, ymax + 0.1, 1.01 * rmax);

	tcx = tcen2dx.getArray();
	tcy = tcen2dy.getArray();

	for(int i = 0; i < triangleCount; i++){
	    triangleGrid.add(i, tcx[i], tcy[i]);
	}
    }

    private static void prepareSphereGrid(){
	int sphereCount = scachex.size();

	if(sphereCount == 0) return;

	double xmin =  1.e10;
	double ymin =  1.e10;
	double xmax = -1.e10;
	double ymax = -1.e10;
	double rmax = 0.0;

	scx = scachex.getArray();
	scy = scachey.getArray();
	scz = scachez.getArray();
	scr = scacher.getArray();
	
	for(int i = 0; i < sphereCount; i++){

	    // project tri center onto light orthonormal set
	    double x = scx[i] * lightx.x + scy[i] * lightx.y + scz[i] * lightx.z;
	    double y = scx[i] * lighty.x + scy[i] * lighty.y + scz[i] * lighty.z;

	    // can do this as we add triangles in principle
	    // gather extents
	    if(x < xmin) xmin = x;
	    if(x > xmax) xmax = x;
	    if(y < ymin) ymin = y;
	    if(y > ymax) ymax = y;
	    if(scr[i] > rmax) rmax = scr[i];
	}

	if(false){
	    FILE.out.print("xmin %6.1f ", xmin);
	    FILE.out.print("xmax %6.1f\n", xmax);
	    FILE.out.print("ymin %6.1f ", ymin);
	    FILE.out.print("ymax %6.1f\n", ymax);
	    FILE.out.print("rmax %6.1f\n", rmax);
	}

	// make the bounding box for the grid.
	sphereGrid.reset(xmin - 0.1, ymin - 0.1,
			 xmax + 0.1, ymax + 0.1, 1.01 * rmax);

	for(int i = 0; i < sphereCount; i++){
	    // project tri center onto light orthonormal set
	    double x = scx[i] * lightx.x + scy[i] * lightx.y + scz[i] * lightx.z;
	    double y = scx[i] * lighty.x + scy[i] * lighty.y + scz[i] * lighty.z;

	    sphereGrid.add(i, x, y);
	}
    }

    private static void prepareCylinderGrid(){
	int cylinderCount = ccachex0.size();

	if(cylinderCount == 0) return;

	double xmin =  1.e10;
	double ymin =  1.e10;
	double xmax = -1.e10;
	double ymax = -1.e10;
	double rmax = 0.0;
	
	for(int i = 0; i < cylinderCount; i++){
	    boundingSphereCylinder(ccachex0.get(i), ccachey0.get(i), ccachez0.get(i),
				   ccachex1.get(i), ccachey1.get(i), ccachez1.get(i),
				   ccacher.get(i),
				   cbs);
	    // project tri center onto light orthonormal set
	    double x = cbs[0] * lightx.x + cbs[1] * lightx.y + cbs[2] * lightx.z;
	    double y = cbs[0] * lighty.x + cbs[1] * lighty.y + cbs[2] * lighty.z;

	    // can do this as we add cylinders in principle
	    // gather extents
	    if(x < xmin) xmin = x;
	    if(x > xmax) xmax = x;
	    if(y < ymin) ymin = y;
	    if(y > ymax) ymax = y;
	    if(cbs[3] > rmax) rmax = cbs[3];
	}

	// make the bounding box for the grid.
	cylinderGrid.reset(xmin - 0.1, ymin - 0.1,
			   xmax + 0.1, ymax + 0.1, 1.01 * rmax);

	tcx = tcen2dx.getArray();
	tcy = tcen2dy.getArray();

	for(int i = 0; i < cylinderCount; i++){
	    boundingSphereCylinder(ccachex0.get(i), ccachey0.get(i), ccachez0.get(i),
				   ccachex1.get(i), ccachey1.get(i), ccachez1.get(i),
				   ccacher.get(i),
				   cbs);
	    // project tri center onto light orthonormal set
	    double x = cbs[0] * lightx.x + cbs[1] * lightx.y + cbs[2] * lightx.z;
	    double y = cbs[0] * lighty.x + cbs[1] * lighty.y + cbs[2] * lighty.z;

	    cylinderGrid.add(i, x, y);
	}
    }

    // Ray-triangle intersection
    // Tomas Möller and Ben Trumbore.
    // Fast, minimum storage ray-triangle intersection.
    // Journal of graphics tools, 2(1):21-28, 1997
    // Source code from
    // http://www.acm.org/jgt/papers/MollerTrumbore97/code.html

    private static final double EPSILON = 0.000001;
    
    private static void CROSS(double dest[], double v1[], double v2[]){
	dest[0]=v1[1]*v2[2]-v1[2]*v2[1];
	dest[1]=v1[2]*v2[0]-v1[0]*v2[2];
	dest[2]=v1[0]*v2[1]-v1[1]*v2[0];
    }

    private static double DOT(double v1[], double v2[]){
	return v1[0]*v2[0]+v1[1]*v2[1]+v1[2]*v2[2];
    }

    private static void SUB(double dest[], double v1[], double v2[]){
	dest[0]=v1[0]-v2[0];
	dest[1]=v1[1]-v2[1];
	dest[2]=v1[2]-v2[2];
    }

    private static double edge1[] = new double[3];
    private static double edge2[] = new double[3];
    private static double tvec[] = new double[3];
    private static double pvec[] = new double[3];
    private static double qvec[] = new double[3];

    public static int intersect_triangle(double orig[], double dir[],
					 double vert0[], double vert1[], double vert2[],
					 double tuv[]){
	double det, inv_det;

	// find vectors for two edges sharing vert0
	SUB(edge1, vert1, vert0);
	SUB(edge2, vert2, vert0);

	// begin calculating determinant - also used to calculate U parameter
	CROSS(pvec, dir, edge2);
	
	// if determinant is near zero, ray lies in plane of triangle
	det = DOT(edge1, pvec);
	
	if(false){
	    if (det < EPSILON)
		return 0;

	    // calculate distance from vert0 to ray origin
	    SUB(tvec, orig, vert0);
	    
	    // calculate U parameter and test bounds
	    tuv[1] = DOT(tvec, pvec);
	    if (tuv[1] < 0.0 || tuv[1] > det)
		return 0;
	    
	    // prepare to test V parameter
	    CROSS(qvec, tvec, edge1);
	    
	    // calculate V parameter and test bounds
	    tuv[2] = DOT(dir, qvec);
	    if (tuv[2] < 0.0 || tuv[1] + tuv[2] > det)
		return 0;
	    
	    // calculate t, scale parameters, ray intersects triangle
	    tuv[0] = DOT(edge2, qvec);
	    inv_det = 1.0 / det;
	    tuv[0] *= inv_det;
	    tuv[1] *= inv_det;
	    tuv[2] *= inv_det;
	}else{                    // the non-culling branch
	    if (det > -EPSILON && det < EPSILON)
		return 0;
	    inv_det = 1.0 / det;
	    
	    // calculate distance from vert0 to ray origin
	    SUB(tvec, orig, vert0);
	    
	    // calculate U parameter and test bounds
	    tuv[1] = DOT(tvec, pvec) * inv_det;
	    if (tuv[1] < 0.0 || tuv[1] > 1.0)
		return 0;
	    
	    // prepare to test V parameter
	    CROSS(qvec, tvec, edge1);

	    // calculate V parameter and test bounds
	    tuv[2] = DOT(dir, qvec) * inv_det;
	    if (tuv[2] < 0.0 || tuv[1] + tuv[2] > 1.0)
		return 0;
	    
	    // calculate t, ray intersects triangle
	    tuv[0] = DOT(edge2, qvec) * inv_det;
	}
	return 1;
    }

    public static void main(String args[]){
	double a[] = { 0., 0., 0. };
	double b[] = { 1., 0., 0. };
	double c[] = { 0., 1., 0. };
	double tuv[] = new double[3];


	double o[] = { 0.2, 0.2, 1. };
	double d[] = { 0., 0., 1. };

	int i = intersect_triangle(o, d, a, b, c, tuv);

	if(i == 1){
	    FILE.out.print("t %f\n", tuv[0]);
	}
    }
}
