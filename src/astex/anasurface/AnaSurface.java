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

package astex.anasurface;

/*
 * Implementation of analytical molecular surface algorithm.
 *
 */

import astex.*;

import java.io.*;

public class AnaSurface {
    /* Input coordinates and radii. */
    public double xyz[][] = null;
    public double radius[] = null;
    public double radius2[] = null;
    public double rsq[] = null;
    public int visible[] = null;
    public int colors[] = null;
    public DynamicArray edgeList[] = null;
    public DynamicArray probeList[] = null;
    public DynamicArray faceList[] = null;
    public DynamicArray vertexList[] = null;
    public DynamicArray torusList[] = null;
    public int nxyz = 0;

    /** Is debugging on? */
    public static boolean debug = false;

    /** The default probe radius for the surface class. */
    public static double defaultProbeRadius = 1.5;

    /** The default quality setting for the surface. */
    public static int defaultQuality = 1;

    /** The probe radius for the surface. */
    public double probeRadius = 1.5;

    /** The tesselation depth for sphere template. */
    public int density = 1;

    /** Write the probes out to a file. */
    public String probesFilename = null;

    /** Probe placements. */
    DynamicArray probes = new DynamicArray(1024);

    /** The vertices on the molecular surface. */
    DynamicArray vertices = new DynamicArray(1024);

    /** The edges of the molecular surface. */
    DynamicArray edges = new DynamicArray(1024);

    /** The faces of the molecular surface. */
    DynamicArray faces = new DynamicArray(1024);

    /** The torii of the molecular surface. */
    DynamicArray torii = new DynamicArray(1024);

    /** The number of torii with a single face. */
    public int singleFace = 0;

    /** The maximum number of edges on a single torus. */
    public int maximumTorusEdges = 0;

    /** The maximum number of edges on a single convex face. */
    public int maximumFaceEdges = 0;

    /** The number of self intersecting torii. */
    public int selfIntersectingTorii = 0;

    /** The number of self intersecting torii. */
    public int selfIntersectingProbes = 0;

    /** The number of distance comparisons made. */
    public int distanceComparisons = 0;

    /** The tmesh object that will hold the final surface. */
    public Tmesh tmesh = new Tmesh();

    /** Desired length of triangle edges. */
    // This is actually the probe separation along the
    // the torus center at the minute
    public double desiredTriangleLength = 1.5;

    /** The target length for toroidal triangles. */
    public double targetLen = 0.0;

    /** The time the surface generation started. */
    long startTime = 0;

    /** The current Color for the triangle. */
    private int currentColor = 0;

    /** The current Color for the triangle. */
    public int backgroundColor = Color32.white;

    /** Edge lengths for different qualities. */
    double qLength[] = {0.0, 1.5, 0.9, 0.5, 0.3};

    /** Default constructor. */
    public AnaSurface(double x[][], double r[],
        int visible[], int colors[], int n){
 this.xyz = x;
 this.radius = r;
 this.visible = visible;
 this.colors = colors;
 this.nxyz = n;

 this.probeRadius = defaultProbeRadius;

 int quality = defaultQuality;

 if(quality > 0 && quality < qLength.length){
     this.density = defaultQuality;
     this.desiredTriangleLength = qLength[quality];
 }
        //tmesh.style = Tmesh.DOTS;
 //tmesh.setColorStyle(Tmesh.TriangleColor);
    }

    /** Construct the surface. */
    public Tmesh construct(){

 long startTime = System.currentTimeMillis();

 initialise();

 long then = System.currentTimeMillis();

 buildNeighbourList();

 print("# neighbour list generation time (ms)",
       (int)(System.currentTimeMillis() - then));

 then = System.currentTimeMillis();

 constructProbePlacements();

 print("# probe generation time (ms)",
       (int)(System.currentTimeMillis() - then));

 currentColor = backgroundColor;

 processTorii();

 triangulate();

 triangulateAtoms();

 int surfaceAtoms = 0;
 int maximumVertices = 0;
 for(int i = 0; i < nxyz; i++){
     if(edgeList[i] != null){
  surfaceAtoms++;
  if(edgeList[i].size() > maximumVertices){
      maximumVertices = edgeList[i].size();
  }
     }
 }

 print("surfaced atoms", surfaceAtoms);
 print("maximum vertices ", maximumVertices);
 print("total probe placements", probes.size());
 print("total vertices", vertices.size());
 print("total edges", edges.size());
 print("total faces", faces.size());
 print("single face torii", singleFace);
 print("maximum torus edges", maximumTorusEdges);
 print("maximum face edges", maximumFaceEdges);
 print("self intersecting torii", selfIntersectingTorii);
 print("self intersecting probes", selfIntersectingProbes);
 print("distance comparisons", distanceComparisons);
 print("total memory (Mb)",
       (int)(Runtime.getRuntime().totalMemory()/1000.));

 if(probesFilename != null){
     outputProbes(probes, probesFilename);
 }

 print("points in tmesh", tmesh.np);
 print("triangles in tmesh", tmesh.nt);
 print("(2n-4)", 2*tmesh.np-4);

 print("# total surface generation time (s)",
       (float)(System.currentTimeMillis() - startTime)*0.001);


 System.out.println("starting decusp");
 deCuspSurface(tmesh);
 System.out.println("done");

 tmesh.setColorStyle(Tmesh.VertexColor);

 return tmesh;
    }

    private void deCuspSurface(Tmesh tmesh){
 int probeCount = probes.size();

 // build probe lattice
 Lattice l = new Lattice(probeRadius * 2.0);

 for(int p = 0; p < probeCount; p++){
     Probe probe = (Probe)probes.get(p);

     l.add(p, probe.x[0], probe.x[1], probe.x[2]);
 }

 // measure distance to nearest probe center
 IntArray neighbours = new IntArray();

 for(int i = 0; i < tmesh.np; i++){
            tmesh.v[i] = 0.0001f;
     neighbours.removeAllElements();
     l.getPossibleNeighbours(-1,
        tmesh.x[i], tmesh.y[i], tmesh.z[i],
        neighbours, true);
     int neighbourCount = neighbours.size();

     double dmin = probeRadius;

     for(int j = 0; j < neighbourCount; j++){
  int p = neighbours.get(j);
  Probe probe = (Probe)probes.get(p);
  double d = distance(probe.x[0], probe.x[1], probe.x[2],
        tmesh.x[i], tmesh.y[i], tmesh.z[i]);
  if(d < dmin){
      dmin = d;
  }
     }

     tmesh.v[i] = (float)(probeRadius - dmin);
 }

        if(true){
            return;
        }

 // now put the self intersecting torii in
 l = new Lattice(3.0 * probeRadius);

 int torusCount = torii.size();

 System.out.println("torus count "+ torusCount);

 double tmin = 1.e10;
 double tmax = -1.e10;

 for(int t = 0; t < torusCount; t++){
     Torus torus = (Torus)torii.get(t);
     if(torus.selfIntersects){
  l.add(t, torus.tij[0], torus.tij[1], torus.tij[2]);
     }
 }

 for(int i = 0; i < tmesh.np; i++){
     neighbours.removeAllElements();
     l.getPossibleNeighbours(-1,
        tmesh.x[i], tmesh.y[i], tmesh.z[i],
        neighbours, true);
     int neighbourCount = neighbours.size();

     double dmin = 1.0;

     for(int j = 0; j < neighbourCount; j++){
  int t = neighbours.get(j);
  Torus torus = (Torus)torii.get(t);
  double d = torusRadiusDistance(torus,
            tmesh.x[i],
            tmesh.y[i],
            tmesh.z[i]);
  d -= probeRadius;

  //FILE.out.print("d %.2f\n", d);


  if(d < dmin){
      dmin = d;
  }
     }

     if(dmin < tmin){
  tmin = dmin;
     }

     if(dmin > tmax){
  tmax = dmin;
     }

     if(dmin < tmesh.v[i]){
      //tmesh.v[i] = (float)dmin;
     }
 }

 FILE.out.print("tmin %.2f ", tmin);
 FILE.out.print("tmax %.2f\n", tmax);

 for(int i = 0; i < tmesh.np; i++){
     //tmesh.v[i] += 0.2;
     //tmesh.v[i] += 0.02;
     if(tmesh.v[i] < 0.0){
  //FILE.out.print("v %.2f\n", tmesh.v[i]);
     }
 }
    }

    private static double meshp[] = new double[3];

    /** Find distance from the point to nerest point on torus radius. */
    private double torusRadiusDistance(Torus torus,
           double x, double y, double z){
 meshp[0] = x;
 meshp[1] = y;
 meshp[2] = z;

 double pe = plane_eqn(meshp, torus.tij, torus.uij);
 double d = distance(meshp, torus.tij);

 double d1 = Math.sqrt(d*d - pe*pe) - torus.rij;

 double trd = Math.sqrt(pe * pe + d1 * d1);

 return trd;
    }

    /** Perform various setup tasks. */
    private void initialise(){
 // create the array for r + probeRadius
 radius2 = new double[nxyz];

 rsq = new double[nxyz];

 for(int i = 0; i < nxyz; i++){
     radius2[i] = radius[i] + probeRadius;
     rsq[i] = radius2[i] * radius2[i];
 }

 // space for keeping the edge lists
 edgeList = new DynamicArray[nxyz];

 // space for keeping the probe lists
 probeList = new DynamicArray[nxyz];

 // space for keeping the probe lists
 faceList = new DynamicArray[nxyz];

 // space for keeping the vertex lists
 vertexList = new DynamicArray[nxyz];

 // space for keeping the torus lists
 torusList = new DynamicArray[nxyz];

 // build target length approximation for
 // diagonal across toroidal triangles
 targetLen = desiredTriangleLength * desiredTriangleLength;
 targetLen = Math.sqrt(0.5 * targetLen);

 buildSphereTemplate(this.density);
    }

    public void triangulateAtoms(){
 for(int ia = 0; ia < nxyz; ia++){
     currentColor = backgroundColor;

     if(colors != null){
  currentColor = colors[ia];
     }

     transformSphere(xyz[ia], radius[ia]);

     triangulateSphere(ia);
 }
    }

    /** Perform the various parts of the triangulation process. */
    public void triangulate(){

 int faceCount = faces.size();

 // do the torus faces first, as they define edge
 // points for all other faces.
 for(int i = faceCount - 1; i >= 0; i--){
     Face f = (Face)faces.get(i);

     int edgeCount = f.size();

     if(edgeCount == 4 && f.type == Face.Saddle){
  processToroidalFace(f);
     }
 }

 for(int i = 0; i < faceCount; i++){
     Face f = (Face)faces.get(i);
     int edgeCount = f.size();

     if(!(edgeCount == 4 && f.type == Face.Saddle)){
  if(f.skip == false){
      processFace(f);
  }
     }
 }
    }

    /** Process a single face. */
    public void processFace(Face f){
 int edgeCount = f.size();

 if(f.type == Face.Undefined){
     processUndefinedFace(f);
 }else if(edgeCount == 4 && f.type == Face.Saddle){
     processToroidalFace(f);
 }else{
     processIrregularFace(f, -1);
 }
    }

    /** Process simple face from cusp trimming. */
    private void processUndefinedFace(Face f){
 if(f.size() != 3){
     System.out.println("undefined face has edges " + f.size());
 }

 Edge e0 = (Edge)f.get(0);
 Edge e1 = (Edge)f.get(1);
 Edge e2 = (Edge)f.get(2);

 currentColor = f.type;
 meshAddTriangle(e0.v0.vi, e1.v0.vi, e2.v0.vi);
    }

    /** Process irregular spherical face. */
    private void processIrregularFace(Face f, int ia){
 if(f.cen == null){
     System.out.println("face has null center, skipping");
     return;
 }

 if(f.type == Face.Concave){
     transformSphere(f.cen, f.r);
 }

 if(f.type != Face.Undefined){
     clipSphere(f, ia);

     addWholeTriangles(f);
 }

 processConvexFace(f);
    }

    /** Edges on the sphere. */
    Edge sphereEdges[] = new Edge[100];
    boolean used[] = new boolean[100];
    Face convexFace = new Face(Face.Convex);
    Face sphereFace = new Face(Face.Convex);
    int scount = 0;

