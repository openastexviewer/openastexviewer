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
import java.util.*;

import jclass.bwt.*;

public class ObjectPropertyDialog extends Dialog
    implements
	WindowListener, JCActionListener, JCSpinBoxListener {
    /** The only instance of an ObjectPropertyDialog. */
    private static ObjectPropertyDialog opd = null;

    /** The object that we are currently operating on. */
    Tmesh object = null;
    
    /** The MoleculeRenderer object. */
    MoleculeRenderer moleculeRenderer = null;

    /** Is the object active yet. */
    boolean active = false;

    /** Create the instance of the opd. */
    public synchronized static ObjectPropertyDialog
	getInstance(Frame f, String label,
		    MoleculeRenderer mr){
	if(opd == null){
	    opd = new ObjectPropertyDialog(f, label, mr);
	}

	return opd;
    }

    /** Set the tmesh object that is being edited. */
    public void setTmesh(Tmesh tm){
	object = tm;
	
	setMinMax();
    }

    /**
     * Create a dialog that lets us edit the texture based
     * properties of a tmesh object.
     */
    public ObjectPropertyDialog(Frame f, String label,
				MoleculeRenderer mr){
	super(f, label, false);

	moleculeRenderer = mr;

	addWindowListener(this);

	createControls();
    }

    /** Which texture coordinate we are editing. */
    JCCheckboxGroup textureCoordinate = null;
    
    private static int textureValues[] = {
	0, 1
    };

    private static String[] textureCoords= {
	"u", "v"
    };

    JCCheckbox applyCharges = null;
    JCCheckbox applyMlp     = null;

    JCSpinBox uminSB        = null;
    JCSpinBox vminSB        = null;
    JCSpinBox umaxSB        = null;
    JCSpinBox vmaxSB        = null;

    Hashtable imageHash = new Hashtable();

    /** Create the controls for this property editor. */
    public void createControls(){
	textureCoordinate =
	    JCCheckbox.makeGroup(textureCoords, textureValues, true);
	textureCoordinate.setOrientation(JCCheckboxGroup.VERTICAL);
	textureCoordinate.setTitle("Coordinate");
	
	Layout.fill(this, textureCoordinate, 0, 0, 1, 1,
		    GridBagConstraints.HORIZONTAL);

	JCGroupBox properties = UI.groupbox("Properties");
	//JCContainer propertyBox = new JCContainer();
	//properties.setLayout(new GridLayout());


	JCButton electrostatic = new JCButton("Electrostatic");
	electrostatic.setActionCommand("electrostatic");
	electrostatic.addActionListener(this);

	Layout.fill(properties, electrostatic, 1, 1, 1, 1,
		    GridBagConstraints.HORIZONTAL);

	applyCharges = new JCCheckbox("Charge");
	applyCharges.setState(1);
	applyCharges.setIndicator(JCCheckbox.INDICATOR_CHECK);

	Layout.fill(properties, applyCharges, 1, 2, 1, 1,
		    GridBagConstraints.HORIZONTAL);

	JCButton mlp = new JCButton("Lipophilicity");
	mlp.setActionCommand("lipophilicity");
	mlp.addActionListener(this);

	Layout.fill(properties, mlp, 1, 3, 1, 1,
		    GridBagConstraints.HORIZONTAL);
	
	applyMlp = new JCCheckbox("Contributions");
	applyMlp.setState(1);
	applyMlp.setIndicator(JCCheckbox.INDICATOR_CHECK);

	Layout.fill(properties, applyMlp, 1, 4, 1, 1,
		    GridBagConstraints.HORIZONTAL);

	JCButton distance = new JCButton("Distance");
	distance.setActionCommand("distance");
	distance.addActionListener(this);

	Layout.fill(properties, distance, 1, 5, 1, 1,
		    GridBagConstraints.HORIZONTAL);
	
	JCButton curvature = new JCButton("Curvature");
	curvature.setActionCommand("curvature");
	curvature.addActionListener(this);

	Layout.fill(properties, curvature, 1, 6, 1, 1,
		    GridBagConstraints.HORIZONTAL);
	
	JCButton atomColors = new JCButton("Atom colors");
	atomColors.setActionCommand("atom_colors");
	atomColors.addActionListener(this);

	Layout.fill(properties, atomColors, 1, 7, 1, 1,
		    GridBagConstraints.HORIZONTAL);
	
	JCButton clipObject = new JCButton("Clip");
	clipObject.setActionCommand("clip_object");
	clipObject.addActionListener(this);

	Layout.fill(properties, clipObject, 1, 8, 1, 1,
		    GridBagConstraints.HORIZONTAL);
	
	//JCSlider distanceMax = new JCSlider(JCSlider.HORIZONTAL, 8, 1, 20);
	//distanceMax.setPreferredSize(70, BWTEnum.NOVALUE);
	//distanceMax.setNumTicks(10);
	//distanceMax.setMinimumLabel(new JCLabel("1"));
	//distanceMax.setMaximumLabel(new JCLabel("20"));

	//distanceMax = new JCSpinBox(6);
	//distanceMax.setMinimum(10);
	//distanceMax.setMaximum(4000);
	//distanceMax.setDecimalPlaces(1);
	//distanceMax.setIncrement(5);
	//distanceMax.setIntValue(80);
	//distanceMax.addSpinBoxListener(this);

	//Layout.fill(properties, distanceMax, 2, 3, 1, 1,
	//	    GridBagConstraints.HORIZONTAL);

	Layout.fill(this, properties, 1, 0, 1, 2,
		    GridBagConstraints.VERTICAL);

	//Texture ranges
	JCGroupBox rangeGB = UI.groupbox("Ranges");

	JCLabel uminLabel = new JCLabel("umin");
	uminSB = UI.spinbox(5, -10000, 10000, 0, 2, 5, this);

	JCLabel umaxLabel = new JCLabel("umax");
	umaxSB = UI.spinbox(5, -10000, 10000, 0, 2, 5, this);

	JCLabel vminLabel = new JCLabel("vmin");
	vminSB = UI.spinbox(5, -10000, 10000, 0, 2, 5, this);

	JCLabel vmaxLabel = new JCLabel("vmax");
	vmaxSB = UI.spinbox(5, -10000, 10000, 0, 2, 5, this);

	//setMinMax();

	Layout.nofill(rangeGB, uminLabel, 0, 0);
	Layout.nofill(rangeGB, uminSB,    1, 0);
	Layout.nofill(rangeGB, umaxLabel, 2, 0);
	Layout.nofill(rangeGB, umaxSB,    3, 0);
	Layout.nofill(rangeGB, vminLabel, 0, 1);
	Layout.nofill(rangeGB, vminSB,    1, 1);
	Layout.nofill(rangeGB, vmaxLabel, 2, 1);
	Layout.nofill(rangeGB, vmaxSB,    3, 1);

	Layout.fill(this, rangeGB, 0, 2, 2, 1,
		    GridBagConstraints.HORIZONTAL);

	JCGroupBox textureGB = UI.groupbox("Textures");

	Layout.fill(this, textureGB, 0, 1, 1, 1,
		    GridBagConstraints.BOTH);

	JCScrolledWindow textureList = new JCScrolledWindow();
	textureList.setScrollbarDisplay(JCScrolledWindow.DISPLAY_VERTICAL_ONLY);

	JCContainer textureContainer = new JCContainer();
	textureContainer.setLayout(new JCGridLayout(0, 3));

	textureList.add(textureContainer);

	Layout.fill(textureGB, textureList, 1, 1, 5, 1, GridBagConstraints.BOTH);

	JCActionListener tal = new JCActionListener(){
		public void actionPerformed(JCActionEvent e){
		    String texture = (String)imageHash.get(e.getSource());
		    String textureName = Settings.getString("config", texture);
		    String textureImage = Settings.getString("config", texture + ".image");
		    String command =
			"texture load '" + textureName + "' '" + textureImage + "';";
		    command += 
			"object '" + object.getName() + "' texture '" + textureName +"';";

		    System.out.println("command " + command);
		    moleculeRenderer.execute(command);
		    moleculeRenderer.repaint();
		}
	    };
		

	for(int t = 0; t < 10000; t++){
	    String texture = "texture." + t;
	    String textureName = Settings.getString("config", texture);

	    if(textureName == null){
		break;
	    }

	    String smallImageTag = "texture." + t + ".small";
	    String smallImageName = Settings.getString("config", smallImageTag);

	    if(smallImageName != null){
		//Image im = JCUtilConverter.toImage(app, smallImageName);

		Image smallImage = Texture.loadImage(smallImageName);

		if(smallImage != null){

		    MediaTracker mt = new MediaTracker(this);
		    
		    mt.addImage(smallImage, 1);
	
		    try {
			mt.waitForAll();
		    }catch(InterruptedException e){
			Log.error("interrupted loading " + smallImageName);
		    }

		    int width = smallImage.getWidth(null);
		    
		    JCButton ib = new JCButton(smallImage);

		    ib.setPreferredSize(16, 16);
		    ib.setInsets(new Insets(0,0,0,0));
		    ib.addActionListener(tal);

		    imageHash.put(ib, texture);

		    textureContainer.add(ib);
		}else{
		    Log.error("couldn't load image defined by " + smallImageTag);
		}
	    }else{
		Log.error("no small image defined by " + texture);
	    }
	}

    }

    public void spinBoxChangeBegin(JCSpinBoxEvent e){
    }

    public void spinBoxChangeEnd(JCSpinBoxEvent e){
	if(!active) return;

	boolean handled = true;
	Object source   = e.getSource();
	String command  = "";

	if(source == uminSB || source == umaxSB ||
	   source == vminSB || source == vmaxSB){
	    command += "object '" +object.getName() + "' texture ";
	    if(source == uminSB || source == umaxSB){
		command +=
		    "urange " + uminSB.getValue() + " " + umaxSB.getValue();
	    }else{
		command +=
		    "vrange " + vminSB.getValue() + " " + vmaxSB.getValue();
	    }

	    command += ";";
	    //System.out.println("command " + command);
	}else{
	    handled = false;
	}

	if(handled){
	    moleculeRenderer.execute(command);
	    moleculeRenderer.moleculeViewer.dirtyRepaint();
	}
    }

    /** Handle actions on the user interface. */
    public void actionPerformed(JCActionEvent e){
	if(!active) return;

	String actionCommand  = e.getActionCommand();
	boolean handled       = true;

	String name           = "'" + (String)object.getName() + "'";
	int tc                = textureCoordinate.getValue();
	String texCoord       = textureCoords[tc];
	boolean needTexture   = true;
	String command        = "";

	if(actionCommand.equals("distance")){
	    //double d = FILE.readDouble(distanceMax.getValue());

	    //System.out.println("distance " + d);
	    command += "object " + name + " texture distance " +
		       texCoord + " default;";
	    command += "object " + name + " texture " +
		       texCoord + "div 8.0;";
	    //moleculeRenderer.execute("object " + name + " texture " +
	    //       texCoord + "div " + d +";");

	    if(tc == 0){
		command += "texture load white 'white.jpg';";
		command += "object " + name + " texture white;";
	    }

	    needTexture = false;

	}else if(actionCommand.equals("electrostatic")){
	    if(applyCharges.getState() == 1){
		command += "run 'charge.properties';";
	    }

	    command += "object " + name + 
		       " texture electrostatic " +
		       texCoord + " 12.0 default;";
	    command += "texture load rwb 'images/textures/rwb.jpg';";
	    command += "object " + name + " texture rwb;";
	    needTexture = false;
	}else if(actionCommand.equals("lipophilicity")){
	    if(applyMlp.getState() == 1){
		command += "run 'lipophilicity.properties';";
	    }

	    command += "object " + name + 
		       " texture lipophilicity " +
		       texCoord + " 7.0 default;";
	    
	    command += "texture load molcad 'images/textures/red2blue.jpg';";

	    command += "object " + name + " texture molcad;";
	    needTexture = false;
	    //moleculeRenderer.execute("object " + name + " texture " +
	    //	   texCoord + "offset -0.44;");
	    //moleculeRenderer.execute("object " + name + " texture " +
	    //	   texCoord + "div 0.6;");
	}else if(actionCommand.equals("curvature")){
	    command += "object " + name + " texture curvature " +
		texCoord + " 6 default;";
	    command += "object " + name + " texture " +
		texCoord + "div 1.0;";
	    command += "texture load rwb 'images/textures/rwb.jpg';";
	    command += "object " + name + " texture rwb;";
	    needTexture = false;
	}else if(actionCommand.equals("atom_colors")){
	    command += "object " + name + " -map { current };";
	}else if(actionCommand.equals("clip_object")){
	    command += "object " + name + " clip " + texCoord + ";";
	    needTexture = false;
	}else{
	    handled = false;
	}

	if(needTexture){
	    moleculeRenderer.execute("texture close simple;");
	    moleculeRenderer.execute("object " + name + " texture close;");
	}

	if(handled){
	    moleculeRenderer.execute(command);

	    setMinMax();

	    moleculeRenderer.moleculeViewer.dirtyRepaint();
	    return;
	}
    }

    /** Set the min max values for this object. */
    public void setMinMax(){
	active = false;

	// add 0.5 to make rounding better when converting to int
	double umin = object.getInverseTexture(Tmesh.UTexture, 0.0);
	uminSB.setIntValue((int)(0.5 + 100.*umin));
	double umax = object.getInverseTexture(Tmesh.UTexture, 1.0);
	umaxSB.setIntValue((int)(0.5 + 100.*umax));
	double vmin = object.getInverseTexture(Tmesh.VTexture, 0.0);
	vminSB.setIntValue((int)(0.5 + 100.*vmin));
	double vmax = object.getInverseTexture(Tmesh.VTexture, 1.0);
	vmaxSB.setIntValue((int)(0.5 + 100.*vmax));

	active = true;

	/*
	FILE.out.print("umin %f\n", umin);
	FILE.out.print("umax %f\n", umax);
	FILE.out.print("vmin %f\n", vmin);
	FILE.out.print("vmax %f\n", vmax);
	*/
    }

    /* Implementation of WindowListener. */
    public void windowClosing(WindowEvent e){
	hide();
	//dispose();
    }

    public void windowActivated(WindowEvent e){ }
    public void windowClosed(WindowEvent e){ }
    public void windowDeactivated(WindowEvent e){ }
    public void windowDeiconified(WindowEvent e){ }
    public void windowIconified(WindowEvent e){ }
    public void windowOpened(WindowEvent e){ }
}
