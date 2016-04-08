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

package astex.thinlet;

import astex.*;
import astex.splitter.*;
import astex.generic.*;
import thinlet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class test extends Thinlet {

    public test() throws Exception {
        add(parse("/test.xml"));
    }
    
    /**
     * Creates a frame including this thinlet demo
     */
    public static void main(String[] args) throws Exception {
        new FrameLauncher(new Frame(), "Demo", new test(), 320, 320);
    }

    public void paintTest(Thinlet thinlet, Object component, Graphics g, int w, int h){
        g.setColor(Color.cyan);
        
        g.fillRect(0, 0, w, h);
    }
}