    /** The final triangulation stage for the remaining sphere surface. */
    private void triangulateSphere(int ia){
 if(edgeList[ia] == null){
     //System.out.println("attempt to surface sphere with no edges.");
     return;
 }

 convexFace.type = Face.Convex;
 copy(xyz[ia], convexFace.cen);
 convexFace.r = radius[ia];

 int ecount = edgeList[ia].size();
 scount = 0;

 sphereFace.removeAllElements();

 for(int i = 0; i < ecount; i++){
     Edge e = (Edge)edgeList[ia].get(i);
     if(e.v0.i == ia && e.v1.i == ia){
  // edge is on atom
  sphereFace.add(e);
     }
 }

 int unusedEdges = sphereFace.size();

 for(int i = 0; i < unusedEdges; i++){
     used[i] = false;
 }

 while(unusedEdges != 0){
     convexFace.removeAllElements();
     Edge firstEdge = null;
     Edge lastEdge = null;
     Edge previousEdge = null;
     boolean addedEdge = false;

     do {
  addedEdge = false;

  for(int i = 0; i < sphereFace.size(); i++){

      if(!used[i]){
   Edge currentEdge = (Edge)sphereFace.get(i);

   if(convexFace.size() > 0){
       previousEdge =
    (Edge)convexFace.getReverse(0);

       if(previousEdge.v1.vi == currentEdge.v0.vi){
    convexFace.add(currentEdge);
    used[i] = true;
    unusedEdges--;
    addedEdge = true;
    break;
       }
   }else{
       convexFace.add(currentEdge);
       used[i] = true;
       unusedEdges--;
       addedEdge = true;
       break;
   }
      }
  }

  firstEdge = (Edge)convexFace.get(0);
  lastEdge = (Edge)convexFace.getReverse(0);
     } while(lastEdge.v1.vi != firstEdge.v0.vi && addedEdge != false);

     if(addedEdge == false){
  System.out.println("failed to extend contact face");
  sphereFace.print("faulty sphere face");
     }else{
  if(convexFace.size() > 0){
      processIrregularFace(convexFace, ia);
  }
     }
 }
    }

    private void processConvexFace(Face f){
 //convexFace.print("convexFace");

 vArray.removeAllElements();
 eArray.removeAllElements();

 if(f.type == Face.Concave && f.size() > 3){
     System.out.println("concave edge count "+ f.size());
 }

 for(int i = 0; i < 10000; i++){
     euse[i] = 0;
 }

 int edgeCount = f.size();

 if(edgeCount > maximumFaceEdges){
     maximumFaceEdges = edgeCount;
 }

 if(edgeCount > 31){
     System.out.println("aaagh! more than 31 edges on a face");
     System.exit(1);
 }

 for(int i = 0; i < edgeCount; i++){
     Edge e = (Edge)f.get(i);
     int nv = e.size();

     if(nv == 0){
  System.out.println("Edge has no vertices!!!!!\n!!!!\n!!!!");
     }

     // build bit mask of face edges that this
     // vertex is on

     for(int j = 0; j < nv-1; j++){

  if(debug){
      FILE.out.print("adding to vlist %4d\n", e.get(j));
  }

  vArray.add(e.get(j));
  int edgeMask = (1 << i);

  if(j == 0){
      int prevEdge = i - 1;
      if(prevEdge == -1){
   prevEdge = edgeCount - 1;
      }
      edgeMask |= (1 << prevEdge);
  }

  eArray.add(edgeMask);
     }
 }

 int nv = vArray.size();
 int vlist[] = vArray.getArray();
 int elist[] = eArray.getArray();

 if(debug){
     for(int i = 0; i < nv; i++){
  FILE.out.print("v[%04d] = ", i);
  FILE.out.print("%04d mask ", vlist[i]);
  for(int ee = 10; ee >= 0; ee--){
      if((elist[i] & (1 << ee)) != 0){
   System.out.print("1");
      }else{
   System.out.print("0");
      }
  }
  System.out.println("");
     }
 }

 ecount = 0;

 // add boundary edges to edge list.
 for(int i = 0; i < nv; i++){
     int i1 = i + 1;
     if(i1 == nv){
  i1 = 0;
     }

     int v0 = vlist[i];
     int v1 = vlist[i1];

     if(debug){
  FILE.out.print("checking edge %4d", v0);
  FILE.out.print(" %4d\n", v1);
     }

     if(elist[i] != 0 && elist[i1] != 0){
  addEdgePair(v0, v1, false);
     }
 }

 if(debug)System.out.println("after edges edge count " + ecount);

 if(f.type != Face.Undefined){
     for(int i = 0; i < nsp; i++){
  if(hull[i] == 1){
      // always needs putting in list.

      vArray.add(clipped[i]);
      eArray.add(0);
  }
     }

     // also add the boundary edges to the 
     for(int i = 0; i < nst; i++){
  addBoundaryEdgeIfNeeded(si[i], sj[i]);
  addBoundaryEdgeIfNeeded(sj[i], sk[i]);
  addBoundaryEdgeIfNeeded(sk[i], si[i]);
     }
 }

 if(debug) System.out.println("after boundary edge count " + ecount);

 if(debug) System.out.println("after clipping point count " +
         vArray.size());

 addTriangles(f);
    }

    /** See if this edge is on the boundary. */
    private void addBoundaryEdgeIfNeeded(int svi, int svj){
 if(hull[svi] == 1 && hull[svj] == 1){
     if(getHullCount(svi) < 3 && getHullCount(svj) < 3){
  //System.out.println("adding boundary edge from interior");
  //XXX
  addEdgePair(svi, svj, true);
  //addEdgePair(clipped[svi], clipped[svj], true);
     }
 }
    }

    /** The pairs of vertex indices. */
    int v0[] = new int[10000];
    int v1[] = new int[10000];
    int euse[] = new int[10000];
    int ecount = 0;

    /** Return how many times the edge v0-v1 is used. */
    public int edgePairCount(int vv0, int vv1){
 if(vv0 > vv1){
     // swap vertex pair
     int tmp = vv0;
     vv0 = vv1;
     vv1 = tmp;
 }

 for(int i = 0; i < ecount; i++){
     if(v0[i] == vv0 && v1[i] == vv1){
  return euse[i];
     }
 }

 return 0;
    }

    public int addEdgePair(int vv0, int vv1, boolean lookup){
 if(vv0 == vv1){
     Log.error("vv0 == vv1");
 }

 if(debug){
     System.out.println("addEdgePair " + vv0 + " " + vv1);
 }

 if(vv0 > vv1){
     // swap vertex pair
     int tmp = vv0;
     vv0 = vv1;
     vv1 = tmp;
 }

 if(lookup){
     for(int i = 0; i < ecount; i++){
  if(v0[i] == vv0 && v1[i] == vv1){
      if(debug){
   FILE.out.print("addEdgePair %4d", vv0);
   FILE.out.print(" %4d", vv1);
   FILE.out.print(" euse %d\n", euse[i]);
      }

      if(euse[i] >= 2){
   FILE.out.print("### edge already used twice %4d", vv0);
   FILE.out.print(" %4d\n", vv1);
   //Exception e = new Exception();
   //e.printStackTrace();
      }
      euse[i]++;
      return i;
  }
     }
 }

 v0[ecount] = vv0;
 v1[ecount] = vv1;
 euse[ecount] = 1;

 return ecount++;
    }

    /** The list of edge vertices we have to triangulate. */
    IntArray vArray = new IntArray(64);

    /** The list of edge ids associated with the vertices. */
    IntArray eArray = new IntArray(64);

    public double pp0[] = new double[3];
    public double pp1[] = new double[3];
    public double pp2[] = new double[3];
    public double pp3[] = new double[3];

    public double npp0[] = new double[3];
    public double npp1[] = new double[3];
    public double npp2[] = new double[3];
    public double npp3[] = new double[3];

    public double circum[] = new double[3];

    /** Triangulate an arbitrary collection of edges and interior points. */
    public void addTriangles(Face f){
 int nv = vArray.size();
 int vlist[] = vArray.getArray();
 int elist[] = eArray.getArray();

 double rlim = currentLongestEdge * 1.5;
 rlim *= rlim;

 if(debug){
     System.out.println("vertex list");
     for(int i = 0; i < nv; i++){
  FILE.out.print("v[%03d] = ", i);
  FILE.out.print("%03d\n", vlist[i]);
     }
     System.out.println("vertex list end");
 }

 // find maximum edge vertex (i.e. not from sphere template)

 int nev = 0;

 for(int i = 0; i < nv; i++){
     if(elist[i] == 0){
  nev = i;
  break;
     }
 }

 if(debug) System.out.println("nev " + nev);

 for(int iteration = 0; iteration < 3; iteration++){

     for(int i = 0; i < nv - 2; i++){
  int vi = vlist[i];
  tmesh.getVertex(vi, pp0, npp0);

  for(int j = i+1; j < nv - 1; j++){
      int vj = vlist[j];

      // if on same edge, only allow if
      // one vertex apart
      if(debug) FILE.out.print("checking edge %3d ", vi);
      if(debug) FILE.out.print("%3d\n", vj);

      if((elist[i] & elist[j]) != 0){
   int ji = j - i;
   // check against maximum exterior edge vertex (nev)
   if(ji > 1 && ji < nev - 1){
       if(debug){
    System.out.println("skipping points not edge neighbours");
    System.out.println("and " + (elist[i] & elist[j]));
    System.out.println("j - i " + (j - i));
       }
       continue;
   }
      }

      tmesh.getVertex(vj, pp1, npp1);

      if(distance2(pp0, pp1) > rlim){
   if(debug) System.out.println("skipping edge points not close enough");
   continue;
      }

      for(int k = j+1; k < nv; k++){
   int vk = vlist[k];

   if(debug){
       FILE.out.print("checking %3d ", vi);
       FILE.out.print("%3d ", vj);
       FILE.out.print("%3d\n", vk);
   }

   int perimeterCount = 0;
   if(elist[i] != 0) perimeterCount++;
   if(elist[j] != 0) perimeterCount++;
   if(elist[k] != 0) perimeterCount++;

   // conditions for accepting triangles
   // can't have 3 points from interior
   if(perimeterCount == 0){
       if(debug) System.out.println("skipping all interior");
       continue;
   }

   // can't have 3 points on same edge
   if(((elist[i] & elist[j]) & elist[k]) != 0){
       if(debug) System.out.println("skipping all same edge");
       continue;
   }

   // conditions for accepting triangles
   // can't have 3 edge points (maybe only first pass)
   if(iteration < 2 && perimeterCount == 3){
       if(debug) System.out.println("skipping all perimeter");
       continue;
   }
   tmesh.getVertex(vk, pp2, npp2);

   if(distance2(pp1, pp2) > rlim ||
      distance2(pp2, pp0) > rlim){
       if(debug) System.out.println("skipping edges too long");
       continue;
   }

   boolean tok = true;

   double rc = circumCircle(circum, pp0, pp1, pp2);

   if(rc == Double.POSITIVE_INFINITY){
       if(debug) System.out.println("## no solution for circumCircle");
       continue;
   }

   rc *= rc;

   for(int l = 0; l < nv; l++){
       if(l != i && l != j && l != k){
    int vl = vlist[l];
    tmesh.getVertex(vl, pp3, null);

    if(distance2(circum, pp3) < rc){
        if(debug) System.out.println("skipping delaunay violation");
        tok = false;
        break;
    }
       }
   }

   if(tok){
       if(edgePairCount(vj, vk) == 2){
    if(debug) System.out.println("skipping edges used");
    continue;
       }
       if(edgePairCount(vk, vi) == 2){
    if(debug) System.out.println("skipping edges used");
    continue;
       }
       if(edgePairCount(vi, vj) == 2){
    if(debug) System.out.println("skipping edges used");
    continue;
       }

       addIfAcceptable(vi, pp0, npp0,
         vj, pp1, npp1,
         vk, pp2, npp2, f);
   }
      }
  }
     }
 }
        if(debug) System.out.println("end");
        if(debug) printIfNotComplete();
    }

    private void printIfNotComplete(){
 boolean printit = false;
 for(int i = 0; i < ecount; i++){
     if(v0[i] > v1[i]){
  System.out.println("error disordered edge");
     }
     if(euse[i] != 2){
  printit = true;
  break;
     }
 }

 if(printit){
     System.out.println("vertex count " + vArray.size());
     for(int i = 0; i < ecount; i++){
  if(euse[i] != 2){
      FILE.out.print("edge[%03d] =", i);
      FILE.out.print(" %4d", v0[i]);
      FILE.out.print(",%4d", v1[i]);
      FILE.out.print(" count = %d\n", euse[i]);
  }
     }
 }
    }

