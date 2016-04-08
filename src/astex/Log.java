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

import java.io.*;
import java.util.*;

/**
 * Lightweight logging interface.
 * Will attempt to discover the method name.
 */
public class Log {
    /** The hard wired log level settings. */
    public static final int    DEBUG    = 1;
    public static final int    DEBUG3   = 2;
    public static final int    DEBUG2   = 3;
    public static final int    DEBUG1   = 4;
    public static final int    INFO     = 5;
    public static final int    WARN     = 6;
    public static final int    ERROR    = 7;
    public static final int    FATAL    = 8;
    public static final int    ASSERT   = 9;

    /** The hard wired log level labels. */
    public static final String logStrings[] = {
	"ALL",
	"DEBUG",
	"DEBUG3",
	"DEBUG2",
	"DEBUG1",
	"INFO",
	"WARN",
	"ERROR",
	"FATAL",
	"NOTHING",
	"ASSERT"
    };

    /** The maps of logging levels for the classes. */
    private static Hashtable methodLevels = new Hashtable();

    /** The current global log level. */
    private static int         level    = INFO;

    /** Is the class database initialised. */
    private static boolean     initialised = false;

    /** Is the logging to show whole class name. */
    private static boolean     fullClass = false;

    /** Set up the database. */
    static {
	initialise();
    }

    public static void debug(String s, double d){
	if(level <= DEBUG && getLoggingLevel(3) <= DEBUG1)
	    log(logStrings[DEBUG], FILE.sprint(s, d));
    }
    
    public static void debug3(String s, double d){
	if(level <= DEBUG3 && getLoggingLevel(3) <= DEBUG3)
	    log(logStrings[DEBUG3], FILE.sprint(s, d));
    }
    
    public static void debug2(String s, double d){
	if(level <= DEBUG2 && getLoggingLevel(3) <= DEBUG2)
	    log(logStrings[DEBUG2], FILE.sprint(s, d));
    }
    
    public static void debug1(String s, double d){
	if(level <= DEBUG1 && getLoggingLevel(3) <= DEBUG1)
	    log(logStrings[DEBUG1], FILE.sprint(s, d));
    }
    
    public static void info(String s, double d){
	if(level <= INFO) log(logStrings[INFO], FILE.sprint(s, d));
    }
    
    public static void warn(String s, double d){
	if(level <= WARN) log(logStrings[WARN], FILE.sprint(s, d));
    }
    
    public static void error(String s, double d){
	if(level <= ERROR) log(logStrings[ERROR], FILE.sprint(s, d));
    }
    
    public static void fatal(String s, double d){
	if(level <= FATAL) log(logStrings[FATAL], FILE.sprint(s, d));
    }

    public static void debug(String s, long d){
	if(level <= DEBUG && getLoggingLevel(3) <= DEBUG1)
	    log(logStrings[DEBUG], FILE.sprint(s, d));
    }
    
    public static void debug3(String s, long d){
	if(level <= DEBUG3 && getLoggingLevel(3) <= DEBUG3)
	    log(logStrings[DEBUG3], FILE.sprint(s, d));
    }
    
    public static void debug2(String s, long d){
	if(level <= DEBUG2 && getLoggingLevel(3) <= DEBUG2)
	    log(logStrings[DEBUG2], FILE.sprint(s, d));
    }
    
    public static void debug1(String s, long d){
	if(level <= DEBUG1 && getLoggingLevel(3) <= DEBUG1)
	    log(logStrings[DEBUG1], FILE.sprint(s, d));
    }
    
    public static void info(String s, long d){
	if(level <= INFO) log(logStrings[INFO], FILE.sprint(s, d));
    }
    
    public static void warn(String s, long d){
	if(level <= WARN) log(logStrings[WARN], FILE.sprint(s, d));
    }
    
    public static void error(String s, long d){
	if(level <= ERROR) log(logStrings[ERROR], FILE.sprint(s, d));
    }
    
    public static void fatal(String s, long d){
	if(level <= FATAL) log(logStrings[FATAL], FILE.sprint(s, d));
    }

    public static void debug(String s){
	if(level <= DEBUG && getLoggingLevel(3) <= DEBUG1)
	    log(logStrings[DEBUG], s);
    }
    
    public static void debug3(String s){
	if(level <= DEBUG3 && getLoggingLevel(3) <= DEBUG3)
	    log(logStrings[DEBUG3], s);
    }
    
    public static void debug2(String s){
	if(level <= DEBUG2 && getLoggingLevel(3) <= DEBUG2)
	    log(logStrings[DEBUG2], s);
    }
    
    public static void debug1(String s){
	if(level <= DEBUG1 && getLoggingLevel(3) <= DEBUG1)
	    log(logStrings[DEBUG1], s);
    }
    
    public static void info(String s){
	if(level <= INFO) log(logStrings[INFO], s);
    }
    
    public static void warn(String s){
	if(level <= WARN) log(logStrings[WARN], s);
    }
    
    public static void error(String s){
	if(level <= ERROR) log(logStrings[ERROR], s);
    }
    
    public static void fatal(String s){
	if(level <= FATAL) log(logStrings[FATAL], s);
    }

    public static void check(boolean condition, String s){
	if(!condition) logAssert(logStrings[ASSERT], s);
    }

