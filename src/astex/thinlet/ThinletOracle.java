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
import astex.thinlet.*;
import astex.splitter.*;
import thinlet.*;
import java.awt.*;
import java.lang.*;
import java.util.*;
import java.sql.*;
import oracle.jdbc.driver.*;
import oracle.jdbc.pool.*;

public class ThinletOracle extends ThinletUI {
    public ThinletOracle(String xml){
        setContent(xml);

        FILE.setDebug(false);

        initialise();
    }

    public ThinletOracle(){
        super();
    }

    private String user = "atlas";
    private String pass = "atlas";

    public void setUser(String s){
        user = s;
    }

    public void setPassword(String p){
        pass = p;
    }


    Connection con = null;

    public void initialise(){
        initialiseDriver();
    }

    private void initialiseDriver(){
	try {
	    DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
	    con =
		DriverManager.getConnection("jdbc:oracle:thin:@solaris1:1521:ASTEX",
                                            user, pass);
	} catch(SQLException e){
	    System.out.println("Hello " + e);
	}
    }

    public void setCode(Object component){
        String code = getString(component, "text");

        if(code.length() == 4){
            // looks like a pdb code
            String query = "select id from pdb_cluster_2706 where code = '" + code + "'";

            int cluster = getSingleInteger(query);

            Object clusterSpinbox = find("clusterSpinbox");

            setString(clusterSpinbox, "text", "" + cluster);

            // this should get triggered by the above setString
            // but doesn't...
            setCluster(clusterSpinbox);
        }
    }

    public void setCluster(Object component){
        if(getClass(component) == "table"){
            String cluster = getCellValueWithName(component, "id");

            Object clusterSpinbox = find("clusterSpinbox");

            setString(clusterSpinbox, "text", cluster);

            setCluster(clusterSpinbox);

        }else{
            Object clusterTable = find("clusterTable");

            try {
                populateTable(clusterTable);
            }catch(Exception e){
                print.f("exception setting cluster");
                e.printStackTrace();
            }
        }
    }

    public void doLigand(Object table){
        String id = getCellValueWithName(table, "ID");

        if(id == null){
            print.f("doStructure has null code");
            return;
        }

        doCommand("center sphere 5 around molecule ligand" + id + ";");
    }

    public void dosql(Object component){
	if(con == null){
            initialiseDriver();

            if(con == null){
                print.f("really trying to dosql before connection");
                return;
            }
        }
	
        if(getClass(component) == "tabbedpane"){
            component = getSelectedItem(component);
        }

        String sql = (String)getProperty(component, "sql");

        reallyDoSql(component, sql);

        for(int i = 1; /* nothing */; i++){
            String property = "sql" + i;

            sql = (String)getProperty(component, property);

            if(sql == null){
                print.f("no sql for " + property);
                break;
            }

            reallyDoSql(component, sql);
        }
    }

    private void reallyDoSql(Object component, String sql){
        if(sql == null){
            return;
        }

        sql = preprocess(component, sql, false);

        Statement stmt = null;

        try {
            stmt = con.createStatement();
            stmt.executeUpdate(sql);

            con.commit();
            if(stmt != null){
                stmt.close();
            }
        }catch(Exception e){
            print.f("exception executing |%s|\n", sql);
            print.f("exception " + e);
        }
    }

    private void doCommand(String command){
        moleculeViewer.execute(command);
        moleculeViewer.dirtyRepaint();
    }

    public void tabchanged(Object tabbedpane){
        Object tab = getSelectedItem(tabbedpane);

        print.f("tab " + tab);
        print.f("name " + getString(tab, "text"));
    }

    private Hashtable statementHash = null;

    private ResultSet getResultSet(String query){
        Statement stmt = null;
        ResultSet rs = null;

	if(con == null){
            initialiseDriver();

            if(con == null){
                print.f("really trying to get result set before connection");
                return null;
            }
        }
	
	try {
	    stmt = con.createStatement();

	    rs = stmt.executeQuery(query);

            if(statementHash == null){
                statementHash = new Hashtable();
            }

            statementHash.put(rs, stmt);
	    
	} catch(SQLException e){
	    System.out.println("Couldn't create statement " + e);
            System.out.println("query " + query);
	}

        return rs;
    }

    /** Convenience function to get rid of all the f...ing exceptions */
    public boolean next(ResultSet rs){
        boolean ret = false;
        try {
            ret = rs.next();
            return ret;
        }catch(Exception e){
            print.f("exception getting next row " + e);
            return false;
        }
    }

