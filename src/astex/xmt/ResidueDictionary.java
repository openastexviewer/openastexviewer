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

package astex.xmt;

import astex.*;
import java.util.*;

/**
 * Data structure for holding the geometric
 * dictionary information for a residue.
 *
 * These are usually aminoacids, nucleotides
 * or cofactors.
 *
 * They are based on those used by the program O.
 *
 * There are methods for generating dictionaries
 * for residues that have correctly defined element
 * types and bond orders.
 */
public class ResidueDictionary {
    /** Default constructor. */
    private ResidueDictionary(){
    }

    /** Constructor that takes the residue name. */
    public ResidueDictionary(String residueName){
	setName(residueName);
    }

    /** Name of the residue. */
    String name;
    
    /** Get the value of name. */
    public String getName() {
	return name;
    }
    
    /** Set the value of name. */
    public void setName(String  v) {
	this.name = v;
    }

    /** Bond length information. */
    public StringArray bond0         = new StringArray();
    public StringArray bond1         = new StringArray();
    public DoubleArray bondLength    = new DoubleArray();
    public DoubleArray bondSigma     = new DoubleArray();

    /** Bond angle information. */
    public StringArray angle0        = new StringArray();
    public StringArray angle1        = new StringArray();
    public StringArray angle2        = new StringArray();
    public DoubleArray angleValue    = new DoubleArray();
    public DoubleArray angleSigma    = new DoubleArray();

    /** Improper angle information. */
    public StringArray improper0     = new StringArray();
    public StringArray improper1     = new StringArray();
    public StringArray improper2     = new StringArray();
    public StringArray improper3     = new StringArray();
    public DoubleArray improperValue = new DoubleArray();
    public DoubleArray improperSigma = new DoubleArray();

    /** Torsion angle information. */
    public StringArray torsionNames  = new StringArray();

    /** DynamicArray of StringArrays that list the affected atoms. */
    public DynamicArray torsionAffectedAtoms = new DynamicArray();
    public DynamicArray torsionAtoms = new DynamicArray();

    /** Rotamer information. */
    public StringArray rotamerNames  = new StringArray();

    /** DynamicArray of StringArrays that contain the torsion names. */
    public DynamicArray rotamerTorsions = new DynamicArray();

    /** DynamicArray of DoubleArrays that contain the torsion values. */
    public DynamicArray rotamerValues = new DynamicArray();

    /**
     * Attempt to create a residue dictionary.
     */
    public static ResidueDictionary createDictionary(Residue res){
	int atomCount = res.getAtomCount();
	boolean isolated = true;
	ResidueDictionary rd = null;

	for(int a = 0; a < atomCount; a++){
	    Atom atom = res.getAtom(a);
	    int bondCount = atom.getBondCount();
	    for(int i = 0; i < bondCount; i++){
		Atom otherAtom = atom.getBondedAtom(i);
		Residue otherResidue = otherAtom.getResidue();

		if(otherResidue != res){
		    isolated = false;
		    break;
		}
	    }

	    if(isolated == false){
		break;
	    }
	}

	if(!isolated){
	    Log.info("residue has connections, not handled");
	    return rd;
	}

	rd = new ResidueDictionary(res.getName());

	Hashtable bonds = new Hashtable();

	for(int a = 0; a < atomCount; a++){
	    Atom atom = res.getAtom(a);
	    int bondCount = atom.getBondCount();
	    for(int i = 0; i < bondCount; i++){
		Bond bond = atom.getBond(i);

		Atom otherAtom = bond.getOtherAtom(atom);

		if(bonds.contains(bond) == false){

		    rd.bond0.add(atom.getAtomLabel());
		    rd.bond1.add(otherAtom.getAtomLabel());
		    rd.bondLength.add(atom.distance(otherAtom));
		    rd.bondSigma.add(0.02);
		    bonds.put(bond, bond);
		}

		for(int j = i + 1; j < bondCount; j++){
		    Atom otherAtom2 = atom.getBondedAtom(j);
		    rd.angle0.add(otherAtom.getAtomLabel());
		    rd.angle1.add(atom.getAtomLabel());
		    rd.angle2.add(otherAtom2.getAtomLabel());
		    rd.angleSigma.add(2.0);
		    rd.angleValue.add(Point3d.angleDegrees(otherAtom,
							   atom,
							   otherAtom2));
		}
	    }

	    if(bondCount > 2){
		addImpropers(atom, rd);
	    }
	}

	// add impropers to keep ring flat
	processRings(res, rd);
	
	return rd;
    }

    /** Process the rings for this residue. */
    public static void processRings(Residue res, ResidueDictionary rd){
	Atom firstAtom = res.getAtom(0);
	Molecule mol = firstAtom.getMolecule();
	int ringCount = mol.getRingCount();

	for(int r = 0; r < ringCount; r++){
	    Ring ring = mol.getRing(r);
	    int atomCount = ring.getAtomCount();
	    boolean ringInResidue = true;

	    for(int a = 0; a < atomCount; a++){
		Atom atom = ring.getAtom(a);
		Residue atomResidue = atom.getResidue();
		if(atomResidue != res){
		    ringInResidue = false;
		    break;
		}
	    }

	    if(ringInResidue){
		if(isPlanarRing(ring)){
		    addRingImpropers(ring, res, rd);
		}
	    }
	}
    }

