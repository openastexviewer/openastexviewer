//#define NOBWT

package astex;

/* Copyright Astex Technology Ltd. 1999 */

/*
 * 20-05-00 mjh
 *	fix bug in command line file reading that prevented maps
 *	being read unless they were the first file
 * 25-11-99 mjh
 *	change centering method to shift-click as there are problems
 *	getting double click events on linux
 * 17-11-99 mjh
 *	created
 */
import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
// only import specific classes to stop
// clashses with my Map class
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Locale;

import nanoxml.*;

import astex.parser.*;
#ifdef XRAYTOOLS
import astex.xmt.Manipulator;
#endif
import astex.splitter.*;
import astex.generic.*;

#ifndef VIEWER_BASE
#define VIEWER_BASE Canvas
#endif

#ifndef VIEWER_CLASS
#define VIEWER_CLASS MoleculeViewer
#endif

/**
 * A component that can draw a molecule.
 */
public class VIEWER_CLASS extends VIEWER_BASE implements MouseListener,
						      MouseMotionListener,
						      KeyListener,
						      ActionListener,
						      ItemListener,
						      AdjustmentListener,
						      MoleculeRendererListener,
						      WindowListener  {
    /** The molecule renderer that this viewer will use. */
    public MoleculeRenderer moleculeRenderer = null;

    /** Should we show the frame rate. */
    private boolean showFrameRate = false;

    /** Should we show the version. */
    private boolean showVersion = false;

    /** Do we allow atoms to be moved. */
    private boolean moveAtomsAllowed = false;

    /** Do we exclude specified atoms from the selection. */
    private boolean excludeFromSelection = false;

    /** Are we initialised? */
    public boolean ready = true;

    /** Are we initialised? */
    public Image splashImage = null;

    /** Are we announcing the origin of AstexViewer. */
    public boolean announce = false;

    /** Definition of key bindings. */
    public Hashtable keyDefinitions = new Hashtable();

    /** The animation thread. */
    private Animate animationThread = null;

    /** Are we currently animating. */
    private boolean animationThreadActive = false;

    /** The animation stages. */
    private Vector stages = new Vector();

    /** Is this MoleculeViewer running in an application. */
    private boolean application = false;

    /** Are we running in application. */
    public boolean isApplication(){
        return application;
    }

    /** Set whether we are running in application. */
    public void setApplication(boolean b){
        application = b;
    }

    /** Get the animation thread. */
    public void handleAnimation(Arguments args){

	String state        = args.getString("-state", "stop");
	boolean interactive = args.getBoolean("-interactive", false);

	// if state is start we just play the animation objects.
	if(state.equals("start")){
	    animationThread = new Animate();
	    animationThread.setMoleculeViewer(this);
	    animationThread.setStages(stages);
	    animationThread.setInteractive(interactive);
	    animationThread.start();

	    return;
	}else if(state.equals("delete")){
	    stages.removeAllElements();
	    return;
	}

	// otherwise we have to set up a new animationobject.
	AnimationObject stage = new AnimationObject();
	stage.setMoleculeViewer(this);

	String mode    = args.getString("-mode", "rock");
	String command = args.getString("-command", null);
	int d          = args.getInteger("-delay", 50);
	int steps      = args.getInteger("-steps", 10);
	boolean sawInteractive = false;

	if(ViewCommand.stepMultiple != 1){
	    System.out.println("step multiple is " + ViewCommand.stepMultiple);
	    steps *= ViewCommand.stepMultiple;
	    System.out.println("step count is now " + steps + " [was " +
			       (steps/ViewCommand.stepMultiple) +"]");
	}

	stage.setSteps(steps);

	stage.setSleepDuration(d);
	stage.setCommand(command);

	if(mode.equals("rock")){
	    double a = args.getDouble("-angle", 7.5);
	    //double r = args.getDouble("-rate", 7.5);

	    stage.setRockAngle(a);
	    //stage.setRockRate(r);

	    stage.setMode(AnimationObject.Rock);
	}else if(mode.equals("roll")){
	    double a = args.getDouble("-angle", 360.0);
	    stage.setRockAngle(a);
	    
	    stage.setMode(AnimationObject.Roll);
	}else if(mode.equals("command")){

	    stage.setMode(AnimationObject.Command);

	}else if(mode.equals("recenter")){
	    String matrix = args.getString("-matrix", null);

	    Point3d center = null;
	    double r       = 10.0;
	    double cf      =  r;
	    double cb      = -r;

	    if(args.defined("-selection")){
		DynamicArray selection = (DynamicArray)args.get("-selection");
		center                 = moleculeRenderer.getCenter(selection);
		r                      = moleculeRenderer.getRadius(selection);
	    }else if(args.defined("-center")){
		String centerString = args.getString("-center", null);

		if(centerString == null){
		    Log.error("center is null: must specify \"x,y,z\"");
		    return;
		}

		String tokens[] = FILE.split(centerString, ",");

		if(tokens.length != 3){
		    Log.error("-center must specify \"x,y,z\"");
		    return;
		}

		center   = new Point3d();
		center.x = FILE.readDouble(tokens[0]);
		center.y = FILE.readDouble(tokens[1]);
		center.z = FILE.readDouble(tokens[2]);
	    }else{
		Log.error("recenter must specify -selection or -center");
		return;
	    }
		

	    if(args.defined("-radius")){
		r = args.getDouble("-radius", r);
	    }

	    if(args.defined("-clipfront")){
		cf = args.getDouble("-clipfront", r);
	    }

	    if(args.defined("-clipback")){
		cb = args.getDouble("-clipback", r);
	    }

	    if(center == null && matrix == null){
		Log.warn("no atoms in center selection - skipping");
		return;
	    }

	    if(matrix != null){
		String elements[] = FILE.split(matrix, " ,");
	    
		if(elements.length != 16){
		    Log.error("matrix needs 16 components not " +
			      elements.length);
		    return;
		}

		Matrix rm = new Matrix();

		rm.x00 = FILE.readDouble(elements[0]);
		rm.x01 = FILE.readDouble(elements[1]);
		rm.x02 = FILE.readDouble(elements[2]);
		rm.x03 = FILE.readDouble(elements[3]);
		rm.x10 = FILE.readDouble(elements[4]);
		rm.x11 = FILE.readDouble(elements[5]);
		rm.x12 = FILE.readDouble(elements[6]);
		rm.x13 = FILE.readDouble(elements[7]);
		rm.x20 = FILE.readDouble(elements[8]);
		rm.x21 = FILE.readDouble(elements[9]);
		rm.x22 = FILE.readDouble(elements[10]);
		rm.x23 = FILE.readDouble(elements[11]);
		rm.x30 = FILE.readDouble(elements[12]);
		rm.x31 = FILE.readDouble(elements[13]);
		rm.x32 = FILE.readDouble(elements[14]);
		rm.x33 = FILE.readDouble(elements[15]);

		stage.setFinishMatrix(rm);
	    }

	    stage.setFinishCenter(center.x, center.y, center.z, r, cf, cb);
	    stage.setMode(AnimationObject.Recenter);
	}else{
	    System.out.println("handleAnimation: unhandled animation mode " + mode);
	}

	stages.addElement(stage);
    }

    /** Try and remove a thread after it has finished running. */
    public synchronized void removeAnimationThread(Thread t){

	if(animationThread != null){
	    //animationThread.stop();
	    animationThread = null;
	    animationThreadActive = false;
	}

	dirtyRepaint();
    }

    /** Are we animating? */
    public boolean animating(){
	if(animationThread != null &&
	   animationThread.isAlive() &&
	   animationThread.getInteractive() == false){
	    return true;
	}else{
	    return false;
	}
    }

    public boolean interactiveAnimation(){
	if(animationThread != null &&
	   animationThread.isAlive()){
            return animationThread.getInteractive();
        }

        return true;
    }

    /** Handle a print command in the scripting language. */
    public void handlePrint(String output){
	StringBuffer sb = new StringBuffer();
	int len = output.length();

	// process any escape characters to introduce
	// new lines etc.
	for(int i = 0; i < len; i++){
	    char c = output.charAt(i);

	    if(c == '\\'){
		if(i < len - 1){
		    i++;
		    c = output.charAt(i);
		    if(c == 'n'){
			sb.append('\n');
		    }else if(c == 'r'){
			sb.append('\r');
		    }else if(c == '\\'){
			sb.append('\\');
		    }else{
			sb.append(c);
		    }
		}else{
		    sb.append('\\');
		}
	    }else{
		sb.append(c);
	    }
	}

	System.out.println(sb.toString());
    }

    /** Handle (de)activation of animation thread. */
    public void suspendAnimationThread(boolean run){
	if(animationThreadActive){
	    if(run){
		animationThread.resume();
	    }else{
		animationThread.suspend();
	    }
	}
    }

    /** Execute a command in the moleculeRenderer. */
    public void execute(String s){
	moleculeRenderer.execute(s);
    }

    /** Default constructor. */
    public MoleculeViewer(){
	System.out.println("AstexViewer " + Version.getVersion());
	System.out.println("Copyright (C) 1999-2005 Astex Therapeutics Ltd.");
	System.out.println("http://www.astex-therapeutics.com/AstexViewer");

        if(false){
            System.out.println("Locale forced to en_us");

            Locale locale = new Locale("en", "us");

            Locale.setDefault(locale);
        }

	moleculeRenderer = new MoleculeRenderer();
	moleculeRenderer.moleculeViewer = this;
	moleculeRenderer.addMoleculeRendererListener(this);
	moleculeRenderer.renderer.setColor(0x00ff00);
	addMouseListener(this);
	addMouseMotionListener(this);
	addKeyListener(this);
	readKeyDefinitions();

	//loadMolecule("r:\\621p.pdb");
    }

    public void addNotify(){
        super.addNotify();
        //print.f("### hello from addNotify");
        repaint();
    }

    /* Read the defintions of the commands from xml fie. */
    public void readKeyDefinitions(){
	FILE file = FILE.open("keymap.properties");
	Reader reader = null;
        XMLElement xml = new XMLElement();

        try {
	    InputStreamReader isr =
		new InputStreamReader(file.getInputStream());
            reader = (Reader)new BufferedReader(isr);
        } catch(Exception e){
            System.out.println("Couldn't open xml file " + e);
        }

        try {
            xml.parseFromReader(reader);
        } catch(IOException e){
            System.out.println("Couldn't parse xml file " + e);
        }

        System.out.println("about to close property file");

        file.close();

	Enumeration objects = xml.enumerateChildren();

	if(xml.getName().equals("keymap") == false){
	    System.out.println("keymap.properties has invalid format");
	    return;
	}

	while(objects.hasMoreElements()){
	    XMLElement child = (XMLElement)objects.nextElement();
	    if(child.getName().equals("key")){
		String key = null;
		String modifiers = child.getStringAttribute("MODIFIERS");
		String code = child.getStringAttribute("CODE");

		/*
		if(code.length() > 1){
		    System.out.println("readKeyDefinitions: key code can only be single character "+ code);
		}

		char c = code.charAt(0);

		if(Character.isLowerCase(c)){
		    c = Character.toUpperCase(c);
		}

		if(modifiers == null){
		    key = "" + c;
		}else{
		    key = modifiers + "+" + c;
		}
		*/
		//System.out.println("key is |" + key + "|");

		String content = child.getContent();

		keyDefinitions.put(code, content);
	    }
	}
    }

    public void dispose(){
	//System.out.println("##### MoleculeViewer.dispose()");
    }

    /** Get the molecule renderer that we contain. */
    public MoleculeRenderer getMoleculeRenderer(){
	return moleculeRenderer;
    }

    /** Should the renderer use arraycopy for clearing its buffers. */
    public void setArrayCopy(boolean arraycopy){
	// fix
	//moleculeRenderer.setArrayCopy(arraycopy);
    }

    /** Set the molecule that the renderer will use. */
    public void addMolecule(Molecule molecule){
	moleculeRenderer.addMolecule(molecule);
    }

    /** Load a molecule given the name of its file. */
    public void loadMolecule(String filename){
	filename = FILE.getRelativePath(filename);

	String command =
	    "molecule load '" + filename + "' '" + filename + "';";

        if(filename.toLowerCase().indexOf(".sdf") != -1 ||
           filename.toLowerCase().indexOf(".mol") != -1){
            command += "cylinder_radius 0.09 molexact '" + filename + "';";
            command += "display cylinders on molexact '" + filename + "';";
        }

	moleculeRenderer.execute(command);

	//Molecule molecule = MoleculeIO.read(filename);

	//if(molecule != null){
	//    moleculeRenderer.addMolecule(molecule);
	//}
    }

    public void executeScript(String filename){
	filename = FILE.getRelativePath(filename);

	System.out.println("about to execute script " + filename);
	moleculeRenderer.executeScript(filename);
    }

    /** Load a map given the name of its file. */
    public void loadMap(String filename){
	filename = FILE.getRelativePath(filename);

	String command =
	    "map load '" + filename + "' '" + filename + "';";

	moleculeRenderer.execute(command);

	//Map map = Map.create();

	//if(map != null){
	//    map.setFile(filename);
	//    map.setName(filename);

	//    moleculeRenderer.addMap(map);
	//}
    }

    /** Handle a resize in a different way. */
    public void setBounds(Rectangle r){
	setBounds(r.x, r.y, r.width, r.height);
    }

    /** Handle a resize. */
    public void setBounds(int x, int y, int width, int height){
	super.setBounds(x, y, width, height);

	//finalize();
	awtImage = null;

	memoryImageSource = null;
	moleculeRenderer.renderer.setSize(width, height);
    }

    /** Set the center point of the underlying renderer. */
    public void setCenter(Point3d center){
	moleculeRenderer.setCenter(center);
    }

    /** Set the radius of the underlying renderer. */
    public void setRadius(double radius){
	moleculeRenderer.setRadius(radius);
    }

    /** Set whether we are displaying symmetry. */
    public void setSymmetry(boolean symmetry){
    }

    /** Add a map to the renderer. */
    public void addMap(Map map){
	moleculeRenderer.addMap(map);
    }

    /** Remove a map from the renderer. */
    public void removeMap(Map map){
	moleculeRenderer.removeMap(map);
    }

    /** The preferred size for this MoleculeViewer. */
    private Dimension preferredSize = null;

    /** Set the preferred size. */
    public void setPreferredSize(int w, int h){
        preferredSize = new Dimension(w, h);
    }

    /** Return the preferred size. */
    public Dimension getPreferredSize(){
        if(preferredSize == null){
            return new Dimension(800, 600);
        }else{
            return preferredSize;
        }
    }

    /** Overridden update method. */
    public void update(Graphics g){
	paint(g);
    }

    /** A format for presenting the frame rate. */
    private Format f1 = new Format("%4.1f");

    boolean firstTime = false;

    /** Override the paint method. */
    public void paint(Graphics g){

        if(firstTime){
            //GoogleFont.setComponent(this);
            
            int size[] = new int[2];
            int pix[] = GoogleFont.makeFontImage("hello", 0x000000, 0xffffff, size);
            
            try {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("tmp.bmp"));
                
                astex.ImageIO.writeBMP(bos, pix, size[0], size[1]);
            }catch(Exception e){
                e.printStackTrace();
            }
            firstTime = false;
        }


	if(!ready){
	    drawLoadingScreen(g);
	}else{

	    drawImage(this, g);

	    if(showFrameRate){
		frameCount++;

		long time = System.currentTimeMillis() - mousePressedTime;
			
		double framesPerSecond = 0.0;

		if(time != 0){
		    framesPerSecond =
			1000.0*((double)frameCount/(double)time);
		}

		g.setColor(Color.white);
		g.drawString(f1.format(framesPerSecond), 10, 15);
	    }
	}
    }

    /** Function that paints the image when we load. */
    public void drawLoadingScreen(Graphics g){
	g.setColor(Color.black);
	g.fillRect(0, 0, bounds().width, bounds().height);

	if(announce){
	    g.setColor(Color.gray);
	    
	    String version =
		"AstexViewer " + Version.getVersion() +
		" Copyright (C) 1999-2005 Astex Therapeutics Ltd.";
	    
	    g.drawString(version, 10, 14);
	    g.drawString("All rights reserved", 10, 28);
	    g.drawString("http://www.astex-technology.com/AstexViewer",
			 10, 42);
	    g.drawString("Loading...",
			 10, 60);
	}
    }

    /** Force the awt image to get replaced. */
    public void resetAwtImage(){
	awtImage = null;
	moleculeRenderer.dirty = true;
    }

    /** The image that we will use. */
    private transient Image awtImage = null;
    
    /** The pixel buffer that will produce the image. */
    transient MemoryImageSource memoryImageSource = null;

    /** Paint the image. */
    public synchronized void drawImage(Component c, Graphics g){

	//System.out.println("Viewer paint()");

	if(animating()){
	    return;
	}

	if(!ready){
	    //System.out.println("Viewer paint() uninitialised");
	    g.setColor(Color.white);
	    g.fillRect(0, 0, bounds().width, bounds().height);

	    if(splashImage != null && splashImage.getWidth(this) != -1){
		int imageX = (bounds().width - splashImage.getWidth(this))/2;
		int imageY = (bounds().height - splashImage.getHeight(this))/2;

		g.drawImage(splashImage, imageX, imageY, null);
	    }

	    return;
	}

	if(awtImage == null){
	    moleculeRenderer.renderer.setSize(bounds().width, bounds().height);
	    moleculeRenderer.dirty = true;

	    // Add the DirectColorModel statement to remove the
	    // need for setting to the transparency bits in the
	    // image to 255. Not clear if this gains you any
	    // performance, as presumably the DirectColorModel
	    // has to do more work? Test later.
	    memoryImageSource =
		new MemoryImageSource(bounds().width, bounds().height,
				      new DirectColorModel(32, 0xff0000,
				      		   0xff00, 0xff),
				      moleculeRenderer.renderer.pbuffer,
				      0, bounds().width);
	    memoryImageSource.setAnimated(true);
	    //memoryImageSource.setFullBufferUpdates(true);
	    awtImage = createImage(memoryImageSource);
	}

	moleculeRenderer.paint();

	memoryImageSource.newPixels();

	//awtImage.flush();

	g.drawImage(awtImage, 0, 0, null);

	//WriteGifEncoder wge = new WriteGifEncoder(awtImage);
	//wge.writeFile("bob.gif");

	//try {
	//    BufferedImage image =
	//	new BufferedImage(bounds().width, bounds().height, BufferedImage.TYPE_INT_RGB);
	//    image.setRGB(0, 0, bounds().width, bounds().height, renderer.pbuffer, 0, bounds().width);
	//    image.flush();
		
	//    OutputStream os = new FileOutputStream("image.jpg");
	//    JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(os);
	//    JPEGEncodeParam param =
	//	encoder.getDefaultJPEGEncodeParam(image);
	//    param.setQuality(1.0f, false);
	//    encoder.setJPEGEncodeParam(param);
	//    encoder.encode(image);
	//    os.close();
	//}catch(Exception e){
	//    System.out.println("exception " + e);
	//}
    }

    public void finalize(){
	//System.out.println("MoleculeViewer.finalize()");

	if(memoryImageSource != null){
	    //memoryImageSource.dispose();
	    memoryImageSource = null;
	}

	if(awtImage != null){
	    //awtImage.dispose();
	    awtImage.flush();
	    awtImage = null;
	}

#ifndef NOBWT
	if(ui != null){
	    ui.userInterfaceFrame.dispose();
	    ui.userInterfaceFrame = null;
	    ui = null;
	}
#endif

	/*
	if(moleculeRenderer.renderer != null){
	    moleculeRenderer.renderer.pbuffer = null;
	    moleculeRenderer.renderer.zbuffer = null;
	    moleculeRenderer.renderer = null;
	}
	*/

	if(moleculeRenderer != null){
	    moleculeRenderer = null;
	}

	System.gc();
	System.runFinalization();
    }

    public void setActiveLight(int al, Light l){
#ifndef NOBWT
    if(ui != null){
        ui.setActiveLight(al, l);
    }
#endif
    }

    TextField centerField = null;

    /** Main entry point. */
    public static void main(String args[]){

	Frame frame = new Frame();
	frame.setLayout(new BorderLayout());

	MoleculeViewer moleculeViewer = new MoleculeViewer();

        moleculeViewer.setApplication(true);

	frame.addWindowListener(moleculeViewer);

	if(false){
	    moleculeViewer.setUsePopupMenu(true);
	    moleculeViewer.createMenuBar();
	}else{
	    frame.setMenuBar(moleculeViewer.createMenuBar());
	}

	// if the system property arraycopy is set to true
	// then the renderer should use arraycopy to clear
	// its buffers rather than brute force
	String arraycopy = System.getProperty("arraycopy");
	if(arraycopy != null && arraycopy.equals("true")){
	    moleculeViewer.setArrayCopy(true);
	}

        boolean loadMolecules = true;
        MoleculeRenderer moleculeRenderer =
            moleculeViewer.getMoleculeRenderer();
        
        for(int i = 0; i < args.length; i++){
            //System.out.println("args " + args[i]);
            if(args[i].endsWith(".xml")){
#ifdef XSTREAM                
                MoleculeRenderer mr =
                    (MoleculeRenderer)astex.xstream.restore(args[i]);
                
                moleculeViewer.moleculeRenderer = mr;
                mr.moleculeViewer = moleculeViewer;
#endif
            }else if(args[i].endsWith(".script")){
                moleculeViewer.executeScript(args[i]);
            }else if(args[i].endsWith(".tmesh")){
                Tmesh tm = Tmesh.read(args[i]);
                tm.setName(args[i]);
                moleculeRenderer.renderer.addGraphicalObject(tm);
            }else if("-c".equals(args[i])){
                if(i + 1 < args.length){
                    i++;
                    String centerString = args[i];
                    DynamicArray centerSelection =
                        moleculeRenderer.getAtomsInSelection(centerString);
		    
                    moleculeRenderer.setCenter(centerSelection);
                }
            }else if("-maps".equals(args[i]) || 
                     "-map".equals(args[i]) || 
                     "-m".equals(args[i])){
                loadMolecules = false;
            }else{
                if(loadMolecules){
                    moleculeViewer.loadMolecule(args[i]);
                }else{
                    moleculeViewer.loadMap(args[i]);
                }
            }
	}

        if(true){
            Panel mvp = new Panel();
            mvp.setLayout(new BorderLayout());
            mvp.add(moleculeViewer, BorderLayout.CENTER);

            frame.add(mvp, BorderLayout.CENTER);
            Panel p = new Panel();

            p.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 2));
            p.add(new Label("Centre"));
            moleculeViewer.centerField = new TextField(10);
            moleculeViewer.centerField.addActionListener(moleculeViewer);
            p.add(moleculeViewer.centerField);

            moleculeViewer.moleculeRenderer.renderer.setColor(0x00ff00);
            
            frame.add(p, BorderLayout.SOUTH);
        }else{
            frame.setLayout(new SplitterLayout(SplitterLayout.HORIZONTAL));

            frame.add("3", moleculeViewer);
            SplitterBar sb = new SplitterBar();
            
            frame.add(sb);

            frame.add("1", new Panel());
        }
	frame.pack();
	frame.show();
    }

    /** The last mouse event. */
    private MouseEvent lastMouseEvent = null;

    /** The mouse pressed event. */
    private MouseEvent mousePressedEvent = null;

    /** The number of frames since we last pressed the mouse button. */
    private int frameCount = 0;

    /** The time the mouse button was pressed. */
    private long mousePressedTime = 0;

    /** The atom that we picked. */
    private Atom pickedAtom = null;

    /** Has the mouse been dragged since the button was pressed. */
    boolean dragged = false;

    /** Has the center been moved by dragging the mouse. */
    boolean centerMoved = false;

    /* Implementation of MouseListener. */

    /** Handle a mousePressed event. */
    public void mousePressed(MouseEvent e){
	requestFocus();

	suspendAnimationThread(false);

	mousePressedEvent = e;
	lastMouseEvent = e;
			
	dragged = false;

	if(e.isPopupTrigger()){
	    showPopupMenu(e);
	}else if(!e.isControlDown()){
			
	    if(showFrameRate){
		frameCount = 0;
		mousePressedTime = System.currentTimeMillis();
	    }
			
	    pickedAtom =
		moleculeRenderer.getNearestAtom(e.getX(), e.getY());
	    
	    if(pickedAtom != null){
	       //System.out.println("mouse pressed over atom");
	    }
	}

#ifdef XRAYTOOLS
	// feed it off to the manipulator if there is one.
	if(e.isAltDown()){
	    if(manipulator != null && pickedAtom != null){
		manipulator.pick(e, this, pickedAtom);
	    }
	    return;
	}
#endif

	dirtyRepaint();
    }

    /* Implementation of WindowListener. */
    public void windowClosing(WindowEvent e){
	Window w = e.getWindow();

	if(w == contourLevelDialog){
	    //contourLevelDialog.hide();
	    contourLevelDialog.hide();
	    contourLevelDialog.dispose();
	    contourLevelDialog = null;
	}else{
	    w.hide();
	    w.dispose();
	    saveAndExit();
	}
    }

    public void windowActivated(WindowEvent e){ }
    public void windowClosed(WindowEvent e){ }
    public void windowDeactivated(WindowEvent e){ }
    public void windowDeiconified(WindowEvent e){ }
    public void windowIconified(WindowEvent e){ }
    public void windowOpened(WindowEvent e){ }

    /** The popup menu we will use if necessary. */
    private PopupMenu popup = null;

    /** Set whether we should use the popup menu. */
    public void setUsePopupMenu(boolean state){
	if(state == true){
	    popup = new PopupMenu();
	}else{
	    popup = null;
	}
    }

    /** Show the popup menu. */
    private void showPopupMenu(MouseEvent e){
	//System.out.println("try to show popup");
	if(popup != null){
	    //System.out.println("popup non-null");
	    add(popup);
	    popup.show(this, e.getX(), e.getY());
	}
    }

    /** Create a popup menu. */
    public void createPopup(MouseEvent e){
	PopupMenu popupMenu = new PopupMenu();
	add(popupMenu);
	popupMenu.add("display");
	popupMenu.add("display2");
	popupMenu.show((Component)e.getSource(), e.getX(), e.getY());
	remove(popupMenu);
    }

    /** Should we show the version. */
    public void setShowVersion(boolean b){
	showVersion = b;
    }

    public void install(Object o){
#ifdef XRAYTOOLS
        setManipulator((Manipulator)o);
#endif
    }