    public int getInt(ResultSet rs, String thing){
        try {
            return rs.getInt(thing);
        }catch(Exception e){
            print.f("exception getting int " + e);
            return 0;
        }
    }

    public double getDouble(ResultSet rs, String thing){
        try {
            return rs.getDouble(thing);
        }catch(Exception e){
            print.f("exception getting double " + e);
            return 0.0;
        }
    }

    public String getString(ResultSet rs, String thing){
        try {
            return rs.getString(thing);
        }catch(Exception e){
            print.f("exception getting string " + e);
            return null;
        }
    }

    public String getDate(ResultSet rs, String thing){
        try {
            return rs.getDate(thing).toString();
        }catch(Exception e){
            print.f("exception getting date " + e);
            return null;
        }
    }

    public int getSingleInteger(String query){
        int val = -1;
        ResultSet rs = getResultSet(query);

        while(next(rs)){

            try {
                val = rs.getInt(1);
            }catch(Exception e){
                print.f("exception getting single integer " + query);
            }
            return val;
        }

        close(rs);

        return val;
    }

    public void close(ResultSet rs){
        try {
            Statement stmt = (Statement)statementHash.get(rs);
            rs.close();
            stmt.close();
            statementHash.remove(rs);
        }catch(Exception e){
            print.f("couldn't close resultset " + rs);
        }
    }

    public String getText(String name){
        Object component = find(name);

        if(component == null){
            print.f("couldn't get text for " + name);
        }

        return getString(component, "text");
    }

    public void setText(String name, String value){
        Object component = find(name);
        
        if(component == null){
            print.f("couldn't set text for " + name);
        }

        setString(component, "text", value);
    }

    public void populateTable(Object table) throws Exception {
        loadTable(table);
    }

    public void populateTableExecute(Object table) throws Exception {
        loadTable(table);

        execute(table);
    }

    private Hashtable colorHash = null;

    public Color getColor(String name){
        if(colorHash == null){
            colorHash = new Hashtable();
        }

        if(name == null || name.equals("")){
            return null;
        }

        Color c = (Color)colorHash.get(name);

        if(c == null){
            c = Color32.getAWTColor(Color32.getColorFromName(name));
            colorHash.put(name, c);
        }

        return c;
    }

    public void loadHeader(Object object){
        Object header = find("headerTextarea");

        String url = (String)getProperty(object, "url");

        url = preprocess(object, url, false);

        FILE f = FILE.open(url);

        if(f == null){
            print.f("couldn't open url |" + url + "|");
        }

        //setString(header, "text", "");

        StringBuffer contents = new StringBuffer(4096);

        while(f.nextLine()){
            String s = f.getCurrentLineAsString();
            if(s.startsWith("ATOM")){
                break;
            }
            contents.append(s);
            contents.append("\n");
        }

        f.close();

        setString(header, "text", contents.toString());
    }

    private int tableCount = 0;

    private int smilesWidth = 80;

