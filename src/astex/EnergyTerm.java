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
 * A class for representing an energy term in a force field.
 *
 * The same class is used for storing all energy terms.
 * The objects that the term operates on are identified by
 * integers rather than object references so that they may
 * work efficiently with the optimizer.
 */
public class EnergyTerm extends DynamicArray {
    /** The target value e.g. bond length, angle, improper torsion. */
    public double targetValue = 0.0;

    /** The force constant for this term. */
    public double forceConstant = 10.0;

    /** The list of atoms that are affected by this energy term. */
    public DynamicArray affectedAtoms = null;

    /** The atoms we will optimise to the density. */
    public DynamicArray signalAtoms = null;

    /** Is this torsion constrained e.g. omega in peptide. */
    public boolean constrained = false;
}