    private static final double angleTol = Math.PI / 2.5;

    double tcp[] = new double[3];

    /** Perform some sanity checking of the new triangle. */
    public void addIfAcceptable(int vi, double ppi[], double nppi[],
    int vj, double ppj[], double nppj[],
    int vk, double ppk[], double nppk[],
    Face f){
 if(false){
     if(f.type == Face.Concave && f.size() > 3){
  currentColor = 5;
     }else{
  currentColor = f.type;
     }
 }

 if(vi == 169 && vj == 654 && vk == 655){
     Exception e = new Exception();
     e.printStackTrace();
 }

 int ec0 = edgePairCount(vi, vj);
 int ec1 = edgePairCount(vj, vk);
 int ec2 = edgePairCount(vk, vi);

 if(ec0 < 2 && ec1 < 2 && ec2 < 2){

     addEdgePair(vi, vj, true);
     addEdgePair(vj, vk, true);
     addEdgePair(vk, vi, true);

     meshAddTriangle(vi, vj, vk);
 }else{
     FILE.out.print("#### try to add triangle where edges are in use\n");
     FILE.out.print("%4d ", vi);
     FILE.out.print("%4d ", vj);
     FILE.out.print("%4d\n", vk);
     if(ec0 < 2){
  FILE.out.print("edge %4d-", vi);
  FILE.out.print("%4d\n", vj);
     }
     if(ec1 < 2){
  FILE.out.print("edge %4d-", vj);
  FILE.out.print("%4d\n", vk);
     }
     if(ec2 < 2){
  FILE.out.print("edge %4d-", vk);
  FILE.out.print("%4d\n", vi);
     }
 }

 if(false){
     boolean ok = true;

     normal(tcp, ppi, ppj, ppk);
     normalise(tcp);

     if(dot(tcp, nppi) < 0.0 &&
        dot(tcp, nppj) < 0.0 &&
        dot(tcp, nppk) < 0.0){
  negate(tcp);
     }

     if(angle(tcp, nppi) < angleTol &&
        angle(tcp, nppj) < angleTol &&
        angle(tcp, nppk) < angleTol){
  meshAddTriangle(vi, vj, vk);
     }
 }
    }

    /**
     * Yes this really is how to calculate the circum center
     * and radius of three points in 3d.
     *
     * Adapted from Graphics Gems.
     */
    public double circumCircle(double cc[],
          double p1[], double p2[], double p3[]) {
 double d1 = 0.0;
 double d2 = 0.0;
 double d3 = 0.0;

 for(int i = 0; i < 3; i++){
     d1 += (p3[i]-p1[i])*(p2[i]-p1[i]);
     d2 += (p3[i]-p2[i])*(p1[i]-p2[i]);
     d3 += (p1[i]-p3[i])*(p2[i]-p3[i]);
 }

 double c1 = d2*d3;
 double c2 = d1*d3;
 double c3 = d1*d2;
 double c = c1 + c2 + c3;
 double ccc = 2. * c;
 double c2c3 = (c2+c3)/ccc;
 double c3c1 = (c3+c1)/ccc;
 double c1c2 = (c1+c2)/ccc;

 for(int i = 0; i < 3; i++){
     cc[i] = (c2c3*p1[i] + c3c1*p2[i] + c1c2*p3[i]);
 }

 return distance(cc, p1);
    }

    private void printList(String s, int list[], int n){
 System.out.print(s);
 for(int i = 0; i < nxyz; i++){
     System.out.print(" " + list[i]);
 }
 System.out.println("");
    }

    int unclipped[] = new int[3];
    int lastunclipped[] = new int[3];
    int leuc[] = new int[3];

    int debugColor[] = {
 0xff781e,
 0x4bc3ff,
 0x37ffc3,
 0xaaff00,
    };

    public void meshAddTriangle(int v0, int v1, int v2){
 if(v0 == 0 && v1 == 0 && v2 == 0){
     try {
  Exception e = new RuntimeException("");
  e.printStackTrace();
     }catch(Exception e){
     }
 }

 //tmesh.addTriangle(v0, v1, v2, currentColor);

 tmesh.addTriangle(v0, v1, v2,
     debugColor[tmesh.nt % debugColor.length]);
    }

    /** Is this triangle clipped by probes. */
    public boolean clippedByProbes(int v0, int v1, int v2){
 if(clippingProbes != null){
     int cpc = clippingProbes.size();
     //System.out.println("clipping probe count " + cpc);
     for(int i = 0; i < cpc; i++){
  Probe p = (Probe)clippingProbes.get(i);
  if(tmesh.distance(v0, p.x) < p.r &&
     tmesh.distance(v1, p.x) < p.r &&
     tmesh.distance(v2, p.x) < p.r){
      System.out.println("!! triangle removed by clipping");
      return true;
  }
     }
 }
 return false;
    }

    public void addTriangle(int t){
 int sit = si[t];
 int sjt = sj[t];
 int skt = sk[t];

 int v0 = tmesh.addPoint(tsx[sit], snx[sit], 0.0, 0.0);
 int v1 = tmesh.addPoint(tsx[sjt], snx[sjt], 0.0, 0.0);
 int v2 = tmesh.addPoint(tsx[skt], snx[skt], 0.0, 0.0);

 tmesh.addTriangle(v0, v1, v2);
    }

    /* Working space for doubles. */
    private double p13[] = new double[3];
    private double p43[] = new double[3];
    private double p21[] = new double[3];

    private static double EPS = 1.e-8;

    private static double u[] = new double[3];
    private static double v[] = new double[3];
    private static double w[] = new double[3];

    public static double intersect(double p1[], double p2[],
       double p3[], double p4[],
       double pa[], double pb[]){
 vector(u, p1, p2);
 vector(v, p3, p4);
 vector(w, p3, p1);
 double a = dot(u,u); // always >= 0
 double b = dot(u,v);
 double c = dot(v,v); // always >= 0
 double d = dot(u,w);
 double e = dot(v,w);
 double D = a*c - b*b; // always >= 0
 double sc, sN, sD = D; // sc = sN / sD, default sD = D >= 0
 double tc, tN, tD = D; // tc = tN / tD, default tD = D >= 0

 // compute the line parameters of the two closest points
 if (D < EPS) { // the lines are almost parallel
     sN = 0.0;
     tN = e;
     tD = c;
 } else { // get the closest points on the infinite lines
     sN = (b*e - c*d);
     tN = (a*e - b*d);
     if (sN < 0) { // sc < 0 => the s=0 edge is visible
  sN = 0.0;
  tN = e;
  tD = c;
     } else if (sN > sD) { // sc > 1 => the s=1 edge is visible
  sN = sD;
  tN = e + b;
  tD = c;
     }
 }

 if (tN < 0) { // tc < 0 => the t=0 edge is visible
     tN = 0.0;
     // recompute sc for this edge
     if (-d < 0)
  sN = 0.0;
     else if (-d > a)
  sN = sD;
     else {
  sN = -d;
  sD = a;
     }
 } else if (tN > tD) { // tc > 1 => the t=1 edge is visible
     tN = tD;
     // recompute sc for this edge
     if ((-d + b) < 0)
  sN = 0;
     else if ((-d + b) > a)
  sN = sD;
     else {
  sN = (-d + b);
  sD = a;
     }
 }

 // finally do the division to get sc and tc
 sc = sN / sD;
 tc = tN / tD;

 // get the difference of the two closest points
 double dist = 0.0;
 double di = 0.0;
 for(int i = 0; i < 3; i++){
     di = w[i] + (sc * u[i]) - (tc * v[i]); // = S1(sc) - S2(tc)
     dist += di * di;
 }

 return Math.sqrt(dist);
    }

    /** Temporary space for inverting surface normal. */
    private double invertn[] = new double[3];

    /** Distance from clip plane that is allowed. */
    private double clipTolerance = 0.0;

    /** Clip sphere according to plane list and add to tmesh. */
    public void clipSphere(Face f, int ia){
 int edgeCount = f.size();

 // reset the clip and hull markers
 for(int isp = 0; isp < nsp; isp++){
     clipped[isp] = -1;
 }

 clipTolerance = -0.15 * currentLongestEdge;

 if(ia == -1){
     for(int a = 0; a < edgeCount; a++){
  Edge e = (Edge)f.get(a);

  for(int isp = 0; isp < nsp; isp++){
      if(clipped[isp] == -1){
   // plane equation
   if(plane_eqn(tsx[isp], e.cen, e.n) > clipTolerance){
       clipped[isp] = -2;
   }
      }
  }
     }
 }else{
     for(int a = 0; a < count[ia]; a++){
  int j = nn[first[ia] + a];

  // generate torus radius and direction
  torusAxisUnitVector(uij, xyz[ia], xyz[j]);

  // generate contact circles on each end of torus
  double rcij = contactCircle(cij, xyz[ia], radius[ia], xyz[j], radius[j]);

  for(int isp = 0; isp < nsp; isp++){
      if(clipped[isp] == -1){
   // plane equation
   if(plane_eqn(tsx[isp], cij, uij) > clipTolerance){
       clipped[isp] = -2;
   }
      }
  }
     }
 }

 // clear the hull markers
 for(int ii = 0; ii < nsp; ii++){
     hull[ii] = 0;

     if(clipped[ii] == -1){
  int nc = vncount[ii];
  for(int j = 0; j < nc; j++){
      if(clipped[vn[ii][j]] == -2){
   hull[ii] = 1;
   break;
      }
  }
     }
 }

 for(int isp = 0; isp < nsp; isp++){
     // add all the points
     if(clipped[isp] == -1){
  if(f.type == Face.Concave){
      invertn[0] = -snx[isp][0];
      invertn[1] = -snx[isp][1];
      invertn[2] = -snx[isp][2];
      clipped[isp] =
   tmesh.addPoint(tsx[isp], invertn, 0.0, 0.0);
      if(debug){
   tmesh.addSphere(tsx[isp][0], tsx[isp][1], tsx[isp][2],
     0.05, Color32.red);
      }

      int color = colorPoint(f, tsx[isp]);

      tmesh.vcolor[clipped[isp]] = color;
  }else{
      clipped[isp] =
   tmesh.addPoint(tsx[isp], snx[isp], 0.0, 0.0);
      tmesh.vcolor[clipped[isp]] = colors[ia];
  }
     }
 }
    }

    public int colorPoint(Face f, double p[]){
 Vertex v0 = ((Edge)f.get(0)).v0;
 Vertex v1 = ((Edge)f.get(1)).v0;
 Vertex v2 = ((Edge)f.get(2)).v0;

 double d0 = distance(p[0], p[1], p[2], v0.x[0], v0.x[1], v0.x[2]);
 double d1 = distance(p[0], p[1], p[2], v1.x[0], v1.x[1], v1.x[2]);
 double d2 = distance(p[0], p[1], p[2], v2.x[0], v2.x[1], v2.x[2]);
 double sum = 2.0 * (d0 + d1 + d2);

 //FILE.out.print("d0 %5.2f ", d0);
 //FILE.out.print("d1 %5.2f ", d1);
 //FILE.out.print("d2 %5.2f\n", d2);

 //FILE.out.print("sum %5.2f\n", sum);

 double comp0 = (d1 + d2)/sum;
 double comp1 = (d0 + d2)/sum;
 double comp2 = (d0 + d1)/sum;

 //FILE.out.print("c0 %5.2f ", comp0);
 //FILE.out.print("c1 %5.2f ", comp1);
 //FILE.out.print("c2 %5.2f\n", comp2);

 int r = 0, g = 0, b = 0;

 r += comp0 * Color32.getRed(colors[v0.i]);
 r += comp1 * Color32.getRed(colors[v1.i]);
 r += comp2 * Color32.getRed(colors[v2.i]);
 g += comp0 * Color32.getGreen(colors[v0.i]);
 g += comp1 * Color32.getGreen(colors[v1.i]);
 g += comp2 * Color32.getGreen(colors[v2.i]);
 b += comp0 * Color32.getBlue(colors[v0.i]);
 b += comp1 * Color32.getBlue(colors[v1.i]);
 b += comp2 * Color32.getBlue(colors[v2.i]);

 //Color32.print("rgb ", r, g, b);

 return Color32.pack(r, g, b);
    }