    /**
     * Populate a table with values from an sql connection.
     */
    public void loadTable(Object table) throws Exception {
        String sql = (String)getProperty(table, "sql");
        String trailingString = (String)getProperty(table, "trailing");
        
        if(getProperty(table, "smileswidth") != null){
            smilesWidth = Integer.parseInt((String)getProperty(table, "smileswidth"));
            System.out.println("smilesWidth " +smilesWidth);
        }

        // should we put a dummy column in table
        // true by default
        boolean trailing = true;

        if(trailingString != null){
            trailing = trailingString.equals("true");
        }

        if(getString(table, "name") == null){
            setString(table, "name", FILE.sprint("private_table_%03d", ++tableCount));
        }

        if(sql == null){
            throw new Exception("no sql for table " + getString(table, "name"));
        }

        sql = preprocess(table, sql, false);

        //print.f("sql |%s|\n", sql);

        removeAll(table);

        ResultSet rs = getResultSet(sql);

        if(rs == null){
            throw new Exception("couldn't get result set for " + getString(table, "name"));
        }

        ResultSetMetaData meta = rs.getMetaData();

        Object header = create("header");

        String method = "sortTable(this)";

        //print.f("method |" + method + "|");

        setMethod(header, "action", method, this.getDesktop(), this);

        setBoolean(header, "resizable", true);

        int columnCount = meta.getColumnCount();

        String columnNames[] = new String[columnCount + 1];

        int columnWidths[] = new int[columnCount + 1];

        Object columnObjects[] = new Object[columnCount + 1];

        for(int col = 1; col <= columnCount; col++){
            String name = meta.getColumnName(col);
            columnNames[col] = name;

            if(name.endsWith("_") == false){

                Object column = create("column");

                columnObjects[col] = column;

                setString(column, "text", name);
                
                add(header, column);
                
                FontMetrics fm = getFontMetrics(getFont(column));
                int width = 0;

                if(name != null){
                    if("SMILES".equals(name)){
                        width = smilesWidth;
                    }else{
                        // add some spaces to allow for the sort indicator
                        width = fm.stringWidth(name + "    ");
                    }
                    columnWidths[col] = width;
                }

                //print.f("header " + col + " width " + columnWidths[col]);
            }
        }

        if(trailing){
            add(header, create("column"));
        }

        add(table, header);

        putProperty(header, "parentTable", table);

        Color foregroundCell = null;
        Color backgroundCell = null;

        while(next(rs)){
            Object row = create("row");
            Color foreground = null;
            Color background = null;
            String fontString = null;

            //putProperty(row, "childCount", "" + columnCount + (trailing ? 1 : 0));

            for(int col = 1; col <= columnCount; col++){
                String name = columnNames[col];

                //print.f("col " + col + " |" + rs.getString(col)+"|");

                if(name.endsWith("_")){
                    // its a pseudo column
                    String value = rs.getString(col);
                    
                    if(name.equalsIgnoreCase("fg_")){
                        foreground = getColor(value);
                    }else if(name.equalsIgnoreCase("bg_")){
                        background = getColor(value);
                    }else if(name.equalsIgnoreCase("bgcell_")){
                        backgroundCell = getColor(value);
                    }else if(name.equalsIgnoreCase("fgcell_")){
                        foregroundCell = getColor(value);
                    }else if(name.equalsIgnoreCase("font_")){
                        fontString = value;
                    }
                }else{
                    Object cell = null;

                    if("SMILES".equalsIgnoreCase(name)){
                        cell = safeParse("<cell paint=\"drawSmiles\"></cell>");
                    }else{
                        cell = create("cell");
                    }

                    int type = meta.getColumnType(col);
                    boolean right = false;

                    String textValue = null;

                    //print.f("type     " + type);

                    //print.f("typename " + meta.getColumnTypeName(col));

                    if(type == Types.VARCHAR){
                        String data = rs.getString(col);
                        textValue = data;
                        //setString(cell, "text", data);
                        
                    }else if(type == Types.DATE ||
                             type == Types.TIME ||
                             type == Types.TIMESTAMP){
                        textValue = "" +rs.getDate(col);
                        //setString(cell, "text", "" +rs.getDate(col));
                    }else if(type == Types.INTEGER){
                        //print.f("integer " + col);
                        textValue = "" +rs.getInt(col);
                        //setString(cell, "text", "" +rs.getInt(col));
                    }else if(type == Types.NUMERIC){
                        String specialFormat = (String)getProperty(table, "format" + col);
                        
                        if(specialFormat != null){
                            if(specialFormat.endsWith("d")){
                                textValue = FILE.sprint(specialFormat, rs.getInt(col));
                                //setString(cell, "text", FILE.sprint(specialFormat, rs.getInt(col)));
                            }else{
                                textValue = FILE.sprint(specialFormat, rs.getDouble(col));
                                //setString(cell, "text", FILE.sprint(specialFormat, rs.getDouble(col)));
                            }
                        }else{
                            int scale = meta.getScale(col);
                            int precision = meta.getPrecision(col);
                            
                            if(scale == 0 && precision == 0){
                                textValue = rs.getString(col);
                                //setString(cell, "text", rs.getString(col));
                            }else if(scale != 0){
                                String format = null;
                                if(precision != 0){
                                    format =
                                        FILE.sprint("%%%d.", precision) +
                                        FILE.sprint("%df", scale);
                                }else{
                                    format = FILE.sprint("%%.%df", scale);
                                }
                                textValue = FILE.sprint(format, rs.getDouble(col));
                                //setString(cell, "text", FILE.sprint(format, rs.getDouble(col)));
                            }else{
                                textValue = FILE.sprint("%d", rs.getInt(col));
                                //setString(cell, "text", FILE.sprint("%d", rs.getInt(col)));
                            }
                        }
                        right = true;
                    }else{
                        // unknown type, show the type name in red
                        textValue = meta.getColumnTypeName(col);
                        //setString(cell, "text", meta.getColumnTypeName(col));
                        setColor(cell, "foreground", Color.red);
                    }

                    if(textValue != null){
                        //print.f("textValue " + textValue);
                        if(textValue.startsWith("icon:")){
                            //print.f("seen icon");
                            String iconString = textValue.substring(5);
                            iconString = Util.replace(iconString, "#", "%23");
                            //print.f("icon is |%s|\n", iconString);

                            Image icon = getIcon(iconString, true);

                            if(icon != null){
                                int width = icon.getWidth(null);

                                if(width > columnWidths[col]){
                                    columnWidths[col] = width;
                                }

                                setIcon(cell, "icon", icon);
                            }
                        }else if("SMILES".equals(name)){
                            setInteger(cell, "width", smilesWidth);
                            setInteger(cell, "height", smilesWidth);

                            setString(cell, "text", textValue);
                        }else{
                            FontMetrics fm = getFontMetrics(getFont(cell));
                            int width = fm.stringWidth(textValue);

                            if(width > columnWidths[col]){
                                columnWidths[col] = width;
                            }            

                            setString(cell, "text", textValue);
                        }
                    }

                    if(right){
                        setChoice(cell, "alignment", "right");
                    }

                    Color fg = foregroundCell;
                    if(fg == null){
                        fg = foreground;
                    }

                    if(fg != null){
                        setColor(cell, "foreground", fg);
                        foregroundCell = null;
                    }

                    Color bg = backgroundCell;
                    if(bg == null){
                        bg = background;
                        backgroundCell = null;
                    }

                    if(bg != null){
                        setColor(cell, "background", bg);
                    }

                    if(fontString != null){
                        Font font = parseFont(fontString, getFont(cell));
                        setFont(cell, "font", font);
                    }



                    add(row, cell);
                }
            }

            if(trailing){
                add(row, create("cell"));
            }

            add(table, row);
        }

        close(rs);

        //print.f("start vectors " + vectorsAllocated);

        for(int i = 1; i <= columnCount; i++){
            if(columnObjects[i] != null &&
               columnWidths[i] > 0 /*&&
               columnWidths[i] <= 50*/){
                setInteger(columnObjects[i], "width", columnWidths[i] + 4);
            }
        }
    }

