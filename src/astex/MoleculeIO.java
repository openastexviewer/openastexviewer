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
 * 07-07-04 mjh
 *	add ability to handle CONECT record bonds
 *	don't connect atoms if they both had bonds
 *	specified by CONECT records (use Atom.ConectRecords);
 * 12-02-04 mjh
 *	improve performance reading bonds of mol2 files
 *	when the atoms are in sequential order from 1.
 * 12-03-03 mjh
 *	fix more problems with 'H' and 'R' spacegroups
 *	basically only change to reflect new symop.lib spacegroup
 *	names.
 * 02-09-02 mjh
 *	exercise caution with the gzip extensions for filenames.
 *	if the file name is 1crn.pdb but the actual file is
 *	1crn.pdb.gz on the server, then microsoft servers will
 *	not handle this situation correctly. they will return
 *	an input stream to the gzip file when you try to
 *	open the .pdb name. This has been addressed internally
 *	in FILE
 * 13-08-02 mjh
 *	switch to the internal field splitting method for reading
 *	mol2 files. Should be much faster as it has no object
 *	creation overhead for all the strings.
 *	run jprobe, and make some straightforward speed 
 *	enhancements - replace all methods that make strings.
 *	especially the one in readPDB that was just converting
 *	every line to a string and then never using it.
 * 25-07-02 mjh
 *	Change the way chain names are read. Previously if
 *	the single character chain id was blank, looked
 *	in column 73-76 for an xplor style chain id. This
 *	doesn't work well with old pdb files, which put the
 *	pdb code there as part of a record identifier. Now
 *	the chain id is just read from the single character
 *	field.
 * 13-03-02 mjh
 *	Fix part of the symmetry generation problem. When the
 *	symmetry was read from a map file, it is indicated
 *	by a spacegroup number and was read correctly from the
 *	file of spacegroup operators. When it was read from a
 *	pdb file the last character was chopped off, meaning
 *	P212121 got mapped to P21212 and causing problems in
 * 	symmetry generation. Changed the last character from
 *	64 to 65 which fixes the problem.
 * 19-07-01 mjh
 *	Add bromine to the pdb reader.
 * 18-06-01 mjh
 *	Change the way pdb atom names are deciphered to
 *	correct some cases where carbons were mistaken for Cl.
 * 30-05-01 mjh
 *	fix bug reading space group name from pdb files. The
 *	line length was being ignored and garbage characters
 *	were added to the space group name.
 * 21-02-00 mjh
 *	make sybyl mol2 files store the atom id that is given in the file.
 * 17-01-00 mjh
 *	make Sybyl mol2 file reader use free format reads to
 *	date from lines. Different programs use different formats
 *	for the lines. This was causing problems with files exported
 *	from CSD for example.
 * 23-11-99 mjh
 *	fix two serious bugs in sybyl mol2 file reader.
 *	bond order was read from wrong column, and never
 *	stored if it was anything other than aromatic
 * 07-11-99 mjh
 *	add pdb file reader. Remove findRings() as it should get called
 *	when we ask for any rings.
 * 01-11-99 mjh
 *	add basic framework for reading sybyl mol2 files
 * 29-10-99 mjh
 *	created
 */

import java.io.*;
import java.util.*;

/**
 * 22-05-01 mjh
 * 	add .sdf as a file type for mol files.
 * 25-05-00 mjh
 *	make file types default to pdb files.
 *
 * A class for reading and writing molecules from various file formats.
 */
public class MoleculeIO {
    /** SYBYL mol file. */
    public static final String SybylMol2 = "mol2";

    /** MDL mol file. */
    public static final String MDLMol = "mol";

    /** simple mol file. */
    public static final String SimpleMol = "simple";

    /** simple xyzr file. */
    public static final String XyzrMol = "xyzr";

    /** simple tmesh file. */
    public static final String TmeshMol = "tmesh";

    /** PDB file. */
    public static final String PDBFile = "pdb";

    /** Static mapping of file extensions to AstexViewer molecule types. */
    public static String getTypeFromExtension(String filename){
	String type = null;

	if(filename == null){
	    return null;
	}

	if(filename.indexOf(".mol2") != -1 ||
	   filename.indexOf(".istr") != -1){
	    type = SybylMol2;
	}else if(filename.indexOf(".mol") != -1 ||
		 filename.indexOf(".sd") != -1 ||
		 filename.indexOf(".sdf") != -1){
	    type = MDLMol;
	}else if(filename.indexOf(".pdb") != -1){
	    type = PDBFile;
	}else if(filename.indexOf(".simple") != -1){
	    type = SimpleMol;
	}else if(filename.indexOf(".xyzr") != -1){
	    type = XyzrMol;
	}else if(filename.indexOf(".tmesh") != -1){
	    type = TmeshMol;
	}

	return type;
    }

    /**
     * Convenience method that will try and determine a file
     * type from its extension (not foolproof).
     */
    public static Molecule read(String filename){
	String type = PDBFile;

	//System.out.println("filename " + filename);

	type = getTypeFromExtension(filename);

	if(type != null){
	    //FILE file = FILE.openFile(filename);
	    FILE file = FILE.open(filename);

	    if(file == null){
		System.err.println("error opening " + filename);
		System.err.println("exception " + FILE.getException());

		return null;
	    }

	    long before = System.currentTimeMillis();

	    Molecule molecule = read(type, file);

	    molecule.setName(filename);
	    molecule.setFilename(filename);

	    long after = System.currentTimeMillis();
			
	    //System.out.println("read time " + (after - before));

	    //summariseMolecule(molecule);
			
	    file.close();
	    
	    return molecule;
	}

	return null;
    }

    /**
     * Main entry point for reading a molecule file.
     */
    public static Molecule read(String type, FILE file){
	Molecule molecule = null;

	if(type.equals(MDLMol)){
	    // MDL mol file
	    molecule = readMDLMol(file);

	}else if(type.equals(SybylMol2)){
	    // SYBYL mol2 file
	    molecule = readMol2(file);

	}else if(type.equals(PDBFile)){
	    // SYBYL mol2 file
	    molecule = readPDB(file);

	}else if(type.equals(SimpleMol)){
	    // simple molecule file
	    molecule = readSimple(file);

	}else if(type.equals(XyzrMol)){
	    // SYBYL mol2 file
	    molecule = readXyzr(file);

	}else if(type.equals(TmeshMol)){
	    // SYBYL mol2 file
	    molecule = readTmesh(file);

	}else{
	    System.err.println("MoleculeIO: format '" +
			       type + "' unsupported");

	    // eventually we will hope to have the ability to 
	    // dynamically load new mol file readers here.
	}

	molecule.setType(type);

	return molecule;
    }

