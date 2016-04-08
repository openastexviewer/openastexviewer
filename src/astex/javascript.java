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

import java.lang.reflect.*;
import java.util.*;
import java.applet.*;
import astex.*;

//import netscape.javascript.JSObject;

public class javascript {
    public static void handleCommand(MoleculeViewer mv,
                                     MoleculeRenderer mr,
                                     Arguments args){
        String command = (String)args.get("-command");

        if(command != null){
            Applet applet = Util.getApplet(mv);

            String jsresult = null;
            boolean success = false;
            try {
                Method getw = null, eval = null;
                Object jswin = null;
                Class c =
                    Class.forName("netscape.javascript.JSObject"); /* does it in IE too */
                Method ms[] = c.getMethods();
                for (int i = 0; i < ms.length; i++) {
                    if (ms[i].getName().compareTo("getWindow") == 0)
                        getw = ms[i];
                    else if (ms[i].getName().compareTo("eval") == 0)
                        eval = ms[i];
                }

                Object a[] = new Object[1];
                a[0] = applet;               /* this is the applet */
                jswin = getw.invoke(c, a); /* this yields the JSObject */
                a[0] = command;
                Object result = eval.invoke(jswin, a);
                if (result instanceof String)
                    jsresult = (String) result;
                else
                    jsresult = result.toString();
                success = true;
            } catch (InvocationTargetException ite) {
                jsresult = "" + ite.getTargetException();
            }catch (Exception e) {
                jsresult = "" + e;
            }

            if (success)
                System.out.println("eval succeeded, result is " + jsresult);
            else
                System.out.println("eval failed with error " + jsresult);
            


//             Class cl = Class.forName("netscape.javascript.JSObject");
//             System.out.println("applet " + applet);
//             Object o = cl.getInstance();
//             JSObject js = JSObject.getWindow(applet);
//             js.eval(command);
        }
    }
}
