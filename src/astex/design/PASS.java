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

// for Probe class
import astex.anasurface.*;


public class PASS {
    /** Radius of the probe. */
    private static double Rprobe      = -1.0;

    /** Number of atoms within RBC for acceptance. */
    private static int    BCthreshold = -1;

    /** Radius for acceptance sphere. */
    private static double Rbc         = -1.0;

    /** Weeding out separation. */
    private static double Rweed       = -1.0;

    /** Accretion radius. */
    private static double Raccretion  = -1.0;

    /** R0. */
    private static double R0          = -1.0;

    /** D0. */
    private static double D0          = -1.0;

    /** The coordinates as an array. */
    private static DynamicArray probes = new DynamicArray();
    private static DynamicArray newProbes = new DynamicArray();

    /**
     * Generate a PASS description of a set of atoms.
     *
     * PASS (Putative Active Site with Spheres) is a
     * method for defining cavities on the surface of a
     * protein.
     *
     * "Fast Prediction and Visualization of
     * Protein Binding Pockets With PASS",
     * G. Patrick Brady, Jr. and Pieter F.W. Stouten,
     * JCAMD, 14: 383-401, 2000.
     */
    public static Molecule generatePASS(Arguments args,
					DynamicArray atoms){
	setup(args);

	Molecule mol = new Molecule();
	mol.setName(args.getString("-name", "PASS"));
	mol.setMoleculeType(Molecule.FeatureMolecule);

	generatePASSMolecule(mol, args, atoms);

	return mol;
    }

    /** Neighbour grid object. */
    private static Lattice l = null;

    /** Maximum radius of an atom. */
    private static double maxRad = 0.0;

    /** Actually generate the PASS atoms. */
    public static void generatePASSMolecule(Molecule mol,
					    Arguments args,
					    DynamicArray atoms){
	int atomCount = atoms.size();

	// find maximum radius
	maxRad = 0.0;

	for(int a = 0; a < atomCount; a++){
	    Atom atom = (Atom)atoms.get(a);

	    Probe p = new Probe();
	    p.x[0] = atom.x;
	    p.x[1] = atom.y;
	    p.x[2] = atom.z;
	    p.r = atom.getVDWRadius();

	    if(p.r > maxRad){
		maxRad = p.r;
	    }

	    probes.add(p);
	}

	// build the lattice
	double spacing = 2.0 * maxRad + 2.0 * Rprobe;

	// make sure we can use it for burial counts
	if(spacing < Rbc){
	    spacing = Rbc;
	}

	FILE.out.print("lattice spacing %.1f\n", spacing);

	l = new Lattice(spacing + 0.1);

	for(int a = 0; a < atomCount; a++){
	    Probe p = (Probe)probes.get(a);
	    l.add(a, p.x[0], p.x[1], p.x[2]);
	}

	// build the neighbour lists.
	buildNeighbourList(probes);

	// build the initial probe placements.
	constructProbePlacements(mol, probes);
    }

    /** Working space for probe placements. */
    private static double p0[] = new double[3];
    private static double p1[] = new double[3];

