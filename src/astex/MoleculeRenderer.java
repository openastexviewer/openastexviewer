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
 * 20-10-03 mjh
 *	Fix labelling of symmetry generated atoms. They now have the
 *	correct residue and chain name from the original molecule.
 *	The atom numbering problem still exists.
 * 15-08-02 mjh
 *	Completely rejig the drawing of CA traces and normal
 *	bond styles. For some reason they were mutually 
 *	exclusive, now you can have the trace displayed
 *	as well as atoms which lets you make simplified
 *	schematic displays. The traces are always drawn wide now.
 * 07-08-02 mjh
 *	Remove the restriction on 255 atoms for drawing bond orders.
 *	Also make aromatic bonds in structures get drawn with
 *	dotted lines as you would expect.
 *
 * 06-08-02 mjh
 *	XXX Need to fix the atom renumbering that goes on when
 *	symmetry is generated. This breaks all sorts of atom
 *	selections.
 *
 * 15-03-02 mjh
 *	Fix bug in symmetry generation when molecule was not in
 *	primary unit cell.
 * 15-07-01 mjh
 *	Fix bug in atom picking that meant that atoms in undisplayed
 *	molecules got picked. Caused by not checking the molecule display
 *	status before checking the atom display status.
 * 01-12-00 mjh
 *	Add simple grid type, so that superstar like grids
 *	can be contoured.
 * 17-03-00 mjh
 *	fix a bug in the map contouring that caused the center grid
 *	point to be miscalculated. Related to the bug that caused the
 *	wrong region to be contoured in the contouring code.
 * 23-12-99 mjh
 *	merge MoleculeScene back into this file as it was too confusing
 *	when the functionality was split between the two values. make
 *	setCenter set to the map radius even if there are no maps
 *	being displayed.
 *	Pick up the properties from a file rather than being hard wired.
 * 07-12-99 mjh
 *	fix serious bug in initialiseCenter which meant
 *	that the center wasn't correctly calculated as MIN_VALUE
 *	is the smallest number not the most negative
 * 05-12-99 mjh
 *	switch over to using a moleculeScene to keep track of
 *	all the objects we want to render.
 * 15-11-99 mjh
 *	created
 */
import java.awt.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;

import astex.parser.*;
import astex.generic.*;

/**
 * Class for drawing a molecule into our renderer.
 */
//public class MoleculeRenderer extends Renderer implements Function {
public class MoleculeRenderer {
    /** The Renderer that will do the work. */
    public Renderer renderer = new Renderer();

    /** The moleucle viewer class that we are drawing for. */
    public transient MoleculeViewer moleculeViewer = null;

    /** The list of molecules. */
    private DynamicArray molecules = new DynamicArray();

    /** The list of maps. */
    private DynamicArray maps = new DynamicArray();

    /** The list of selected atoms. */
    private DynamicArray selectedAtoms = new DynamicArray();

    /** The list of labelled atoms. */
    //private DynamicArray labelledAtoms = new DynamicArray();

    /** The list of atoms that are bumped. */
    private DynamicArray bumpAtoms = new DynamicArray();

    /** The list of atoms that are showing distance monitors. */
    private DynamicArray distances = new DynamicArray();

    /** The list of atoms that are showing torsion monitors. */
    private DynamicArray torsions = new DynamicArray();

    /** The list of hydrogen bonds. */
    private DynamicArray hbonds = new DynamicArray();

    /** The list of MoleculeRendererListeners. */
    private transient DynamicArray moleculeRendererListeners = new DynamicArray();

    /** The set of defined groups. */
    public Hashtable groups = new Hashtable();

    /** The symmetry we will use. */
    private Symmetry symmetry = null;

    /** The radius of map that we will display. */
    private double mapRadius = 7.0;

    /** The map radius property name. */
    private static String mapRadiusProperty = "map.radius";

    /** The radius of symmetry generated atoms that we will generate. */
    //private double symmetryRadius = 12.0;
    private double symmetryRadius = 15.0;

    /** The symmetry radius property name. */
    private static String symmetryRadiusProperty = "symmetry.radius";

    /** The radius that we will display when we set origin. */
    private double displayRadius = 6.0;

    /** The minimum radius that we will allow to set. */
    private static final double minimumDisplayRadius = 6.0;

    /** The minimum clip distance we will allow. */
    private double minimumClipDistance = 6.0;

    /** The view radius property name. */
    private static String displayRadiusProperty = "display.radius";

    /** The contour level property name. */
    private static String contourLevelsProperty = "contour.levels";

    /** The standard aminoacid properties name. */
    private static String standardAminoacidsProperty = "standard.aminoacids";

    /** Should we display symmetry at all. */
    private boolean displaySymmetry = true;

    /** Should we display atom bumps. */
    private boolean displayBumps = false;

    /** Should bumps be restrictd to atoms in the same molecule. */
    private boolean bumpInSameMolecule = false;

    /** Should we display atom distances. */
    private boolean displayDistances = true;

    /** Should we display electron density maps. */
    private boolean displayMaps = true;

    /** Should we display solvent. */
    private boolean displaySolvent = true;

    /** Normal picking mode. */
    public static int NORMAL_PICK = 1;

    /** Distance picking mode. */
    public static int DISTANCE_PICK = 2;

    /** Angle picking mode. */
    public static int ANGLE_PICK = 3;

    /** Angle picking mode. */
    public static int TORSION_PICK = 4;

    /** The current pick mode. */
    public int pickMode = NORMAL_PICK;

    boolean allowFastDraw = true;

    boolean fastDraw = false;

    private boolean selectCount = true;

    /** Are we debugging. */
    public boolean debug = false;

    /**
     * The current contour grid size.
     * The number of grid points we contour around
     * the map center.
     */
    public int contourSize = 24;

    public boolean hersheyFonts = false;

    /** Default constructor. */
    public MoleculeRenderer(){
	super();

	// set up the default contour levels
	resetContourLevels();
	addContourLevel(3.0, Color32.red, true, Map.Surface);
	addContourLevel(2.0, Color32.orange, false, Map.Lines);
	addContourLevel(-2.0, Color32.blue, false, Map.Lines);

	hersheyFonts = Settings.getBoolean("fonts", "hershey.fonts");
	shadows      = Settings.getBoolean("config", "shadows");
	bondLineRadius = Settings.getDouble("config", "bondlineradius");

	initialise();
    }

    public void repaint(){
	if(moleculeViewer != null){
	    moleculeViewer.dirtyRepaint();
	}
    }

    public void setContourSize(int cs){
	contourSize = cs;
    }

    /** Set the picking mode. */
    public void setPickMode(int pick){
	if(pick != NORMAL_PICK && 
	   pick != DISTANCE_PICK && 
	   pick != ANGLE_PICK && 
	   pick != TORSION_PICK){
	    pick = NORMAL_PICK;

	}
	pickMode = pick;
    }

    /** Set whether or not we print select counts. */
    public void setSelectCount(boolean b){
	selectCount = b;
    }

    /** Return whether we are printing select counts. */
    public boolean getSelectCount(){
	return selectCount;
    }

    /** Handle a delete atoms command. */
    public void handleDeleteCommand(DynamicArray selectedAtoms){
	int atomCount = selectedAtoms.size();
	for(int a = 0; a < atomCount; a++){
	    Atom atom = (Atom)selectedAtoms.get(a);

	    deleteAtom(atom);
	}
    }

    /** Handle an atom edit command. */
    public void handleEditCommand(String name, String value, DynamicArray selectedAtoms){
	int atomCount = selectedAtoms.size();

	for(int a = 0; a < atomCount; a++){
	    Atom atom = (Atom)selectedAtoms.get(a);

	    atom.edit(name, value);
	}
    }

    /** Actually delete an atom. */
    public void deleteAtom(Atom atom){
	int bondCount = atom.getBondCount();
	Molecule mol = atom.getMolecule();

	for(int b = bondCount - 1; b >= 0; b--){
	    Bond bond = atom.getBond(b);
	    Atom other = bond.getOtherAtom(atom);
	    atom.removeBond(bond);
	    other.removeBond(bond);
	    mol.removeBond(bond);
	}

	mol.removeAtom(atom);
    }

    /** The list of atoms that have been picked so far. */
    public DynamicArray pickedAtoms = new DynamicArray();

    /**
     * Handle a pick on the screen and
     * label the appropriate item.
     */
    public void handlePick(Atom pickedAtom){

	if(pickMode == NORMAL_PICK){
	    return;
	}

	//if(pickedAtoms.contains(pickedAtom)){
	//    pickedAtoms.remove(pickedAtom);
	//    return;
	//}

	// add the latest atom to the pick list
	//pickedAtoms.add(pickedAtom);

	if(pickMode == DISTANCE_PICK){
	    if(pickedAtoms.size() == 2){
		Atom atom0 = (Atom)pickedAtoms.get(0);
		Atom atom1 = (Atom)pickedAtoms.get(1);
		//addDistance(atom0, atom1);
		Distance distance =Distance.createDistanceMonitor(atom0, atom1);
		addDistance(distance);
		//distances.add(distance);
		pickedAtoms.removeAllElements();
	    }
	}else if(pickMode == ANGLE_PICK){

	    if(pickedAtoms.size() == 3){
		Atom atom0 = (Atom)pickedAtoms.get(0);
		Atom atom1 = (Atom)pickedAtoms.get(1);
		Atom atom2 = (Atom)pickedAtoms.get(2);
		addAngle(atom0, atom1, atom2);
		pickedAtoms.removeAllElements();
	    }
	}else if(pickMode == TORSION_PICK){

	    if(pickedAtoms.size() == 4){
		Atom atom0 = (Atom)pickedAtoms.get(0);
		Atom atom1 = (Atom)pickedAtoms.get(1);
		Atom atom2 = (Atom)pickedAtoms.get(2);
		Atom atom3 = (Atom)pickedAtoms.get(3);

		addTorsion(atom0, atom1, atom2, atom3);
		pickedAtoms.removeAllElements();
	    }
	}else{
	    System.out.println("Invalid pick mode");
	}
    }

    /** Handle a command that updates the properties of atoms. */
    public void handleUpdateCommand(Arguments args, FloatArray fa){
    }

    /** Interpret a new style map command. */
    public void handleMapCommand(String name, Arguments args){
	DynamicArray maps = getMaps(name);
	double hueStep    = args.getDouble("-huestep", -25.0);

	for(int i = 0; i < maps.size(); i++){
	    Map map = (Map)maps.get(i);

	    if(args.defined("-volumerender") || args.defined("-volumecolor") ||
	       args.defined("-volumemin") || args.defined("-volumemax")){
		if(args.defined("-volumerender")){
		    map.volumeRender = args.getBoolean("-volumerender", false);
		}
		if(args.defined("-volumecolor")){
		    map.volumeColor = args.getColor("-volumecolor", Color32.red);
		}
		if(args.defined("-volumemin")){
		    map.volumeMin = args.getDouble("-volumemin", 0.0);
		}
		if(args.defined("-volumemax")){
		    map.volumeMax = args.getDouble("-volumemax", 1.0);
		}

	    }else{

		System.out.println("changing " + map.getName());

		boolean changed = false;

		double centerValue = args.getDouble("-multicenter",
						    Double.NEGATIVE_INFINITY);

		if(centerValue != Double.NEGATIVE_INFINITY){
		    double spread = args.getDouble("-multispread", 1.0);

		    for(int j = 0; j < Map.MaximumContourLevels; j++){
			map.setContourLevel(j, centerValue);
			centerValue += spread;
		    }
		    changed = true;
		}

		String contourColourString = args.getString("-multicolor", null);

		double hsv[] = new double[3];

		if(contourColourString != null){
		    int colors[] = new int[3];

		    int centerColour =
			Color32.getColorFromName(contourColourString);

		    /*
                      Color32.packed2hsv(centerColour, hsv);

                      colors[0] = centerColour;
                      hsv[0] += hueStep;
                      colors[1] = Color32.hsv2packed(hsv);
                      hsv[0] += hueStep;
                      colors[2] = Color32.hsv2packed(hsv);
		    */

		    colors[0] = centerColour;
		    colors[1] = Color32.add(centerColour, Color32.scale(Color32.white, 64));
		    colors[2] = Color32.add(centerColour, Color32.scale(Color32.white, 128));

		    for(int j = 0; j < Map.MaximumContourLevels; j++){
			map.setContourColor(j, colors[j]);
		    }
		    changed = true;
		}

		if(changed){
		    for(int j = 0; j < Map.MaximumContourLevels; j++){
			contourMap(map, j);
		    }
		}
	    }
	}
    }

    /**
     * Handle a user command.
     * These are of the form
     *   user -class classname [args];
     * The method will load the class and then pass the
     * argument object to it and the moleculeViewer instance.
     */
    public void handleUserCommand(String command, Arguments args) throws Throwable{
	String className = command;

	if(className == null){
	    Log.error("command name is null");
	    return;
	}

	try {
	    // load the class and try and create one.
	    String userMethodName = args.getString("-method", "handleCommand");

	    Class userClass = Class.forName(className);

	    Object userObject = userClass.newInstance();

	    Class userMethodParams[] = {
		MoleculeViewer.class,
		MoleculeRenderer.class,
		Arguments.class
	    };

	    Object userParams[] = { moleculeViewer, this, args };

	    Method userMethod =
		userClass.getDeclaredMethod(userMethodName, userMethodParams);

	    try {
		userMethod.invoke(userObject, userParams);
	    }catch(InvocationTargetException e){
		//				throw e.getCause();
		Exception realException = (Exception)e.getTargetException();
		realException.printStackTrace();
		throw realException;
	    }

	    moleculeViewer.dirtyRepaint();

	}catch(Exception e){
	    Log.error("error loading user class " + className);
	    e.printStackTrace();
	}
    }

    /**
     * Interpret a molecule write command.
     *
     * write
     *  -file		'filename'
     *  -url		'urlname'
     *  -molecule	'molecule_name'
     * ;
     */
    public void handleWriteCommand(Arguments args){
	String typeString     = args.getString("-type", null);
	String urlString      = args.getString("-url", null);
	String fileString     = args.getString("-file", null);
	String moleculeString = args.getString("-molecule", null);
	String parameterName  = args.getString("-parameter", "pdb");
	boolean selected  = args.getBoolean("-selected", true);
	boolean debugConnect  = args.getBoolean("-debug", true);
	Molecule mol          = null;
	String type           = null;

	if(moleculeString != null){
	    mol = getMolecule(moleculeString);
	    if(mol == null){
		Log.error("no molecule " + moleculeString);
	    }
	}else if(selected){
	    // get molecule from selected atoms
	    // just takes first one it finds
	    AtomIterator iterator = getAtomIterator();

	    while(iterator.hasMoreElements()){
		Atom atom = iterator.getNextAtom();
		if(atom.isSelected()){
		    mol = atom.getMolecule();
		    break;
		}
	    }
	}else{
	    Log.error("no molecule specification specified");
	    return;
	}

	if(mol == null){
	    Log.error("no molecule matches specification");
	    return;
	}

	if(typeString != null && typeString.startsWith(".") == false){
	    typeString = "." + typeString;
	}

	type = MoleculeIO.getTypeFromExtension(typeString);

	if(urlString != null){
	    writeMoleculeToUrl(mol, urlString, debugConnect, type,
			       parameterName);
	}else if(fileString != null){
	    writeMoleculeToFile(mol, fileString, debugConnect, type);
	}else{
	    Log.error("you must specify -file or -url");
	}
    }

    /**
     * Write the molecule to the specified file.
     */
    public void writeMoleculeToFile(Molecule mol, String fileString,
				    boolean debugConnect, String type){
	FILE file = FILE.write(fileString);

	if(file == null){
	    Log.error("couldn't open " + fileString);
	    return;
	}

	MoleculeIO.write(mol, file, type);

	file.close();
    }

    /**
     * Write the molecule to the url.
     * 
     * This uses the POST method to communicate with the server.
     * The additional parameters are stripped off and sent
     * along with the pdb file, which is appended as
     * the pdb=.... parameter.
     *
     * This uses the POST implementation described in
     * 'Dodge the traps hiding in the URLConnection class'
     * http://www.javaworld.com/javaworld/jw-03-2001/jw-0323-traps.html
     *
     * I would suggest not changing anything to do with the
     * URLConnection methods unless you know much more about
     * this than I do.
     */
    public String writeMoleculeToUrl(Molecule mol, String urlString,
				     boolean debugConnect, String type,
				     String parameterName){
	String result = "";

	try {
	    URL url = new URL(urlString);

	    URLConnection con = url.openConnection();
	    //System.out.println("Received a : " + con.getClass().getName());

	    con.setDoInput(true);
	    con.setDoOutput(true);
	    con.setUseCaches(false);

	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    FILE file = new FILE((OutputStream)baos);

	    MoleculeIO.write(mol, file, type);

	    int questionPos = urlString.indexOf('?');

	    String urlSection = urlString;
	    String parameterSection = null;

	    if(questionPos != -1){
		urlSection = urlString.substring(0, questionPos);
		parameterSection = urlString.substring(questionPos + 1,
						       urlString.length());
	    }

	    String msg = "";

	    if(parameterSection != null){
		msg += parameterSection + "&";
	    }

	    msg += parameterName + "=" + baos.toString();

	    con.setRequestProperty("CONTENT_LENGTH", "" + msg.length()); 


	    OutputStream os = con.getOutputStream();

	    OutputStreamWriter osw = new OutputStreamWriter(os);
	    osw.write(msg);
	    osw.flush();
	    osw.close();

	    InputStream is = con.getInputStream();

	    // any response?
	    InputStreamReader isr = new InputStreamReader(is);
	    BufferedReader br = new BufferedReader(isr);
	    String line = null;

	    while ( (line = br.readLine()) != null) {
		if(!line.startsWith("OK")){
		    System.out.println(line);
		}

		result += line;
	    }

	    br.close();
	} catch (Throwable t) {
	    t.printStackTrace();
	}finally{
	    return result;
	}
    }

    /** Handle a distance command. */
    public void handleDistanceCommand(Arguments args){
	DynamicArray from   = (DynamicArray)args.get("-from");
	DynamicArray to     = (DynamicArray)args.get("-to");
	String mode         = args.getString("-mode", "pairs");
	double dmax         = args.getDouble("-dmax", 5.0);
	double contact      = args.getDouble("-contact", 0.5);
	String visible      = args.getString("-visible", null);

	if(visible != null){
	    String pattern = args.getString("-name", null);

	    for(int i = 0; i < distances.size(); i++){
		Distance d = (Distance)distances.get(i);

		String name = d.getString(Generic.Name, null);
		if(pattern == null || (name != null && match.matches(pattern, name))){
		    d.setBoolean(Distance.Visible, "on".equals(visible));
		}
	    }

	}else if(from != null && mode.equals("bumps")){
	    Distance d = new Distance();
	    configureDistance(d, args);
	    d.setInteger(Distance.Mode, Distance.Pairs);

	    addDistance(d);
	    //distances.add(d);

	    int fromCount = from.size();

	    for(int i = 0; i < fromCount; i++){
		Atom froma = (Atom)from.get(i);
		double fromr = froma.getVDWRadius();

		for(int j = i + 1; j < fromCount; j++){
		    Atom toa = (Atom)from.get(j);
		    double dist = froma.distance(toa);

		    if(dist < dmax && froma != toa &&
		       dist < (toa.getVDWRadius() + fromr + contact)){

			if(froma.connected121314(toa) == false){
			    d.group0.add(froma);
			    d.group1.add(toa);
			}
		    }
		}
	    }

	}else if(from != null && to != null){

	    System.out.println("fromCount "+ from.size());
	    System.out.println("toCount   "+ to.size());

	    // create a distance object
	    if(mode.equals("centroid")){
		Distance d = new Distance();
		d.setInteger(Distance.Mode, Distance.Centroids);
		//d.setMode(Distance.Centroids);

		for(int i = 0; i < from.size(); i++){
		    d.group0.add(from.get(i));
		}

		for(int i = 0; i < to.size(); i++){
		    d.group1.add(to.get(i));
		}

		if(d.getCenter0().distance(d.getCenter1()) < dmax){
		    configureDistance(d, args);

		    addDistance(d);
		    //distances.add(d);
		}
	    }else if(mode.equals("pairs") || mode.equals("nbpairs")){
		boolean allowBonded = true;
		if(mode.equals("nbpairs")){
		    allowBonded = false;
		}

		int fromCount = from.size();
		int toCount   = to.size();

		Distance d = new Distance();
		configureDistance(d, args);
		//d.setMode(Distance.Pairs);
		d.setInteger(Distance.Mode, Distance.Pairs);

		addDistance(d);
		//distances.add(d);

		for(int i = 0; i < fromCount; i++){
		    Atom froma = (Atom)from.get(i);
		    double fromr = froma.getVDWRadius();

		    for(int j = 0; j < toCount; j++){
			Atom toa = (Atom)to.get(j);
			double dist = froma.distance(toa);

			if(dist < dmax && froma != toa &&
			   dist < (toa.getVDWRadius() + fromr + contact)){
			    if(allowBonded || froma.connected121314(toa) == false){
				d.group0.add(froma);
				d.group1.add(toa);
			    }
			}
		    }
		}
	    }
	}else{
	    String deletePattern = args.getString("-delete", null);

	    if(deletePattern != null){
		deleteDistances(deletePattern);
	    }else{
		System.out.println("distance: you must specify "+
				   "-from and -to or -delete");
	    }
	}
    }

    /** Delete distances that match the pattern. */
    private void deleteDistances(String pattern){
	int distanceCount = distances.size();
	int deleted = 0;

	for(int j = distanceCount - 1; j >= 0; j--){
	    Distance d = (Distance)distances.get(j);
	    String name = d.getString(Generic.Name, null);
	    if(name != null && match.matches(pattern, name)){
		distances.removeElement(j);
		fireGenericRemovedEvent((Generic)d);
		deleted++;
	    }
	}

	System.out.println("deleted " + deleted + " distances");
    }