    /** Add the remaing triangles. */
    public void addWholeTriangles(Face f){
 // now we clipped all the points add the
 // remaining triangles to the tmesh
 if(false){
     if(f.type == Face.Concave){
  currentColor = 3;
     }else{
  currentColor = 1;
     }
 }

 for(int t = 0; t < nst; t++){
     if(clipped[si[t]] >= 0 &&
        clipped[sj[t]] >= 0 &&
        clipped[sk[t]] >= 0){
  // all the points were unclipped
  meshAddTriangle(clipped[si[t]],
    clipped[sj[t]],
    clipped[sk[t]]);
     }
 }
    }

    /** Transform the sphere to this atom position/size. */
    public void transformSphere(double xs[], double rs){

 for(int i = 0; i < nsp; i++){
     clipped[i] = -1;
     for(int j = 0; j < 3; j++){
  tsx[i][j] = xs[j] + sx[i][j] * rs;

     }
 }

 currentLongestEdge = rs * longestEdge;
    }

    Edge sorted[] = new Edge[3];

    /** Sort the edge list. */
    private void sortEdgeList(Probe p){
 sorted[0] = p.edge0;
 sorted[1] = p.edge1;
 sorted[2] = p.edge2;

 for (int ia = 0; ia < 3 - 1; ia++) {
     for (int ja = 0; ja < 3 - 1 - ia; ja++){
  if (sorted[ja+1].size() < sorted[ja].size()) {
      Edge tmp = sorted[ja];
      sorted[ja] = sorted[ja+1];
      sorted[ja+1] = tmp;
  }
     }
 }
    }

    /** Current list of clipping probes. */
    private DynamicArray clippingProbes = null;

    /** Print the list of vertices for this edge. */
    private void printEdgeVertices(String s, Edge e){
 int count = e.size();
 System.out.print(s + " ");

 for(int i = 0; i < count; i++){
     int v = e.get(i);
     if(i > 0) System.out.print(",");
     System.out.print(v);
 }
 System.out.println("");
    }

    /** Working space for contact face triangulation. */
    double mid01[] = new double[3];
    double mid12[] = new double[3];
    double mid20[] = new double[3];
    double nmid01[] = new double[3];
    double nmid12[] = new double[3];
    double nmid20[] = new double[3];

    /** vector for coordinate frame. */
    private double pp[] = new double[3];
    private double ccij[] = new double[3];
    private double ccji[] = new double[3];

    private double vij2ji[] = new double[3];

    /** The point on the torus. */
    private double tp[] = new double[3];

    /** The normal at the point on the torus. */
    private double ntp[] = new double[3];

    /** Where the saddle face points end up in the tmesh. */
    int tmeshv[][] = new int[100][100];

    /** Deltas for the wrap angle calculation. */
    private double pp2cij[] = new double[3];
    private double pp2cji[] = new double[3];
    private double n1[] = new double[3];
    private double n2[] = new double[3];

    public void processToroidalFace(Face f){
 triangulateToroidalFace(f);
    }

    /**
     * Triangulate a toroidal face.
     *
     * Vital face ordering diagram for face triangulation.
     *            
     *                         <- aa                            
     *                i
     *               e3
     *     ----------->-----------
     *     |                     |
     *     |                     |          ii
     *     |                     |           |
     *     |                     |           v
     *  e1 ^                     v e0
     *     |                     |
     *     |                     |
     *     |                     |
     *     |                     |
     *     |                     |
     *     -----------<-----------
     *               e2
     *                j
     */
    private void triangulateToroidalFace(Face f){
 Torus t = f.torus;
 Edge e0 = (Edge)f.get(0);
 Edge e1 = (Edge)f.get(1);
 Edge e2 = (Edge)f.get(2);
 Edge e3 = (Edge)f.get(3);
 double a0 = f.startAngle;
 double a1 = f.stopAngle;

 if(e2.v0.i != t.j){
     System.out.println("t.i " + t.i + " t.j " + t.j);
     System.out.println("e0.v0.i " + e0.v0.i + " e0.v1.i " + e0.v1.i);
     System.out.println("e1.v0.i " + e1.v0.i + " e1.v1.i " + e1.v1.i);
     System.out.println("e2.v0.i " + e2.v0.i + " e2.v1.i " + e2.v1.i);
     System.out.println("e3.v0.i " + e3.v0.i + " e3.v1.i " + e3.v1.i);
 }

 // form coordinate set.

 // correct for wrapped angles
 if(a1 < a0){
     a1 += 2.0 * Math.PI;
 }

 if(a0 > a1){
     System.out.println("angle error ");
 }

 // calculate angular step for probe center
 // a1 must be greater than a0
 // use largest raduis contact circle to
 // control triangulation parameters
 double effectiveArc = t.rcij;

 if(t.rcji > effectiveArc){
     effectiveArc = t.rcji;
 }

 double arcLength = (a1 - a0) * (effectiveArc);
 // always at least two points
 int tpcount = 2 + (int)(arcLength/targetLen);
 int tpcount1 = tpcount - 1;
 double angle = a0;
 double wrapAngle = 0.0;
 double wrapAngleStep = 0.0;
 int nwap = 0;

 double step = (a1 - a0)/(tpcount1);

 e2.setCapacity(tpcount);
 e3.setCapacity(tpcount);

 for(int a = 0; a < tpcount; a++){
     // tidy up any slight rounding error
     if(angle > a1){
  angle = a1;
     }

     double sina = Math.sin(angle);
     double cosa = Math.cos(angle);

     // interpolate the vectors
     for(int i = 0; i < 3; i++){
  double component = t.uijnorm2[i] * sina + t.uijnorm[i] * cosa;

  pp[i] = t.tij[i] + t.rij * component;
  //ccij[i] = t.cij[i] + t.rcij * component;
  //ccji[i] = t.cji[i] + t.rcji * component;
  ccij[i] = f.iij[i] + t.rcij * component;
  ccji[i] = f.iji[i] + t.rcji * component;
     }

     // calculate the wrap angle
     // and the offsets for this torus segment.
     vector(pp2cij, pp, ccij);
     vector(pp2cji, pp, ccji);
     wrapAngle = angle(pp2cij, pp2cji);

     cross(n1, pp2cij, pp2cji);
     cross(n2, n1, pp2cij);
     normalise(n2);
     normalise(pp2cij);
     double wrapArcLength = wrapAngle * probeRadius;

     // always at least two points
     nwap = 2 + (int)(wrapArcLength/targetLen);
     int nwap1 = nwap - 1;

     wrapAngleStep = wrapAngle/(nwap1);

     if(a == 0){
  e0.setCapacity(nwap);
  e1.setCapacity(nwap);
     }

     double wa = 0.0;

     // now interpolate from one end
     // of the arc to the other.
     for(int ii = 0; ii < nwap; ii++){
  double sinwa = Math.sin(wa);
  double coswa = Math.cos(wa);

  // tidy up any slight rounding error
  if(wa > wrapAngle){
      //System.out.println("t.i " + t.i + " t.j " + t.j);
      //System.out.println("ii " + ii + " nwap " + nwap);
      wa = wrapAngle;
  }

  // relative vector
  for(int i = 0; i < 3; i++){
      ntp[i] = coswa * pp2cij[i] + sinwa * n2[i];
      tp[i] = probeRadius * ntp[i] + pp[i];
      ntp[i] = -ntp[i];
  }

  // record the vertex index
  int vid = 0;

  // need to get the tmesh vertices for the
  // corner points from the vertex objects
  // to ensure mesh sharing
  if(a == 0 && ii == 0){
      vid = e0.v0.vi;
  }else if(a == 0 && ii == nwap1){
      vid = e0.v1.vi;
  }else if(a == tpcount1 && ii == nwap1){
      vid = e1.v0.vi;
  }else if(a == tpcount1 && ii == 0){
      vid = e1.v1.vi;
  }else{
      // not corner
      // need a new point
      vid = tmesh.addPoint(tp, ntp, 0.0, 0.0);
  }

  tmeshv[a][ii] = vid;

  // interpolate color along arc
  double colorFrac = (double)ii/(double)(nwap-1);
  tmesh.vcolor[vid] = Color32.blend(colors[t.i],
       colors[t.j],
       1. - colorFrac);

  // assign the vertices to edge structures
  //
  // XXX need to retrieve the vertex indexes
  // for the corner vertices of the toriodal patch
  //!!
  // the ordering of these edges is crucial
  // do not change unless you know better than me
  if(a == 0) e0.set(ii, vid);
  if(a == tpcount1) e1.set(nwap1-ii, vid);
  if(ii == 0) e3.set(tpcount1-a, vid);
  if(ii == nwap1) e2.set(a, vid);

  wa += wrapAngleStep;


     }

     //if(t.selfIntersects == false){
     //currentColor = 2;
     if(!debug){
  if(a > 0){
      for(int ii = 0; ii < nwap1; ii++){
   meshAddTriangle(tmeshv[a-1][ii],
     tmeshv[a][ii],
     tmeshv[a-1][ii+1]);
   meshAddTriangle(tmeshv[a][ii],
     tmeshv[a-1][ii+1],
     tmeshv[a][ii+1]);
      }
  }
     }
     //}

     angle += step;
 }
    }

    /** Working space for torus processing. */
    Edge torusEdges[] = new Edge[1000];

    /** Angles relative to torus normal. */
    double angles[] = new double[1000];

    /** Vector form contact circle to torus vertex. */
    private double cij2v[] = new double[3];

    /** Private probe for torus clipping. */
    private Probe torusProbe = new Probe();

    private DynamicArray torusProbes = new DynamicArray(1);

    /** Working space for intersecting torii. */
    double qij[] = new double[3];
    double qji[] = new double[3];

    double cusp[] = new double[3];
    double nleft[] = new double[3];
    double nright[] = new double[3];