    /**
     * Read the Sybyl atom block.
     */
    public static void readBondBlock(FILE file, int bondCount,
				     Molecule molecule,
				     boolean atomsInOrder){
	for(int i = 0; i < bondCount; i++){
	    file.nextLine();
	    //int firstAtom = file.readInteger(6, 5);
	    //int secondAtom = file.readInteger(11, 5);
	    int firstAtom = file.readIntegerFromField(1);
	    int secondAtom = file.readIntegerFromField(2);
	    int bondOrder = 0;

	    //String bondOrderToken = file.getField(3);
	    int field3start = file.getFieldStart(3);
	    char char17 = file.getChar(field3start);
	    char char18 = 0;
	    if(file.getFieldLength(3) > 1){
		//char18 = bondOrderToken.charAt(1);
		char18 = file.getChar(field3start+1);
	    }

	    if(char17 == 'a' && char18 == 'r'){
				//System.out.println("saw aromatic bond");

		bondOrder = Bond.AromaticBond;
	    }else if(char17 == 'a' && char18 == 'm'){
				//System.out.println("saw amide bond");

				// for now store as aromatic
		bondOrder = Bond.AmideBond;
	    }else{
		bondOrder = file.readIntegerFromField(3);
	    }

	    // if we know the atoms were in sequential order
	    // we can just look up the atoms direct from the
	    // ids.
	    if(atomsInOrder){
		Atom a1 = molecule.getAtom(firstAtom - 1);
		Atom a2 = molecule.getAtom(secondAtom - 1);
		
		Bond newBond =
		    molecule.addBond(a1, a2, bondOrder);
	    }else{
		// we have to search for the specific ids
		// this is much slower
		Bond newBond =
		    molecule.addBondFromIds(firstAtom, secondAtom, bondOrder);
	    }
	}
    }

    /**
     * Read the Sybyl atom block.
     */
    public static boolean readAtomBlock(FILE file, int atomCount,
					Molecule molecule){
	StringBuffer elementBuffer = new StringBuffer(2);
	boolean atomsInOrder = true;
        int previousId = Integer.MIN_VALUE;

	// read the specified number of atoms
	for(int i = 0; i < atomCount; i++){
	    file.nextLine();

            if(file.getFieldCount() >= 7){
                int resId = file.readIntegerFromField(6);

                if(resId != previousId){
		    Residue residue = molecule.addResidue();
		    //String residueName = file.getSubstring(17, 3);

                    if(file.getFieldCount() >= 8){
                        String resName = file.getField(7);
                        String resNumber = resName.substring(3);
                        resName = resName.substring(0, 3);

                        residue.setName(resName);

                        residue.setNumber(FILE.readInteger(resNumber));
                    }
                    previousId = resId;
		}

            }

	    // add an atom to the molecule and set its properties.
	    Atom newAtom = molecule.addAtom();

	    // store the atom id
	    newAtom.setId(file.readIntegerFromField(0));

	    if(atomsInOrder && newAtom.getId() != i + 1){
		System.err.println("atoms not in order in mol2 file");
		atomsInOrder = false;
	    }

	    //String atomLabel = file.getSubstring(8, 8);
	    // seem to have to keep the label as a string
	    String atomLabel = file.getField(1);

	    //atomLabel = atomLabel.trim();

	    //System.out.println("atomLabel " + atomLabel);

	    newAtom.setAtomLabel(atomLabel);

	    // grab the atomic coordinates
	    //double x = file.readDouble(16, 10);
	    //double y = file.readDouble(26, 10);
	    //double z = file.readDouble(36, 10);
	    double x = file.readDoubleFromField(2);
	    double y = file.readDoubleFromField(3);
	    double z = file.readDoubleFromField(4);

	    //System.out.println("x " + x + " y " + y + " z " + z);
			
	    newAtom.set(x, y, z);

            // set the sybyl atom type
            if(file.getFieldCount() >= 6){
                newAtom.setAtomType(file.getField(5));

                // figure out the element type.
                //String elementString = file.getField(5);
                int field5start = file.getFieldStart(5);
                elementBuffer.setLength(0);

                //char char0 = file.getChar(47);
                //char char1 = file.getChar(48);
                char char0 = file.getChar(field5start);

                elementBuffer.append(char0);
                if(file.getFieldLength(5) > 1){
                    char char1 = file.getChar(field5start+1);
                    
                    if(char1 != '.' && char1 != ' '){
                        elementBuffer.append(char1);
                    }
                }

                //System.out.println("elementBuffer <" + elementBuffer + ">");
                
                int element =
                    PeriodicTable.getElementFromSymbol(elementBuffer.toString());

                //System.out.println("element " + element);

                newAtom.setElement(element);
            }

	    if(file.getFieldCount() >= 9){
		newAtom.setBFactor(file.readDoubleFromField(8));
	    }

	}

	//System.out.println("atomsInOrder " + atomsInOrder);

	return atomsInOrder;
    }

