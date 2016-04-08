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
/* Copyright Astex Technology Ltd. 2002 */

/*
 */

/**
 * An object that implements a dynamic array.
 * 
 * The array grows as necessary as objects are added to it.
 * Array copying is currently performed by explicit loops
 * rather than using System.arraycopy.
 *
 */
public class StringArray {
    /**
     * The array of objects.
     */
    private String objects[] = null;

    /**
     * The number of objects stored in the array.
     */
    private int objectCount = 0;

    /**
     * The amount by which the array is grown.
     *
     * If the value is 0 then the array is doubled in size.
     */
    private int capacityIncrement = 4;

    /**
     * Constructor which specifies the initial size and the
     * capacity increment.
     */
    public StringArray(int initialSize, int increment){
        if(initialSize < 0){
            initialSize = 0;
        }
        if(increment < 0){
            increment = 0;
        }
        if(initialSize > 0){
            objects = new String[initialSize];
        }
        objectCount = 0;
        capacityIncrement = increment;
    }

    /**
     * Default constructor.
     */
    public StringArray(){
        this(0);
    }

    /**
     * Constructor which specifies the initial size.
     */
    public StringArray(int initialSize){
        this(initialSize, 0);
    }

    /**
     * Convenience method to make sure that the object array has enough
     * room for a new object.
     */
    private void ensureCapacity(){
        // we need to grow our array.
        if(objects == null || objectCount == objects.length){
            int newCapacity;

            if(capacityIncrement == 0){
                newCapacity = objectCount * 2;
            }else{
                newCapacity = objectCount + capacityIncrement;
            }

            if(newCapacity == 0){
                newCapacity = 1;
            }

            String newObjects[] = new String[newCapacity];

            if(objects != null){
                for(int i = 0; i < objectCount; i++){
                    newObjects[i] = objects[i];
                }
            }

            objects = newObjects;
        }
    }

    /** Set the capacity for the object. */
    public void setCapacity(int count){
        if(objectCount != 0){
            System.err.println("setCapacity called on non-empty Array");
            return;
        }

        objectCount = count;

        objects = new String[objectCount];
    }

    /**
     * Add an entry to the CLASSNAME.
     */
    public int add(String object){
        ensureCapacity();

        objects[objectCount] = object;

        return objectCount++;
    }

    /**
     * Remove an object from the CLASSNAME.
     *
     * All occurrences of the object will be removed.
     */
    public void remove(String object){
        for(int i = objectCount - 1; i >= 0; i--){
            if(objects[i] == object){
                removeElement(i);
            }
        }
    }

    /**
     * Remove a specified element from the CLASSNAME.
     */
    public void removeElement(int element){
        if(element == objectCount - 1){
            // its the last entry so we can just decrement the
            // object counter
            objectCount--;
            objects[objectCount] = null;
        }else if(element < objectCount && element >= 0){
            for(int i = element + 1; i < objectCount; i++){
                objects[i - 1] = objects[i];
            }

            objectCount--;
            objects[objectCount] = null;
        }
    }

    /**
     * Remove all elements from the dynamic array.
     */
    public void removeAllElements(){
        for(int i = 0; i < objectCount; i++){
            objects[i] = null;
        }
        objectCount = 0;
    }

    /**
     * Return a specified element from the array.
     */
    public String get(int index){
        return objects[index];
    }

    /** Return a specified element from the array end of the array. */
    public String getReverse(int index){
        return objects[objectCount - index - 1];
    }

    /** Set a specified element in the array. */
    public void set(int index, String val){
        // don't check for array bounds conditions.
        // or even the allocation of the array
        objects[index] = val;
    }

    /** Return the reference to the object array. */
    public String[] getArray(){
        return objects;
    }

    /** Does the array contain the specified object. */
    public boolean contains(String object){
        return getIndex(object) != -1;
    }

    /** Return the location of the object or -1 if its not present. */
    public int getIndex(String object){
        for(int i = 0; i < objectCount; i++){
            if(objects[i] == object){
                return i;
            }
        }

        return -1;
    }

    /**
     * Return the number of objects in the object array.
     */
    public int size(){
        return objectCount;
    }
    /**
     * Print a CLASSNAME contents.
     */
    public static void print(String message, StringArray array){
        System.out.println(message);

        for(int i = 0; i < array.size(); i++){
            System.out.println("array[" + i + "] = " + array.get(i));
        }
    }
}