    /** Process a single torus. */
    private void processTorus(Torus t){
 int edgeCount = t.edges.size();

 // try to prevent these getting into the
 // data structure at some point.
 if(edgeCount == 0){
     // do nothing
     System.out.println("!!! torus with no edges!!!!");

     return;
 }

 // can't be an odd number of edge.
 if(edgeCount % 2 != 0){
     System.out.println("atom i " + t.i + " j " + t.j);
     System.out.println("odd number of edges " + edgeCount);

     return;
 }

 for(int i = 0; i < edgeCount; i++){
     torusEdges[i] = (Edge)t.edges.get(i);
 }

 for(int ee = 0; ee < edgeCount; ee++){
     Edge e = torusEdges[ee];

     if(e.v0.i == t.i){
  vector(cij2v, t.cij, e.v0.x);
     }else if(e.v1.i == t.i){
  vector(cij2v, t.cij, e.v1.x);
     }else{
  System.out.println("edge doesn't involve i " + t.i);
  System.out.println("edge has " + e.v0.i + "," + e.v1.i);
     }

     //angles[ee] = angle2(t.uijnorm, cij2v, t.uij);
     angles[ee] = angle(cij2v, t.uijnorm, t.uijnorm2);

     //System.out.println("angle "+ angles[ee]);
 }

 // sort them using bubble sort...
 // will do for now (not usually more than 4)
 for (int ia = 0; ia < edgeCount - 1; ia++) {
     for (int ja = 0; ja < edgeCount - 1 - ia; ja++){
  if (angles[ja+1] > angles[ja]) {
      double tmp = angles[ja];
      angles[ja] = angles[ja+1];
      angles[ja+1] = tmp;

      Edge etmp = torusEdges[ja];
      torusEdges[ja] = torusEdges[ja+1];
      torusEdges[ja+1] = etmp;
  }
     }
 }

 // check that we got the correct ordering
 for(int ee = 0; ee < edgeCount - 1; ee++){
     if(angles[ee] < angles[ee + 1]){
  System.out.println("!!!! error sorting vertex angles " +
       t.i + "," + t.j);
     }
 }

 double tcen[] = new double[3];

 for(int ee = 0; ee < edgeCount; ee += 2){
     Edge e0 = torusEdges[ee];
     int ee1 = 0;
     Edge e1 = null;

     // depending on whether this edge start on i
     // or runs to i, depends on whether the paired
     // Edge is before or after us in the list.
     if(e0.v1.i == t.i){
  // can never overflow
  ee1 = ee + 1;
     }else{
  ee1 = ee - 1;
  // make sure we get the last edge
  // if we ask for -1.
  if(ee1 == -1){
      ee1 = edgeCount - 1;
  }
     }

     e1 = torusEdges[ee1];

     if(e0.v0.i != e1.v1.i || e0.v1.i != e1.v0.i){
  System.out.println("!! unpaired edges");
     }

     Edge e2 = null, e3 = null;

     e2 = addEdge(e0.v1, e1.v0, t);
     e3 = addEdge(e1.v1, e0.v0, t);

     if(false && t.selfIntersects){
  // need to generate 2 torus faces and
  // rejig the probe face on each side
  double dtq = Math.sqrt(probeRadius*probeRadius - t.rij*t.rij);
  mid(cusp, e1.probeFace.cen, e0.probeFace.cen);
  double dist =
      0.5 * distance(e0.probeFace.cen, e1.probeFace.cen);
  double rint = Math.sqrt(probeRadius*probeRadius - dist*dist);

  // generate normals.
  vector(nleft, e1.probeFace.cen, e0.probeFace.cen);
  normalise(nleft);
  copy(nleft, nright);
  negate(nright);

  for(int i = 0; i < 3; i++){
      qij[i] = t.tij[i] - dtq * t.uij[i];
      qji[i] = t.tij[i] + dtq * t.uij[i];
  }

  //e0.probeFace.skip = true;
  //e1.probeFace.skip = true;

  if(e0.probeFace == null || e1.probeFace == null){
      System.out.println("e0 or e1 has null probe face");
  }

  Vertex vi0 = null;
  Vertex vj0 = null;

  if(e0.v1.i == t.i){
      vi0 = addVertex(qji, t.j, cusp);
      vj0 = addVertex(qij, t.i, cusp);
  }else{
      vi0 = addVertex(qij, t.i, cusp);
      vj0 = addVertex(qji, t.j, cusp);
  }

  e2.add(e2.v0.vi);
  e2.add(e2.v1.vi);

  e3.add(e3.v0.vi);
  e3.add(e3.v1.vi);

  Edge topRight = addSimpleEdge(e0.v0, vi0, e0, null, null, -1.);
  Edge topLeft = addSimpleEdge(vi0, e1.v1, e1, null, null, -1.);

  Face topf = new Face(Face.Undefined);
  topf.add(e3); topf.add(topRight); topf.add(topLeft);
  faces.add(topf);

  Edge bottomLeft =
      addSimpleEdge(e1.v0, vj0, e1, null, null, -1.);
  Edge bottomRight =
      addSimpleEdge(vj0, e0.v1, e0, null, null, -1.);

  Face bottomf = new Face(Face.Undefined);
  bottomf.add(e2);
  bottomf.add(bottomLeft);
  bottomf.add(bottomRight);
  faces.add(bottomf);

  Edge connectLeft =
      addSimpleEdge(vj0, vi0, null, cusp, nleft, rint);
  Edge connectRight =
      addSimpleEdge(vi0, vj0, null, cusp, nright, rint);

  connectRight.removeAllElements();

  int vcount = connectLeft.size();

  for(int iv = 0; iv < vcount; iv++){
      connectRight.add(connectLeft.getReverse(iv));
  }

  // tidy up the probe boundary edges.
  replaceProbeEdges(e0, topRight, connectRight, bottomRight);

  replaceProbeEdges(e1, bottomLeft, connectLeft, topLeft);

     }else{

  // and add the new face

  Face f = new Face(Face.Saddle);

  f.torus = t;

  copy(t.cij, f.iij);
  copy(t.cji, f.iji);

  faces.add(f);

  if(e0.v1.i == t.i){
      f.startAngle = angles[ee1];
      f.stopAngle = angles[ee];
      f.add(e1); f.add(e0); f.add(e3); f.add(e2);
  }else{
      f.startAngle = angles[ee];
      f.stopAngle = angles[ee1];
      f.add(e0); f.add(e1); f.add(e2); f.add(e3);
  }
     }
 }

 // record maximum number of torus edges we saw
 if(edgeCount > maximumTorusEdges){
     maximumTorusEdges = edgeCount;
 }
    }

    public double vx0[] = new double[3];
    public double vx1[] = new double[3];
    public double pint[] = new double[3];

    private Edge addSimpleEdge(Vertex v0, Vertex v1, Edge refEdge,
          double x[], double n[], double rr){
 Edge e = new Edge();
 e.v0 = v0;
 e.v1 = v1;

 e.add(v0.vi);

 if(refEdge != null){
     copy(refEdge.n, e.n);
     copy(refEdge.cen, e.cen);
     e.r = refEdge.r;
 }else if(x != null){
     copy(x, e.cen);
     copy(n, e.n);
     e.r = rr;

     tmesh.getVertex(v0.vi, vx0, null);
     tmesh.getVertex(v1.vi, vx1, null);

     double dist = distance(vx0, vx1);
     int points = 2 + (int)(4.*dist/desiredTriangleLength);

     for(int i = 1; i < points - 1; i++){
  double frac = (double)i/(double)points;

  for(int j = 0; j < 3; j++){
      pint[j] = vx0[j] + frac*(vx1[j] - vx0[j]);
      pint[j] -= x[j];
  }

  normalise(pint);
  scale(pint, rr);

  for(int j = 0; j < 3; j++){
      pint[j] += x[j];
  }

  Vertex vint = addVertex(pint, -1, x);
  e.add(vint.vi);
     }
 }else{
     System.out.println("addSimpleEdge: no edge/point for reference");
     System.exit(1);
 }

 e.add(v1.vi);

 return e;
    }

    private Edge oldEdges[] = new Edge[100];

    public void replaceProbeEdges(Edge olde, Edge e0, Edge e1, Edge e2){
 Face probeFace = olde.probeFace;

 if(olde.v0 != e0.v0 || olde.v1 != e2.v1 ||
    e0.v1 != e1.v0 || e1.v1 != e2.v0){
     System.out.println("replacement edges don't span same vertices");
     System.out.println("olde.v0 " + olde.v0.vi + " olde.v1 " + olde.v1.vi);
     System.out.println("e0.v0 " + e0.v0.vi + " e0.v1 " + e0.v1.vi);
     System.out.println("e1.v0 " + e1.v0.vi + " e1.v1 " + e1.v1.vi);
     System.out.println("e2.v0 " + e2.v0.vi + " e2.v1 " + e2.v1.vi);

     return;
 }

 if(probeFace == null){
     System.out.println("edge had no probe in replaceProbeEdges");
     return;
 }

 if(probeFace.contains(olde) == false){
     System.out.println("face didn't contain old edge...");
     return;
 }

 int edgeCount = probeFace.size();

 for(int i = 0; i < edgeCount; i++){
     oldEdges[i] = (Edge)probeFace.get(i);
 }

 probeFace.removeAllElements();

 for(int i = 0; i < edgeCount; i++){
     if(oldEdges[i] == olde){
  probeFace.add(e0);
  probeFace.add(e1);
  probeFace.add(e2);
     }else{
  probeFace.add(oldEdges[i]);
     }
 }

 if(probeFace.isValid() == false){
     System.out.println("new probeFace is not valid");
 }
    }

    /** Calculate contact circle center and radius. */
    private double contactCircle(double cij[],
     double ai[], double ri,
     double aj[], double rj){
 double rip = ri + probeRadius;
 double rjp = rj + probeRadius;

 double rij = torusCenter(tij, ai, ri, aj, rj, probeRadius);

 for(int ii = 0; ii < 3; ii++){
     cij[ii] = (ri * tij[ii] + probeRadius * ai[ii])/rip;
 }

 return rij*ri/(rip);
    }

    /** Process all of the torii. */
    private void processTorii(){
 int toriiCount = torii.size();

 for(int i = 0; i < toriiCount; i++){
     Torus t = (Torus)torii.get(i);

     processTorus(t);
 }
    }

    /** Working space for probe placements. */
    private double probe0[] = new double[3];
    private double probe1[] = new double[3];

    /** Construct probe placements from triplets of atoms. */
    private void constructProbePlacements(){

 int tripletCount = 0;

 for(int i = 0; i < nxyz; i++){

     for(int a = 0; a < count[i]; a++){
  int j = nn[first[i] + a];
  if(j > i){
      commonCount = commonElements(nn, first[i], count[i],
       nn, first[j], count[j],
       commonNeighbours);

      for(int b = 0; b < commonCount; b++){
   int k = commonNeighbours[b];

   if(k > j){
       tripletCount++;

       /*
			    double trij = torusRadius(radius[i], radius[j], distance(xyz[i], xyz[j]), probeRadius);
			    double trjk = torusRadius(radius[j], radius[j], distance(xyz[j], xyz[k]), probeRadius);
			    double trki = torusRadius(radius[k], radius[i], distance(xyz[k], xyz[i]), probeRadius);
			    
			    if(trij < probeRadius || trjk < probeRadius || trki < probeRadius) continue;
			    */

       if(constructProbePlacement(xyz[i], radius[i],
             xyz[j], radius[j],
             xyz[k], radius[k],
             probeRadius,
             probe0, probe1)){
    int probeCount = 0;
    if(!obscured(probe0, i, j, k)){
        processPlacement(probe0, i, j, k);

        probeCount++;
    }

    if(!obscured(probe1, i, j, k)){
        processPlacement(probe1, i, j, k);

        probeCount++;
    }

    // placed both probes
    if(probeCount == 2){
        double rp2 = (2.0*probeRadius)*(2.0*probeRadius);
        if(distance2(probe0, probe1) < rp2){
     // they intersect one another.
     selfIntersectingProbes++;

     // get the last two probes
     // and add as intersecting pair
     Probe p1 = (Probe)probes.getReverse(0);
     Probe p2 = (Probe)probes.getReverse(1);

     p1.addClippingProbe(p2);
     p2.addClippingProbe(p1);

     Face f1 = (Face)faces.getReverse(0);
     Face f2 = (Face)faces.getReverse(1);

     //f1.skip = true;
     //f2.skip = true;

     f1.intersection = Face.ProbeIntersection;
     f2.intersection = Face.ProbeIntersection;
        }
    }
       }
   }
      }
  }
     }
 }

 print("triplets", tripletCount);
    }

    /** The last sphere that occluded a probe placement. */
    int cacheSphere = -1;

    /**
     * Is p obscured by any of the neigbhours of i, j or k.
     * But not by i, j or k itself as these were used to
     * construct the point.
     */
    private boolean obscured(double p[], int i, int j, int k){

        // this order seems slightly more effective - k, i, j
        if(obscured2(p, k, i, j)){
            return true;
        }
        if(obscured2(p, i, j, k)){
            return true;
        }
        if(obscured2(p, j, i, k)){
            return true;
        }

        //cacheSphere = -1;

        return false;
    }

    /** Is p obscured by a neighbour of i, except for j or k. */
    private boolean obscured2(double p[], int i, int j, int k){
        double localrsq[] = rsq;

        // check the last sphere that clipped
        // can often be the same one
        if(cacheSphere != -1){
            if(cacheSphere != j && cacheSphere != k && cacheSphere != i){
                distanceComparisons++;

                if(distance2(xyz[cacheSphere], p) < localrsq[cacheSphere]){
                    return true;
                }

                cacheSphere = -1;
            }
        }

        int lastn = first[i] + count[i];

        for(int a = first[i]; a < lastn; a++){
            int neighbour = nn[a];

            distanceComparisons++;

            double dx = p[0] - xyz[neighbour][0];
            double dy = p[1] - xyz[neighbour][1];
            double dz = p[2] - xyz[neighbour][2];

            if(dx*dx+dy*dy+dz*dz < localrsq[neighbour]){
            //if(distance2(p, xyz[neighbour]) < rsq[neighbour]){
                // measurably faster to check after
                // satisfying the distance
                if(neighbour != j && neighbour != k){
                    cacheSphere = neighbour;

                    return true;
                }
            }
        }

        return false;
    }

    /** Add a probe placement for the surface. */
    private Probe addProbePlacement(double pijk[], int i, int j, int k){
 Probe p = new Probe();

 copy(pijk, p.x);

 p.i = i; p.j = j; p.k = k;

 p.r = probeRadius;

 probes.add(p);

 // add the probes to the atom lists.
 if(probeList[i] == null) probeList[i] = new DynamicArray(10);
 if(probeList[j] == null) probeList[j] = new DynamicArray(10);
 if(probeList[k] == null) probeList[k] = new DynamicArray(10);
 probeList[i].add(p);
 probeList[j].add(p);
 probeList[k].add(p);

 return p;
    }

    /** Temporary for calculating normals. */
    double nnv[] = new double[3];