    /** Configure the settings of a distance object. */
    private void configureDistance(Distance d, Arguments args){
	//d.radius = args.getDouble("-radius", d.radius);
	//d.on     = args.getDouble("-on",     d.on);
	//d.off    = args.getDouble("-off",    d.off);
	//d.format = args.getString("-format", d.format);
	//d.name   = args.getString("-name",   d.name);
	d.setBoolean(Distance.Visible, true);
	d.setDouble(Distance.Radius, args.getDouble("-radius", 0.0));
	d.setDouble(Distance.On,     args.getDouble("-on",     0.0));
	d.setDouble(Distance.Off,    args.getDouble("-off",    0.0));
	d.setString(Distance.Format, args.getString("-format", null));
	d.setString(Generic.Name,    args.getString("-name", null));

	String colour = args.getString("-colour", "white");
	d.set(Distance.Color, new Color(Color32.getColorFromName(colour)));

	colour = args.getString("-labelcolour", "white");
	d.set(Distance.LabelColor, new Color(Color32.getColorFromName(colour)));
    }

    /**
     * Handle a hydrogen bond command.
     */
    public void handleHbondCommand(Arguments args){
	DynamicArray calculateAtoms = (DynamicArray)args.get("-calculate");
	DynamicArray deleteAtoms    = (DynamicArray)args.get("-delete");
	boolean deleteAll           = args.getBoolean("-deleteall", false);

	if(deleteAll){
	    hbonds.removeAllElements();
	}

	if(calculateAtoms != null){
	    calculateHbonds(calculateAtoms, args);
	}else if(deleteAtoms != null){
	    deleteHbonds(deleteAtoms, args);
	}
    }

    /** Calculate hydrogen bonds. */
    public void calculateHbonds(DynamicArray atoms, Arguments args){
	Lattice l             = new Lattice(5.5);
	int atomCount         = atoms.size();
	double hbondConstant  = args.getDouble("hbond.constant", -999.0);
	double hbondCutoff    = args.getDouble("hbond.cutoff", -999.0);

	System.out.println("hbondCutoff " + hbondCutoff);

	for(int i = 0; i < atomCount; i++){
	    Atom a = (Atom)atoms.get(i);

	    if(a.getAtomLabel().equals("O")){
		l.add(i, a.x, a.y, a.z);
	    }
	}

	// space for the neighbours
	IntArray neighbours = new IntArray();

	for(int i = 0; i < atomCount; i++){
	    Atom a = (Atom)atoms.get(i);

	    if(a.getAtomLabel().equals("N")){
		Atom n = a;
		Point3d h = getAmideHydrogen(a);

		neighbours.removeAllElements();

		l.getPossibleNeighbours(Lattice.Undefined,
					n.x, n.y, n.z,
					neighbours, true);

		int neighbourCount = neighbours.size();

		for(int j = 0; j < neighbourCount; j++){
		    Atom o = (Atom)atoms.get(neighbours.get(j));
		    Atom c = o.getBondedAtom("C");

		    double e = hbondEnergy(n, h, o, c, hbondConstant);

		    if(e < hbondCutoff){
			Bond hbond = Bond.create();
			hbond.setFirstAtom(n);
			hbond.setSecondAtom(o);
			hbonds.add(hbond);
		    }
		}
	    }
	}

	System.out.println("number of hbonds " + hbonds.size());
    }

    /** Delete hbonds that involve atoms in the selection. */
    private void deleteHbonds(DynamicArray atoms, Arguments args){
	int hbondCount   = hbonds.size();
	int removedCount = 0;

	for(int i = hbondCount - 1; i >= 0; i--){
	    Bond hbond = (Bond)hbonds.get(i);
	    Atom firstAtom = hbond.getFirstAtom();
	    Atom secondAtom = hbond.getSecondAtom();

	    if(atoms.contains(firstAtom) || atoms.contains(secondAtom)){
		hbonds.removeElement(i);
		removedCount++;
	    }
	}

	FILE.out.print("removed %d hydrogen bonds", removedCount);
    }

    /**
     * Calculate the energy of an hbond as defined in
     * 
     * Kabsch and Sander, 'Dictionary of Protein Secondary
     * Structure: Pattern Recognition of Hydrogen-Bonded and
     * Geometrical Features', Biopolymers, 22, 2577-2637 (1983).
     */
    public static double hbondEnergy(Atom n, Point3d h,
				     Atom o, Atom c,
				     double fq1q2){
	if(n != null && h != null &&
	   o != null && c != null){
	    if(c.hasBond(n) == false ||
	       c.hasBond(o) == false){

		double rON = o.distance(n);
		double rCH = c.distance(h);
		double rOH = o.distance(h);
		double rCN = c.distance(n);

		Point3d nh = Point3d.unitVector(n, h);
		Point3d no = Point3d.unitVector(n, o);
		Point3d co = Point3d.unitVector(c, o);

		double hno = (180./Math.PI)* Math.acos(nh.dot(no));
		double hnoc = (180./Math.PI)* Math.acos(nh.dot(co));

		if(hno > 63.0 || hnoc < 90.0){
		    return 10000.0;
		}

		return fq1q2 * (1./rON + 1./rCH - 1./rOH - 1./rCN);
	    }
	}

	return 10000.0;
    }

    /** Calculate the hdyrogen position for this nitrogen. */
    private static Point3d getAmideHydrogen(Atom N){
	if(N == null){
	    return null;
	}

	Residue r = N.getResidue();

	Atom CA = N.getBondedAtom("CA");
	Atom C = N.getBondedAtom("C");

	if(N == null || CA == null || C == null){
	    return null;
	}

	Point3d hpos = new Point3d();
	hpos.set(N);
	hpos.subtract(C);
	hpos.add(N);
	hpos.subtract(CA);

	hpos.normalise();
	hpos.scale(1.04);

	hpos.add(N);

	Molecule m = N.getMolecule();

	//
	Atom ah = m.addAtom();
	ah.set(hpos);

	return hpos;
    }

    /**
     * Interpret a texture command.
     */
    public void handleTextureCommand(Arguments args){
	String name = args.getString("-name", null);
	Texture t = null;

	if(name != null){
	    t = (Texture)renderer.textures.get(name);

	    if(t == null){
		t = new Texture();
		renderer.textures.put(name, t);
		System.out.println("adding new texture " + name);
	    }
	}else{
	    System.out.println("handleTextureCommand: no texture specified [-name]");
	    return;
	}

	String coord = args.getString("-coord", null);

	int tc = 0;

	if(coord != null){
	    if(coord.equals("u")){
		tc = 0;
	    }else if(coord.equals("v")){
		tc = 1;
	    }else{
		tc = -1;
	    }
	}

	if(tc == -1){
	    System.out.println("handleTextureCommand: illegal texture coord " + coord);
	    return;
	}

	String values = args.getString("-values", null);

	if(values != null){
	    String colors[] = FILE.split(values);
	    t.fillValues(colors, tc);
	}
    }

    /** Handle a light command. */
    public void handleLightCommand(int l, Arguments args){
	if(l < 0 || l >= renderer.lights.size()){
	    return;
	}

	Light light = (Light)renderer.lights.get(l);
	boolean changed = false;

	if(light == null){
	    return;
	}

	if(args.defined("-x")){
	    light.pos[0] = args.getDouble("-x", 0.0);
	    changed = true;
	}
	if(args.defined("-y")){
	    light.pos[1] = args.getDouble("-y", 0.0);
	    changed = true;
	}
	if(args.defined("-z")){
	    light.pos[2] = args.getDouble("-z", 1.0);
	    changed = true;
	}

	if(args.defined("-on")){
	    light.on = args.getBoolean("-on", true);
	    changed = true;
	}

	if(args.defined("-specularint")){
	    int intensity = args.getInteger("-specularint", 64);
	    light.specular = Color32.pack(intensity, intensity, intensity);
	    changed = true;
	}

	if(args.defined("-phongpower")){
	    double intensity = args.getDouble("-phongpower", 16.0);
	    light.power = intensity;
	    changed = true;
	}

	if(args.defined("-diffuseint")){
	    int intensity = args.getInteger("-diffuseint", 128);
	    light.diffuse = Color32.pack(intensity, intensity, intensity);
	    changed = true;
	}

	if(changed){
	    light.normalisePos();
	    renderer.calculateLightMap();
	    moleculeViewer.dirtyRepaint();
	}
    }

    /** Handle an object command. */
    public void handleObjectCommand(String namePattern, Arguments args){
	DynamicArray objects = renderer.getGraphicalObjects(namePattern);
	int objectCount = objects.size();

	for(int i = 0; i < objectCount; i++){
	    Tmesh tmesh = (Tmesh)objects.get(i);

	    if(args.defined("-map")){
		DynamicArray mapAtoms = (DynamicArray)args.get("-map");
		// back ground color
		int defaultColor = tmesh.getColor();
		if(args.defined("-defaultcolor")){
		    String defaultColorName = args.getString("-defaultcolor", "white");
		    defaultColor = Color32.getColorFromName(defaultColorName);
		}
		// maximum distance before we become default color
		double dmax = args.getDouble("-dmax", 1.5);
		double wmax = args.getDouble("-wmax", 1000.0);

		// wmax is used as 1/(d + wmax)
		wmax = 1./wmax;

		mapAtomColors(tmesh, mapAtoms, defaultColor, dmax, wmax);

		if(tmesh.lines != null){
		    mapAtomColors(tmesh.lines, mapAtoms, defaultColor, dmax, wmax);
		}
		if(tmesh.spheres != null){
		    mapAtomColors(tmesh.spheres, mapAtoms, defaultColor, dmax, wmax);
		}
		if(tmesh.cylinders != null){
		    mapAtomColors(tmesh.cylinders, mapAtoms, defaultColor, dmax, wmax);
		}
	    }
	}
    }

    /** Map the colors of a set of atoms to the object. */
    public void mapAtomColors(Tmesh tmesh, DynamicArray mapAtoms,
			      int defaultColor, double dmax, double wmax){
	double maxRadius = 0.0;

	int atomCount = mapAtoms.size();

	for(int a = 0; a < atomCount; a++){
	    Atom atom = (Atom)mapAtoms.get(a);
	    double r = atom.getVDWRadius();
	    if(r > maxRadius){
		maxRadius = r;
	    }
	}

	maxRadius += dmax;

	// build the lattice structure for atom lookup
	Lattice l = new Lattice(maxRadius + 0.01);

	for(int a = 0; a < atomCount; a++){
	    Atom atom = (Atom)mapAtoms.get(a);
	    l.add(a, atom.x, atom.y, atom.z);
	}


	// find the nearest atom for each tmesh point
	int pointCount = tmesh.np;

	IntArray neighbours = new IntArray();
	IntArray nearest = new IntArray();
	DoubleArray distances = new DoubleArray();

	for(int i = 0; i < pointCount; i++){
	    neighbours.removeAllElements();
	    double tx = tmesh.x[i];
	    double ty = tmesh.y[i];
	    double tz = tmesh.z[i];

	    l.getPossibleNeighbours(-1, tx, ty, tz, neighbours, true);

	    int neighbourCount = neighbours.size();

	    Atom nearestAtom = null;
	    double dnear = 1.e10;

	    nearest.removeAllElements();
	    distances.removeAllElements();

	    for(int j = 0; j < neighbourCount; j++){
		int neighbour = neighbours.get(j);
		Atom atom = (Atom)mapAtoms.get(neighbour);
		double ar = atom.getVDWRadius();
		double dx = atom.x - tx;
		double dy = atom.y - ty;
		double dz = atom.z - tz;
		double d = Math.sqrt(dx*dx + dy*dy + dz*dz);

		if(d < ar + dmax){
		    nearest.add(neighbour);
		    d -= ar;
		    d += wmax;
		    //d = 1. / Math.sqrt(Math.abs(d)) - 1./Math.sqrt(dmax);
		    d = 1. / Math.abs(d) - 1./dmax;
		    //d = (dmax - d)/dmax;
		    //d *= d*d;
		    //addDistance(d);
		    distances.add(d);
		    if(d < dnear){
			nearestAtom = atom;
			dnear = d;
		    }
		}
	    }

	    //if(nearestAtom != null){
	    //    tmesh.vcolor[i] = nearestAtom.getColor();
	    //}else{
	    //    tmesh.vcolor[i] = defaultColor;
	    //}

	    int nearCount = nearest.size();

	    if(nearCount == 0){
		tmesh.vcolor[i] = defaultColor;
	    }else if(nearCount == 1){
		int a = nearest.get(0);
		Atom atom = (Atom)mapAtoms.get(a);
		int color = atom.getColor();
		tmesh.vcolor[i] = color;
	    }else{
		double sum = 0.0;
		for(int nn = 0; nn < nearCount; nn++){
		    //sum += ddd*ddd*ddd;
		    sum += distances.get(nn);
		}

		int r = 0, g = 0, b = 0;
		double total = 0.0;

		for(int nn = 0; nn < nearCount; nn++){
		    int a = nearest.get(nn);
		    Atom atom = (Atom)mapAtoms.get(a);
		    int color = atom.getColor();

		    double comp = distances.get(nn)/sum;

		    r += comp * Color32.getRed(color);
		    g += comp * Color32.getGreen(color);
		    b += comp * Color32.getBlue(color);
		}

		tmesh.vcolor[i] = Color32.getClampColor(r, g, b);
	    }
	}

	tmesh.setColorStyle(Tmesh.VertexColor);
    }

    /** Name of properties resource for the MoleculeRenderer class. */
    private static String moleculeRendererProperties =
	"MoleculeRenderer.properties";

    /** The properties for the moleculerenderer. */
    private Properties properties = null;

    /** Initialise the MoleculeRenderer. */
    public void initialise(){
	properties =
	    FILE.loadProperties(moleculeRendererProperties);

	String value = null;

	value = properties.getProperty(displayRadiusProperty);
	if(value != null){
	    displayRadius = FILE.readDouble(value);
	}

	value = properties.getProperty(symmetryRadiusProperty);
	if(value != null){
	    symmetryRadius = FILE.readDouble(value);
	}

	value = properties.getProperty(mapRadiusProperty);
	if(value != null){
	    mapRadius = FILE.readDouble(value);
	}

	value = properties.getProperty(contourLevelsProperty);
	if(value != null){
	    setupContourLevels(value);
	}

	/*
	  value = properties.getProperty(standardAminoacidsProperty);
	  if(value != null){
	  String aminoacids[] = FILE.split(value, ",");

	  for(int i = 0; i < aminoacids.length; i++){
	  aminoacids[i] = aminoacids[i].trim();
	  }

	  Residue.setStandardAminoAcids(aminoacids);
	  }
	*/
    }

    /** The default status string. */
    private String defaultStatusLabelFormat = null;

    /** Generate a label for the status bar. */
    private String generateStatusLabel(Atom atom){
	if(defaultStatusLabelFormat == null){
	    defaultStatusLabelFormat = Settings.getString("config", "atom.long.format");
	    if(defaultStatusLabelFormat == null){
		// no config so set it here
		defaultStatusLabelFormat = "%a %R %c:%r ID=%i  X=%x Y=%y Z=%z  O=%o B=%b %m";
	    }
	}

	return atom.generateLabel(defaultStatusLabelFormat);
    }

    /** Set the status label format. */
    public void setStatusLabelFormat(String s){
	defaultStatusLabelFormat = s;
    }

    /** Get the status label format. */
    public String getStatusLabelFormat(){
	return defaultStatusLabelFormat;
    }

    /** Set the atom that will produce the status label. */
    public void setStatusAtom(Atom a){
	if(a == null){
	    renderer.setStatusString(null);
	}else{
	    renderer.setStatusString(generateStatusLabel(a));
	}
    }

    /** Should we display status strings. */
    public void setDisplayStatusString(boolean b){
	renderer.setDisplayStatusString(b);
    }

    /** Set the radius allowing for minimum value for renderer. */
    public void setRadius(double radius){
	if(radius < minimumDisplayRadius){
	    radius = minimumDisplayRadius;
	}

	displayRadius = radius;

	renderer.setRadius(radius);
    }

    /** Set the clip distance allowing for minimum clip distance. */
    public void setClip(double distance){
	if(distance < minimumClipDistance){
	    distance = minimumClipDistance;
	}

	//System.out.println("setClip " + distance);

	//Exception e = new Exception();
	//e.printStackTrace();

	renderer.setClip(distance);
    }

    public void removeGraphicalObjectsBeginningWith(String prefix){
	renderer.removeGraphicalObjectsBeginningWith(prefix);
    }

    public void removeGraphicalObjects(String prefix){
	renderer.removeGraphicalObjects(prefix);
    }

    public void addGraphicalObject(Tmesh object){
	renderer.addGraphicalObject(object);
    }

    /** Setup the contour levels from the passed string. */
    public void setupContourLevels(String contourLevels){
	resetContourLevels();

	String levels[] = FILE.split(contourLevels, ",");

	for(int i = 0; i < levels.length; i++){
	    String attributes[] = FILE.split(levels[i], ":");

	    double level = FILE.readDouble(attributes[0].trim());
	    int color = Color32.white;

	    if(attributes.length >= 2){
		color = Color32.getColorFromName(attributes[1].trim());
	    }

	    boolean display = false;

	    if(attributes.length >= 3){
		if(attributes[2].equals("display")){
		    display = true;
		}
	    }

	    addContourLevel(level, color, display, Map.Lines);
	}
    }

    /** Set whether we display symmetry. */
    public void setSymmetry(boolean state){
	displaySymmetry = state;

	if(displaySymmetry){
	    generateSymmetry();
	}else{
	    removeSymmetry();
	}
    }

    /** Remove the stored space group information. */
    public void removeSpaceGroup(){
	symmetry = null;
    }

    /** Add a MoleculeRendererListener. */
    public void addMoleculeRendererListener(MoleculeRendererListener l){
	if(moleculeRendererListeners == null){
	    moleculeRendererListeners = new DynamicArray();
	}
	moleculeRendererListeners.add(l);
    }

    /** Remove a MoleculeRendererListener. */
    public void removeMoleculeRendererListener(MoleculeRendererListener l){
	if(moleculeRendererListeners != null){
	    moleculeRendererListeners.remove(l);
	    System.out.println("moleculeRendererListeners " + moleculeRendererListeners.size());
	}
    }

    /** Fire a molecule added event. */
    public void fireMoleculeAddedEvent(Molecule molecule){
	if(moleculeRendererListeners != null){
	    for(int i = 0; i < moleculeRendererListeners.size(); i++){
		MoleculeRendererListener l =
		    (MoleculeRendererListener)moleculeRendererListeners.get(i);

		l.moleculeAdded(this, molecule);
	    }
	}
    }

    /** Fire a molecule removed event. */
    public void fireMoleculeRemovedEvent(Molecule molecule){
	if(moleculeRendererListeners != null){
	    for(int i = 0; i < moleculeRendererListeners.size(); i++){
		MoleculeRendererListener l =
		    (MoleculeRendererListener)moleculeRendererListeners.get(i);
		//print.f("notifying " + l);
		l.moleculeRemoved(this, molecule);
	    }
	}
    }

    /** Fire a distance added event. */
    public void fireGenericAddedEvent(Generic generic){
	if(moleculeRendererListeners != null){
	    for(int i = 0; i < moleculeRendererListeners.size(); i++){
		MoleculeRendererListener l =
		    (MoleculeRendererListener)moleculeRendererListeners.get(i);

		l.genericAdded(this, generic);
	    }
	}
    }

    /** Fire a generic removed event. */
    public void fireGenericRemovedEvent(Generic generic){
	if(moleculeRendererListeners != null){
	    for(int i = 0; i < moleculeRendererListeners.size(); i++){
		MoleculeRendererListener l =
		    (MoleculeRendererListener)moleculeRendererListeners.get(i);
		l.genericRemoved(this, generic);
	    }
	}
    }

    /** Fire a map added event. */
    public void fireMapAddedEvent(Map map){
	if(moleculeRendererListeners != null){
	    for(int i = 0; i < moleculeRendererListeners.size(); i++){
		MoleculeRendererListener l =
		    (MoleculeRendererListener)moleculeRendererListeners.get(i);

		l.mapAdded(this, map);
	    }
	}
    }

    /** Fire a map added event. */
    public void fireMapRemovedEvent(Map map){
	if(moleculeRendererListeners != null){
	    for(int i = 0; i < moleculeRendererListeners.size(); i++){
		MoleculeRendererListener l =
		    (MoleculeRendererListener)moleculeRendererListeners.get(i);

		l.mapRemoved(this, map);
	    }
	}
    }

    /** Fire an atom selected event. */
    public void fireAtomSelectedEvent(Atom atom){
	if(moleculeRendererListeners != null){
	    for(int i = 0; i < moleculeRendererListeners.size(); i++){
		MoleculeRendererListener l =
		    (MoleculeRendererListener)moleculeRendererListeners.get(i);

		l.atomSelected(this, atom);
	    }
	}
    }

    /** Add a molecule to the scene. */
    public void addMolecule(Molecule molecule){
	molecules.add(molecule);

	if(getMoleculeCount() == 1){
	    resetView();
	    //setCenter(null);
	    //renderer.setRadius(0.0);
	    initialiseCenter();
	}

	fireMoleculeAddedEvent(molecule);
    }

    public boolean displayAsCylinders = false;