    public void populateTree(Object tree) throws Exception {

        print.f("### hello from populateTree");
        String sql = (String)getProperty(tree, "sql");

        if(sql == null){
            throw new Exception("no sql for tree " + getString(tree, "name"));
        }

        sql = preprocess(tree, sql, false);

        print.f("sql |%s|\n", sql);

        removeAll(tree);

        ResultSet rs = getResultSet(sql);

        if(rs == null){
            throw new Exception("couldn't get result set for " + getString(tree, "name"));
        }

        ResultSetMetaData meta = rs.getMetaData();

        int columnCount = meta.getColumnCount();

        int currentLevel = 0;

        Object treeStack[] = new Object[1024];

        treeStack[0] = tree;

        setBoolean(tree, "visible", false);

        while(next(rs)){
            int level = -1;
            String text = null;

            boolean expand = false;
            boolean select = false;
            Color foreground = null;
            Color background = null;
            String fontString = null;

            Object item = create("node");

            for(int col = 1; col <= columnCount; col++){
                // its the level pseudocolumn
                String name = meta.getColumnName(col);

                if("level".equalsIgnoreCase(name)){
                    level = rs.getInt(col);
                }else if("text".equalsIgnoreCase(name)){
                    text = rs.getString(col);
                }else if(name.endsWith("_")){
                    // its a pseudo column
                    String value = rs.getString(col);
                    
                    if(name.equalsIgnoreCase("fg_")){
                        foreground = getColor(value);
                    }else if(name.equalsIgnoreCase("bg_")){
                        background = getColor(value);
                    }else if(name.equalsIgnoreCase("font_")){
                        fontString = value;
                    }else{
                        throw new Exception("unknown pseudo column " + name);
                    }
                }else{
                    // anything else goes as generic property on item
                    putProperty(item, name, rs.getString(col));
                }
            }

            if(level == -1){
                throw new Exception("no level defined for row");
            }else if(text == null){
                throw new Exception("no text defined for row");
            }

            Object parent = treeStack[level - 1];

            if(parent == null){
                throw new Exception("no parent for " + level + " " + text);
            }
            
            //print.f(level + " " + text);

            setString(item, "text", text);
            setBoolean(item, "expanded", expand);
            setBoolean(item, "selected", select);

            if(foreground != null){
                setColor(item, "foreground", foreground);
            }
            if(background != null){
                setColor(item, "background", background);
            }

            if(fontString != null){
                Font font = parseFont(fontString, getFont(item));
                setFont(item, "font", font);
            }

            add(parent, item);
            
            treeStack[level] = item;
        }

        close(rs);

        setBoolean(tree, "visible", true);
    }

