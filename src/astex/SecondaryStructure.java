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
 * Class for assigning secondary structure to a protein molecule.
 */
public class SecondaryStructure {
    /** Cut off for NH...O distance. */
    private static double MaxHBondDistance = 5.5;

    private static Tmesh tm = null;

    /** Assign secondary structure to residues in a protein molecule. */
    public static Tmesh assign(DynamicArray molecules){
	int moleculeCount = molecules.size();

	tm = new Tmesh();

	for(int m = 0; m < moleculeCount; m++){
	    Molecule mol = (Molecule)molecules.get(m);
	    assignMolecule(mol);
	}

	return tm;
    }

    // static workspace for secondary structure assignment
    private static final int MaxResidues = 10000;

    private static Residue residues[]  = new Residue[MaxResidues];
    private static Point3d hpos[]      = new Point3d[MaxResidues];
    private static Atom opos[]         = new Atom[MaxResidues];
    private static int types[]         = new int[MaxResidues];
    private static int mapping[]       = new int[MaxResidues];
    private static IntArray hbond_no[] = new IntArray[MaxResidues];
    private static IntArray hbond_on[] = new IntArray[MaxResidues];

    // number of residues including gaps in molecule.
    private static int nres = 0;

    public static boolean debug = false;

    /** Assign secondary structure for this molecule. */
    public static void assignMolecule(Molecule mol){
	Arguments args        = new Arguments();
	double hbondConstant  = args.getDouble("hbond.constant", -999.0);
	double hbondCutoff    = args.getDouble("hbond.cutoff", -999.0);

	//System.out.println("hbondCutoff " + hbondCutoff);

	nres = 0;
	int realRes = 0;

	int chainCount = mol.getChainCount();

	for(int c = 0; c < chainCount; c++){
	    Chain chain = mol.getChain(c);
	    int residueCount = chain.getResidueCount();
	    for(int r = 0; r < residueCount; r++){
		Residue res = (Residue)chain.getResidue(r);

		//assignInitialType(res);
		res.setSecondaryStructure(Residue.Coil);

		// need to assign gaps here to 
		// stop helix hbonds being fooled by gaps

		residues[nres] = res;
		types[nres] = res.getSecondaryStructure();
		hbond_no[nres] = new IntArray();
		hbond_on[nres] = new IntArray();
		hpos[nres] = null;
		opos[nres] = null;
		mapping[realRes] = nres;
		realRes++;
		nres++;
	    }

	    // add 4 residue gaps to stop helix
	    // h-bond spanning different chains.
	    for(int i = 0; i < 4; i++){
		residues[nres] = null;
		types[nres] = Residue.Undefined;
		hbond_no[nres] = new IntArray();
		hbond_on[nres] = new IntArray();
		hpos[nres] = null;
		opos[nres] = null;
		nres++;
	    }
	}

	// generate amide hydrogen positions.
	// gather amide oxygen positions
	for(int r1 = 0; r1 < nres; r1++){
	    if(residues[r1] != null){
		hpos[r1] = getAmideHydrogen(residues[r1]);
		opos[r1] = residues[r1].getAtom("O");
	    }
	}

	//Util.startTimer(0);

	IntArray neighbours = new IntArray();

	Lattice ol = new Lattice(MaxHBondDistance * 1.05);

	for(int r2 = 0; r2 < nres; r2++){
	    if(opos[r2] != null){
		Point3d o = opos[r2];
		ol.add(r2, o.x, o.y, o.z);
	    }
	}

	//System.out.println("lattice created");

	// assign mainchain hydrogen bonds.
	for(int r1 = 0; r1 < nres; r1++){
	    // NH...O
	    Point3d h = hpos[r1];
	    if(h != null){
		Atom n = residues[r1].getAtom("N");

		neighbours.removeAllElements();

		ol.getPossibleNeighbours(r1, n.x, n.y, n.z,
					 neighbours, true);

		int neighbourCount = neighbours.size();

		for(int i = 0; i < neighbourCount; i++){
		    int oid = neighbours.get(i);
		    Atom o = opos[oid];
		    
		    if(o != null){
			Atom c = o.getBondedAtom("C");

			double e = MoleculeRenderer.hbondEnergy(n, h, o, c, hbondConstant);

			if(e < hbondCutoff){
			    
			    hbond_no[r1].add(oid);
			    hbond_on[oid].add(r1);
			    
			    if(debug){
				int rid1 = residues[r1].getNumber();
				int rid2 = residues[oid].getNumber();
				System.out.println("adding NH..O " +
						   residues[r1] + " to " +
						   residues[oid] + " d=" + o.distance(h));
				
				//tm.addCylinder(h.x, h.y, h.z,
				//	       opos[oid].x, opos[oid].y, opos[oid].z,
				//	       0.1, Color32.white, Color32.white);
			    }
			}
		    }else{
			Log.error("shouldn't be a null reference in o lattice");
		    }
		}
	    }
	}

	//Util.stopTimer("hydrogen bond calculation %5dms\n", 0);

	//System.out.println("about to do helix");

	// put turns in...
	for(int r1 = 3; r1 < nres; r1++){
	    if(hbonded(r1, r1 - 3)){
		//System.out.println("seen turn");
		//for(int r2 = r1 - 3; r2 <= r1; r2++){
		for(int r2 = r1 - 2; r2 < r1; r2++){
		    //System.out.println("setting " + r2);
		    types[r2] = Residue.Turn;
		}
	    }
	}

	// now look for helices
	for(int r1 = 1; r1 < nres-4; r1++){
	    if(hbonded(r1 + 3, r1 - 1) &&
	       hbonded(r1 + 4, r1)){
		for(int r2 = r1; r2 <= r1 + 3; r2++){
		    types[r2] = Residue.Helix;
		}
	    }
	    if(hbonded(r1 + 2, r1 - 1) &&
	       hbonded(r1 + 3, r1)){
		for(int r2 = r1; r2 <= r1 + 2; r2++){
		    types[r2] = Residue.Helix;
		}
	    }
	}

	// assign beta-sheet secondary structure
	// according to basic rules in
	// Kabsch and Sander, Biopolymers, 22, 2577-2637 (1983).
	// anti-parallel
	for(int ri = 0; ri < nres; ri++){
	    if(types[ri] == Residue.Coil || types[ri] == Residue.Sheet){
		int hbondCount = hbond_no[ri].size();
		if(debug && hbondCount > 0){
		    System.out.println("checking residue " + residues[ri] + " " +
				       hbondCount + " hbonds");
		}
		for(int hb = 0; hb < hbondCount; hb++){
		    int rj = hbond_no[ri].get(hb);
		    if(debug){
			System.out.println("hydrogen bonded to " + residues[rj]);
		    }
		    if(ri < rj && rj >= 0 && rj < nres){
			if(debug){
			    System.out.println("## ri < rj rj valid");
			    //if(types[rj] == Residue.Coil || types[rj] == Residue.Sheet){
			    System.out.println("### type is coilri < rj rj valid");
			}
			if((hbonded(ri, rj)     && hbonded(rj,ri))){
			    //(hbonded(ri-1, rj+1) && hbonded(rj-1,ri+1))){
			    // anti-parallel
			    if(debug){
				System.out.println("### anti-parallel");
			    }

			    assignSheetType(ri);
			    assignSheetType(rj);
			    assignSheetType(ri-1);
			    assignSheetType(rj+1);
				
			    //types[ri] = types[rj] = Residue.Sheet;
			    //types[ri-1] = types[rj+1] = Residue.Sheet;
			    if(Math.abs(ri - rj) >= 5){
				assignSheetType(ri+1);
				assignSheetType(rj-1);
				//types[ri+1] = types[rj-1] = Residue.Sheet;
			    }
			}
			//}
		    }
		}
	    }
	}

	for(int ri = 0; ri < nres; ri++){
	    if(types[ri] == Residue.Coil || types[ri] == Residue.Sheet){
		int hbondCount = hbond_no[ri].size();
		//if(hbondCount > 0){
		//    System.out.println("checking residue " + (ri+1) + " " +
		//		       hbondCount + " hbonds");
		//}
		for(int hb = 0; hb < hbondCount; hb++){
		    int rrj = hbond_no[ri].get(hb);
		    //System.out.println("hydrogen bonded to " + (rrj+1));
		    for(int rj = rrj - 1; rj < rrj + 2; rj++){
			if(rj >= 0 && rj < nres){
			    //if(types[rj] == Residue.Coil){
				//System.out.println("target not helix");
				//if((hbonded(ri-1, rj) && hbonded(rj,ri+1)) ||
				//   (hbonded(rj-1, ri) && hbonded(ri,rj+1))){
				if((hbonded(ri, rj-1) && hbonded(rj+1,ri)) ||
				   (hbonded(rj-1, ri) && hbonded(ri,rj+1))){
				    // parallel
				    //types[ri] = types[rj] = Residue.Sheet;
				    assignSheetType(ri);
				    assignSheetType(rj);
				    if(Math.abs(ri - rj) >= 5){
					assignSheetType(rj+1);
					//types[rj+1] = Residue.Sheet;
				    }
				    assignSheetType(rj-1);
				    //types[rj-1] = Residue.Sheet;
				    //System.out.println("parallel bridge");
				}
				//}
			}
		    }
		}
	    }
	}

	// pick up some bulge structures
	if(false){
	for(int ri = 0; ri < nres; ri++){
	    if(types[ri] != Residue.Helix){
		for(int rj = 0; rj < nres - 1; rj++){
		    if(types[ri] != Residue.Helix){
			if(Math.abs(rj - ri) > 3 &&
			   hbond_on[ri].contains(rj) &&
			   hbond_on[ri].contains(rj+1)){
			    System.out.println("bulge ri " + ri + " res " + residues[ri]);
			    System.out.println("bulge rj " + rj + " res " + residues[rj]);
			    types[ri] = types[rj] = types[rj+1] = Residue.Sheet;
			}
		    }
		}
	    }
	}
	}

	// regularise the assignments
	regulariseSS(types, nres);

	nres = 0;
	    
	// gather the initial assignments.
	for(int c = 0; c < chainCount; c++){
	    Chain chain = mol.getChain(c);
	    int residueCount = chain.getResidueCount();

	    // copy them back
	    for(int r = 0; r < residueCount; r++){
		Residue res = (Residue)chain.getResidue(r);
		//System.out.println(types[mapping[nres]] + " "+ res);
		res.setSecondaryStructure(types[mapping[nres++]]);
	    }
	}
    }

