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

public class ColorChooser extends Thinlet {
    private static int width = 320;
    private static int height = 200;

    private static ColorChooser colorChooser = null;

    public ColorChooser(){
        try {
            add(parse("/astex/thinlet/colorchooser.xml.properties"));
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Display a thinlet ColorChooser and return a new Color.
     *
     * Returns null if the cancel button is pressed.
     */
    public static Color getcolor(Frame f, String title, int x, int y){
        int xpos = x - width/2;
        int ypos = y - height/2;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	DialogLauncher dialogLauncher = null;

        // approximately force window on screen
        // DialogLauncher needs to enforce this, as only it knows
        // the dimensions of the frame insets
        if(xpos < 0) xpos = 0;
        if(ypos < 0) ypos = 0;

        if(xpos + width > screenSize.width) xpos = screenSize.width - width;
        if(ypos + height > screenSize.height) ypos = screenSize.height - height;

        if(dialogLauncher == null){
            colorChooser = new ColorChooser();

            dialogLauncher = new DialogLauncher(f, title, colorChooser,
                                                xpos, ypos,
                                                width, height,
                                                true);
        }

        Object root = colorChooser.getDesktop();

        colorChooser.putProperty(root, "dialog", dialogLauncher);
        colorChooser.putProperty(root, "ok", Boolean.FALSE);

        // block here
        dialogLauncher.setVisible(true);

        Object value = colorChooser.getProperty(root, "ok");

	dialogLauncher.dispose();

        if(value == null || ((Boolean)value).booleanValue() == false){
            return null;
        }else{
            return (Color)colorChooser.getProperty(root, "newcolor");
        }
    }
    
    public void colorok(Object dialogPanel){
        Dialog dialog = (Dialog)getProperty(getDesktop(), "dialog");

        dialog.hide();

        Object sample = find("ccsample");
        Color color = getColor(sample, "background");

        putProperty(getDesktop(), "newcolor", color);
        putProperty(getDesktop(), "ok", Boolean.TRUE);
    }

    public void colorcancel(Object component){
        Dialog dialog = (Dialog)getProperty(getDesktop(), "dialog");

        dialog.hide();

        putProperty(getDesktop(), "ok", Boolean.FALSE);
    }

    public void change(Thinlet thinlet, Object component){
        Object ccred    = thinlet.find("ccred");
        Object ccgreen  = thinlet.find("ccgreen");
        Object ccblue   = thinlet.find("ccblue");
        Object cchex    = thinlet.find("cchex");
        Object ccsample = thinlet.find("ccsample");

        Color newcolor = null;
        int red = 0, green = 0, blue = 0;

        if(getClass(component) == "button"){
            newcolor = getColor(component, "background");
            if(newcolor == null){
                newcolor = new Color(0, 255, 0);
            }
            red = newcolor.getRed();
            green = newcolor.getGreen();
            blue = newcolor.getBlue();
        }else{
            red   = Integer.parseInt(getString(ccred, "text"));
            green = Integer.parseInt(getString(ccgreen, "text"));
            blue  = Integer.parseInt(getString(ccblue, "text"));
        }

        if(newcolor == null){
            newcolor = new Color(red, green, blue);
        }

        setColor(ccsample, "background", newcolor);

        setString(ccred,   "text", "" + red);
        setString(ccgreen, "text", "" + green);
        setString(ccblue,  "text", "" + blue);

        String hex = Integer.toHexString(0xff000000|(red<<16)|(green<<8)|blue);
        setString(cchex, "text", "0x" + hex.substring(2, 8));
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