    /** Add a molecule to the scene. */
    public void addMolecule(String filename, String name){
	boolean sdf = (filename.toLowerCase().indexOf(".sd") != -1);

	// only implement this file loading semantics
	// for applications, its too confusing in an applet.
	if(moleculeViewer.isApplication() == false){
	    sdf = false;
	}

	System.out.println("sdf " + sdf);

	if(sdf){
	    FILE f = FILE.open(filename);

	    if(f == null){
		System.out.println("addMolecule: couldn't load " + filename);
	    }

	    Molecule molecule = null;

	    int count = 0;

	    int start = getMoleculeCount();

	    while((molecule = MoleculeIO.readMDLMol(f)) != null){
		String molname = molecule.getName();

		if(molname == null || molname.length() == 0){
		    molname = filename + "_" + count;
		    molecule.setName(molname);
		}

		System.out.println("molecule " + count + " name " + molecule.getName());

		molecule.setFilename(filename);

		addMolecule(molecule);

		int molNum = getMoleculeCount();

		count++;
	    }

	    int finish = getMoleculeCount();

	    String sel = "molecule";

	    for(int m = start; m < finish; m++){
		sel += " '#" + m + "'";
	    }

	    String command = "cylinder_radius 0.09 " + sel + ";";
	    command += "display cylinders on " + sel + ";";

	    execute(command);

	    if(finish - start == 1){
		// added a single mol from the sd file
		// rename it to be the name that was given

		Molecule mol = getMolecule(start);

		mol.setName(name);

		System.out.println("renamed single molecule sd to " + name);
	    }

	}else{

	    Molecule molecule = MoleculeIO.read(filename);

	    if(molecule != null){
		molecule.setFilename(filename);
		molecule.setName(name);

		addMolecule(molecule);
	    }else{
		System.out.println("addMolecule: couldn't load " + filename);
	    }
	}
    }

    /** Return the total number of molecules. */
    public int getMoleculeCount(){
	return molecules.size();
    }

    /** Return the specified molecule. */
    public Molecule getMolecule(int i){
	return (Molecule)molecules.get(i);
    }

    /** Return the first molecule that matches the specified name. */
    public Molecule getMolecule(String name){
	int moleculeCount = getMoleculeCount();
	for(int i = 0; i < moleculeCount; i++){
	    Molecule molecule = getMolecule(i);
	    String moleculeName = molecule.getName();
	    if(name.equals(moleculeName)){
		return molecule;
	    }
	}

	return null;
    }

    /** Return the DynamicArray of molecules. */
    public DynamicArray getMolecules(){
	return molecules;
    }

    /** Remove a molecule. */
    public void removeMoleculeByName(String pattern){

	int moleculeCount = getMoleculeCount();
	for(int m = moleculeCount - 1; m >= 0; m--){
	    Molecule molecule = getMolecule(m);
	    String moleculeName = molecule.getName();

	    if(pattern.equals(moleculeName)){
		molecules.remove(molecule);
		System.out.println("removed " + moleculeName);

		fireMoleculeRemovedEvent(molecule);
	    }
	}

	return;
    }

    /** Remove a molecule. */
    public void removeMolecule(String pattern){

	int moleculeCount = getMoleculeCount();
	for(int m = moleculeCount - 1; m >= 0; m--){
	    Molecule molecule = getMolecule(m);
	    String moleculeName = molecule.getName();

	    //if(match.matches(pattern, moleculeName)){
	    if(moleculeMatches(pattern, molecule)){
		//if(pattern.equals(moleculeName)){
		molecules.remove(molecule);
		//System.out.println("removed " + moleculeName);

		fireMoleculeRemovedEvent(molecule);
	    }
	}

	return;
    }

    public boolean moleculeMatches(String pattern, Molecule mol){
	if(pattern.startsWith("#")){
	    int moleculeCount = getMoleculeCount();

	    int id = Integer.parseInt(pattern.substring(1)) % moleculeCount;

	    for(int m = 0; m < moleculeCount; m++){
		if(getMolecule(m) == mol && m == id){
		    return true;
		}
	    }
	}else{
	    return match.matches(pattern, mol.getName());
	}

	return false;
    }

    /** Return the name for the next molecule. */
    public String getNextMoleculeName(){
	int mol = getMoleculeCount();
	String molName = null;

	do {
	    molName = "mol" + mol;
	    mol++;
	} while(getMolecule(molName) != null);

	//System.out.println("returning molecule name " + molName);

	return molName;
    }

    /** Remove maps. */
    public void removeMap(String pattern){
	//System.out.println("removeMolecule pattern is " + pattern);

	int mapCount = getMapCount();
	for(int m = mapCount - 1; m >= 0; m--){
	    Map map = getMap(m);
	    String mapName = map.getName();

	    //System.out.println("checking map |" + mapName + "|");
	    if(match.matches(pattern, mapName)){
		String mapPrefix = mapName;
		maps.remove(map);
		removeGraphicalObjectsBeginningWith(mapPrefix);
		//System.out.println("matched " + mapName);

		//fireMoleculeRemovedEvent(molecule);
	    }
	}

	return;
    }

    public void removeMap(Map map){
	if(maps.contains(map)){
	    String name = map.getName();
	    removeGraphicalObjectsBeginningWith(name);
	    maps.remove(map);
	}
    }

    /** Add a map to the scene. */
    public void addMap(Map map){

	maps.add(map);
	int mapCount = maps.size();

	String mapType = null;

	String filename = map.getFile();

	// try and guess map type from name
	if(filename != null){
	    String lowercase = filename.toLowerCase();
	    if(lowercase.indexOf("2fofc") != -1){
		mapType = "2fofc";
	    }else if(lowercase.indexOf("fofc") != -1){
		mapType = "fofc";
	    }
	}

	// we couldn't figure it out from the name
	// so if it is first map make it a 2fofc
	// if it is second map make it fofc
	if(mapType == null){
	    if(mapCount == 1){
		mapType = "2fofc";
	    }else{
		mapType = "fofc";
	    }
	}

	if(mapType == null){
	    mapType = "fofc";
	}

	Log.info("mapType " + mapType);

	if(map.initialiseContours){
	    for(int i = 0; i < contourLevelCount; i++){
		String prefix = mapType + "." + i;
		String levelLabel  = prefix + ".level";
		String levelString = (String)properties.get(levelLabel);
		double level = 3.0;
		if(levelString != null){
		    level = FILE.readDouble(levelString);
		}
		map.setContourLevel(i, level);

		String colourLabel  = prefix + ".colour";
		String colourString = (String)properties.get(colourLabel);
		int colour = Color32.blue;
		if(colourString != null){
		    colour = Color32.getColorFromName(colourString);
		}
		map.setContourColor(i, colour);

		String displayLabel  = prefix + ".displayed";
		String displayString = (String)properties.get(displayLabel);
		boolean display = false;
		if(displayString != null){
		    if(displayString.equals("true")){
			display = true;
		    }
		}

		map.setContourDisplayed(i, display);

		String styleLabel  = prefix + ".style";
		String styleString = (String)properties.get(styleLabel);
		int style = Map.Lines;
		if(styleString != null){
		    if(styleString.equals("solid")){
			style = Map.Surface;
		    }else if(styleString.equals("lines")){
			style = Map.Lines;
		    }
		}

		map.setContourStyle(i, style);
	    }
	}

	for(int i = 0; i < contourLevelCount; i++){
	    // create the graphical object once
	    String contourName =
		getContourGraphicalObjectName(map, i);

	    Tmesh contourObject = new Tmesh();
	    contourObject.setName(contourName);
	    addGraphicalObject(contourObject);
	}

	// hack to change contour levels of second map
	/*
	  if(mapCount == 2){
	  if(map.initialiseContours){
	  map.setContourColor(0, Color32.blue);
	  map.setContourLevel(0, 3.0);
	  map.setContourDisplayed(0, true);

	  map.setContourColor(1, Color32.yellow);
	  map.setContourDisplayed(1, false);
	  map.setContourColor(2, Color32.red);
	  map.setContourLevel(2, -3.0);
	  map.setContourDisplayed(2, true);
	  }
	  }
	*/
	// fix the rereading of maps when we load another map
	//generateMaps();
	readMap(map);

	for(int j = 0; j < Map.MaximumContourLevels; j++){
	    contourMap(map, j);
	}

	fireMapAddedEvent(map);
    }

    /** Return the name for the next molecule. */
    public String getNextMapName(){
	int map = getMapCount();
	String mapName = null;

	do {
	    mapName = "map" + map;
	    map++;
	} while(getMolecule(mapName) != null);

	//System.out.println("returning map name " + mapName);

	return mapName;
    }

    /** Change the line width. */
    public void setMapContourTransparency(String name, int contour, int t){
	Map map = getMap(name);

	if(map != null){
	    Tmesh contourObject =
		getContourGraphicalObject(map, contour);
	    if(contourObject != null){
		contourObject.setTransparency(t);
	    }
	}
    }


    /** Change the line width. */
    public void setMapContourLineWidth(String name, int contour, double w){
	Map map = getMap(name);

	if(map != null){
	    Tmesh contourObject =
		getContourGraphicalObject(map, contour);
	    if(contourObject != null){
		contourObject.setLineWidth(w);
	    }
	}
    }


    /** Change the value of a contour level. */
    public void setMapContourLevel(String name, int contour, double level){
	Map map = getMap(name);

	if(map != null){
	    map.setContourLevel(contour, level);
	    //map.setContourDisplayed(contour, true);
	    if(map.getContourDisplayed(contour)){
		map.setCenter(renderer.getCenter());
		map.setRadius(mapRadius);
		//determineRegion(map);
		contourMap(map, contour);
	    }else{
		Tmesh contourObject =
		    getContourGraphicalObject(map, contour);

		//removeGraphicalObjectsBeginningWith(contourName);
	    }
	}
    }

    /** Change the value of a contour level. */
    public void setMapContourColour(String name, int contour, int colour){
	Map map = getMap(name);

	if(map != null){
	    map.setContourColor(contour, colour);
	    //map.setContourDisplayed(contour, true);
	    if(map.getContourDisplayed(contour)){
		map.setCenter(renderer.getCenter());
		map.setRadius(mapRadius);
		//determineRegion(map);
		contourMap(map, contour);
	    }
	}
    }

    /** Turn contour level on/off of a contour level. */
    public void setMapContourDisplayed(String name,
				       int contour, int visible){
	Map map = getMap(name);

	if(map != null){
	    if(visible == 2){
		// toggle
		boolean current = map.getContourDisplayed(contour);
		if(current){
		    map.setContourDisplayed(contour, false);
		}else{
		    map.setContourDisplayed(contour, true);
		}
	    }else{
		boolean vis = false;
		if(visible == 1){
		    vis = true;
		}

		map.setContourDisplayed(contour, vis);
	    }
	    if(map.getContourDisplayed(contour)){
		map.setCenter(renderer.getCenter());
		map.setRadius(mapRadius);
		//determineRegion(map);
		contourMap(map, contour);
	    }else{
		Tmesh contourObject = 
		    getContourGraphicalObject(map, contour);
		contourObject.setVisible(false);
		//String contourName =
		//    getContourGraphicalObjectName(map, contour);

		//removeGraphicalObjectsBeginningWith(contourName);
	    }
	}
    }

    /** Return the total count of maps. */
    public int getMapCount(){
	return maps.size();
    }

    /** Return the specified map. */
    public Map getMap(int i){
	return (Map)maps.get(i);
    }

    /** Return the specified map from its name. */
    public Map getMap(String name){
	for(int i = 0; i < getMapCount(); i++){
	    Map map = getMap(i);
	    if(map.getName().equals(name)){
		return map;
	    }
	}

	return null;
    }

    /** Return the specified map from its name. */
    public DynamicArray getMaps(String name){
	DynamicArray mapArray = new DynamicArray();

	for(int i = 0; i < getMapCount(); i++){
	    Map map = getMap(i);
	    if(match.matches(name, map.getName())){
		mapArray.add(map);
	    }
	}

	return mapArray;
    }

    /** Get a residue iterator. */
    public ResidueIterator getResidueIterator(){
	return new ResidueIterator(this);
    }

    /** Get a bond iterator. */
    public BondIterator getBondIterator(){
	return new BondIterator(this);
    }

    /** Get a atom iterator. */
    public AtomIterator getAtomIterator(){
	return new AtomIterator(this);
    }

    /** Should we display the atom label. */
    private boolean displayAtomLabel = true;

    /** Set whether we should display the atom label. */
    public void setDisplayAtomLabel(boolean b){
	displayAtomLabel = b;
    }

    /** Add a labelled atom. */
    public void addLabelledAtom(Atom atom){
	if(atom.isLabelled()){
	    atom.setLabelled(false);
	}else{
	    atom.setLabelled(true);
	}
	//if(labelledAtoms.contains(atom) == false){
	//	labelledAtoms.add(atom);
	//}else{
	//	labelledAtoms.remove(atom);
	//}
    }

    /** Do we already have a label for this atom. */
    public boolean labelledAtomsContains(Atom atom){
	//return labelledAtoms.contains(atom);
	return false;
    }

    /** Add a selected atom. */
    public void addSelectedAtom(Atom atom){
	//if(atom.isSelected()){
	//	atom.setSelected(false);
	//}else{
	//	atom.setSelected(true);
	//}
	String atomSelect = atom.selectStatement();

	if(selectedAtoms.contains(atom) == false){
	    selectedAtoms.add(atom);
	    //atom.setSelected(true);
	    //System.out.println("|" + atomSelect + "|");
	    execute("append " + atomSelect + ";");
	    fireAtomSelectedEvent(atom);

	}else{
	    selectedAtoms.remove(atom);
	    //atom.setSelected(false);
	    execute("exclude " + atomSelect + ";");
	}

	if(pickedAtoms.contains(atom)){
	    pickedAtoms.remove(atom);
	}else{
	    pickedAtoms.add(atom);
	}
    }

    /** Add a pair of bump atoms. */
    public void addBumpPair(Atom atom1, Atom atom2){
	// only add if they are in the same molecule
	if((bumpInSameMolecule && atom1.getMolecule() == atom2.getMolecule()) ||
	   bumpInSameMolecule == false){
	    bumpAtoms.add(atom1);
	    bumpAtoms.add(atom2);
	}
    }

    /** Generate the bump atoms around the specified atom. */
    public void generateBumps(Atom atom){
	DynamicArray bumpAtoms = new DynamicArray();

	bumpAtoms.add(atom);

	generateBumps(bumpAtoms);
    }

    /** Generate bumps for the specified set of atoms. */
    public void generateBumps(DynamicArray bumpAtoms){
	removeAllBumpAtoms();

	int bumpAtomCount = bumpAtoms.size();

	if(displayBumps && bumpAtomCount > 0){
	    DynamicArray sphereAtoms =
		getAtomsAroundSelection(bumpAtoms, 5.0, true);
	    int sphereAtomCount = sphereAtoms.size();

	    for(int i = 0; i < sphereAtomCount; i++){
		Atom sphereAtom = (Atom)sphereAtoms.get(i);
		double vdwRadius = sphereAtom.getVDWRadius();
		for(int a = 0; a < bumpAtomCount; a++){
		    Atom atom = (Atom)bumpAtoms.get(a);
		    double atomVDWRadius = atom.getVDWRadius();
		    if(sphereAtom != atom &&
		       atom.hasBond(sphereAtom) == false &&
		       atom.connected13(sphereAtom) == false &&
		       atom.connected14(sphereAtom) == false){
			double dSq = atomVDWRadius + vdwRadius;
			dSq = dSq * dSq;

			if(atom.distanceSq(sphereAtom) < dSq){
			    addBumpPair(atom, sphereAtom);
			}
		    }
		}
	    }
	}
    }

    /** Add a distance monitor to the list. */
    public void addDistance(Distance d){
	distances.add(d);
	fireGenericAddedEvent((Generic)d);
    }

    /** Return how many distances. */
    public int getDistanceCount(){
	return distances.size();
    }

    /** Return a specific distance. */
    public Distance getDistance(int i){
	return (Distance)distances.get(i);
    }

    /** Add a distance monitor between the selected atoms. */
    public void addDistanceBetweenSelectedAtoms(){
	int selectedAtomCount = selectedAtoms.size();

	if(selectedAtomCount == 2){
	    Atom firstAtom = (Atom)selectedAtoms.get(0);
	    Atom secondAtom = (Atom)selectedAtoms.get(1);

	    Distance d = Distance.createDistanceMonitor(firstAtom,
							secondAtom);
	    addDistance(d);
	    //distances.add(d);
	}
    }

    /** Add a pair of atoms to the distance list. */
    public void addDistance(Atom firstAtom, Atom secondAtom){
	// reuse bond for maintaining a distance.
	Distance distance = new Distance();

	distance.group0.add(firstAtom);
	distance.group1.add(secondAtom);

	addDistance(distance);
	//distances.add(distance);
    }

    /** Add a pair of atoms to the distance list. */
    public void addDistance2(Atom firstAtom, Atom secondAtom){
	// reuse bond for maintaining a distance.
	Bond distance = Bond.create();

	distance.setFirstAtom(firstAtom);
	distance.setSecondAtom(secondAtom);

	//addDistance(distance);
	distances.add(distance);
    }

    private DynamicArray angles = new DynamicArray();

    /** Add a set of atoms to the angle list. */
    public void addAngle(Atom firstAtom, Atom secondAtom,
			 Atom thirdAtom){
	angles.add(firstAtom);
	angles.add(secondAtom);
	angles.add(thirdAtom);
    }

    /** Add a set of atoms to the angle list. */
    public void addTorsion(Atom firstAtom, Atom secondAtom,
			   Atom thirdAtom, Atom fourthAtom){
	torsions.add(firstAtom);
	torsions.add(secondAtom);
	torsions.add(thirdAtom);
	torsions.add(fourthAtom);
    }

    /** Remove everything from the renderer. */
    public void reset(){
	removeGraphicalObjects("*");
	removeMolecule("*");
	removeMaps();
	removeAllAngles();
	removeAllDistances();
	removeAllTorsions();
	removeAllLabelledAtoms();
	removeAllSelectedAtoms();
	removeAllBumpAtoms();
	removeSpaceGroup();
    }

    /** Remove all the angles. */
    public void removeAllAngles(){
	angles.removeAllElements();
    }

    /** Remove all the distance monitors. */
    public void removeAllDistances(){
	//distances.removeAllElements();
	deleteDistances("*");
    }

    /** Remove all the torsion monitors. */
    public void removeAllTorsions(){
	torsions.removeAllElements();
    }

    /** Remove all labelled atoms. */
    public void removeAllLabelledAtoms(){
	AtomIterator atomIterator = getAtomIterator();

	while(atomIterator.hasMoreElements()){
	    Atom atom = atomIterator.getNextAtom();
	    atom.setLabelled(false);
	}
	//labelledAtoms.removeAllElements();
    }

    /** Remove all selected atoms. */
    public void removeAllSelectedAtoms(){
	//AtomIterator atomIterator = getAtomIterator();

	//while(atomIterator.hasMoreElements()){
	//    Atom atom = atomIterator.getNextAtom();
	//    atom.setSelected(false);
	//}

	execute("select none;");

	selectedAtoms.removeAllElements();
	pickedAtoms.removeAllElements();
	fireAtomSelectedEvent(null);
    }

    /** Remove all bump atoms. */
    public void removeAllBumpAtoms(){
	bumpAtoms.removeAllElements();
    }

    /** Get the bump atoms. */
    public DynamicArray getBumpAtoms(){
	return bumpAtoms;
    }

    /** Generate symmetry for the molecules we are displaying. */
    public Molecule generateSymmetry(Point3d center, double radius){
	Molecule symmetryMolecule = null;

	if(displaySymmetry == true){
	    if(getMoleculeCount() > 0){
		// figure out which symmetry we want to use
		determineSymmetry();

		if(symmetry != null){
		    symmetryMolecule =
			generateSymmetryMolecule(center, radius);
		}
	    }
	}

	return symmetryMolecule;
    }

    /** Acutally generate the symmetry atoms. */
    public Molecule generateSymmetryMolecule(Point3d center, double radius){
	Molecule symmetryMolecule = new Molecule();
	symmetryMolecule.setName("Symmetry");
	symmetryMolecule.setMoleculeType(Molecule.SymmetryMolecule);

	Point3d fractionalCenter = new Point3d(center);

	Molecule molecule = getMolecule(0);
	Point3d moleculeCenter = molecule.getCenter();
	double moleculeRadius = molecule.getRadius();

	int atomCount = molecule.getAtomCount();

	int oldIds[] = new int[atomCount];

	// record the current atom ids
	// as we need to put them back to
	// prevent atom id selection expressions
	// breaking, there must be a better way
	// of fixing this.
	for(int a = 0; a < atomCount; a++){
	    Atom atom = molecule.getAtom(a);
	    oldIds[a] = atom.getId();
	}

	// make sure the atom numbers start at 0
	molecule.assignAtomNumbers();

	// get the matrices for our chosen symmetry.
	Matrix cartesianToFractional =
	    symmetry.getCartesianToFractionalMatrix();
	Matrix fractionalToCartesian =
	    symmetry.getFractionalToCartesianMatrix();

	//cartesianToFractional.print("cartesianToFractional");

	// figure out where the molecule was located in
	// fractional unit cells.
	fractionalCenter.transform(cartesianToFractional);

	// if the point has -ve coords we need to subtract 1
	int originx = (int)fractionalCenter.x;
	if(fractionalCenter.x < 0.0) originx -= 1;
	int originy = (int)fractionalCenter.y;
	if(fractionalCenter.y < 0.0) originy -= 1;
	int originz = (int)fractionalCenter.z;
	if(fractionalCenter.z < 0.0) originz -= 1;

	//System.out.println("fractional center " + fractionalCenter);

	//System.out.println("origin " + originx + " " + originy + " " + originz);

	// the symmetry operators
	DynamicArray symmetryOperators = symmetry.getSymmetryOperators();

	Point3d currentCenter = new Point3d();

	int operatorCount = symmetryOperators.size();

	Matrix currentTransform = new Matrix();

	int unit_cell_offset = 2;

	// now iterate over 1 unit cell in each direction along
	// each fractional axis
	for(int ix = -unit_cell_offset; ix <= unit_cell_offset; ix++){
	    for(int iy = -unit_cell_offset; iy <= unit_cell_offset; iy++){
		for(int iz = -unit_cell_offset; iz <= unit_cell_offset; iz++){
		    int startOperator = 0;


		    // if we're in the primary unit cell
		    // we don't need the identity matrix
		    if(ix == 0 && iy == 0 && iz == 0){
			startOperator = 1;
		    }

		    for(int s = startOperator; s < operatorCount; s++){
			Matrix operator = (Matrix)symmetryOperators.get(s);

			currentTransform.setIdentity();
			//currentTransform.print("identity");
			currentTransform.transform(cartesianToFractional);
			//currentTransform.print("cartesianToFractional");
			// shift to origin
			currentTransform.translate(-originx, -originy, -originz);
			//currentTransform.print("after -origin");
			currentTransform.transform(operator);
			//currentTransform.print("after symm op");
			currentTransform.translate(ix, iy, iz);
			//currentTransform.print("after cell shift");
			// and shift back to original unit cell
			currentTransform.translate(originx, originy, originz);
			//currentTransform.print("after +origin");
			currentTransform.transform(fractionalToCartesian);
			//currentTransform.print("fractionalToCartesian");

			currentCenter.set(moleculeCenter);

			currentCenter.transform(currentTransform);

			double d = currentCenter.distance(center);

			if(d < radius + moleculeRadius){
			    //System.out.println("ix " + ix + " iy " + iy + " iz " + iz);
			    //System.out.println("Symmetry operator "+ s);
			    addSymmetryAtoms(molecule, center, radius,
					     currentTransform,
					     symmetryMolecule);
			}
		    }
		}
	    }
	}

	symmetryMolecule.connect();

	// restore the original atom ids.
	for(int a = 0; a < atomCount; a++){
	    Atom atom = molecule.getAtom(a);
	    atom.setId(oldIds[a]);
	}

	return symmetryMolecule;
    }