    private static void assignSheetType(int r){
	if(r >= 0 && r < nres &&
	   (types[r] == Residue.Coil || types[r] == Residue.Sheet)){
	    types[r] = Residue.Sheet;
	}
    }

    /** Is there a mainchain h-bond from O to N of the two residues. */
    private static boolean hbonded(int ri, int rj){
	if(ri < 0 || ri >= nres || rj < 0 || rj >= nres){
	    return false;
	}

	//if(hbond_on[ri].contains(rj) && hbond_no[rj].contains(ri)){
	if(hbond_no[ri].contains(rj)){
	    return true;
	}
	
	return false;
    }

    /** Is there an hbond to rtarget in the list. */
    private static boolean hasHBond(IntArray hbond, int rtarget){
	if(hbond == null) return false;

	return hbond.contains(rtarget);
    }

    private static Point3d getAmideHydrogen(Residue r){
	if(r == null){
	    return null;
	}

	Atom N = r.getAtom("N");

	if(N == null){
	    return null;
	}

	Atom CA = N.getBondedAtom("CA");
	Atom C = N.getBondedAtom("C");

	if(N == null || CA == null || C == null){
	    return null;
	}

	Point3d hpos = new Point3d();
	hpos.set(N);
	hpos.subtract(C);
	hpos.add(N);
	hpos.subtract(CA);

	hpos.normalise();
	hpos.scale(1.04);

	hpos.add(N);

	return hpos;
    }

