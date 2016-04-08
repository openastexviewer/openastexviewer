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

public class Settings {
	/** Hashtable of property objects. */
	private static Hashtable propertyObjects = new Hashtable();

	/** The main get method. */
	public static Object get(String table, String property){
		//Log.info("looking for table " + table);
		Properties properties = (Properties)propertyObjects.get(table);

		if(properties == null){
			//Log.info("couldn't find " + table);
			String resource = table + ".properties";

			properties = FILE.loadProperties(resource);

			if(properties != null){
				//Log.info("loaded " + table);
				propertyObjects.put(table, properties);
			}else{
				Log.error("couldn't load " + table);
				return null;
			}
		}

		Object object = properties.get(property);

		if(object == null){
			return null;
		}else{
			return object;
		}
	}

	public static Object get(String table, String property, Object defaultVal){
		Object object = get(table, property);

		if(object == null){
			return defaultVal;
		}else{
			return object;
		}
	}

	public static double getDouble(String table, String argument){
		return getDouble(table, argument, 0.0);
	}

	/** Lookup value of double arg or return default. */
	public static double getDouble(String table, String argument, double defaultVal){

		Object o = get(table, argument);

		if(o == null){
			return defaultVal;
		}else{
			try {
				return Double.valueOf((String)o).doubleValue();
			}catch(Exception e){
				Log.error(table + "," + argument + " not a double");
				return defaultVal;
			}
		}
	}

	public static int getInteger(String table, String argument){
		return getInteger(table, argument, 0);
	}

	/** Lookup value of int arg or return default. */
	public static int getInteger(String table, String argument, int defaultVal){
		Object o = get(table, argument);

		if(o == null){
			return defaultVal;
		}else{
			try {
				return Integer.valueOf((String)o).intValue();
			}catch(Exception e){
				Log.error(table + "," + argument + " not an integer");
				return defaultVal;
			}
		}
	}

	public static String getString(String table, String argument){
		return getString(table, argument, null);
	}

	/** Lookup value of String arg or return default. */
	public static String getString(String table, String argument, String defaultVal){
		Object o = get(table, argument);

		if(o == null){
			return defaultVal;
		}else{
			return (String)o;
		}
	}

	public static boolean getBoolean(String table, String argument){
		return getBoolean(table, argument, false);
	}

	/** Lookup value of String arg or return default. */
	public static boolean getBoolean(String table, String argument, boolean defaultVal){
		Object o = get(table, argument);

		if(o == null){
			return defaultVal;
		}else{

			try {
				return Boolean.valueOf((String)o).booleanValue();
			}catch(Exception e){
				Log.error(table + "," + argument + " not a boolean");
				return defaultVal;
			}
		}
	}

	/** Test method. */
	public static void main(String args[]){

		System.out.println("thinlet    " + Settings.getString("thinlet", "astex.Distance.mode"));
		System.out.println("hb    " + Settings.getDouble("config", "hbond.constant"));

		System.out.println("gecko " + Settings.getDouble("config", "hbond.gecko"));

		System.out.println("log " + Settings.getBoolean("config", "log"));

		System.out.println("fragment.1 " + Settings.getString("fragment", "fragment.1"));
	}
}