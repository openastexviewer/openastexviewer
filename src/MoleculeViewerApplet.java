/* Copyright Astex Technology Ltd. 1999 */

/*
 * 17-11-99 mjh
 *	created
 */
import astex.*;
import astex.parser.*;

import java.applet.*;
import java.awt.*;
import java.net.*;
import java.io.*;

import astex.splitter.*;
import astex.thinlet.*;
import thinlet.*;

/**
 * An applet container for the MoleculeViewer.
 */
public class MoleculeViewerApplet extends Applet implements Runnable{
    /** The MoleculeViewer. */
    public MoleculeViewer moleculeViewer;

    /** Should we output debugging info. */
    private boolean debug = false;

    /** Have we been initialised. */
    private boolean initialised = false;

    /** What stage of initialisation are we at. */
    private int initStep = 0;

    /** The thread that will drive the progress bar. */
    private Thread prepareThread = null;

    public MoleculeViewerApplet(){
        super();
        setBackground(Color.white);
    }

    public void init(){

        //print.f("hello from init thread="+ Thread.currentThread());
        //super.init();

        // if we display the splash screen the applet gets init'ed
        // before it really is inited. This breaks every single
        // application that does an onload() initialisation from
        // javascript
        // set splashscreen to something to initialise the splashscreen
        // at startup
	if(getParameter("splashscreen") == null){
            run();
	    //initialiseApplet();
	    //moleculeViewer.ready = true;
	}else{
	    prepareThread = new Thread(this);
            //print.f("hello from init prepare="+ prepareThread);
	    prepareThread.start();
	}
    }

    private void reportProgress(){
        initStep++;
        repaint(0);
        getToolkit().sync();
        //print.f("thread=" + Thread.currentThread() + " " + initStep);
        try {
            Thread.currentThread().sleep(20);
        }catch(Exception e){
        }
    }

    public void publicInit(){

        if(getParameter("appletui") != null){
            try {
                setLayout(new SplitterLayout(SplitterLayout.HORIZONTAL));

                add("3", moleculeViewer);
                
                String ui = new URL(getDocumentBase(), getParameter("appletui")).toString();

                ThinletUI tui = new ThinletUI(moleculeViewer, ui);

                SplitterBar sb = new SplitterBar();
                
                add(sb);
                
                add("1", tui);

                MoleculeRenderer moleculeRenderer = moleculeViewer.getMoleculeRenderer();

                moleculeRenderer.removeMoleculeRendererListener(tui);
                moleculeRenderer.renderer.removeRendererEventListener(tui);
            }catch(Exception e){
                e.printStackTrace();
            }
        }else{
            setLayout(new BorderLayout());

            //print.f("addLayout");

            add(moleculeViewer, BorderLayout.CENTER);
        }

        layout();
    }

    public void run(){
        reportProgress();

        //print.f("hello from run thread="+ Thread.currentThread());
	moleculeViewer = new MoleculeViewer();

	moleculeViewer.ready = false;

        reportProgress();

	String host = getCodeBase().getHost();
	
	System.out.println("host         = [" + host + "]");
	System.out.println("documentbase = [" + getDocumentBase() + "]");
	System.out.println("codebase     = [" + getCodeBase() + "]");

        FILE.setDebug(false);
	
	FILE.setDocumentBase(getDocumentBase());
	FILE.setCodeBase(getCodeBase());
	Texture.setDocumentBase(getDocumentBase());
	
        if(getDocumentBase().toString().startsWith("file")){
            // we seem to be running off a file system
            System.out.println("filesystem   = [true]");
            FILE.setTryFiles(true);
        }

        reportProgress();

	moleculeViewer.setUsePopupMenu(true);
	moleculeViewer.createMenuBar();
	moleculeViewer.getMoleculeRenderer().renderer.setColor(0x00ff00);

        reportProgress();

	initialiseApplet();

        reportProgress();

        try {
            publicInit();
        }catch(Exception e){
            print.f("some terrible error occurred loading user extension");
            e.printStackTrace();
        }

	moleculeViewer.ready = true;

        repaint();
        moleculeViewer.repaint();
    }