    /** Add a vertex for the surface. */
    //private Vertex addVertex(double vx[], int i, Probe p){
    private Vertex addVertex(double vx[], int i, double px[]){
 Vertex v = new Vertex();

 //v.p = p;

 vector(nnv, vx, px);
 normalise(nnv);

 v.i = i;

 v.vi = tmesh.addPoint(vx, nnv, 0.0, 0.0);

 // record the coords here for convenience.
 copy(vx, v.x);

 if(i != -1){
     if(vertexList[i] == null) vertexList[i] = new DynamicArray(10);
     vertexList[i].add(v);
 }

 vertices.add(v);

 return v;
    }

    /** Add an edge for the molecular surface. */
    private Edge addEdge(Vertex v0, Vertex v1, Torus t){
 Edge e = new Edge();

 e.v0 = v0;
 e.v1 = v1;

 if(v0.i == t.i){
     copy(t.cij, e.cen);
     e.r = t.rcij;
     copy(t.uij, e.n);
 }else{
     copy(t.cji, e.cen);
     e.r = t.rcji;
     copy(t.uij, e.n);
     negate(e.n);
 }

 edges.add(e);

 if(edgeList[v0.i] == null) edgeList[v0.i] = new DynamicArray(10);
 if(edgeList[v1.i] == null) edgeList[v1.i] = new DynamicArray(10);

 edgeList[v0.i].add(e);

 if(v0.i != v1.i){
     // half the torus faces have both edges
     // on the same atom - make sure we only
     // add it once.
     edgeList[v1.i].add(e);
 }

 return e;
    }

    /** Add a face to the molecular surface. */
    private Face addFace(Edge e0, Edge e1, Edge e2, Edge e3){
 Face f = new Face(Face.Saddle);

 if(e0 != null) f.add(e0);
 if(e1 != null) f.add(e1);
 if(e2 != null) f.add(e2);
 if(e3 != null) f.add(e3);

 faces.add(f);

 return f;
    }

    /* Vectors for the atom positions. */
    private static double uij[] = new double[3];
    private static double uik[] = new double[3];
    private static double tij[] = new double[3];
    private static double tji[] = new double[3];
    private static double tik[] = new double[3];
    private static double uijk[] = new double[3];
    private static double utb[] = new double[3];
    private static double bijk[] = new double[3];
    private static double pijk[] = new double[3];

    private static double cij[] = new double[3];
    private static double cji[] = new double[3];

    private static double api[] = new double[3];
    private static double apj[] = new double[3];
    private static double apk[] = new double[3];



    /**
     * Construct the two probe placements for a single triplet.
     *
     * Follows the terminology of
     * Connolly M., J.Appl.Cryst. (1983), 16, 548-558.
     */
    public static boolean constructProbePlacement(double xi[], double ri,
        double xj[], double rj,
        double xk[], double rk,
        double rp,
        double p0[], double p1[]){

 torusAxisUnitVector(uij, xi, xj);
 torusAxisUnitVector(uik, xi, xk);

 double rij = torusCenter(tij, xi, ri, xj, rj, rp);
 double rik = torusCenter(tik, xi, ri, xk, rk, rp);

 // rejig in terms of 1-cos2
 //double wijk = baseTriangleAngle(uij, uik);
 //double swijk = Math.sin(wijk);
 double swijk = baseTriangleAngle(uij, uik);

 basePlaneNormalVector(uijk, uij, uik, swijk);

 torusBasepointUnitVector(utb, uijk, uij);

 basePoint(bijk, tij, utb, uik, tik, swijk);

 double hijk = probeHeight(ri + rp, bijk, xi);

 // special case for certain combinations of
 // sphere radii and positions.
 // i dont think it is an error
 if(hijk < 0.0){
     return false;
 }

 int probeCount = 0;

 // + the probe height
 probePosition(p0, bijk, hijk, uijk);

 probePosition(p1, bijk, -hijk, uijk);

 return true;
    }

    private double pdir[] = new double[3];

    /** Process the probe placement. */
    private void processPlacement(double pijk[], int i, int j, int k){
 Probe p = addProbePlacement(pijk, i, j, k);

 // add the vertices
 constructVertex(api, pijk, xyz[i], radius[i]);
 Vertex v0 = addVertex(api, i, p.x);
 constructVertex(apj, pijk, xyz[j], radius[j]);
 Vertex v1 = addVertex(apj, j, p.x);
 constructVertex(apk, pijk, xyz[k], radius[k]);
 Vertex v2 = addVertex(apk, k, p.x);

 // get direction of probe placement
 vector(pdir, bijk, pijk);

 Edge edge0 = null, edge1 = null, edge2 = null;

 // assign edges depending on orientation
 if(dot(pdir, uijk) > 0.0){
     edge0 = constructProbeEdge(v0, api, v1, apj, apk, pijk, probeRadius);
     edge1 = constructProbeEdge(v1, apj, v2, apk, api, pijk, probeRadius);
     edge2 = constructProbeEdge(v2, apk, v0, api, apj, pijk, probeRadius);
 }else{
     edge0 = constructProbeEdge(v0, api, v2, apk, apj, pijk, probeRadius);
     edge1 = constructProbeEdge(v2, apk, v1, apj, api, pijk, probeRadius);
     edge2 = constructProbeEdge(v1, apj, v0, api, apk, pijk, probeRadius);
 }

 // store the face
 Face f = new Face(Face.Concave);
 f.add(edge0);
 f.add(edge1);
 f.add(edge2);

 copy(pijk, f.cen);
 f.r = probeRadius;

 faces.add(f);
    }

    // working space
    double edgen[] = new double[3];
    double otherv[] = new double[3];

    public Edge constructProbeEdge(Vertex v0, double p0[],
       Vertex v1, double p1[],
       double pother[],
       double pijk[], double rad){
 int i = v0.i;
 int j = v1.i;

 Edge edge = new Edge();

 // construct edge normal
 normal(edgen, p0, pijk, p1);
 vector(otherv, pijk, pother);

 // make sure it points towards the other vertex.
 if(dot(edgen, otherv) > 0.0){
     negate(edgen);
 }

 edge.r = rad;
 edge.v0 = v0;
 edge.v1 = v1;

 copy(edgen, edge.n);
 copy(pijk, edge.cen);

 // now create the torus if necessary
 Torus torus = findTorus(v0.i, v1.i);

 if(torus == null){
     torus = new Torus(v0.i, v1.i);

     torusAxisUnitVector(torus.uij, xyz[i], xyz[j]);

     torus.rij = torusCenter(torus.tij, xyz[i], radius[i], xyz[j], radius[j], probeRadius);

     if(torus.rij < probeRadius){
  selfIntersectingTorii++;
  // if the radius is smaller than the probe
  // radius the torus intersects itself
  torus.selfIntersects = true;
  //torus.selfIntersects = false;
     }

     // generate contact circles on each end of torus
     torus.rcij = contactCircle(torus.cij, xyz[i], radius[i], xyz[j], radius[j]);
     torus.rcji = contactCircle(torus.cji, xyz[j], radius[j], xyz[i], radius[i]);

     normal(torus.uijnorm, torus.uij);
     cross(torus.uijnorm2, torus.uij, torus.uijnorm);
     normalise(torus.uijnorm2);

     // ok we got the torus
     // now stick it in all the data structures
     if(torusList[i] == null) torusList[i] = new DynamicArray(4);
     if(torusList[j] == null) torusList[j] = new DynamicArray(4);

     torusList[i].add(torus);
     torusList[j].add(torus);

     if(edgeList[i] == null) edgeList[i] = new DynamicArray(4);
     if(edgeList[j] == null) edgeList[j] = new DynamicArray(4);

     // add edges to atom edge lists.
     edgeList[i].add(edge);
     edgeList[j].add(edge);

     torii.add(torus);
 }

 torus.edges.add(edge);

 return edge;
    }

    /** Is there a torus between i and j. */
    public Torus findTorus(int i, int j){
 if(torusList[i] == null) return null;

 int torusCount = torusList[i].size();

 for(int t = 0; t < torusCount; t++){
     Torus torus = (Torus)torusList[i].get(t);

     if((torus.i == i && torus.j == j) ||
        (torus.i == j && torus.j == i)){
  return torus;
     }
 }

 return null;
    }

    /** Calculate the torus axis unit vector. */
    public static void torusAxisUnitVector(double uij[],
        double ai[], double aj[]){
 double dij = 1.0/distance(ai, aj);

 for(int i = 0; i < 3; i++){
     uij[i] = (aj[i] - ai[i])*dij;
 }
    }

    /** Calculate the position of the torus center. */
    public static double torusCenter(double tij[],
         double ai[], double ri,
         double aj[], double rj,
         double rp){
 double rip = ri + rp;
 double rjp = rj + rp;
 double dij2 = distance2(ai, aj);
 double dij = Math.sqrt(dij2);
 double rconst = ((rip*rip) - (rjp*rjp))/dij2;

 for(int i = 0; i < 3; i++){
     tij[i] = 0.5 * (ai[i] + aj[i]) + 0.5 * (aj[i] - ai[i]) * rconst;
 }

 double rsum = rip + rjp;
 double rdiff = ri - rj;

 return 0.5 *
     Math.sqrt((rsum*rsum) - dij2) *
     Math.sqrt(dij2 - (rdiff * rdiff))/dij;
    }

    /** Torus radius. */
    public static double torusRadius(double ri, double rj, double dij, double rp){
 double dij2 = dij * dij;
 double rsum = ri + rj + rp + rp;
 double rdiff = ri - rj;

 return 0.5 *
     Math.sqrt((rsum*rsum) - dij2) *
     Math.sqrt(dij2 - (rdiff * rdiff))/dij;
    }

    /** Base triangle angle. */
    public static double baseTriangleAngle(double uij[], double uik[]){
 double dot2 = dot(uij, uik);
 dot2 *= dot2;

 if(dot2 < -1.0) dot2 = -1.0;
 else if(dot2 > 1.0) dot2 = 1.0;

 return Math.sqrt(1. - dot2);
 //return Math.acos(dot(uij, uik));
    }

    /** Calculate the base plane normal vector. */
    public static void basePlaneNormalVector(double uijk[], double uij[],
          double uik[], double swijk){
 cross(uijk, uij, uik);

 uijk[0] /= swijk;
 uijk[1] /= swijk;
 uijk[2] /= swijk;
    }

    /** Generate the torus basepoint unit vector. */
    public static void torusBasepointUnitVector(double utb[], double uijk[],
      double uij[]){
 cross(utb, uijk, uij);
    }

    /** Generate the base point. */
    public static void basePoint(double bijk[], double tij[], double utb[],
     double uik[], double tik[], double swijk){
 double dotut = 0.0;

 for(int i = 0; i < 3; i++){
     dotut += uik[i] * (tik[i] - tij[i]);
 }

 dotut /= swijk;

 for(int i = 0; i < 3; i++){
     bijk[i] = tij[i] + utb[i] * dotut;
 }
    }

    /** Calculate probe height. */
    public static double probeHeight(double rip, double bijk[], double ai[]){
 double h2 = (rip*rip) - distance2(bijk, ai);
 if(h2 < 0.0){
     return -1.0;
 }else{
     return Math.sqrt(h2);
 }
    }

    /**
     * Generate probe position.
     *
     * Call with -hijk for the opposite placement.
     */
    public static void probePosition(double pijk[], double bijk[],
         double hijk, double uijk[]){
 for(int i = 0; i < 3; i++){
     pijk[i] = bijk[i] + hijk * uijk[i];
 }
    }

    /** Construct vertex position. */
    public void constructVertex(double v[], double pijk[],
     double ai[], double r){
 double rip = r + probeRadius;

 for(int i = 0; i < 3; i++){
     v[i] = (r * pijk[i] + probeRadius * ai[i])/rip;
 }
    }

    /** Form cross product of two vectors (a = b x c). */
    public static double cross(double a[], double b[], double c[]){
 a[0] = (b[1] * c[2]) - (b[2] * c[1]);
 a[1] = (b[2] * c[0]) - (b[0] * c[2]);
 a[2] = (b[0] * c[1]) - (b[1] * c[0]);

 return a[0] + a[1] + a[2];
    }

    private double ab[] = new double[3];
    private double bc[] = new double[3];