    /**
     * Read a SYBYL mol file.
     */
    public static Molecule readMol2(FILE file){
	Molecule molecule = new Molecule();
	int atomCount = 0, bondCount = 0, subCount = 0;
	double averageDenstiy = -1.0;
	int symmetryElements  = 1;
	boolean atomsInOrder = false;

	while(file.nextLine()){
	    char c0 = file.getChar(0);
	    if(c0 == '@'){
		char char9 = file.getChar(9);
		if(char9 == 'M' &&
		   file.currentLineContains("@<TRIPOS>MOLECULE", 0)){
		    if(file.nextLine() == false){
			System.err.println("error reading molecule header");
		    }
					
		    // this line contains the molecule name.
		    molecule.setName(file.getCurrentLineAsString());
					
		    if(file.nextLine() == false){
			System.err.println("error reading molecule header");
		    }

		    //oldway
		    //oldway broken by files that use different formats
		    //oldway atomCount = file.readInteger(0, 5);
		    //oldway bondCount = file.readInteger(6, 5);

		    String line = file.getCurrentLineAsString();
		    String tokens[] = FILE.split(line);

		    atomCount = FILE.readInteger(tokens[0]);
		    bondCount = FILE.readInteger(tokens[1]);
		    //System.out.println("atomCount = " + atomCount);
		    //System.out.println("bondCount = " + bondCount);

		}else if(char9 == 'A' &&
			 file.currentLineContains("@<TRIPOS>ATOM", 0)){

		    atomsInOrder = readAtomBlock(file, atomCount, molecule);
		}else if(char9 == 'B' &&
			 file.currentLineContains("@<TRIPOS>BOND", 0)){

		    readBondBlock(file, bondCount, molecule, atomsInOrder);
		}
	    }else if(c0 == '#'){
		char c1 = file.getChar(1);
		char c2 = file.getChar(2);
		if(c1 == ' ' && c2 == 'N'){
		    // should have the central group atom count
		    String line = file.getCurrentLineAsString();
		    if(line.startsWith("# Number_Of_Central_Group_Atoms:")){
			int ncentral = file.getInteger(2);
			//Log.info("central group atoms %d", ncentral);
			molecule.setCentralAtomCount(ncentral);
		    }
		}else if(c1 == ' ' && c2 == 'A'){
		    String line = file.getCurrentLineAsString();
		    if(line.startsWith("# Average_Density:")){
			averageDenstiy = file.getDouble(2);
			//System.out.println("average density " +
			//		   averageDenstiy);
		    }
		}else if(c1 == 'E' && c2 == 'L'){
		    // should hold a matrix
		    Matrix symmetry = new Matrix();
		    symmetry.x00 = file.getDouble(1);
		    symmetry.x01 = file.getDouble(2);
		    symmetry.x02 = file.getDouble(3);
		    symmetry.x10 = file.getDouble(4);
		    symmetry.x11 = file.getDouble(5);
		    symmetry.x12 = file.getDouble(6);
		    symmetry.x20 = file.getDouble(7);
		    symmetry.x21 = file.getDouble(8);
		    symmetry.x22 = file.getDouble(9);

		    symmetryElements++;

		    //symmetry.print("symmetry matrix");

		    int ncentral = molecule.getCentralAtomCount();
		    Point3d p = new Point3d();

		    //Log.info("initial atomCount %5d",
		    //molecule.getAtomCount());

		    int currentAtomCount = molecule.getAtomCount();

		    for(int a = ncentral; a < atomCount; a++){
			Atom atom = molecule.getAtom(a);
			p.set(atom);
			symmetry.transform(p);
			Atom newAtom = molecule.addAtom();
			newAtom.set(p);
			newAtom.setId(currentAtomCount++);
			newAtom.setElement(atom.getElement());
		    }

		    //Log.info("final atomCount %5d", molecule.getAtomCount());
		}
	    }
	}

	// there was an average density in the molecule so
	// set all the b-factors to that.
	if(averageDenstiy > 0.0){
	    // we should multiply average density
	    // by the number of symmetry elements
	    averageDenstiy *= symmetryElements;

	    atomCount = molecule.getAtomCount();
	    for(int a = 0; a < atomCount; a++){
		Atom atom = molecule.getAtom(a);
		atom.setBFactor(averageDenstiy);
	    }
	}else{
	    averageDenstiy = 1.0;
	}

        if(molecule.getChainCount() > 0){
            Chain chain = molecule.getChain(0);

            chain.setName(" ");
        }

	return molecule;
    }

    /**
     * Summarise the contents of a molecule.
     */
    public static void summariseMolecule(Molecule molecule){
		
	System.out.println("name " + molecule.getName());
	int atomCount = molecule.getAtomCount();
	int bondCount = molecule.getBondCount();

	System.out.println("" + atomCount + " atoms " + bondCount + " bonds ");
    }

    public static Molecule readTmesh(FILE file){
	Molecule molecule = new Molecule();
	int acount = 0;

	file.nextLine();

	acount = file.readIntegerFromField(0);

	for(int i = 0; i < acount; i++){
	    file.nextLine();
	    Atom atom = molecule.addAtom();
	    atom.setId(i);

	    double x = file.readDoubleFromField(0);
	    double y = file.readDoubleFromField(1);
	    double z = file.readDoubleFromField(2);

	    //atom.setCoordinates(x, y, z);
	    atom.set(x, y, z);

	    atom.setElement(1);
	}

	file.nextLine();

	int bcount = file.readIntegerFromField(0);

	int vertices[] = new int[100];

	for(int i = 0; i < bcount; i++){
	    file.nextLine();
	    int vcount = file.readIntegerFromField(0);

	    for(int v = 0; v < vcount; v++){
		file.nextLine();
		vertices[v] = file.readIntegerFromField(0);
	    }

	    for(int v = 0; v < vcount; v++){
		int v1 = (v + 1) % vcount;
		if(vertices[v] == -1 || vertices[v1] == -1){
		    System.err.println("invalid vertex " +
				       vertices[v] + " " + vertices[v1]);
		}else{
		    molecule.addBond(vertices[v], vertices[v1], 1);
		}
	    }
	}	

	return molecule;
    }

    public static Molecule readXyzr(FILE file){
	Molecule molecule = new Molecule();
	int acount = 0;

	// read the molcule name.
	while(file.nextLine()){
	    Atom atom = molecule.addAtom();
	    acount++;
	    atom.setId(acount);

	    // first come the coordinates
	    double x = file.readDoubleFromField(0);
	    double y = file.readDoubleFromField(1);
	    double z = file.readDoubleFromField(2);

	    //atom.setCoordinates(x, y, z);
	    atom.set(x, y, z);

	    double r = 1.5;

            if(file.getFieldCount() == 4){
                file.readDoubleFromField(3);
            }

	    atom.setVDWRadius(r);

	    atom.setElement(0);

	    atom.setColor(Color32.white);
	}

	return molecule;
    }

    /**
     * Read an an MDL mol file.
     */
    public static Molecule readSimple(FILE file){
	Molecule molecule = new Molecule();

	// read the molcule name.
	file.nextLine();

	int atomCount = file.readIntegerFromField(0);

	System.err.println("atomCount " + atomCount);

	for(int i = 0 ; i < atomCount; i++){
	    file.nextLine();
	    Atom atom = molecule.addAtom();
	    atom.setId(i);

	    int element = PeriodicTable.getElementFromSymbol(file.getField(0));
			
	    atom.setElement(element);

	    // first come the coordinates
	    double x = file.readDoubleFromField(1);
	    double y = file.readDoubleFromField(2);
	    double z = file.readDoubleFromField(3);

	    //atom.setCoordinates(x, y, z);
	    atom.set(x, y, z);
	}

	file.nextLine();

	int bondCount = file.readIntegerFromField(0);

	//next is the bond block
	for(int b = 0; b < bondCount; b++){
	    file.nextLine();
	    int firstAtom = file.readIntegerFromField(0);
	    int secondAtom = file.readIntegerFromField(1);
	    int bondOrder = file.readIntegerFromField(2);

	    //System.out.println("firstAtom " + firstAtom);
	    //System.out.println("secondAtom " + secondAtom);
	    Bond bond = molecule.addBond(firstAtom - 1, secondAtom - 1,
					 bondOrder);
	}

	return molecule;
    }

