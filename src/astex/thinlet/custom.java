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

package astex.thinlet;

import astex.*;
import thinlet.*;
import java.awt.*;
import java.util.*;

/**
 * Implement some kind of custom thinlet
 * user interface.
 */
public class custom {
    static ThinletUI thinletFrame = null;
    static FrameLauncher frameLauncher = null;

    public custom(){
        super();
    }

    public static void handleCommand(MoleculeViewer mv,
                                     MoleculeRenderer mr,
                                     Arguments args){
	String typeString     = args.getString("-type", "mol");
	String urlString      = args.getString("-url", null);
	String moleculeString = args.getString("-molecule", null);
	String parameterName  = args.getString("-parameter", "mol");
	String componentName  = args.getString("-component", null);

        if(urlString == null){
            Log.error("no url defined in custom command");
            return;
        }

        AtomIterator iterator = mr.getAtomIterator();
        Molecule mol = null;

        while(iterator.hasMoreElements()){
            Atom atom = iterator.getNextAtom();
            if(atom.isSelected()){
                mol = atom.getMolecule();
                break;
            }
        }

        if(mol == null){
            Log.error("no molecule defined in custom command");
            return;
        }

        String result = mr.writeMoleculeToUrl(mol, urlString, false,
                                              typeString, parameterName);

        if(componentName != null){
            Enumeration e = ThinletUI.userinterfaces.keys();
            ThinletUI content = null;

            while(e.hasMoreElements()){
                String name = (String)e.nextElement();

                System.out.println("#### name " + name);
                if(name.indexOf("astexviewer.") != -1){
                    content = (ThinletUI)((DialogLauncher)ThinletUI.userinterfaces.get(name)).getContent();
                    break;
                }
            }

            if(content != null){
                Object component = content.find(componentName);

                if(component != null){
                    content.removeAll(component);

                    content.add(component, content.safeParse(result));
                }else{
                    System.out.println("couldn't find component " + componentName);
                }
            }else{
                System.out.println("custom: couldn't find astexviewer control panel");
            }
        }else{

            thinletFrame = new ThinletUI(mv);
            
            thinletFrame.add(thinletFrame.safeParse(result));

            frameLauncher =
                new FrameLauncher(Util.getFrame(mv), "bob",
                                   //new FrameLauncher(new Frame(), "bob",
                                   thinletFrame, 400, 400);

            frameLauncher.setVisible(true);
        }
    }
}
