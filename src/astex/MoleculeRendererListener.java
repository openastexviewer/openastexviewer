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

import astex.generic.*;

/* Copyright Astex Technology Ltd. 1999 */

/*
 * 29-12-99 mjh
 *	created
 */

/**
 * An interface that allows objects to listen to changes in a
 * MoleculeRenderer.
 */
public interface MoleculeRendererListener {
    /** A molecule was added. */
    public void moleculeAdded(MoleculeRenderer renderer, Molecule molecule);

    /** A molecule was removed. */
    public void moleculeRemoved(MoleculeRenderer renderer, Molecule molecule);

    /** A generic was added. */
    public void genericAdded(MoleculeRenderer renderer, Generic generic);

    /** A generic was removed. */
    public void genericRemoved(MoleculeRenderer renderer, Generic generic);

    /** A map was added. */
    public void mapAdded(MoleculeRenderer renderer, Map map);

    /** A map was removed. */
    public void mapRemoved(MoleculeRenderer renderer, Map map);

    /** An atom was selected. */
    public void atomSelected(MoleculeRenderer renderer, Atom atom);
}
