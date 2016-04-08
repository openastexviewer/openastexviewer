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

/* Copyright Astex Technology Ltd. 1999 */

/*
 * 29-10-99 mjh
 *	created
 */

/**
 * A class for representing an arbitrary function that is to be optimized.
 *
 * It defines only one method, evaluate, which returns a the function
 * value.  It should also determine the function gradients and return
 * them in the gradients parameter.
 */
interface Function {
	/**
	 * Evaluate the function at the specified point.
	 *
	 * @param n The number of coordinates.
	 * @param x The coordinates at which the function is to be evaluated.
	 * @param gradients The gradient of the function at that point.
	 * @param user User defined data that can be used in calculations.
	 */
	double evaluate(int n, double x[],
					double gradient[], Object user);
}
