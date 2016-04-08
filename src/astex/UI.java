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
import jclass.bwt.*;

public class UI {
    // Convenience methods for BWT.

    /** Create a button. */
    public static JCButton button(String label, String command,
				  JCActionListener l){
	JCButton b = new JCButton(label);
	b.setActionCommand(command);
	if(l != null){
	    b.addActionListener(l);
	}

	return b;
    }

    /** Create a checkBox. */
    public static JCCheckbox checkbox(String label, String command,
				      JCItemListener l){
	JCCheckbox b = new JCCheckbox(label);
	b.setIndicator(JCCheckbox.INDICATOR_CHECK);
	b.setUserData(command);
	if(l != null){
	    b.addItemListener(l);
	}
	b.setInsets(new Insets(0, 0, 0, 0));

	return b;
    }

    /** Create a group box with nice insets. */
    public static JCGroupBox groupbox(String label){
	JCGroupBox gb = new JCGroupBox(label);
	gb.setInsets(new Insets(5, 5, 5, 5));

	return gb;
    }

    /** Create a spin box. */
    public static JCSpinBox spinbox(int columns,
				    int min, int max, int val, int dp,
				    int inc, JCSpinBoxListener sbl){
	JCSpinBox sb = new JCSpinBox(columns);
	sb.setMinimum(min);
	sb.setMaximum(max);
	sb.setDecimalPlaces(dp);
	sb.setIntValue(val);
	sb.setIncrement(inc);
	
	if(sbl != null){
	    sb.addSpinBoxListener(sbl);
	}

	sb.setBackground(Color.white);
	//sb.setInsets(new Insets(0,0,0,0));
	return sb;
    }

    private static int xGrid[] = new int[1000];
    private static int yGrid[] = new int[1000];
    private static Object obj[] = new Object[1000];
    private static int depth = 0;

    public static final int NONE = GridBagConstraints.NONE;
    public static final int HORIZONTAL = GridBagConstraints.HORIZONTAL;
    public static final int VERTICAL = GridBagConstraints.VERTICAL;
    public static final int BOTH = GridBagConstraints.BOTH;

    public static void add(Object o){
	add(o, 1, 1, 0);
    }

    public static void add(Object o, int xWidth){
	add(o, xWidth, 1, 0);
    }

    public static void add(Object o, int xWidth, int fill){
	add(o, xWidth, 1, fill);
    }

    public static void add(Object o, int xWidth, int yWidth, int fill){

	if(o instanceof String){
	    //System.out.println("creating box");
	    JCGroupBox box = new JCGroupBox((String)o);
	    box.setInsets(new Insets(5, 5, 5, 5));

	    o = box;
	}

	if(depth > 0){
	    int currentDepth = depth - 1;
	    Container parent = (Container)obj[currentDepth];

	    Layout.fill(parent, (Component)o,
			xGrid[currentDepth],
			yGrid[currentDepth],
			xWidth, yWidth,
			fill);

	    xGrid[currentDepth] += xWidth;
	    yGrid[currentDepth] += (yWidth-1);
	}

	if(o instanceof Container && !(o instanceof JCComboBox)){
	    Container p = (Container)o;
	    p.setLayout(new GridBagLayout());
	    obj[depth] = p;
	    xGrid[depth] = 0;
	    yGrid[depth] = 0;
	    depth++;
	}

	//System.out.println("after add depth = " + depth);
    }

    public static void pop(){
	if(depth > 0){
	    depth--;
	}else{
	    System.out.println("ui stack already empty");
	}
    }

    public static void newRow(){
	if(depth > 0){
	    int currentDepth = depth - 1;
	    xGrid[currentDepth] = 0;
	    yGrid[currentDepth]++;
	}
    }

    public static void reset(){
	depth = 0;
    }

    public static void main(String args[]){
	Frame f = new Frame();

	add(f);
	add("Controls 1");
	add(new JCLabel("fred"));
	add(new JCLabel("bob"));
	newRow();
	add(new JCLabel("angela"));
	//pop();
	newRow();
	add("Controls 2", 2,1, UI.BOTH);
	add(new JCLabel("fred"));
	add(new JCLabel("bob"));
	newRow();
	add(new JCLabel("angela"));
	pop();

	f.pack();
	f.show();
    }
}
