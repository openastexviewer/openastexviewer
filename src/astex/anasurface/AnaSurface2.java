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
import java.util.*;

public class AnaSurface2 {
    /** Are we debugging. */
    private static boolean debug = false;

    public static void handleCommand(MoleculeViewer mv,
                                     MoleculeRenderer mr,
                                     Arguments args){
        DynamicArray surfaceAtoms = (DynamicArray)args.get("-selection");

        if(surfaceAtoms == null){
            Log.error("you must specify atoms to surface");
            return;
        }
        
        double probeRadius = args.getDouble("-proberadius", 1.5);
        int depth          = args.getInteger("-tesselation", 2);
        debug              = args.getBoolean("-debug", false);
        targetLen          = args.getDouble("-targetlen", 0.7);
        clipTolerance      = args.getDouble("-cliptolerance", 0.2);
        rextra             = args.getDouble("-rextra", 0.0);

        mr.renderer.debug  = args.getBoolean("-renderdebug", false);

        Tmesh surface = generateSurface(surfaceAtoms, probeRadius, depth);
        surface.colorStyle = Tmesh.TriangleColor;

        surface.setName(args.getString("-name", "anasurface1"));

        mr.addGraphicalObject(surface);
    }

    /** The extra radius for generating a solvent accessible surface. */
    public static double rextra = 0.0;

    /** The target length for toroidal triangles. */
    public static double targetLen = 0.7;

    /** The tolerance for clipping points. */
    public static double clipTolerance = 0.2;

    static double atoms[][]   = null;
    static int atomCount      = 0;
    static double rp          = 0.0;
    static double dp          = 0.0;
    static double rp2         = 0.0;
    static double dp2         = 0.0;
    static double r[]         = null;
    static double rext[]      = null;

    static int neighbours[]   = null;
    static int neighbourCount = 0;

    static DynamicArray probes       = new DynamicArray();
    static DynamicArray torusProbes  = new DynamicArray();

    static Tmesh atomSurface = null;

    static DynamicArray atomProbes[] = null;

    public static void initialiseSurface(DynamicArray surfaceAtoms,
                                         double probeRadius,
                                         int tesselationDepth){
        rp = probeRadius;
        dp = 2.0 * rp;
        rp2 = rp * rp;
        dp2 = dp * dp;

        atomCount = surfaceAtoms.size();
        atoms = new double[atomCount][3];

        neighbours = new int[atomCount];

        r = new double[atomCount];
        rext = new double[atomCount];

        for(int i = 0; i < atomCount; i++){
            Atom ai = (Atom)surfaceAtoms.get(i);
            atoms[i] = new double[3];
            atoms[i][0] = ai.x;
            atoms[i][1] = ai.y;
            atoms[i][2] = ai.z;
            r[i]     = ai.getVDWRadius() + rextra;
            rext[i]  = r[i] + rp;
        }

        buildSphereTemplate(tesselationDepth);

        if(atomSurface == null){
            atomSurface = new Tmesh();
        }

        atomProbes = new DynamicArray[atomCount];
    }

    static IntArray torusEdgesPoints = new IntArray();

    static Hashtable torusEdgesHash = new Hashtable();

    static Tmesh surface = null;

    public static Tmesh generateSurface(DynamicArray surfaceAtoms,
                                        double probeRadius,
                                        int tesselationDepth){
        double p0[] = new double[3];
        double p1[] = new double[3];

        initialiseSurface(surfaceAtoms, probeRadius, tesselationDepth);

        surface = new Tmesh();

        for(int i = 0; i < atomCount; i++){
            //print.f("-----\natom i " + i);

            if((i+1) % 100 == 0){
                print.f("" + (i+1));
            }

            Atom atom = (Atom)surfaceAtoms.get(i);

            if(debug){
                //if(i != 6) continue;
                //if(i < atomCount - 1) continue;
            }

            double ai[] = atoms[i];
            double ri   = r[i];

            //setCentralAtom(i);
            buildNeighbourList(i);

            for(int jn = 0; jn < neighbourCount; jn++){
                int j = neighbours[jn];
                
                if(j < i) continue;

                //print.f("atom j " + j);
                double aj[] = atoms[j];
                double rj   = r[j];

                for(int kn = jn + 1; kn < neighbourCount; kn++){
                    int k = neighbours[kn];

                    if(k < j) continue;

                    //print.f("atom k " + k);
                    double ak[] = atoms[k];
                    double rk   = r[k];

                    double d2 = rext[k] + rext[j];

                    if(distance2(aj, ak) > d2*d2) continue;

                    if(constructProbePlacement(ai, ri,
                                               aj, rj,
                                               ak, rk,
                                               rp, p0, p1)){
                        for(int pos = 0; pos < 2; pos++){
                            double probe[] = (pos == 0) ? p0 : p1;

                            if(!obscured(probe, i, j, k)){
                                //surface.addSphere(probe[0], probe[1], probe[2],
                                //                  rp, Color32.magenta);
                                //print("adding point", probe) ;
                                Probe p = processPlacement(probe, i, j, k);

                                for(int aa = 0; aa < 3; aa++){
                                    int slot = -1;
                                    if(aa == 0) slot = i;
                                    if(aa == 1) slot = j;
                                    if(aa == 2) slot = k;

                                    if(atomProbes[slot] == null){
                                        atomProbes[slot] = new DynamicArray(4);
                                    }

                                    atomProbes[slot].add(p);
                                }
                            }
                        }
                    }
                }
            }
        }

        summarizeAtomProbes();

        for(int i = 0; i < atomCount; i++){
            //print.f("-----\natom i " + i);
            
            if((i+1) % 100 == 0){
                print.f("" + (i+1));
            }
            
            Atom atom = (Atom)surfaceAtoms.get(i);
            
            if(debug){
                //if(i != 6) continue;
                //if(i < atomCount - 1) continue;
            }
            
            double ai[] = atoms[i];
            double ri   = r[i];
            
            setCentralAtom(i);
            
            int probeCount = getAtomProbeCount(i);
            
            probes = atomProbes[i];
            
            for(int p = 0; p < probeCount; p++){
                Probe probe = (Probe)probes.get(p);
                Vertex v = probe.getVertexForAtom(i);
                if(v == null){
                    print.f("null vertex for atom " + i);
                }else{
                    vector(n, v.x, probe.x);
                    normalise(n);
                    v.vi = addPoint(v.x, n, 1);
                    
                }
            }

            for(int jn = 0; jn < neighbourCount; jn++){
                int j = neighbours[jn];

                //print.f("atom j " + j);
                double aj[] = atoms[j];
                double rj   = r[j];

                setNeighbourAtom(j);

                for(int p = 0; p < probeCount; p++){
                    Probe probe = (Probe)probes.get(p);
                    if(probe.involves(j)){
                        torusProbes.add(probe);
                    }
                }

                processTorus(i, j);
            }
            
            processAtom(i, atom.getColor());
        }


        return surface;
    }

    /** mapping of points in atom object to overall object. */
    private static int pointMap[] = new int[1024];

    private static Hashtable sphereEdgesHash = new Hashtable();
    private static IntArray  spherePoints    = new IntArray();
    private static IntArray  delauneyPoints  = new IntArray();