    /** Form cross from three vectors (n = ab x bc). */
    public double normal(double n[],
    double a[], double b[], double c[]){
 vector(ab, a, b);
 vector(bc, b, c);
 n[0] = (ab[1] * bc[2]) - (ab[2] * bc[1]);
 n[1] = (ab[2] * bc[0]) - (ab[0] * bc[2]);
 n[2] = (ab[0] * bc[1]) - (ab[1] * bc[0]);

 return n[0] + n[1] + n[2];
    }

    /** Negate the vector. */
    public void negate(double a[]){
 a[0]= -a[0];
 a[1]= -a[1];
 a[2]= -a[2];
    }

    /** Generate the plane equation. */
    public double plane_eqn(double p[], double o[], double n[]){
 double px = p[0] - o[0];
 double py = p[1] - o[1];
 double pz = p[2] - o[2];

 // plane equation
 return px*n[0] + py*n[1] + pz*n[2];
    }

    /** Generate the dot product. */
    public static double dot(double a[], double b[]){
 return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }

    /** Generate the dot product. */
    public double dotnorm(double a[], double b[]){
 return (a[0]*b[0] + a[1]*b[1] + a[2]*b[2])/(length(a)*length(b));
    }

    /** Generate the dot product. */
    public double dot(double ax, double ay, double az,
        double bx, double by, double bz){
 return ax*bx + ay*by + az*bz;
    }

    /** Generate vector from a to b. */
    public static void vector(double v[], double a[], double b[]){
 v[0] = b[0] - a[0];
 v[1] = b[1] - a[1];
 v[2] = b[2] - a[2];
    }

    /* Form normal n to vector v. */
    public void normal(double n[], double v[]){
 n[0] = 1.0; n[1] = 1.0; n[2] = 1.0;

        if(v[0] != 0.) n[0] = (v[2] + v[1]) / -v[0];
        else if(v[1] != 0.) n[1] = (v[0] + v[2]) / -v[1];
        else if(v[2] != 0.) n[2] = (v[0] + v[1]) / -v[2];

        normalise(n);
    }

    /** Normalise the vector. */
    public void normalise(double p[]){
 double len = p[0]*p[0] + p[1]*p[1] + p[2]*p[2];

 if(len != 0.0){
     len = Math.sqrt(len);
     p[0] /= len;
     p[1] /= len;
     p[2] /= len;
 }else{
     print("Can't normalise vector", p);
 }
    }

    /** Copy b into a. */
    public void copy(double b[], double a[]){
 a[0] = b[0]; a[1] = b[1]; a[2] = b[2];
    }

    public void mid(double m[], double a[], double b[]){
 for(int i = 0; i < 3; i++){
     m[i] = 0.5 * (a[i] + b[i]);
 }
    }

