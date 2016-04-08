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

/**
 * Class for manipulating molecular structure.
 *
 * The renderer will decide on which atoms are picked.
 * The MouseEvent is passed along just in case you
 * want to check the on any modifier keys that are
 * pressed.
 */
package astex.xmt;
import astex.*;

import java.awt.event.*;

public interface Manipulator {
    /** Pick an atom. */
    public void pick(MouseEvent e, MoleculeViewer mv, Atom a);

    /** Drag an atom. */
    public void drag(MouseEvent e, MoleculeViewer mv, Atom a);

    /** Release an atom. */
    public void release(MouseEvent e, MoleculeViewer mv, Atom a);
}