    /**
     * Carry out all processing of the different
     * features that contribute to this atoms surface.
     */
    private static void processAtom(int iatom, int c){
        //print.f("probes.size() " + probes.size());

        if(getAtomProbeCount(iatom) == 0 && neighbourCount != 0){
            return;
        }

        for(int sp = 0; sp < nsp; sp++){
            for(int in = 0; in < neighbourCount; in++){
                double rr = rext[neighbours[in]] + clipTolerance;
                if(distance2(tsextx[sp], atoms[neighbours[in]]) < (rr*rr)){
                    clipped[sp] = 1;
                    break;
                }
            }
        }

        copySphereTriangles2(false, spherePoints, sphereEdgesHash, true);

        int tc = torusEdgesPoints.size();

        for(int i = 0; i < tc; i++){
            spherePoints.add(torusEdgesPoints.get(i));
        }

        stitch(torusEdgesHash, spherePoints);

        //stitch(spherePoints, sphereEdgesHash,
        //       torusEdgesPoints, torusEdgesHash, delauneyPoints);

        for(int p = 0; p < probes.size(); p++){
            //for(int p = 0; p < 1; p++){
            Probe probe = (Probe)probes.get(p);

            processProbe(iatom, probe);
        }

        // copy the atom surface to the overall surface.
        int anp = atomSurface.np;

        if(anp > pointMap.length){
            pointMap = new int[anp];
        }

        for(int i = 0; i < anp; i++){
            pointMap[i] =
                surface.addPoint(atomSurface.x[i], atomSurface.y[i], atomSurface.z[i],
                                 atomSurface.nx[i], atomSurface.ny[i], atomSurface.nz[i],
                                 0.0, 0.0);
        }

        int ant = atomSurface.nt;

        for(int i = 0; i < ant; i++){
            addTriangle(surface,
                        pointMap[atomSurface.t0[i]],
                        pointMap[atomSurface.t1[i]],
                        pointMap[atomSurface.t2[i]],
                        debug ? atomSurface.tcolor[i] : c);
        }
    }

    /** Plane origin. */
    private static double po[] = new double[3];

    /** Plane normal. */
    private static double pn[] = new double[3];

    /** Plane midpoint. */
    private static double pm[] = new double[3];

    /** Plane midpoint. */
    private static double ref[] = new double[3];

    /** cusp midpoint. */
    private static double cuspm[] = new double[3];

    /** Probe edge list. */
    private static Hashtable probeEdges = new Hashtable();

    private static void processProbe(int iatom, Probe probe){
        setSphereTemplate(probe.x, rp, rp);
            
        // clip to the probe boundary
        constructPlane(pn, probe.x, atoms[probe.i], atoms[probe.j], atoms[probe.k]);
        clipPlane(probe.x, pn);
            
        constructPlane(pn, probe.x, atoms[probe.j], atoms[probe.k], atoms[probe.i]);
        clipPlane(probe.x, pn);
            
        constructPlane(pn, probe.x, atoms[probe.k], atoms[probe.i], atoms[probe.j]);
        clipPlane(probe.x, pn);
            
        // clip to the other possibly impingeing 
        // probe spheres
        for(int i = 0; i < probes.size(); i++){
            Probe probe2 = (Probe)probes.get(i);
            if(probe2 != probe){
                if(distance2(probe2.x, probe.x) < dp2){
                    vector(pn, probe2.x, probe.x);
                    normalise(pn);
                    
                    mid(pm, probe2.x, probe.x);
                    
                    clipPlane(pm, pn);
                }
            }
        }

        probeEdges.clear();
        spherePoints.removeAllElements();

        constructCenterPoint(pm, probe.x, probe.edge0.v0.x, probe.edge1.v0.x, probe.edge2.v0.x);
        
        vector(pn, pm, probe.x);
        normalise(pn);
        
        int vid = 
            addPoint(pm[0], pm[1], pm[2],
                     pn[0], pn[1], pn[2],
                     1);
        
        if(debug){
            surface.addSphere(pm[0], pm[1], pm[2], 0.03, Color32.white);
        }

        Edge e0 = probe.getEdgeTo(iatom);

        Probe otherProbe = findMatchingProbe(probe, e0.get(0));

        constructProbeArc(iatom,
                          probe, otherProbe, e0,
                          e0.get(0), vid,
                          probeEdges, spherePoints);

        Edge e1 = probe.getEdgeFrom(iatom);

        otherProbe = findMatchingProbe(probe, e1.getReverse(0));

        constructProbeArc(iatom,
                          probe, otherProbe, e1,
                          e1.getReverse(0), vid,
                          probeEdges, spherePoints);

        copySphereTriangles2(true, spherePoints, sphereEdgesHash, false);

        constructProbeEdgeList(probeEdges, spherePoints, e0, e1, false);

        spherePoints.add(vid);
        
        stitch(probeEdges, spherePoints);
    }

    
    private static double cv[] = new double[3];
    private static double cp[] = new double[3];
    private static double cuspn[] = new double[3];

    /**
     * Make a half arc for this probe edge.
     * Also construct the probe-probe intersection arc if
     * it is a cusp edge.
     */
    private static void constructProbeArc(int iatom,
                                          Probe probe, Probe otherProbe, Edge e,
                                          int vlast, int vmid,
                                          Hashtable edges, IntArray points){
        atomSurface.getVertex(vlast, ref, null);

        if(e.selfIntersects && otherProbe != null){
            // we got it
            mid(cuspm, probe.x, otherProbe.x);
            vector(cv, cuspm, e.torus.tij);
            normalise(cv);
            
            double dprobes = 0.5 * distance(probe.x, otherProbe.x);
            
            if(dprobes > rp){
                print.f("bad interprobe distance " + dprobes);
            }
            
            double reff = Math.sqrt(rp2 - dprobes*dprobes);
            
            for(int i = 0; i < 3; i++){
                cp[i] = cuspm[i] + cv[i] * reff;
            }

            /*
              print("probe       ", probe.x);
              print("otherProbe  ", otherProbe.x);
        
              print("cuspm       ", cuspm);
              print("torus center", e.torus.tij);
              print("cusp point  ", cp);
            */

            if(debug){
                surface.addSphere(cp[0], cp[1], cp[2], 0.03, Color32.white);
                surface.addSphere(e.torus.tij[0], e.torus.tij[1], e.torus.tij[2],
                                  0.03, Color32.white);
                surface.addSphere(cuspm[0], cuspm[1], cuspm[2], 0.03, Color32.white);
                surface.addCylinder(cuspm[0], cuspm[1], cuspm[2],
                                    cp[0], cp[1], cp[2],
                                    0.03, Color32.white,Color32.white);
            }
            
            vector(cuspn, cp, probe.x);
            normalise(cuspn);
            
            int finalPoint = addPoint(cp[0], cp[1], cp[2],
                                      cuspn[0], cuspn[1], cuspn[2],
                                      1);
            
            constructPlane(pn, probe.x, pm, cp, atoms[iatom]);
            clipPlane(probe.x, pn);
            
            constructArc(cp, finalPoint, pm, vmid, probe.x, spherePoints, probeEdges, probe.x, false);
            constructArc(cp, finalPoint, ref, vlast, cuspm, spherePoints, probeEdges, probe.x, false);
            
            spherePoints.add(finalPoint);
            
        }else{
            // make clip planes from end of edge to mid point
            constructPlane(pn, probe.x, pm, ref, atoms[iatom]);
            clipPlane(probe.x, pn);
            
            constructArc(ref, vlast, pm, vmid, probe.x, spherePoints, probeEdges, probe.x, false);
        }
    }

