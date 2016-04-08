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

public class Torus {
    /** Index of first atom. */
    int i;

    /** Index of second atom. */
    int j;

    /** Torus center. */
    double tij[] = new double[3];

    /** Torus axis unit vector. */
    double uij[] = new double[3];

    /** Contact circle on i. */
    double cij[] = new double[3];

    /** Contact circle on j. */
    double cji[] = new double[3];

    /** Radius of contact circle on i. */
    double rcij = 0.0;

    /** Radius of contact circle on j. */
    double rcji = 0.0;

    /** Torus radius. */
    double rij = 0.0;

    /** Perpendicular to torus axis. */
    double uijnorm[] = new double[3];

    /** Second perpendicular to torus axis. */
    double uijnorm2[] = new double[3];

    /** Constructor. */
    public Torus(int ai, int aj){
	i = ai;
	j = aj;
    }

    /** List of probe placements that are on this torus. */
    DynamicArray probes = new DynamicArray(2);

    /** List of faces for this torus. */
    DynamicArray faces = new DynamicArray(10);

    /** List of faces for this torus. */
    DynamicArray edges = new DynamicArray(10);

    /** Does the torus self intersect. */
    boolean selfIntersects = false;
}
