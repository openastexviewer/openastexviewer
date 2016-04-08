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

/**
 * Molecule Object Model Event.
 *
 * This represents some kind of action on a molecule
 * or component thereof.
 */
public class MoleculeEvent {
    public static final int MoleculeAdded    =   1;
    public static final int MoleculeRemoved  =   2;
    public static final int MoleculeChanged  =   4;
    public static final int AtomAdded        =   8;
    public static final int AtomRemoved      =  16;
    public static final int AtomChanged      =  32;
    public static final int AtomSelected     =  64;

    public static final int MoleculeEventMask     =
	MoleculeAdded | MoleculeRemoved | MoleculeChanged;

    public static final int AtomEventMask     =
	AtomAdded | AtomRemoved | AtomChanged | AtomSelected;
}