    private static double vpos[] = new double[3];

    private static Probe findMatchingProbe(Probe probe, int v){
        atomSurface.getVertex(v, vpos, null);
        double d = distance(probe.x, vpos);

        //print.f("initial distance " + d);

        for(int i = 0; i < probes.size(); i++){
            Probe otherProbe = (Probe)probes.get(i);
            if(otherProbe != probe){
                double dd = distance(otherProbe.x, vpos);
                if(Math.abs(dd - d) < 1.e-5){
                    return otherProbe;
                }
            }
        }
        
        return null;
    }

    /**
     * Make an arc from one point to the other.
     */
    private static void constructArc(double p0[], int v0, double p1[], int v1,
                                     double cen[],
                                     IntArray points,
                                     Hashtable edges,
                                     double ndir[],
                                     boolean initialise){
        if(initialise){
            points.removeAllElements();
            edges.clear();
        }

        // calculate the wrap angle
        // and the offsets for this torus segment.
        vector(pp2cij, cen, p0);
        vector(pp2cji, cen, p1);
        double wrapAngle = angle(pp2cij, pp2cji);
        
        //print.f("wrapAngle " + wrapAngle);

        cross(n1, pp2cij, pp2cji);
        cross(n2, n1, pp2cij);
        normalise(n2);
        normalise(pp2cij);

        double rscale = distance(p0, cen);

        double wrapArcLength = wrapAngle * rscale;

        // always at least two points
        // use twice the target length as we only have half
        // the torus
        int nwap = 2 + (int)(wrapArcLength/(targetLen * rscale));
        int nwap1 = nwap - 1;

        double wrapAngleStep = wrapAngle/(nwap1);

        double wa = 0.0;

        //print.f("v0 %d\n", v0);
        //print.f("v1 %d\n", v1);

        // now interpolate from one end
        // of the arc to the other.
        for(int ii = 0; ii < nwap; ii++){
            double sinwa = Math.sin(wa);
            double coswa = Math.cos(wa);

            // tidy up any slight rounding error
            if(wa > wrapAngle){
                wa = wrapAngle;
            }
		
            // relative vector
            for(int i = 0; i < 3; i++){
                ntp[i] = coswa * pp2cij[i] + sinwa * n2[i];
                tp[i] = rscale * ntp[i] + cen[i];
                //ntp[i] = -ntp[i];
            }

            vector(ntp, tp, ndir);
            normalise(ntp);

            // record the vertex index
            int vid = 0;

            // need to get the tmesh vertices for the
            // corner points from the vertex objects
            // to ensure mesh sharing
            if(ii == 0){
                vid = v0;
            }else if(ii == nwap1){
                vid = v1;
            }else{
                // not end
                // need a new point
                vid = addPoint(tp, ntp, 1);
                if(debug){
                    surface.addSphere(tp[0], tp[1], tp[2], 0.08, Color32.white);
                }
            }

            // put an edge in the data structure.
            if(ii > 0){
                if(ii == 1){
                    addEdge(edges, v0, vid, 1);
                }else{
                    addEdge(edges, points.getReverse(0), vid, 1);
                }
            }

            if(ii > 0 && ii < nwap1){
                points.add(vid);
            }

            wa += wrapAngleStep;
        }
    }

    private static void constructProbeEdgeList(Hashtable edges, IntArray points,
                                               Edge e0, Edge e1, boolean initialise){

        if(initialise){
            edges.clear();
        }

        if(e0.getReverse(0) != e1.get(0)){
            print.f("unmatched edge starts");
            return;
        }

        int e0count = e0.size();

        for(int i = 0; i < e0count; i++){
            if(i < e0count - 1){
                points.add(e0.get(i));
            }
            if(i > 0){
                addEdge(edges, e0.get(i - 1), e0.get(i), 1);
            }
        }

        int e1count = e1.size();

        for(int i = 0; i < e1count; i++){
            points.add(e1.get(i));

            if(i > 0){
                addEdge(edges, e1.get(i - 1), e1.get(i), 1);
            }
        }
    }

    private static void constructCenterPoint(double c[], double p[],
                                             double v0[], double v1[], double v2[]){
        for(int i = 0; i < 3; i++){
            c[i] = (v0[i] + v1[i] + v2[i])/3.0;
        }

        //surface.addSphere(c[0], c[1], c[2], 0.07, Color32.red);

        vector(po, p, c);
        normalise(po);

        for(int i = 0; i < 3; i++){
            c[i] = p[i] + po[i] * rp;
        }

        //surface.addSphere(c[0], c[1], c[2], 0.07, Color32.red);
    }

    /**
     * Clip the current sphere template by the plane
     * specified by the origin o and normal n.
     */
    private static void clipPlane(double o[], double n[]){
        for(int isp = 0; isp < nsp; isp++){
            if(clipped[isp] == 0){
                // plane equation
                if(plane_eqn(tsx[isp], o, n) < clipTolerance){
                    clipped[isp] = 1;
                }
            }
        }

        int remaining = 0;

        for(int isp = 0; isp < nsp; isp++){
            if(clipped[isp] == 0){
                remaining++;
            }
        }

        //print.f("nsp %d ", nsp);
        //print.f("remaining %d\n", remaining);
    }

    private static double v0[] = new double[3];
    private static double v1[] = new double[3];
    //private static double vc[] = new double[3];
    private static double vref[] = new double[3];

    private static void constructPlane(double n[],
                                       double x[],
                                       double a[], double b[], double ref[]){

        vector(v0, x, a);
        vector(v1, x, b);
        vector(vref, a, ref);
        cross(n, v0, v1);
        normalise(n);
        if(dot(n, vref) < 0.0){
            negate(n);
        }
    }

    /** Edges of the sphere template that are on the triangle hull. */
    private static Hashtable hullEdges = new Hashtable();

    /** Copy the unclipped sphere triangles to the atom surface. */
    private static void copySphereTriangles2(boolean negateNormals,
                                             IntArray points,
                                             Hashtable edges,
                                             boolean initialise){
        if(initialise){
            edges.clear();
            points.removeAllElements();
        }

        double mul = negateNormals ? -1.0 : 1.0;

        for(int sp = 0; sp < nsp; sp++){
            if(clipped[sp] != 1){
                clipped[sp] =
                    addPoint(tsx[sp][0], tsx[sp][1], tsx[sp][2],
                             mul * snx[sp][0], mul * snx[sp][1], mul * snx[sp][2],
                             0);
                if(debug){
                    surface.addSphere(tsx[sp][0],
                                      tsx[sp][1],
                                      tsx[sp][2], 0.02, Color32.white);
                }
                hull[sp] = 0;
                
                points.add(clipped[sp]);
            }
        }
    }

    private static final Integer INT0 = new Integer(0);
    private static final Integer INT1 = new Integer(1);
    private static final Integer INT2 = new Integer(2);