#ifdef XRAYTOOLS
    /** The manipulator object for the viewer. */
    private Manipulator manipulator;
#endif
    
#ifdef XRAYTOOLS
    /** Get the value of manipulator. */
    protected Manipulator getManipulator() {
	return manipulator;
    }
#endif
    
#ifdef XRAYTOOLS
    /** Set the value of manipulator. */
    protected void setManipulator(Manipulator  v) {
	this.manipulator = v;
    }
#endif

    public String onClickLabel = null;

    /** Handle a mouseReleased event. */
    public void mouseReleased(MouseEvent e){
	if(e.isAltDown()){
#ifdef XRAYTOOLS
	    if(manipulator != null && pickedAtom != null){
		manipulator.release(e, this, pickedAtom);
	    }
#endif
	    return;
	}

	if(e.isPopupTrigger()){
	    showPopupMenu(e);
	}else{
	    if(pickedAtom != null){
		if(dragged == false){
					
		    if(e.isShiftDown()){
			moleculeRenderer.setCenter(pickedAtom);
						
			//}else if(e.isControlDown()){
		    }else{
			//Log.info("saw picked atom " + pickedAtom);
			moleculeRenderer.addSelectedAtom(pickedAtom);
			
			moleculeRenderer.handlePick(pickedAtom);

                        if(onClickLabel != null){
                            if(pickedAtom.getCustomLabel() == null){
                                String label = pickedAtom.generateLabel(onClickLabel);
                                pickedAtom.setCustomLabel(label);
                            }else{
                                pickedAtom.setCustomLabel(null);
                            }
                        }else{
                            if(pickedAtom.isLabelled()){
                                pickedAtom.setLabelled(false);
                                moleculeRenderer.setStatusAtom(null);
                            }else{
                                moleculeRenderer.removeAllLabelledAtoms();
                                pickedAtom.setLabelled(true);
                                moleculeRenderer.setStatusAtom(pickedAtom);
                            }
                        }
						
			moleculeRenderer.generateBumps(pickedAtom);
			//moleculeRenderer.generateBumps(pickedAtom);
			
		    }
		}
	    }else{
		
		//		    if(moveAtomsAllowed){
		//	moleculeRenderer.finishOptimization();
		//}
		if(dragged == false){
		    moleculeRenderer.removeAllSelectedAtoms();
		// fix
                    moleculeRenderer.setStatusAtom(null);
		}
	    }

	    if(centerMoved){
		// try and repaint before redoing maps
		// makes the transition a bit smoother
		dirtyRepaint();

		// we need to force a real recenter
		// to redo the maps.
		Point3d center = moleculeRenderer.renderer.getCenter();
		moleculeRenderer.setCenter(center, false);
		centerMoved = false;
	    }else if(e.isShiftDown()){
	    }else{
		if(dragged == false){
		    //if(e.isControlDown()){
		    //moleculeRenderer.removeAllSelectedAtoms();
		    // fix
		    //moleculeRenderer.renderer.setStatusString(null);
		    //}
		}
	    }
	}

	pickedAtom = null;

	lastMouseEvent = null;
	mousePressedEvent = null;

	dirtyRepaint();

	suspendAnimationThread(true);
    }

    /** Handle the mouse being dragged. */
    public void mouseDragged(MouseEvent e){
	if(e.isAltDown()){
#ifdef XRAYTOOLS
	    if(manipulator != null && pickedAtom != null){
		manipulator.drag(e, this, pickedAtom);
	    }
#endif
	    return;
	}

	int dx = e.getX() - mousePressedEvent.getX();
	int dy = e.getY() - mousePressedEvent.getY();

	if(Math.abs(dx) > 2 || Math.abs(dy) > 2){
	    dragged = true;
	}

	if(dragged){
	    dx = e.getX() - lastMouseEvent.getX();
	    dy = e.getY() - lastMouseEvent.getY();

	    if(pickedAtom != null && moveAtomsAllowed == true){
		if(dragged == false){
		    //System.out.println("setting up optimizer");
		    //		if(moveAtomsAllowed){
		    //   moleculeRenderer.setupOptimization(pickedAtom);
		    //}
		}

		//System.out.println("drag moving atom");
		//System.out.println("running optimizer");

		//	    if(moveAtomsAllowed){
		//	moleculeRenderer.moveAtom(pickedAtom, dx, dy);
		//			
		//	moleculeRenderer.runOptimization(pickedAtom);
		//}
	    }else if(e.isControlDown()){
		//System.out.println("translate by " + dx + " " + dy);
		moleculeRenderer.translateCenter(dx, dy);
		centerMoved = true;
	    }else if(e.isShiftDown()){
		//System.out.println("drag scaling");
		// scale the molecule
		moleculeRenderer.renderer.applyZoom(dy * 0.005);
	    }else{
		if(mousePressedEvent.getY() < bounds().height * 0.05){
		    moleculeRenderer.renderer.rotateZ(dx * 0.5);
		}else{
		//System.out.println("drag rotating");
		    moleculeRenderer.renderer.rotateY(dx * 0.5);
		    moleculeRenderer.renderer.rotateX(dy * 0.5);
		}
	    }
	    
	    lastMouseEvent = e;
	}

        viewChangeOnly = true;

	dirtyRepaint();
    }

    String mouseOverCommand = null;
    String mouseOverLabel = null;
    Atom trackedAtom = null;
    String trackedAtomLabel = null;

    public void handleMouseOver(MouseEvent e){

        Atom nearestAtom = moleculeRenderer.getNearestAtom(e.getX(), e.getY());

        if(nearestAtom == trackedAtom){
            return;
        }
            
        if(trackedAtom != null){
            // put the label back
            trackedAtom.setCustomLabel(trackedAtomLabel);
            trackedAtom = null;
        }

        if(nearestAtom != null){
            trackedAtom = nearestAtom;
            trackedAtomLabel = nearestAtom.getCustomLabel();
            trackedAtom.setCustomLabel(mouseOverLabel);



        }



        dirtyRepaint();
    }

    public void handleMouseOverCommand(MouseEvent e){

        Atom nearestAtom = moleculeRenderer.getNearestAtom(e.getX(), e.getY());

        if(nearestAtom == trackedAtom){
            return;
        }
            
        if(trackedAtom != null){
        }

        if(nearestAtom != null){
            trackedAtom = nearestAtom;

            String command = nearestAtom.generateLabel(mouseOverCommand);

            execute(command);
        }



        dirtyRepaint();
    }

    public void mouseEntered(MouseEvent e){
	//requestFocus();
    }

    public void mouseExited(MouseEvent e){
    }

    public void mouseClicked(MouseEvent e){
    }

    /* Implementation of MouseMotionListener. */
    public void mouseMoved(MouseEvent e){
        if(mouseOverLabel != null){
            handleMouseOver(e);
        }

        if(mouseOverCommand != null){
            handleMouseOverCommand(e);
        }
    }

    /** Implementation of KeyListener. */
    public void keyPressed(KeyEvent e){

	if(e.getKeyCode() == KeyEvent.VK_CONTROL ||
	   e.getKeyCode() == KeyEvent.VK_ALT ||
	   e.getKeyCode() == KeyEvent.VK_SHIFT){
	    // we don't want the repeat events for
	    // holding the modifier key itself...
	    //Log.info(e.toString());
	    return;
	}

	boolean redraw = true;
	int c = e.getKeyChar();

	int code = e.getKeyCode();

	String key = "";

	if(e.isControlDown()){
	    if(key.length() > 0){
		key += "+";
		}
	    key += "control";
	}
	
	if(e.isShiftDown()){
	    if(key.length() > 0){
		key += "+";
	    }
	    key += "shift";
	}

	if(e.isAltDown()){
	    if(key.length() > 0){
		key += "+";
	    }
	    key += "alt";
	}

	if(key.length() > 0){
	    key += "+";
	}

	key += (char)e.getKeyCode();

	String modifiers = e.getKeyModifiersText(e.getModifiers());
	key       = e.getKeyText(e.getKeyCode());

	if(!modifiers.equals("")){
	    key = modifiers + "+" + key;
	}

	//Log.info("key <" + key +">");

	String command = (String)keyDefinitions.get(key);

	if(command != null){
	    moleculeRenderer.execute(command);

	    dirtyRepaint();
	    return;
	}

	if(c == 'r'){
	    moleculeRenderer.resetView();
	}else if(c == '!'){
	    System.out.println("saw ! character");

	    moleculeRenderer.reExecute();
	}else if(c == 'q'){
	    saveAndExit();
	}else if(c == '0'){
	    moleculeRenderer.renderer.analyticalSpheres =
		!moleculeRenderer.renderer.analyticalSpheres;
	}else if(c == 'b'){
	    moleculeRenderer.allowFastDraw = !moleculeRenderer.allowFastDraw;
	}else if(c == 'B'){
	    if(e.isControlDown()){
		moleculeRenderer.execute("object * backface off;");
	    }else{
		moleculeRenderer.execute("object * backface on;");
	    }
	}else if(c == 'f'){
	    showFrameRate = !showFrameRate;
	    frameCount = 0;
	}else if(c == 'v'){
	    showVersion = !showVersion;

	    if(showVersion){
		String version =
		    "AstexViewer " + Version.getVersion() +
		    " Copyright (C) 1999-2007 Astex Therapeutics Ltd.";
		moleculeRenderer.renderer.setLogo(version);
	    }else{
		moleculeRenderer.renderer.setLogo(null);
	    }
	}else if(c == 'd'){
	    moleculeRenderer.addDistanceBetweenSelectedAtoms();
	}else if(c == 'c'){
	    DynamicArray selectedAtoms =
		moleculeRenderer.getSelectedOrLabelledAtoms();

	    moleculeRenderer.setCenter(selectedAtoms);

	    double r = moleculeRenderer.getRadius(selectedAtoms);

	    moleculeRenderer.setRadius(r);

	    r = moleculeRenderer.renderer.getRadius();

	    //System.out.println("setClip in MoleculeViewer to " + r);

	    moleculeRenderer.renderer.setClip(r);

	}else if(c == 'z'){
	    moleculeRenderer.renderer.depthcue =
		!moleculeRenderer.renderer.depthcue;
	}else if(c == 'm'){
	    moleculeRenderer.printMatrix();
	}else if(c == '-'){
	    if(e.isControlDown()){
		moleculeRenderer.renderer.decrementFrontClip();
	    }else{
		moleculeRenderer.renderer.decrementClip();
	    }
	}else if(c == '+' || c == '='){
	    if(e.isControlDown()){
		moleculeRenderer.renderer.incrementFrontClip();
	    }else{
		moleculeRenderer.renderer.incrementClip();
	    }
	    //}else if(c == 'c'){
	    //moleculeRenderer.clipMaps();
	}else if(c == 's'){
	    moleculeRenderer.execute("select aminoacid and sphere 8.0 around current; surface binding_site green aminoacid and sphere 12.0 around current;");
	}else if(c == 'w'){
	    moleculeRenderer.execute("display wide current;");
	}else if(c == 'u'){
	    moleculeRenderer.execute("remove object binding_site;");
	}else if(c == 't'){
	    moleculeRenderer.execute("dotsurface testdots 4 current;");
	}else if(c == 'A'){
	    AtomIterator atomIterator = moleculeRenderer.getAtomIterator();

	    while(atomIterator.hasMoreElements()){
	    	Atom atom = atomIterator.getNextAtom();
	    	atom.setSelected(true);
	    }
	}else if(c == '.'){
	    AtomIterator atomIterator = moleculeRenderer.getAtomIterator();

	    while(atomIterator.hasMoreElements()){
	    	Atom atom = atomIterator.getNextAtom();
		if(atom.isSelected()){
		    atom.attributes |= Atom.VDWSphere;
		}else{
		    atom.attributes &= ~Atom.VDWSphere;
		}
	    }
	    
	}else if(c == '5'){
	    moleculeRenderer.renderer.emulate555 =
		!moleculeRenderer.renderer.emulate555;
	    //System.out.println("emulate555 " + moleculeRenderer.renderer.emulate555);
	}else if(c == 'a'){
	    
	    if(moleculeRenderer.pickMode != moleculeRenderer.ANGLE_PICK){
		moleculeRenderer.setPickMode(moleculeRenderer.ANGLE_PICK);
	    }else{
		moleculeRenderer.setPickMode(moleculeRenderer.NORMAL_PICK);
	    }
	}else{
	    redraw = false;
	}

	if(redraw){
	    dirtyRepaint();
	}


	System.gc();
    }

    public void keyReleased(KeyEvent e){
    }

    public void keyTyped(KeyEvent e){
    }

    /** The list of atoms that are picked. */
    private DynamicArray pickedAtoms = new DynamicArray();

    /* Tokens for the menu items. */
	
    private static String FileString = "File";
    private static String OpenStructureString = "Open Structure...";
    private static String OpenMapString = "Open Map...";
    private static String OpenObjectString = "Open Object...";
    private static String RunScriptString = "Run Script...";
    private static String SaveViewString = "Save View...";
    private static String SaveString = "Save All";
    private static String SaveMoleculeString = "Save Molecule";
    private static String WriteBMPString = "Write BMP...";
    private static String ExitString = "Exit";

    private static String DisplayString = "Display";
    private static String SymmetryString = "Symmetry";
    private static String BumpsString = "Bumps";
    private static String MapsString = "Maps";
    private static String SolventString = "Solvent";

    private static String OptionsString = "Options";
    private static String MoveAtomsString = "Move Atoms";

    private static String ColorString = "Colour";
    private static String ColorByChainString = "By Chain";
    private static String ColorByAtomString = "By Atom";
    private static String ColorByBFactorString = "By B-factor";
    private static String ColorByBFactorRangeString = "By B-factor Range";
    private static String ColorByRainbowString = "By Rainbow";
    private static String ColorChoiceString = "Change To";
    private static String ColorBackgroundString = "Background";

    private static String CloseString = "Close";

    private static String SelectString = "Select";
    private static String SelectionPopupString = "Popup...";
    private static String SelectLigandString = "Ligands";
    private static String LigandString = "Ligand";
    private static String ClearSelectionString = "Clear";
    private static String ExcludeString = "Exclude";

    private static String ViewString = "View";
    private static String ResetViewString = "Reset";
    private static String CenterViewString = "Center On Selection";
    private static String ClipMapsToSelectionString =
	"Clip Maps To Selection";
    private static String WideBondsForSelectionString =
	"Wide Bonds For Selection";
    private static String ContourLevelsString = "Contour Levels...";

    /** The menu that we will use for closing molecules. */
    private Menu closeMoleculeMenu = null;

    /** The menu that we will use for saving molecules. */
    private Menu saveMoleculeMenu = null;

    /** The menu that we will use for choosing ligand residues. */
    private Menu ligandMenu = null;

    /** Create the menu bar that we will use. */
    public MenuBar createMenuBar(){
	DynamicArray menus = new DynamicArray();

	menus.add(createFileMenu());

	menus.add(createSelectMenu());

	menus.add(createDisplayMenu());

	menus.add(createColorMenu());

	menus.add(createViewMenu());

	menus.add(createMeasureMenu());

	//menus.add(createOptionsMenu());

	updateMenus();

	MenuBar menuBar = new MenuBar();

	for(int i = 0; i < menus.size(); i++){
	    Menu menu = (Menu)menus.get(i);
	    if(popup != null){
		popup.add(menu);
	    }else{
		menuBar.add(menu);
	    }
	}

	return menuBar;
    }

    /** Create the file menu. */
    public Menu createFileMenu(){
	Menu fileMenu = new Menu(FileString);
	fileMenu.add(createMenuItem(OpenStructureString));
	fileMenu.add(createMenuItem(OpenMapString));
	fileMenu.add(createMenuItem(OpenObjectString));
	fileMenu.add(createMenuItem(RunScriptString));
	fileMenu.addSeparator();

	fileMenu.add(createSaveMoleculeMenu());
	fileMenu.add(createMenuItem(SaveViewString));
	fileMenu.add(createMenuItem(SaveString));

	fileMenu.add(createCloseMoleculeMenu());
	fileMenu.addSeparator();
	fileMenu.add(createBMPMenu());
	fileMenu.addSeparator();
	fileMenu.add(createMenuItem(ExitString));

	return fileMenu;
    }

    private static final String bitmapPrefix = "Current size";

    private Hashtable writeBMPHash= new Hashtable();

    public Menu createBMPMenu(){
	Menu menu = new Menu(WriteBMPString);

	for(int i = 0; ; i++){
	    String label   = Settings.getString("config", "imagesizelabel." + i);
	    String command = Settings.getString("config", "imagesizecommand." + i);

	    if(label == null){
		break;
	    }

	    menu.add(createMenuItem(label));
	    writeBMPHash.put(label, command);
	}

	return menu;
    }

    /** Create the select menu. */
    public Menu createSelectMenu(){
	Menu selectMenu = new Menu(SelectString);
	selectMenu.add(createMenuItem(SelectionPopupString));
	selectMenu.addSeparator();
	selectMenu.add(createMenuItem(ClearSelectionString));
	selectMenu.addSeparator();
	selectMenu.add(createCheckboxMenuItem(ExcludeString,
					      excludeFromSelection));
	selectMenu.addSeparator();
	MenuItem menuItem = createMenuItem(SelectLigandString);
	menuItem.setActionCommand(SelectLigandCommand);
	selectMenu.add(menuItem);
	selectMenu.addSeparator();
	selectMenu.add(createLigandMenu());

	return selectMenu;
    }

    /** Create the display menu. */
    public Menu createDisplayMenu(){
	Menu displayMenu = new Menu(DisplayString);
	displayMenu.add(createCheckboxMenuItem(MapsString, true));
	displayMenu.add(createCheckboxMenuItem(SymmetryString, true));
	displayMenu.add(createCheckboxMenuItem(BumpsString, false));
	displayMenu.add(createCheckboxMenuItem(SolventString, true));

	return displayMenu;
    }

    public String builtinColors[] = {
	"red", "green", "blue", "cyan", "magenta", "orange", "yellow", "brown",
	"black", "white", "darkgrey"
    };

    /** Create the color menu. */
    public Menu createColorMenu(){
	Menu colorMenu = new Menu(ColorString);

	colorMenu.add(createMenuItem(ColorByAtomString));
	colorMenu.add(createMenuItem(ColorByChainString));
	colorMenu.add(createMenuItem(ColorByBFactorString));
	colorMenu.add(createMenuItem(ColorByBFactorRangeString));
	colorMenu.add(createMenuItem(ColorByRainbowString));

	colorMenu.addSeparator();

	Menu colorChoiceMenu = new Menu(ColorChoiceString);
	Menu colorBackgroundMenu = new Menu(ColorBackgroundString);

	for(int i = 0; i < builtinColors.length; i++){
	    String colorName = builtinColors[i];
	    String firstLetter = colorName.substring(0, 1).toUpperCase();
	    String label = firstLetter + colorName.substring(1);
	    MenuItem menuItem = createMenuItem(label);
	    menuItem.setActionCommand(SetColorCommand + " " + colorName);
	    colorChoiceMenu.add(menuItem);

	    MenuItem menuItem2 = createMenuItem(label);
	    menuItem2.setActionCommand(SetBackgroundColorCommand +
				       " " + colorName);
	    colorBackgroundMenu.add(menuItem2);
	}

	colorMenu.add(colorChoiceMenu);

	colorMenu.addSeparator();

	colorMenu.add(colorBackgroundMenu);

	return colorMenu;
    }

    /** Create the view menu. */
    public Menu createViewMenu(){
	Menu viewMenu = new Menu(ViewString);

	viewMenu.add(createMenuItem(ResetViewString));
	viewMenu.add(createMenuItem(CenterViewString));
	viewMenu.add(createMenuItem(ClipMapsToSelectionString));
	viewMenu.add(createMenuItem(WideBondsForSelectionString));
	viewMenu.add(createMenuItem(ContourLevelsString));

	return viewMenu;
    }

    /** Create the option menu. */
    public Menu createOptionsMenu(){
	Menu optionsMenu = new Menu(OptionsString);
	optionsMenu.add(createCheckboxMenuItem(MoveAtomsString,
					       moveAtomsAllowed));

	return optionsMenu;
    }

    /** Create the menu that lets us close molecules. */
    public Menu createCloseMoleculeMenu(){
	ensureMenusCreated();

	return closeMoleculeMenu;
    }

    /** Create the menu that lets us save molecules. */
    public Menu createSaveMoleculeMenu(){
	ensureMenusCreated();

	return saveMoleculeMenu;
    }

    /** Create the menu that lets us choose ligand groups. */
    public Menu createLigandMenu(){
	ensureMenusCreated();

	return ligandMenu;
    }

    CheckboxMenuItem nothingCheckbox = null;
    CheckboxMenuItem distancesCheckbox = null;
    CheckboxMenuItem angleCheckbox = null;
    CheckboxMenuItem torsionsCheckbox = null;

    /** Create the menu that picks measurement options. */
    public Menu createMeasureMenu(){
	ensureMenusCreated();

	Menu measureMenu = new Menu("Measure");
	
	nothingCheckbox = createCheckboxMenuItem("Nothing", true);
	measureMenu.add(nothingCheckbox);
	distancesCheckbox = createCheckboxMenuItem("Distances", false);
	measureMenu.add(distancesCheckbox);
	angleCheckbox = createCheckboxMenuItem("Angles", false);
	measureMenu.add(angleCheckbox);
	torsionsCheckbox = createCheckboxMenuItem("Torsions", false);
	measureMenu.add(torsionsCheckbox);
	measureMenu.addSeparator();
	measureMenu.add(createMenuItem("Clear distances"));
	measureMenu.add(createMenuItem("Clear angles"));
	measureMenu.add(createMenuItem("Clear torsions"));

	return measureMenu;
    }	

    /** Create a single menu item. */
    public MenuItem createMenuItem(String label){
	MenuItem menuItem = new MenuItem(label);
	menuItem.addActionListener(this);
	return menuItem;
    }

    /** Create a single checkbox menu item. */
    public CheckboxMenuItem createCheckboxMenuItem(String label,
						   boolean state){
	CheckboxMenuItem menuItem = new CheckboxMenuItem(label);
	menuItem.setState(state);
	menuItem.addItemListener(this);
	return menuItem;
    }