    /**
     * Read an an MDL mol file.
     */
    public static Molecule readMDLMol(FILE file){
	// read the molcule name.
	if(file.nextLine() == false){
	    return null;
	}

	Molecule molecule = new Molecule();

	molecule.setName(file.getCurrentLineAsString());

	// the next two lines contains stuff we don't need to
	// worry about.
	file.nextLine();
	file.nextLine();

	// the next line has the atom and bond counts amongst other things.
	file.nextLine();

	int atomCount = file.readInteger(0, 3);
	int bondCount = file.readInteger(3, 3);

	//System.out.println("atoms " + atomCount + " bonds " + bondCount);
		
	// next is the atom block

	for(int i = 0; i < atomCount; i++){
	    file.nextLine();
	    Atom atom = molecule.addAtom();
	    atom.setId(i + 1);

	    // first come the coordinates
	    double x = file.readDouble(0, 10);
	    double y = file.readDouble(10, 10);
	    double z = file.readDouble(20, 10);

	    //atom.setCoordinates(x, y, z);
	    atom.set(x, y, z);

	    // then the element type
	    String atomName = file.getSubstring(31, 2).trim();

	    int element = PeriodicTable.getElementFromSymbol(atomName);
			
	    atom.setElement(element);

	    // charge
	    int charge = file.readInteger(37, 2);

	    if(charge != 0){
				// charge is stored in strange way.
		charge = -charge + 4;
		atom.setCharge(charge);
	    }
	}

	//next is the bond block
	for(int b = 0; b < bondCount; b++){
	    file.nextLine();
	    int firstAtom = file.readInteger(0, 3);
	    int secondAtom = file.readInteger(3, 3);
	    int bondOrder = file.readInteger(6, 3);

	    //System.out.println("firstAtom " + firstAtom);
	    //System.out.println("secondAtom " + secondAtom);
	    Bond bond = molecule.addBond(firstAtom - 1, secondAtom - 1,
					 bondOrder);
	}

	while(file.nextLine()){
	    if(file.getChar(0) == '$' && file.getChar(1) == '$' &&
	       file.getChar(2) == '$' && file.getChar(3) == '$'){
		break;
	    }
	}

        /*
        for(int r = 0; r < molecule.getRingCount(); r++){
            Ring ring = molecule.getRing(r);

            System.out.println(r + " size " + ring.getAtomCount());
        }
        */

	return molecule;
    }

    /* The last values for the various residue and chain names. */
    private static int lastResidueNumber;
    private static char lastInsertionCode;
    private static char lastChainId;
    private static char lastResidueA;
    private static char lastResidueB;
    private static char lastResidueC;
    private static char lastXplorChainId1;
    private static char lastXplorChainId2;
    private static char lastXplorChainId3;
    private static char lastXplorChainId4;
	
    /** Initialise the values for chain ids. */
    private static void initialiseReader(){
	lastResidueNumber = Residue.undefinedResidueNumber;
	lastInsertionCode = 0;
	lastResidueA = 0;
	lastResidueB = 0;
	lastResidueC = 0;
	lastXplorChainId1 = 0;
	lastXplorChainId2 = 0;
	lastXplorChainId3 = 0;
	lastXplorChainId4 = 0;
	lastChainId = 0;
    }

    /**
     * Do we need a new chain.
     * Simplified case. Ignore the contents of columns 73-76.
     */
    private static boolean needNewChain(char currentChainId,
					char xplorChainId1,
					char xplorChainId2,
					char xplorChainId3,
					char xplorChainId4){
	if(currentChainId != lastChainId){
	    initialiseReader();

	    lastChainId = currentChainId;
	    return true;
	}

	return false;
    }

    /** Do we need a new chain. */
    private static boolean needNewChain2(char currentChainId,
					char xplorChainId1,
					char xplorChainId2,
					char xplorChainId3,
					char xplorChainId4){
	if(currentChainId != ' ' &&
	   lastChainId != currentChainId){
	    initialiseReader();

	    lastChainId = currentChainId;
	    return true;
	}else if(currentChainId == ' '&&
		 (lastXplorChainId1 != xplorChainId1 ||
		  lastXplorChainId2 != xplorChainId2 ||
		  lastXplorChainId3 != xplorChainId3 ||
		  lastXplorChainId4 != xplorChainId4)){
	    initialiseReader();

	    lastXplorChainId1 = xplorChainId1;
	    lastXplorChainId2 = xplorChainId2;
	    lastXplorChainId3 = xplorChainId3;
	    lastXplorChainId4 = xplorChainId4;

	    return true;
	}else{
	    return false;
	}
    }

    /** Do we need a new residue? */
    public static boolean needNewResidue(int currentResidueNumber,
					 char currentInsertionCode,
					 char currentResidueA,
					 char currentResidueB,
					 char currentResidueC){
	if(lastResidueA != currentResidueA ||
	   lastResidueB != currentResidueB ||
	   lastResidueC != currentResidueC ||
	   lastResidueNumber != currentResidueNumber ||
	   lastInsertionCode != currentInsertionCode){

	    lastResidueNumber = currentResidueNumber;
	    lastInsertionCode = currentInsertionCode;
	    lastResidueA = currentResidueA;
	    lastResidueB = currentResidueB;
	    lastResidueC = currentResidueC;

	    return true;
	}else{
	    return false;
	}
    }