    private static boolean addEdge(Hashtable h, int i, int j, int c){
        if(i > j){
            int tmp = i;
            i = j;
            j = tmp;
        }

        int key = (i << 15) | j;

        Integer edge = new Integer(key);

        Integer count = (Integer)h.get(edge);

        if(c == -1){
            // increment the edge counter
            
            if(count == null){
                h.put(edge, INT1);
            }else if(count == INT1){
                h.put(edge, INT2);
            }else if(count == INT2){
                System.out.println("edge " + i + "," + j + " defined twice already");
                return false;
            }
        }else{
            if(count != null){
                System.out.println("edge " + i + "," + j + " defined already");
                return false;
            }else if(c == 0){
                // add a phantom edge, that we wish to guide
                // the triangulation
                h.put(edge, INT0);
            }else if(c == 1){
                h.put(edge, INT1);
            }else if(c == 2){
                h.put(edge, INT2);
            }else{
                System.out.println("can only specify edge use of (0), 1 or 2");
                return false;
            }
        }

        return true;
    }

    private static int getEdgeCount(Hashtable h, int i, int j){
        int key = (i << 15) | j;

        Integer edge = new Integer(key);

        Integer count = (Integer)h.get(edge);

        if(count == null){
            return 0;
        }else{
            return count.intValue();
        }
    }

    public static int debugColor[] = {
	0xff9999, 0x99ff99, 0x9999ff, 0xffff99, 0xff99ff, 0x99ffff
    };

    private static void addTriangle(Tmesh tmesh, int i, int j, int k, int c){
        if(debug){
            c = debugColor[tmesh.nt % debugColor.length];
        }
        tmesh.addTriangle(i, j, k, c);
    }

    static Edge torusEdges[]    = new Edge[64];
    static double torusAngles[] = new double[64];

    /** Vector form contact circle to torus vertex. */
    private static double cij2v[] = new double[3];

    private static void processTorus(int i, int j){
        int tpCount = torusProbes.size();

        if(tpCount == 0 && neighbourCount != 0){
            return;
        }

        Torus torus = new Torus(i, j);
        
        torusAxisUnitVector(torus.uij, atoms[i], atoms[j]);

        torus.rij = torusCenter(torus.tij, atoms[i], r[i], atoms[j], r[j], rp);
        
        if(torus.rij < rp){
            // if the radius is smaller than the probe
            // radius the torus intersects itself
            torus.selfIntersects = true;
            //torus.selfIntersects = false;
        }
        
        // generate contact circles on each end of torus
        torus.rcij = contactCircle(torus.cij, torus.tij, atoms[i], r[i], atoms[j], r[j]);
        // probably dont need this as we only make one half of torus per atom
        torus.rcji = contactCircle(torus.cji, torus.tij, atoms[j], r[j], atoms[i], r[i]);
        
        //surface.addSphere(torus.cij[0], torus.cij[1], torus.cij[2], 0.05, Color32.white);
        //surface.addSphere(torus.tij[0], torus.tij[1], torus.tij[2], 0.05, Color32.white);

        normal(torus.uijnorm, torus.uij);
        cross(torus.uijnorm2, torus.uij, torus.uijnorm);
        normalise(torus.uijnorm2);

        if(tpCount % 2 != 0){
            print.f("odd number of edges " + tpCount);
        }

        if(tpCount > torusEdges.length){
            torusEdges  = new Edge[tpCount];
            torusAngles = new double[tpCount];
        }

        for(int t = 0; t < tpCount; t++){
            Probe p = (Probe)torusProbes.get(t);
            Edge  e = p.getEdge(i, j);

            if(e == null){
                print.f("torus " + i + " " + j);
                print.f("e " + e);
                print.f("probe with null edge " + p);
            }

            torusEdges[t] = e;

	    if(e.v0.i == i){
		vector(cij2v, torus.cij, e.v0.x);
	    }else if(e.v1.i == i){
		vector(cij2v, torus.cij, e.v1.x);
	    }else{
		System.out.println("edge doesn't involve i " + i);
		System.out.println("edge has " + e.v0.i + "," + e.v1.i);
	    }

	    torusAngles[t] = angle(cij2v, torus.uijnorm, torus.uijnorm2);
        }

	// sort them using bubble sort...
	// will do for now (not usually more than 4)
	for (int ia = 0; ia < tpCount - 1; ia++) {
	    for (int ja = 0; ja < tpCount - 1 - ia; ja++){
		if (torusAngles[ja+1] > torusAngles[ja]) {
		    double tmp = torusAngles[ja];
		    torusAngles[ja] = torusAngles[ja+1];
		    torusAngles[ja+1] = tmp;
			
		    Edge etmp = torusEdges[ja];
		    torusEdges[ja] = torusEdges[ja+1];
		    torusEdges[ja+1] = etmp;
		}
	    }
	}

        /*
          for(int ee = 0; ee < tpCount; ee++){
          print.f("angle["+ee+"]="+ torusAngles[ee]);
          }
        */

	// check that we got the correct ordering
	for(int ee = 0; ee < tpCount - 1; ee++){
	    if(torusAngles[ee] < torusAngles[ee + 1]){
		System.out.println("!!!! error sorting vertex angles " +
				   torus.i + "," + torus.j);
	    }
	}

	for(int ee = 0; ee < tpCount; ee += 2){
	    Edge e0 = torusEdges[ee];
	    int ee1 = 0;
	    Edge e1 = null;

	    // depending on whether this edge start on i
	    // or runs to i, depends on whether the paired
	    // Edge is before or after us in the list.
	    if(e0.v1.i == torus.i){
		// can never overflow
		ee1 = ee + 1;
	    }else{
		ee1 = ee - 1;
		// make sure we get the last edge
		// if we ask for -1.
		if(ee1 == -1){
		    ee1 = tpCount - 1;
		}
	    }

	    e1 = torusEdges[ee1];

	    if(e0.v0.i != e1.v1.i || e0.v1.i != e1.v0.i){
		System.out.println("!! unpaired edges");
                e0.print("e0");
                e1.print("e1");
	    }

            //processTorusFace(torus, i, j, e0, e1,
            //                 torusAngles[ee], torusAngles[ee1]);
            if(e0.v1.i == torus.i){
                processTorusFace(torus, i, j, e1, e0,
                                 torusAngles[ee1], torusAngles[ee]);
            }else{
                processTorusFace(torus, i, j, e0, e1,
                                 torusAngles[ee], torusAngles[ee1]);
            }

            /*
              Edge e2 = null, e3 = null;

              e2 = addEdge(e0.v1, e1.v0, torus);
              e3 = addEdge(e1.v1, e0.v0, torus);
            */
        }
    }

    /** vector for coordinate frame. */
    private static double pp[] = new double[3];
    private static double ccij[] = new double[3];
    private static double ccji[] = new double[3];

    private static double vij2ji[] = new double[3];

    /** The point on the torus. */
    private static double tp[] = new double[3];

    /** The normal at the point on the torus. */
    private static double ntp[] = new double[3];

    /** Where the saddle face points end up in the tmesh. */
    static int tmeshv[][] = new int[100][100];

