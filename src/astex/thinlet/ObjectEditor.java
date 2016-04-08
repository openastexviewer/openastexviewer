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

import thinlet.*;
import astex.*;
import java.awt.*;
import java.awt.event.*;

public class ObjectEditor extends ThinletUI {
    private static int width = 320;
    private static int height = 400;

    private static DialogLauncher dialogLauncher = null;
    private static ObjectEditor objectEditor = null;

    public ObjectEditor(String name){
        String template = readTemplate("/astex/thinlet/objecteditortemplate.xml.properties");
        template = Util.replace(template, "%n", name);
        add(safeParse(template));
    }

    /**
     * Display a thinlet ObjectEditor and return a new Color.
     *
     * Returns null if the cancel button is pressed.
     */
    public static void editObject(Frame f, String title, int x, int y,
                                  ThinletUI tui, String name){
        int xpos = x - width/2;
        int ypos = y - height/2;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // approximately force window on screen
        // DialogLauncher needs to enforce this, as only it knows
        // the dimensions of the frame insets
        if(xpos < 0) xpos = 0;
        if(ypos < 0) ypos = 0;

        if(xpos + width > screenSize.width) xpos = screenSize.width - width;
        if(ypos + height > screenSize.height) ypos = screenSize.height - height;

        objectEditor = new ObjectEditor(name);
        objectEditor.moleculeRenderer = tui.moleculeRenderer;
        objectEditor.moleculeViewer = tui.moleculeViewer;
        
        objectEditor.setTextureCoordinates(name);

        dialogLauncher = new DialogLauncher(f, name, objectEditor,
                                            xpos, ypos,
                                            width, height,
                                            false);

        Object root = objectEditor.getDesktop();

        objectEditor.putProperty(root, "dialog", dialogLauncher);
        objectEditor.putProperty(root, "ok", Boolean.FALSE);

        // block here
        dialogLauncher.setVisible(true);
    }
    
    public void setTextureCoordinates(String name){
        if(name == null){
            print.f("can't find object called " + name);
            return;
        }

        Tmesh object = moleculeRenderer.renderer.getGraphicalObject(name);

        if(object != null){
            Object oumin = find("umin");
            if(oumin != null){
                double umin = object.getInverseTexture(Tmesh.UTexture, 0.0);
                setString(oumin, "text", FILE.sprint("%.2f", umin));
            }
            Object oumax = find("umax");
            if(oumax != null){
                double umax = object.getInverseTexture(Tmesh.UTexture, 1.0);
                setString(oumax, "text", FILE.sprint("%.2f", umax));
            }
            Object ovmin = find("vmin");
            if(ovmin != null){
                double vmin = object.getInverseTexture(Tmesh.VTexture, 0.0);
                setString(ovmin, "text", FILE.sprint("%.2f", vmin));
            }
            Object ovmax = find("vmax");
            if(ovmax != null){
                double vmax = object.getInverseTexture(Tmesh.VTexture, 1.0);
                setString(ovmax, "text", FILE.sprint("%.2f", vmax));
            }
        }else{
            print.f("couldn't configure editor for " + name);
        }
    }

    public void executeTexture(Object o){
        execute(o);

        Object parent = getParent(o);

        while(getParent(parent) != getDesktop()){
            parent = getParent(parent);
        }

        String name = getString(parent, "name");

        setTextureCoordinates(name);
    }

    public void ok(Object dialogPanel){
        Dialog dialog = (Dialog)getProperty(getDesktop(), "dialog");

        dialog.hide();
    }

    public boolean destroy(){
        return false;
    }

    public void windowClosing(WindowEvent e){
	e.getWindow().hide();
    }

    public void windowActivated(WindowEvent e){ }
    public void windowClosed(WindowEvent e){ }
    public void windowDeactivated(WindowEvent e){ }
    public void windowDeiconified(WindowEvent e){ }
    public void windowIconified(WindowEvent e){ }
    public void windowOpened(WindowEvent e){ }
}