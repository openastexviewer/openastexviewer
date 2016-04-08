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

public class Spin extends Thread {
    MoleculeViewer moleculeViewer = null;
    MoleculeRenderer moleculeRenderer = null;

    static double angle = 0.5;

    static Thread spinner = null;

    public Spin(){
    }

    public Spin(MoleculeViewer mv, MoleculeRenderer mr){
        this();
        moleculeViewer = mv;
        moleculeRenderer = mr;
    }

    public static void handleCommand(MoleculeViewer mv,
                                     MoleculeRenderer mr,
                                     Arguments args){
        if(args.defined("-angle")){
            angle = args.getDouble("-angle", 0.5);
        }

        if(args.defined("-active")){
            if(args.getBoolean("-active", false)){
                if(spinner != null){
                    spinner.stop();
                    spinner = null;
                }
                
                Spin spin = new Spin(mv, mr);
                spinner = new Thread(spin);
                spinner.start();
            }else{
                spinner.stop();
                spinner = null;
            }
        }
    }

    /** Implement the Runnable interface. */
    public void run(){
        while(true){
            //moleculeRenderer.execute("view -rotatez " + angle +";");

            //moleculeViewer.dirtyRepaint();

            CommandThread.execute(moleculeRenderer,
                                  "spin",
                                  "view -rotatez " + angle +";");

            try {
                sleep(20);
            }catch(InterruptedException e){
            }
        }
    }
}