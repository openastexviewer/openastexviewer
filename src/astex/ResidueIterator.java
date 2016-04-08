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

/**
 * Iterate over the residues in a scene.
 */
public class ResidueIterator implements Enumeration {
	/** The scene that we will enumerate the residues of. */
	private MoleculeRenderer renderer = null;

	/** Private default constructor. */
	private ResidueIterator(){
	}

	/** The number of residues in the scene. */
	private int residueCount = 0;

	/** The current residue number. */
	private int currentResidue = 0;

	/** The array of residues. */
	private Residue residues[] = null;

	/** The only public constructor. */
	public ResidueIterator(MoleculeRenderer moleculeRenderer){
		renderer = moleculeRenderer;
		int moleculeCount = renderer.getMoleculeCount();

		residueCount = 0;

		for(int m = 0; m < moleculeCount; m++){
			Molecule molecule = renderer.getMolecule(m);
			int chainCount = molecule.getChainCount();
			for(int c = 0; c < chainCount; c++){
				Chain chain = molecule.getChain(c);

				residueCount += chain.getResidueCount();
			}
		}

		if(residueCount > 0){
			residues = new Residue[residueCount];

			currentResidue = 0;

			for(int m = 0; m < moleculeCount; m++){
				Molecule molecule = renderer.getMolecule(m);
				int chainCount = molecule.getChainCount();
				for(int c = 0; c < chainCount; c++){
					Chain chain = molecule.getChain(c);
					int residueCount = chain.getResidueCount();
					for(int r = 0; r < residueCount; r++){
						Residue residue = chain.getResidue(r);
						residues[currentResidue++] = residue;
					}
				}
			}
		}

		currentResidue = 0;
	}

	/** Do we have more elements. */
	public boolean hasMoreElements(){
		return currentResidue < residueCount;
	}

	/** Get the next element. */
	public Object nextElement(){
		if(currentResidue < residueCount){
			return residues[currentResidue++];
		}else{
			return null;
		}
	}

	/** Get the next residue. */
	public Residue getNextResidue(){
		return (Residue)nextElement();
	}
}
