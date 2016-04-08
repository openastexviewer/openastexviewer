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



/**
 * print class 
 * <p>
 * This class implements f() method which allows emulation of 
 * C style printf() routine
 *
 * @author mikeh
 * @version 1.0
 */
/**
	03-01-99 tmm	made class public (required for move to packages)
						made methods public
	06-09-98 tmm	added javadoc header comments/change comment section
	03-01-98 mh		created
*/

public class print {
    /*
     * Utility functions for printing.
     */
    
    public static void f(String output){
        System.out.println(output);
    }
    
    public static void f(Object o){
        System.out.println(o.toString());
    }
    
    public static void f(String s, double d){
        FILE.out.print(s, d);
    }
    
    public static void f(String s, float f){
        FILE.out.print(s, f);
    }
    
    public static void f(String s, int i){
        FILE.out.print(s, i);
    }
    
    public static void f(String s, char c){
        FILE.out.print(s, c);
    }
    
    public static void f(String s, String a){
        FILE.out.print(s, a);
    }
}
