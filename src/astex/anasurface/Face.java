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

/**
 * A face on the molecular surface.
 * Can be either convex, saddle or concave.
 * It contains a list of ordered edges that
 * are the boundary of the face.
 *
 * Each is processed in slightly different ways.
 */

public class Face extends DynamicArray {
    /** Type of face. */
    public int type = 0;

    /** Intsersection status. */
    public int intersection = 0;

    public static final int ProbeIntersection = 1;
    public static final int TorusIntersection = 2;

    /** Possible types of face. */
    public static final int Convex  = 1;
    public static final int Saddle  = 2;
    public static final int Concave = 3;
    public static final int Undefined = 4;

    /** Interpolation start on i. */
    double iij[] = null;

    /** Interpolation start on j. */
    double iji[] = null;

    /** Skip triangulation for this face. */
    public boolean skip = false;

    /** Sphere centre if Convex/Concave. */
    public double cen[] = new double[3];

    /** Sphere radius if Convex/Concave. */
    public double r = -1.0;

    /** Start angle for toroidal edges. */
    public double startAngle = 0.0;

    /** Stop angle for toroidal edges. */
    public double stopAngle = 0.0;

    /** Torus that this face belongs to for Saddle type. */
    public Torus torus = null;

    /** Constructor. */
    public Face(int t){
	super(4);
	type = t;

	if(type == Saddle){
	    iij = new double[3];
	    iji = new double[3];
	}
    }

    /** Must specify type in constructor. */
    private Face(){
    }

    public void add(Edge e){
	super.add(e);

	if(this.type == Face.Concave){
	    if(e.probeFace == null){
		e.probeFace = this;
	    }else{
		//System.out.println("adding edge to probe face again!!!");
	    }
	}
    }

    /** Is this face valid? */
    boolean isValid(){
	int edgeCount = size();
	Edge previous = (Edge)get(size() - 1);

	for(int i = 0; i < edgeCount; i++){
	    Edge e = (Edge)get(i);

	    if(e.v0 != previous.v1){
		System.out.println("face error");

		return false;
	    }

	    previous = e;
	}

	return true;
    }

    public void print(String s){
	System.out.println(s + " " + size() + " edges");
	for(int i = 0; i < size(); i++){
	    Edge e = (Edge)get(i);
	    System.out.println("v0.vi " + e.v0.vi + " v1.vi " + e.v1.vi);
	}
    }
}