    /** Construct probe placements from triplets of atoms. */
    private static void constructProbePlacements(Molecule mol, DynamicArray probes){
	int n = probes.size();
	int tripletCount = 0;

	for(int i = 0; i < n; i++){
	    Probe pi = (Probe)probes.get(i);
	    for(int a = 0; a < count[i]; a++){
		int j = nn[first[i] + a];
		if(j > i){
		    Probe pj = (Probe)probes.get(j);
		    commonCount =
			AnaSurface.commonElements(nn, first[i], count[i],
						  nn, first[j], count[j],
						  commonNeighbours);

		    for(int b = 0; b < commonCount; b++){
			int k = commonNeighbours[b];
			
			if(k > j){
			    Probe pk = (Probe)probes.get(k);
			    tripletCount++;
			    
			    boolean retCode =
				AnaSurface.constructProbePlacement(pi.x, pi.r,
								   pj.x, pj.r,
								   pk.x, pk.r,
								   Rprobe,
								   p0, p1);

			    if(retCode){
				// placement was succesful.
				
				for(int p = 0; p < 2; p++){
				    double ppp[] = p0;
				    if(p == 1){
					ppp = p1;
				    }
				    
				    if(!obscured(ppp, Rprobe, i, j, k)){
					int bc = burialCount(ppp);
					if(bc >= BCthreshold){
                                            int np = newProbes.size();
                                            Probe weedProbe = null;

                                            for(int op = 0; op < np; op++){
                                                Probe old = (Probe)newProbes.get(op);
                                                if(AnaSurface.distance2(old.x, ppp) < 1.0){
                                                    if(weedProbe == null ||
                                                       old.bc < weedProbe.bc){
                                                        weedProbe = old;
                                                    }
                                                }
                                            }

                                            if(weedProbe == null){
                                                Probe probe = new Probe();
                                                probe.x[0] = ppp[0];
                                                probe.x[1] = ppp[1];
                                                probe.x[2] = ppp[2];
                                                probe.r = Rprobe;
                                                probe.bc = bc;
                                                newProbes.add(probe);
                                            }else if(bc > weedProbe.bc){
                                                weedProbe.x[0] = ppp[0];
                                                weedProbe.x[1] = ppp[1];
                                                weedProbe.x[2] = ppp[2];
                                                weedProbe.r = Rprobe;
                                                weedProbe.bc = bc;
                                            }
					}
				    }
				}
			    }
			}
		    }
		}
	    }
	}

	FILE.out.print("triplets %7d\n", tripletCount);

	int secondLayer = 0;

	do {
	    secondLayer = 0;
	    n = newProbes.size();

	    FILE.out.print("first layer %5d\n", n);

	    tripletCount = 0;

	    //Rprobe = Raccretion;

	    for(int i = 0; i < n; i++){
		Probe pi = (Probe)newProbes.get(i);
		for(int j = i+1; j < n; j++){
		    Probe pj = (Probe)newProbes.get(j);
		    double dsq = AnaSurface.distance2(pi.x, pj.x);
		    double r = pi.r + pj.r + 2.*Raccretion;
		    if(dsq < r*r){
			for(int k = j+1; k < n; k++){
			    Probe pk = (Probe)newProbes.get(k);
			    dsq = AnaSurface.distance2(pi.x, pk.x);
			    r = pi.r + pk.r + 2.*Raccretion;
			    if(dsq < r*r){
				dsq = AnaSurface.distance2(pj.x, pk.x);
				r = pj.r + pk.r + 2.*Raccretion;
				if(dsq < r*r){
				    tripletCount++;
				    
				    boolean retCode =
					AnaSurface.constructProbePlacement(pi.x, Raccretion,
									   pj.x, Raccretion,
									   pk.x, Raccretion,
									   Raccretion,
									   p0, p1);
                                    //AnaSurface.constructProbePlacement(pi.x, pi.r,
                                    //				   pj.x, pj.r,
                                    //				   pk.x, pk.r,
                                    //				   Raccretion,
                                    //				   p0, p1);

				    if(retCode){
					// placement was succesful.
					
					for(int p = 0; p < 2; p++){
					    double ppp[] = p0;
					    if(p == 1){
						ppp = p1;
					    }
					    
					    if(!clashed(ppp, Rprobe)){
						int bc = burialCount(ppp);
                                                if(bc >= BCthreshold){
                                                    boolean probeClashed = false;
                                                    for(int l = 0; l < n; l++){
                                                        Probe pl = (Probe)newProbes.get(l);
                                                        dsq = AnaSurface.distance2(pl.x, ppp);
                                                        //r = pl.r + Raccretion;
                                                        r = 2.0 * Raccretion;
                                                        if(dsq < r * r){
                                                            if(l != i && l != j && l != k){
                                                                probeClashed = true;
                                                                break;
                                                            }
                                                        }
                                                    }
						
                                                    if(probeClashed == false){
                                                        int np = newProbes.size();
                                                        Probe weedProbe = null;

                                                        for(int op = n; op < np; op++){
                                                            Probe old = (Probe)newProbes.get(op);
                                                            if(AnaSurface.distance2(old.x, ppp) < 1.0){
                                                                if(weedProbe == null ||
                                                                   old.bc < weedProbe.bc){
                                                                    weedProbe = old;
                                                                }
                                                            }
                                                        }

                                                        if(weedProbe == null){
                                                            Probe probe = new Probe();
                                                            probe.x[0] = ppp[0];
                                                            probe.x[1] = ppp[1];
                                                            probe.x[2] = ppp[2];
                                                            probe.r = Raccretion;
                                                            probe.bc = bc;
                                                            newProbes.add(probe);
                                                            secondLayer++;
                                                        }else if(bc > weedProbe.bc){
                                                            weedProbe.x[0] = ppp[0];
                                                            weedProbe.x[1] = ppp[1];
                                                            weedProbe.x[2] = ppp[2];
                                                            weedProbe.r = Raccretion;
                                                            weedProbe.bc = bc;
                                                        }
						    }
						}
					    }
					}
				    }
				}
			    }
			}
		    }
		}
	    }
	    FILE.out.print("second layer %5d\n", secondLayer);
	} while(secondLayer != 0);


	int probeCount = newProbes.size();

        int probeNeighbours[] = new int[probeCount];
        int probeNeighbours2[] = new int[probeCount];

        double rcut = 2.5 * 2.5;

        for(int iteration = 0; iteration < 2; iteration++){
            
            for(int i = 0; i < probeCount; i++){
                Probe pi = (Probe)newProbes.get(i);
                for(int j = i + 1; j < probeCount; j++){
                    Probe pj = (Probe)newProbes.get(j);
         
                    if(iteration == 0){
                        if(AnaSurface.distance2(pi.x, pj.x) < rcut){
                            probeNeighbours[i]++;
                            probeNeighbours[j]++;
                        }
                    }else{
                        if(probeNeighbours[j] >= 4 && probeNeighbours[i] >= 4 &&
                           AnaSurface.distance2(pi.x, pj.x) < rcut){
                            probeNeighbours2[i]++;
                            probeNeighbours2[j]++;
                        }
                    }
                }
            }
        }

	for(int i = 0; i < probeCount; i++){
            if(probeNeighbours2[i] >= 4){
                Probe probe = (Probe)newProbes.get(i);
                Atom atom = mol.addAtom();
                atom.x = probe.x[0];
                atom.y = probe.x[1];
                atom.z = probe.x[2];
                //atom.setVDWRadius(probe.r);
                atom.setVDWRadius(Rprobe);
                atom.setBFactor(burialCount(probe.x));
            }
	}
    }