    /**
     * Read a PDB file from the input.
     */
    public static Molecule readPDB(FILE file){
	Molecule molecule = new Molecule();
        boolean seenENDMDL = false;

	// initialise all of the reader variables.
	initialiseReader();

	// each line is identified with its type.
	while(file.nextLine()){
	    //System.out.println("line: " + file.getCurrentLineAsString());

	    // work around bug in mac io...
	    //if(file.getLineLength() == 0){
	    //	break;
	    //}

	    char c0 = file.getChar(0);
	    char c1 = file.getChar(1);
	    char c2 = file.getChar(2);
	    char c3 = file.getChar(3);

            if(c0 == 'E' && c1 == 'N' && c2 == 'D' && c3 == 'M'){
                seenENDMDL = true;
            }

	    // its an atom.
	    if(seenENDMDL == false &&
               ((c0 == 'A' && c1 == 'T' && c2 == 'O' && c3 == 'M') ||
                (c0 == 'H' && c1 == 'E' && c2 == 'T' && c3 == 'A'))){

		int residueId = file.readInteger(22, 4);
				// where is the insertion code exactly???
		char insertionCode = file.getChar(26);
		char chainId = file.getChar(21);
		char xplorChainId1 = file.getChar(72);
		char xplorChainId2 = file.getChar(73);
		char xplorChainId3 = file.getChar(74);
		char xplorChainId4 = file.getChar(75);
		char ca = file.getChar(17);
		char cb = file.getChar(18);
		char cc = file.getChar(19);

		if(needNewChain(chainId,
				xplorChainId1, xplorChainId2,
				xplorChainId3, xplorChainId4)){
		    Chain chain = molecule.addChain();
		    //if(chainId != ' '){
		    //		    if(chainId != ' ' ||
		    //   (xplorChainId1 == ' ' || xplorChainId1 == FILE.EOF) &&
		    //   (xplorChainId2 == ' ' || xplorChainId2 == FILE.EOF) &&
		    //  (xplorChainId3 == ' ' || xplorChainId3 == FILE.EOF) &&
		    //   (xplorChainId4 == ' ' || xplorChainId4 == FILE.EOF)){

			chain.setName(file.getSubstring(21, 1));

			//}else{
			//chain.setName(file.getSubstring(72, 4));
			//}						
		}

		if(needNewResidue(residueId, insertionCode, ca, cb, cc)){
		    Residue residue = molecule.addResidue();
		    //String residueName = file.getSubstring(17, 3);
		    String residueName = getResidueName(ca, cb, cc);
		    residue.setNumber(residueId);
		    residue.setInsertionCode(insertionCode);
		    residue.setName(residueName);
		}

		readPDBAtom(file, molecule);
	    }else if(c0 == 'C' && c1 == 'O' && c2 == 'N' && c3 == 'E'){
		// its a connect record.
		int firstId = file.readInteger(6, 5);
		Atom firstAtom = null;
		
		//if(firstAtom == null){
		//    System.out.println("first atom in conect is null");
		//}

		//firstAtom.attributes |= Atom.ConectRecords;

		int lineLength = file.getLineLength();

		for(int i = 0; i < 6; i++){
		    int start = 11 + i * 5;
		    int secondId = file.readInteger(11 + i * 5, 5);

		    if(secondId == 0){
			break;
		    }

                    // only pay attention when new atom has id more than first atom
                    if(secondId > firstId){

                        if(firstAtom == null){
                            firstAtom = molecule.getAtomWithId(firstId);

                            // we may not have this atom as
                            // we could have skipped its model
                            if(firstAtom == null){
                                break;
                            }
                        }
                        Atom secondAtom = molecule.getAtomWithId(secondId);

                        if(secondAtom != null){

                            //secondAtom.attributes |= Atom.ConectRecords;
                            
                            Bond bond = firstAtom.getBond(secondAtom);
                            
			    //System.out.println("addBond " + firstId + " " + secondId);
			
			    if(bond != null){
				bond.setBondOrder(bond.getBondOrder()+ 1);
			    }else{
				bond = molecule.addBond(firstAtom, secondAtom, Bond.SingleBond);
			    }
			}
		    }
		}
	    }else if(c0 == 'C' && c1 == 'R' && c2 == 'Y' && c3 == 'S'){
		readUnitCell(molecule, file);

		//System.out.println("finished in unit cell");
	    }else if(c0 == 'R' && c1 == 'E' && c2 == 'M' ){
		//readCNXUnitCell(molecule, file);
	    }else if(c0 == 'S' && c1 == 'C' && c2 == 'A' && c3 == 'L'){
		readScaleRecord(molecule, file);
	    }
	}

	Symmetry symmetry = molecule.getSymmetry();

	if(symmetry != null){
	    symmetry.prepareSymmetry();

	    //System.out.println("finished preparing symmetry");
	}

	// pdb files don't usually have explicit connectivity

	//molecule.connect();
	molecule.connect2();
	//System.out.println("finished connect");

	return molecule;
    }

    /** Read a scale record from the input file. */
    public static void readScaleRecord(Molecule molecule, FILE file){
	Symmetry symmetry = molecule.getSymmetry();

	if(symmetry == null){
	    System.err.println("readScaleRecord: molecule has scale but " +
			       "no CRYST1 record");
	    return;
	}else{
	    // assign the scale matrices.
	    if(symmetry.scale == null){
		symmetry.scale = new Matrix();
	    }
	}

	Matrix r = symmetry.scale;

	char c5 = file.getChar(5);

	if(c5 == '1'){
	    r.x00 = file.readDouble(11, 9);
	    r.x10 = file.readDouble(21, 9);
	    r.x20 = file.readDouble(31, 9);
	    r.x30 = file.readDouble(44, 11);
	}else if(c5 == '2'){
	    r.x01 = file.readDouble(11, 9);
	    r.x11 = file.readDouble(21, 9);
	    r.x21 = file.readDouble(31, 9);
	    r.x31 = file.readDouble(44, 11);
	}else if(c5 == '3'){
	    r.x02 = file.readDouble(11, 9);
	    r.x12 = file.readDouble(21, 9);
	    r.x22 = file.readDouble(31, 9);
	    r.x32 = file.readDouble(44, 11);
	}else{
	    System.err.println("readScaleRecord: illegal scale record");
	    System.err.println(file.getCurrentLineAsString());
	    return;
	}
    }