    public void update(Graphics g){
        //print.f("hello in update");
        paint(g);
	//super.paintComponents(g);
    }

    public void paint(Graphics g){
        if(moleculeViewer != null && moleculeViewer.ready){
            //print.f("paintComponents");
            super.paintComponents(g);
        }else{
            paintSplashScreen(g);
        }
    }

    private static final int progressHeight = 12;
    private static final int barWidth       = 280;
    private static final int maxProgress    = 15;

    private int fontHeight = 0;

    private Image splashImage = null;
    private int splashWidth   = -1;
    private int splashHeight  = -1;

    private void paintSplashScreen(Graphics g){
        //print.f("hello in paintSplashScreen " +initStep);
        //print.f("g " + g);

        int width = size().width;
        int height = size().height;
        int midx = width/2;
        int midy = height/2;
        int halfWidth = Math.min(barWidth/2, midx - 10);

        if(splashImage == null){
            //g.setColor(Color.white);
            //g.fillRect(0, 0, width, height);
            
            splashImage = Texture.loadImage("splash.gif");
            if (splashImage != null){
                MediaTracker mediatracker = new MediaTracker(this);
                mediatracker.addImage(splashImage, 1);
                try {
                    mediatracker.waitForID(1, 5000);
                } catch (InterruptedException ie) { }
            }else{
                return;
            }
            //print.f("#######splashImage " + splashImage);
            splashWidth = splashImage.getWidth(null);
            splashHeight = splashImage.getHeight(null);

            //print.f("width " + splashWidth);
            //print.f("height " + splashHeight);

        }

        if(splashImage != null){
            g.drawImage(splashImage,
                        midx - splashWidth/2,
                        midy - splashHeight/2,
                        this);
        }

        //print.f("moleculeViewer.ready " + moleculeViewer.ready);

        //fontHeight = getFontMetrics(getFont()).getHeight();
        
        //int fontHeight2 = (int)(fontHeight * 1.5);
        int spacing = 4;
            
        int frac = Math.min(maxProgress, initStep);
            
        // background
        g.setColor(Color.lightGray);
        g.fillRect(midx - halfWidth,
                   midy - progressHeight/2,
                   (int)(2 * halfWidth * (double)frac / maxProgress),
                   progressHeight);

        g.drawRect(0, 0, width - 1, height - 1);

        g.setColor(Color.black);
        g.drawRect(midx - halfWidth,
                   midy - progressHeight/2,
                   2 * halfWidth,
                   progressHeight);
        
        /*
          if(false){
            drawCenteredString(g, "Powered by AstexViewer",
                               Color.black, midx, midy - (fontHeight2 + spacing));
            drawCenteredString(g, "Version " + Version.getVersion(),
                               Color.black, midx, midy - spacing);
            drawCenteredString(g, "Copyright (C) Astex Therapeutics Ltd., 1999-2005",
                               Color.black, midx,
                               midy + progressHeight + (fontHeight2 + spacing));
            drawCenteredString(g, "http://www.astex-therapeutics.com/AstexViewer",
                               Color.black, midx,
                               midy + progressHeight + (2* fontHeight2 + spacing));
        }
        */
    }

    private void drawCenteredString(Graphics g, String s, Color c, int x, int y){
        Font f = getFont();
        int width = getFontMetrics(f).stringWidth(s);

        g.setColor(c);
        g.drawString(s, x - width/2, y - fontHeight/2);
    }

