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

import java.awt.*;
import java.awt.event.*;

public class ColorButton extends ImageButton {
    /**
     * Default constructor.
     * Create a ColorButton that is white and 16x16 pixels.
     */
    public ColorButton(){
	this(Color.white, 16);
    }

    private Frame colorChooserFrame = null;
    private Dialog colorChooserDialog = null;
    private ColorChooser colorChooser = null;

    /**
     * Instruct AstexViewer to display its color gadget
     * so that a JavaScript interface can use it for
     * picking colours.
     */
    public String getColor(int x, int y){

	if(colorChooserDialog == null){
	    if(colorChooserFrame == null){
		colorChooserFrame = new Frame();
	    }
	    colorChooserDialog = new Dialog(colorChooserFrame, true);

	    colorChooser = new ColorChooser(colorChooserDialog);
	    colorChooserDialog.add(colorChooser);
	}

	showAt(colorChooserDialog, x, y);

	if(colorChooser.accept){
	    String s = FILE.sprint("0x%06x", colorChooser.rgb & 0xffffff);
	    return s;
	}else{
	    return null;
	}
    }

    /**
     * Show the dialog at the position.
     */
    private static void showAt(Dialog d, int x, int y){
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

	// change the screen height to reflect possible toolbar
	screenSize.height -= 54;

	d.pack();

	Dimension dSize = d.size();

	// center chooser on the passed location
	//x -= (int)(dSize.width * 0.5);
	//y -= (int)(dSize.height * 0.5);

	// shuffle coords to stop dialog being off screen.

	if(x + dSize.width > screenSize.width){
	    x = screenSize.width - dSize.width;
	}else if(x < 0){
	    x = 0;
	}

	if(y + dSize.height > screenSize.height){
	    y = screenSize.height - dSize.height;
	}else if(y < 0){
	    y = 0;
	}

	d.setLocation(x, y);
	d.show();
    }

    /**
     * Create a color button.
     * This will popup a ColorChooser and let the user
     * select a color.
     *
     * @param color The initial color of the button.
     * @param size  The size of the button in pixels (always square).
     */
    public ColorButton(Color color, int size){
	super(null, size, null);
	setBackground(color);

	ActionListener cbal = new ActionListener(){
		public void actionPerformed(ActionEvent e){
		    //JCButton b = (JCButton)e.getSource();
		    Point location = getLocationOnScreen();
		    Dimension bsize = getSize();
	    
		    String newColorString =
			getColor(location.x + bsize.width,
				 location.y + bsize.height);

		    if(newColorString != null){
			int newColor =
			    Color32.getColorFromName(newColorString);

			//setForeground(new Color(newColor));
			setBackground(new Color(newColor));
			//setLabel(null);
			colorString = Color32.formatNoQuotes(newColor);
			cancelled = false;
		    }else{
			cancelled = true;
		    }
		}
	    };

	setForeground(color);
	setBackground(color);
	//setLabel("");
	//setSize(size, size);
	//setInsets(new Insets(0,0,0,0));
	addActionListener(cbal);

	colorString = Color32.formatNoQuotes(color.getRGB());
    }

    /** Was the action cancelled. */
    private boolean cancelled = false;

    /** Was the button cancelled. */
    public boolean wasCancelled(){
	return cancelled;
    }

    /**
     * The last color that was selected
     * or null if cancel was pressed.
     */
    private String colorString = null;

    /** Return the selected color as a string. */
    public String getValue(){
	return colorString;
    }

    /** Return the selected color as a packed integer. */
    public int getIntegerValue(){
	return Color32.getColorFromName(colorString);
    }
}