    /** Read the unit cell info from the current line. */
    public static void readUnitCell(Molecule molecule, FILE file){
	//Symmetry symmetry = molecule.getSymmetry();
	Symmetry symmetry = new Symmetry();

	molecule.setSymmetry(symmetry);

	double cell[] = new double[6];

	cell[0] = file.readDouble(6, 9);
	cell[1] = file.readDouble(15, 9);
	cell[2] = file.readDouble(24, 9);
	cell[3] = file.readDouble(33, 7);
	cell[4] = file.readDouble(40, 7);
	cell[5] = file.readDouble(47, 7);

	molecule.setUnitCell(cell);

	StringBuffer spaceGroupName = new StringBuffer();
	String originalSpaceGroupName = file.getSubstring(55, 10);
	if(originalSpaceGroupName != null){
	    originalSpaceGroupName = originalSpaceGroupName.trim();
	}

	symmetry.setOriginalSpaceGroupName(originalSpaceGroupName);

	int len = file.getLineLength();

	if(len > 65){
	    len = 65;
	}

	for(int i = 55; i < len; i++){
	    char c = file.getChar(i);
	    // if the first char is H
	    // turn it to R to fix problems with Hexagonally
	    // classified spacegroups
	    if(i == 55){
		if(c == 'R' && Math.abs(cell[3] - cell[5]) > 0.001){
		    System.err.println("Spacegroup changed from R to H " +
				       "classification as alpha != gamma");
		    c ='H';
		}
	    }
	    if(c != ' '){
		spaceGroupName.append(c);
	    }
	}

	if(spaceGroupName.length() != 0){
	    molecule.setSpaceGroupName(spaceGroupName.toString());
	}else{
	    System.err.println("molecule had CRYST1 record but no spacegroup");
	    System.err.println(file.getCurrentLineAsString());

	    molecule.setSpaceGroupName(null);
	}
    }
	
    /**
     * Read a pdb atom from the current record.
     */
    public static Atom readPDBAtom(FILE file, Molecule molecule){
	Atom atom = molecule.addAtom();

	// atom name
	char c12 = file.getChar(12);
	char c13 = file.getChar(13);
	char c14 = file.getChar(14);
	char c15 = file.getChar(15);
	String atomLabel = getAtomName(c12, c13, c14, c15);
		
	//System.out.println("label <" + atomLabel + ">");
	atom.setAtomLabel(atomLabel);

	// record if the atom had a left justified name...
	if(c12 != ' '){
	    atom.attributes |= Atom.NameLeftJustified;
	}
		
	if(isSolventAtom()){
	    atom.setSolvent(true);
	}

	char c0 = file.getChar(0);

	if(c0 == 'H'){
	    atom.setHeteroAtom(true);
	}

	// atom id
	int id = file.readInteger(6, 5);
	
	atom.setId(id);

	// insertion code. 
	char c16 = file.getChar(16);
	atom.setInsertionCode(c16);

	// atom coordinates
	double x = file.readDouble(30, 8);
	double y = file.readDouble(38, 8);
	double z = file.readDouble(46, 8);
		
	//atom.setCoordinates(x, y, z);
	atom.set(x, y, z);

	// set bFactor and occupancy
	double o = file.readDouble(56, 4);
	atom.setOccupancy(o);

	double b = file.readDouble(60, 6);
	atom.setBFactor(b);

	// figure out the element
	int element = PeriodicTable.UNKNOWN;

	// assign the element
	if(file.getLineLength() >= 78){
	    char e0 = file.getChar(76);
	    char e1 = file.getChar(77);

	    element = PeriodicTable.getElementFromSymbol(e0, e1);
	}

	if(element == PeriodicTable.UNKNOWN){
	    element = getElementFromPDBAtomLabel(c12, c13);
	}

	atom.setElement(element);

	return atom;
    }

    /** Is the atom label a solvent label. */
    public static boolean isSolventAtom(){
	if((lastResidueA == 'H' &&
	    lastResidueB == 'O' &&
	    lastResidueC == 'H') ||
	   (lastResidueA == 'W' &&
	    lastResidueB == 'A' &&
	    lastResidueC == 'T')){
	    return true;
	}

	return false;
    }

    /** Assign element type from pdb atom label. */
    public static int getElementFromPDBAtomLabel(char c0, char c1){

	if(c0 == ' '){
	    // its a standard amino acid atom.
	    switch(c1){
	    case 'C': return PeriodicTable.CARBON;
	    case 'O': return PeriodicTable.OXYGEN;
	    case 'N': return PeriodicTable.NITROGEN;
	    case 'S': return PeriodicTable.SULPHUR;
	    case 'P': return PeriodicTable.PHOSPHORUS;
	    case 'F': return PeriodicTable.FLUORINE;
	    case 'H': return PeriodicTable.HYDROGEN;
	    case 'W': return PeriodicTable.WOLFRAM;
	    case 'Q': return PeriodicTable.UNKNOWN;
	    default : return PeriodicTable.CARBON;
	    }
	}else{
	    // its not a standard amino acid...
	    // this needs some work.
	    switch(c0){
	    case 'C':
		if(c1 == 'l' || c1 == 'L'){
		    return PeriodicTable.CHLORINE;
		}else{
		    return PeriodicTable.CARBON;
		}
	    case 'H': return PeriodicTable.HYDROGEN;
	    case 'F': return PeriodicTable.IRON;
	    case 'W': return PeriodicTable.WOLFRAM;
	    case 'B':
		if(c1 == 'R' || c1 == 'r'){
		    return PeriodicTable.BROMINE;
		}else{
		    return PeriodicTable.BORON;
		}
	    case '1': case '2': case '3': case '4': case '5':
	    case '6': case '7': case '8': case '9': case '0':
		return PeriodicTable.HYDROGEN;
	    }
	}

	// or should we return unknown which is technically true
	return PeriodicTable.CARBON;
    }

    /** Look up the residue name. */
    public static String getResidueName(char a, char b, char c){
	int sum = a + 256*b + 256*256*c;
	switch(sum){
	case 4268064: return "A";
	case 4399136: return "C";
	case 4661280: return "G";
	case 5578784: return "U";
	case 4279361: return "ALA";
	case 4674113: return "ARG";
	case 5133121: return "ASN";
	case 5264193: return "ASP";
	case 5462339: return "CYS";
	case 5590087: return "GLU";
	case 5131335: return "GLN";
	case 5852231: return "GLY";
	case 5458248: return "HIS";
	case 4541513: return "ILE";
	case 5588300: return "LEU";
	case 5462348: return "LYS";
	case 5522765: return "MET";
	case 4540496: return "PHE";
	case 5198416: return "PRO";
	case 5391699: return "SER";
	case 5263956: return "TRP";
	case 5392468: return "THR";
	case 5396820: return "TYR";
	case 4997462: return "VAL";
	case 5521751: return "WAT";
	default:
	    char tmp[] = new char[3];
	    tmp[0] = a;
	    tmp[1] = b;
	    tmp[2] = c;
	    String s = new String(tmp, 0, 3);
	    return s.trim();
	}
    }

