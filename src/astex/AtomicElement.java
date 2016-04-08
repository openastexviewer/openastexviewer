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


/*
	AtomicElement class

	used by PTable class for storing element info
*/

import java.util.*;
import java.io.*;

public class AtomicElement {
	/** The maximum number of valence states that any of our atoms exist in. */
	
	public static final int MAX_VALENCE_STATES = 4;
	
	public String symbol;

	int symbolLen;

	public int atomicNumber;
	double mass;
	int valences[];
	
	public int drow;
	public int dcol;

	public AtomicElement(int an, String s, double m,
						 int vv1, int vv2, int vv3, int vv4,
						 int ddrow, int ddcol){
		atomicNumber = an;
		symbol = s;
		
		mass = m;
		valences = new int[MAX_VALENCE_STATES];
		valences[0] = vv1;
		valences[1] = vv2;
		valences[2] = vv3;
		valences[3] = vv4;
		drow = ddrow;
		dcol = ddcol;
	}
}
