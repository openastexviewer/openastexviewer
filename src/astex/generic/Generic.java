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

package astex.generic;

import java.util.*;

public class Generic implements GenericInterface {
    
    public static final String ClassName   = "__class__";

    private static final Hashtable emptyHashtable = new Hashtable();
    private static final Vector    emptyVector    = new Vector();

    public Generic(String cl){
        setClassname(cl);
    }

    public Generic(){
        setClassname(getClass().getName());
    }

    private Hashtable properties = null;

    public Object get(Object key, Object def){
        if(properties == null){

            return def;
        }else{
            Object value = properties.get(key);
        
            return (value != null) ? value : def;
        }
    }

    public Object set(Object name, Object value){
        Object oldValue = null;

        if(properties == null){
            properties = new Hashtable();
        }else{
            oldValue = properties.get(name);
        }

        if(value == null){
            properties.remove(name);
        }else{
            properties.put(name, value);
        }

        if(listeners != null){
            GenericEvent ge = new GenericEvent(GenericEvent.PropertyChanged,
                                               this, name, value);
            
            notifyListeners(ge);
        }

        return oldValue;
    }

    public Enumeration getProperties(){
        if(properties == null){
            return emptyHashtable.keys();
        }else{
            return properties.keys();
        }
    }

    public void setClassname(String c){
        set(ClassName, c);
    }

    public Object getClassname(){
        return get(ClassName, null);
    }

    Vector children = null;

    public void addChild(GenericInterface child){
        if(children == null){
            children = new Vector();
        }

        children.addElement(child);

        if(listeners != null){
            GenericEvent ge = new GenericEvent(GenericEvent.ChildAdded,
                                               this, child, null);
            notifyListeners(ge);
        }
    }

    public void removeChild(GenericInterface child){
        if(children != null){
            children.removeElement(child);
        }else{
            throw new RuntimeException("no such child: " + child);
        }

        if(listeners != null){
            GenericEvent ge = new GenericEvent(GenericEvent.ChildRemoved,
                                               this, child, null);
            notifyListeners(ge);
        }
    }

    public Enumeration getChildren(Object type){
        if(children == null){
            return emptyVector.elements();
        }else{
            return children.elements();
        }
    }

    Vector parents = null;

    public void addParent(GenericInterface parent){
        if(parents == null){
            parents = new Vector();
        }

        parents.addElement(parent);

        if(listeners != null){
            GenericEvent ge = new GenericEvent(GenericEvent.ParentAdded,
                                               this, parent, null);
            notifyListeners(ge);
        }
    }

    public void removeParent(GenericInterface parent){
        if(parents != null){
            parents.removeElement(parent);
        }else{
            throw new RuntimeException("no such parent: " + parent);
        }

        if(listeners != null){
            GenericEvent ge = new GenericEvent(GenericEvent.ParentRemoved,
                                               this, parent, null);
            notifyListeners(ge);
        }
    }

    public Enumeration getParents(Object type){
        if(parents == null){
            return emptyVector.elements();
        }else{
            return parents.elements();
        }
    }

    Vector listeners = null;

    public void addListener(GenericEventInterface gei){
        if(listeners == null){
            listeners = new Vector();
        }

        listeners.addElement(gei);
    }

    public void removeListener(GenericEventInterface gei){
        if(listeners != null){
            listeners.removeElement(gei);
        }
    }

    private void notifyListeners(GenericEvent ge){
        if(listeners != null){
            int listenersCount = listeners.size();
            
            for(int i = 0; i < listenersCount; i++){
                GenericEventInterface listener =
                    (GenericEventInterface)listeners.elementAt(i);
                
                listener.handleEvent(ge);
            }
        }
    }

    public static void main(String args[]){
        Generic scene = new Generic("scene");

        GenericEventInterface listener = new GenericEventInterface(){
                public boolean handleEvent(GenericEvent ge){
                    System.out.println("scene listener " + ge);
                    
                    return true;
                }
            };


        scene.addListener(listener);

        Generic mol = new Generic("mol");

        mol.addListener(listener);

        mol.set("RenderStyle", "trace");
        mol.set("Displayed", Boolean.TRUE);

        scene.addChild(mol);
    }

    /* Static convenience functions. */

    /** Get a double. */
    public double getDouble(Object property, double def){
        Double val = (Double)get(property, null);

        return val != null ? val.doubleValue() : def;
    }

    /** Get an int. */
    public int getInteger(Object property, int def){
        Integer val = (Integer)get(property, null);

        return val != null ? val.intValue() : def;
    }

    /** Get a String. */
    public String getString(Object property, String def){
        String val = (String)get(property, null);

        return val != null ? val : def;
    }

    /** Get a boolean. */
    public boolean getBoolean(Object property, boolean def){
        Boolean val = (Boolean)get(property, null);

        return val != null ? val.booleanValue() : def;
    }

    /** Set a double. */
    public void setDouble(Object property, double val){
        Double dval = new Double(val);

        set(property, dval);
    }

    /** Set an int. */
    public void setInteger(Object property, int val){
        Integer ival = new Integer(val);

        set(property, ival);
    }

    /** Set a String. */
    public void setString(Object property, String val){
        set(property, val);
    }

    /** Set a Boolean. */
    public void setBoolean(Object property, boolean val){
        if(val){
            set(property, Boolean.TRUE);
        }else{
            set(property, Boolean.FALSE);
        }
    }
}