    public static void check(boolean condition, String s, double d){
	if(!condition) logAssert(logStrings[ASSERT], FILE.sprint(s, d));
    }
    
    public static void check(boolean condition, String s, long d){
	if(!condition) logAssert(logStrings[ASSERT], FILE.sprint(s, d));
    }
    
    /** Log the assertion. */
    private static void logAssert(String intro, String s){
	String methodName = getMethodName(3);

	if(fullClass == false){
	    int lastDot = methodName.lastIndexOf('.');
	    methodName = methodName.substring(lastDot + 1,
					      methodName.length());
	}

	FILE.out.print("%-7s", intro);

	if(fullClass){
	    FILE.out.print("%-15s - ", methodName);
	}else{
	    FILE.out.print("%-10s - ", methodName);
	}
	FILE.out.println(s);
    }

    /** Output the log message. */
    private static void log(String intro, String s){
	String methodName = getMethodName(3);

	if(fullClass == false){
	    int lastDot = methodName.lastIndexOf('.');
	    methodName = methodName.substring(lastDot + 1,
					      methodName.length());
	}

	FILE.out.print("%-7s", intro);

	if(fullClass){
	    FILE.out.print("%-15s - ", methodName);
	}else{
	    FILE.out.print("%-10s - ", methodName);
	}
	FILE.out.println(s);
    }

    /** Set the logging level for this class. */
    public static void setLoggingLevel(int l){
	String methodName = getMethodName(1);
	Integer newLevel = new Integer(l);

	methodLevels.put(methodLevels, newLevel);
    }

    /** Get the logging level. */
    public static int getLoggingLevel(int l){
	String methodName = getMethodName(l);

	Integer il = (Integer)methodLevels.get(methodName);

	if(il != null){
	    return il.intValue();
	}

	String className = getClassName(methodName);

	il = (Integer)methodLevels.get(className);

	if(il != null){
	    return il.intValue();
	}

	return logStrings.length;
    }

    /** Return the class name from the stack trace entry. */
    private static String getClassName(String methodName){
	if(methodName == null){
	    return "unknown-class";
	}

	int pos = methodName.lastIndexOf('.');

	if(pos == -1){
	    return methodName;
	}else{
	    return methodName.substring(0, pos);
	}
    }
    
    /** Find the method name that called the Logger. */
    private static String getMethodName(int nline){
	Exception e = new Exception();
	ByteArrayOutputStream baos =
	    new ByteArrayOutputStream();
	PrintStream ps = new PrintStream(baos);

	e.printStackTrace(ps);

	ps.close();

	String exceptionString = baos.toString();

	StringTokenizer st = new StringTokenizer(exceptionString, "\r\n");

	int iline = 0;
	boolean seenThisMethod = false;
	String realLine = null;

	while(st.hasMoreElements()){
	    String line = st.nextToken();
	    line = line.replace('/', '.');

	    if(seenThisMethod){
		iline++;
	    }

	    if(iline == nline){
		realLine = line;
		break;
	    }

	    if(line.indexOf("astex.Log.getMethodName") != -1){
		seenThisMethod = true;
	    }
	}

	String methodName = "unknown-method";

	if(realLine != null){
	    StringBuffer sb = new StringBuffer(30);
	    int len = realLine.length();

	    for(int i = 0; i < len; i++){
		char c0 = realLine.charAt(i);

		if(c0 == 'a'){
		    char c1 = realLine.charAt(i+1);
		    char c2 = realLine.charAt(i+2);
		    
		    if(c1 == 't' && c2 == ' '){
			// found it
			for(int j = i + 3; j < len; j++){
			    char c = realLine.charAt(j);
			    if(c == '('){
				break;
			    }
			    sb.append(realLine.charAt(j));
			}
			break;
		    }
		}
	    }

	    methodName = sb.toString();
	}

	return methodName;
    }

    /** Initialise the class defintions. */
    private static void initialise(){
	Properties logProperties = 
	    FILE.loadProperties("log.properties");
	
	if(logProperties == null){
	    System.out.println("Log.initialise: couldn't open log.properties");
	    return;
	}

	Enumeration names = logProperties.propertyNames();

	while(names.hasMoreElements()){
	    String property = (String)names.nextElement();
	    String value = (String)logProperties.get(property);
	    value = value.toUpperCase();

	    if(property.equals("classname")){
		if(value.equals("FULL")){
		    fullClass = true;
		}else if(value.equals("METHOD")){
		    fullClass = false;
		}
	    }else if(property.equals("level")){
		int l = string2level(value);

		if(l != -1){
		    level = l;
		}else{
		    System.out.println("Log.initialise: invalid log level " +
				       value);
		}
	    }else{
		int level = string2level(value);

		if(level != -1){
		    Integer ilevel = new Integer(level);
		    methodLevels.put(property, ilevel);
		}else{
		    System.out.println("Log.initialise: invalid log level " +
				       value + " for class " + property);
		}
	    }
	}

	initialised = true;
    }

    /** Map a string level name to the corresponding int. */
    public static int string2level(String s){
	for(int i = 0; i < logStrings.length; i++){
	    if(s.equals(logStrings[i])){
		return i;
	    }
	}

	return -1;
    }

    public static void main(String args[]){
	Log.debug1("debugging info");
	Log.info("Just for your info");
	Log.warn("That number should be bigger");
	Log.fatal("something terrible happened");
    }
}