    /** Deltas for the wrap angle calculation. */
    private static double pp2cij[] = new double[3];
    private static double pp2cji[] = new double[3];
    private static double n1[] = new double[3];
    private static double n2[] = new double[3];

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
    private static void processTorusFace(Torus t, int iatom, int jatom,
                                         Edge e0, Edge e1,
                                         double a0, double a1){
	// form coordinate set.

        e0.torus = t;
        e1.torus = t;

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

        // this is important to choose same spacing for
        // both parts of torus
	if(t.rcji > effectiveArc){
	    effectiveArc = t.rcji;
	}

        // biggest of the two radii
        double rmax = Math.max(r[iatom], r[jatom]);

	double arcLength = (a1 - a0) * (effectiveArc);
	// always at least two points
	int tpcount = 2 + (int)(arcLength/(targetLen * rmax));
	int tpcount1 = tpcount - 1;
	double angle = a0;
	double wrapAngle = 0.0;
	double wrapAngleStep = 0.0;
	int nwap = 0;

        //print.f("tpcount " + tpcount);

	double step = (a1 - a0)/(tpcount1);

        if(t.selfIntersects){
            // all torus arcs end at the cusp point.
            double dtq = Math.sqrt(rp*rp - t.rij*t.rij);

	    for(int i = 0; i < 3; i++){
                ccji[i] = t.tij[i] - dtq * t.uij[i];
            }

            e0.selfIntersects = true;
            e1.selfIntersects = true;

            //print.f("seen self intersecting torus");
        }

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

                // probe position on its trajectory.
		pp[i] = t.tij[i] + t.rij * component;

                if(t.selfIntersects == false){
                    
                    ccji[i] = t.cji[i] + t.rcji * component;
                    //ccji[i] = t.tij[i] + (t.rij - rp) * component;
                    
                }
		ccij[i] = t.cij[i] + t.rcij * component;
	    }

	    // calculate the wrap angle
	    // and the offsets for this torus segment.
	    vector(pp2cij, pp, ccij);
	    vector(pp2cji, pp, ccji);
	    wrapAngle = angle(pp2cij, pp2cji);

            // must have the angle as only one half
            // of torus is generated for this atom
            if(t.selfIntersects == false){
                wrapAngle *= 0.5;
            }

            //print.f("wrapAngle " + wrapAngle);

	    cross(n1, pp2cij, pp2cji);
	    cross(n2, n1, pp2cij);
	    normalise(n2);
	    normalise(pp2cij);
	    double wrapArcLength = wrapAngle * rp;

	    // always at least two points
            // use twice the target length as we only have half
            // the torus
	    nwap = 2 + (int)(wrapArcLength/(targetLen * rmax));
	    int nwap1 = nwap - 1;

	    wrapAngleStep = wrapAngle/(nwap1);

	    if(a == 0){
                if(e0.size() != 0){
                    print.f("e0.size() " + e0.size());
                }
                if(e1.size() != 0){
                    print.f("e1.size() " + e1.size());
                }
		e0.setCapacity(nwap);
		e1.setCapacity(nwap);
	    }

	    double wa = 0.0;

            //print.f("a " + a);

	    // now interpolate from one end
	    // of the arc to the other.
	    for(int ii = 0; ii < nwap; ii++){
		double sinwa = Math.sin(wa);
		double coswa = Math.cos(wa);

		// tidy up any slight rounding error
		if(wa > wrapAngle){
		    wa = wrapAngle;
		}
		
		// relative vector
		for(int i = 0; i < 3; i++){
		    ntp[i] = coswa * pp2cij[i] + sinwa * n2[i];
		    tp[i] = rp * ntp[i] + pp[i];
		    ntp[i] = -ntp[i];
		}

		// record the vertex index
		int vid = 0;

		// need to get the tmesh vertices for the
		// corner points from the vertex objects
		// to ensure mesh sharing
		if(a == 0 && ii == 0){
		    vid = e0.v0.vi;
                    //print.f("lookup e0");
		//}else if(a == 0 && ii == nwap1){
		//    vid = e0.v1.vi;
		//}else if(a == tpcount1 && ii == nwap1){
		//    vid = e1.v0.vi;
		}else if(a == tpcount1 && ii == 0){
		    vid = e1.v1.vi;
                    //print.f("lookup e1");
		}else{
		    // not corner
		    // need a new point
		    vid = addPoint(tp, ntp, 1);
                }

                // put an edge in the data structure.
                if(ii == 0){
                    if(a > 0){
                        addEdge(torusEdgesHash,
                                vid, torusEdgesPoints.getReverse(0), 1);
                    }
                    if(a < tpcount1){
                        torusEdgesPoints.add(vid);
                    }
                }

                //print.f("vid " + vid);

		tmeshv[a][ii] = vid;

		// assign the vertices to edge structures
		//
		// XXX need to retrieve the vertex indexes
		// for the corner vertices of the toriodal patch
		//!!
		// the ordering of these edges is crucial
		// do not change unless you know better than me
		if(a == 0)        e0.set(ii, vid);
		if(a == tpcount1) e1.set(nwap1-ii, vid);

		wa += wrapAngleStep;
	    }

            if(a > 0){
                for(int ii = 0; ii < nwap1; ii++){
                    addTriangle(atomSurface,
                                tmeshv[a-1][ii],
                                tmeshv[a][ii],
                                tmeshv[a-1][ii+1], Color32.white);
                    addTriangle(atomSurface,
                                tmeshv[a][ii],
                                tmeshv[a-1][ii+1],
                                tmeshv[a][ii+1], Color32.white);
                }
            }

