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
import astex.splitter.*;
import astex.generic.*;
import thinlet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class ThinletUI extends Thinlet implements WindowListener,
                                                  MoleculeRendererListener,
                                                  RendererEventListener {

    public static Hashtable userinterfaces = new Hashtable();

    public static void handleCommand(MoleculeViewer mv,
                                     MoleculeRenderer mr,
                                     Arguments args){
        String xml = args.getString("-xml", null);

        DialogLauncher dialogLauncher = (DialogLauncher)userinterfaces.get(xml);

        if(dialogLauncher == null){
            ThinletUI tui = new ThinletUI(mv, xml);
            dialogLauncher = tui.displayThinlet(mv, args);
            if(dialogLauncher != null){
                userinterfaces.put(xml, dialogLauncher);
            }
        }
        
        dialogLauncher.setVisible(true);
    }

    public MoleculeViewer moleculeViewer     = null;
    public MoleculeRenderer moleculeRenderer = null;

    Hashtable initialised = new Hashtable();

    /** Main entry point for scripting execution via thinlet. */
    public void execute(Object init, Object component){

        if(initialised.get(init) == null){
            initialised.put(init, init);

            String text = getString(init, "text");

            text = preprocess(init, text);
            execute(text);
        }

        execute(component);
    }

    public Object console = null;

    public long then = 0;

    public void startTimer(){
        then = System.currentTimeMillis();

        if(console == null){
            console = find("console");
            System.out.print("Console is " + console);
        }

        setString(console, "text", "");
    }

    public void stopTimer(String s){

        long now  = System.currentTimeMillis();

        String current = getString(console, "text");

        current += s + " " + (now - then) + " ms\n";

        setString(console, "text", current);

        then = now;
    }

    /** Main entry point for scripting execution via thinlet. */
    public void execute(Object component){
        String init = (String)getProperty(component, "init");

        if(init != null && initialised.get(init) == null){
            initialised.put(init, init);

            init = preprocess(component, init);

            execute(init);
        }

        String commands[] = { "precommand", "command", "postcommand" };

        for(int i = 0; i < commands.length; i++){
            String command = (String)getProperty(component, commands[i]);

            //System.out.println("class " + getClass(component));

            //System.out.println(commands[i] + " " + command);

            if(command == null && getClass(component) != "table"){
                if(getClass(component) == "checkbox"){
                    boolean selected = getBoolean(component, "selected");
                    if(selected){
                        command = (String)getProperty(component, "commandon");
                    }else{
                        command = (String)getProperty(component, "commandoff");
                    }
                }else if(getClass(component) == "tabbedpane"){
                    component = getSelectedItem(component);
                    
                    if(getProperty(component, "command") != null){
                        execute(component);
                    }
                    
                    return;
                    // this next block was commented out - why?
                    // must have caused some other effect, restrict it to a textarea
                }else if(getClass(component) == "textarea"){
                    command = getString(component, "text");
                }
            }else if(command != null && (getClass(component) == "table" || getClass(component) == "tree")){

                if(i == 1){
                    //System.out.println("doing row part command = " + command);
                
                    // only preprocess the pertable command
                    Object rows[] = getSelectedItems(component);
                
                    String wholeCommand = "";
                    
                    for(int r = 0; r < rows.length; r++){
                        //System.out.println("row["+r+"] " + command);
                        
                        wholeCommand += preprocess(rows[r], command);
                    }
                    // do this per row...
                    
                    command = wholeCommand;
                }
            }

            if(command != null){
                //System.out.println("command:           " + command);

                command = preprocess(component, command);

                //System.out.println("processed command: " + command);
                execute(command);
            }else{
                if(i == 1){
                    Log.error("no command or text to execute");
                }
            }
        }
    }

    /** Execute one component but using data from another. */
    public void executeUsing(Object component, Object data){
        String init = (String)getProperty(component, "init");

        if(init != null){
            init = preprocess(data, init);
        }

        if(init != null && initialised.get(init) == null){
            initialised.put(init, init);

            execute(init);
        }

        String commands[] = { "precommand", "command", "postcommand" };

        for(int i = 0; i < commands.length; i++){
            String command = (String)getProperty(component, commands[i]);

            System.out.println(commands[i] + " " + command);

            if(command == null && i == 1){
                command = getString(component, "text");
            }

            if(command != null){
                command = preprocess(data, command);
                
                execute(command);
            }else{
                Log.error("no command or text to execute");
            }
        }
    }

    /** Actually execute a command and repaint. */
    private void execute(String command){
        if(command.endsWith(";") == false){
            command += ";";
        }

        moleculeRenderer.execute(command);

        moleculeViewer.dirtyRepaint();
    }

    public void clear(Object component){
        setString(component, "text", "");
    }

    /**
     * get the cell from the selected row that has
     * the specified columnName property.
     */
    public String getCellValueWithName(Object table, String name){
        Object row = null;

        if(getClass(table).equals("row")){
            // it was a row really...
            //System.out.println("getCellValueWithName called with row");
            row = table;
            table = getParent(row);
        }

        Object header = getWidget(table, "header");

        int columnId = -1;

        for(int i = 0; i < getCount(header); i++){
            Object column = getItem(header, i);
            String text = (String)getString(column, "text");
            if(text != null && text.equalsIgnoreCase(name)){
                columnId = i;
                break;
            }
        }

        if(columnId == -1){
            print.f("ERROR: table has no column called " + name);
        }

        if(row == null){
            // the original object was a table so we need to get the row
            // XXX will this ever actually get called this way?
            row = getSelectedItem(table);
        }

        Object cell = getItem(row, columnId);

        return getString(cell, "text");
    }

    public String getValue(Object component, String s){
        //System.out.println("getValue " + getClass(component) + " " + s);

        if(getClass(component).equals("table") || getClass(component).equals("row")){

            String field = s.substring(1);

            String value = getCellValueWithName(component, field);

            return value;
        }

        if(s.equals("$o")){
            boolean selected = getBoolean(component, "selected");
            return selected ? "on" : "off";
        }

        if(s.equals("$b")){
            boolean selected = getBoolean(component, "selected");
            return selected ? "true" : "false";
        }

        if(s.equals("$t")){
            return getString(component, "text");
        }

        if(s.equals("$d")){
            return "" + getInteger(component, "value");
        }

        if(s.equals("$V")){
            return "" + getInteger(component, "value") * 0.1;
        }

        if(s.equals("$v")){
            return "" + getInteger(component, "value") * 0.01;
        }

        if(s.equals("$c")){
            Color color = getColor(component, "background");
            return Color32.format(color.getRGB());
        }

        if(s.equals("$C")){
            Color color = getColor(component, "background");
            return Color32.formatNoQuotes(color.getRGB());
        }

        if(s.equals("$f")){
            Color color = getColor(component, "foreground");
            return Color32.formatNoQuotes(color.getRGB());
        }

        if(s.equals("$n")){
            if(getClass(component) == "combobox"){
                component = getSelectedItem(component);
            }
            String name = getString(component, "name");
            return name;
        }

        if(s.equals("$h")){
            double hsv[] = { 0.0, 1.0, 1.0 };
            int value = getInteger(component, "value");
            hsv[0] = (double)value;
	    int c = Color32.hsv2packed(hsv);
            return Color32.format(c);
        }

        String property = (String)getProperty(component, s.substring(1));

        if(property != null){
            return property;
        }

        System.out.println("invalid attribute " + s);

        return null;
    }

    public String preprocess(Object component, String origCommand){
        return preprocess(component, origCommand, true);
    }

    public String preprocess(Object component, String origCommand, boolean replacePipe){
        //System.out.println("initial command |" + command + "|");
        String command = origCommand;

        if(replacePipe){
            command = origCommand.replace('|', ';');
        }

        StringBuffer newCommand = new StringBuffer();

        try {
            // now do the values from other objects.
            int len = command.length();
            for(int i = 0; i < len; i++){
                if(command.charAt(i) == '$'){
                    Object comp = component;
                    String attribute = null;
                    
                    if(command.charAt(++i) == '{'){
                        StringBuffer sb = new StringBuffer();
                        while(command.charAt(++i) != '}'){
                            sb.append(command.charAt(i));
                        }

                        String bits[] = FILE.split(sb.toString(), ".");
                        if(bits.length != 2){
                            System.out.println("no . character");
                            System.out.println(origCommand);
                        }else{
                            // this should cache the object name...
                            comp = findComponent(bits[0]);
                            attribute = "$" + bits[1];
                            if(comp == null){
                                System.out.println("couldn't find object " + bits[0]);
                            }
                        }
                    }else{
                        attribute = "$" + command.charAt(i);
                    }

                    String value = null;

                    if(comp == getParent(component)){
                        // if the named table was our parent then
                        // we just go for the row instead
                        value = getValue(component, attribute);
                    }else if("tree".equals(getClass(comp))){
                        value = getValue(getSelectedItem(comp), attribute);
                    }else{
                        value = getValue(comp, attribute);
                    }

                    if(value == null){
                        // just append what was there...
                        newCommand.append(attribute);
                    }else{
                        newCommand.append(value);
                    }
                }else{
                    newCommand.append(command.charAt(i));
                }
            }
        }catch(Exception e){
            System.out.println("error processing command: " + origCommand);
            System.out.println("exception " + e);
            return null;
        }
            
        //System.out.println("final command   |" + newCommand + "|");

        return newCommand.toString();
    }


    public void windowClosing(WindowEvent e){
	close(e.getWindow());
    }

    public void windowActivated(WindowEvent e){ }
    public void windowClosed(WindowEvent e){ }
    public void windowDeactivated(WindowEvent e){ }
    public void windowDeiconified(WindowEvent e){ }
    public void windowIconified(WindowEvent e){ }
    public void windowOpened(WindowEvent e){ }

    private void close(Window window){
        window.hide();
        window.dispose();
    }

    public boolean destroy(){
        return false;
    }

    public void setContent(String xml){
        try{
            add(parse(xml));
        }catch(Exception e){
            System.out.println("Exception: " + e);
        }
    }

    public DialogLauncher displayThinlet(MoleculeViewer mv,
                                         Arguments args){
        String title     = args.getString("-title", args.getString("-xml", "..."));
        int width        = args.getInteger("-width", 400);
        int height       = args.getInteger("-height", 400);
        int x            = args.getInteger("-x", 806);
        int y            = args.getInteger("-y", 0);
        boolean internal = args.getBoolean("-internal", false);
        
        if(internal){
            Container parent = mv.getParent();
            System.out.println("parent " + parent);

            parent.removeAll();

            if(!(parent.getLayout() instanceof SplitterLayout)){
                System.out.println("not splitter " + parent);
                
                parent.setLayout(new SplitterLayout(SplitterLayout.HORIZONTAL));
                
                parent.add("3", mv);
                
                mv.setSize((int)(mv.getSize().width * 0.75), mv.getSize().height);

                SplitterBar sb = new SplitterBar();
                
                parent.add(sb);
                
                //parent.add("1", new ComponentLauncher(this));
                parent.add("1", this);
            }else{
                parent.setLayout(new BorderLayout());
                parent.add(mv, BorderLayout.CENTER);
            }

            parent.layout();
        }else{
            DialogLauncher dialogLauncher =
                new DialogLauncher(Util.getFrame(mv), title,
                                   this, x, y, width, height);

            return dialogLauncher;
        }

        return null;
    }

    public ThinletUI(MoleculeViewer mv, String xml){
        this(mv);

        setContent(xml);

        //initialise();
    }

    public ThinletUI(MoleculeViewer mv){
        System.out.println("Thinlet GUI toolkit - www.thinlet.com");
        System.out.println("Copyright (C) 2002-2003 Robert Bajzat (robert.bajzat@thinlet.com)");

        moleculeViewer = mv;
        moleculeRenderer = mv.getMoleculeRenderer();
        
        moleculeRenderer.addMoleculeRendererListener(this);
        moleculeRenderer.renderer.addRendererEventListener(this);

	setFont(new Font("Arial", Font.PLAIN, 12));
    }

    public String readTemplate(String xmlFile) {
        try {
            FILE f = FILE.open(xmlFile);

            InputStream fis = f.getInputStream();
            
            StringBuffer sb = new StringBuffer(1024);
            
            int c = 0;
            
            while((c = fis.read()) != -1){
                sb.append((char)c);
            }
            
            fis.close();

            return sb.toString();
        }catch(Exception e){
            print.f("exception opening template " + e);
            return null;
        }
    }

    public void initialiseObjects(Object objectComponent){
        if(objectComponent != null){
            int objectCount = moleculeRenderer.renderer.getGraphicalObjectCount();

            for(int o = 0; o < objectCount; o++){
                Tmesh tmesh = moleculeRenderer.renderer.getGraphicalObject(o);
                
                addObject(objectComponent, tmesh);
            }
        }
    }

    public void initialiseMolecules(Object molTree){
        int molCount = moleculeRenderer.getMoleculeCount();

        for(int m = 0; m < molCount; m++){
            Molecule mol = moleculeRenderer.getMolecule(m);

            addMolecule(molTree, mol);
        }
    }

    public void initialiseMaps(Object mapComponent){
        int mapCount = moleculeRenderer.getMapCount();

        for(int m = 0; m < mapCount; m++){
            astex.Map map = moleculeRenderer.getMap(m);

            addMap(mapComponent, map);
        }
    }

    public void initialiseFrontClip(Object fc){
        setString(fc, "text", new String(FILE.sprint("%.1f", moleculeRenderer.renderer.front)));
    }

    public void initialiseBackClip(Object fc){
        setString(fc, "text", new String(FILE.sprint("%.1f", moleculeRenderer.renderer.back)));
    }

    public void initialiseDistances(Object object){
        int distanceCount = moleculeRenderer.getDistanceCount();

        //print.f("object " + object);

        for(int d = 0; d < distanceCount; d++){
            Distance distance = (Distance)moleculeRenderer.getDistance(d);

            addDistance(object, distance);
        }
    }

    /** MoleculeRendererListener interface. */

    /** A molecule was added. */
    public void moleculeAdded(MoleculeRenderer renderer, Molecule molecule){
        //System.out.println("moleculeAdded " + molecule);

        Object moleculeTree = findComponent("molecule_tree");

        addMolecule(moleculeTree, molecule);

        if(molecule.getMoleculeType() != Molecule.SymmetryMolecule){
            Object resnode = findComponent("residuelist");
            Object atomnode = findComponent("atomlist");
            
            if(resnode != null){
                populateResidues(resnode, atomnode);
            }
        }
    }

    private void addMolecule(Object tree, Molecule mol){

        //System.out.println("moleculeTree " + tree);

        if(tree != null){
            Object node = createNode(mol.getName(), false, mol);

            int chainCount = mol.getChainCount();

            for(int c = 0; c < chainCount; c++){
                Chain chain = mol.getChain(c);

                String name = chain.getName();
                name.replace(' ', 'X');

                Object chainNode = createNode(name, false, chain);
                Object dummyNode = createNode("dummy", false, null);
                
                add(chainNode, dummyNode);
                add(node, chainNode);
            }
            
            add(tree, node);

            repaint();
        }else{
            print.f("couldn't find molecule tree");
        }
    }

    /** A molecule was removed. */
    public void moleculeRemoved(MoleculeRenderer renderer, Molecule molecule){

        removeMolecule(molecule);
    }

    private void removeMolecule(Molecule molecule){
        Object moleculeTree = find("molecule_tree");

        Object molNode = find(moleculeTree, molecule.getName());

        remove(molNode);

        if(molecule.getMoleculeType() != Molecule.SymmetryMolecule){
            Object resnode = findComponent("residuelist");
            Object atomnode = findComponent("atomlist");
            
            if(resnode != null){
                populateResidues(resnode, atomnode);
            }
        }
    }

    public void initialiseResidues(Object resnode, Object atomnode){
        removeAll(resnode);

        populateResidues(resnode, atomnode);
    }

    private void populateResidues(Object resnode, Object atomnode){
        removeAll(resnode);
        removeAll(atomnode);
        Hashtable resnames = new Hashtable();
        Hashtable atomnames = new Hashtable();

        for(int m = 0; m < moleculeRenderer.getMoleculeCount(); m++){
            Molecule mol = moleculeRenderer.getMolecule(m);
            for(int c = 0; c < mol.getChainCount(); c++){
                Chain chain = mol.getChain(c);
                for(int r = 0; r < chain.getResidueCount(); r++){
                    Residue res = chain.getResidue(r);
                    String resname = res.getName();
                    if(resnames.contains(resname) == false){
                        resnames.put(resname, resname);
                    }
                    for(int a = 0; a < res.getAtomCount(); a++){
                        Atom atom = res.getAtom(a);
                        String atomname = atom.getAtomLabel();
                        if(atomnames.contains(atomname) == false){
                            atomnames.put(atomname, atomname);
                        }
                    }
                }
            }
        }

        // residue names
        String names[] = new String[resnames.size()];
        int count = 0;

        Enumeration k = resnames.elements();

        while(k.hasMoreElements()){
            String name = (String)k.nextElement();
            names[count++] = name;
        }

        sort(names, count);

        //print.f("residue names " + resnames.size());

        char lastChar = 0;
        Object folder = null;

        for(int i = 0; i < count; i++){
            char c = names[i].charAt(0);
            if(c != lastChar){
                folder = create("node");
                setString(folder, "text", "" + c);
                setBoolean(folder, "expanded", false);

                add(resnode, folder);

                lastChar = c;
            }

            Object node = create("node");
            setString(node, "text", names[i]);
            putProperty(node, "selection", "name '" + names[i] + "'");
            add(folder, node);
        }

        // atom names

        count = 0;
        names = new String[atomnames.size()];
        k = atomnames.elements();

        while(k.hasMoreElements()){
            String name = (String)k.nextElement();
            names[count++] = name;
        }

        sort(names, count);

        //print.f("residue names " + resnames.size());

        lastChar = 0;
        folder = null;

        for(int i = 0; i < count; i++){
            char c = names[i].charAt(0);
            if(c != lastChar){
                folder = create("node");
                setString(folder, "text", "" + c);
                setBoolean(folder, "expanded", false);

                add(atomnode, folder);

                lastChar = c;
            }

            Object node = create("node");
            setString(node, "text", names[i]);
            putProperty(node, "selection", "atom '" + names[i] + "'");
            add(folder, node);
        }

    }

    public void sort(String a[], int n){
	for (int i = n; --i >= 0; ) {
            boolean flipped = false;
	    for (int j = 0; j < i; j++) {
		//if (a[j] > a[j+1]) {
		if (a[j].compareTo(a[j+1]) > 0){
		    String T = a[j];
		    a[j] = a[j+1];
		    a[j+1] = T;
		    flipped = true;
		}
	    }
	    if (!flipped) {
	        return;
	    }
        }
    }

    public void genericAdded(MoleculeRenderer renderer, Generic generic){

        System.out.println("generic added " + generic);

        if(generic instanceof Distance){
            Object list = findComponent("distance_list");

            addDistance(list, (Distance)generic);
        }
    }

    private void addDistance(Object list, Distance distance){
        
        if(list == null){
            return;
        }

        String name = distance.getString(Generic.Name, "generic");

        String itemString = "<item name=\"" + name + "\" text=\"" + name + "\"/>";

        Object item = safeParse(itemString);

        putProperty(item, "reference", distance);

        add(list, item);
    }

    public void editGeneric(Object list){
        //print.f("class " + getClass(list));

        Object distance = getSelectedItem(list);

        //print.f("list " + list);
        //print.f("distance " + distance);

        Object propertyPanel = findComponent("property_panel");

        //print.f("property_panel " + propertyPanel);

        removeAll(propertyPanel);

        Object ref = getProperty(distance, "reference");

        if(ref != null && ref instanceof GenericInterface){
            GenericInterface generic = (GenericInterface)ref;

            //print.f("generic " + generic);
            
            Enumeration properties = generic.getProperties();
            
            while(properties.hasMoreElements()){
                String property = (String)properties.nextElement();

                //print.f("property " + property);

                if(property.startsWith("__") &&
                   property.endsWith("__")){
                    //its private
                }else{
                    addPropertyEditor(propertyPanel, generic, property);
                }
            }
        }
    }

    public String generateDoubleEditor(String valString,
                                       String minString,
                                       String maxString,
                                       String stepString,
                                       String dpString){
        String editString = "<spinbox text=\"" + valString;

        if(minString != null){
            editString += "\" minimum=\"" + minString;
        }

        if(maxString != null){
            editString += "\" maximum=\"" + maxString;
        }

        editString += "\" decimals=\""+dpString +
            "\" step=\"" + stepString + "\" action=\"applyEdit(this)\" columns=\"6\"/>";

        return editString;
    }

    public String getColorString(int rgb){
        return "#" + Integer.toHexString(rgb|0xff000000).substring(2);
    }

    public String getColorString2(int rgb){
        return "0x" + Integer.toHexString(rgb|0xff000000).substring(2);
    }

    public void addPropertyEditor(Object panel, GenericInterface generic, String property){
        String labelString = "<label text=\"" + property + "\"/>";

        Object labelObject = safeParse(labelString);

        add(panel, labelObject);

        Object value = generic.get(property, "");

        String className = (String)generic.getClassname();
        String lookup = className + "." + property;

        String editString = Settings.getString("thinlet", lookup);

        if(editString != null){
            editString = Util.replace(editString, "%v", value.toString());
            editString = Util.replace(editString, "%e",  "action=\"applyEdit(this)\"");
        }else{
            if(value instanceof Color){
                Color c = (Color)value;
                String cs = getColorString(c.getRGB());
                editString = readTemplate("/astex/thinlet/colorbutton.xml.properties");
                editString = Util.replace(editString, "%c", cs);
            }else if(value instanceof Double){
                String minString = Settings.getString("thinlet", className + ".min", "-10000.0");
                String maxString = Settings.getString("thinlet", className + ".max", "10000.0");
                
                String stepString = Settings.getString("thinlet", className + ".step", "0.1");
                String dpString = Settings.getString("thinlet", className + ".decimals", "2");

                String valString = FILE.sprint("%.6g", ((Double)value).doubleValue());

                editString = generateDoubleEditor(valString, minString, maxString, stepString, dpString);
            }else if(value instanceof Integer){
                editString =
                    "<spinbox text=\"" + value.toString() +
                    "\" action=\"applyEdit(this)\" minimum=\"-100000\" maximum=\"100000\"/>";
            }else if(value instanceof Boolean){
                editString = "<checkbox selected=\"" + value.toString() + "\" action=\"applyEdit(this)\"/>";
            }else{
                editString = "<textfield text=\"" + value.toString() + "\" action=\"applyEdit(this)\"/>";
            }
        }

        Object editObject = safeParse(editString);

        add(panel, editObject);

        // button must be first object in colorbutton template master
        // panel
        if(value instanceof Color){
            editObject = getItem(editObject, 0);
        }

        putProperty(editObject, "reference", generic);
        putProperty(editObject, "propertyString", property);
    }

    public Point getPositionOnScreen(Object component){
        Point screen = getLocationOnScreen();
        Point pos = new Point(screen.x, screen.y);

        do {
            Rectangle bounds = getRectangle(component, "bounds");
            pos.x += bounds.x;
            pos.y += bounds.y;
            component = getParent(component);
        }while(component != getDesktop());

        return pos;
    }

    public void editObject(Object component){
        Point pos = getPositionOnScreen(component);
        Object parent = getParent(component);
        String name = getString(parent, "name");
        ObjectEditor.editObject(Util.getFrame(moleculeViewer),
                                "Edit object...", pos.x, pos.y, this, name);

    }

    /** Apply a color edit then execute the other component. */
    public void applyColorEdit(Object editor, Object component){
        applyColorEdit(editor);

        execute(component);
    }

    public void applyColorEdit(Object editor){
        Point pos = getPositionOnScreen(editor);

        Color color = ColorChooser.getcolor(Util.getFrame(moleculeViewer),
                                            "Edit colour...", pos.x, pos.y);

        if(color != null){
            setColor(editor, "background", color);

            String command = (String)getProperty(editor, "command");

            if(command != null){
                execute(editor);
            }else{
                applyEdit(editor);
            }
        }
    }

    public void applyEdit(Object editor){
        GenericInterface generic = (GenericInterface)getProperty(editor, "reference");

        if(generic == null){
            return;
        }
        String propertyName = (String)getProperty(editor, "propertyString");
        String value = getString(editor, "text");
        boolean checkbox = false;
        boolean boolValue = false;

        if(getClass(editor) == "checkbox"){
            checkbox = true;
            boolValue = getBoolean(editor, "selected");
        }

        String name = getString(editor, "name");

        if(name != null &&
           getString(editor, "name").equals("editor")){
            Object parent = getParent(editor);

            generic = (GenericInterface)getProperty(parent, "reference");
            propertyName = (String)getProperty(parent, "propertyString");
        }

        Object o = generic.get(propertyName, null);

        if(o == null){
            print.f("no such property " + propertyName + " on " + generic);
            return;
        }else{
            Object newValue = null;

            if(o instanceof Double){
                newValue = (Object)new Double(value);
            }else if(o instanceof Integer){
                newValue = (Object)new Integer(value);
            }else if(o instanceof Boolean){
                if(checkbox){
                    newValue = (Object)new Boolean(boolValue);
                }else{
                    newValue = (Object)new Boolean(value);
                }
            }else if(o instanceof String){
                newValue = (Object)value;
            }else if(o instanceof Color){
                newValue = getColor(editor, "background");
            }

            generic.set(propertyName, newValue);

            moleculeViewer.dirtyRepaint();
        }
    }

    public void genericRemoved(MoleculeRenderer renderer, Generic generic){
        System.out.println("generic removed " + generic);

        Object list = findComponent("distance_list");
        Object item = find(list, (String)generic.get(Generic.Name, "generic"));

        remove(item);
    }

    /** A map was added. */
    public void mapAdded(MoleculeRenderer renderer, astex.Map map){
        System.out.println("mapAdded " + map);

        Object mapComponent = findComponent("map_panel");

        addMap(mapComponent, map);
    }

    /** A map was removed. */
    public void mapRemoved(MoleculeRenderer renderer, astex.Map map){
        System.out.println("mapRemoved " + map);

        Object mapComponent = findComponent("map_panel");

        Object mapObject = find(mapComponent, map.getName());

        remove(mapObject);

        addMap(mapComponent, map);
    }

    private void addMap(Object component, astex.Map map){
        Object panel = create("panel");

        String mapTemplate = readTemplate("/astex/thinlet/maptemplate.xml.properties");

        for(int i = 0; i < map.MaximumContourLevels; i++){
            String contourTemplate = readTemplate("/astex/thinlet/contourtemplate.xml.properties");
            int color = map.getContourColor(i);
            String scolor = getColorString(color);

            contourTemplate = Util.replace(contourTemplate, "%contour", "" + i);
            contourTemplate = Util.replace(contourTemplate, "%color" + i, scolor);
            contourTemplate = Util.replace(contourTemplate, "%display" + i, "" + map.getContourDisplayed(i));

            String solid = "false";

            if(map.getContourStyle(i) == astex.Map.Surface){
                solid = "true";
            }
            contourTemplate = Util.replace(contourTemplate, "%solid" + i, solid);

            contourTemplate = Util.replace(contourTemplate, "%level" + i, "" + map.getContourLevel(i));

	    Tmesh contourObject =
		moleculeRenderer.getContourGraphicalObject(map, i);

            contourTemplate = Util.replace(contourTemplate, "%width" + i, "" + contourObject.getLineWidth());

            mapTemplate = Util.replace(mapTemplate, "%c" + i, contourTemplate);
        }

        mapTemplate = Util.replace(mapTemplate, "%n", map.getName());
        // stupid, stupid, stupid, needs doing in loop above
        mapTemplate = Util.replace(mapTemplate, "%max", "" + Math.max(map.getContourLevel(0), map.getContourLevel(1)));
        mapTemplate = Util.replace(mapTemplate, "%min", "" + Math.min(map.getContourLevel(0), map.getContourLevel(1)));
        mapTemplate = Util.replace(mapTemplate, "%level2", "" + map.getContourLevel(2));
        mapTemplate = Util.replace(mapTemplate, "%color0", getColorString2(map.getContourColor(0)));
        mapTemplate = Util.replace(mapTemplate, "%color1", getColorString2(map.getContourColor(1)));
        mapTemplate = Util.replace(mapTemplate, "%color2", getColorString2(map.getContourColor(2)));

        add(component, safeParse(mapTemplate));
    }

    /** An atom was selected. */
    public void atomSelected(MoleculeRenderer renderer, Atom atom){
        //System.out.println("atomSelected " + atom);

        if(atom == null){
            Object moleculeTree = findComponent("molecule_tree");
            Object items[] = getSelectedItems(moleculeTree);

            for(int i = 0; i < items.length; i++){
                setBoolean(items[i], "selected", false);
            }

            repaint();
        }
    }

    public boolean handleRendererEvent(RendererEvent re){
        //System.out.println("rendererEvent " + re);

        if(re.getType() == RendererEvent.ObjectAdded){
            Tmesh tmesh = (Tmesh)re.getItem();
            Object objectList = findComponent("object_list");

            //print.f("objectList " + objectList);

            addObject(objectList, tmesh);
        }else if(re.getType() == RendererEvent.ObjectRemoved){
            Tmesh tmesh = (Tmesh)re.getItem();

            remove(find(tmesh.getName()));
	}else if(re.getType() == RendererEvent.FrontClipMoved){
	    Double d = (Double)re.getItem();
	    if(d != null){
		double val = d.doubleValue();
                Object clip = findComponent("frontclip");

                if(clip != null){
                    setString(clip, "text", FILE.sprint("%.1f", val));
                }
            }
	}else if(re.getType() == RendererEvent.BackClipMoved){
	    Double d = (Double)re.getItem();
	    if(d != null){
		double val = d.doubleValue();
                Object clip = findComponent("backclip");
                if(clip != null){
                    setString(clip, "text", FILE.sprint("%.1f", val));
                }
            }
        }else{
        }

        return true;
    }

    public void resetView(Object component){
        moleculeRenderer.resetView();
        moleculeViewer.dirtyRepaint();
    }

    private void addObject(Object component, Tmesh object){
        if(component != null){
            String objectString =
                readTemplate("/astex/thinlet/objecttemplate.xml.properties");
            
            objectString = Util.replace(objectString, "%n", object.getName());
            objectString = Util.replace(objectString, "%c", Color32.formatNoQuotes(object.getColor()));
            
            add(component, safeParse(objectString));
        }
    }

    public Object createNode(String name, boolean expanded, Object ref){
        String nodeString =
            "<node text=\"" + name + "\" name=\"" + name + "\" expanded=\"" + expanded + "\" font=\"courier\"/>";

        Object node = safeParse(nodeString);

        putProperty(node, "reference", ref);

        return node;
    }

    public Object safeParse(String xml){
        try {
            return parse(new StringBufferInputStream(xml));
        }catch(Exception e){
            System.out.println("error parsing xml: " + xml);
            e.printStackTrace();

            return null;
        }
    }

    public ThinletUI(){
    }

    public void actionNode(Object component, Object mode){
        //System.out.println("actionNode " + component);

        editGeneric(component);

        //doAction(component, "select");
        doAction(component, getString(mode, "text"));
    }

    public void performNode(Object component){
        //System.out.println("performNode " + component);

        doAction(component, "center");
    }

    public void doAction(Object component, String action){
        try {
            moleculeRenderer.setSelectCount(false);

            Object selectedItems[] = getSelectedItems(component);

            //moleculeRenderer.execute("select none;");

            for(int i = 0; i < selectedItems.length; i++){
                Object selected = selectedItems[i];
                Object reference = getProperty(selected, "reference");

                if(reference != null && reference instanceof Selectable){
                    String selectionExpression = ((Selectable)reference).selectStatement();
                    
                    moleculeRenderer.execute(action + " " + selectionExpression + ";");
                    //moleculeViewer.dirtyRepaint();
                }else{
                    String selectionExpression = (String)getProperty(selected, "selection");
     
                    if(selectionExpression != null){
                        moleculeRenderer.execute(action + " " + selectionExpression + ";");
                    }
                    //moleculeViewer.dirtyRepaint();
                }
            }
            moleculeViewer.dirtyRepaint();
        }catch(Exception e){
        }finally{
            moleculeRenderer.setSelectCount(true);
        }
    }

    public void expandNode(Object component){
        Object selected = getSelectedItem(component);

        if(selected != null){
            setBoolean(component, "visible", false);

            Object firstChild = getItem(selected, 0);

            String name = getString(firstChild, "name");

            if(name.equals("dummy")){
                remove(firstChild);

                Object ref = getProperty(selected, "reference");

                if(ref != null){
                    if(ref instanceof Chain){
                        Chain chain = (Chain)ref;
                        
                        int resCount = chain.getResidueCount();
                        
                        for(int r = 0; r < resCount; r++){
                            Residue residue = chain.getResidue(r);
                            String label = residue.getName() + " " + residue.getNumber();

                            if(residue.getInsertionCode() != ' '){
                                label += residue.getInsertionCode();
                            }

                            Object node = createNode(label, false, residue);
                            
                            Object dummy = createNode("dummy", false, null);
                            
                            setColor(node, "foreground", (Color)residue.get(Residue.ResidueColor, Color.black));

                            add(node, dummy);
                            
                            add(selected, node);
                        }
                    }else if(ref instanceof Residue){

                        Residue residue = (Residue)ref;
                        
                        int atomCount = residue.getAtomCount();
                        
                        for(int a = 0; a < atomCount; a++){
                            Atom atom = residue.getAtom(a);
                            
                            Object node = createNode(atom.getAtomLabel(), false, atom);

                            if(atom.getBondCount() > 0){
                                Object dummy = createNode("dummy", false, null);
                            
                                add(node, dummy);
                            }
                            
                            add(selected, node);
                        }
                    }else if(ref instanceof Atom){
                        Atom atom = (Atom)ref;

                        int bondCount = atom.getBondCount();

                        //print.f("atom " + atom);
                        
                        for(int b = 0; b < bondCount; b++){
                            Bond bond = atom.getBond(b);
                            
                            Object node = createNode(getLabel(atom, bond), false, bond);
                            
                            add(selected, node);
                        }
                    }
                }
            }
            setBoolean(component, "visible", true);
        }
    }

    public void processPopup(Object component){
        String command = (String)getProperty(component, "command");

        Object parent = component;

        do {
            parent = getParent(parent);
        }while(getClass(parent).equals("tree") == false);

        Object selected = getSelectedItem(parent);

        Object reference = getProperty(selected, "reference");

        if(reference != null && reference instanceof Selectable){
            String sel = ((Selectable)reference).selectStatement();
            command = Util.replace(command, "%s", sel);
            command = Util.replace(command, "|", ";");
        }

        //print.f("command "+command);

        moleculeRenderer.execute(command);
        moleculeViewer.dirtyRepaint();
    }

    public String getLabel(Atom atom, Bond bond){
	Atom firstAtom = bond.getFirstAtom();
	Atom secondAtom = bond.getSecondAtom();

        if(atom != firstAtom){
            Atom tmp = firstAtom;
            firstAtom = secondAtom;
            secondAtom = tmp;
        }

        Residue firstResidue = firstAtom.getResidue();
        Residue secondResidue = secondAtom.getResidue();

        String suffix = "";

        if(secondResidue.getSequentialNumber() >
           firstResidue.getSequentialNumber()){
            suffix = "(+)";
        }else if(secondResidue.getSequentialNumber() <
                 firstResidue.getSequentialNumber()){
            suffix = "(-)";
        }
            
        return
            firstAtom.getAtomLabel() + "-" + 
            secondAtom.getAtomLabel() + suffix;
    }

    public void collapse(Object button){
        Object parent = getParent(getParent(button));

        Object target = getItem(parent, 1);

        setBoolean(target, "visible", !getBoolean(target, "visible"));
    }

    public void initLightCanvas(Object lc, Object lightPanel){
        LightCanvas lightCanvas = (LightCanvas)getComponent(lc, "bean");

        lightCanvas.moleculeRenderer = moleculeRenderer;
        lightCanvas.moleculeViewer = moleculeViewer;
        lightCanvas.renderer = moleculeRenderer.renderer;

        for(int i = 0; i < lightCanvas.renderer.lights.size(); i++){
            Light light = (Light)lightCanvas.renderer.lights.get(i);
            Object ld = find(lightPanel, "l" + i + "d");

            if(ld != null){
                setString(ld, "text", FILE.sprint("%d", light.diffuse & 255));
            }
            Object ls = find(lightPanel, "l" + i + "s");
            if(ls != null){
                setString(ls, "text", FILE.sprint("%d", light.specular & 255));
            }
            Object lp = find(lightPanel, "l" + i + "p");
            if(lp != null){
                setString(lp, "text", FILE.sprint("%d", (int)light.power));
            }
            Object l = find(lightPanel, "l" + i);
            if(l != null){
                setBoolean(l, "selected", light.on);
            }
        }
    }

    /** Hashtable of component names to objects. */
    private Hashtable components = null;

    /**
     * Look up a cached object name.
     * Don't use this if the object may change.
     */
    public Object findComponent(String name){
        if(components == null){
            components = new Hashtable();
        }

        Object component = components.get(name);

        if(component == null){
            component = shallowFind(name);

            if(component == null){
                find(name);
            }

            if(component == null){
                print.f("WARNING: couldn't find thinlet component " + name);
            }else{
                components.put(name, component);
            }
        }

        return component;
    }
}
