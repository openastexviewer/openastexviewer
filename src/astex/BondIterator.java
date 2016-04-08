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

public class BondIterator implements Enumeration {
	/** The scene that we will enumerate the bonds of. */
	private MoleculeRenderer renderer = null;

	/** Private default constructor. */
	private BondIterator(){
	}

	/** The number of molecules. */
	private int moleculeCount = 0;

	/** The current molecule. */
	private Molecule currentMolecule = null;

	/** The current molecule number. */
	private int currentMoleculeIndex = 0;

	/** The current bond. */
	private int currentBondIndex = 0;

	/** The bond count for the current molecule. */
	private int currentMoleculeBondCount = 0;

	/** The only public constructor. */
	public BondIterator(MoleculeRenderer moleculeRenderer){
		renderer = moleculeRenderer;
		moleculeCount = renderer.getMoleculeCount();

		if(moleculeCount > 0){
			currentMolecule = renderer.getMolecule(0);
			currentMoleculeBondCount =
				currentMolecule.getBondCount();
		}
	}

	/** Do we have more elements. */
	public boolean hasMoreElements(){
		if(currentMolecule == null){
			return false;
		}
		
		//System.out.println("currentBondIndex " + currentBondIndex);
		//System.out.println("currentMoleculeBondCount " +
		//				   currentMoleculeBondCount);

		if(currentBondIndex < currentMoleculeBondCount){
			return true;
		}else{
			currentBondIndex = 0;
			currentMolecule = null;

			while(++currentMoleculeIndex < moleculeCount){
				currentMolecule =
					renderer.getMolecule(currentMoleculeIndex);

				if(currentMolecule.getBondCount() > 0){
					break;
				}else{
					currentMolecule = null;
				}
			}

			if(currentMolecule != null){
				currentMoleculeBondCount =
					currentMolecule.getBondCount();
				return true;
			}
												 
		}
		
		return false;
	}

	/** Get the next element. */
	public Object nextElement(){
		if(hasMoreElements()){
			return currentMolecule.getBond(currentBondIndex++);
		}else{
			return null;
		}
	}

	/** Get the next bond. */
	public Bond getNextBond(){
		return (Bond)nextElement();
	}
}