    /** Add impropers for this ring. */
    private static void addRingImpropers(Ring ring, Residue res, 
					 ResidueDictionary rd){
	Log.debug("adding for ring " + ring);

	int atomCount = ring.getAtomCount();

	for(int a = 0; a < atomCount; a++){
	    atoms[0] = ring.getAtom(a);
	    atoms[1] = ring.getAtom((a+1)%atomCount);
	    atoms[2] = ring.getAtom((a+2)%atomCount);
	    atoms[3] = ring.getAtom((a+3)%atomCount);
	    Log.debug("atom %d " + atoms[0], a);

	    rd.improper0.add(atoms[0].getAtomLabel());
	    rd.improper1.add(atoms[1].getAtomLabel());
	    rd.improper2.add(atoms[2].getAtomLabel());
	    rd.improper3.add(atoms[3].getAtomLabel());
	    rd.improperValue.add(0.0);
	    rd.improperSigma.add(2.0);
	}
    }

    /** Working space for hybridisation. */
    private static int hybrid[] = new int[6];

    /** Is this residue planar (kind of aromatic?). */
    public static boolean isPlanarRing(Ring ring){
	int atomCount = ring.getAtomCount();

	if(atomCount == 3 || atomCount == 4){
	    return true;
	}else if(atomCount > 6){
	    return false;
	}

	for(int a = 0; a < atomCount; a++){
	    Atom a0 = ring.getAtom(a);
	    Atom a1 = ring.getAtom((a+1)%atomCount);
	    Atom a2 = ring.getAtom((a+2)%atomCount);

	    int h0 = a0.getHybridisation();
	    int h1 = a1.getHybridisation();
	    int h2 = a2.getHybridisation();

	    if(h0 == Atom.sp3 && h1 == Atom.sp3 && h2 == Atom.sp3){
		return false;
	    }

	    if(a0.getElement() == PeriodicTable.CARBON &&
	       a1.getElement() == PeriodicTable.CARBON){
		if(h0 == Atom.sp3 && h1 == Atom.sp3){
		    return false;
		}
	    }

	    if(atomCount == 6){
		if((a0.getElement() == PeriodicTable.CARBON &&
		    a1.getElement() == PeriodicTable.OXYGEN) ||
		   (a1.getElement() == PeriodicTable.CARBON &&
		    a0.getElement() == PeriodicTable.OXYGEN)){
		    if(h0 == Atom.sp3 && h1 == Atom.sp3){
			return false;
		    }
		}
	    }
	}

	return true;
    }

    /** Working space for storing atoms. */
    private static Atom atoms[] = new Atom[4];

    /** Add impropers for this atom. */
    public static void addImpropers(Atom atom, ResidueDictionary rd){
	int bondCount = atom.getBondCount();

	if(bondCount == 3){
	    // one improper with us as the central atom
	    atoms[0] = atom.getBondedAtom(0);
	    atoms[1] = atom;
	    atoms[2] = atom.getBondedAtom(1);
	    atoms[3] = atom.getBondedAtom(2);
	}else if(bondCount == 4){
	    for(int i = 0; i < 4; i++){
		atoms[i] = atom.getBondedAtom(i);
	    }
	}else{
	    Log.error("can't handle %d atoms", bondCount);

	    return;
	}

	double angle = Point3d.torsionDegrees(atoms[0], atoms[1],
					      atoms[2], atoms[3]);

	rd.improper0.add(atoms[0].getAtomLabel());
	rd.improper1.add(atoms[1].getAtomLabel());
	rd.improper2.add(atoms[2].getAtomLabel());
	rd.improper3.add(atoms[3].getAtomLabel());
	rd.improperValue.add(angle);
	rd.improperSigma.add(2.0);
    }

    /** Find the torsion number from the torsion name. */
    public int findTorsionNumber(String name){
	for(int i = 0; i < torsionNames.size(); i++){
	    String torsionName = (String)torsionNames.get(i);

	    if(name.equals(torsionName)){
		return i;
	    }
	}

	return -1;
    }

    /** Return the torsion number from the name. */
    public int getRotamerIndex(String name){
	for(int i = 0; i < rotamerNames.size(); i++){
	    if(name.equals(rotamerNames.get(i))){
		return i;
	    }
	}

	return -1;
    }

    /** Return the number of rotamer states for this residue. */
    public int getRotamerCount(){
	return rotamerNames.size();
    }

    /** Get rotamer angle names. There may be others. */
    public StringArray getRotamerTorsionNames(){
	if(rotamerNames.size() == 0){
	    return null;
	}

	return (StringArray)rotamerTorsions.get(0);
    }
}