#ifndef NOBWT
    /** The user interface that we will generated if necessary. */
    public transient UserInterface ui = null;
#endif

    /** Implementation of ActionListener. */
    public void actionPerformed(ActionEvent e){
	boolean redraw = true;
	boolean handled = true;

	Object source = e.getSource();

	if(source instanceof ColorButton){
	    ColorButton button = (ColorButton)source;

	    Map map = (Map)mapHashtable.get(button);
	    Integer contour = (Integer)contourLevelHashtable.get(button);

	    Point p = button.getLocationOnScreen();
	    
	    //String color = getColor(p.x, p.y);
	    String color = button.getValue();
	    
	    if(color != null){
		String mapName = map.getName();
	    
		String command =
		    "map '" + mapName + "' contour " + contour + " '" + color + "';";

		//System.out.println("command " + command);
		
		moleculeRenderer.execute(command);

		int c = Color32.getColorFromName(color);

		Color awtColor = Color32.getAWTColor(c);

		button.setBackground(awtColor);
		button.setForeground(Color.black);
	    }

	    dirtyRepaint();
	    return;
	}

	Object target = e.getSource();
	String command = e.getActionCommand();

	if(target == centerField){
	    command = command.toUpperCase();
	    moleculeRenderer.execute("center composite '" + command + "';");
	}else{
	    handled = false;
	}

	if(handled){
	    dirtyRepaint();
	    return;
	}

	if(command.equals(ExitString)){
	    saveAndExit();
	}else if(command.equals(RunScriptString)){
	    String scriptFile = loadFile("Run script");

	    if(scriptFile != null){
		executeScript(scriptFile);
	    }
	}else if(command.equals(OpenStructureString)){
	    String pdbFile = loadFile("Load structure");

	    if(pdbFile != null){
		loadMolecule(pdbFile);
	    }
	}else if(command.equals(OpenMapString)){
	    String mapFile = loadFile("Load map");

	    if(mapFile != null){
		loadMap(mapFile);
	    }
	}else if(command.equals(OpenObjectString)){
	    String tmeshFile = loadFile("Load object (.tmesh)");

	    if(tmeshFile != null){
		Tmesh tm = Tmesh.read(tmeshFile);
		moleculeRenderer.renderer.addTmesh(tm);
	    }
	}else if(command.equals(ColorByChainString)){

	    moleculeRenderer.colorByChain();
	}else if(command.equals(ColorByAtomString)){

	    moleculeRenderer.colorByAtom();
	}else if(command.equals(ColorByBFactorString)){

	    moleculeRenderer.colorByBFactor();
	}else if(command.equals(ColorByBFactorRangeString)){

	    moleculeRenderer.colorByPropertyRange(Atom.B);
	}else if(command.equals(ColorByRainbowString)){

	    moleculeRenderer.execute("color_by_rainbow default;");
	}else if(command.equals(ClearSelectionString)){

	    moleculeRenderer.removeAllSelectedAtoms();
	}else if(command.equals(SelectionPopupString)){

#ifndef NOBWT
	    if(ui == null){
		ui = new UserInterface(this);
	    }else{
		ui.userInterfaceFrame.show();
	    }
#endif

	}else if(command.equals(CenterViewString)){
	    DynamicArray selectedAtoms =
		moleculeRenderer.getSelectedOrLabelledAtoms();

	    moleculeRenderer.setCenter(selectedAtoms);

	}else if(command.equals(ClipMapsToSelectionString)){
	    DynamicArray selectedAtoms =
		moleculeRenderer.getSelectedAtoms();

	    moleculeRenderer.clipMaps(null, selectedAtoms, true);
	}else if(command.equals(WideBondsForSelectionString)){
	    DynamicArray selectedAtoms =
		moleculeRenderer.getSelectedAtoms();

	    moleculeRenderer.setWideBonds(selectedAtoms);
	}else if(command.equals(ResetViewString)){

	    moleculeRenderer.resetView();
	}else if(command.equals(ContourLevelsString)){

	    showContourLevelDialog();

	}else if(command.equals(HideContourDialogString)){

	    hideContourLevelDialog();
	}else if(writeBMPHash.containsKey(command)){
	    String bitmapFileName = saveFile("Choose a BMP file...", null);

	    if(bitmapFileName != null){

		System.out.println("starting offscreen render");

		String s = (String)writeBMPHash.get(command);
		
		s += " -writeimage '" + bitmapFileName + "';";
		
		moleculeRenderer.execute(s);

		System.out.println("finished writing image");
	    }
	}else if(command.equals("Clear distances")){
	    moleculeRenderer.removeAllDistances();

	}else if(command.equals("Clear angles")){
	    moleculeRenderer.removeAllAngles();

	}else if(command.equals("Clear torsions")){
	    moleculeRenderer.removeAllTorsions();

	}else{
	    redraw = handleMenuAction(e, command);
	}

	if(redraw){
	    dirtyRepaint();
	}
    }

    /** The command to hide the contour dialog. */
    private static String HideContourDialogString = "Hide contour dialog";

    /** The contour level dialog. */
    private transient Dialog contourLevelDialog = null;

    /** The hashtable to map from sliders to checkboxes. */
    private Hashtable checkboxHashtable = new Hashtable();

    /** The hashtable to map from sliders to maps. */
    private Hashtable mapHashtable = new Hashtable();

    /** The hashtable to map from sliders to contourlevels. */
    private Hashtable contourLevelHashtable = new Hashtable();

    /** Actually build the contour level. */
    public void buildContourLevelDialog(){
	checkboxHashtable.clear();
	mapHashtable.clear();
	contourLevelHashtable.clear();

	contourLevelDialog.setLayout(new GridBagLayout());

	int mapCount = moleculeRenderer.getMapCount();
	int contourSlot = 0;
	for(int m = 0; m < mapCount; m++){
	    Map map = moleculeRenderer.getMap(m);
	    Label label = new Label(map.getFile());
	    Layout.fill(contourLevelDialog, label, 0, contourSlot++,
			GridBagConstraints.REMAINDER, 1);

	    for(int i = 0; i < Map.MaximumContourLevels; i++){
		addContourLevelControls(map, contourSlot++, i);
	    }
	}

	Panel panel = new Panel();

	Button dismissButton = new Button("Done");
	dismissButton.setActionCommand(HideContourDialogString);
	dismissButton.addActionListener(this);
	panel.add(dismissButton);

	Layout.fill(contourLevelDialog, panel, 0, contourSlot,
		    GridBagConstraints.REMAINDER, 1,
		    GridBagConstraints.HORIZONTAL);

	contourLevelDialog.addWindowListener(this);

	//contourLevelDialog.setSize(400, 0);
	contourLevelDialog.reshape(0, 709, 240, 80);
	contourLevelDialog.pack();
	//contourLevelDialog.layout();
	contourLevelDialog.show();
    }

    /** Add a single contour level control to the dialog. */
    private void addContourLevelControls(Map map, int y, int contourLevel){
	double level = map.getContourLevel(contourLevel);
	int color = map.getContourColor(contourLevel);
	boolean displayed = map.getContourDisplayed(contourLevel);

	int column = 0;

	//Checkbox checkbox = new Checkbox("xxx");
	Checkbox checkbox = new Checkbox("");
	// fix
	checkbox.setState(displayed);
	checkbox.addItemListener(this);
	Layout.constrain(contourLevelDialog, checkbox,
			 column++, y, 1, 1,
			 GridBagConstraints.NONE,
			 GridBagConstraints.WEST,
			 0., 0.);
	//Layout.nofill(contourLevelDialog, checkbox, 0, y);


	//JCButton label = new JCButton("");
	//label.setHighlightThickness(1);
	//label.setShadowThickness(0);
	//Color awtColor = Color32.getAWTColor(color);
	//label.setForeground(awtColor);
	//label.setText(contourFormat.format(level));
	//label.addActionListener(this);

	Color awtColor = Color32.getAWTColor(color);
	ColorButton label = new ColorButton(awtColor, 12);
	//label.setForeground(Color.black);
	//label.setLabel(contourFormat.format(level));
	//label.addActionListener(this);
	checkbox.setForeground(Color.black);
	checkbox.setLabel(contourFormat.format(level));
	label.addActionListener(this);

	Layout.constrain(contourLevelDialog, label,
			 column++, y, 1, 1,
			 GridBagConstraints.NONE,
			 GridBagConstraints.WEST,
			 0., 0.);

	Scrollbar contourScrollbar = new ContourScrollbar(Scrollbar.HORIZONTAL);
	int range = 500;
	if(map.getMapType() != Map.CCP4_BINARY){
	    range = 2000;
	}
	contourScrollbar.setMinimum(-range);
	contourScrollbar.setMaximum(range);
	contourScrollbar.setUnitIncrement(10);
	contourScrollbar.setBlockIncrement(100);
	contourScrollbar.setValue((int)(level * 100.0));
	contourScrollbar.addAdjustmentListener(this);

	Layout.constrain(contourLevelDialog, contourScrollbar,
			 column++, y, 1, 1,
			 GridBagConstraints.HORIZONTAL,
			 GridBagConstraints.WEST,
			 100., 0.);

	checkboxHashtable.put(contourScrollbar, checkbox);
	mapHashtable.put(contourScrollbar, map);

	Integer contour = new Integer(contourLevel);
	contourLevelHashtable.put(contourScrollbar, contour);

	mapHashtable.put(checkbox, map);
	contourLevelHashtable.put(checkbox, contour);
    	mapHashtable.put(label, map);
	contourLevelHashtable.put(label, contour);
    }

    /** Popup a dialog which allow us to alter contour levels. */
    public void showContourLevelDialog(){
	if(contourLevelDialog == null){
	    Frame frame = Util.getFrame(this);
	    contourLevelDialog =
		new Dialog(frame, "Change Map Contour Levels", false);
		
	    buildContourLevelDialog();
	}else{
	    contourLevelDialog.show();
	}
    }

    /** Hide the contour level dialog. */
    public void hideContourLevelDialog(){
	if(contourLevelDialog != null){
	    //contourLevelDialog.hide();
	    contourLevelDialog.dispose();
	    contourLevelDialog = null;
	}
    }

    /** Format object for formatting the label. */
    private Format contourFormat = new Format("%6.2f");

    /** AdjustmentListener to listen to contour levels being created. */
    public void adjustmentValueChanged(AdjustmentEvent e){
	Adjustable slider = e.getAdjustable();
	int value = e.getValue();

	Map map = (Map)mapHashtable.get(slider);
	Integer contour = (Integer)contourLevelHashtable.get(slider);
	double level = 0.01 * value;
			
	map.setContourLevel(contour.intValue(), level);

	moleculeRenderer.contourMap(map, contour.intValue());

	// force the label to a tidy value
	int tidyValue = 5 * (value/5);
	double tidyLevel = 0.01 * tidyValue;

	Checkbox checkbox = (Checkbox)checkboxHashtable.get(slider);
	checkbox.setLabel(contourFormat.format(tidyLevel));
		
	dirtyRepaint();
    }

    /* Various command names. */
    private static String CloseMoleculeCommand = "CloseMolecule";
    private static String SaveMoleculeCommand = "SaveMolecule";
    private static String SelectCommand = "Select";
    private static String SelectLigandCommand = "SelectLigand";
    private static String SetColorCommand = "SetColor";
    private static String SetBackgroundColorCommand = "SetBackgroundColor";

    /** Handle more complex menu actions. */
    public boolean handleMenuAction(ActionEvent e, String commandString){
	String words[] = FILE.split(commandString);
	String command = words[0];
	boolean redraw = true;

	if(command.equals(CloseMoleculeCommand)){
	    moleculeRenderer.removeMoleculeByName(words[1]);

	}else if(command.equals(SaveMoleculeCommand)){
	    saveMoleculeByName(words[1]);

	}else if(command.equals(SetColorCommand)){
	    //int color = Color32.getColorFromName(words[1]);

	    //moleculeRenderer.colorBySpecifiedColor(color);

	    moleculeRenderer.execute("color "+ words[1] + " default;");
	}else if(command.equals(SetBackgroundColorCommand)){
	    //int color = Color32.getColorFromName(words[1]);

	    //moleculeRenderer.renderer.setBackgroundColor(color);
	    moleculeRenderer.execute("background "+ words[1] +";");
	}else if(command.equals(SelectCommand)){
	    DynamicArray selection =
		moleculeRenderer.getAtomsInSelection(words[1]);

	    moleculeRenderer.setSelected(selection, excludeFromSelection);

	}else if(command.equals(SelectLigandCommand)){
	    DynamicArray selection = moleculeRenderer.getAtomsInLigands();

	    moleculeRenderer.setSelected(selection, excludeFromSelection);
	}else if(commandString.equals(SaveViewString)){
	    String viewFile = saveFile("Save view", null);

	    if(viewFile != null){
		saveScript(viewFile);
	    }

	}else if(commandString.equals(SaveString)){
	    saveMolecules();

	}else{
	    System.out.println("unhandled command <" + commandString + ">");

	    redraw = false;
	}

	return redraw;
    }

    /** Save this molecule. */
    public void saveMoleculeByName(String name){
	Molecule mol = moleculeRenderer.getMolecule(name);

	if(mol == null){
	    System.out.println("no such molecule <"+name+">");
	    return;
	}else{
	    String newFileName = saveFile("Choose Filename...", null);

	    // null from saveFile indicates Cancel
	    if(newFileName == null){
		return;
	    }

	    newFileName = checkExtension(mol, newFileName);

	    System.out.println("new file name <"+newFileName+">");

	    FILE output = FILE.write(newFileName);

	    if(output == null){
		System.out.println("saveMolecule: couldn't open " + newFileName);
		return;
	    }

	    String type = MoleculeIO.getTypeFromExtension(newFileName);

	    MoleculeIO.write(mol, output, type);

	    output.close();

	    mol.setName(newFileName);
	    mol.setFilename(newFileName);

	    moleculeRenderer.fireMoleculeRemovedEvent(mol);
	    moleculeRenderer.fireMoleculeAddedEvent(mol);

	    updateMenus();
	}
    }

    /**
     * Check the file extension on a save file name.
     */
    public String checkExtension(Molecule mol, String f){
	String file = null;
	String extension = null;
	boolean validExtension = true;
	int dot = f.lastIndexOf('.');

	if(dot == -1){
	    validExtension = false;
	    file = f;
	}else{
	    file = f.substring(0, dot);
	    extension = f.substring(dot+1, f.length());
	}
    
	if(extension != null &&
	   (extension.equals("pdb") || extension.equals("mol") ||
	    extension.equals("sdf"))){
	    // its ok
	    return f;
	}else{
	    if(extension == null){
		// look to see what we need to do
		String originalName = mol.getFilename();
		dot = originalName.lastIndexOf('.');

		if(dot == -1){
		    // no extension

		    int atomCount = mol.getAtomCount();
		    if(atomCount < 256){
			extension = "mol";
		    }else{
			extension = "pdb";
		    }
		}else{
		    extension =
			originalName.substring(dot+1,
					       originalName.length());
		}
	    }
	}

	//Log.info("file      "+ file);
	//Log.info("extension "+ extension);

	return file + "." + extension;
    }

    /** Implementation of ItemListener. */
    public void itemStateChanged(ItemEvent e){
	Object source = e.getSource();
	boolean redraw = true;

	if(source instanceof CheckboxMenuItem){
	    CheckboxMenuItem menuItem = (CheckboxMenuItem)e.getSource();
	    String command = menuItem.getLabel();
	    boolean state = menuItem.getState();
			
	    if(command.equals(SymmetryString)){

		if(state){
		    moleculeRenderer.execute("set symmetry on;");
		}else{
		    moleculeRenderer.execute("set symmetry off;");
		}
	    }else if(command.equals(BumpsString)){

		moleculeRenderer.setDisplayBumps(state);
	    }else if(command.equals(MapsString)){
				
		moleculeRenderer.setDisplayMaps(state);
	    }else if(command.equals(SolventString)){
				
		moleculeRenderer.setDisplaySolvent(state);
	    }else if(command.equals(MoveAtomsString)){
				
		moveAtomsAllowed = state;
	    }else if(command.equals(ExcludeString)){
				
		excludeFromSelection = state;
	    }else if(command.equals("Torsions")){
		if(state){
		    moleculeRenderer.setPickMode(MoleculeRenderer.TORSION_PICK);
		}else{
		    nothingCheckbox.setState(true);
		}
		angleCheckbox.setState(false);
		distancesCheckbox.setState(false);
		nothingCheckbox.setState(false);
	    }else if(command.equals("Angles")){
		if(state){
		    moleculeRenderer.setPickMode(MoleculeRenderer.ANGLE_PICK);
		}else{
		    nothingCheckbox.setState(true);
		}
		torsionsCheckbox.setState(false);
		distancesCheckbox.setState(false);
		nothingCheckbox.setState(false);
	    }else if(command.equals("Distances")){
		if(state){
		    moleculeRenderer.setPickMode(MoleculeRenderer.DISTANCE_PICK);
		}else{
		    nothingCheckbox.setState(true);
		}
		torsionsCheckbox.setState(false);
		angleCheckbox.setState(false);
		nothingCheckbox.setState(false);
	    }else if(command.equals("Nothing")){
		if(state){
		    moleculeRenderer.setPickMode(MoleculeRenderer.NORMAL_PICK);
		}
		torsionsCheckbox.setState(false);
		distancesCheckbox.setState(false);
		angleCheckbox.setState(false);
	    }else{
		redraw = false;
	    }
	}else if(source instanceof Checkbox){
	    Checkbox checkbox = (Checkbox)source;
	    boolean state = checkbox.getState();
	    Map map = (Map)mapHashtable.get(checkbox);
	    Integer contour = (Integer)contourLevelHashtable.get(checkbox);
	    
	    map.setContourDisplayed(contour.intValue(), state);

	    moleculeRenderer.contourMap(map, contour.intValue());
	}

	if(redraw){
	    dirtyRepaint();
	}
    }

    private boolean viewChangeOnly = false;

    /** Mark the renderer dirty and ask for repaint(). */
    // this used to be synchronized... this causes deadlocks though
    // I'm sure there was a reason for it but can't recall it now
    public void dirtyRepaint(){
	moleculeRenderer.dirty = true;
	repaint();

        if(viewChangeOnly == false && repaintListeners != null){
            notifyRepaintListeners();
        }

        viewChangeOnly = false;
    }

    DynamicArray repaintListeners = null;

    public void addRepaintListener(Component c){
        if(repaintListeners == null){
            repaintListeners = new DynamicArray();
        }

        repaintListeners.add(c);
    }

    public void removeRepaintListener(Component c){
        if(repaintListeners != null){
            repaintListeners.remove(c);
        }else{
            throw new RuntimeException("no such repaintListener " + c);
        }
    }

    private void notifyRepaintListeners(){
        if(repaintListeners != null){
            int repaintListenerCount = repaintListeners.size();
            
            for(int r = 0; r < repaintListenerCount; r++){
                Component c = (Component)repaintListeners.get(r);
                
                c.repaint();
            }
        }
    }

    /** The pdb file extensions. */
    private static String pdbExtensions[] = {
	"pdb"
    };

    /** Convenience method for loading a file. */
    public String loadFile(String title){
	Frame frame = Util.getFrame(this);

	FileDialog dialog = new FileDialog(frame, title, FileDialog.LOAD);
	UniversalFilenameFilter pdbFilter =
	    new UniversalFilenameFilter(pdbExtensions);

	// don't work...
	//dialog.setFilenameFilter(pdbFilter);
	//dialog.setDirectory("C:\\htx");

	//  wait here for the dialog to be answered
	dialog.show();

	dialog.dispose();

	String directory = dialog.getDirectory();
	String file = dialog.getFile();

	String returnValue = null;

	if(directory == null || file == null){
	    ;
	}else{
	    if(directory.endsWith(File.separator)){
		returnValue = directory + file;
	    }else{
		returnValue = directory + File.separator + file;
	    }
	}

	if(returnValue != null){
	    returnValue = FILE.getRelativePath(returnValue);
	}

	//System.out.println("loadFile returning |" + returnValue + "|");

	return returnValue;
    }

    /** Get a file name to save to. */
    public String saveFile(String title, String extensions[]){
	Frame frame = Util.getFrame(this);

	FileDialog dialog = new FileDialog(frame, title, FileDialog.SAVE);

	if(extensions != null){
	    UniversalFilenameFilter filter =
		new UniversalFilenameFilter(extensions);

	    dialog.setFilenameFilter(filter);
	}

	dialog.show();

	String directory = dialog.getDirectory();
	String file = dialog.getFile();

	dialog.dispose();

	if(directory == null || file == null){
	    return null;
	}else{
	    if(directory.endsWith(File.separator)){
		return directory + file;
	    }else{
		return directory + File.separator + file;
	    }
	}
    }

    /* Implementation of MoleculeRendererListener. */
	
    /** Respond to a molecule added event. */
    public void moleculeAdded(MoleculeRenderer renderer, Molecule molecule){
	// update all the various stuff that changes with the
	// addition of a new molecule
	updateMenus();
    }
	
    /** Respond to a molecule removed event. */
    public void moleculeRemoved(MoleculeRenderer renderer, Molecule molecule){
	updateMenus();
    }

    /** Respond to a map added event. */
    public void mapAdded(MoleculeRenderer renderer, Map map){
	System.out.println("map added");
    }

    /** Respond to a map added event. */
    public void mapRemoved(MoleculeRenderer renderer, Map map){
	System.out.println("map removed");
    }

    /** Respond to an atom click event. */
    public void atomSelected(MoleculeRenderer renderer, Atom atom){
    }

    public void genericAdded(MoleculeRenderer renderer, Generic generic){
    }

    public void genericRemoved(MoleculeRenderer renderer, Generic generic){
    }

    /** Ensure that the close molecule menu is created. */
    public void ensureMenusCreated(){
	if(closeMoleculeMenu == null){
	    closeMoleculeMenu = new Menu(CloseString);
	}

	if(saveMoleculeMenu == null){
	    saveMoleculeMenu = new Menu(SaveMoleculeString);
	}

	if(ligandMenu == null){
	    ligandMenu = new Menu(LigandString);
	}
    }

    /** Cause all the menus to get updated. */
    public void updateMenus(){
	// The menus that can change as we add/remove molecules.
	Menu editableMenus[] = {
	    closeMoleculeMenu,
	    saveMoleculeMenu,
	    ligandMenu
	};

	for(int i = 0; i < editableMenus.length; i++){
	    Menu menu = editableMenus[i];

	    // if any of the menus are null we don't update
	    // as we are probably running without a menubar
	    if(menu == null){
		return;
	    }

	    menu.removeAll();
	}

	ensureMenusCreated();

	updateCloseMoleculeMenu();
	updateSaveMoleculeMenu();
	updateSelectMenu();

	for(int i = 0; i < editableMenus.length; i++){
	    Menu menu = editableMenus[i];

	    if(menu.getItemCount() == 0){
		menu.setEnabled(false);
	    }else{
		menu.setEnabled(true);
	    }
	}
    }

    /** Update the close molecule menu. */
    public void updateCloseMoleculeMenu(){
	int moleculeCount = moleculeRenderer.getMoleculeCount();
	for(int i = 0; i < moleculeCount; i++){
	    Molecule molecule = moleculeRenderer.getMolecule(i);
	    String name = molecule.getName();
	    MenuItem menuItem = createMenuItem(name);
	    menuItem.setActionCommand(CloseMoleculeCommand + " " + name);

	    //System.out.println("adding command |" + CloseMoleculeCommand + " " + name + "|");

	    closeMoleculeMenu.add(menuItem);
	}
    }

    /** Update the save molecule menu. */
    public void updateSaveMoleculeMenu(){
	int moleculeCount = moleculeRenderer.getMoleculeCount();
	for(int i = 0; i < moleculeCount; i++){
	    Molecule molecule = moleculeRenderer.getMolecule(i);
	    String name = molecule.getName();
	    MenuItem menuItem = createMenuItem(name);
	    menuItem.setActionCommand(SaveMoleculeCommand + " " + name);

	    //System.out.println("adding command |" + SaveMoleculeCommand + " " + name + "|");

	    saveMoleculeMenu.add(menuItem);
	}
    }

    /** Update the contents of the select menu. */
    public void updateSelectMenu(){
	ResidueIterator iterator = moleculeRenderer.getResidueIterator();

	while(iterator.hasMoreElements()){
	    Residue residue = iterator.getNextResidue();
	    if(residue.isStandardAminoAcid() == false &&
	       residue.isIon() == false &&
	       residue.isSolvent() == false){
		String residueName = residue.getName();
		int number = residue.getNumber();
		Chain chain = residue.getParent();
		String chainName = chain.getName();

		String selectionExpression = chainName + ":" + number;

		MenuItem menuItem =
		    createMenuItem(residueName + " " + selectionExpression);

				// replace spaces with ^
		selectionExpression = selectionExpression.replace(' ', '^');

		menuItem.setActionCommand(SelectCommand + " "
					  + selectionExpression);

		ligandMenu.add(menuItem);
	    }
	}
    }

    /** Save the small molecules. */
    public void saveMolecules(){
	int moleculeCount = moleculeRenderer.getMoleculeCount();
	//System.out.println("saveMolecules");

	for(int m = 0; m < moleculeCount; m++){
	    Molecule molecule = moleculeRenderer.getMolecule(m);
	    
	    saveMolecule(molecule);
	}
    }

    /** Return scripting description of the view. */
    public String getView(){
	String viewString = "matrix\n";
	Renderer r = getMoleculeRenderer().renderer;
	Matrix m = r.rotationMatrix;
	viewString += " " + m.x00 + " " + m.x01 + " " + m.x02 + " " + m.x03 + "\n";
	viewString += " " + m.x10 + " " + m.x11 + " " + m.x12 + " " + m.x13 + "\n";
	viewString += " " + m.x20 + " " + m.x21 + " " + m.x22 + " " + m.x23 + "\n";
	viewString += " " + m.x30 + " " + m.x31 + " " + m.x32 + " " + m.x33 + "\n";
	viewString += ";";

	Point3d center = r.getCenter();

	viewString +=
	    "center " + center.getX() + " " +
	    center.getY() + " " + center.getZ() + ";";

	viewString += "radius " + (r.width/r.zoom) + ";";

	viewString += "clip " + r.front + " " + r.back + ";";

	return viewString;
    }

    /** Save this molecule. */
    public void saveMolecule(Molecule molecule){
	String name = molecule.getFilename();
	//System.out.println("in saveMolecule " + name);

	if(name == null) return;

	FILE output = FILE.write(name);

	if(output == null){
	    System.out.println("saveMolecule: couldn't open " + name);
	    return;
	}

	MoleculeIO.write(molecule, output);

	output.close();
    }

    public void saveScript(String scriptFile){
	FILE save = FILE.write(scriptFile);

	StringBuffer commands = moleculeRenderer.commandLog;

	commands.append("/* Set the current view. */\n");

	commands.append(getView());
	
	int len = commands.length();

	for(int i = 0; i < len; i++){
	    char c = commands.charAt(i);
	    save.print(c);
	    if(c == ';'){
		save.print('\n');
	    }
	}
	
	save.close();
    }

    /** Save the whole state. */
    public void saveAndExit(){
	//saveScript("startup.script");

	System.exit(0);
    }


    Format hexFormat = new Format("0x%06x");

    private transient Frame colorChooserFrame = null;
    private transient Dialog colorChooserDialog = null;
    private transient ColorChooser colorChooser = null;

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
	
	if(false){
	    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	    
	    // change the screen height to reflect possible toolbar
	    screenSize.height -= 54;
	    
	    colorChooserDialog.pack();
	    Dimension chooserSize = colorChooserDialog.size();
	    
	    // center chooser on the passed location
	    x -= (int)(chooserSize.width * 0.5);
	    y -= (int)(chooserSize.height * 0.5);
	    
	    // shuffle coords to stop dialog being off screen.
	    
	    if(x + chooserSize.width > screenSize.width){
		x = screenSize.width - chooserSize.width;
	    }else if(x < 0){
		x = 0;
	    }
	    
	    if(y + chooserSize.height > screenSize.height){
		y = screenSize.height - chooserSize.height;
	    }else if(y < 0){
	    y = 0;
	    }
	    
	    colorChooserDialog.setLocation(x, y);
	    colorChooserDialog.show();
	}

	if(colorChooser.accept){
	    String s = hexFormat.format(colorChooser.rgb & 0xffffff);
	    return s;
	}else{
	    return null;
	}
    }

    /**
     * Show the dialog at the position.
     */
    public static void showAt(Dialog d, int x, int y){
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
}