    /** Distance between two points. */
    public static double distance(double a[], double b[]){
 double dx = a[0] - b[0];
 double dy = a[1] - b[1];
 double dz = a[2] - b[2];

 return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    /** Distance squared between two points. */
    public static double distance2(double a[], double b[]){
 double dx = a[0] - b[0];
 double dy = a[1] - b[1];
 double dz = a[2] - b[2];

 return dx*dx + dy*dy + dz*dz;
    }

    /** Length of vector. */
    public static double length(double v[]){
 return Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
    }

    /** Scale the length of a vector. */
    public static void scale(double v[], double s){
 v[0] *= s; v[1] *= s; v[2] *= s;
    }

    /** Working space for angle calculations. */
    private double dd1[] = new double[3];
    private double dd3[] = new double[3];
    private double pos_d[] = new double[3];

    /** Small value for zero comparisons. */
    private static double R_SMALL = 0.000000001;

    /**
     * Return angle between two vectors.
     *
     * Adapted from PyMol.
     */
    public double angle2(double v10[], double v23[], double v12[]){
 double result = 0.0;

 if(length(v12) < R_SMALL){
     result = angle(v10, v23);
 }else{
     cross(dd1, v12, v10);
     cross(dd3, v12, v23);
     if(length(dd1) < R_SMALL || length(dd3) < R_SMALL){
  result = angle(v10, v23);
     }else{
  result = angle(dd1, dd3);
  cross(pos_d, v12, dd1);
  if(dot(dd3, pos_d) < 0.0){
      result = -result;
  }
     }
 }

 return result;
    }

    /** another angle function. */
    public double angle(double ref[], double n1[], double n2[]){
 double result = angle(ref, n1);

 if(dot(ref, n2) < 0.0){
     result = -result;
 }

 return result;
    }

    /** Angle between two vectors. */
    public double angle(double v1[], double v2[]){
 double denom = length(v1) * length(v2);
 double result = 0.0;

 if(denom > R_SMALL){
     result = dot(v1, v2)/denom;
 }else{
     result = 0.0;
 }

 if(result < -1.0){
     result = -1.0;
 }if(result > 1.0){
     result = 1.0;
 }

 result = Math.acos(result);

 return result;
    }

    /** Print a vector. */
    private static void print(String s, double x[]){
 Format.print(System.out, "%-10s", s);
 Format.print(System.out, " %8.3f,", x[0]);
 Format.print(System.out, " %8.3f,", x[1]);
 Format.print(System.out, " %8.3f\n", x[2]);
    }

    private static int dotCount = 45;

    /** Print an int. */
    private static void print(String s, int i){
 int len = s.length();
 System.out.print(s);
 System.out.print(" ");
 for(int dot = len + 1; dot < dotCount; dot++){
     System.out.print(".");
 }

 Format.print(System.out, " %7d\n", i);
    }

    /** Print a double. */
    private static void print(String s, double d){
 int len = s.length();
 System.out.print(s);
 System.out.print(" ");
 for(int dot = len + 1; dot < dotCount; dot++){
     System.out.print(".");
 }

 Format.print(System.out, " %10.2f\n", d);
    }

    /** Print a String. */
    private static void print(String s, String s2){
 int len = s.length();
 System.out.print(s);
 System.out.print(" ");
 for(int dot = len + 1; dot < dotCount; dot++){
     System.out.print(".");
 }

 System.out.print(" ");
 System.out.println(s2);
    }

    /* Sphere neighbours. */
    private int first[] = null;
    private int count[] = null;
    private int nn[] = null;
    private int neighbourCount = 0;

    private int commonNeighbours[] = null;
    private int commonCount = 0;
    private int mergeNeighbours[] = null;
    private int mergeCount = 0;

    /**
     * Build a list of each spheres neighbours.
     *
     * A neighbour is any sphere within ri + rj + 2 * rp
     */
    public void buildNeighbourList(){
 first = new int[nxyz];
 count = new int[nxyz];
 // use IntArray to dynamically grow the 
 // neighbour list
 IntArray nList = new IntArray(nxyz*60);

 int maxNeighbours = 0;

 // replace with more sophisticated algorithm later
 for(int i = 0; i < nxyz; i++){
     double ri = radius2[i];
     first[i] = neighbourCount;
     for(int j = 0; j < nxyz; j++){
  double dij2 = distance2(xyz[i], xyz[j]);
  double rirj = ri + radius2[j];
  //double trad = torusRadius(radius[i], radius[j], Math.sqrt(dij2), probeRadius);
  if(dij2 < rirj*rirj /*  && trad > probeRadius*/ ){
      if(i != j){
   count[i]++;
   nList.add(j);
   //nn[neighbourCount] = j;
   neighbourCount++;
      }
  }
     }

     // record the maximum number of neighbours
     if(count[i] > maxNeighbours){
  maxNeighbours = count[i];
     }
 }

 // grab the neighbour list for easy reference
 nn = nList.getArray();

 print("total neighbours", neighbourCount);
 print("maximum neighbours", maxNeighbours);

 // allocate space for common neighbours.
 commonNeighbours = new int[maxNeighbours];

 // allocate space for merged neighbours.
 mergeNeighbours = new int[maxNeighbours * 3];
    }

    /**
     * Return common elements of sorted arrays a and b.
     * c is assumed long enough to receive all the elements.
     */
    public static int commonElements(int a[], int firsta, int na,
         int b[], int firstb, int nb,
         int c[]){

 // if either is the empty set, the intersection is empty
 if(na == 0 || nb == 0){
     return 0;
 }

 int enda = firsta + na;
 int endb = firstb + nb;

 int j = firsta;
 int k = firstb;
 int t = 0;

 while(j < enda && k < endb){
     if(a[j] == b[k]){
  c[t] = a[j];
  t++;
  j++;
  k++;
     }else if(a[j] < b[k]){
  j++;
     }else{
  k++;
     }
 }

 return t;
    }

    /**
     * Merge the elements of a and b.
     * c is assumed long enough to receive all the elements.
     */
    public int mergeElements(int a[], int firsta, int na,
        int b[], int firstb, int nb,
        int c[]){

 // if either is the empty set, the intersection is empty
 int enda = firsta + na;
 int endb = firstb + nb;
 int j = firsta;
 int k = firstb;
 int t = 0;

 //printArray("---\na", a, firsta, na);
 //printArray("b", b, firstb, nb);

 while(j < enda && k < endb){

     if(a[j] < b[k]){
  c[t] = a[j];
  j++;
  t++;
     }else if(a[j] > b[k]){
  c[t] = b[k];
  k++;
  t++;
     }else{
  c[t] = a[j];
  t++;
  k++;
  j++;
     }
 }

 while(j < enda){
     c[t] = a[j];
     t++;
     j++;
 }

 while(k < endb){
     c[t] = b[k];
     t++;
     k++;
 }

 //printArray("c", c, 0, t);

 return t;
    }

    /** Print array section. */
    private void printArray(String s, int a[], int start, int n){
 System.out.print(s);
 for(int i = start; i < start+nxyz; i++){
     System.out.print(" " + a[i]);
 }
 System.out.println("");
    }

    /** Check to see if the range of the array is sorted. */
    private boolean checkSorted(int a[], int first, int count){
 int firsta = a[first];

 for(int i = first + 1; i < first + count; i++){
     if(a[i] < firsta){
  return false;
     }else{
  firsta = a[i];
     }
 }

 return true;
    }

    /** Are the two points within distance d of each other. */
    public boolean within(double x1, double y1, double z1,
     double x2, double y2, double z2,
     double d){
 double dx = x2 - x1;
 double dy = y2 - y1;
 double dz = z2 - z1;

 if(dx*dx + dy*dy + dz*dz < d*d){
     return true;
 }else{
     return false;
 }
    }

    /** Calculate distance between two points. */
    public double distance(double x1, double y1, double z1,
      double x2, double y2, double z2){
 double dx = x2 - x1;
 double dy = y2 - y1;
 double dz = z2 - z1;

 return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    /** Calculate squared distance between two points. */
    public double distance2(double x1, double y1, double z1,
       double x2, double y2, double z2){
 double dx = x2 - x1;
 double dy = y2 - y1;
 double dz = z2 - z1;

 return dx*dx + dy*dy + dz*dz;
    }

    /** Output the probes file. */
    public void outputProbes(DynamicArray pr, String filename){
 FILE output = FILE.write(filename);

 if(output == null){
     System.err.println("couldn't open " + filename);
     return;
 }

 for(int i = 0; i < pr.size(); i++){
     Probe p = (Probe)pr.get(i);
     output.print("%.3f", p.x[0]);
     output.print(" %.3f", p.x[1]);
     output.print(" %.3f", p.x[2]);
     output.print(" %.3f\n", probeRadius);
 }

 output.close();
    }

    /** Maximum number of points in sphere template. */
    private static int MAX_SPHERE_POINTS = 642;

    /** Maximum number of triangles in sphere template. */
    private static int MAX_SPHERE_TRIANGLES = 2*2*MAX_SPHERE_POINTS - 4;

    /* Sphere template data structures. */
    private double sx[][] = new double[MAX_SPHERE_POINTS][3];
    private double snx[][] = new double[MAX_SPHERE_POINTS][3];

    /* The transformed sphere points. */
    private double tsx[][] = new double[MAX_SPHERE_POINTS][3];

    /* Is the sphere point clipped. */
    private int clipped[] = new int[MAX_SPHERE_POINTS];

    /* Is the sphere point on the hull. */
    private int hull[] = new int[MAX_SPHERE_POINTS];

    /** Number of points in the sphere template. */
    private int nsp = 0;

    /* Sphere triangles. */
    private int si[] = new int[MAX_SPHERE_TRIANGLES];
    private int sj[] = new int[MAX_SPHERE_TRIANGLES];
    private int sk[] = new int[MAX_SPHERE_TRIANGLES];

    /** Vertex neighbours. */
    private int vn[][] = null;

    /** Vertex neighbour count. */
    private int vncount[] = new int[MAX_SPHERE_POINTS];

    /** Triangles neighbours. */
    private int tlist[][] = null;

    /** Vertex neighbour count. */
    private int tcount[] = new int[MAX_SPHERE_POINTS];

    /** Number of triangles in the sphere template. */
    private int nst = 0;

    /** Shortest edge length on sphere template. */
    private double shortestEdge = 0.0;

    /** Longest edge length on sphere template. */
    private double longestEdge = 0.0;

    /** Current longest edge. */
    private double currentLongestEdge = 0.0;

    /** Build sphere template. */
    private void buildSphereTemplate(int subDivisions){
 initialiseSphereTemplate();

 int firstTriangle = 0;
 int triangleCount = nst;

 int start = 0;
 int stop = 0;

 for(int sub = 0; sub < subDivisions; sub++){

     //System.out.println("subdivision " + sub);
     //System.out.println("firstTriangle " + firstTriangle);
     //System.out.println("triangleCount " + triangleCount);

     for(int t = firstTriangle; t < triangleCount; t++){
  int midij = findSpherePoint(si[t], sj[t]);
  int midjk = findSpherePoint(sj[t], sk[t]);
  int midki = findSpherePoint(sk[t], si[t]);

  addTriangle(midij, midjk, midki);
  addTriangle(si[t], midij, midki);
  addTriangle(sj[t], midjk, midij);
  addTriangle(sk[t], midki, midjk);
     }

     start = triangleCount;
     stop = nst;

     firstTriangle = triangleCount;
     triangleCount = nst;
 }

 // copy down the last group of triangles
 // as only the ones created by the final
 // subdivision are relevant
 nst = 0;

 for(int t = start; t < stop; t++){
     si[nst] = si[t];
     sj[nst] = sj[t];
     sk[nst] = sk[t];
     nst++;
 }

 for(int i = 0; i < nsp; i++){
     for(int j = 0; j < 3; j++){
  snx[i][j] = sx[i][j];
     }
 }

 longestEdge = 0.0;
 shortestEdge = 1.e10;

 for(int i = 0; i < nst; i++){
     int vi = si[i];
     int vj = sj[i];
     int vk = sk[i];
     double dedge = distance(sx[vi], sx[vj]);

     if(dedge < shortestEdge){
  shortestEdge = dedge;
     }
     if(dedge > longestEdge){
  longestEdge = dedge;
     }

     dedge = distance(sx[vi], sx[vk]);

     if(dedge < shortestEdge){
  shortestEdge = dedge;
     }
     if(dedge > longestEdge){
  longestEdge = dedge;
     }

     dedge = distance(sx[vk], sx[vj]);

     if(dedge < shortestEdge){
  shortestEdge = dedge;
     }
     if(dedge > longestEdge){
  longestEdge = dedge;
     }

 }

 // build the vertex neighbour list.
 vn = new int[nsp][6];

 for(int i = 0; i < nst; i++){
     addNeighbour(si[i], sj[i]);
     addNeighbour(si[i], sk[i]);
     addNeighbour(sj[i], sk[i]);
 }

 // build the triangle list
 tlist = new int[nsp][6];

 for(int i = 0; i < nst; i++){
     int vi = si[i];
     int vj = sj[i];
     int vk = sk[i];
     tlist[vi][tcount[vi]++] = i;
     tlist[vj][tcount[vj]++] = i;
     tlist[vk][tcount[vk]++] = i;
 }

 //print("sphere template shortest edge", shortestEdge);
 //print("sphere template longest edge", longestEdge);

 print("points in sphere template", nsp);
 print("triangles in sphere template", nst);

 //outputSphereTemplate();
    }

    /** How many hull neighbours does this point have. */
    private int getHullCount(int svi){
 int hullCount = 0;

 for(int j = 0; j < vncount[svi]; j++){
     if(hull[vn[svi][j]] == 1){
  hullCount++;
     }
 }

 return hullCount;
    }

    /** Does this sphere vertex have the other one as a neighbour. */
    private boolean addNeighbour(int i, int v){
 for(int j = 0; j < vncount[i]; j++){
     if(vn[i][j] == v){
  return true;
     }
 }

 vn[i][vncount[i]++] = v;
 vn[v][vncount[v]++] = i;

 return false;
    }

    /** Add a triangle to the data structure. */
    private void addTriangle(int ti, int tj, int tk){
 si[nst] = ti;
 sj[nst] = tj;
 sk[nst] = tk;
 nst++;
    }

    /** Find the position of the current mid point. */
    private int findSpherePoint(int i, int j){
 double mx = 0.5 * (sx[i][0] + sx[j][0]);
 double my = 0.5 * (sx[i][1] + sx[j][1]);
 double mz = 0.5 * (sx[i][2] + sx[j][2]);
 double len = Math.sqrt(mx*mx + my*my + mz*mz);
 mx /= len; my /= len; mz /= len;

 for(int d = 0; d < nsp; d++){
     if(within(mx, my, mz, sx[d][0], sx[d][1], sx[d][2], 0.0001)){
  //System.out.println("found point " + d);
  return d;
     }
 }

 sx[nsp][0] = mx; sx[nsp][1] = my; sx[nsp][2] = mz;

 nsp++;

 return nsp - 1;
    }

    /** Output the sphere template as a tmesh. */
    private void outputSphereTemplate(){
 try {
     FileOutputStream outputStream =
  new FileOutputStream("sphere.tmesh");
     PrintStream ps = new PrintStream(outputStream);

     ps.println(nsp);

     for(int i = 0; i < nsp; i++){
  Format.print(ps, " %.4f", sx[i][0]);
  Format.print(ps, " %.4f", sx[i][1]);
  Format.print(ps, " %.4f", sx[i][2]);
  Format.print(ps, " %.4f", snx[i][0]);
  Format.print(ps, " %.4f", snx[i][1]);
  Format.print(ps, " %.4f", snx[i][2]);
  ps.println(" 0 0 0");
     }

     ps.println(nst);

     for(int i = 0; i < nst; i++){
  ps.println("3");
  ps.println(si[i]);
  ps.println(sj[i]);
  ps.println(sk[i]);
     }

     ps.close();

 }catch(Exception e){
     System.out.println(e);
 }
    }

    /** Initialise the sphere template. */
    private void initialiseSphereTemplate(){
 sx[0][0] = -0.851024; sx[0][1] = 0; sx[0][2] = 0.525126;
 sx[1][0] = 0; sx[1][1] = 0.525126; sx[1][2] = -0.851024;
 sx[2][0] = 0; sx[2][1] = 0.525126; sx[2][2] = 0.851024;
 sx[3][0] = 0.851024; sx[3][1] = 0; sx[3][2] = -0.525126;
 sx[4][0] = -0.525126; sx[4][1] = -0.851024; sx[4][2] = 0;
 sx[5][0] = -0.525126; sx[5][1] = 0.851024; sx[5][2] = 0;
 sx[6][0] = 0; sx[6][1] = -0.525126; sx[6][2] = 0.851024;
 sx[7][0] = 0.525126; sx[7][1] = 0.851024; sx[7][2] = 0;
 sx[8][0] = 0; sx[8][1] = -0.525126; sx[8][2] = -0.851024;
 sx[9][0] = 0.851024; sx[9][1] = 0; sx[9][2] = 0.525126;
 sx[10][0] = 0.525126; sx[10][1] = -0.851024; sx[10][2] = 0;
 sx[11][0] = -0.851024; sx[11][1] = 0; sx[11][2] = -0.525126;
 nsp = 12;
 si[0] = 9; sj[0] = 2; sk[0] = 6;
 si[1] = 1; sj[1] = 5; sk[1] = 11;
 si[2] = 11; sj[2] = 1; sk[2] = 8;
 si[3] = 0; sj[3] = 11; sk[3] = 4;
 si[4] = 3; sj[4] = 7; sk[4] = 1;
 si[5] = 3; sj[5] = 1; sk[5] = 8;
 si[6] = 9; sj[6] = 3; sk[6] = 7;
 si[7] = 0; sj[7] = 2; sk[7] = 6;
 si[8] = 4; sj[8] = 6; sk[8] = 10;
 si[9] = 1; sj[9] = 7; sk[9] = 5;
 si[10] = 7; sj[10] = 2; sk[10] = 5;
 si[11] = 8; sj[11] = 10; sk[11] = 3;
 si[12] = 4; sj[12] = 11; sk[12] = 8;
 si[13] = 9; sj[13] = 2; sk[13] = 7;
 si[14] = 10; sj[14] = 6; sk[14] = 9;
 si[15] = 0; sj[15] = 11; sk[15] = 5;
 si[16] = 0; sj[16] = 2; sk[16] = 5;
 si[17] = 8; sj[17] = 10; sk[17] = 4;
 si[18] = 3; sj[18] = 9; sk[18] = 10;
 si[19] = 6; sj[19] = 4; sk[19] = 0;
 nst = 20;
    }

    /** Driver routine for the surface generation algorithm. */
    public static void main(String args[]){
 double qLength[] = {0.0, 1.5, 0.9, 0.5, 0.3};
 double probeRadius = 1.5;
 int subdivisions = 1;
 double edgeLength = 1.5;
 String probesFilename = null;

 if(args.length == 0){
     System.out.print("usage: java surface [-r rp] [-e len]");
     System.out.print(" [-d subdiv] [-o tmesh] [-t] [-q int]");
     System.out.println(" file.xyzr");
     System.exit(1);
 }

 int lastArg = args.length - 1;

 String inputFile = null;
 String tmeshFile = "surface.tmesh";

 boolean faceType = false;

 for(int i = 0; i < args.length; i++){
     if(args[i].equals("-r")){
  if(i < lastArg){
      probeRadius = FILE.readDouble(args[++i]);
  }
     }else if(args[i].equals("-e")){
  if(i < lastArg){
      edgeLength = FILE.readDouble(args[++i]);
  }
     }else if(args[i].equals("-d")){
  if(i < lastArg){
      subdivisions = FILE.readInteger(args[++i]);
  }
     }else if(args[i].equals("-q")){
  if(i < lastArg){
      int quality = FILE.readInteger(args[++i]);
      if(quality > 0 && quality < qLength.length){
   subdivisions = quality;
   edgeLength = qLength[quality];
      }
  }
     }else if(args[i].equals("-o")){
  if(i < lastArg){
      tmeshFile = args[++i];
      if(tmeshFile.equals("none")){
   tmeshFile = null;
      }
  }
     }else if(args[i].equals("-t")){
  faceType = true;
     }else if(args[i].equals("-p")){
  if(i < lastArg){
      probesFilename = args[++i];
  }
     }else{
  if(i == lastArg){
      inputFile = args[i];
  }else{
      System.err.println("error: unknown command line argument " +
           args[i]);
      System.exit(1);
  }
     }
 }

 if(inputFile == null){
     System.err.println("error: no input file specified.");
     System.exit(1);
 }


 FILE f = FILE.open(inputFile);

 if(f == null){
     System.out.println("error: couldn't open input file " + inputFile);
     System.exit(2);
 }

 DoubleArray x = new DoubleArray(1024);
 DoubleArray y = new DoubleArray(1024);
 DoubleArray z = new DoubleArray(1024);
 DoubleArray r = new DoubleArray(1024);
 IntArray visible = new IntArray(1024);
 int n = 0;

 while(f.nextLine()){
     if(f.getFieldCount() >= 4){
  x.add(f.getDouble(0));
  y.add(f.getDouble(1));
  z.add(f.getDouble(2));
  r.add(f.getDouble(3));
  visible.add(1);
  n++;
     }
 }

 f.close();

 double xxx[][] = new double[n][3];

 for(int i = 0; i < n; i++){
     xxx[i][0] = x.get(i);
     xxx[i][1] = y.get(i);
     xxx[i][2] = z.get(i);
 }

 print("number of spheres", n);

 AnaSurface s = new AnaSurface(xxx, r.getArray(), visible.getArray(), null, n);

 s.density = subdivisions;
 s.probeRadius = probeRadius;
 s.desiredTriangleLength = edgeLength;
 s.probesFilename = probesFilename;

 Tmesh tmesh = s.construct();

 if(faceType == false){
     for(int i = 0; i < tmesh.nt; i++){
  tmesh.tcolor[i] = 0;
     }
 }

 if(tmeshFile != null){
     print("tmesh written to", tmeshFile);
     tmesh.output(tmeshFile);
 }
    }
}
