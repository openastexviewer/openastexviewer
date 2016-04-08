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

import java.util.*;
import java.io.*;
import astex.parser.*;

class CommandThread extends Thread {
    static Hashtable threads = new Hashtable();

    String name = null;

    MoleculeRenderer mr = null;
    parser p = null;
    Yylex l = null;
    
    public Vector commandQueue = new Vector();

    public static synchronized void execute(MoleculeRenderer mr,
                                            String threadName,
                                            String command){
        CommandThread t = (CommandThread)threads.get(threadName);

        if(t == null || t.isAlive() == false){
            t = new CommandThread(mr);
            t.name = threadName;
            threads.put(threadName, t);
            t.start();
        }

        t.commandQueue.addElement(command);

        t.resume();
    }

    public CommandThread(MoleculeRenderer m){
        super();
        mr = m;
        p = new parser();
        l = new Yylex((java.io.BufferedReader)null);
    }

    public void run(){
        while(true){
            String command = null;
            if(commandQueue.isEmpty() == false){
                //print.f("queue length " + commandQueue.size());

                command = (String)commandQueue.elementAt(0);
                commandQueue.removeElementAt(0);

                p.setMoleculeRenderer(mr);

                p.setScanner(l);
                l.setInput(new StringReader(command));

                try {
                    p.parse();
                    mr.repaint();
                }catch(Exception e){
                    print.f("thread " + name + " error executing:");
                    print.f(command);
                    e.printStackTrace();
                }
            }else{
                //print.f("suspending");
                suspend();
            }
        }
    }
}