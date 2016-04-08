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

public class Probe {
    /** The coordinates of the probe placement. */
    public double x[] = new double[3];

    /** The coordinates of the base point placement. */
    public double bijk[] = new double[3];

    /**
     * The radius of the probe.
     * Nearly always the solvent sphere radius.
     */
    public double r = 0.0;

    /**
     * The burial count.
     */
    public int bc = 0;

    /** The first edge. */
    public Edge edge0 = null;

    /** The second edge. */
    public Edge edge1 = null;

    /** The third edge. */
    public Edge edge2 = null;

    /** The first contact atom. */
    public int i;

    /** The second contact atom. */
    public int j;

    /** The third contact atom. */
    public int k;

    DynamicArray clippingProbes = null;

    public void addClippingProbe(Probe p){
	if(clippingProbes == null){
	    clippingProbes = new DynamicArray(2);
	}

	clippingProbes.add(p);
    }

    public boolean involves(int iatom){
        if(i == iatom || j == iatom || k == iatom){
            return true;
        }

        return false;
    }

    public Vertex getVertexForAtom(int iatom){
        if(edge0.v0.i == iatom) return edge0.v0;
        if(edge1.v0.i == iatom) return edge1.v0;
        if(edge2.v0.i == iatom) return edge2.v0;

        return null;
    }

    public Edge getEdgeTo(int i){
        if(edge0.v1.i == i) return edge0;
        if(edge1.v1.i == i) return edge1;
        if(edge2.v1.i == i) return edge2;

        return null;
    }

    public Edge getEdgeFrom(int i){
        if(edge0.v0.i == i) return edge0;
        if(edge1.v0.i == i) return edge1;
        if(edge2.v0.i == i) return edge2;

        return null;
    }

    public Edge getEdge(int i, int j){

        if(edge0.isEdge(i, j)){
            return edge0;
        }
        if(edge1.isEdge(i, j)){
            return edge1;
        }
        if(edge2.isEdge(i, j)){
            return edge2;
        }

        return null;
    }
}
