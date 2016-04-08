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

/* Copyright Astex Technology Ltd. 1999 */

/*
 * 08-11-99 mjh
 *	created
 */

/**
 * A class for storing a group of atoms that form part
 * of a protein chain.
 */
public class Chain implements Selectable {
    /** Default constructor. */
    private Chain(){
	residues = new DynamicArray(1);
	initialise();
    }

    /** Initialise a chain object. */
    private void initialise(){
	name = null;
	number = undefinedChainNumber;
	parent = null;
	residues.removeAllElements();
	currentResidue = null;
    }

    /** Dynamic array of residues. */
    private DynamicArray residues = null;

    /** Undefined residue number. */
    public static int undefinedChainNumber = -9999;

    /** Undefined resiude name. */
    public static String undefinedChainName = "X";

    /**
     * Public interface for creating chains.
     */
    public static Chain create(){
	return new Chain();
    }

    /** Parent molecule. */
    private Molecule parent = null;
	
    /** Set the parent molecule. */
    public void setParent(Molecule molecule){
	parent = molecule;
    }

    /** Get the parent. */
    public Molecule getParent(){
	return parent;
    }

    /** Insertion code. */
    private char insertionCode = 0;

    /** Set the insertion code. */
    public void setInsertionCode(char code){
	insertionCode = code;
    }

    /** Get the insertion code. */
    public char getInsertionCode(){
	return insertionCode;
    }

    /** Chain name. */
    private String name = null;

    /** Set the chain name. */
    public void setName(String newName){
	name = newName;
    }

    /** Get the chain name. */
    public String getName(){
	if(name == null){
	    return undefinedChainName;
	}else{
	    return name;
	}
    }

    /** Chain number. */
    private int number;

    /** Set the chain number. */
    public void setNumber(int newNumber){
	number = newNumber;
    }

    /** Get the chain number. */
    public int getNumber(){
	if(number == undefinedChainNumber){
	    return 1;
	}else{
	    return number;
	}
    }

    /**
     * Return the number of atoms in the molecule.
     */
    public int getResidueCount(){
	return residues.size();
    }

    /**
     * Return the specified atom.
     */
    public Residue getResidue(int index){
	return (Residue)residues.get(index);
    }

    /** The current residue. */
    private Residue currentResidue = null;

    /** Add a residue to the chain. */
    public Residue addResidue(){
	currentResidue = Residue.create();
	currentResidue.setParent(this);

	residues.add(currentResidue);

	return currentResidue;
    }
	
    /** Remove a residue from the chain. */
    public void removeResidue(Residue res){
	residues.remove(res);

	Molecule mol = getParent();

	
    }

    /** Return the current residue. */
    public Residue getCurrentResidue(){
	if(currentResidue == null){
	    addResidue();
	}
		
	return currentResidue;
    }

    /** Get maximum residue id. */
    public int getMaximumResidueId(){
	int resCount = getResidueCount();
	int maximum = Integer.MIN_VALUE;

	for(int r = 0; r < resCount; r++){
	    Residue res = getResidue(r);
	    if(res.getNumber() > maximum){
		maximum = res.getNumber();
	    }
	}

	if(maximum == Integer.MIN_VALUE){
	    maximum = 0;
	}

	return maximum;
    }

    public String selectStatement(){
	Molecule mol = getParent();
	String molSelect = mol.selectStatement();
	return "chain '" + getName() + "' and " + molSelect;
    }

    /** Apply a selection recursively. */
    public int select(int state){
	int selectCount = 0;
	for(int r = 0; r < getResidueCount(); r++){
	    Residue residue = getResidue(r);
	    selectCount += residue.select(state);
	}

	return selectCount;
    }
}