    /** Add the symmetry atoms that overlap. */
    public void addSymmetryAtoms(Molecule molecule, Point3d center,
				 double radius, Matrix transform,
				 Molecule symmetryMolecule){
	int atomCount   = molecule.getAtomCount();
	int inSphere[]  = new int[atomCount];
	Point3d p       = new Point3d();
	double radiusSq = radius * radius;
	int added       = 0;

	for(int a = 0; a < atomCount; a++){
	    if(inSphere[a] == 0){
		Atom atom = molecule.getAtom(a);
		p.set(atom);
		p.transform(transform);

		if(p.distanceSq(center) < radiusSq){
		    inSphere[a] = 1;
		    added++;
		}
	    }
	}

	expandSelection(molecule, inSphere);

	Residue lastResidue = null;
	Chain lastChain     = null;

	for(int a = 0; a < atomCount; a++){
	    if(inSphere[a] != 0){
		Atom atom = molecule.getAtom(a);
		p.set(atom);
		p.transform(transform);

		Residue res = atom.getResidue();
		Chain chain = res.getParent();

		if(chain != lastChain){
		    Chain newChain = symmetryMolecule.addChain();
		    newChain.setName(chain.getName());
		}

		if(res != lastResidue){
		    Residue newResidue = symmetryMolecule.addResidue();
		    newResidue.setNumber(res.getNumber());
		    newResidue.setInsertionCode(res.getInsertionCode());
		    newResidue.setName(res.getName());
		}

		lastResidue = res;
		lastChain = chain;

		Atom newAtom = symmetryMolecule.addAtom();
		newAtom.set(p);
		newAtom.setElement(atom.getElement());
		newAtom.setAtomLabel(atom.getAtomLabel());
	    }
	}
    }

    /** Return the total number of atoms in the renderer. */
    public int getAtomCount(){
	int moleculeCount = molecules.size();
	int atomCount = 0;
	for(int m = 0; m < moleculeCount; m++){
	    Molecule molecule = (Molecule)molecules.get(m);
	    atomCount += molecule.getAtomCount();
	}

	return atomCount;
    }

    public int setMoleculeVariable(String pattern, String name, String value){
	int moleculeCount = getMoleculeCount();

	for(int i = 0; i < moleculeCount; i++){
	    Molecule m = getMolecule(i);

	    if(moleculeMatches(pattern, m)){
		if("bonddetails".equalsIgnoreCase(name)){
		    m.setBoolean(Molecule.DisplayBondDetails, "on".equalsIgnoreCase(value));
		}else{
		    throw new RuntimeException("unknown molecule variable: " + name);
		}
	    }
	}

	return 0;
    }

    /** Change molecule visibility. */
    public int setMoleculeVisibility(String pattern, String action){
	int moleculeCount = getMoleculeCount();

	for(int i = 0; i < moleculeCount; i++){
	    Molecule m = getMolecule(i);
	    String name = m.getName();
	    if(moleculeMatches(pattern, m)){
		//if(match.matches(pattern, name)){
		if(action.equals("off")){
		    m.setDisplayed(0);
		}else if(action.equals("on")){
		    m.setDisplayed(1);
		}else if(action.equals("toggle")){
		    m.setDisplayed(2);
		}else if(action.equals("trace")){
		    m.setDisplayStyle(Molecule.Trace);
		}else if(action.equals("tracealways")){
		    m.setDisplayStyle(Molecule.TraceAlways);
		}else if(action.equals("normal")){
		    m.setDisplayStyle(Molecule.Normal);
		}else{
		    System.out.println("molecule display: unknown option " +
				       action);
		    return 1;
		}
	    }
	}

	return 0;
    }

    /** Expand the selected atoms to include the whole residue. */
    public void expandSelection(Molecule molecule, int inSphere[]){
	int atomCount = molecule.getAtomCount();
	for(int a = 0; a < atomCount; a++){
	    Atom atom = molecule.getAtom(a);
	    int id = atom.getId();

	    if(inSphere[id] == 1){
		int bondCount = atom.getBondCount();
		for(int b = 0; b < bondCount; b++){
		    Atom bondedAtom = atom.getBondedAtom(b);
		    int bondedId = bondedAtom.getId();
		    inSphere[bondedId] = 2;
		}
	    }
	}
    }

    /** Expand the selected atoms to include the whole residue. */
    public void expandSelection1(Molecule molecule, int inSphere[]){

	for(int c = 0; c < molecule.getChainCount(); c++){
	    Chain chain = molecule.getChain(c);
	    int residueCount = chain.getResidueCount();

	    for(int r = 0; r < residueCount; r++){
		Residue residue = chain.getResidue(r);
		int atomCount = residue.getAtomCount();
		boolean residueSelected = false;

		for(int a = 0; a < atomCount; a++){
		    Atom atom = residue.getAtom(a);
		    int id = atom.getId();
		    if(inSphere[id] != 0){
			residueSelected = true;
			break;
		    }
		}

		if(residueSelected){
		    for(int a = 0; a < atomCount; a++){
			Atom atom = residue.getAtom(a);
			int id = atom.getId();
			inSphere[id] = 1;
		    }
		}
	    }
	}
    }

    /** Determine the set of symmetry operators. */
    public void determineSymmetry(){
	// this is more difficult than it appears as the
	// molecules may not have a spacegroup specified.

	if(symmetry == null){
	    for(int m = 0; m < getMapCount(); m++){
		Map map = getMap(m);

		//System.out.println("map " + map);
		if(map.getMapType() != Map.O_BINARY &&
		   map.getMapType() != Map.INSIGHT_ASCII){
		    // we don't want symmetry if its p1

		    if(map.getSpaceGroupNumber() > 1){
			System.out.println("map number " +
					   map.getSpaceGroupNumber());
			symmetry = map;
			break;
		    }else{
			symmetry = null;
		    }
		}
	    }
	}

	if(symmetry == null){
	    for(int m = 0; m < getMoleculeCount(); m++){
		Molecule molecule = getMolecule(m);
		Symmetry moleculeSymmetry = molecule.getSymmetry();
		if(moleculeSymmetry != null){
		    symmetry = moleculeSymmetry;
		    break;
		}
	    }
	}

	//System.out.println("after determineSymmetry "+ symmetry);
    }

    /* Methods for performing various kinds of atom selections. */

    /** Handle a selection stack command. */
    public void handleSelectionCommand(String command, Arguments args){
    }

    private DynamicArray selectionStack = new DynamicArray();

    /** Push a selection onto the stack. */
    public void pushSelection(DynamicArray atoms){
	selectionStack.add(atoms);
    }

    /** Pop a selection from the selection stack. */
    public DynamicArray popSelection(){
	DynamicArray atoms = null;
	int selectionStackSize = selectionStack.size();

	if(selectionStackSize > 0){
	    atoms = (DynamicArray)selectionStack.getReverse(0);

	    selectionStack.removeElement(selectionStackSize - 1);
	}else{
	    Log.warn("can't pop, stack is empty");
	}

	// always return something.
	if(atoms == null){
	    atoms = new DynamicArray();
	}

	return atoms;
    }

    /** Peek a selection on the stack. */
    public DynamicArray peekSelection(int i){
	DynamicArray atoms = null;
	int selectionStackSize = selectionStack.size();

	if(i < selectionStackSize){
	    atoms = (DynamicArray)selectionStack.getReverse(i);

	}else{
	    Log.warn("can't peek selection " + i +
		     ": stack is only " + selectionStackSize + " deep");
	}

	// always return something.
	if(atoms == null){
	    atoms = new DynamicArray();
	}

	return atoms;
    }

    /** Evaluate a selection expression. */
    public DynamicArray getAtomsInSelection(String expression){
	DynamicArray selectedAtoms = new DynamicArray();

	String residueExpressions[] = FILE.split(expression, " ");

	for(int i = 0; i < residueExpressions.length; i++){
	    DynamicArray thisSelection = getAtomsInResidue(residueExpressions[i]);
	    int thisSelectionCount = thisSelection.size();

	    for(int j = 0; j < thisSelectionCount; j++){
		selectedAtoms.add(thisSelection.get(j));
	    }
	}

	return selectedAtoms;
    }

    /** Return the atoms in a particular residue. */
    public DynamicArray getAtomsInResidue(String residueSpecification){
	DynamicArray selectedAtoms = new DynamicArray();
	String chainName = null;
	int stopResidueNumber[] = null;
	int startResidueNumber[] = null;
	int residueRangeCount = 0;
	int atomNameCount = 0;
	String atomNames[] = null;
	String residueString = null;
	String remainder = null;
	String moleculeName = null;
	char insertionCode = 0;
	double radius = -1.0;

	if(residueSpecification.equalsIgnoreCase("current")){
	    DynamicArray currentSelection = getSelectedAtoms();
	    int currentSelectionCount = currentSelection.size();

	    for(int i = 0; i < currentSelectionCount; i++){
		selectedAtoms.add(currentSelection.get(i));
	    }

	    return selectedAtoms;
	}else if(residueSpecification.equalsIgnoreCase("all")){
	    AtomIterator iterator = getAtomIterator();

	    while(iterator.hasMoreElements()){
		Atom atom = iterator.getNextAtom();
		selectedAtoms.add(atom);
	    }

	    return selectedAtoms;

	}

	String colonTokens[] = FILE.split(residueSpecification, ":");

	if(residueSpecification.startsWith("id:") && colonTokens.length == 3){
	    //System.out.println("its an id selection");

	    // its a specific atom id selection
	    int moleculeCount = getMoleculeCount();
	    String molName = colonTokens[1];
	    DynamicArray idSelection = new DynamicArray();

	    for(int i = 0; i < moleculeCount; i++){
		Molecule molecule = getMolecule(i);
		if(molName.equals(molecule.getName())){

		    String idTokens[] = FILE.split(colonTokens[2], ",");
		    int idCount = idTokens.length;
		    int startId[] = new int[idCount];
		    int stopId[] = new int[idCount];

		    for(int id = 0; id < idCount; id++){
			String ids[] = FILE.split(idTokens[id], "-");

			startId[id] = FILE.readInteger(ids[0]);

			if(ids.length == 1){
			    stopId[id] = startId[id];
			}else if(ids.length == 2){
			    stopId[id] = FILE.readInteger(ids[1]);
			}
		    }

		    int atomCount = molecule.getAtomCount();

		    for(int a = 0; a < atomCount; a++){
			Atom atom = molecule.getAtom(a);
			int atomId = atom.getId();

			for(int id = 0; id < idCount; id++){
			    if(atomId >= startId[id] && atomId <= stopId[id]){
				idSelection.add(atom);
				break;
			    }
			}
		    }
		}
	    }

	    return idSelection;
	}

	if(debug){
	    System.out.println("colonTokens.length " + colonTokens.length);

	    for(int i = 0; i < colonTokens.length; i++){
		System.out.println("token "+ colonTokens[i]);
	    }
	}

	if(colonTokens.length == 1){
	    remainder = colonTokens[0];
	}else if(colonTokens.length == 2){
	    chainName = colonTokens[0];
	    remainder = colonTokens[1];
	}else if(colonTokens.length == 3){
	    moleculeName = colonTokens[0];
	    chainName = colonTokens[1];
	    remainder = colonTokens[2];
	}else if(colonTokens.length == 4){
	    radius = FILE.readDouble(colonTokens[0]);
	    moleculeName = colonTokens[1];
	    chainName = colonTokens[2];
	    remainder = colonTokens[3];
	}

	String dotTokens[] = FILE.split(remainder, ".");

	residueString = dotTokens[0];

	if(dotTokens.length == 2){
	    atomNames = FILE.split(dotTokens[1], ",");
	    atomNameCount = atomNames.length;
	}

	if(residueString != null){
	    if(residueString.equals("*") || residueString.equals("")){
		// do nothing
	    }else{
		String residueRanges[] = FILE.split(residueString, ",");
		residueRangeCount = residueRanges.length;
		startResidueNumber = new int[residueRangeCount];
		stopResidueNumber = new int[residueRangeCount];

		for(int i = 0; i < residueRangeCount; i++){
		    String residueTokens[] = FILE.split(residueRanges[i], "-");

		    startResidueNumber[i] = FILE.readInteger(residueTokens[0]);
		    if(residueTokens.length == 2){
			stopResidueNumber[i] = FILE.readInteger(residueTokens[1]);
		    }else{
			stopResidueNumber[i] = startResidueNumber[i];
		    }
		}
	    }
	}

	if(chainName != null){
	    // replace ^ characters with space
	    chainName = chainName.replace('^', ' ');
	    if(chainName.equals("*") || chainName.equals("")){
		chainName = null;
	    }
	}

	if(moleculeName != null){
	    if(moleculeName.equals("")){
		moleculeName = null;
	    }
	}

	if(debug){
	    System.out.println("radius " + radius);
	    System.out.println("moleculeName " + moleculeName);
	    System.out.println("chainName " + moleculeName);
	    System.out.println("remainder " + remainder);
	}

	for(int m = 0; m < getMoleculeCount(); m++){
	    Molecule molecule = getMolecule(m);
	    int chainCount = molecule.getChainCount();
	    if(moleculeName == null ||
	       moleculeMatches(moleculeName, molecule)){
		//match.matches(moleculeName, molecule.getName())){
		for(int c = 0; c < chainCount; c++){
		    Chain chain = molecule.getChain(c);
		    int residueCount = chain.getResidueCount();

		    // check residues if no chain name is
		    // specified or if the chain names match
		    if(chainName == null ||
		       match.matches(chainName, chain.getName())){
			for(int r = 0; r < residueCount; r++){
			    Residue residue = chain.getResidue(r);
			    int residueNumber = residue.getNumber();
			    int residueMatch = 0;

			    if(residueRangeCount == 0){
				residueMatch = 1;
			    }else{
				for(int i = 0; i < residueRangeCount; i++){
				    if(residueNumber >= startResidueNumber[i] &&
				       residueNumber <= stopResidueNumber[i]){
					residueMatch = 1;
					break;
				    }
				}
			    }

			    if(residueMatch == 1){
				int atomCount = residue.getAtomCount();

				for(int a = 0; a < atomCount; a++){
				    Atom atom = residue.getAtom(a);
				    int matchedAtom = 0;


				    if(atomNameCount == 0){
					matchedAtom = 1;
				    }else{
					for(int i = 0; i < atomNameCount; i++){
					    if(match.matches(atomNames[i], atom.getAtomLabel())){
						matchedAtom = 1;
						break;
					    }
					}
				    }

				    if(matchedAtom == 1){
					selectedAtoms.add(atom);
				    }
				}
			    }
			}
		    }
		}
	    }
	}

	// if there was a radius specification
	if(radius > 0.0){
	    double radiusSq = radius * radius;
	    DynamicArray radiusSelection = new DynamicArray();
	    AtomIterator iterator = getAtomIterator();
	    int selectedAtomCount = selectedAtoms.size();

	    while(iterator.hasMoreElements()){
		Atom atom = iterator.getNextAtom();
		for(int i = 0; i < selectedAtomCount; i++){
		    Atom selectedAtom = (Atom)selectedAtoms.get(i);
		    if(atom.distanceSq(selectedAtom) < radiusSq){
			radiusSelection.add(atom);
		    }
		}
	    }

	    return radiusSelection;
	}

	return selectedAtoms;
    }

    /** Return the atoms in the specified sphere. */
    public DynamicArray getAtomsInSphere(Point3d c, double r){
	DynamicArray selectedAtoms = new DynamicArray();

	double rSq = r * r;
	int moleculeCount = getMoleculeCount();

	for(int m = 0; m < moleculeCount; m++){
	    Molecule molecule = getMolecule(m);
	    int atomCount = molecule.getAtomCount();
	    for(int a = 0; a < atomCount; a++){
		Atom atom = molecule.getAtom(a);

		if(atom.distanceSq(c) < rSq){
		    selectedAtoms.add(atom);
		}
	    }
	}

	return selectedAtoms;
    }

    /** Return atoms in a shell around the specified selection. */
    public DynamicArray getAtomsAroundSelection(DynamicArray selection,
						double radius,
						boolean include){
	DynamicArray newSelection = new DynamicArray();

	Point3d selectionCenter = getCenter(selection);

	if(selectionCenter != null){
	    double selectionRadius = getRadius(selection, selectionCenter);

	    DynamicArray sphereSelection =
		getAtomsInSphere(selectionCenter, selectionRadius + radius);

	    int sphereSelectionCount = sphereSelection.size();
	    int selectionCount = selection.size();
	    Object selectionArray[] = selection.getArray();
	    double radiusSq = radius * radius;


	    for(int i = 0; i < sphereSelectionCount; i++){
		Atom atom = (Atom)sphereSelection.get(i);

		if(include == false ||
		   selection.contains(atom) == false){
		    for(int a = 0; a < selectionCount; a++){
			Atom selectedAtom = (Atom)selectionArray[a];

			if(atom.distanceSq(selectedAtom) < radiusSq){
			    newSelection.add(atom);
			    break;
			}
		    }
		}
	    }
	}

	return newSelection;
    }

    /** Get atoms that are part of ligands. */
    public void getAtomsInLigands(DynamicArray selectedAtoms){

	AtomIterator atomIterator = getAtomIterator();

	while(atomIterator.hasMoreElements()){
	    Atom atom = atomIterator.getNextAtom();

	    Residue residue = atom.getResidue();

	    if(residue.isStandardAminoAcid() == false &&
	       residue.isIon() == false &&
	       residue.isNucleicAcid() == false &&
	       residue.isSolvent() == false &&
	       atom.getBondCount() > 0){

		selectedAtoms.add(atom);
	    }
	}
    }

    /** Wrapper around getAtomsInLigands. */
    public DynamicArray getAtomsInLigands(){
	DynamicArray dynamicArray = new DynamicArray();

	getAtomsInLigands(dynamicArray);

	return dynamicArray;
    }

    /** Return the number of selected atoms. */
    public int getSelectedAtomCount(){
	AtomIterator iterator = getAtomIterator();

	int selectedAtomCount = 0;
	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    if(atom.isSelected()){
		selectedAtomCount++;
	    }
	}

