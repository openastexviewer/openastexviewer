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

/** Events fired from the renderer. */
public class RendererEvent {
    public RendererEvent(int t, Object o){
	type = t;
	item = o;
    }

    public static final int ObjectAdded = 1;
    public static final int ObjectRemoved = 2;
    public static final int FrontClipMoved = 3;
    public static final int BackClipMoved = 4;

    /** The type of the event. */
    private int type = 0;
    
    /**
     * Get the value of type.
     * @return value of type.
     */
    public int getType() {
	return type;
    }
    
    /**
     * Set the value of type.
     * @param v  Value to assign to type.
     */
    public void setType(int  v) {
	this.type = v;
    }

    private Object item = null;
    
    /**
     * Get the value of item.
     * @return value of item.
     */
    public Object getItem() {
	return item;
    }
    
    /**
     * Set the value of item.
     * @param v  Value to assign to item.
     */
    public void setItem(Object  v) {
	this.item = v;
    }
}