    /** Look up the atom name. If it is a standard PDB one we will use it. */
    public static String getAtomName(char a, char b, char c, char d){

	int sum = a + 256*b + 65536*c + 16777216*d;
	switch(sum){
	case  538985248: return "C";
	case  707871520: return "C1*";
	case  540164896: return "C2";
	case  707937056: return "C2*";
	case  708002592: return "C3*";
	case  540295968: return "C4";
	case  708068128: return "C4*";
	case  540361504: return "C5";
	case  708133664: return "C5*";
	case  540427040: return "C6";
	case  540558112: return "C8";
	case  541147936: return "CA";
	case  541213472: return "CB";
	case  541344544: return "CD";
	case  826557216: return "CD1";
	case  843334432: return "CD2";
	case  541410080: return "CE";
	case  826622752: return "CE1";
	case  843399968: return "CE2";
	case  860177184: return "CE3";
	case  541541152: return "CG";
	case  826753824: return "CG1";
	case  843531040: return "CG2";
	case  843596576: return "CH2";
	case  542786336: return "CZ";
	case  844776224: return "CZ2";
	case  861553440: return "CZ3";
	case  538988064: return "N";
	case  540102176: return "N1";
	case  540167712: return "N2";
	case  540233248: return "N3";
	case  540298784: return "N4";
	case  540429856: return "N6";
	case  540495392: return "N7";
	case  540626464: return "N9";
	case  826560032: return "ND1";
	case  843337248: return "ND2";
	case  541412896: return "NE";
	case  826625568: return "NE1";
	case  843402784: return "NE2";
	case  826822176: return "NH1";
	case  843599392: return "NH2";
	case  542789152: return "NZ";
	case  538988320: return "O";
	case 1345408800: return "O1P";
	case  540167968: return "O2";
	case  707940128: return "O2*";
	case 1345474336: return "O2P";
	case  708005664: return "O3*";
	case  540299040: return "O4";
	case  708071200: return "O4*";
	case  708136736: return "O5*";
	case  540430112: return "O6";
	case  826560288: return "OD1";
	case  843337504: return "OD2";
	case  826625824: return "OE1";
	case  843403040: return "OE2";
	case  541544224: return "OG";
	case  826756896: return "OG1";
	case  541609760: return "OH";
	case 1415073568: return "OXT";
	case  538988576: return "P";
	case  541348640: return "SD";
	case  541545248: return "SG";
	    
	default:
	    char tmp[] = new char[4];
	    tmp[0] = a;
	    tmp[1] = b;
	    tmp[2] = c;
	    tmp[3] = d;
	    String string = new String(tmp, 0, 4);
	    //System.out.println("building string "+string);
	    return string.trim();
	}
    }

    /* File output methods. */
   
    /** Write a Molecule. */
    public static void write(Molecule molecule, FILE output){
	write(molecule, output, null);
    }

    public static void write(Molecule molecule, FILE output, String type){
	if(type == null){
	    type = molecule.getType();
	}

	if(MDLMol.equals(type)){
	    writeMDLMol(molecule, output);
	}else if(PDBFile.equals(type)){
	    writePDB(molecule, output);
	}else if(SybylMol2.equals(type)){
	    writeMol2(molecule, output);
	}else{
	    System.err.println("MoleculeIO.write: unsupported type: " + type);
	}
    }

    /** Write a molecule out as a PDB file. */
    public static void writePDB(Molecule molecule, FILE output){
	Symmetry symmetry = molecule.getSymmetry();

	output.println("REMARK Written by AstexViewer " + Version.getVersion());

	// write out the symmetry if any is defined for the molecule
	// this requires the CRYST1 record at the minimum.
	// if a SCALE record was present in the input, and was
	// determined as being different from that produced from
	// the unit cell parameters, then we must output this as well
	if(symmetry != null){
	    output.print("CRYST1");
	    for(int i = 0; i < 3; i++)
		output.print("%9.3f", symmetry.unitCell[i]);
	    for(int i = 3; i < 6; i++)
		output.print("%7.2f", symmetry.unitCell[i]);
	    output.println(" " + symmetry.getOriginalSpaceGroupName());

	    Matrix scale = symmetry.scale;

	    if(scale != null){
		// we need to write out the scale matrix

		output.print("SCALE1    ");
		output.print("%10.6f", scale.x00);
		output.print("%10.6f", scale.x10);
		output.print("%10.6f", scale.x20);
		output.print("     %10.5f\n", scale.x30);
		output.print("SCALE2    ");
		output.print("%10.6f", scale.x01);
		output.print("%10.6f", scale.x11);
		output.print("%10.6f", scale.x21);
		output.print("     %10.5f\n", scale.x31);
		output.print("SCALE3    ");
		output.print("%10.6f", scale.x02);
		output.print("%10.6f", scale.x12);
		output.print("%10.6f", scale.x22);
		output.print("     %10.5f\n", scale.x32);
	    }
	}

	int atomCount = molecule.getAtomCount();

	for(int i = 0; i < atomCount; i++){
	    Atom atom = molecule.getAtom(i);

	    if(atom.isHeteroAtom()){
		output.print("HETATM");
	    }else{
		output.print("ATOM  ");
	    }

	    output.print("%5d", atom.getId());

	    String atomName = atom.getAtomLabel();
	    int len = atomName.length();

	    output.print(" ");

	    // need to handle hydrogens a bit differently
	    if(len == 4){
		output.print("%s", atomName);
	    }else if(len == 3){
		if((atom.attributes & Atom.NameLeftJustified) != 0){
		    output.print("%s ", atomName);
		}else{
		    output.print(" %s", atomName);
		}
	    }else if(len == 2){
		if((atom.attributes & Atom.NameLeftJustified) != 0){
		    output.print("%s  ", atomName);
		}else{
		    output.print(" %s ", atomName);
		}
	    }else if(len == 1){
		if((atom.attributes & Atom.NameLeftJustified) != 0){
		    output.print("%s   ", atomName);
		}else{
		    output.print(" %s  ", atomName);
		}
	    }

	    char altLoc = atom.getInsertionCode();

	    output.print("%c", altLoc);

	    Residue res = atom.getResidue();

	    output.print("%-3s", res.getName());

	    Chain chain = res.getParent();

	    String chainName = chain.getName();

	    if(chainName.length() > 1){
		System.err.println("MoleculeIO.writePDB: chain name > 1 character |" +
				   chainName + "|");
		chainName = chainName.substring(0, 1);
	    }

	    output.print(" ");

	    output.print("%s", chainName);

	    output.print("%4d", res.getNumber());

	    char c = res.getInsertionCode();

	    output.print("%c", c);

	    output.print("   ");

	    output.print("%8.3f", atom.getX());
	    output.print("%8.3f", atom.getY());
	    output.print("%8.3f", atom.getZ());
	    output.print("%6.2f", atom.getOccupancy());
	    output.print("%6.2f", atom.getBFactor());
	    output.print("          ");

	    String symbol =
		PeriodicTable.getAtomSymbolFromElement(atom.getElement());

	    if(symbol.length() == 1){
		output.print(" %s", symbol);
	    }else{
		output.print("%s", symbol.toUpperCase());
	    }

	    output.print("\n");
	}

	for(int i = 0; i < atomCount; i++){
	    Atom atom = molecule.getAtom(i);
	    int bondCount = atom.getBondCount();
	    boolean needsConects = false;
	    for(int b = 0; b < bondCount; b++){
		Bond bond = atom.getBond(b);
		if(bond.getBondOrder() > 1){
		    needsConects = true;
		    break;
		}
	    }

	    for(int b = 0; b < bondCount; b++){
		Bond bond = atom.getBond(b);
		if(needsConects || bond.isExplicitBond()){
		    // need to print the conect info out
		    Atom otherAtom = bond.getOtherAtom(atom);
		    output.print("CONECT");
		    output.print("%5d", atom.getId());
		    int bondOrder = bond.getBondOrder();
		    int otherId = otherAtom.getId();
		    for(int bo = 0; bo < bondOrder; bo++){
			output.print("%5d", otherId);
		    }
		    output.print("\n");
		}
	    }
	}

	//output.print("END\n");
    }