	    angle += step;
	}
    }

    private static void setCentralAtom(int i){
        if(probes != null){
            // clear out the edge points for this atoms probes
            int probeCount = probes.size();
            for(int p = 0; p < probeCount; p++){
                Probe probe = (Probe)probes.get(p);
                probe.edge0.removeAllElements();
                probe.edge1.removeAllElements();
                probe.edge2.removeAllElements();
            }
            
            probes.removeAllElements();
        }

        buildNeighbourList(i);

        setSphereTemplate(atoms[i], r[i], rext[i]);

        // empty out any existing previous atom surface
        atomSurface.np = 0;
        atomSurface.nt = 0;

        torusEdgesPoints.removeAllElements();

        torusEdgesHash.clear();

        flags.removeAllElements();
    }

    private static void setNeighbourAtom(int j){
        torusProbes.removeAllElements();
    }

    /** Temporary for calculating normals. */
    static double nnv[] = new double[3];

    public static double pp0[] = new double[3];
    public static double pp1[] = new double[3];
    public static double pp2[] = new double[3];
    public static double pp3[] = new double[3];

    public static double npp0[] = new double[3];
    public static double npp1[] = new double[3];
    public static double npp2[] = new double[3];
    public static double npp3[] = new double[3];

    public static double circum[] = new double[3];

    private static int stitchIteration = 0;

    private static void stitch(Hashtable edges, IntArray points){

        int pointCount = points.size();

        for(stitchIteration = 0; stitchIteration < 2; stitchIteration++){
            
            boolean added = false;
            
            double rcut = targetLen;
            
            do {
                added = false;
                
                double rcut2 = rcut * rcut;
                
                do {
                    Enumeration edgesEnumeration = edges.keys();
                    
                    boolean printit = false;
                    
                    added = false;
                    
                    mainloop:
                    while(edgesEnumeration.hasMoreElements()){
                        Integer e     = (Integer)edgesEnumeration.nextElement();
                        Integer count = (Integer)edges.get(e);
                        
                        if(count.intValue() < 2){
                            // once only, both points on hull
                            // of triangles
                            int evalue = e.intValue();
                            int i = (evalue >> 15) & 0x7fff;
                            int j = (evalue) & 0x7fff;
                            
                            // this should never happen but is doing
                            if(i == j){
                                print.f("i == j = " + i);
                                continue;
                            }
                            
                            atomSurface.getVertex(i, pp0, npp0);
                            atomSurface.getVertex(j, pp1, npp1);
                            
                            for(int sk = 0; sk < pointCount; sk++){
                                int k = points.get(sk);
                                if(k != i && k != j){
                                    if(getEdgeCount(edges, j, k) >= 2 ||
                                       getEdgeCount(edges, i, k) >= 2) continue;
                                    
                                    atomSurface.getVertex(k, pp2, npp2);
                                    
                                    if(distance2(pp0, pp2) > rcut2) continue;
                                    if(distance2(pp1, pp2) > rcut2) continue;
                                    
                                    if(compatible(i, j, k) &&
                                       geometryAcceptable(pp0, npp0, pp1, npp1, pp2, npp2) &&
                                       delauneyAcceptable(pp0, i, pp1, j, pp2, k, points) &&
                                       newTriangle(i, j, k)){
                                        
                                        //print.f("k acceptable " + k);

                                        if(getEdgeCount(edges, i, j) < 2 &&
                                           getEdgeCount(edges, j, k) < 2 &&
                                           getEdgeCount(edges, k, i) < 2){
                                            addTriangle(atomSurface, i, j, k, Color32.white);
                                        
                                            if(!addEdge(edges, i, k, -1)){
                                                print.f("couldn't add edge ik " + i + " " + k);
                                            }
                                            if(!addEdge(edges, j, k, -1)){
                                                print.f("couldn't add edge jk " + j + " " + k);
                                            }
                                            if(!addEdge(edges, i, j, -1)){
                                                print.f("couldn't add edge jk " + j + " " + k);
                                            }
                                        
                                        //print.f("added " + i + " " + j + " " + k);
                                            
                                            added = true;
                                            break mainloop;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } while(added);
                
                rcut *= 2.0;
                
            } while(rcut < 5. * targetLen);
        }
    }

    private static boolean newTriangle(int i, int j, int k){
        int triCount = atomSurface.nt;
        int temp = 0;
        boolean printit = false;

        if(i == 61){
            printit = false;
            //print.f(" ijk  "+ i + " " + j + " " + k);
        }

        if (i > j) { temp = i; i = j; j = temp; }
        if (j > k) { temp = j; j = k; k = temp; }
        if (i > j) { temp = i; i = j; j = temp; }

        for(int p = 0; p < triCount; p++){
            int mi = atomSurface.t0[p];
            int mj = atomSurface.t1[p];
            int mk = atomSurface.t2[p];
            if (mi > mj) { temp = mi; mi = mj; mj = temp; }
            if (mj > mk) { temp = mj; mj = mk; mk = temp; }
            if (mi > mj) { temp = mi; mi = mj; mj = temp; }

            if(printit){
                print.f("mijk  "+ mi + " " + mj + " " + mk);
            }

            if(i == mi && j == mj && k == mk){
                if(printit){
                    print.f("match");
                }
                return false;
            }

        }

        return true;
    }

    private static int addPoint(double p[], double n[], int flag){
        return addPoint(p[0], p[1], p[2], n[0], n[1], n[2], flag);
    }

    private static IntArray flags = new IntArray();

    private static int addPoint(double px, double py, double pz,
                                double nx, double ny, double nz,
                                int flag){
        flags.add(flag);

        return atomSurface.addPoint(px, py, pz, nx, ny, nz, 0.0, 0.0);
    }

    /** Are the edge flags ok for this triangle. */
    public static boolean compatible(int i, int j, int k){
        boolean all = false;

        if(flags.get(i) == 1 && flags.get(j) == 1 && flags.get(k) == 1){
            all = true;
        }
        
        if(stitchIteration == 0 && all == true){
            return false;
        }

        return true;
    }

    private static double n[]             = new double[3];
    private static double nsum[]          = new double[3];

    private static final double cosCutoff = Math.cos(Math.PI/6.0);

    /**
     * Determine if the triangle bridges some unsuitable points
     * by comparing the triangle normal with the normals for the
     * triangle vertices.
     */
    public static boolean geometryAcceptable(double pp0[], double npp0[], 
                                             double pp1[], double npp1[], 
                                             double pp2[], double npp2[]){
        // generate normal to plane of triangle
        normal(n, pp0, pp1, pp2);
        normalise(n);

        // generate average normal from vertex normals
        for(int i = 0; i < 3; i++){
            nsum[i] = npp0[i] + npp1[i] + npp2[i];
        }

        normalise(nsum);

        // are we within acceptable angle
        if(Math.abs(dot(n, nsum)) < cosCutoff){
            return false;
        }

        /*
          double rcut2 = 4.0 * targetLen;
          rcut2 *= rcut2;

          if(distance2(pp0, pp1) > rcut2) return false;
          if(distance2(pp1, pp2) > rcut2) return false;
          if(distance2(pp2, pp0) > rcut2) return false;
        */

        return true;
    }

    /**
     * Determine if the given points can add a triangle
     * to the current mesh without violating the delauney
     * triangulation criteria for the other points.
     */
    public static boolean delauneyAcceptable(double pp0[], int i,
                                             double pp1[], int j,
                                             double pp2[], int k,
                                             IntArray delauney){
        double rc = circumCircle(circum, pp0, pp1, pp2);
                    
        if(rc == Double.POSITIVE_INFINITY){
            //System.out.println("## no solution for circumCircle");
            return false;
        }
                    
        int delauneyCount = delauney.size();

        boolean tok = true;
                    
        rc *= rc;
                    
        //for(int l = 0; l < atomSurface.np; l++){
        for(int lp = 0; lp < delauneyCount; lp++){
            int l = delauney.get(lp);
            if(l != i && l != j && l != k){
                atomSurface.getVertex(l, pp3, null);
                
                if(distance2(circum, pp3) < rc){
                    tok = false;
                    break;
                }
            }
        }

        return tok;
    }



    /**
     * Yes this really is how to calculate the circum center
     * and radius of three points in 3d.
     *
     * Adapted from Graphics Gems.
     */
    public static double circumCircle(double cc[],
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

    /** Add a vertex for the surface. */
    private static Vertex addVertex(double vx[], int i, double px[]){
	Vertex v = new Vertex();

	//v.p = p;

	vector(nnv, vx, px);
	normalise(nnv);

	v.i = i;

        v.vi = -1;

        if(false){
            for(int p = 0; p < atomSurface.np; p++){
                double dx = atomSurface.x[p] - vx[0];
                double dy = atomSurface.y[p] - vx[1];
                double dz = atomSurface.z[p] - vx[2];
                double d2 = dx*dx + dy*dy + dz*dz;
                if(d2 < 1.e-4){
                    v.vi = p;
                    //print.f("found vertex " + p);
                    break;
                }
            }

            if(v.vi == -1){
                v.vi = addPoint(vx, nnv, 0);
                //print.f("adding vertex " + v.vi + " for atom " + i);
            }
        }


	// record the coords here for convenience.
	copy(vx, v.x);

	return v;
    }

    static double pdir[] = new double[3];

    /** Process the probe placement. */
    private static Probe processPlacement(double pijk[], int i, int j, int k){
	Probe p = addProbePlacement(pijk, bijk, i, j, k);

	// add the vertices
	constructVertex(api, pijk, atoms[i], r[i]);
	Vertex v0 = addVertex(api, i, p.x);
	constructVertex(apj, pijk, atoms[j], r[j]);
	Vertex v1 = addVertex(apj, j, p.x);
	constructVertex(apk, pijk, atoms[k], r[k]);
	Vertex v2 = addVertex(apk, k, p.x);

	// get direction of probe placement
	vector(pdir, bijk, pijk);

	Edge edge0 = null, edge1 = null, edge2 = null;

	// assign edges depending on orientation
	if(dot(pdir, uijk) > 0.0){
	    p.edge0 = constructProbeEdge(v0, api, v1, apj, apk, pijk, rp);
	    p.edge1 = constructProbeEdge(v1, apj, v2, apk, api, pijk, rp);
	    p.edge2 = constructProbeEdge(v2, apk, v0, api, apj, pijk, rp);
	}else{
	    p.edge0 = constructProbeEdge(v0, api, v2, apk, apj, pijk, rp);
	    p.edge1 = constructProbeEdge(v2, apk, v1, apj, api, pijk, rp);
	    p.edge2 = constructProbeEdge(v1, apj, v0, api, apk, pijk, rp);
	}

        return p;
    }

    /** Add a probe placement for the surface. */
    private static Probe addProbePlacement(double pijk[], double bijk[], int i, int j, int k){
	Probe p = new Probe();

	copy(pijk, p.x);
	copy(bijk, p.bijk);

	p.i = i; p.j = j; p.k = k;

	p.r = rp;

	return p;
    }

    // working space
    static double edgen[] = new double[3];
    static double otherv[] = new double[3];

    public static Edge constructProbeEdge(Vertex v0, double p0[],
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
	
	//torus.edges.add(edge);

	return edge;
    }

    private static boolean obscured(double p[], int i, int j, int k){
        for(int n = 0; n < neighbourCount; n++){
            int in = neighbours[n];
            if(in != i && in != j && in != k){
                double rr = rext[in];
                if(distance2(p, atoms[in]) < rr * rr){
                    return true;
                }
            }
        }

        return false;
    }

    /** Improve later. */
    private static void buildNeighbourList(int i){
        double ai[] = atoms[i];
        double ri = rext[i];

        neighbourCount = 0;

        for(int j = 0; j < atomCount; j++){
            if(j != i){
                double aj[] = atoms[j];
                double rtot = rext[j] + ri;
                if(distance2(ai, aj) < rtot * rtot){
                    neighbours[neighbourCount++] = j;
                }
            }
        }

        //print.f("atom %5d ", i);
        //print.f("has %4d neighbours\n", neighbourCount);
    }

    /* Vectors for the atom positions. */
    private static double uij[] =  new double[3];
    private static double uik[] =  new double[3];
    private static double tij[] =  new double[3];
    private static double tji[] =  new double[3];
    private static double tik[] =  new double[3];
    private static double uijk[] = new double[3];
    private static double utb[] =  new double[3];
    private static double bijk[] = new double[3];
    private static double pijk[] = new double[3];

    private static double cij[] =  new double[3];
    private static double cji[] =  new double[3];

    private static double api[] =  new double[3];
    private static double apj[] =  new double[3];
    private static double apk[] =  new double[3];

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
	probePosition(p0, bijk,  hijk, uijk);

	probePosition(p1, bijk, -hijk, uijk);

	return true;
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
    public static double torusCenter(double ttij[],
				     double ai[], double ri,
				     double aj[], double rj,
				     double rprobe){
	double rip = ri + rprobe;
	double rjp = rj + rprobe;
	double dij2 = distance2(ai, aj);
	double dij = Math.sqrt(dij2);
	double rconst = ((rip*rip) - (rjp*rjp))/dij2;

	for(int i = 0; i < 3; i++){
	    ttij[i] = 0.5 * (ai[i] + aj[i]) + 0.5 * (aj[i] - ai[i]) * rconst;
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
    public static void constructVertex(double v[], double pijk[],
                                       double ai[], double r){
	double rip = r + rp;

	for(int i = 0; i < 3; i++){
	    v[i] = (r * pijk[i] + rp * ai[i])/rip;
	}
    }

    /** Calculate contact circle center and radius. */
    private static double contactCircle(double cij[],
                                        double ttij[],
                                        double ai[], double ri,
                                        double aj[], double rj){
	double rip = ri + rp;
	double rjp = rj + rp;

	double rij = torusCenter(ttij, ai, ri, aj, rj, rp);

	for(int ii = 0; ii < 3; ii++){
	    cij[ii] = (ri * ttij[ii] + rp * ai[ii])/rip;
	}

	return rij*ri/(rip);
    }

    /** Form cross product of two vectors (a = b x c). */
    public static double cross(double a[], double b[], double c[]){
	a[0] = (b[1] * c[2]) - (b[2] * c[1]);
	a[1] = (b[2] * c[0]) - (b[0] * c[2]);
	a[2] = (b[0] * c[1]) - (b[1] * c[0]);

	return a[0] + a[1] + a[2];
    }

    private static double ab[] = new double[3];
    private static double bc[] = new double[3];

    /** Form cross from three vectors (n = ab x bc). */
    public static double normal(double n[],
                                double a[], double b[], double c[]){
	vector(ab, a, b);
	vector(bc, b, c);
	n[0] = (ab[1] * bc[2]) - (ab[2] * bc[1]);
	n[1] = (ab[2] * bc[0]) - (ab[0] * bc[2]);
	n[2] = (ab[0] * bc[1]) - (ab[1] * bc[0]);

	return n[0] + n[1] + n[2];
    }

    /** Negate the vector. */
    public static void negate(double a[]){
	a[0]= -a[0];
	a[1]= -a[1];
	a[2]= -a[2];
    }

    /** Generate the plane equation. */
    public static double plane_eqn(double p[], double o[], double n[]){
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
    public static double dotnorm(double a[], double b[]){
	return (a[0]*b[0] + a[1]*b[1] + a[2]*b[2])/(length(a)*length(b));
    }

    /** Generate the dot product. */
    public static double dot(double ax, double ay, double az,
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
    public static void normal(double n[], double v[]){
	n[0] = 1.0; n[1] = 1.0; n[2] = 1.0;

        if(v[0] != 0.) n[0] = (v[2] + v[1]) / -v[0];
        else if(v[1] != 0.) n[1] = (v[0] + v[2]) / -v[1];
        else if(v[2] != 0.) n[2] = (v[0] + v[1]) / -v[2];

        normalise(n);
    }

    /** Normalise the vector. */
    public static void normalise(double p[]){
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
    public static void copy(double b[], double a[]){
	a[0] = b[0]; a[1] = b[1]; a[2] = b[2];
    }

    public static void mid(double m[], double a[], double b[]){
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
    public static double angle(double ref[], double n1[], double n2[]){
	double result = angle(ref, n1);

	if(dot(ref, n2) < 0.0){
	    result = -result;
	}

	return result;
    }

    /** Angle between two vectors. */
    public static double angle(double v1[], double v2[]){
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

    /** Calculate distance between two points. */
    public static double distance(double x1, double y1, double z1,
                                  double x2, double y2, double z2){
	double dx = x2 - x1;
	double dy = y2 - y1;
	double dz = z2 - z1;

	return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    /** Calculate squared distance between two points. */
    public static double distance2(double x1, double y1, double z1,
                                   double x2, double y2, double z2){
	double dx = x2 - x1;
	double dy = y2 - y1;
	double dz = z2 - z1;

	return dx*dx + dy*dy + dz*dz;
    }

    /** Are the two points within distance d of each other. */
    public static boolean within(double x1, double y1, double z1,
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

    /** Print a vector. */
    private static void print(String s, double x[]){
	Format.print(System.out, "%-10s", s);
	Format.print(System.out, " %8.3f,", x[0]);
	Format.print(System.out, " %8.3f,", x[1]);
	Format.print(System.out, " %8.3f\n", x[2]);
    }

    private static void setSphereTemplate(double x[], double ra, double raext){
        sphereEdgesHash.clear();

        for(int sp = 0; sp < nsp; sp++){
            clipped[sp] = 0;
            for(int j = 0; j < 3; j++){
                tsx[sp][j] = sx[sp][j] * ra + x[j];
                tsextx[sp][j] = sx[sp][j] * raext + x[j];
            }
        }
    }

    /** Maximum number of points in sphere template. */
    private static int MAX_SPHERE_POINTS = 642;

    /** Maximum number of triangles in sphere template. */
    private static int MAX_SPHERE_TRIANGLES = 2*2*MAX_SPHERE_POINTS - 4;

    /* Sphere template data structures. */
    private static double sx[][] = new double[MAX_SPHERE_POINTS][3];
    private static double snx[][] = new double[MAX_SPHERE_POINTS][3];

    /* The transformed sphere points. */
    private static double tsx[][] = new double[MAX_SPHERE_POINTS][3];
    private static double tsextx[][] = new double[MAX_SPHERE_POINTS][3];

    /* Is the sphere point clipped. */
    private static int clipped[] = new int[MAX_SPHERE_POINTS];

    /* Is the sphere point on the hull. */
    private static int hull[] = new int[MAX_SPHERE_POINTS];

    /** Number of points in the sphere template. */
    private static int nsp = 0;

    /* Sphere triangles. */
    private static int si[] = new int[MAX_SPHERE_TRIANGLES];
    private static int sj[] = new int[MAX_SPHERE_TRIANGLES];
    private static int sk[] = new int[MAX_SPHERE_TRIANGLES];
    
    /** Vertex neighbours. */
    private static int vn[][] = null;

    /** Vertex neighbour count. */
    private static int vncount[] = new int[MAX_SPHERE_POINTS];

    /** Triangles neighbours. */
    private static int tlist[][] = null;

    /** Vertex neighbour count. */
    private static int tcount[] = new int[MAX_SPHERE_POINTS];

    /** Number of triangles in the sphere template. */
    private static int nst = 0;

    /** Shortest edge length on sphere template. */
    private static double shortestEdge = 0.0;

    /** Longest edge length on sphere template. */
    private static double longestEdge = 0.0;

    /** Current longest edge. */
    private static double currentLongestEdge = 0.0;

    /** Build sphere template. */
    private static void buildSphereTemplate(int subDivisions){
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

        targetLen = longestEdge;

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

	print.f("points in sphere template " + nsp);
	print.f("triangles in sphere template " + nst);

	//outputSphereTemplate();
    }

    /** Initialise the sphere template. */
    private static void initialiseSphereTemplate(){
	sx[0][0]  = -0.851024; sx[0][1]  =         0; sx[0][2]  =  0.525126;
	sx[1][0]  =         0; sx[1][1]  =  0.525126; sx[1][2]  = -0.851024;
	sx[2][0]  =         0; sx[2][1]  =  0.525126; sx[2][2]  =  0.851024;
	sx[3][0]  =  0.851024; sx[3][1]  =         0; sx[3][2]  = -0.525126;
	sx[4][0]  = -0.525126; sx[4][1]  = -0.851024; sx[4][2]  =         0;
	sx[5][0]  = -0.525126; sx[5][1]  =  0.851024; sx[5][2]  =         0;
	sx[6][0]  =         0; sx[6][1]  = -0.525126; sx[6][2]  =  0.851024;
	sx[7][0]  =  0.525126; sx[7][1]  =  0.851024; sx[7][2]  =         0;
	sx[8][0]  =         0; sx[8][1]  = -0.525126; sx[8][2]  = -0.851024;
	sx[9][0]  =  0.851024; sx[9][1]  =         0; sx[9][2]  =  0.525126;
	sx[10][0] =  0.525126; sx[10][1] = -0.851024; sx[10][2] =         0;
	sx[11][0] = -0.851024; sx[11][1] =         0; sx[11][2] = -0.525126;
	nsp = 12;
	si[0]  =  9; sj[0]  =  2; sk[0]  =  6;
	si[1]  =  1; sj[1]  =  5; sk[1]  = 11;
	si[2]  = 11; sj[2]  =  1; sk[2]  =  8;
	si[3]  =  0; sj[3]  = 11; sk[3]  =  4;
	si[4]  =  3; sj[4]  =  7; sk[4]  =  1;
	si[5]  =  3; sj[5]  =  1; sk[5]  =  8;
	si[6]  =  9; sj[6]  =  3; sk[6]  =  7;
	si[7]  =  0; sj[7]  =  2; sk[7]  =  6;
	si[8]  =  4; sj[8]  =  6; sk[8]  = 10;
	si[9]  =  1; sj[9]  =  7; sk[9]  =  5;
	si[10] =  7; sj[10] =  2; sk[10] =  5;
	si[11] =  8; sj[11] = 10; sk[11] =  3;
	si[12] =  4; sj[12] = 11; sk[12] =  8;
	si[13] =  9; sj[13] =  2; sk[13] =  7;
	si[14] = 10; sj[14] =  6; sk[14] =  9;
	si[15] =  0; sj[15] = 11; sk[15] =  5;
	si[16] =  0; sj[16] =  2; sk[16] =  5;
	si[17] =  8; sj[17] = 10; sk[17] =  4;
	si[18] =  3; sj[18] =  9; sk[18] = 10;
	si[19] =  6; sj[19] =  4; sk[19] =  0;
	nst = 20;
    }

    /** Find the position of the current mid point. */
    private static int findSpherePoint(int i, int j){
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

    /** Add a triangle to the data structure. */
    private static void addTriangle(int ti, int tj, int tk){
	si[nst] = ti;
	sj[nst] = tj;
	sk[nst] = tk;
	nst++;
    }

    /** Does this sphere vertex have the other one as a neighbour. */
    private static boolean addNeighbour(int i, int v){
	for(int j = 0; j < vncount[i]; j++){
	    if(vn[i][j] == v){
		return true;
	    }
	}

	vn[i][vncount[i]++] = v;
	vn[v][vncount[v]++] = i;

	return false;
    }

    private static int getAtomProbeCount(int i){
        if(atomProbes[i] == null){
            return 0;
        }else{
            return atomProbes[i].size();
        }
    }

    private static void summarizeAtomProbes(){
        int zeroProbes = 0;
        int maxProbes  = 0;

        for(int i = 0; i < atomCount; i++){
            int pc = getAtomProbeCount(i);
            if(pc == 0){
                zeroProbes++;
            }else if(pc > maxProbes){
                maxProbes = pc;
            }
        }

        print.f("atom with zero probes %5d\n", zeroProbes);
        print.f("maximum probes        %5d\n", maxProbes);
    }
}
