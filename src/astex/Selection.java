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
 * 25-04-01 mjh
 *	created
 */
import java.util.*;

/**
 * Class for evaluating selection expressions.
 */
public class Selection {
    /** Residue names for aminoacids. */
    public static DynamicArray aminoacidNames = new DynamicArray();

    /** Residue names for dna. */
    public static DynamicArray dnaNames = new DynamicArray();

    /** Residue names for solvent. */
    public static DynamicArray solventNames = new DynamicArray();

    /** Residue names for ions. */
    public static DynamicArray ionNames = new DynamicArray();

    static {
        String names[] = {
            "aminoacid", 
            "dna", 
            "solvent", 
            "ions"
        };

        for(int i = 0; i < names.length; i++){
            String value = (String)Settings.get("residue", names[i]);
            if(value == null){
                print.f("no residue definition for " + names[i]);
                continue;
            }
            String residues[] = FILE.split(value, ",");
            if(residues == null) continue;

            for(int r = 0; r < residues.length; r++){
                if("aminoacid".equals(names[i])){
                    aminoacidNames.add(residues[r]);
                }else if("dna".equals(names[i])){
                    dnaNames.add(residues[r]);
                }else if("solvent".equals(names[i])){
                    solventNames.add(residues[r]);
                }else if("ions".equals(names[i])){
                    ionNames.add(residues[r]);
                }
            }
        }
    }

    private static DynamicArray selectionMaskCache = new DynamicArray(10);

    /** Generate a new selection mask. */
    private static byte[] generateSelectionMask(MoleculeRenderer r){
	int atomCount = r.getAtomCount();

	int cacheCount = selectionMaskCache.size();

	//System.out.println("selectionMaskCache size " + cacheCount);

	if(false){
	    if(cacheCount > 0){
		byte mask[] = (byte [])selectionMaskCache.get(cacheCount - 1);
		selectionMaskCache.removeElement(cacheCount - 1);
		
		if(mask.length >= atomCount){
		    System.out.println("reusing cached mask");
		    for(int i = 0; i < atomCount; i++){
			mask[i] = 0;
		    }
		    return mask;
		}
	    }
	}

	// otherwise allocate one that is big enough
	// for a molecule twice as big.
	//return new byte[atomCount * 2];
	return new byte[atomCount];
    }

    /** Greater than attribute. */
    public static final int GT = 0;

    /** Greater equal attribute. */
    public static final int GE = 1;

    /** Less than attribute. */
    public static final int LT = 2;

    /** Less equal attribute. */
    public static final int LE = 3;

    /** Equal attribute. */
    public static final int EQ = 4;

    /** Not equal attribute. */
    public static final int NE = 5;

    private static Atom atomArray[] = null;

    /** Cache for DynamicArray that will return selected atoms. */
    public static DynamicArray dynamicArrayCache =
	new DynamicArray();

    /** The stored selection expression. */
    private static DynamicArray storedExpression = null;

    /** Store the result of a selection expression. */
    public static void store(DynamicArray sel){
	storedExpression = sel;
    }

    /** Retrieve the result of a selection expression. */
    public static DynamicArray retrieve(){
	return storedExpression;
    }

    /** Return a DynamicArray from a selection mask. */
    public static synchronized DynamicArray maskToArray(MoleculeRenderer r,
							byte mask[]){
	int atomCount = r.getAtomCount();

	// figure out how many atoms are needed in the DynamicArray
	// so they can all be allocated in one go.
	if(atomArray == null || atomArray.length < atomCount){
	    atomArray = new Atom[atomCount];
	}

	AtomIterator iterator = r.getAtomIterator();
	int count = 0;
	int atomArrayCount = 0;

	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    if(mask[count++] > 0){
		atomArray[atomArrayCount++] = atom;
	    }
	}

	if(count != atomCount){
	    System.out.println("Selection.maskToArray() count != atomCount");
	}

	int arrayCacheCount = dynamicArrayCache.size();

	DynamicArray selected = null;

	//System.out.println("dynamicArrayCache.size() " + arrayCacheCount);

	//System.out.println("searching for array of size " + atomArrayCount);

	if(false){
	    //for(int i = arrayCacheCount - 1; i >= 0; i--){
	    for(int i = 0; i < arrayCacheCount; i++){
		DynamicArray da = (DynamicArray)dynamicArrayCache.get(i);
		Object daArray[] = da.getArray();
	    
		//if(daArray != null){
		//System.out.println(i + " daArray.length " + daArray.length);
		//}

		if(daArray != null && daArray.length >= atomArrayCount){
		    da.removeAllElements();
		    dynamicArrayCache.removeElement(i);
		    selected = da;
		    //System.out.println("reusing dynamicArrayCache " + i);
		    break;
		}
	    }
	}