    /**
     * Is p obscured by any of the neigbhours of i, j or k.
     * But not by i, j or k itself as these were used to
     * construct the point.
     */
    private static boolean obscured(double p[], double r, int i, int j, int k){

	// this order seems slightly more effective - k, i, j
	if(obscured2(p, r, k, i, j)){
	    return true;
	}
	if(obscured2(p, r, i, j, k)){
	    return true;
	}
	if(obscured2(p, r, j, i, k)){
	    return true;
	}

	return false;
    }

    /** Is p obscured by a neighbour of i, except for j or k. */
    private static boolean obscured2(double p[], double r, int i, int j, int k){

	int lastn = first[i] + count[i];

	for(int a = first[i]; a < lastn; a++){
	    int neighbour = nn[a];
	    Probe probe = (Probe)probes.get(neighbour);
	    double dx = p[0] - probe.x[0];
	    double dy = p[1] - probe.x[1];
	    double dz = p[2] - probe.x[2];

	    double rsq = probe.r + r;
	    rsq = rsq * rsq;

	    if(dx*dx+dy*dy+dz*dz < rsq){
	    //if(distance2(p, x[neighbour]) < rsq[neighbour]){
		// measurably faster to check after
		// satisfying the distance
		if(neighbour != j && neighbour != k){

		    return true;
		}
	    }
	}

	return false;
    }

    /** neighbours of the point for burial calculations. */
    private static IntArray burialNeighbours = new IntArray();

    /** 
     * Return the number of atoms within Rbc of the point.
     */
    private static int burialCount(double p[]){
	burialNeighbours.removeAllElements();

	l.getPossibleNeighbours(-1, p[0], p[1], p[2], burialNeighbours, true);

	int neighbourCount = burialNeighbours.size();

	double Rbc2 = Rbc * Rbc;

	int bc = 0;

	for(int i = 0; i < neighbourCount; i++){
	    int neighbour = burialNeighbours.get(i);
	    Probe probe = (Probe)probes.get(neighbour);
	    if(AnaSurface.distance2(p, probe.x) < Rbc2){
		bc++;
	    }
	}

	return bc;
    }

