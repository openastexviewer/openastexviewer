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
 * 29-10-99 mjh
 *	created
 */

/**
 * Convenience methods for using GridBagLayout manager.
 */
import java.util.*;
import java.awt.*;
import java.io.Serializable;

public class Layout implements Serializable {
    public static void fill(Container container, Component component,
			    int gx, int gy){
	Layout.fill(container, component, gx, gy, 1, 1);
    }
	
    public static void fill(Container container, Component component,
			    int gx, int gy, int filldir){
	double wx = 0., wy = 0.;
		
	// We have to check the possibilities like this as AWT
	// does not define BOTH = HORIZONTAL|VERTICAL
		
	if(filldir == GridBagConstraints.BOTH){
	    wx = wy = 100.;
	}else if(filldir == GridBagConstraints.HORIZONTAL){
	    wx = 100.;
	}else if(filldir == GridBagConstraints.VERTICAL){
	    wy = 100.;
	}
		
	Layout.constrain(container, component, gx, gy, 1, 1, filldir,
			 GridBagConstraints.WEST, wx, wy);
    }
	
    public static void fill(Container container, Component component,
			    int gx, int gy, int wx, int wy){
	Layout.constrain(container, component, gx, gy, wx, wy,
			 GridBagConstraints.BOTH,
			 GridBagConstraints.CENTER,
			 100., 100.);
    }
	
    public static void fill(Container container, Component component,
			    int gx, int gy,
			    int widthx, int widthy, int filldir){
	double wx = 0., wy = 0.;
		
	// We have to check the possibilities like this as AWT
	// does not define BOTH = HORIZONTAL|VERTICAL
		
	if(filldir == GridBagConstraints.BOTH){
	    wx = wy = 100.;
	}else if(filldir == GridBagConstraints.HORIZONTAL){
	    wx = 100.;
	}else if(filldir == GridBagConstraints.VERTICAL){
	    wy = 100.;
	}
		
	Layout.constrain(container, component, gx, gy, widthx, widthy,
			 filldir,
			 GridBagConstraints.CENTER,
			 wx, wy);
    }
	
    public static void nofill(Container container, Component component,
			      int gx, int gy){
	Layout.constrain(container, component, gx, gy, 1, 1,
			 GridBagConstraints.NONE,
			 GridBagConstraints.WEST,
			 0., 0.);
    }
	
    public static void constrain(Container container, Component component,
				 int gridx, int gridy,
				 int width, int height,
				 int fill, int anchor,
				 double weightx, double weighty){
		
	GridBagConstraints c = new GridBagConstraints();
	c.gridx = gridx;
	c.gridy = gridy;
	c.gridwidth = width;
	c.gridheight = height;
	c.fill = fill;
	c.anchor = anchor;
	c.weightx = weightx;
	c.weighty = weighty;
		
	if(!(container.getLayout() instanceof GridBagLayout)){
	    container.setLayout(new GridBagLayout());
	}

	((GridBagLayout)container.getLayout()).setConstraints(component, c);
		
	container.add(component);
    }
}