    /** Initialise the applet. */
    public void initialiseApplet(){

	// stop the applet trying to find resources
	// on the local file system.

	FILE.setTryFiles(false);
	
	String arraycopy = getParameter("arraycopy");
	if(arraycopy != null && arraycopy.equals("true")){
	    moleculeViewer.setArrayCopy(true);
	}
	
	//System.out.println("**** codeBase " + getCodeBase());
	
	DynamicArray moleculeNames = getParameterList("molecule");
	
	for(int i = 0; i < moleculeNames.size(); i++){
	    String moleculeName = (String)moleculeNames.get(i);

	    //FILE file = FILE.open(moleculeName);
	    Molecule molecule = MoleculeIO.read(moleculeName);
	    moleculeViewer.addMolecule(molecule);
	}

        reportProgress();
	
	DynamicArray mapNames = getParameterList("map");
	
	for(int i = 0; i < mapNames.size(); i++){
	    String mapName = (String)mapNames.get(i);
	    
	    Map map = Map.create();
	    //System.out.println("map name is " + mapName);
	    map.setFile(mapName);
	    moleculeViewer.addMap(map);
	}

        reportProgress();
	
	String centerString = getParameter("center");
	
	MoleculeRenderer moleculeRenderer =
	    moleculeViewer.getMoleculeRenderer();
	
	if(centerString != null){
	    String words[] = FILE.split(centerString);
	    // its a x y z specification
	    if(words.length == 3){
		double x = FILE.readDouble(words[0]);
		double y = FILE.readDouble(words[1]);
		double z = FILE.readDouble(words[2]);
		moleculeRenderer.setCenter(x, y, z);
	    }else{
		// its an atom selection
		DynamicArray centerSelection =
		    moleculeRenderer.getAtomsInSelection(centerString);
		
		moleculeRenderer.setCenter(centerSelection);
	    }
	}

        reportProgress();
	
	String clipString = getParameter("clip");
	
	if(clipString != null){
	    double clip = FILE.readDouble(clipString);
	    moleculeRenderer.setClip(clip);
	}

        reportProgress();
	
	String wideBondsString = getParameter("wide");
	
	if(wideBondsString != null){
	    //System.out.println("wide was set");
	    moleculeRenderer.resetWideBonds();
	    
	    DynamicArray wideBondsSelection =
		moleculeRenderer.getAtomsInSelection(wideBondsString);
	    
	    int atomCount = wideBondsSelection.size();
	    //System.out.println("atomCount " + atomCount);
	    
	    for(int a = 0; a < atomCount; a++){
		Atom atom = (Atom)wideBondsSelection.get(a);
		int bondCount = atom.getBondCount();
		for(int b = 0; b < bondCount; b++){
		    Bond bond = atom.getBond(b);
		    bond.setWideBond(true);
		}
	    }
	}

        reportProgress();
	
	String bumpString = getParameter("bump");
	
	if(bumpString != null){
	    // switch on bump checking.
	    moleculeRenderer.setDisplayBumps(true);
	    
	    DynamicArray bumpAtoms =
		moleculeRenderer.getAtomsInSelection(bumpString);
	    
	    moleculeRenderer.generateBumps(bumpAtoms);
	}

        reportProgress();
	
	String scriptFile = getParameter("scriptFile");

	//System.out.println("executing script from " + scriptFile);

	if(scriptFile != null){
	    executeFile(scriptFile);
	}

        reportProgress();

	String scriptString = getParameter("script");
	
	if(scriptString != null){
	    //System.out.println("about to execute start script");
	    //System.out.println(scriptString);
	    
	    moleculeRenderer.execute(scriptString);
	}

        reportProgress();
    }

    /** Destroy the applet. */
    public void destroy(){
	super.destroy();
	//System.out.println("MoleculeViewerApplet.destroy()");

	if(moleculeViewer != null){
	    //moleculeViewer.finalize();
	    //moleculeViewer.getMoleculeRenderer().moleculeViewer = null;
	    moleculeViewer = null;
	    System.gc();	
	    System.runFinalization();
	}
    }

    public void finalize(){
	//System.out.println("MoleculeViewerApplet.finalize()");
    }

    public void stop(){
	super.stop();
	System.out.println("MoleculeViewerApplet.stop()");
        removeAll();
        moleculeViewer = null;
    }

    public void start(){
	super.start();
	//System.out.println("MoleculeViewerApplet.start()");
    }

    /** Return a DynamicArray of parameters that begin with the String. */
    private DynamicArray getParameterList(String prefix){
	DynamicArray parameters = new DynamicArray();

	String value = getParameter(prefix);

	if(value != null){
	    parameters.add(value);
	}

	for(int i = 1; /* nothing */; i++){
	    value = getParameter(prefix + i);

	    if(value == null){
		break;
	    }else{
		parameters.add(value);
	    }
	}

	return parameters;
    }

