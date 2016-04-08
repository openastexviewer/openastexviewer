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


package astex.splitter;

import java.awt.*;
import java.awt.event.*;

/**
 * <p>Use this code at your own risk!  MageLang Institute is not
 * responsible for any damage caused directly or indirctly through
 * use of this code.
 * <p><p>
 * <b>SOFTWARE RIGHTS</b>
 * <p>
 * MageLang support classes, version 1.0, MageLang Institute
 * <p>
 * We reserve no legal rights to this code--it is fully in the
 * public domain. An individual or company may do whatever
 * they wish with source code distributed with it, including
 * including the incorporation of it into commerical software.
 *
 * <p>However, this code cannot be sold as a standalone product.
 * <p>
 * We encourage users to develop software with this code. However,
 * we do ask that credit is given to us for developing it
 * By "credit", we mean that if you use these components or
 * incorporate any source code into one of your programs
 * (commercial product, research project, or otherwise) that
 * you acknowledge this fact somewhere in the documentation,
 * research report, etc... If you like these components and have
 * developed a nice tool with the output, please mention that
 * you developed it using these components. In addition, we ask that
 * the headers remain intact in our source code. As long as these
 * guidelines are kept, we expect to continue enhancing this
 * system and expect to make other tools available as they are
 * completed.
 * <p>
 * The MageLang Support Classes Gang:
 * @version MageLang Support Classes 1.0, MageLang Insitute, 1997
 * @author <a href="http:www.scruz.net/~thetick">Scott Stanchfield</a>, <a href=http://www.MageLang.com>MageLang Institute</a>
*/
class SplitterBarMouseMotionListener extends MouseMotionAdapter {
	private SplitterBar s;
	
	public SplitterBarMouseMotionListener(SplitterBar s) {this.s = s;}
   	public void mouseDragged(MouseEvent e) {s.mouseDrag(e);}            
}