	return selectedAtomCount;
    }

    /** Return the atoms that are currently selected. */
    public DynamicArray getSelectedAtoms(){
	return getSomeAtoms(true, false);
    }

    /** Return the atoms that are currently labelled. */
    public DynamicArray getLabelledAtoms(){
	return getSomeAtoms(false, true);
    }

    /** Return the atoms that are currently labelled or selected. */
    public DynamicArray getSelectedOrLabelledAtoms(){
	return getSomeAtoms(true, true);
    }

    /** Return the atoms that are currently labelled. */
    public DynamicArray getSomeAtoms(boolean selected, boolean labelled){
	DynamicArray selectedAtoms = new DynamicArray();
	AtomIterator atomIterator = getAtomIterator();

	while(atomIterator.hasMoreElements()){
	    Atom atom = atomIterator.getNextAtom();

	    if((labelled && atom.isLabelled()) ||
	       (selected && atom.isSelected())){
		selectedAtoms.add(atom);
	    }
	}

	return selectedAtoms;
    }

    /** Return the residues that are currently selected. */
    public DynamicArray getSelectedResidues(){
	DynamicArray selectedResidues = new DynamicArray();
	AtomIterator atomIterator = getAtomIterator();
	Residue lastResidue = null;

	while(atomIterator.hasMoreElements()){
	    Atom atom = atomIterator.getNextAtom();

	    if(atom.isSelected()){
		Residue residue = atom.getResidue();
		if(residue != lastResidue){
		    selectedResidues.add(residue);
		    lastResidue = residue;
		}
	    }
	}

	return selectedResidues;
    }

    /** Set the specified atoms as selected. */
    public void setSelected(DynamicArray selection){
	setSelected(selection, false);
    }

    /** Set the specified atoms as selected. */
    public void setSelected(DynamicArray selection, boolean exclude){
	int selectionCount = selection.size();

	for(int a = 0; a < selectionCount; a++){
	    Atom atom = (Atom)selection.get(a);
	    if(exclude){
		atom.setSelected(false);
	    }else{
		atom.setSelected(true);
	    }
	}

	// if we are displaying bumps then generate bumps when
	// we change the selection
	if(displayBumps){
	    DynamicArray selectedAtoms = getSelectedAtoms();

	    generateBumps(selectedAtoms);
	}
    }

    /** Return the geometric center of the atoms in the selection. */
    public Point3d getCenter(DynamicArray selection){
	int atomCount = selection.size();

	if(atomCount > 0){
	    Point3d center = new Point3d();
	    for(int a = 0; a < atomCount; a++){
		Atom atom = (Atom)selection.get(a);
		center.add(atom);
	    }

	    center.divide(atomCount);

	    return center;
	}

	return null;
    }

    /** Return the radius of the atoms in the selection. */
    public double getRadius(DynamicArray selection){
	return getRadius(selection, null);
    }

    /** Return the radius of the atoms in the selection. */
    public double getRadius(DynamicArray selection, Point3d center){
	double radius = 0.0;
	int atomCount = selection.size();

	if(atomCount > 0){
	    if(center == null){
		center = getCenter(selection);
	    }
	    for(int a = 0; a < atomCount; a++){
		Atom atom = (Atom)selection.get(a);
		double distance = center.distance(atom);
		if(distance > radius){
		    radius = distance;
		}
	    }
	}

	return radius;
    }

    /** Reset the wide bonds flags. */
    public void resetWideBonds(){
	int moleculeCount = getMoleculeCount();

	for(int m = 0; m < moleculeCount; m++){
	    Molecule molecule = getMolecule(m);
	    int bondCount = molecule.getBondCount();

	    for(int b = 0; b < bondCount; b++){
		Bond bond = molecule.getBond(b);
		bond.setBondWidth(1);
	    }
	}
    }

    /** Are the two atoms in the same planar ring. */
    public boolean inSameplanarRing(Atom atom1, Atom atom2){
	int moleculeCount = getMoleculeCount();
	for(int m = 0; m < moleculeCount; m++){
	    Molecule molecule = getMolecule(m);
	    int ringCount = molecule.getRingCount();
	    for(int r = 0; r < ringCount; r++){
		Ring ring = molecule.getRing(r);

		if(ring.getBondCount() == 6){
		    if(ring.contains(atom1) && ring.contains(atom2)){
			if(ring.isPlanar()){
			    return true;
			}
		    }
		}
	    }
	}

	return false;
    }

    /** Set the center for the renderer. */
    public void setCenter(double x, double y, double z){
	Point3d newCenter = new Point3d(x, y, z);
	setCenter(newCenter);
    }

    public void setCenter(Point3d newCenter){
	setCenter(newCenter, true);
    }

    /** Set the center for the renderer. */
    public void setCenter(Point3d newCenter, boolean setClipping){
	//System.out.println("boolean setClipping " + setClipping);
	renderer.setCenter(newCenter);

	//super.setRadius(displayRadius);
	//setClip(mapRadius);

	if(setClipping){
	    setRadius(displayRadius);
	    setClip(displayRadius);
	}
	//setClip(displayRadius);

	// recontour any maps that we might be displaying
	generateMaps();

	// generate the symmetry atoms.
	generateSymmetry();
    }

    /** Set the center from a set of atoms. */
    public void setCenter(DynamicArray selectedAtoms){
	Point3d center = getCenter(selectedAtoms);

	if(center != null){
	    setCenter(center);

	    double radius = getRadius(selectedAtoms);

	    setClip(radius);

	    setRadius(radius);

	    renderer.setZoom(1.0);
	}
    }

    /** Generate the map display. */
    public void generateMaps(){
	if(displayMaps){
	    readMaps();
	    //setClip(displayRadius);
	    contourMaps();
	}
    }

    /** Remove the displayed maps. */
    public void removeContourLevels(){
	removeGraphicalObjectsBeginningWith("Map");
    }

    /** Remove the maps rather than the contour levels. */
    public void removeMaps(){
	removeGraphicalObjectsBeginningWith("Map");
	maps.removeAllElements();
    }

    /** Should we display maps. */
    public void setDisplayMaps(boolean state){
	displayMaps = state;

	if(displayMaps == false){
	    removeContourLevels();
	}else{
	    generateMaps();
	}
    }

    /** Set whether we display bumps. */
    public void setDisplayBumps(boolean state){
	displayBumps = state;

	if(displayBumps == false){
	    removeAllBumpAtoms();
	}else{
	    DynamicArray selectedAtoms = getSelectedAtoms();

	    generateBumps(selectedAtoms);
	}
    }

    public void setBumpInSameMolecule(boolean b){
	bumpInSameMolecule = b;
    }

    /** Set the distance display flag. */
    public void setDisplayDistances(boolean d){
	displayDistances = d;
    }

    /** Return the distance display flag. */
    public boolean getDisplayDistances(){
	return displayDistances;
    }

    /** Return the state of bump display. */
    public boolean getDisplayBumps(){
	return displayBumps;
    }

    /** Set whether we display solvent. */
    public void setDisplaySolvent(boolean state){
	displaySolvent = state;
    }

    /** Set which bonds are drawn wide. */
    public void setWideBonds(){
	double mapRadiusSq = mapRadius * mapRadius;
	Point3d rendererCenter = renderer.getCenter();
	int moleculeCount = getMoleculeCount();

	for(int m = 0; m < moleculeCount; m++){
	    Molecule molecule = getMolecule(m);
	    int bondCount = molecule.getBondCount();

	    for(int b = 0; b < bondCount; b++){
		Bond bond = molecule.getBond(b);
		Atom firstAtom = bond.getFirstAtom();
		Atom secondAtom = bond.getSecondAtom();

		if(firstAtom.distanceSq(rendererCenter) < mapRadiusSq &&
		   secondAtom.distanceSq(rendererCenter) < mapRadiusSq){
		    bond.setWideBond(true);
		}else{
		    bond.setWideBond(false);
		}
	    }
	}
    }

    /** Return bonds where both atoms are in selectedAtoms. */
    public DynamicArray getBondsInSelection(DynamicArray selectedAtoms){
	DynamicArray selectedBonds = new DynamicArray();

	AtomIterator iterator = getAtomIterator();

	while(iterator.hasMoreElements()){
	    Atom atom = iterator.getNextAtom();
	    atom.setTemporarilySelected(false);
	}

	int selectedAtomCount = selectedAtoms.size();
	for(int i = 0; i < selectedAtomCount; i++){
	    Atom a = (Atom)selectedAtoms.get(i);
	    a.setTemporarilySelected(true);
	}

	for(int m = 0; m < getMoleculeCount(); m++){
	    Molecule molecule = getMolecule(m);
	    int bondCount = molecule.getBondCount();

	    for(int b = 0; b < bondCount; b++){
		Bond bond = molecule.getBond(b);
		Atom firstAtom = bond.getFirstAtom();
		Atom secondAtom = bond.getSecondAtom();
		if(firstAtom.isTemporarilySelected() &&
		   secondAtom.isTemporarilySelected()){
		    selectedBonds.add(bond);
		}
	    }
	}

	return selectedBonds;
    }

    /** Set wide bonds for the atoms in the selection. */
    public void setWideBonds(DynamicArray selectedAtoms){
	resetWideBonds();

	int selectedAtomCount = selectedAtoms.size();
	for(int a = 0; a < selectedAtomCount; a++){
	    Atom atom = (Atom)selectedAtoms.get(a);
	    atom.setTemporarilySelected(true);
	}

	for(int a = 0; a < selectedAtomCount; a++){
	    Atom atom = (Atom)selectedAtoms.get(a);
	    int bondCount = atom.getBondCount();
	    if(atom.isTemporarilySelected()){
		for(int b = 0; b < bondCount; b++){
		    Bond bond = atom.getBond(b);
		    Atom otherAtom = bond.getOtherAtom(atom);
		    if(otherAtom.isTemporarilySelected()){
			bond.setBondWidth(2);
		    }
		}
	    }
	}

	for(int a = 0; a < selectedAtomCount; a++){
	    Atom atom = (Atom)selectedAtoms.get(a);
	    atom.setTemporarilySelected(false);
	}
    }

    /** The maximum number of contour levels. */
    private static int MaximumContourLevels = Map.MaximumContourLevels;

    /** The number of contour levels we are using. */
    private int contourLevelCount = 0;

    /** The default contour levels. */
    private double contourLevels[] = new double[MaximumContourLevels];

    /** The default colors for each level. */
    private int contourColors[] = new int[MaximumContourLevels];

    /** The default styles for each level. */
    private int contourStyles[] = new int[MaximumContourLevels];

    /** The default display setting for each level. */
    private boolean contourDisplayed[] = new boolean[MaximumContourLevels];

    /** Add a contour level. */
    public void addContourLevel(double level, int color,
				boolean display, int style){
	if(contourLevelCount == MaximumContourLevels){
	    System.out.println("maximum number of contour levels exceeded");
	    return;
	}

	contourLevels[contourLevelCount] = level;
	contourColors[contourLevelCount] = color;
	contourStyles[contourLevelCount] = style;
	contourDisplayed[contourLevelCount] = display;

	contourLevelCount++;
    }

    /** Reset the contour levels. */
    public void resetContourLevels(){
	contourLevelCount = 0;
    }

    /** Generate the symmetry copies of the molecules. */
    public void generateSymmetry(){
	removeSymmetry();

	Molecule symmetry = 
	    generateSymmetry(renderer.getCenter(), symmetryRadius);

	if(symmetry != null){
	    assignSymmetryAtomColors(symmetry);
	    addMolecule(symmetry);
	}
    }

    /** Remove the symmetry molecule if there is one. */
    public void removeSymmetry(){
	removeMolecule("Symmetry");
    }

    /** Assign atom colors. */
    public void assignSymmetryAtomColors(Molecule molecule){
	int atomCount = molecule.getAtomCount();
	for(int a = 0; a < atomCount; a++){
	    Atom atom = molecule.getAtom(a);
	    if(atom.getElement() == PeriodicTable.CARBON){
		atom.setColor(0xaaaaaa);
	    }
	}
    }

    /** Read the maps that we are displaying. */
    public void readMap(Map map){
	map.setCenter(renderer.getCenter());
	map.setRadius(mapRadius);
	//determineRegion(map);
    }

    public void readMaps(){
	for(int i = 0; i < getMapCount(); i++){
	    Map map = getMap(i);
	    if(map.hasContoursDisplayed()){
		readMap(map);
	    }
	}
    }

    /** Contour the maps that we are displaying. */
    public void contourMaps(){
	// blow away the old maps
	//removeContourLevels();

	for(int i = 0; i < getMapCount(); i++){
	    Map map = getMap(i);

	    for(int j = 0; j < Map.MaximumContourLevels; j++){
		contourMap(map, j);
	    }
	}
    }

    /** Get name of contour for particular map. */
    public String getContourGraphicalObjectName(Map map, int contour){
	return map.getName() + "_" + contour;
    }

    /** Get tmesh of contour for particular map. */
    public Tmesh getContourGraphicalObject(Map map, int contour){
	String contourName = getContourGraphicalObjectName(map, contour);

	Tmesh contourObject = renderer.getGraphicalObject(contourName);

	return contourObject;
    }

    /** Generate one specific contour level for the map. */
    public void contourMap(Map map, int contour){
	String contourName = getContourGraphicalObjectName(map, contour);

	//System.out.println("contour map " + map + " contour " + contour);

	//removeGraphicalObjectsBeginningWith(contourName);

	if(map.getContourDisplayed(contour) == true){
	    //System.out.println("is displayed");
	    if(map.needsReading()){
		//System.out.println("needs reeding");
		determineRegion(map);

		map.setNeedsReading(false);
	    }

	    double contourLevel = map.getContourLevel(contour);
	    int style = map.getContourStyle(contour);

	    Tmesh contourObject =
		contourRegion(map, contour, style);

	    contourObject.setColor(map.getContourColor(contour));

	    contourObject.setName(contourName);

	    contourObject.setVisible(true);

	    //addGraphicalObject(contourObject);
	}else{
	    Tmesh contourObject = getContourGraphicalObject(map, contour);
	    contourObject.setVisible(false);
	}


	//generateMapBoundingBox(map);
    }

    /** The coordinates of the unit cell corners. */
    private static double unitCellCorners[][] = {
	{0., 0., 0.},
	{1., 0., 0.},
	{1., 1., 0.},
	{0., 1., 0.},
	{0., 0., 1.},
	{1., 0., 1.},
	{1., 1., 1.},
	{0., 1., 1.},
    };

    /** Figure out the region of the map we will contour. */
    public void determineRegion(Map map){
	//print.f("contourSize " + contourSize);

	map.read();
	if(map.getMapType() == Map.CCP4_BINARY ||
	   map.getMapType() == Map.O_BINARY){

	    Matrix cartesianToFractional =
		map.getCartesianToFractionalMatrix();
	    Point3d mapCenter = renderer.getCenter();
	    //Symmetry.transformPoint(mapCenter, cartesianToFractional);
	    mapCenter.transform(cartesianToFractional);

	    mapCenter.x *= map.nv[0];
	    mapCenter.y *= map.nv[1];
	    mapCenter.z *= map.nv[2];

	    swapAxes(mapCenter, map);

	    map.centerGrid[0] = (int)(mapCenter.x);
	    map.centerGrid[1] = (int)(mapCenter.y);
	    map.centerGrid[2] = (int)(mapCenter.z);

	    for(int i = 0; i < 3; i++){
		// subtract the map origin
		map.centerGrid[i] -= map.nu[i];
		map.minimumGrid[i] = map.centerGrid[i] - contourSize;
		map.maximumGrid[i] = map.centerGrid[i] + contourSize;

		// force the grid range to lie within the bounds of the map
		if(map.minimumGrid[i] < 0){
		    map.minimumGrid[i] = 0;
		}
		if(map.maximumGrid[i] < 0){
		    map.maximumGrid[i] = 0;
		}
		if(map.minimumGrid[i] >= map.grid[i]){
		    map.minimumGrid[i] = map.grid[i];
		}
		if(map.maximumGrid[i] >= map.grid[i]){
		    map.maximumGrid[i] = map.grid[i];
		}
	    }

	    // now read the region that we identified.
	    map.readRegion();
	}
    }

    /** Contour the data that is in stored in the map. */
    public Tmesh contourRegion(Map map, int contourNumber, int style){
	int nx =0, ny=0, nz=0;
	int mapType = map.getMapType();
	//Tmesh contour = new Tmesh();
	double level = map.getContourLevel(contourNumber);
	Tmesh contour = getContourGraphicalObject(map, contourNumber);

	// remove all the old points from the object.
	contour.empty();

	//style = Map.Surface;
	if(style == Map.Lines){
	    contour.style = Tmesh.LINES;
	}else if(style == Map.Surface){
	    contour.style = Tmesh.TRIANGLES;
	}

	if(mapType == Map.CCP4_BINARY || mapType == Map.O_BINARY){
	    nx = map.maximumGrid[0] - map.minimumGrid[0];
	    ny = map.maximumGrid[1] - map.minimumGrid[1];
	    nz = map.maximumGrid[2] - map.minimumGrid[2];
	    //Log.info("z min %d", map.minimumGrid[2]);
	    //Log.info("z max %d", map.maximumGrid[2]);
	}else{
	    nx = map.ngrid[0];
	    ny = map.ngrid[1];
	    nz = map.ngrid[2];
	}

	double rmsLevel = map.rms * level;

	//Log.info("nx %d", nx);
	//Log.info("ny %d", ny);
	//Log.info("nz %d", nz);

	// make sure we weren't contouring
	// off edge of grid
	if(nx > 0 && ny > 0 && nz > 0){

	    if(style == Map.Lines){
		//System.out.println("about to contour nx "+nx+" ny "+ ny + " nz " + nz);
		March.generateTriangles = false;
		March.surface(map.data, nx, ny, nz, (float)rmsLevel, false, contour);
		//Contour.contour(map.data, nx, ny, nz, rmsLevel, contour);
	    }else if(style == Map.Surface){
		boolean invert = (rmsLevel < 0.0)?true:false;

		March.generateTriangles = true;
		March.surface(map.data, nx, ny, nz, (float)rmsLevel, invert, contour);
	    }
	}

	//System.out.println("contour.np " + contour.np);

	// ok we got the contour back contoured on unit grid
	// now transform all the coordinates into real space
	transformContourPoints(map, contour);

	//System.out.println("contour.np " + contour.np);

	if(style == Map.Surface){
	    //contour.recalculateNormals();
	    //contour.smooth(0.4);
	}

	if(false){
	    System.out.println("contour.np " + contour.np);
	    System.out.println("contour.nt " + contour.nt);
	    System.out.println("total memory " + Runtime.getRuntime().totalMemory());
	}

	//clipContour(contour, map.getRadius());

	return contour;
    }

    /** Clip the contour object to the molecule. */
    public void clipContour(Tmesh contour, double radius){
	/*
	// fix
	Point3d p = new Point3d();
	Point3d center = renderer.getCenter();
	int pointCount = contour.pointCount;
	double radiusSq = radius * radius;
	for(int i = 0; i < pointCount; i++){
	p.set(contour.x[i], contour.y[i], contour.z[i]);

	if(center.distanceSq(p) > radiusSq){
	contour.setClipped(i, true);
	}else{
	contour.setClipped(i, false);
	}				
	}
	*/
    }

    /** Transform the contour points into real space. */
    public void transformContourPoints(Map map, Tmesh contour){
	int pointCount = contour.np;
	Point3d p = new Point3d();
	int mapType = map.getMapType();

	if(mapType == Map.CCP4_BINARY || mapType == Map.O_BINARY){

	    for(int i = 0; i < pointCount; i++){
		map.relativeGridToCartesian(contour.x[i], contour.y[i],
					    contour.z[i], p);

		contour.x[i] = (float)p.x;
		contour.y[i] = (float)p.y;
		contour.z[i] = (float)p.z;
	    }
	}else{
	    for(int i = 0; i < pointCount; i++){
		contour.x[i] *= map.spacing.x;
		contour.y[i] *= map.spacing.y;
		contour.z[i] *= map.spacing.z;
		contour.x[i] += map.origin.x;
		contour.y[i] += map.origin.y;
		contour.z[i] += map.origin.z;
	    }
	}

	if(contour.style == Tmesh.TRIANGLES && mapType == Map.CCP4_BINARY){
	    // swap surface normals round according
	    // to the map header
	    // we will regenerate them later...
	    double xxx[] = new double[3];
	    double swapped[] = new double[3];
	    for(int i = 0; i < contour.np; i++){
		xxx[0] = contour.nx[i];
		xxx[1] = contour.ny[i];
		xxx[2] = contour.nz[i];

		swapped[map.axis[0]] = xxx[0];
		swapped[map.axis[1]] = xxx[1];
		swapped[map.axis[2]] = xxx[2];

		contour.nx[i] = (float)swapped[0];
		contour.ny[i] = (float)swapped[1];
		contour.nz[i] = (float)swapped[2];
	    }
	}
    }

    /** Map the point coordinates back into the map ordering. */
    public void swapAxes(Point3d p, Map map){
	double x = p.x, y = p.y, z = p.z;

	if     (map.axis[0] == 0) p.x = x;
	else if(map.axis[0] == 1) p.x = y;
	else if(map.axis[0] == 2) p.x = z;

	if     (map.axis[1] == 0) p.y = x;
	else if(map.axis[1] == 1) p.y = y;
	else if(map.axis[1] == 2) p.y = z;

	if     (map.axis[2] == 0) p.z = x;
	else if(map.axis[2] == 1) p.z = y;
	else if(map.axis[2] == 2) p.z = z;
    }

    /** Clip around center point. */
    public void clipMaps(){
	DynamicArray atomsInSphere =
	    getAtomsInSphere(renderer.getCenter(), displayRadius);

	clipMaps(null, atomsInSphere, false);
    }

    /** Clip any maps we are displaying to show only unoccupied density. */
    public void clipMaps(String namePattern, DynamicArray selection, boolean inside){

	// reset the last atom to clip as we currently dont have one
	lastAtom = null;
	lastAtomClips = 0;
	lastAtomBondedClips = 0;
	clips = 0;
	//System.out.println("atoms in sphere " + selection.size());

	for(int i = 0; i < getMapCount(); i++){
	    Map map = getMap(i);

	    if(namePattern == null || match.matches(namePattern, map.getName())){
		clipMap(map, selection, inside);
	    }
	}

	System.out.println("clips " + clips);
	System.out.println("last atom clips " + lastAtomClips);
	System.out.println("last atom bonded clips " + lastAtomBondedClips);

	//findMaximaInMaps();

	// recontour the maps we just clipped.
	contourMaps();
    }

    /** Clip this map. */
    public void clipMap(Map map, DynamicArray selection, boolean inside){
	Point3d p = new Point3d();
	float data[] = map.data;
	int point = 0;

	for(int iz = map.minimumGrid[2]; iz < map.maximumGrid[2]; iz++){
	    for(int iy = map.minimumGrid[1]; iy < map.maximumGrid[1]; iy++){
		int gridStart = map.minimumGrid[0];
		int gridStop = map.maximumGrid[0];

		for(int ix = gridStart; ix < gridStop; ix++){
		    double value = data[point];
		    //map.absoluteGridToCartesian(ix, iy, iz, p);
		    map.absoluteGridToCartesian(ix + map.nu[0],
						iy + map.nu[1],
						iz + map.nu[2], p);

		    if(clipped(p, selection) != inside){
			data[point] = 0.0f;
			//System.out.println("clipped");
		    }

		    point++;
		}
	    }
	}
    }

    private static final double clipDistance = 1.5;

    private Atom lastAtom = null;
    private int clips = 0;
    private int lastAtomClips = 0;
    private int lastAtomBondedClips = 0;

    /** Is this point inside an atom in the molecule. */
    public boolean clipped(Point3d p, DynamicArray selection){
	double dSq = clipDistance * clipDistance;

	// first check the last atom that clipped.
	if(lastAtom != null){
	    if(p.distanceSq(lastAtom) < dSq){
		lastAtomClips ++;
		return true;
	    }
	    /*
	    // now check atoms that were bonded to the last atom
	    int bondCount = lastAtom.getBondCount();
	    for(int b = 0; b < bondCount; b++){
	    Atom bondedAtom = lastAtom.getBondedAtom(b);
	    if(p.distanceSq(bondedAtom) < dSq){
	    lastAtom = bondedAtom;
	    lastAtomBondedClips ++;
	    return true;
	    }
	    }
	    */

	    lastAtom = null;
	}

	int atomCount = selection.size();
	Object atoms[] = selection.getArray();

	for(int a = 0; a < atomCount; a++){
	    Atom atom = (Atom)atoms[a];
	    double dx = 0.0;

	    if(atom.x > p.x){
		dx = atom.x - p.x;
	    }else{
		dx = p.x - atom.x;
	    }

	    if(dx < clipDistance){
		if(p.distanceSq(atom) < dSq){
		    lastAtom = atom;
		    clips++;
		    return true;
		}
	    }
	}

	return false;
    }

    /** Are shadows on. */
    public boolean shadows = false;

    /** Has something changed the scene. */
    public boolean dirty = false;

    /** Render passes. */
    private int renderPasses[] = new int[2];

    /** Paint the rendered image into the screen. */
    public synchronized void paint(){
	if(!dirty){
	    return;
	}

	int passCount = 1;

	if(shadows){
	    passCount = 2;
	    renderPasses[0] = Renderer.ShadowsAccumulate;
	    renderPasses[1] = Renderer.ShadowsOn;
	}else{
	    passCount = 1;
	    renderPasses[0] = Renderer.ShadowsOff;
	}

	// this paint method needs completely restructuring
	// this object should have a method registered with
	// the renderer that calls back to here.
	for(int i = 0; i < passCount; i++){
	    renderer.shadowMode = renderPasses[i];

	    renderer.redraw();

	    if(getMoleculeCount() != 0){
		drawMolecules();
	    }

	    renderer.drawObjects();

	    drawMaps();

	    renderer.postProcess();
	}

	dirty = false;

	/*
	  if(renderer.triangleRaysCast > 0 ||
	  renderer.triangleRaysIntersected > 0){
	  FILE.out.print("rays cast %10d\n", renderer.triangleRaysCast);
	  FILE.out.print("rays int  %10d\n", renderer.triangleRaysIntersected);
	  }
	*/
    }

    public void drawMaps(){
	int mapCount = getMapCount();

	for(int m = 0; m < mapCount; m++){
	    Map map = getMap(m);
	    if(map.volumeRender == true){
		drawMap(map);
	    }
	}
    }

    private int splatKernel[] = null;

    private double s[] = {0., 0., 0.};

    public void drawMap(Map map){
	int gp = 0;

	double overallScale = renderer.getOverallScale();

	// seems to be sufficient
	double spacing2 = map.spacing.x * 2.0;

	int pixels = (int)(spacing2 * overallScale + 0.5);
	int pixels2 = (pixels/2) * (pixels/2);

	int splatSize = 2 * pixels + 1;

	// allocate splat kernel
	if(splatKernel == null ||
	   splatKernel.length < splatSize*splatSize){
	    splatKernel = new int[splatSize * splatSize];
	}

	//print.f("pixels " + pixels);

	int index = 0;

	for(int iy = -pixels; iy <= pixels; iy++){
	    for(int ix = -pixels; ix <= pixels; ix++){
		splatKernel[index] =
		    (int)(255.0 * Math.exp(- ( (ix*ix)/(double)pixels2 + (iy*iy)/(double)pixels2)));
		//print.f("%3d ", splatKernel[index]);
		index++;
	    }
	    //print.f("\n");
	}

	double emax = map.volumeMax * map.getSigma();
	double emin = map.volumeMin * map.getSigma();
	int color = map.volumeColor;

	int minTransp = 3;

	for(int k = 0; k < map.ngrid[2]; k++){
	    double z = map.origin.z + k * map.spacing.z;
	    for(int j = 0; j < map.ngrid[1]; j++){
		double y = map.origin.y + j * map.spacing.y;
		for(int i = 0; i < map.ngrid[0]; i++){
		    double x = map.origin.x + i * map.spacing.x;

		    double v = map.data[gp];

		    int op = (int)(255 * (emax - v)/(emax - emin));
		    //int op = (int)(255 * (v - emin)/(emax - emin));


		    //print.f("v " + v + " emin " + emin + " emax " + emax + " op " + op);

		    if(op < 256 && op >= minTransp){
			renderer.applyTransform(x, y, z, s);
			int xs = (int)s[0];
			int ys = (int)s[1];

			if(xs > -pixels && xs < renderer.pixelWidth + pixels &&
			   ys > -pixels && ys < renderer.pixelHeight + pixels){
			    // seems this is the way to get the z-coordinate right...
			    int zs = (int)(s[2] * (1<< (Renderer.FixedBits+8)));

			    if(zs < renderer.frontClip &&
			       zs > renderer.backClip){
				for(int iy = -pixels; iy <= pixels; iy++){
				    int yp = ys + iy;
				    if(yp >= 0 && yp < renderer.pixelHeight){
					index = (iy + pixels) * splatSize;
					for(int ix = -pixels; ix <= pixels; ix++){
					    int xp = xs + ix;
					    if(xp >= 0 && xp < renderer.pixelWidth){
						int intensity = (op * splatKernel[index]) >> 8;

						if(intensity > 0 ){
						    if(intensity > 255){
							intensity = 255;
						    }
						    renderer.blendPixel2(xp, yp, zs,
									 color, intensity);
						}
					    }
					    index++;
					}
				    }
				}
			    }
			}
		    }

		    gp++;
		}
	    }
	}
    }

    /** Figure out the molecule center and scale to make it just fit. */
    public void initialiseCenter(){
	if(renderer.getCenter() == null || true){
	    double xmin = Double.MAX_VALUE;
	    double xmax = -Double.MAX_VALUE;
	    double ymin = Double.MAX_VALUE;
	    double ymax = -Double.MAX_VALUE;
	    double zmin = Double.MAX_VALUE;
	    double zmax = -Double.MAX_VALUE;

	    for(int m = 0; m < getMoleculeCount(); m++){
		Molecule molecule = getMolecule(m);
		for(int a = 0; a < molecule.getAtomCount(); a++){
		    Atom atom = molecule.getAtom(a);
		    if(atom.isSolvent() == false){
			double x = atom.getX();
			double y = atom.getY();
			double z = atom.getZ();
			if(x < xmin) xmin = x;
			if(y < ymin) ymin = y;
			if(z < zmin) zmin = z;
			if(x > xmax) xmax = x;
			if(y > ymax) ymax = y;
			if(z > zmax) zmax = z;
		    }
		}
	    }

	    double xCenter = 0.5 * (xmin + xmax);
	    double yCenter = 0.5 * (ymin + ymax);
	    double zCenter = 0.5 * (zmin + zmax);
	    Point3d moleculeCenter = new Point3d(xCenter, yCenter, zCenter);

	    renderer.setCenter(moleculeCenter);
	}

	if(false || renderer.getRadius() == 0.0){
	    // determine the radius.
	    double radius = 0.0;
	    Point3d moleculeCenter = renderer.getCenter();

	    for(int m = 0; m < getMoleculeCount(); m++){
		Molecule molecule = getMolecule(m);
		for(int a = 0; a < molecule.getAtomCount(); a++){
		    Atom atom = molecule.getAtom(a);
		    if(atom.isSolvent() == false){
			double distance = atom.distance(moleculeCenter);
			if(distance > radius){
			    radius = distance;
			}
		    }
		}
	    }

	    setClip(radius);
	    radius *= 1.05;
	    setRadius(radius);
	}
    }

    /** Reset the transformation matrix. */
    public void resetTransformationMatrix(){
	renderer.rotationMatrix.setIdentity();
    }

    /** Reset the view of the renderer. */
    public void resetView(){
	renderer.resetCenterAndRadius();
	initialiseCenter();
	resetTransformationMatrix();
	renderer.setZoom(1.0);
    }

    /** Print the matrix out. */
    public void printMatrix(){
	renderer.rotationMatrix.print("matrix");

	String command = "animate\n\t-mode\t\trecenter\n\t-matrix\t\t\"";
	Matrix m = renderer.rotationMatrix;

	command += FILE.sprint("%g", m.x00) + FILE.sprint(",%g", m.x01);
	command += FILE.sprint(",%g", m.x02) + FILE.sprint(",%g", m.x03);
	command += FILE.sprint(",%g", m.x10) + FILE.sprint(",%g", m.x11);
	command += FILE.sprint(",%g", m.x12) + FILE.sprint(",%g", m.x13);
	command += FILE.sprint(",%g", m.x20) + FILE.sprint(",%g", m.x21); 
	command += FILE.sprint(",%g", m.x22) + FILE.sprint(",%g", m.x23);
	command += FILE.sprint(",%g", m.x30) + FILE.sprint(",%g", m.x31);
	command += FILE.sprint(",%g", m.x32) + FILE.sprint(",%g", m.x33);

	Point3d p = renderer.getCenter();

	command += "\"\n\t-center\t\t\"";
	command += FILE.sprint("%g,", p.x);
	command += FILE.sprint("%g,", p.y);
	command += FILE.sprint("%g", p.z);
	command += "\"\n";

	command += FILE.sprint("\t-radius\t\t%g\n", (renderer.width/renderer.zoom));
	command += FILE.sprint("\t-clipfront\t%g\n", renderer.front);
	command += FILE.sprint("\t-clipback\t%g\n", renderer.back);
	command += "\t-steps\t\t10\n";
	command += "\t;\n";

	System.out.println("\nCut and paste the command below to recreate the current view");
	System.out.println("Make sure you include the ;\n");

	System.out.println(command);
    }

    /** The length of a cross for an atom with no bonds. */
    private static double crossLength = 0.4;

    DynamicArray sphereAtoms = new DynamicArray(512);

    /** Apply the current transform to the molecule. */
    public void transformMolecule(){
	renderer.buildOverallMatrix();

	int crossPixels = (int)(crossLength * renderer.getOverallScale());

	//renderer.center.print("center");

	int boxPixels = (int)(boxSize * renderer.getOverallScale());
	if(boxPixels == 0){
	    boxPixels = 1;
	}

	sphereAtoms.removeAllElements();

	int size[] = new int[2];

	for(int m = 0; m < getMoleculeCount(); m++){
	    Molecule molecule = getMolecule(m);
	    boolean displayHydrogens = molecule.getBoolean(Molecule.DisplayHydrogens, false);

	    if(molecule.getDisplayed()){
		int style = molecule.getDisplayStyle();
		boolean normal = (style & Molecule.Normal) > 0;
		//int atomCount = molecule.getAtomCount();
		//Object atomArray[] = molecule.getAtomArray();

		int chainCount = molecule.getChainCount();
		for(int c = 0; c < chainCount; c++){
		    Chain chain = molecule.getChain(c);
		    int residueCount = chain.getResidueCount();
		    for(int r = 0; r < residueCount; r++){
			Residue res = chain.getResidue(r);
			int atomCount = res.getAtomCount();
			for(int a = 0; a < atomCount; a++){
			    Atom atom = res.getAtom(a);

			    if(displayHydrogens ||
			       atom.getElement() != PeriodicTable.HYDROGEN){

				//for(int a = 0; a < atomCount; a++){
				//Atom atom = (Atom)atomArray[a];

				atom.transformToScreen(renderer.overallMatrix);

				if((atom.attributes & Atom.VDWSphere) != 0){
				    sphereAtoms.add(atom);
				}

				if((atom.attributes & Atom.BallAndStick) != 0){
				    renderer.drawSphere(atom.x, atom.y, atom.z,
							atom.getBallRadius(),
							atom.getSelectedColor());
				}

				if((atom.attributes & Atom.Cylinder) != 0 &&
				   atom.getBondCount() == 0){
				    renderer.drawAccurateSphere(atom.x, atom.y, atom.z,
								atom.getBallRadius(),
								atom.getSelectedColor(), 255);
				}

				if(atom.isSimpleDisplayed()){
				    if(normal && atom.getBondCount() == 0){
					drawAtom(atom, crossPixels);
				    }

				    if(atom.hasAttributes()){
					if(atom.isLabelled() && displayAtomLabel){
					    int color = Color32.white;

					    if(renderer.getBackgroundColor() ==
					       Color32.white){
						color = Color32.black;
					    }

					    String label = generateAtomLabel(atom);
					    double zoff = atom.getBiggestDisplayedRadius();

					    if(false){
						label = label.substring(label.indexOf(">")+1);
						int pix[] = GoogleFont.makeFontImage(label, 0x000000, 0xffffff, size);
						renderer.drawPixels(atom.x, atom.y, atom.z, size[0], size[1], pix, 0);
					    }else{
						renderer.drawString(atom.x, atom.y, atom.z,
								    zoff,
								    color, label);
					    }
					}

					String format = atom.getCustomLabel();

					if(format != null){
					    int color = Color32.white;

					    if(renderer.getBackgroundColor() ==
					       Color32.white){
						color = Color32.black;
					    }

					    String customLabel = atom.generateLabel(format);
					    double zoff = atom.getBiggestDisplayedRadius();

					    renderer.drawString(atom.x, atom.y, atom.z,
								zoff,
								color,
								customLabel);
					}

					if(atom.isSelected()){
					    //Log.info("atom selected " + atom);
					    renderer.drawBox(atom.xs, atom.ys, atom.zs,
							     boxPixels, Color32.yellow);
					}
				    }
				}
			    }
			}
		    }
		}
	    }
	}

	if(sphereAtoms.size() > 0){
	    sortSphereAtoms();

	    int sphereCount = sphereAtoms.size();

	    for(int i = 0; i < sphereCount; i++){
		Atom satom = (Atom)sphereAtoms.get(i);
		renderer.drawSphere(satom.x, satom.y, satom.z,
				    satom.getVDWRadius(), satom.getSelectedColor(), satom.getTransparency());
	    }
	}
    }

    private void sortSphereAtoms(){
	Object satoms[] = sphereAtoms.getArray();

	if(false){
	    sortRadiiAndColors(satoms, 0, sphereAtoms.size()-1);
	}
    }

    private double radiusColor(Object o){
	Atom a = (Atom)o;
	return a.getVDWRadius();
    }

    private void sortRadiiAndColors(Object sArray[], int L, int R) {
	double m=(radiusColor(sArray[L])+radiusColor(sArray[R]))/2;
	int i=L;
	int j=R;
	Object temp;

	do {
	    while (radiusColor(sArray[i])>m) i++;
	    while (radiusColor(sArray[j])<m) j--;

	    if (i<=j) {
		temp=sArray[i];
		sArray[i]=sArray[j];
		sArray[j]=temp;
		i++;
		j--;
	    }
	} while (j>=i);

	if (L<j) sortRadiiAndColors(sArray,L,j);
	if (R>i) sortRadiiAndColors(sArray,i,R);
    }

    private Molecule currentMolecule = null;

    /** Draw the molecule. */
    public void drawMolecules(){
	transformMolecule();

	for(int m = 0; m < getMoleculeCount(); m++){
	    Molecule molecule = getMolecule(m);
	    boolean displayHydrogens = molecule.getBoolean(Molecule.DisplayHydrogens, false);

	    if(molecule.getDisplayed()){
		int style = molecule.getDisplayStyle();

		if((style & Molecule.Normal) == Molecule.Normal){
		    currentMolecule = molecule;

		    if(allowFastDraw){
			fastDraw = !molecule.getBoolean(Molecule.DisplayBondDetails, true);
		    }else{
			fastDraw = true;
		    }

		    int chainCount = molecule.getChainCount();
		    for(int c = 0; c < chainCount; c++){
			Chain chain = molecule.getChain(c);
			int residueCount = chain.getResidueCount();
			for(int r = 0; r < residueCount; r++){
			    Residue res = chain.getResidue(r);

			    if(res.getBoolean(Residue.Torsions, false)){
				//drawTorsions(res);
			    }

			    int atomCount = res.getAtomCount();
			    for(int a = 0; a < atomCount; a++){
				Atom atom = res.getAtom(a);
				int bondCount = atom.getBondCount();
				for(int b = 0; b < bondCount; b++){
				    Bond bond = atom.getBond(b);
				    Atom firstAtom = bond.getFirstAtom();

				    if(displayHydrogens ||
				       firstAtom.getElement() != PeriodicTable.HYDROGEN){

					if(atom == firstAtom){
					    Atom secondAtom = bond.getSecondAtom();
					    if(displayHydrogens ||
					       secondAtom.getElement() != PeriodicTable.HYDROGEN){

						if(firstAtom.isSimpleDisplayed()){
						    if(secondAtom.isSimpleDisplayed()){
							double w = -bond.getBondWidth();
							drawBond(bond, w);
						    }
						}

						if((firstAtom.attributes & Atom.Cylinder) != 0 &&
						   (secondAtom.attributes & Atom.Cylinder) != 0){
						    //drawCylinderBond(bond, Atom.Cylinder);
						    double w = bond.getCylinderWidth();
						    drawBond(bond, w);
						}

						if((firstAtom.attributes & Atom.BallAndStick) != 0 &&
						   (secondAtom.attributes & Atom.BallAndStick) != 0){
						    double w = bond.getStickWidth();
						    drawSimpleBond(bond, w);
						}
					    }
					}
				    }
				}
			    }
			}
		    }

		    /*
		    // old way of drawing bonds.
		    int bondCount = molecule.getBondCount();
		    Object bondArray[] = molecule.getBondArray();

		    for(int b = 0; b < bondCount; b++){
		    Bond bond = (Bond)bondArray[b];
		    // changes to support display masks
		    Atom firstAtom = bond.getFirstAtom();
		    Atom secondAtom = bond.getSecondAtom();

		    if(firstAtom.isSimpleDisplayed()){
		    if(secondAtom.isSimpleDisplayed()){
		    double w = -bond.getBondWidth();
		    drawBond(bond, w);
		    }
		    }

		    if((firstAtom.attributes & Atom.Cylinder) != 0 &&
		    (secondAtom.attributes & Atom.Cylinder) != 0){
		    //drawCylinderBond(bond, Atom.Cylinder);
		    double w = bond.getCylinderWidth();
		    drawBond(bond, w);
		    }

		    if((firstAtom.attributes & Atom.BallAndStick) != 0 &&
		    (secondAtom.attributes & Atom.BallAndStick) != 0){
		    double w = bond.getStickWidth();
		    drawSimpleBond(bond, w);
		    }
		    }
		    */
		}

		if((style & Molecule.Trace) == Molecule.Trace){
		    drawTrace(molecule);
		}
	    }
	}


	//drawLabelledAtoms();

	//drawSelectedAtoms();

	drawBumpPairs();

	drawDistances();

	drawAngles();

	drawTorsions();

	drawHbonds();
    }

    Atom atoms[] = new Atom[4];

    /*
      private void drawTorsions(Residue res){
      astex.xmt.ResidueDictionary rd =
      astex.xmt.XMT.getResidueDictionary(res);

      for(int t = 0; t < rd.torsionNames.size(); t++){
      StringArray torsionAtoms = (StringArray)rd.torsionAtoms.get(t);
      boolean allDefined = true;
      for(int a = 0; a < 4; a++){
      atoms[a] = res.findAtom(torsionAtoms.get(a));
      if(atoms[a] == null){
      allDefined = false;
      break;
      }
      }

      if(allDefined){
      drawTorsion(res, rd.torsionNames.get(t), atoms[0], atoms[1], atoms[2], atoms[3]);
      }
      }
      }
    */

    private Point3d ta01 = new Point3d();
    private Point3d ta12 = new Point3d();
    private Point3d ta23 = new Point3d();
    private Point3d n012 = new Point3d();
    private Point3d n123 = new Point3d();
    private Point3d p01 = new Point3d();
    private Point3d p23 = new Point3d();
    private Point3d m12 = new Point3d();
    private Point3d n = new Point3d();
    private Point3d last = new Point3d();
    private Point3d arc = new Point3d();

    private void drawTorsion(Generic g, String label,
			     Point3d a0, Point3d a1,
			     Point3d a2, Point3d a3){
	Point3d.unitVector(ta01, a0, a1);
	Point3d.unitVector(ta12, a1, a2);
	Point3d.unitVector(ta23, a2, a3);
	Point3d.cross(n012, ta01, ta12);
	Point3d.cross(n123, ta12, ta23);
	Point3d.cross(p01, n012, ta12);
	Point3d.cross(p23, n123, ta12);
	Point3d.mid(m12, a1, a2);

	// second orthogonal vector
	Point3d.cross(n, ta12, p01);

	double radius = g.getDouble("torsionRadius", 0.3);

	int lineColor = Color32.magenta;
	double lineRadius = 0.015;

	if(false){
	    renderer.drawCylinder(m12.x, m12.y, m12.z,
				  m12.x + radius * p01.x,
				  m12.y + radius * p01.y,
				  m12.z + radius * p01.z,
				  lineColor, lineColor, lineRadius);
	}

	if(true){
	    double dot = radius/p01.dot(ta01);

	    renderer.drawCylinder(m12.x + radius * p01.x,
				  m12.y + radius * p01.y,
				  m12.z + radius * p01.z,
				  a1.x + dot * ta01.x,
				  a1.y + dot * ta01.y,
				  a1.z + dot * ta01.z,
				  lineColor, lineColor, lineRadius);
	}

	// second part
	if(false){
	    renderer.drawCylinder(m12.x, m12.y, m12.z,
				  m12.x + radius * p23.x,
				  m12.y + radius * p23.y,
				  m12.z + radius * p23.z,
				  lineColor, lineColor, lineRadius);
	}

	if(true){
	    double dot = radius/p23.dot(ta23);

	    renderer.drawCylinder(m12.x + radius * p23.x,
				  m12.y + radius * p23.y,
				  m12.z + radius * p23.z,
				  a2.x + dot * ta23.x,
				  a2.y + dot * ta23.y,
				  a2.z + dot * ta23.z,
				  lineColor, lineColor, lineRadius);
	}

	// torsion angle
	double t = Point3d.torsion(a0, a1, a2, a3);

	// arc
	double step = g.getDouble("torsionStep", 5.0);
	step *= (Math.PI/180.0);

	if(t < 0.0){
	    step = -step;
	}

	int steps = 1 + (int)((t/step) + 0.5);
	double angle = 0.0;

	for(int s = 0; s < steps; s++){
	    double ct = Math.cos(angle);
	    double st = Math.sin(angle);

	    arc.x = m12.x + radius * (ct * p01.x + st * n.x);
	    arc.y = m12.y + radius * (ct * p01.y + st * n.y);
	    arc.z = m12.z + radius * (ct * p01.z + st * n.z);

	    if(s > 0){
		renderer.drawCylinder(arc.x, arc.y, arc.z,
				      last.x, last.y, last.z,
				      lineColor, lineColor, lineRadius);
	    }

	    last.set(arc);

	    angle += step;
	}


	String format = g.getString(Residue.TorsionFormat, "%t %.1f");

	format = Util.replace(format, "%t", label);

	if(g.getBoolean(Residue.TorsionGreek, false)){
	    if(false){
		format = Util.replace(format, "chi", "\\w");
		format = Util.replace(format, "phi", "\\v");
		format = Util.replace(format, "psi", "\\x");
		format = Util.replace(format, "omega", "\\o");
	    }else{
		format = Util.replace(format, "chi", "\\c");
		format = Util.replace(format, "phi", "\\f");
		format = Util.replace(format, "psi", "\\y");
		format = Util.replace(format, "omega", "\\w");
	    }
	}

	format = FILE.sprint(format, t * 180.0/Math.PI);

	renderer.drawString(m12.x, m12.y, m12.z, 1.0, Color32.white, format);
    }

    /** Draw the molecule as a trace. */
    public void drawTrace(Molecule molecule){
	int atomCount = molecule.getAtomCount();
	Atom previous = null;

	for(int i = 0; i < atomCount; i++){
	    Atom a = molecule.getAtom(i);
	    if(a.getAtomLabel().equals("CA")){
		if(previous != null && previous.distance(a) < 4.1 &&
		   previous.isDisplayed() && a.isDisplayed()){
		    int previousColor = previous.getColor();
		    int atomColor = a.getColor();

		    int width = 2;

		    if(previousColor == atomColor){
			// one line
			//renderer.drawLine(previous.xs, previous.ys, previous.zs,
			//		  a.xs, a.ys, a.zs,
			//		  previousColor, previousColor, width);
			drawLine(previous.xs, previous.ys, previous.zs,
				 a.xs, a.ys, a.zs,
				 previousColor, previousColor, -width);
		    }else{
			//renderer.drawLine(previous.xs, previous.ys, a.zs,
			//		  a.xs, a.ys, a.zs,
			//		  previousColor, atomColor,
			//		  width);
			drawLine(previous.xs, previous.ys, a.zs,
				 a.xs, a.ys, a.zs,
				 previousColor, atomColor,
				 -width);
		    }
		}

		previous = a;
	    }
	}
    }

    /** Draw the distance markers. */
    public void drawDistances(){
	if(displayDistances){

	    int distanceCount = distances.size();

	    for(int i = 0; i < distanceCount; i++){
		Distance distance = (Distance)distances.get(i);
		drawDistanceObject(distance);
	    }
	}
    }

    /**
     * Draw the specific distance.
     * The dash on and off values are adjusted
     * to give a half off gap at each end of the
     * line.
     */
    public void drawDistanceObject(Distance distance){
	if(distance.getBoolean(Distance.Visible, true) == false) return;

	if(distance.getInteger(Distance.Mode, -1) == Distance.Centroids){
	    if(distance.valid()){
		if(distance.group0.size() > 0){
		    Point3d g0 = distance.getCenter0();
		    Point3d g1 = distance.getCenter1();

		    drawDashedLine(g0.x, g0.y, g0.z,
				   g1.x, g1.y, g1.z,
				   distance.getDouble(Distance.On, 0.2),
				   distance.getDouble(Distance.Off, 0.2),
				   distance.getDouble(Distance.Radius, -1.0),
				   ((Color)distance.get(Distance.Color, Color.white)).getRGB());
		    //distance.on, distance.off, distance.radius,
		    //distance.color);

		    drawDistanceMarker(g0, g1, distance);
		}
	    }
	}else{
	    int distanceCount = distance.group0.size();

	    for(int i = 0; i < distanceCount; i++){
		Atom g0 = (Atom)distance.group0.get(i);
		Atom g1 = (Atom)distance.group1.get(i);

		if(g0.isDisplayed() && g1.isDisplayed()){
		    drawDashedLine(g0.x, g0.y, g0.z,
				   g1.x, g1.y, g1.z,
				   distance.getDouble(Distance.On, 0.2),
				   distance.getDouble(Distance.Off, 0.2),
				   distance.getDouble(Distance.Radius, -1.0),
				   ((Color)distance.get(Distance.Color, Color.white)).getRGB());
		    //distance.on, distance.off, distance.radius,
		    //               distance.color);
		    drawDistanceMarker(g0, g1, distance);
		}
	    }
	}
    }

    /** Space for the dashed line drawer. */
    private Point3d dla = new Point3d();
    private Point3d dlb = new Point3d();

    /** Draw distance marker. */
    private void drawDistanceMarker(Point3d g0, Point3d g1, Distance d){

	//if(d.format != null){
	String label = d.getString(Distance.Format, null);
	if(label != null){

	    // if there is a format character 
	    // calculate and subsitute the distance.
	    if(label.indexOf('%') != -1){
		double len = g0.distance(g1);
		label = FILE.sprint(label, len);
	    }

	    double mx = 0.5 * (g0.x + g1.x);
	    double my = 0.5 * (g0.y + g1.y);
	    double mz = 0.5 * (g0.z + g1.z);

	    renderer.drawString(mx, my, mz, 0.5,
				((Color)d.get(Distance.LabelColor, Color.white)).getRGB(),
				label);
	}
    }

    /** Draw the distance marker line. */
    public void drawDashedLine(double ax, double ay, double az,
			       double bx, double by, double bz,
			       double on, double off, double radius,
			       int color){
	dla.set(ax, ay, az);
	dlb.set(bx, by, bz);

	Point3d v = Point3d.unitVector(dla, dlb);

	double len = dla.distance(dlb);

	double totalDash = on + off;
	double scale     = (len/totalDash)/(int)(len / totalDash);

	on  = on * scale;
	off = off * scale;
	double d = 0.5 * off;

	if(Math.abs(off) < 1.e-3 || Math.abs(on) < 1.e-3){
	    drawLine(dla.x, dla.y, dla.z, dlb.x, dlb.y, dlb.z,
		     color, color, radius);
	}else{
	    while((d + on) < (len)){
		double dend = d + on;
		drawLine(dla.x + d    * v.x, dla.y + d    * v.y, dla.z + d    * v.z,
			 dla.x + dend * v.x, dla.y + dend * v.y, dla.z + dend * v.z,
			 color, color, radius);
		d += on + off;
	    }
	}
    }


    /** Draw the distance markers. */
    public void drawDistances2(){
	if(displayDistances){
	    int distanceCount = distances.size();

	    for(int i = 0; i < distanceCount; i++){
		Bond distance = (Bond)distances.get(i);
		Atom firstAtom = distance.getFirstAtom();
		Atom secondAtom = distance.getSecondAtom();

		if(firstAtom.isDisplayed() && secondAtom.isDisplayed()){
		    Molecule firstMolecule = firstAtom.getMolecule();
		    Molecule secondMolecule = secondAtom.getMolecule();

		    if(firstMolecule.getDisplayed() == true &&
		       secondMolecule.getDisplayed() == true){
			drawDistance(firstAtom, secondAtom, true);
		    }
		}
	    }
	}
    }

    /** Format for angles. */
    private Format angleFormat = new Format("%.1f");

    private final Generic dummyGeneric = new Generic("dummy");

    /** Draw the angle markers. */
    public void drawTorsions(){
	int count = torsions.size();

	for(int i = 0; i < count; i += 4){
	    boolean drawTorsion = true;

	    // make sure that we don't draw torsions
	    // when the atoms aren't displayed
	    for(int j = 0; j < 4; j++){
		Atom aa = (Atom)torsions.get(i + j);
		Molecule mol = aa.getMolecule();

		if(aa.isDisplayed() == false ||
		   mol.getDisplayed() == false){
		    drawTorsion = false;
		}
	    }

	    if(drawTorsion){
		Atom a1 = (Atom)torsions.get(i);
		Atom a2 = (Atom)torsions.get(i+1);
		Atom a3 = (Atom)torsions.get(i+2);
		Atom a4 = (Atom)torsions.get(i+3);

		drawTorsion(dummyGeneric, "", a1, a2, a3, a4);
	    }
	}
    }

    /**
     * Draw a torsion marker.
     * Very simple initial implementation.
     */
    public void drawTorsion(Atom a1, Atom a2, Atom a3, Atom a4){
	//int xm = (a1.xs + a2.xs + a3.xs + a4.xs)/4;
	//int ym = (a1.ys + a2.ys + a3.ys + a4.ys)/4;
	//int zm = (a1.zs + a2.zs + a3.zs + a4.zs)/4;
	double xm = (a1.x + a2.x + a3.x + a4.x)/4;
	double ym = (a1.y + a2.y + a3.y + a4.y)/4;
	double zm = (a1.z + a2.z + a3.z + a4.z)/4;

	double torsion = Point3d.torsionDegrees(a1, a2, a3, a4);

	String label = angleFormat.format(torsion);

	renderer.drawString(xm, ym, zm, Color32.white, label);
    }

    /** Draw the angle markers. */
    public void drawAngles(){
	int count = angles.size();

	for(int i = 0; i < count; i += 3){
	    boolean drawAngle = true;

	    // make sure that we don't draw angles
	    // when the atoms aren't displayed
	    for(int j = 0; j < 3; j++){
		Atom aa = (Atom)angles.get(i + j);
		Molecule mol = aa.getMolecule();

		if(aa.isDisplayed() == false ||
		   mol.getDisplayed() == false){
		    drawAngle = false;
		}
	    }

	    if(drawAngle){
		Atom a1 = (Atom)angles.get(i);
		Atom a2 = (Atom)angles.get(i+1);
		Atom a3 = (Atom)angles.get(i+2);

		drawAngle(a1, a2, a3);
	    }
	}
    }

    /**
     * Draw an angle marker.
     * Very simple initial implementation.
     */
    public void drawAngle(Atom a1, Atom a2, Atom a3){
	double xm = (a1.x + a2.x + a3.x)/3.0;
	double ym = (a1.y + a2.y + a3.y)/3.0;
	double zm = (a1.z + a2.z + a3.z)/3.0;

	double angle = Point3d.angleDegrees(a1, a2, a3);

	String label = angleFormat.format(angle);

	renderer.drawString(xm, ym, zm, Color32.yellow, label);
    }

    /** Draw the bump atoms. */
    public void drawBumpPairs(){
	DynamicArray bumpPairs = getBumpAtoms();
	int bumpCount = bumpPairs.size();

	for(int i = 0; i < bumpCount; i += 2){
	    Atom atom1 = (Atom)bumpPairs.get(i);
	    Atom atom2 = (Atom)bumpPairs.get(i + 1);
	    drawDistance(atom1, atom2, true);
	}
    }

    /** Draw hydrogen bonds. */
    private void drawHbonds(){
	int hbondCount = hbonds.size();

	for(int i = 0; i < hbondCount; i++){
	    Bond hbond = (Bond)hbonds.get(i);
	    Atom atom0 = hbond.getAtom(0);
	    Atom atom1 = hbond.getAtom(1);

	    if(atom0.isDisplayed() && atom1.isDisplayed()){
		drawDistance(atom0, atom1, false);
	    }
	}
    }

    /** Format for distances. */
    private Format distanceFormat = new Format("%.2fA");

    /** Draw a distance between two atoms. */
    public void drawDistance(Atom atom1, Atom atom2, boolean displayDistance){
	Molecule molecule1 = atom1.getMolecule();
	Molecule molecule2 = atom2.getMolecule();

	if(molecule1.getDisplayStyle(Molecule.Normal) &&
	   molecule2.getDisplayStyle(Molecule.Normal) &&
	   molecule1.getDisplayed() == true &&
	   molecule2.getDisplayed() == true){
	    if(atom1.isDisplayed() && atom2.isDisplayed()){
		drawDottedLine(atom1, atom2, 0.2, Color32.white);

		if(displayDistance){
		    double xm = (atom1.x + atom2.x)/2;
		    double ym = (atom1.y + atom2.y)/2;
		    double zm = (atom1.z + atom2.z)/2;

		    double d = atom1.distance(atom2);

		    String label = distanceFormat.format(d);

		    renderer.drawString(xm, ym, zm, Color32.white, label);
		}
	    }
	}
    }

    /** Dummy Atom for transforming dot points. */
    private Atom dummyAtom = Atom.create();

    /** Draw a dotted line. */
    public void drawDottedLine(Atom atom1, Atom atom2,
			       double gap, int color){
	double d = atom1.distance(atom2);
	double current = gap;

	Point3d v12 = Point3d.unitVector(atom1, atom2);

	while(current < d){
	    dummyAtom.set(atom1.x + v12.x * current,
			  atom1.y + v12.y * current, 
			  atom1.z + v12.z * current);

	    dummyAtom.transformToScreen(renderer.overallMatrix);

	    renderer.drawDot(dummyAtom.xs, dummyAtom.ys, dummyAtom.zs, color);

	    current += gap;
	}
    }

    /** Draw a dotted line. */
    public void drawTwinColourDottedLine(Atom atom1, Atom atom2,
					 double gap,
					 int color1, int color2){
	double d = atom1.distance(atom2);
	double d2 = d * 0.5;
	double current = gap;
	int color = color1;

	Point3d v12 = Point3d.unitVector(atom1, atom2);

	while(current < d){
	    dummyAtom.set(atom1.x + v12.x * current,
			  atom1.y + v12.y * current, 
			  atom1.z + v12.z * current);

	    dummyAtom.transformToScreen(renderer.overallMatrix);

	    // switch the colour at the half way point.
	    if(current > d2){
		color = color2;
	    }

	    //int shade = getShade(color, dummyAtom.zs);
	    int shade = 0;
	    renderer.setPixel(dummyAtom.xs, dummyAtom.ys, dummyAtom.zs, shade);

	    current += gap;
	}
    }

    /** Draw all of the labelled atoms. */
    public void drawLabelledAtoms(){
	DynamicArray labelledAtoms = getLabelledAtoms();

	for(int i = 0; i < labelledAtoms.size(); i++){
	    Atom atom = (Atom)labelledAtoms.get(i);
	    String label = generateAtomLabel(atom);

	    renderer.drawString(atom.x, atom.y, atom.z,
				Color32.white, label);

	    /*
	      if(hersheyFonts){
	      double zoff = atom.getBiggestDisplayedRadius();
	      renderer.drawHersheyString(atom.x, atom.y, atom.z,
	      zoff,
	      0.5,
	      Color32.white, label);
	      }else{
	      renderer.drawString(atom.xs, atom.ys, atom.zs,
	      Color32.white, label);
	      }
	    */
	}
    }

    /** Size of box for selected atoms in Angstroms. */
    private static double boxSize = 0.05;

    /** Default format for short atom labels. */
    private static String defaultShortFormat = null;

    /** Set the label format that is used for atoms on screen. */
    public void setAtomLabelFormat(String s){
	defaultShortFormat = s;
    }

    /** Get the atom label format. */
    public String getAtomLabelFormat(){
	return defaultShortFormat;
    }

    /** Generate the atom label. */
    public String generateAtomLabel(Atom atom){
	if(defaultShortFormat == null){
	    defaultShortFormat = Settings.getString("config", "atom.short.format");
	    if(defaultShortFormat == null){
		// no config value here
		defaultShortFormat = "%a %R:%c%r%I";
	    }
	}

	return atom.generateLabel(defaultShortFormat);
    }

    /** Generate an atom label according to the format statments. */
    public void generateAtomLabels(String format, DynamicArray selectedAtoms){
	for(int i = 0; i < selectedAtoms.size(); i++){
	    Atom a = (Atom)selectedAtoms.get(i);

	    //String label = a.generateLabel(format);

	    a.setCustomLabel(format);
	}
    }

    /** Draw one atom. */
    public void drawAtom(Atom atom, int crossPixels){
	if(displaySolvent || atom.isSolvent() == false){
	    int z = atom.zs;
	    int atomColor = atom.getColor();

	    if(crossPixels == 1){
		//int shade = getShade(atomColor, z);
		renderer.drawDot(atom.xs, atom.ys, z, atomColor);
	    }else{
		crossPixels <<= Renderer.FixedBits;
		crossPixels /= 2;
		int x = atom.xs;
		int y = atom.ys;
		//renderer.drawLine(x - crossPixels, y, z,
		//		  x + crossPixels, y, z, atomColor, atomColor, 1);
		//renderer.drawLine(x, y - crossPixels, z,
		//		  x, y + crossPixels, z, atomColor, atomColor, 1);
		drawLine(x - crossPixels, y, z,
			 x + crossPixels, y, z, atomColor, atomColor, -1);
		drawLine(x, y - crossPixels, z,
			 x, y + crossPixels, z, atomColor, atomColor, -1);
	    }
	}
    }

    /** Draw one bond. */
    public void drawBond(Bond bond, double w){
	if(fastDraw || bond.getBondOrder() == Bond.SingleBond){
	    drawSimpleBond(bond, w);
	}else{
	    drawDetailedBond(bond, w);
	}
    }

    /** Set whether or not we display bond types. */
    public void displayBondTypes(boolean b){
	allowFastDraw = b;
    }

    /** Draw a bond that shows the bond order. */
    private void drawDetailedBond(Bond bond, double w){
	int bondOrder = bond.getBondOrder();

	if(bondOrder == Bond.DoubleBond ||
	   bondOrder == Bond.AromaticBond){
	    drawDoubleBond(bond, false, w > 0.0 ? w * doubleBondRadiusScale : w);
	    drawSimpleBond(bond, w);
	}else if(bondOrder == Bond.TripleBond){
	    drawDoubleBond(bond, true, w > 0.0 ? w * doubleBondRadiusScale : w);
	    drawSimpleBond(bond, w);
	}else{
	    drawSimpleBond(bond, w);
	}
    }

    private int s1[] = new int[3];
    private int s2[] = new int[3];

    private double doubleBondOffset = 0.35;
    private double doubleBondRadiusScale = 0.4;

    private Atom dummyAtom1 = Atom.create();
    private Atom dummyAtom2 = Atom.create();

    private double aromaticBondDotGap = 0.2;

    /** Draw a double bond. */
    private void drawDoubleBond(Bond bond, boolean triple, double w){
	Atom firstAtom = bond.getAtom(0);
	Atom secondAtom = bond.getAtom(1);
	int firstAtomColor = firstAtom.getColor();
	int secondAtomColor = secondAtom.getColor();

	Ring ring = currentMolecule.getBestRingContainingBond(bond);

	if(ring != null){
	    Point3d center = ring.getRingCenter();
	    Point3d mid = new Point3d();
	    mid.add(firstAtom);
	    mid.add(secondAtom);
	    mid.scale(0.5);
	    Point3d mid2Center = Point3d.vector(mid, center);
	    double length = mid2Center.length();
	    double scale = doubleBondOffset / length;
	    Point3d first2Center = Point3d.vector(firstAtom, center);
	    Point3d second2Center = Point3d.vector(secondAtom, center);
	    first2Center.scale(scale);
	    second2Center.scale(scale);
	    first2Center.add(firstAtom);
	    second2Center.add(secondAtom);

	    if(bond.getBondOrder() == Bond.AromaticBond){
		dummyAtom1.set(first2Center);
		dummyAtom2.set(second2Center);
		drawTwinColourDottedLine(dummyAtom1, dummyAtom2, aromaticBondDotGap,
					 firstAtomColor, secondAtomColor);
	    }else{
		//renderer.drawLine(first2Center.x, first2Center.y, first2Center.z,
		//		  second2Center.x, second2Center.y, second2Center.z,
		//		  firstAtomColor, secondAtomColor,
		//		  width);
		drawLine(first2Center.x, first2Center.y, first2Center.z,
			 second2Center.x, second2Center.y, second2Center.z,
			 firstAtomColor, secondAtomColor, w);
	    }
	}else{
	    Point3d first2second = Point3d.unitVector(firstAtom, secondAtom);
	    first2second.normalise();
	    //	    Point3d normal = Point3d.normalToLine(first2second);
	    Point3d normal = getDoubleBondOffsetVector(firstAtom, secondAtom);
	    normal.scale(doubleBondOffset);
	    first2second.scale(0.1);

	    Point3d firstEnd = new Point3d(firstAtom);
	    firstEnd.add(normal);
	    if(firstAtom.getBondCount() > 1){
		firstEnd.add(first2second);
	    }

	    Point3d secondEnd = new Point3d(secondAtom);
	    secondEnd.add(normal);
	    if(secondAtom.getBondCount() > 1){
		secondEnd.subtract(first2second);
	    }

	    if(bond.getBondOrder() == Bond.AromaticBond){
		dummyAtom1.set(firstEnd);
		dummyAtom2.set(secondEnd);
		drawTwinColourDottedLine(dummyAtom1, dummyAtom2, aromaticBondDotGap,
					 firstAtomColor, secondAtomColor);
	    }else{
		//renderer.drawLine(firstEnd.x, firstEnd.y, firstEnd.z,
		//		  secondEnd.x, secondEnd.y, secondEnd.z,
		//		  firstAtomColor, secondAtomColor,
		//		  width);
		drawLine(firstEnd.x, firstEnd.y, firstEnd.z,
			 secondEnd.x, secondEnd.y, secondEnd.z,
			 firstAtomColor, secondAtomColor, w);
	    }

	    if(triple){
		firstEnd = new Point3d(firstAtom);
		firstEnd.subtract(normal);
		if(firstAtom.getBondCount() > 1){
		    firstEnd.add(first2second);
		}

		secondEnd = new Point3d(secondAtom);
		secondEnd.subtract(normal);
		if(secondAtom.getBondCount() > 1){
		    secondEnd.subtract(first2second);
		}

		//renderer.drawLine(firstEnd.x, firstEnd.y, firstEnd.z,
		//		  secondEnd.x, secondEnd.y, secondEnd.z,
		//		  firstAtomColor, secondAtomColor,
		//		  width);

		drawLine(firstEnd.x, firstEnd.y, firstEnd.z,
			 secondEnd.x, secondEnd.y, secondEnd.z,
			 firstAtomColor, secondAtomColor, w);
	    }
	}
    }

    /** Direction of double bond offset for non-ring double bond. */
    private Point3d getDoubleBondOffsetVector(Atom firstAtom, Atom secondAtom){
	Point3d first2second = Point3d.unitVector(firstAtom, secondAtom);
	first2second.normalise();
	Atom targetAtom = null;
	Atom otherAtom = null;
	if(firstAtom.getBondCount() > 1){
	    targetAtom = firstAtom;
	    otherAtom = secondAtom;
	}else if(secondAtom.getBondCount() > 1){
	    targetAtom = secondAtom;
	    otherAtom = firstAtom;
	}

	if(targetAtom != null){
	    // now find a relevant bond.
	    int bondCount = targetAtom.getBondCount();

	    for(int i = 0; i < bondCount;i++){
		Atom a = targetAtom.getBondedAtom(i);
		if(a != otherAtom){
		    // this will do for the vector.
		    Point3d dir = Point3d.unitVector(targetAtom, a);
		    Point3d reference = dir.cross(first2second);
		    reference.normalise();
		    Point3d normal = first2second.cross(reference);
		    normal.normalise();
		    return normal;
		}
	    }
	}

	return Point3d.normalToLine(first2second);
    }

    /** Draw a triple bond. */
    private void drawTripleBond(Bond bond, int w){
    }


    private double bondLineRadius = -1.0;

    /** Draw a simple bond just made up of a colored line. */
    private void drawSimpleBond(Bond bond, double w){

	Atom firstAtom = bond.getAtom(0);
	Atom secondAtom = bond.getAtom(1);

	int width = bond.getBondWidth();

	int firstAtomColor = firstAtom.getColor();
	int secondAtomColor = secondAtom.getColor();

	//renderer.drawLine(firstAtom.xs, firstAtom.ys, firstAtom.zs,
	//		  secondAtom.xs, secondAtom.ys, secondAtom.zs,
	//		  firstAtomColor, secondAtomColor, width);

	if(w < 0.0){
	    if(renderer.shadowMode != Renderer.ShadowsOff){
		drawLine(firstAtom.x, firstAtom.y, firstAtom.z,
			 secondAtom.x, secondAtom.y, secondAtom.z,
			 firstAtomColor, secondAtomColor, (double)(-w * bondLineRadius));
	    }else{
		drawLine(firstAtom.xs, firstAtom.ys, firstAtom.zs,
			 secondAtom.xs, secondAtom.ys, secondAtom.zs,
			 firstAtomColor, secondAtomColor, w);
	    }
	}else{
	    if(firstAtom.isSelected()){
		firstAtomColor = Color32.yellow;
	    }
	    if(secondAtom.isSelected()){
		secondAtomColor = Color32.yellow;
	    }

	    drawLine(firstAtom.x, firstAtom.y, firstAtom.z,
		     secondAtom.x, secondAtom.y, secondAtom.z,
		     firstAtomColor, secondAtomColor, w);
	}
    }

    /** Entry point for line/cylinder drawing. */
    private void drawLine(int x1, int y1, int z1,
			  int x2, int y2, int z2,
			  int rgb1, int rgb2, double width){
	if(width < 0.0){
	    int iw = (int)(-width + 0.5);

	    renderer.drawLine(x1, y1, z1, x2, y2, z2, rgb1, rgb2, iw);
	}else{
	    renderer.drawCylinder(x1, y1, z1, x2, y2, z2, rgb1, rgb2, width);
	}
    }

    /** Entry point for line/cylinder drawing. */
    private void drawLine(double x1, double y1, double z1,
			  double x2, double y2, double z2,
			  int rgb1, int rgb2, double width){
	if(width < 0.0){
	    int iw = (int)(-width + 0.5);

	    renderer.drawLine(x1, y1, z1, x2, y2, z2, rgb1, rgb2, iw);
	}else{
	    renderer.drawCylinder(x1, y1, z1, x2, y2, z2, rgb1, rgb2, width);
	}
    }

    /** Draw a simple bond just made up of a colored line. */
    private void drawCylinderBond(Bond bond){

	Atom firstAtom = bond.getAtom(0);
	Atom secondAtom = bond.getAtom(1);

	int firstAtomColor = firstAtom.getSelectedColor();
	int secondAtomColor = secondAtom.getSelectedColor();

	//renderer.drawCylinder(firstAtom.x, firstAtom.y, firstAtom.z,
	//		      secondAtom.x, secondAtom.y, secondAtom.z,
	//		      firstAtomColor, secondAtomColor, bond.getCylinderWidth());
	drawLine(firstAtom.x, firstAtom.y, firstAtom.z,
		 secondAtom.x, secondAtom.y, secondAtom.z,
		 firstAtomColor, secondAtomColor, bond.getCylinderWidth());
    }

    /** Draw a simple bond just made up of a colored line. */
    private void drawStickBond(Bond bond, double w){

	Atom firstAtom = bond.getAtom(0);
	Atom secondAtom = bond.getAtom(1);

	int stickColor = bond.getBondColor();

	//renderer.drawCylinder(firstAtom.x, firstAtom.y, firstAtom.z,
	//		      secondAtom.x, secondAtom.y, secondAtom.z,
	//		      stickColor, stickColor, bond.getStickWidth());

	drawLine(firstAtom.x, firstAtom.y, firstAtom.z,
		 secondAtom.x, secondAtom.y, secondAtom.z,
		 stickColor, stickColor, w);
    }

    /** Find atom with screen coordinates nearest to the specified point. */
    public Atom getNearestAtom(int x, int y){
	Atom nearestAtom = null;
	int nearest = Integer.MAX_VALUE;

	for(int m = 0; m < getMoleculeCount(); m++){
	    Molecule molecule = getMolecule(m);
	    int atomCount = molecule.getAtomCount();
	    int style = molecule.getDisplayStyle();

	    if((style & Molecule.Normal) == Molecule.Normal &&
	       molecule.getDisplayed()){

		for(int a = 0; a < atomCount; a++){
		    Atom atom = molecule.getAtom(a);
		    int az = atom.zs;
		    if(atom.isDisplayed() &&
		       az >= renderer.backClip && az <= renderer.frontClip){
			int ax = atom.xs >> Renderer.FixedBits;
			int ay = atom.ys >> Renderer.FixedBits;
			int dSquare = (ax - x)*(ax - x) + (ay - y)*(ay - y);
			if(dSquare < nearest && dSquare < 64){
			    nearest = dSquare;
			    nearestAtom = atom;
			}
		    }
		}
	    }else if((style & Molecule.Trace) == Molecule.Trace &&
		     molecule.getDisplayed()){
		for(int a = 0; a < atomCount; a++){
		    Atom atom = molecule.getAtom(a);
		    int az = atom.zs;
		    if(atom.isDisplayed() &&
		       atom.getAtomLabel().equals("CA") &&
		       az >= renderer.backClip && az <= renderer.frontClip){
			int ax = atom.xs >> Renderer.FixedBits;
			int ay = atom.ys >> Renderer.FixedBits;
			int dSquare = (ax - x)*(ax - x) + (ay - y)*(ay - y);
			if(dSquare < nearest && dSquare < 64){
			    nearest = dSquare;
			    nearestAtom = atom;
			}
		    }
		}

	    }
	}

	return nearestAtom;
    }

    /** Move an atom in the plane of the screen. */
    public void moveAtom(Atom atom, int dx, int dy){
	// the on screen direction
	Point3d direction = new Point3d(dx, -dy, 0);

	Matrix matrix = renderer.rotationMatrix;

	renderer.rotationMatrix.transformByInverse(direction);

	direction.divide(renderer.getOverallScale());

	atom.add(direction);
    }

    /** Translate the center of the view. */
    public void translateCenter(int dx, int dy){
	Point3d direction = new Point3d(-dx, dy, 0);

	Matrix matrix = renderer.rotationMatrix;

	renderer.rotationMatrix.transformByInverse(direction);

	direction.divide(renderer.getOverallScale());

	Point3d center = renderer.getCenter();

	center.add(direction);

	renderer.setCenter(center);
    }

    /* Various methods for applying color schemes to molecules. */

    /** List of color names for the different chains. */
    private static int chainColors[] = {
	Color32.green,
	Color32.yellow,
	Color32.orange,
	Color32.blue,
	Color32.red,
    };

    /** Color by chain. */
    public void colorByChain(){
	boolean all = (getSelectedAtomCount() == 0);

	int chainNumber = 0;
	for(int m = 0; m < getMoleculeCount(); m++){
	    Molecule molecule = getMolecule(m);
	    int chainCount = molecule.getChainCount();
	    for(int c = 0; c < chainCount; c++){
		Chain chain = molecule.getChain(c);
		int residueCount = chain.getResidueCount();
		int color = chainColors[chainNumber % chainColors.length];

		for(int r = 0; r < residueCount; r++){
		    Residue residue = chain.getResidue(r);
		    int atomCount = residue.getAtomCount();

		    for(int a = 0; a < atomCount; a++){
			Atom atom = residue.getAtom(a);
			if(all || atom.isSelected()){
			    atom.setColor(color);
			}
		    }
		}

		chainNumber++;
	    }
	}
    }

    /** Color by atom type. */
    public void colorByAtom(){
	boolean all = (getSelectedAtomCount() == 0);

	for(int m = 0; m < getMoleculeCount(); m++){
	    Molecule molecule = getMolecule(m);
	    int atomCount = molecule.getAtomCount();
	    boolean symmetryMolecule = molecule.isSymmetryMolecule();

	    for(int a = 0; a < atomCount; a++){
		Atom atom = molecule.getAtom(a);

		if(all || atom.isSelected()){
		    atom.resetColor();
		    int color = atom.getColor();

		    if(symmetryMolecule &&
		       atom.getElement() == PeriodicTable.CARBON){
			color = Color32.grey;
		    }

		    atom.setColor(color);
		}
	    }
	}
    }

    /** Color by bFactor. */
    public void colorBySpecifiedColor(int color){
	boolean all = (getSelectedAtomCount() == 0);

	AtomIterator atomIterator = getAtomIterator();

	while(atomIterator.hasMoreElements()){
	    Atom atom = atomIterator.getNextAtom();

	    if(all || atom.isSelected()){
		atom.setColor(color);
	    }
	}
    }

    /**
     * Color by bFactor.
     * This method uses a fixed range to set the b-factor
     * colors as the absolute value has meaning.
     */
    public void colorByBFactor(){
	boolean all = (getSelectedAtomCount() == 0);

	AtomIterator atomIterator = getAtomIterator();

	while(atomIterator.hasMoreElements()){
	    Atom atom = atomIterator.getNextAtom();

	    if(all || atom.isSelected()){
		double b = atom.getBFactor();
		int color = 0;

		if(b <= 10.0){
		    color = Color32.rwb7;
		}else if(b > 10.0 && b <= 15.0){
		    color = Color32.rwb6;
		}else if(b > 15.0 && b <= 20.0){
		    color = Color32.rwb5;
		}else if(b > 20.0 && b <= 25.0){
		    color = Color32.rwb4;
		}else if(b > 25.0 && b <= 30.0){
		    color = Color32.rwb3;
		}else if(b > 30.0 && b <= 35.0){
		    color = Color32.rwb2;
		}else if(b > 35.0 && b <= 40.0){
		    color = Color32.rwb1;
		}else{
		    color = Color32.rwb0;
		}

		atom.setColor(color);
	    }
	}
    }

    /**
     * The colors that we will use for b-factor ranges.
     */
    private static int predefinedColors[]  = null;
    private static final int ColorRampSize = 512;

    public void ensureColorRampDefined(){
	if(predefinedColors == null){
	    predefinedColors = new int[ColorRampSize];
	    int ColorRampSize2 = ColorRampSize/2;

	    for(int i = 0; i < ColorRampSize2; i++){
		int r = i;
		int g = i;
		int b = 255;
		predefinedColors[i] = Color32.pack(r, g, b);
	    }
	    for(int i = 0; i < ColorRampSize2; i++){
		int r = 255;
		int g = 255 - i;
		int b = 255 - i;
		predefinedColors[ColorRampSize2 + i] =
		    Color32.pack(r, g, b);
	    }
	}
    }

    /*
      private static int predefinedColors[] = {
      Color32.rwb7, Color32.rwb6, Color32.rwb5, Color32.rwb4,
      Color32.rwb3, Color32.rwb2, Color32.rwb1, Color32.rwb0,
      };
    */

    /** 
     * Color by b-factor but bin the colors according to range.
     */
    public void colorByPropertyRange(int property){
	double min = 1.e10, max = -1.e10;
	boolean all = (getSelectedAtomCount() == 0);

	ensureColorRampDefined();

	AtomIterator atomIterator = getAtomIterator();
	double rms = 0.0;
	int selectedCount = 0;
	while(atomIterator.hasMoreElements()){
	    Atom atom = atomIterator.getNextAtom();

	    if(all || atom.isSelected()){
		selectedCount++;
		double b = atom.getAttribute(property);
		rms += b * b;
		if(b < min){
		    min = b;
		}
		if(b > max){
		    max = b;
		}
	    }
	}

	if(selectedCount > 0){
	    rms /= (double)selectedCount;
	    rms = Math.sqrt(rms);
	}


	if(property == Atom.B){
	    max = 2. * rms;
	}
	//System.out.println("rms " + rms);
	//System.out.println("min " + min);
	//System.out.println("max " + max);

	atomIterator = getAtomIterator();

	int colorCount = predefinedColors.length;

	while(atomIterator.hasMoreElements()){
	    Atom atom = atomIterator.getNextAtom();

	    if(all || atom.isSelected()){
		double b = atom.getAttribute(property);
		int color = Color32.rwb7;

		if(property == Atom.B && Math.abs(b) > 1.e-3){
		    color = (int)(colorCount * (b - min) / (max - min));

		    if(color < 0){
			color = 0;
		    }else if(color >= colorCount){
			color = colorCount - 1;
		    }

		    color = predefinedColors[color];
		}

		atom.setColor(color);
	    }
	}
    }

    /** Working space for the rainbow coloring. */
    private double hsvtmp[] = new double[3];

    /** Max hue to stop at blue. */
    private static final double maxHue = 240.0;

    /**
     * Color a set of atoms with rainbow hue.
     * Rainbow goes from blue to red.
     */
    public void colorByRainbow(DynamicArray selectedAtoms){
	int atomCount = selectedAtoms.size();
	double step = maxHue/(atomCount - 1);
	double hue = maxHue;

	hsvtmp[1] = 1.0;
	hsvtmp[2] = 1.0;

	for(int i = 0; i < atomCount; i++){
	    Atom a = (Atom)selectedAtoms.get(i);
	    hsvtmp[0] = hue;
	    //System.out.println("hue "+ hue);
	    int c = Color32.hsv2packed(hsvtmp);
	    a.setColor(c);
	    hue -= step;
	}
    }

    /** The slide number for running slide shows. */
    private int slideNumber = -1;

    /** The slideshow we are showing. */
    private String slideShow = null;

    /** Handle a slide command. */
    public void handleSlideCommand(Arguments args){
	String slideShowString = args.getString("-slideshow", null);
	int forward = args.getInteger("-forward", Integer.MIN_VALUE);
	int backward = args.getInteger("-backward", Integer.MIN_VALUE);
	int show     = args.getInteger("-show", Integer.MIN_VALUE);

	if(slideShowString != null){
	    slideShow = slideShowString;
	    slideNumber = 0;
	    Log.info("slideshow is " + slideShow);
	}

	if(forward != Integer.MIN_VALUE){
	    slideNumber += forward;
	}

	if(backward != Integer.MIN_VALUE){
	    slideNumber -= backward;
	}

	if(show != Integer.MIN_VALUE){
	    slideNumber = show;
	}

	// only go back to -1
	if(slideNumber < 0){
	    slideNumber = 0;
	}

	Log.info("showing slide %d", slideNumber);

	if(slideShow != null){
	    String script = FILE.sprint(slideShow, slideNumber);
	    Log.info("about to play slide " + script);
	    executeScript(script);
	}else{
	    Log.error("no slideshow defined");
	}
    }

    transient parser astexViewerParser = new parser();
    transient Yylex lexer = new Yylex((java.io.BufferedReader)null);

    /** The buffer of commands we executed. */
    transient StringBuffer commandLog = new StringBuffer(65536);

    /**
     * Main interface for command processing.
     * Not used... the below stack based method is used now.
     */
    public synchronized void execute2(String command){
	parser.setMoleculeRenderer(this);

	if(debug){
	    System.out.println("MoleculeRenderer.execute() command:");
	    System.out.println("|" + command + "|");
	    System.out.println("fancy");
	}

	StringReader sr = new StringReader(command);
	try {

	    astexViewerParser.setScanner(lexer);
	    lexer.setInput(sr);
	    astexViewerParser.parse();
	    commandLog.append(command);
	    //System.gc();
	    //System.runFinalization();
	    //new parser(new Yylex(new StringReader(command))).parse();
	}catch(Exception e){
	    System.out.println("error processing command: ");
	    System.out.println(command);

	    e.printStackTrace();
	}finally{
	    sr.close();
	}
    }

    public synchronized void execute22(String command){
	executeInternal(command);
    }

    private CommandThread commandThread = null;

    public synchronized void execute(String command){
	//CommandThread.execute(this, "main", command);
	executeInternal(command);
    }

    private transient parser parserStack[] = new parser[10];
    private transient Yylex  lexerStack[]  = new Yylex[10];

    private int parserDepth = -1;

    private synchronized boolean parse(java.io.Reader reader){
	boolean errorCondition = false;

	if(parserStack == null) parserStack = new parser[10];
	if(lexerStack == null) lexerStack = new Yylex[10];

	parserDepth++;

	try {
	    // only allocate the parser and lexer for each
	    // depth when needed, the lexer in particular
	    // uses quite a lot of memory...
	    if(parserStack[parserDepth] == null){
		parserStack[parserDepth] = new parser();
		lexerStack[parserDepth] =
		    new Yylex((java.io.BufferedReader)null);
		parserStack[parserDepth].setScanner(lexerStack[parserDepth]);
	    }

	    parserStack[parserDepth].setMoleculeRenderer(this);
	    lexerStack[parserDepth].setInput(reader);
	    parserStack[parserDepth].parse();
	}catch(Exception e){
	    //Log.error("error parsing: " + command);
	    e.printStackTrace();
	    errorCondition = true;
	}

	parserDepth--;

	if(errorCondition){
	    return false;
	}else{
	    return true;
	}
    }

    /*
      private synchronized boolean parse2(java.io.Reader reader){
      Object parseInfo[] = null;

      synchronized(parseStack){
      if(false && parseStack.empty() == false){
      parseInfo = (Object[])parseStack.pop();
      //print.f("#### resusing");
      }else{
      parseInfo = new Object[2];
      parseInfo[0] = new parser();
      parseInfo[1] = new Yylex((java.io.BufferedReader)null);
      //print.f("#### creating");
      }
      }

      //print.f("parseInfo "+ parseInfo);

      parser p = (parser)parseInfo[0];
      p.setMoleculeRenderer(this);

      Yylex y  = (Yylex)parseInfo[1];

      p.setScanner(y);
      y.setInput(reader);

      CommandThread t = new CommandThread(p, parseStack, parseInfo);

      t.start();

      return true;
      }    
    */

    public synchronized void executeInternal(String command){
	//Log.info("command: " + command);

	StringReader sr = new StringReader(command);

	if(parse(sr) == false){
	    System.err.println("Syntax error in command:");
	    System.err.println(command);
	}else{
	    if(commandLog == null){
		commandLog = new StringBuffer();
	    }
	    commandLog.append(command);
	}

	sr.close();
    }

    /** The last script we executed. */
    public String lastScriptFile = null;

    public synchronized void reExecute(){
	if(lastScriptFile != null){
	    System.out.println("reExecute " + lastScriptFile);
	    executeScript(lastScriptFile);
	}
    }

    /** Execute a script of commands. */
    public synchronized void executeScript(String filename){
	try {
	    boolean previous = FILE.getTryFiles();

	    FILE.setTryFiles(true);
	    FILE file = FILE.open(filename);

	    if(file != null){
		InputStream is = file.getInputStream();

		parse(new InputStreamReader(is));

		file.close();

		lastScriptFile = filename;
	    }else{
		System.out.println("couldn't open script " + filename);

		lastScriptFile = null;
	    }

	    file = FILE.open(filename);
	    if(file != null){
		int c = 0;

		while(file.nextLine()){
		    String line = file.getCurrentLineAsString();
		    commandLog.append(line);
		}

		file.close();
	    }

	    FILE.setTryFiles(previous);
	}catch(Exception e){
	    System.out.println("error processing command: ");
	    e.printStackTrace();
	}
    }
}