    /*
     * Execute a set of commands.
     */
    public synchronized void execute(String command){
	MoleculeRenderer renderer = moleculeViewer.getMoleculeRenderer();

	if(debug){
	    System.out.println("command: " + command);
	}

	renderer.execute(command);

	moleculeViewer.repaint();
    }

    /** Open a file and execute the script in it. */
    public void executeFile(String scriptFile){
	MoleculeRenderer renderer = moleculeViewer.getMoleculeRenderer();

	FILE f = FILE.open(scriptFile);
	
	StringBuffer sb = new StringBuffer(128);
	int c = 0;

	while((c = f.read()) != FILE.EOF){
	    sb.append((char)c);
	}
	
	execute(sb.toString());

	moleculeViewer.repaint();
    }

    /** Turn on debugging. */
    public void debugOn(){
	debug = true;
    }

    /** Turn on debugging. */
    public void debugOff(){
	debug = false;
    }

    /**
     * Return the contents of the specified url.
     * This provides some functionality around the parser
     * to allow it to return a string.
     *
     * If this is used in an applet, it will only
     * be possible to open urls from the server that
     * the applet came from.
     */
    public String fetch(String urlName){
	if(debug){
	    System.out.println("about to call fetch");
	    System.out.println("url="+urlName);
	}

	MoleculeRenderer renderer = moleculeViewer.getMoleculeRenderer();

	renderer.execute("fetch '" + urlName + "';");

	if(debug){
	    System.out.println("back from fetch");
	    System.out.println("data="+parser.getUrlContents());
	}

	return parser.getUrlContents();
    }

    /** Fetch using method=POST. */
    private String fetchPost(String urlString){

        //System.out.println("URLFetchPost");

        StringBuffer contents = new StringBuffer(2048);

        int questionPos = urlString.indexOf('?');

        String urlSection = urlString;
        String parameterSection = null;

        if(questionPos != -1){
            urlSection = urlString.substring(0, questionPos);
            parameterSection = urlString.substring(questionPos + 1,
						   urlString.length());
        }

        //System.out.println(urlSection);
        //System.out.println(parameterSection);

        try {
            
            URL url = new URL(urlSection);

            URLConnection con = url.openConnection();
            //System.out.println("Received a : " + con.getClass().getName());

            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);

            con.setRequestProperty("CONTENT_LENGTH", 
				   "" + parameterSection.length()); 

            OutputStream os = con.getOutputStream();

            OutputStreamWriter osw = new OutputStreamWriter(os);
            osw.write(parameterSection);
            osw.flush();
            osw.close();
            
            InputStream is = con.getInputStream();
            
            // any response?
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            String lineSeparator =
                java.lang.System.getProperty("line.separator");

            while ( (line = br.readLine()) != null) {
                contents.append(line);
                contents.append(lineSeparator);
            }
            
            br.close();

        } catch(Throwable t) {
            System.out.println("URLFetch - fetchPost(): " +
			       "error opening url: " + urlString);
            t.printStackTrace();
        }

