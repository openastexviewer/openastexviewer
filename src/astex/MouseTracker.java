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

import java.util.*;
import astex.*;

public class MouseTracker {
    public static void handleCommand(MoleculeViewer mv,
                                     MoleculeRenderer mr,
                                     Arguments args){
        Enumeration e = args.keys();

        while(e.hasMoreElements()){
            String s = (String)e.nextElement();
            Object o = args.get(s);

            if("null".equals(o)){
                o = null;
            }

            if("-mouseover".equals(s)){
                //System.out.println(s + " = " + o);

                mv.mouseOverLabel = (String)o;
            }else if("-onclick".equals(s)){
                mv.onClickLabel = (String)o;
            }else if("-mouseovercommand".equals(s)){
                mv.mouseOverCommand = (String)o;
            }
        }
    }
}
