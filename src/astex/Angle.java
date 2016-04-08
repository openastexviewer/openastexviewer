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
 * 14-12-99 mjh
 *	created
 */

/**
 * A class for storing information about a bond angle.
 */
public class Angle {
	/** Default constructor. */
	private Angle(){
		initialise();
	}

	/**
	 * Initialise a angle.
	 *
	 * This method is called to reset a angle that is reused,
	 * so all fields must get initialised here.
	 */
	private void initialise(){
		attributes = 0;
	}

	/** The first atom in the angle. */
	private Atom firstAtom;

	/** The second atom in the angle. */
	private Atom secondAtom;

	/** The third atom in the angle. */
	private Atom thirdAtom;

	/** The ideal bond angle. */
	private float idealBondAngle = -1.0f;

	/* Various attributes of the bond. */
	
	/** The integer that stores the attributes. */
	private int attributes = 0;

	/**
	 * A method that will create a new Bond.
	 *
	 * This may in the future reuse angles from a list of those
	 * that are no longer in use.
	 */
	public static Angle create(){
		return new Angle();
	}

	/** Set the first atom in the angle. */
	public void setFirstAtom(Atom newFirstAtom){
		firstAtom = newFirstAtom;
	}

	/** Set the second atom in the angle. */
	public void setSecondAtom(Atom newSecondAtom){
		secondAtom = newSecondAtom;
	}

	/** Set the second atom in the angle. */
	public void setThirdAtom(Atom newThirdAtom){
		thirdAtom = newThirdAtom;
	}

	/** Return the first atom in a angle. */
	public Atom getFirstAtom(){
		return firstAtom;
	}

	/** Return the second atom in a angle. */
	public Atom getSecondAtom(){
		return secondAtom;
	}

	/** Return the third atom in a angle. */
	public Atom getThirdAtom(){
		return thirdAtom;
	}

	/** Get the specified atom. */
	public Atom getAtom(int index){
		if(index == 0){
			return firstAtom;
		}else if(index == 1){
			return secondAtom;
		}else if(index == 2){
			return thirdAtom;
		}else{
			return null;
		}
	}

	/** Set the ideal angle angle. */
	public void setIdealBondAngle(double d){
		idealBondAngle = (float)d;
	}

	/** Get the ideal angle angle. */
	public double getIdealBondAngle(){
		if(idealBondAngle < 0.0f){
			idealBondAngle =
				(float)Point3d.angle(firstAtom, secondAtom, thirdAtom);
		}

		return idealBondAngle;
	}
}