        return contents.toString();
    }

    /** Fetch using method=GET. */
    private String fetchNormal(String urlString){

	//System.out.println("URLFetch");

        StringBuffer contents = new StringBuffer(2048);

        try {

            URL url = new URL(urlString);
            DataInputStream dis = new DataInputStream(url.openStream());
            String line;
            String lineSeparator =
                java.lang.System.getProperty("line.separator");

            while((line = dis.readLine()) != null){
                System.out.println("|" + line + "|");
                contents.append(line);
                contents.append(lineSeparator);
            }
            
            dis.close();
            
        }catch(Exception e){
            System.out.println("URLFetch - fetchNormal(): " +
			       "error opening url: " + urlString);
            System.out.println("" + e);
        }

        return contents.toString();
    }

    /**
     * Safe url fetcher.
     * Will use POST if URL data is too long.
     */
    public String fetchSafe(String urlString){

        String output = null;

        if(urlString.length() > 2048){
            output = fetchPost(urlString);
        } else {
            output = fetchNormal(urlString);
        }

        return output;
    }


    /** Return scripting language to restore the current view. */
    public String getView(){
	return moleculeViewer.getView();
    }

    public String getSelection(){
	MoleculeRenderer renderer = moleculeViewer.getMoleculeRenderer();
	StringBuffer selection = new StringBuffer();

	int molCount = renderer.getMoleculeCount();

	for(int m = 0; m < molCount; m++){
	    boolean alreadySelected = false;

	    Molecule mol = renderer.getMolecule(m);

	    int atomCount = mol.getAtomCount();
	    for(int a = 0; a < atomCount; a++){
		Atom atom = mol.getAtom(a);

		if(atom.isSelected()){
		    if(!alreadySelected){
			if(selection.length() > 0){
			    selection.append("|");
			}
			selection.append(mol.getName());
			alreadySelected = true;
		    }

		    selection.append("," + atom.getId());
		}
	    }
	}

	return selection.toString();
    }

    public String getCoordinates(){
	MoleculeRenderer renderer = moleculeViewer.getMoleculeRenderer();
	StringBuffer buf = new StringBuffer();
        AtomIterator iterator = renderer.getAtomIterator();
        
        while(iterator.hasMoreElements()){
            Atom atom = iterator.getNextAtom();
            if(atom.isSelected()){
                if(buf.length() > 0){
                    buf.append("|");
                }
                buf.append("" + atom.getX());
                buf.append("," + atom.getY());
                buf.append("," + atom.getZ());
            }
        }

        return buf.toString();
    }

    public String getSelectedAtoms(){
	MoleculeRenderer renderer = moleculeViewer.getMoleculeRenderer();
	StringBuffer buf = new StringBuffer(2048);
        AtomIterator iterator = renderer.getAtomIterator();
        
        boolean first = true;

        while(iterator.hasMoreElements()){
            Atom atom = iterator.getNextAtom();
            if(atom.isSelected()){
                if(!first){
                    buf.append(",");
                }

                Residue res = atom.getResidue();
                Chain chain = res.getParent();
                Molecule mol = chain.getParent();

                buf.append(mol.getName()); buf.append("|");
                buf.append(chain.getName()); buf.append("|");
                buf.append(res.getNumber()); buf.append("|");
                buf.append(res.getInsertionCode()); buf.append("|");
                buf.append(res.getName()); buf.append("|");
                buf.append(atom.getAtomSymbol()); buf.append("|");
                buf.append(atom.getAttribute(Atom.X)); buf.append("|");
                buf.append(atom.getAttribute(Atom.Y)); buf.append("|");
                buf.append(atom.getAttribute(Atom.Z)); buf.append("|");
                buf.append(atom.getAttribute(Atom.B)); buf.append("|");
                buf.append(atom.getAttribute(Atom.O)); buf.append("|");
                buf.append(atom.getAtomLabel());

                first = false;
            }
        }

        return buf.toString();
    }


    Format hexFormat = new Format("0x%06x");

    private Frame colorChooserFrame = null;
    private Dialog colorChooserDialog = null;
    private astex.ColorChooser colorChooser = null;

    /**
     * Instruct AstexViewer to display its color gadget
     * so that something can use it for picking colours.
     */
    public String getColor(int x, int y){

	return moleculeViewer.getColor(x, y);

	/*
	Log.info("start of color Chooser");

	if(colorChooserDialog == null){
	    if(colorChooserFrame == null){
		colorChooserFrame = new Frame();
	    }
	    colorChooserDialog = new Dialog(colorChooserFrame, true);

	    colorChooser = new ColorChooser(colorChooserDialog);
	    colorChooserDialog.add(colorChooser);
	}

	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

	colorChooserDialog.pack();
	Dimension chooserSize = colorChooserDialog.size();

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

	Log.info("show color chooser");

	if(colorChooser.accept){
	    String s = hexFormat.format(colorChooser.rgb & 0xffffff);
	    Log.info("user hit ok: color is " + s);
	    return s;
	}else{
	    Log.info("user hit cancel: color is null");
	    return null;
	}

	*/
    }
}