    private static void regulariseSS(int types[], int n){
	// single residue gaps in sheets/helix
	for(int r = 1; r < n - 1; r++){
	    if(types[r] == Residue.Coil &&
	       ((types[r-1] == Residue.Sheet &&
		 types[r+1] == Residue.Sheet) ||
		((types[r-1] == Residue.Helix &&
		  types[r+1] == Residue.Helix)))){

		if(debug){
		    System.out.println("changing " + residues[r]);
		}
		types[r] = types[r-1];
	    } else if(types[r] == Residue.Helix &&
	       ((types[r-1] == Residue.Sheet &&
		 types[r+1] == Residue.Sheet))){
		if(debug){
		    System.out.println("changing " + residues[r]);
		}
		types[r] = types[r-1];
	    } else if(types[r] == Residue.Sheet &&
	       ((types[r-1] != Residue.Sheet &&
		 types[r+1] != Residue.Sheet))){
		if(debug){
		    System.out.println("changing " + residues[r]);
		}
		types[r] = types[r-1];
	    } else if(types[r] != Residue.Coil &&
	       ((types[r-1] == Residue.Coil &&
		 types[r+1] == Residue.Coil))){
		if(debug){
		    System.out.println("changing " + residues[r]);
		}
		types[r] = Residue.Coil;

	    } else if(types[r] == -1){
		types[r] = Residue.Coil;
	    }
	}
	

	// get rid of the two residue sheet.

	for(int r = 0; r < n - 2; r++){
	    if(r == 0 &&
	       types[r]   == Residue.Sheet &&
	       types[r+1] == Residue.Sheet &&
	       types[r+2] != Residue.Sheet){
		System.out.println("removing 2 residue strand at n-terminus");
		types[r] = types[r+1] = Residue.Coil;
	    }else if(r == n - 2 && 
	       types[r]   != Residue.Sheet &&
	       types[r+1] == Residue.Sheet &&
	       types[r+2] == Residue.Sheet){
		types[r+1] = types[r+2] = Residue.Coil;
		System.out.println("removing 2 residue strand at c-terminus");
	    }else if(
	       types[r]   != Residue.Sheet &&
	       types[r+1] == Residue.Sheet &&
	       types[r+2] == Residue.Sheet &&
	       types[r+3] != Residue.Sheet){
		System.out.println("removing 2 residue internal strand");
		types[r+1] = types[r+2] = Residue.Coil;
	    }
	}

	for(int r = 0; r < n; r++){
	    if(types[r] == Residue.Turn){
		types[r] = Residue.Coil;
	    }
	}
    }

