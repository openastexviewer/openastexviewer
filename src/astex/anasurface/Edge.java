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

package astex.anasurface;

import astex.*;

public class Edge extends IntArray {
    /** Torus to which we belong. */
    public Torus torus = null;

    /** Index of first sphere on edge. */
    public Vertex v0 = new Vertex();
    
    /** Index of second sphere on edge. */
    public Vertex v1 = new Vertex();

    /** Coordinates of center of arc. */
    public double cen[] = new double[3];

    /** Radius of arc. */
    public double r;

    /** Angle of arc. */
    public double angle;
    
    /** Do we self intersect? */
    public boolean selfIntersects = false;

    /** Normal to edge plane, used for clipping. */
    public double n[] = new double[3];

    /** Probe face that this edge belongs to. */
    public Face probeFace = null;

    /** Torus face that this edge belongs to. */
    public Face torusFace = null;

    /** Constructor. */
    public Edge(int n){
	super(n);
    }

    /** Constructor. */
    public Edge(){
	super();
    }

    /** print the edge. */
    public void print(String s){
	System.out.println(s + " v0.i " + v0.i + " v1.i " + v1.i);
    }

    public boolean isEdge(int i, int j){
        if((v0.i == i && v1.i == j) ||
           (v1.i == i && v0.i == j)){
            return true;
        }

        return false;
    }

    public Edge copy(){
	Edge newe = new Edge();
	for(int i = 0; i < 3; i++){
	    newe.cen[i] = cen[i];
	    newe.n[i] = n[i];
	}

	newe.angle = angle;
	newe.r = r;
	newe.v0 = v0;
	newe.v1 = v1;

	return newe;
    }
}

