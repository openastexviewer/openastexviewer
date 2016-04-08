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

import java.util.*;

public class Arguments extends Hashtable {
    /** Add a dash to the argument if it doesn't exist. */
    public static String addDash(String argument){
	if(argument != null && argument.startsWith("-") == false){
	    return "-" + argument;
	}else{
	    return argument;
	}
    }
    
    /** Is this argument defined. */
    public boolean defined(String argument){
	argument = addDash(argument);

	if(get(argument) == null){
	    return false;
	}else{
	    return true;
	}
    }

    /** Lookup value of double arg or return default. */
    public double getDouble(String argument, double defaultVal){
	argument = addDash(argument);
	
	Object o = get(argument);

	if(o == null){
	    String value = (String)propertyGet(argument);
	    if(value != null){
		return FILE.readDouble(value);
	    }
	}

	if(o == null){
	    return defaultVal;
	}else{
	    if(o instanceof Double){
		return ((Double)o).doubleValue();
	    }else if(o instanceof Integer){
		return (double)((Integer)o).intValue();
	    }else{
		System.out.print("getDouble " + argument);
		System.out.println(": not a double");
		return defaultVal;
	    }
	}
    }

    /** Lookup value of int arg or return default. */
    public int getInteger(String argument, int defaultVal){
	argument = addDash(argument);
	
	Object o = get(argument);

	if(o == null){
	    String value = (String)propertyGet(argument);
	    if(value != null){
		return FILE.readInteger(value);
	    }
	}

	if(o == null){
	    return defaultVal;
	}else{
	    if(o instanceof Integer){
		return ((Integer)o).intValue();
	    }else{
		System.out.print("getInteger " + argument);
		System.out.println(": not an integer");
		return defaultVal;
	    }
	}
    }

    /** Lookup value of String arg or return default. */
    public String getString(String argument, String defaultVal){
	argument = addDash(argument);
	
	Object o = get(argument);

	if(o == null){
	    o = propertyGet(argument);
	}

	if(o == null){
	    return defaultVal;
	}else{
	    if(o instanceof String){
		return (String)o;
	    }else{
		System.out.print("getString " + argument);
		System.out.println(": not a String");
		System.out.println("value " + o);
		return defaultVal;
	    }
	}
    }

    /** Return argument interpreted as a color. */
    public int getColor(String argument, int defaultVal){
        String o = getString(argument, null);

        if(o == null){
            return defaultVal;
        }else{
            if(o instanceof String){
                return Color32.getColorFromName(o);
            }else{
		System.out.print("getColor " + argument);
		System.out.println(": not a String");
		System.out.println("value " + o);
		return defaultVal;
            }
        }
    }

    /** Lookup value of String arg or return default. */
    public boolean getBoolean(String argument, boolean defaultVal){
	argument = addDash(argument);
	
	Object o = get(argument);

	if(o == null){
	    o = propertyGet(argument);
	}

	if(o == null){
	    return defaultVal;
	}else{
	    if(o instanceof Boolean){
		return ((Boolean)o).booleanValue();
	    }else{
		System.out.print("getBoolean " + argument);
		System.out.println(": not a Boolean");
		return defaultVal;
	    }
	}
    }

    /** Return lookup of string argument from array of possibles. */
    public int getStringOption(String argument, String possibles[], 
			       int possibleValues[], int defaultValue){
	String value = getString(argument, null);

	if(value == null){
	    value = (String)propertyGet(argument);
	}

	if(value == null){
	    return defaultValue;
	}
	
	for(int i = 0; i < possibles.length; i++){
	    if(value.equals(possibles[i])){
		if(possibleValues != null){
		    return possibleValues[i];
		}else{
		    return i;
		}
	    }
	}

	System.out.println("getStringOption: illegal value for " + argument);
	System.out.println("getStringOption: value was " + value);
	System.out.print("getStringOption: possible values: ");

	for(int i = 0; i < possibles.length; i++){
	    if(i > 0){
		System.out.print(", ");
	    }
	    System.out.print(possibles[i]);
	}

	System.out.println("");

	return -1;
    }

    /** Return a property from the builtin file. */
    public static Object propertyGet(String argument){
	ensureConfigurationFileLoaded();

	if(argument.startsWith("-")){
	    argument = argument.substring(1, argument.length());
	}

	return properties.get(argument);
    }

    /** Make sure we loaded the config file. */
    public static void ensureConfigurationFileLoaded(){
	if(properties == null){
	    loadConfigurationFile(null);
	}
    }

    private static Properties properties = null;

    /** Load a set of properties from a configuration file. */
    public static synchronized void loadConfigurationFile(String filename){
	if(filename == null){
	    filename = "config.properties";
	}

	properties = FILE.loadProperties(filename);
    }
}
