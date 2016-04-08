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
 * 11-12-99 mjh
 *	created
 */
import java.util.*;

public class AtomIterator implements Enumeration {
	/** The scene that we will enumerate the atoms of. */
	private MoleculeRenderer renderer = null;

	/** Private default constructor. */
	private AtomIterator(){
	}

	/** The number of molecules. */
	private int moleculeCount = 0;

	/** The current molecule. */
	private Molecule currentMolecule = null;

	/** The current molecule number. */
	private int currentMoleculeIndex = 0;

	/** The current atom. */
	private int currentAtomIndex = 0;

	/** The atom count for the current molecule. */
	private int currentMoleculeAtomCount = 0;

	/** The only public constructor. */
	public AtomIterator(MoleculeRenderer moleculeRenderer){
		renderer = moleculeRenderer;
		moleculeCount = renderer.getMoleculeCount();

		if(moleculeCount > 0){
			currentMolecule = renderer.getMolecule(0);
			currentMoleculeAtomCount =
				currentMolecule.getAtomCount();
		}
	}

	/** Do we have more elements. */
	public boolean hasMoreElements(){
		if(currentMolecule == null){
			return false;
		}
		
		//System.out.println("currentAtomIndex " + currentAtomIndex);
		//System.out.println("currentMoleculeAtomCount " +
		//				   currentMoleculeAtomCount);

		if(currentAtomIndex < currentMoleculeAtomCount){
			return true;
		}else{
			currentAtomIndex = 0;
			currentMolecule = null;

			while(++currentMoleculeIndex < moleculeCount){
				currentMolecule =
					renderer.getMolecule(currentMoleculeIndex);

				if(currentMolecule.getAtomCount() > 0){
					break;
				}else{
					currentMolecule = null;
				}
			}

			if(currentMolecule != null){
				currentMoleculeAtomCount =
					currentMolecule.getAtomCount();
				return true;
			}
												 
		}
		
		return false;
	}

	/** Get the next element. */
	public Object nextElement(){
		if(hasMoreElements()){
			return currentMolecule.getAtom(currentAtomIndex++);
		}else{
			return null;
		}
	}

	/** Get the next atom. */
	public Atom getNextAtom(){
		return (Atom)nextElement();
	}
}
