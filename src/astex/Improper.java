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
 * 15-12-99 mjh
 *	created
 */

/**
 * A class for storing information about an improper torsion torsion.
 */
public class Improper {
	/** Default constructor. */
	private Improper(){
		initialise();
	}

	/**
	 * Initialise an improper.
	 *
	 * This method is called to reset a improper that is reused,
	 * so all fields must get initialised here.
	 */
	private void initialise(){
		attributes = 0;
	}

	/** The first atom in the improper. */
	private Atom firstAtom;

	/** The second atom in the improper. */
	private Atom secondAtom;

	/** The third atom in the improper. */
	private Atom thirdAtom;

	/** The fourth atom in the improper. */
	private Atom fourthAtom;

	/** The ideal bond torsion. */
	private float idealImproperAngle = 0.0f;

	/** Has the ideal improper been assigned. */
	private boolean idealImproperAngleAssigned = false;

	/* Various attributes of the bond. */
	
	/** The integer that stores the attributes. */
	private int attributes = 0;

	/**
	 * A method that will create a new Improper.
	 *
	 * This may in the future reuse impropers from a list of those
	 * that are no longer in use.
	 */
	public static Improper create(){
		return new Improper();
	}

	/** Set the first atom in the improper. */
	public void setFirstAtom(Atom atom){
		firstAtom = atom;
	}

	/** Set the second atom in the improper. */
	public void setSecondAtom(Atom atom){
		secondAtom = atom;
	}

	/** Set the third atom in the improper. */
	public void setThirdAtom(Atom atom){
		thirdAtom = atom;
	}

	/** Set the fourth atom in the improper. */
	public void setFourthAtom(Atom atom){
		fourthAtom = atom;
	}

	/** Return the first atom in a improper. */
	public Atom getFirstAtom(){
		return firstAtom;
	}

	/** Return the second atom in a improper. */
	public Atom getSecondAtom(){
		return secondAtom;
	}

	/** Return the third atom in a improper. */
	public Atom getThirdAtom(){
		return thirdAtom;
	}

	/** Return the fourth atom in a improper. */
	public Atom getFourthAtom(){
		return fourthAtom;
	}

	/** Get the specified atom. */
	public Atom getAtom(int index){
		if(index == 0){
			return firstAtom;
		}else if(index == 1){
			return secondAtom;
		}else if(index == 2){
			return thirdAtom;
		}else if(index == 3){
			return fourthAtom;
		}else{
			return null;
		}
	}

	/** Set the ideal improper angle. */
	public void setIdealImproperAngle(double d){
		idealImproperAngle = (float)d;
	}

	/** Get the ideal improper angle. */
	public double getIdealImproperAngle(){
		if(idealImproperAngleAssigned == false){
			idealImproperAngle =
				(float)Point3d.torsion(firstAtom, secondAtom,
									   thirdAtom, fourthAtom);
			idealImproperAngleAssigned = true;
		}

		return idealImproperAngle;
	}
}