    /** Assign initial type on basis of phi/psi. */
    public static void assignInitialType(Residue res){
	Atom Ni = res.getAtom("N");
	Atom CAi = res.getAtom("CA");
	Atom Ci = res.getAtom("C");
	Atom Cim1 = null;
	Atom Nip1 = null;

	if(Ni == null && CAi == null && Ci == null){
	    res.setSecondaryStructure(Residue.Undefined);
	}else{
	    if(Ni != null){
		Cim1 = Ni.getBondedAtom("C");
	    }

	    if(Ci != null){
		Nip1 = Ci.getBondedAtom("N");
	    }

	    if(Ni == null || CAi == null || Ci == null ||
	       Cim1 == null || Nip1 == null){
		res.setSecondaryStructure(Residue.Coil);
	    }else{

		double phi = Point3d.torsionDegrees(Cim1, Ni, CAi, Ci);
		double psi = Point3d.torsionDegrees(Ni, CAi, Ci, Nip1);

		//System.out.print(res);
		//System.out.println(" " + phi + " " + psi);

		int ssType = Residue.Coil;

		if(phi < -45 && phi > -160 &&
		   (psi < -170 || psi > 10)){
		    //ssType = Residue.Sheet;
		}else if(phi < -45 && phi > -160 &&
			 psi > -80 && psi < -25){
		    ssType = Residue.Helix;
		}

		res.setSecondaryStructure(ssType);
	    }

	    int ssType = res.getSecondaryStructure();
	    String label = null;
	    
	    if(ssType == Residue.Coil){
		label = "C";
	    }else if(ssType == Residue.Sheet){
		label = "S";
	    }else if(ssType == Residue.Helix){
		label = "H";
	    }else{
	    }

	    if(CAi != null){
		//System.out.println(res.getNumber() + " " + label);
	    }
	}
    }
}