    /**
     * Parse a simple font definition.
     * Unspecified entries are used from the passed font.
     */
    public Font parseFont(String value, Font font){
        String name = null;
        boolean bold = false; boolean italic = false;
        int size = 0;
        StringTokenizer st = new StringTokenizer(value);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if ("bold".equalsIgnoreCase(token)) { bold = true; }
            else if ("italic".equalsIgnoreCase(token)) { italic = true; }
            else {
                try {
                    size = Integer.parseInt(token);
                } catch (NumberFormatException nfe) {
                    name = (name == null) ? token : (name + " " + token);
                }
            }
        }
        if (name == null) {
            name = (font == null) ? "times" : font.getName();
        }
        if (size == 0) {
            size = (font == null) ? 12 : font.getSize();
        }
        return new Font(name, (bold ? Font.BOLD : 0) | (italic ? Font.ITALIC : 0), size);
    }

    private boolean numeric = false;
    private int sortColumn = -1;
    private String sortDir = null;

    public void sortTable(Object header){
        // retrieve table from property in header
        // can't seem to get it as parent
        Object table = getProperty(header, "parentTable");

        if(table == null){
            print.f("can't find table for this header");
            return;
        }

        //print.f("header " + getClass(header));
        //print.f("table  " + table);
        //print.f("table " + getString(table, "name"));

        int columnCount = getCount(header);

        sortColumn = -1;
        sortDir = null;

        for(int i = 0; i < columnCount; i++){
            Object column = getItem(header, i);
            String sort = getChoice(column, "sort");

            if(sort != "none"){
                sortColumn = i;
                sortDir = sort;
                break;
            }

        }

        //print.f("sortColumn " + sortColumn + " dir " + sortDir);

        if(sortColumn == -1){
            return;
        }

        //print.f("getCount " + getCount(table));

        Object rows[] = getItems(table);
        int rowCount = rows.length;

        numeric = true;

        for(int r = 0; r < rowCount; r++){
            Object row = rows[r];
            Object cell = getItem(row, sortColumn);

            String text = getString(cell, "text");

            // this is a bit of a pain having to
            // create an object just to find if
            // it is a double or not...
            try {
                Double d = Double.valueOf(text);
            }catch(Exception e){
                numeric = false;
                break;
            }
        }

        compares = 0;

        QuickSort(rows, 0, rowCount - 1);

        // remove old row order
        setBoolean(table, "visible", false);

        removeAll(table);
        
        putProperty(table, "childCount", "" + rowCount);
            
        // new sorted rows
        for(int r = 0; r < rowCount; r++){
            Object row = rows[r];
            
            add(table, row);
        }

        setBoolean(table, "visible", true);
    }

    void QuickSort(Object a[], int lo0, int hi0) {
        int lo = lo0;
        int hi = hi0;
        Object mid;
        
        if (hi0 > lo0) {
            mid = a[(lo0 + hi0) / 2];
            
            while (lo <= hi) {
                while ((lo < hi0) && (compare(a[lo], mid) < 0)){
                    ++lo;
                }
                
                while ((hi > lo0) && (compare(a[hi], mid) > 0)){
                    --hi;
                }
                
                if (lo <= hi) {
                    Object t = a[hi];
                    a[hi] = a[lo];
                    a[lo] = t;
                    
                    ++lo;
                    --hi;
                }
            }
            
            if (lo0 < hi){
                QuickSort(a, lo0, hi);
            }

            if (lo < hi0){
                QuickSort(a, lo, hi0);
            }
        }
    }

    private int compares = 0;

    public int compare(Object o1, Object o2){
        Object cell1 = getItem(o1, sortColumn);
        Object cell2 = getItem(o2, sortColumn);
        String s1 = getString(cell1, "text");
        String s2 = getString(cell2, "text");

        compares++;

        if(s1 == null && s2 == null){
            return 0;
        }else if(s1 == null){
            return (sortDir == "descent") ? -1 : 1;
        }else if(s2 == null){
            return (sortDir == "descent") ? 1 : -1;
        }

        if(numeric){
            double d1 = FILE.readDouble(s1);
            double d2 = FILE.readDouble(s2);
            if(d1 == d2){
                return 0;
            }else if(d1 < d2){
                return (sortDir == "descent") ? 1 : -1;
            }else if(d1 > d2){
                return (sortDir == "descent") ? -1 : 1;
            }
        }else{
            return (sortDir == "descent") ? s1.compareTo(s2) : -s1.compareTo(s2);
        }

        return 0;
    }
}