	if(selected == null){
	    selected = new DynamicArray(atomArrayCount*2);
	    //dynamicArrayCache.add(selected);
	}

	if(selected.size() != 0){
	    System.out.println("Selection.maskToArray() initial size != 0");
	}

	for(int i = 0; i < atomArrayCount; i++){
	    selected.add(atomArray[i]);
	}

	//selectionMaskCache.add(mask);

	return selected;
    }

    /** Return a mask from a DynamicArray. */
    public static byte[] arrayToMask(MoleculeRenderer r,
				     DynamicArray selectedAtoms){
	int count = 0;
	AtomIterator iterator = r.getAtomIterator();
	byte mask[] = generateSelectionMask(r);
	int selectedAtomCount = selectedAtoms.size();

	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
            atom.setTemporarilySelected(false);
        }

	for(int i = 0 ; i < selectedAtomCount; i++){
	    Atom a = (Atom)selectedAtoms.get(i);
	    a.setTemporarilySelected(true);
	}
	
	iterator = r.getAtomIterator();

	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    if(atom.isTemporarilySelected()){
		mask[count] = 1;
	    }else{
		mask[count] = 0;
	    }

	    count++;
	}
	
	for(int i = 0 ; i < selectedAtomCount; i++){
	    Atom a = (Atom)selectedAtoms.get(i);
	    a.setTemporarilySelected(false);
	}
	
	return mask;
    }

    /** Select all the atoms. */
    public static byte[] all(MoleculeRenderer r){
	byte mask[] = generateSelectionMask(r);
	int atomCount = mask.length;

	for(int i = 0; i < atomCount; i++){
	    mask[i] = 1;
	}

	return mask;
    }

    /** Select all the atoms. */
    public static byte[] none(MoleculeRenderer r){
	byte mask[] = generateSelectionMask(r);
	int atomCount = mask.length;

	for(int i = 0; i < atomCount; i++){
	    mask[i] = 0;
	}

	return mask;
    }

    /** Select on the basis of an attribute. */
    public static byte[] attribute(MoleculeRenderer r, int attribute, int operator,
				   double value){
	byte[] mask = generateSelectionMask(r);
	AtomIterator iterator = r.getAtomIterator();
	int count = 0;

	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    double d = atom.getAttribute(attribute);

	    switch(operator){
	    case GT: mask[count] = (byte)((d > value) ? 1 : 0); break;
	    case GE: mask[count] = (byte)((d >= value) ? 1 : 0); break;
	    case LT: mask[count] = (byte)((d < value) ? 1 : 0); break;
	    case LE: mask[count] = (byte)((d <= value) ? 1 : 0); break;
	    case EQ: mask[count] = (byte)((d == value) ? 1 : 0); break;
	    case NE: mask[count] = (byte)((d != value) ? 1 : 0); break;
	    default:
		System.out.println("attribute: unknown operator " + operator);
		break;
	    }
	    count++;
	}

	return mask;
    }

    /** Select a set of atoms on the basis of ids. */
    public static byte[] residue2(MoleculeRenderer r, Vector ids){
	int minId = 1000000;
	int maxId = -1000000;
	int idCount = ids.size();

	for(int i = 0; i < idCount; i++){
	    int range[] = (int[])ids.elementAt(i);
	    if(range[0] < minId){
		minId = range[0];
	    }
	    if(range[1] > maxId){
		maxId = range[1];
	    }
	}

	byte[] mask = generateSelectionMask(r);
	AtomIterator iterator = r.getAtomIterator();
	int count = 0;

	// this can be done much more efficiently
	// by going through residues and setting blocks
	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    Residue res = atom.getResidue();
	    int number = res.getNumber();
	    if(number >= minId && number <= maxId){
		// it could match
		for(int i = 0; i < idCount; i++){
		    int range[] = (int[])ids.elementAt(i);
		    if(number >= range[0] &&
		       number <= range[1]){
			mask[count] = 1;
			break;
		    }
		}
	    }

	    count++;
	}

	return mask;
    }

    /** Select a set of atoms on the basis of ids. */
    public static byte[] residue(MoleculeRenderer r, Vector ids){
	int minId = 1000000;
	int maxId = -1000000;
	int idCount = ids.size();

	for(int i = 0; i < idCount; i++){
	    int range[] = (int[])ids.elementAt(i);
	    if(range[0] < minId){
		minId = range[0];
	    }
	    if(range[1] > maxId){
		maxId = range[1];
	    }
	}

	byte[] mask = generateSelectionMask(r);
	int count = 0;

	// this can be done much more efficiently
	// by going through residues and setting blocks
	for(int m = 0; m < r.getMoleculeCount(); m++){
	    Molecule mol = r.getMolecule(m);
	    int chainCount = mol.getChainCount();
	    //Log.info("molecule " + mol.getName());
	    //Log.info("chainCount %d", chainCount);

	    for(int c = 0; c < chainCount; c++){
		Chain chain = mol.getChain(c);
		int resCount = chain.getResidueCount();

		//Log.info("chain " + chain.getName());
		//Log.info("resCount %d", resCount);

		for(int rid = 0; rid < resCount; rid++){
		    Residue res = chain.getResidue(rid);
		    int number = res.getNumber();
		    //if(number == 255){
		    //	Log.info("residue " + res);
		    //	Log.info("residue atomCount %d", res.getAtomCount());
		    //}
		    int match = 0;
		    if(number >= minId && number <= maxId){
			// it could match
			for(int i = 0; i < idCount; i++){
			    int range[] = (int[])ids.elementAt(i);
			    if(number >= range[0] &&
			       number <= range[1]){
				match = 1;
				break;
			    }
			}
		    }

		    int residueAtomCount = res.getAtomCount();

		    if(match == 1){
			for(int a = 0; a < residueAtomCount; a++){
			    mask[count] = (byte)match;
			    count++;
			}
		    }else{
			count += residueAtomCount;
		    }
		}
	    }
	}

	return mask;
    }

    /** Select a set of atoms on the basis of ids. */
    public static byte[] modulo(MoleculeRenderer r, int n){
	byte[] mask = generateSelectionMask(r);
	int count = 0;

	// this can be done much more efficiently
	// by going through residues and setting blocks
	for(int m = 0; m < r.getMoleculeCount(); m++){
	    Molecule mol = r.getMolecule(m);
	    int chainCount = mol.getChainCount();
	    //Log.info("molecule " + mol.getName());
	    //Log.info("chainCount %d", chainCount);

	    for(int c = 0; c < chainCount; c++){
		Chain chain = mol.getChain(c);
		int resCount = chain.getResidueCount();

		//Log.info("chain " + chain.getName());
		//Log.info("resCount %d", resCount);

		for(int rid = 0; rid < resCount; rid++){
		    Residue res = chain.getResidue(rid);
		    int number = res.getNumber();

		    int match = 0;
		    if(number % n == 0){
			match = 1;
		    }

		    int residueAtomCount = res.getAtomCount();

		    if(match == 1){
			for(int a = 0; a < residueAtomCount; a++){
			    mask[count] = (byte)match;
			    count++;
			}
		    }else{
			count += residueAtomCount;
		    }
		}
	    }
	}

	return mask;
    }

    /** Select a set of atoms on the basis of hierarchy like PyMol. */
    /*
    public static byte[] hierarchy(MoleculeRenderer r, Vector ids){
	int idCount = ids.size();

	byte[] mask = generateSelectionMask(r);
	int count = 0;

	Stack selectionStack = new Stack();

	for(int i = 0; i < idCount; i++){
	}
    }
    */

    /** Select a set of atoms on the basis of ids. */
    public static byte[] composite(MoleculeRenderer r, Vector ids){
	int idCount = ids.size();

	byte[] mask = generateSelectionMask(r);
	int count = 0;

	Stack selectionStack = new Stack();

	for(int i = 0; i < idCount; i++){
	    StringBuffer chainBuffer = new StringBuffer();
	    StringBuffer residueBuffer = new StringBuffer();
	    StringBuffer insertionBuffer = new StringBuffer();
	    String compositeString = (String)ids.elementAt(i);
	    int j;
	    for(j = 0; j < compositeString.length(); j++){
		char c = compositeString.charAt(j);
		if(c >= 'A' && c <= 'Z'){
		    chainBuffer.append(c);
		}else{
		    break;
		}
	    }
	    for(/* nothing */; j < compositeString.length(); j++){
		char c = compositeString.charAt(j);
		if(c >= '0' && c <= '9'){
		    residueBuffer.append(c);
		}else{
		    break;
		}
	    }
	    for(/* nothing */; j < compositeString.length(); j++){
		char c = compositeString.charAt(j);
		if(c >= 'A' && c <= 'Z'){
		    insertionBuffer.append(c);
		}else{
		    break;
		}
	    }

	    //System.out.println("chainBuffer " + chainBuffer);
	    //System.out.println("residueBuffer " + residueBuffer);
	    //System.out.println("insertionBuffer " + insertionBuffer);

	    if(residueBuffer.length() == 0){
		return mask;
	    }

	    byte chainMask[] = null;
	    byte residueMask[] = null;
	    byte insertionMask[] = null;
	    Vector v = new Vector();

	    if(chainBuffer.length() > 0){
		v.removeAllElements();
		v.addElement(chainBuffer.toString());
		chainMask = chain(r, v);
	    }

	    if(residueBuffer.length() > 0){
		int res[] = new int[2];
		int resValue = FILE.readInteger(residueBuffer.toString());
		res[0] = resValue; res[1] = resValue;
		v.removeAllElements();
		v.addElement(res);
		residueMask = residue(r, v);
	    }

	    if(insertionBuffer.length() > 0){
		insertionMask = insertion(r, insertionBuffer.toString());
	    }

	    byte comp[] = null;

	    if(chainMask != null){
		comp = and(chainMask, residueMask);
	    }

	    if(comp == null){
		comp = residueMask;
	    }

	    if(insertionMask != null){
		comp = and(comp, insertionMask);
	    }

	    return comp;
	}
	
	return mask;
    }

    /** Select a set of atoms on the basis of ids. */
    public static byte[] sequential(MoleculeRenderer r, Vector ids){
	int minId = 1000000;
	int maxId = -1000000;
	int idCount = ids.size();

	for(int i = 0; i < idCount; i++){
	    int range[] = (int[])ids.elementAt(i);
	    if(range[0] < minId){
		minId = range[0];
	    }
	    if(range[1] > maxId){
		maxId = range[1];
	    }
	}

	byte[] mask = generateSelectionMask(r);
	int count = 0;

	// this can be done much more efficiently
	// by going through residues and setting blocks
	for(int m = 0; m < r.getMoleculeCount(); m++){
	    Molecule mol = r.getMolecule(m);
	    int chainCount = mol.getChainCount();
	    for(int c = 0; c < chainCount; c++){
		Chain chain = mol.getChain(c);
		int resCount = chain.getResidueCount();
		for(int rid = 0; rid < resCount; rid++){
		    Residue res = chain.getResidue(rid);
		    int number = res.getSequentialNumber();
		    int match = 0;
		    if(number >= minId && number <= maxId){
			// it could match
			for(int i = 0; i < idCount; i++){
			    int range[] = (int[])ids.elementAt(i);
			    if(number >= range[0] &&
			       number <= range[1]){
				match = 1;
				break;
			    }
			}
		    }

		    int residueAtomCount = res.getAtomCount();

		    if(match == 1){
			for(int a = 0; a < residueAtomCount; a++){
			    mask[count] = (byte)match;
			    count++;
			}
		    }else{
			count += residueAtomCount;
		    }
		}
	    }
	}

	return mask;
    }

    /** Select a set of atoms on the basis of insertion code. */
    public static byte[] insertion(MoleculeRenderer r, String insertionCode){
	char icode = insertionCode.charAt(0);

	byte[] mask = generateSelectionMask(r);
	int count = 0;

	// this can be done much more efficiently
	// by going through residues and setting blocks
	for(int m = 0; m < r.getMoleculeCount(); m++){
	    Molecule mol = r.getMolecule(m);
	    int chainCount = mol.getChainCount();
	    for(int c = 0; c < chainCount; c++){
		Chain chain = mol.getChain(c);
		int resCount = chain.getResidueCount();
		for(int rid = 0; rid < resCount; rid++){
		    Residue res = chain.getResidue(rid);
		    char thisInsertionCode = res.getInsertionCode();
		    int match = 0;
		    if(thisInsertionCode == icode){
			match = 1;
		    }

		    int residueAtomCount = res.getAtomCount();

		    if(match == 1){
			// array should be cleared to 0's
			for(int a = 0; a < residueAtomCount; a++){
			    mask[count] = (byte)match;
			    count++;
			}
		    }else{
			count += residueAtomCount;
		    }
		}
	    }
	}

	return mask;
    }

    /** Select a set of atoms on the basis of molecule name. */
    public static byte[] molecule(MoleculeRenderer r, Vector ids){
	int idCount = ids.size();

	byte[] mask = generateSelectionMask(r);
	int count = 0;

	// this can be done much more efficiently
	int moleculeCount = r.getMoleculeCount();
	for(int m = 0; m < moleculeCount; m++){
	    Molecule mol = r.getMolecule(m);
	    byte matched = 0;
	    for(int i = 0; i < idCount; i++){
                String id = (String)ids.elementAt(i);

                if(id.startsWith("#")){
                    int molNumber = Integer.parseInt(id.substring(1));
                    if(molNumber == m){
                        matched = 1;
                        break;
                    }
                }else if(match.matches((String)ids.elementAt(i), mol.getName())){
		    matched = 1;
		    break;
		}
	    }

	    int atomCount = mol.getAtomCount();

	    if(matched == 1){
		for(int a = 0; a < atomCount; a++){
		    mask[count++] = 1;
		}
	    }else{
		count += atomCount;
	    }
	}

	return mask;
    }

    /** Select a set of atoms on the basis of molecule name. */
    public static byte[] moleculeExact(MoleculeRenderer r, Vector ids){
	int idCount = ids.size();

	byte[] mask = generateSelectionMask(r);
	int count = 0;

	// this can be done much more efficiently
	int moleculeCount = r.getMoleculeCount();
	for(int m = 0; m < moleculeCount; m++){
	    Molecule mol = r.getMolecule(m);
	    byte matched = 0;
	    for(int i = 0; i < idCount; i++){
		if(((String)ids.elementAt(i)).equals(mol.getName())){
		    matched = 1;
		    break;
		}
	    }

	    int atomCount = mol.getAtomCount();

	    if(matched == 1){
		for(int a = 0; a < atomCount; a++){
		    mask[count++] = 1;
		}
	    }else{
		count += atomCount;
	    }
	}

	return mask;
    }

    /** Select a set of atoms on the basis of residue names. */
    public static byte[] byresidue(MoleculeRenderer r, byte mask[]){

	AtomIterator iterator = r.getAtomIterator();
	int count = 0;

	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    if(mask[count] == 0){
		atom.setTemporarilySelected(false);
	    }else{
		atom.setTemporarilySelected(true);
	    }

	    count++;
	}

	count = 0;
	int moleculeCount = r.getMoleculeCount();

	for(int m = 0; m < moleculeCount; m++){
	    Molecule mol = r.getMolecule(m);
	    int chainCount = mol.getChainCount();

	    for(int c = 0; c < chainCount; c++){
		Chain chain = mol.getChain(c);
		int residueCount = chain.getResidueCount();

		for(int rr = 0; rr < residueCount; rr++){
		    Residue residue = chain.getResidue(rr);
		    int atomCount = residue.getAtomCount();
		    int residueSelected = 0;

		    for(int a = 0; a < atomCount; a++){
			Atom atom = residue.getAtom(a);
			if(atom.isTemporarilySelected()){
			    residueSelected = 1;
			    break;
			}
		    }

		    for(int a = 0; a < atomCount; a++){
			Atom atom = residue.getAtom(a);
			mask[count++] = (byte)residueSelected;
		    }
		}
	    }
	}

	return mask;
    }

    /** Select a set of atoms on the basis of residue names. */
    public static byte[] bonded(MoleculeRenderer r, byte mask[]){

        AtomIterator iterator = r.getAtomIterator();

	int count = 0;

	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
            atom.setTemporarilySelected(false);
        }

        iterator = r.getAtomIterator();

	count = 0;

	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
            if(mask[count] > 0){
                int bondCount = atom.getBondCount();

                for(int b = 0; b < bondCount; b++){
                    Atom bondedAtom = atom.getBondedAtom(b);
                    bondedAtom.setTemporarilySelected(true);
                }
            }

            count++;
        }

        iterator = r.getAtomIterator();

	count = 0;

	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();

            if(atom.isTemporarilySelected()){
                mask[count] = 1;
            }

            count++;
        }

	return mask;
    }

    /** Select a set of atoms on the basis of residue names. */
    public static byte[] name(MoleculeRenderer r, Vector ids){
	int idCount = ids.size();

	byte[] mask = generateSelectionMask(r);
	AtomIterator iterator = r.getAtomIterator();
	int count = 0;

	// this can be done much more efficiently
	// by going through residues and setting blocks
	for(int m = 0; m < r.getMoleculeCount(); m++){
	    Molecule mol = r.getMolecule(m);
	    int chainCount = mol.getChainCount();
	    for(int c = 0; c < chainCount; c++){
		Chain chain = mol.getChain(c);
		int resCount = chain.getResidueCount();
		for(int rid = 0; rid < resCount; rid++){
		    Residue res = chain.getResidue(rid);
		    String name = res.getName();
		    int matched = 0;
		    // it could match
		    for(int i = 0; i < idCount; i++){
			if(match.matches((String)ids.elementAt(i), name)){
			    matched = 1;
			    break;
			}
		    }

		    int residueAtomCount = res.getAtomCount();

		    if(matched == 1){
			for(int a = 0; a < residueAtomCount; a++){
			    mask[count] = 1;
			    count++;
			}
		    }else{
			count += residueAtomCount;
		    }
		}
	    }
	}

	return mask;
    }

    /** Select a set of atoms on the basis of residue names. */
    public static byte[] chain(MoleculeRenderer r, Vector ids){
	int idCount = ids.size();

	byte[] mask = generateSelectionMask(r);
	AtomIterator iterator = r.getAtomIterator();
	int count = 0;

	// this can be done much more efficiently
	// by going through residues and setting blocks
	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    Residue res = atom.getResidue();
	    Chain chain = res.getParent();
	    String name = chain.getName();
	    for(int i = 0; i < idCount; i++){
		String chainId = (String)ids.elementAt(i);
		if(chainId.equals("_")){
		    chainId = " ";
		}
		if(match.matches(chainId, name)){
		    mask[count] = 1;
		    break;
		}
	    }
	    count++;
	}

	return mask;
    }

    /** Select a set of atoms on the basis of atom ids. */
    public static byte[] atom(MoleculeRenderer r, Vector ids){
	int idCount = ids.size();

	byte[] mask = generateSelectionMask(r);
	AtomIterator iterator = r.getAtomIterator();
	int count = 0;

	// this can be done much more efficiently
	// by going through residues and setting blocks
	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    String name = atom.getAtomLabel();
	    for(int i = 0; i < idCount; i++){
		if(match.matches((String)ids.elementAt(i), name)){
		    mask[count] = 1;
		    break;
		}
	    }
	    count++;
	}

	return mask;
    }

    /** Select a set of atoms in a group. */
    public static byte[] group(MoleculeRenderer r, Hashtable group){
	byte[] mask = generateSelectionMask(r);

	AtomIterator iterator = r.getAtomIterator();

	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
            atom.setTemporarilySelected(false);
        }

        Enumeration groupEnum = group.keys();

        while(groupEnum.hasMoreElements()){
            Atom atom = (Atom)groupEnum.nextElement();
            atom.setTemporarilySelected(true);
        }

	int count = 0;

	iterator = r.getAtomIterator();

	// this can be done much more efficiently
	// by going through residues and setting blocks
	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
            if(atom.isTemporarilySelected()){
                mask[count] = 1;
            }else{
                mask[count] = 0;
	    }
	    count++;
	}

	return mask;
    }

    /** Select a set of atoms on the basis of ids. */
    public static byte[] id(MoleculeRenderer r, Vector ids){
	int minId = 1000000;
	int maxId = -1000000;
	int idCount = ids.size();

	for(int i = 0; i < idCount; i++){
	    int range[] = (int[])ids.elementAt(i);
	    if(range[0] < minId){
		minId = range[0];
	    }
	    if(range[1] > maxId){
		maxId = range[1];
	    }
	}

	byte[] mask = generateSelectionMask(r);
	AtomIterator iterator = r.getAtomIterator();
	int count = 0;

	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    int number = atom.getId();
	    if(number >= minId && number <= maxId){
		// it could match
		for(int i = 0; i < idCount; i++){
		    int range[] = (int[])ids.elementAt(i);
		    if(number >= range[0] &&
		       number <= range[1]){
			mask[count] = 1;
			break;
		    }
		}
	    }

	    count++;
	}

	return mask;
    }

    /** Select a set of atoms on the basis of elements. */
    public static byte[] element(MoleculeRenderer r, Vector ids){
	int minId = 1000000;
	int maxId = -1000000;
	int idCount = ids.size();

	for(int i = 0; i < idCount; i++){
	    int range[] = (int[])ids.elementAt(i);
	    if(range[0] < minId){
		minId = range[0];
	    }
	    if(range[1] > maxId){
		maxId = range[1];
	    }
	}

	byte[] mask = generateSelectionMask(r);
	AtomIterator iterator = r.getAtomIterator();
	int count = 0;

	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    int number = atom.getElement();
	    if(number >= minId && number <= maxId){
		// it could match
		for(int i = 0; i < idCount; i++){
		    int range[] = (int[])ids.elementAt(i);
		    if(number >= range[0] &&
		       number <= range[1]){
			mask[count] = 1;
			break;
		    }
		}
	    }

	    count++;
	}

	return mask;
    }

    /** Return the current selection. */
    public static byte[] current(MoleculeRenderer r){
	byte[] mask = generateSelectionMask(r);
	AtomIterator iterator = r.getAtomIterator();
	int count = 0;

	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    //System.out.println("atom.attributes " + (atom.attributes & Atom.Selected));
	    if(atom.isSelected()){
		mask[count] = 1;
	    }else{
		mask[count] = 0;
	    }
	    count++;
	}

	return mask;
    }

    /** Return atoms that have the specified property. */
    public static byte[] property(MoleculeRenderer r, int property){
	byte[] mask = generateSelectionMask(r);
	AtomIterator iterator = r.getAtomIterator();
	int count = 0;

	// this can be done much more efficiently
	// by going through residues and setting blocks
	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    if((atom.attributes & property) != 0){
		mask[count] = 1;
	    }else{
		mask[count] = 0;
	    }
	    count++;
	}

	return mask;
    }

    /**
     * Return the atoms that are actively displayed.
     * Also check to see if the molecule that contains
     * the atom is displayed.
     *
     * Used to check the molecule display status as well
     * as the atom, but this was arguably wrong as it
     * could result in atoms in turned off molecueles
     * getting changed unexpectedly.
     */
    public static byte[] displayed(MoleculeRenderer r){
	byte[] mask = generateSelectionMask(r);
	AtomIterator iterator = r.getAtomIterator();
	int count = 0;

	// this can be done much more efficiently
	// by going through residues and setting blocks
	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    Molecule mol = atom.getMolecule();

	    //if(mol.getDisplayed()){
		if(atom.isDisplayed()){
		    mask[count] = 1;
		}else{
		    mask[count] = 0;
		}
		//}else{
		//mask[count] = 0;
		//}

	    count++;
	}

	return mask;
    }

    /** Return the current selection. */
    public static byte[] labelled(MoleculeRenderer r){
	byte[] mask = generateSelectionMask(r);
	AtomIterator iterator = r.getAtomIterator();
	int count = 0;

	// this can be done much more efficiently
	// by going through residues and setting blocks
	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    if(atom.isLabelled()){
		mask[count] = 1;
	    }else{
		mask[count] = 0;
	    }
	    count++;
	}

	return mask;
    }

    /**
     * Return the default selection.
     * If any atoms are selected they are returned,
     * otherwise all atoms are returned.
     */
    public static byte[] defaultSelection(MoleculeRenderer r){
	byte[] mask = generateSelectionMask(r);
	AtomIterator iterator = r.getAtomIterator();
	int count = 0;
	int selected = 0;

	// this can be done much more efficiently
	// by going through residues and setting blocks
	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    if(atom.isSelected()){
		selected++;
		mask[count] = 1;
	    }else{
		mask[count] = 0;
	    }
	    count++;
	}

	if(selected == 0){
	    // nothing selected so return all.
	    int maskCount = mask.length;
	    for(int i = 0; i < maskCount; i++){
		mask[i] = 1;
	    }
	}

	return mask;
    }

    /** Return the current selection. */
    public static byte[] wide(MoleculeRenderer r){
	byte[] mask = generateSelectionMask(r);
	AtomIterator iterator = r.getAtomIterator();
	int count = 0;

	// this can be done much more efficiently
	// by going through residues and setting blocks
	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    if(atom.isWide()){
		mask[count] = 1;
	    }else{
		mask[count] = 0;
	    }
	    count++;
	}

	return mask;
    }

    /** Return a set of atoms withinin a sphere. */
    public static byte[] sphere(MoleculeRenderer r, double rad,
				double x, double y, double z){
	byte[] mask = generateSelectionMask(r);
	AtomIterator iterator = r.getAtomIterator();
	int count = 0;
	double radSq = rad * rad;
	Point3d p = new Point3d(x, y, z);

	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    if(atom.distanceSq(p) < radSq){
		mask[count] = 1;
	    }else{
		mask[count] = 0;
	    }
	    count++;
	}

	return mask;
    }

    /** Return a set of atoms withinin a sphere. */
    public static byte[] sphere(MoleculeRenderer r, double rad, byte sphereMask[]){
	DynamicArray sphereSelection = maskToArray(r, sphereMask);
	int sphereSelectionCount = sphereSelection.size();
	byte[] mask = generateSelectionMask(r);
	AtomIterator iterator = r.getAtomIterator();
	int count = 0;
	double radSq = rad * rad;

	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    int inside = 0;
	    for(int i = 0; i < sphereSelectionCount; i++){
		Atom sphereAtom = (Atom)sphereSelection.get(i);
		if(atom.distanceSq(sphereAtom) < radSq){
		    inside = 1;
		    break;
		}
	    }

	    if(inside > 0){
		mask[count] = 1;
	    }else{
		mask[count] = 0;
	    }
	    
	    count++;
	}
	 
	return mask;
    }

    /** Return a set of atoms withinin a tolerance of sum of vdw radii. */
    public static byte[] contact(MoleculeRenderer r, double rad, byte sphereMask[]){
	DynamicArray sphereSelection = maskToArray(r, sphereMask);
	int sphereSelectionCount = sphereSelection.size();
	byte[] mask = generateSelectionMask(r);
	AtomIterator iterator = r.getAtomIterator();
	int count = 0;

	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    double arad = atom.getVDWRadius() + rad;

	    int inside = 0;

	    for(int i = 0; i < sphereSelectionCount; i++){
		Atom sphereAtom = (Atom)sphereSelection.get(i);
		double srad = sphereAtom.getVDWRadius();
		double radSq = arad + srad;
		radSq *= radSq;

		if(atom.distanceSq(sphereAtom) < radSq){
		    inside = 1;
		    break;
		}
	    }

	    if(inside > 0){
		mask[count] = 1;
	    }else{
		mask[count] = 0;
	    }
	    
	    count++;
	}
	 
	// save the mask
	//selectionMaskCache.add(sphereMask);

	return mask;
    }

    /** Return a set of atoms in a connected graph. */
    public static byte[] graph(MoleculeRenderer r, byte graphMask[]){
	DynamicArray graphSelection = maskToArray(r, graphMask);
	int graphSelectionCount = graphSelection.size();
	byte[] mask = generateSelectionMask(r);
	int count = 0;

	AtomIterator iterator = r.getAtomIterator();
	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    atom.setTemporarilySelected(false);
	}

	for(int i = 0; i < graphSelectionCount; i++){
	    Atom atom = (Atom)graphSelection.get(i);
	    if(atom.isTemporarilySelected() == false){
		propagateGraph(atom);
	    }
	}

	iterator = r.getAtomIterator();

	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    if(atom.isTemporarilySelected()){
		mask[count] = 1;
	    }else{
		mask[count] = 0;
	    }

	    count++;
	    
	    atom.setTemporarilySelected(false);
	}
	 
	// save the mask
	selectionMaskCache.add(graphMask);

	return mask;
    }

    /** Propagate the graph selection. */
    private static void propagateGraph(Atom a){
	int bondCount = a.getBondCount();
	a.setTemporarilySelected(true);

	for(int i = 0; i < bondCount; i++){
	    Atom otherAtom = a.getBondedAtom(i);
	    if(otherAtom.isTemporarilySelected() == false){
		propagateGraph(otherAtom);
	    }
	}
    }

    /** Composite specification. */
    public static byte[]
	compositeSelection(MoleculeRenderer r, Vector molecules,
			   Vector chains, Vector residues, Vector atoms){
	byte mask1[] = molecule(r, molecules);
	byte mask2[] = chain(r, molecules);
	mask1 = and(mask1, mask2);
	mask2 = residue(r, residues);
	mask1 = and(mask1, mask2);
	mask2 = atom(r, atoms);
	return and(mask1, mask2);
    }

    /** And two selection masks together. */
    public static byte[] and(byte mask1[], byte mask2[]){
	int count = Math.min(mask1.length, mask2.length);

	for(int i = 0; i < count; i++){
	    if(mask1[i] > 0 && mask2[i] > 0){
		mask1[i] = 1;
	    }else{
		mask1[i] = 0;
	    }
	}

	// save the mask
	//selectionMaskCache.add(mask2);

	// mask2 can just be garbage collected
	return mask1;
    }

    /** Or two selection masks together. */
    public static byte[] or(byte mask1[], byte mask2[]){
	int count = Math.min(mask1.length, mask2.length);

	for(int i = 0; i < count; i++){
	    if(mask1[i] > 0 || mask2[i] > 0){
		mask1[i] = 1;
	    }else{
		mask1[i] = 0;
	    }
	}

	// save the mask
	selectionMaskCache.add(mask2);

	// mask2 can just be garbage collected
	return mask1;
    }

    /** Not a selection mask. */
    public static byte[] not(byte mask1[]){
	int count = mask1.length;

	for(int i = 0; i < count; i++){
	    if(mask1[i] == 0){
		mask1[i] = 1;
	    }else{
		mask1[i] = 0;
	    }
	}

	return mask1;
    }

    /** Evaluate a builtin expression against residue names. */
    public static byte[] builtin(MoleculeRenderer r, DynamicArray names){
	Vector ids = new Vector(names.size());
	for(int i = 0; i < names.size(); i++){
	    ids.addElement(names.get(i));
	}

	return name(r, ids);
    }

    /** Evaluate a builtin expression atom names. */
    public static byte[] builtin2(MoleculeRenderer r, DynamicArray names){
	Vector ids = new Vector(names.size());
	for(int i = 0; i < names.size(); i++){
	    ids.addElement(names.get(i));
	}

	return atom(r, ids);
    }

    /** Evaluate a builtin expression atom names. */
    public static byte[] builtin3(MoleculeRenderer r, DynamicArray names){
	Vector ids = new Vector(names.size());
	for(int i = 0; i < names.size(); i++){
	    ids.addElement(names.get(i));
	}

	return name(r, ids);
    }

    /** Aminoacid expression. */
    public static byte[] aminoacid(MoleculeRenderer r){
	return builtin(r, aminoacidNames);
    }

    /** Solvent expression. */
    public static byte[] solvent(MoleculeRenderer r){
	return builtin(r, solventNames);
    }

    /** DNA expression. */
    public static byte[] dna(MoleculeRenderer r){
	return builtin(r, dnaNames);
    }

    /** Ions expression. */
    public static byte[] ions(MoleculeRenderer r){
	return builtin3(r, ionNames);
    }
}