    /** Write an Sybyl mol2 file to the output stream. */
    public static void writeMol2(Molecule molecule, FILE output){
        output.println("# Sybyl Mol2 file written by AstexViewer " + Version.getVersion());
        output.println("@<TRIPOS>MOLECULE");
        output.println(molecule.getName());

        int residueCount = molecule.getResidueCount();
        output.print("%d", molecule.getAtomCount());
        output.print(" %d", molecule.getBondCount());
        output.print(" %d\n", residueCount);
        output.println((residueCount == 1) ? "SMALL" : "PROTEIN");
        output.println("USER_CHARGES");
        output.println("");
        output.println("");

        int atomCount = molecule.getAtomCount();

        Residue prevRes = null;
        int resNumber = 0;

        Hashtable atomNumberHash = new Hashtable();

        output.println("@<TRIPOS>ATOM");
        for(int a = 0; a < atomCount; a++){
            Atom atom = molecule.getAtom(a);
            output.print("%5d", a + 1);
            output.print(" %-4s", atom.getAtomLabel());
            output.print(" %8.3f", atom.getX());
            output.print(" %8.3f", atom.getY());
            output.print(" %8.3f", atom.getZ());
            output.print(" %-6s", atom.getAtomType());

            Residue res = atom.getResidue();
            if(res != prevRes){
                resNumber++;
                prevRes = res;
            }
            output.print(" %5d", resNumber);

            if(residueCount == 1){
                // probably a ligand
                output.print(" %3s", res.getName());
            }else{
                //probably a protein
                output.print(" %3s", res.getName());
                output.print("%-5d", res.getNumber());
            }

            output.print(" %8.3f\n", atom.getBFactor());

            atomNumberHash.put(atom, new Integer(a + 1));
        }


        int bondCount = molecule.getBondCount();
        output.println("@<TRIPOS>BOND");
        for(int b = 0; b < bondCount; b++){
            Bond bond = molecule.getBond(b);
            output.print("%5d", b + 1);
            int i = ((Integer)atomNumberHash.get(bond.getFirstAtom())).intValue();
            int j = ((Integer)atomNumberHash.get(bond.getSecondAtom())).intValue();
            output.print(" %5d", i);
            output.print(" %5d", j);

            int bondOrder = bond.getBondOrder();

            if(bondOrder > 0 && bondOrder <= 3){
                output.print(" %3d", bondOrder);
            }else if(bondOrder == Bond.AromaticBond){
                output.print("  ar");
            }else if(bondOrder == Bond.AmideBond){
                output.print("  am");
            }else{
                output.print("  un");
            }

            output.print("\n");
        }

        atomNumberHash = null;
    }

    /** Write an MDL mol file to the output stream. */
    public static void writeMDLMol(Molecule molecule, FILE output){
        writeMDLMol(molecule, output, true);
    }

    public static void writeMDLMol(Molecule molecule, FILE output, boolean dollars){
	output.println(molecule.getName());
	output.println("AstexViewer");
	output.println("");

	int atomCount = molecule.getAtomCount();
	int bondCount = molecule.getBondCount();

	output.print("%3d", atomCount);
	output.print("%3d", bondCount);
	output.println("  0  0  0  0  0  0  0  0999 V2000");

	// first of all force the ids to be numbered from 1
	for(int a = 0; a < atomCount; a++){
	    Atom atom = molecule.getAtom(a);
	    atom.setId(a+1);
	}

	for(int a = 0; a < atomCount; a++){
	    Atom atom = molecule.getAtom(a);
	    // coordinates
	    output.print("%10.4f", atom.getX());
	    output.print("%10.4f", atom.getY());
	    output.print("%10.4f", atom.getZ());

	    // element symbol
	    output.print(" ");
	    String symbol = atom.getAtomSymbol();
	    output.print(symbol);
	    if(symbol.length() == 1){
		output.print("  ");
	    }else if(symbol.length() == 2){
		output.print(" ");
	    }else if(symbol.length() == 3){
	    }

	    // isotope
	    output.print(" ");
	    output.print("0");
	    output.print(" ");

	    // charge
            //	    output.print(" ");
	    int charge = atom.getCharge();
	    if(charge != 0){
		charge = 4 - charge;
	    }

	    output.print("%2d", charge);

	    // other stuff?
	    output.println("  0  0  0  0  0  0  0  0  0  0");
	}

	for(int b = 0; b < bondCount; b++){
	    Bond bond = molecule.getBond(b);
	    Atom firstAtom = bond.getFirstAtom();
	    Atom secondAtom = bond.getSecondAtom();
	    int order = bond.getBondOrder();

	    output.print("%3d", firstAtom.getId());
	    output.print("%3d", secondAtom.getId());
	    output.print("%3d", order);
	    output.println("  0  0  0  0");
	}
	output.println("M  END");

        if(dollars){
            output.println("$$$$");
        }
    }
	
    /** Write a molecule separator to the output stream. */
    public static void writeMDLMolSeparator(FILE output){
	output.println("$$$$");
    }
}