    /** 
     * Return the number of atoms within Rbc of the point.
     */
    private static boolean clashed(double p[], double rp){
	burialNeighbours.removeAllElements();

	l.getPossibleNeighbours(-1, p[0], p[1], p[2], burialNeighbours, true);

	int neighbourCount = burialNeighbours.size();

	for(int i = 0; i < neighbourCount; i++){
	    int neighbour = burialNeighbours.get(i);
	    Probe probe = (Probe)probes.get(neighbour);
	    double r = probe.r + rp;
	    if(AnaSurface.distance2(p, probe.x) < r*r){
		return true;
	    }
	}

	return false;
    }

    /* Sphere neighbours. */
    private static int first[] = null;
    private static int count[] = null;
    private static int nn[] = null;
    private static int neighbourCount = 0;

    private static int commonNeighbours[] = null;
    private static int commonCount = 0;
    private static int mergeNeighbours[] = null;
    private static int mergeCount = 0;

    /**
     * Build a list of each spheres neighbours.
     *
     * A neighbour is any sphere within ri + rj + 2 * rp
     */
    public static void buildNeighbourList(DynamicArray atoms){
	int n = atoms.size();

	first = new int[n];
	count = new int[n];
	// use IntArray to dynamically grow the 
	// neighbour list
	IntArray nList = new IntArray(n*60);

	int maxNeighbours = 0;

	// replace with more sophisticated algorithm later
	for(int i = 0; i < n; i++){
	    Probe pi = (Probe)probes.get(i);
	    double ri = pi.r + 2.0 * Rprobe;
	    first[i] = neighbourCount;
	    for(int j = 0; j < n; j++){
		Probe pj = (Probe)probes.get(j);
		double dij2 = AnaSurface.distance2(pi.x, pj.x);
		double rirj = ri + pj.r;
		if(dij2 < rirj*rirj){
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

	FILE.out.print("total neighbours   %7d\n", neighbourCount);
	FILE.out.print("maximum neighbours %7d\n", maxNeighbours);

	// allocate space for common neighbours.
	commonNeighbours = new int[maxNeighbours];

	// allocate space for merged neighbours.
	mergeNeighbours = new int[maxNeighbours * 3];
    }

    /* Vectors for the atom positions. */
    private double uij[] =  new double[3];
    private double uik[] =  new double[3];
    private double tij[] =  new double[3];
    private double tji[] =  new double[3];
    private double tik[] =  new double[3];
    private double uijk[] = new double[3];
    private double utb[] =  new double[3];
    private double bijk[] = new double[3];
    private double pijk[] = new double[3];

    private double cij[] =  new double[3];
    private double cji[] =  new double[3];

    private double api[] =  new double[3];
    private double apj[] =  new double[3];
    private double apk[] =  new double[3];

    /** Setup the parameters for the pass calculation. */
    private static void setup(Arguments args){
	Rprobe      = args.getDouble("-rprobe",
				     Settings.getDouble("config",
							"PASS.rprobe"));
	BCthreshold = args.getInteger("-bcthreshold",
				      Settings.getInteger("config",
							  "PASS.bcthreshold"));
	Rweed       = args.getDouble("-rweed",
				     Settings.getDouble("config",
							"PASS.rweed"));
	Rbc         = args.getDouble("-rbc",
				     Settings.getDouble("config",
							"PASS.rbc"));
	Raccretion  = args.getDouble("-raccretion",
				     Settings.getDouble("config",
							"PASS.raccretion"));
	R0          = args.getDouble("-r0",
				     Settings.getDouble("config",
							"PASS.r0"));
	D0          = args.getDouble("-d0",
				     Settings.getDouble("config",
							"PASS.d0"));

	FILE.out.print("PASS analysis\n");
	FILE.out.print("Rprobe      = %.2f\n", Rprobe);
	FILE.out.print("BCthreshold = %d\n",   BCthreshold);
	FILE.out.print("Rbc         = %.2f\n", Rbc);
	FILE.out.print("Rweed       = %.2f\n", Rweed);
	FILE.out.print("Raccretion  = %.2f\n", Raccretion);
	FILE.out.print("R0          = %.2f\n", R0);
	FILE.out.print("D0          = %.2f\n", D0);
    }
}
