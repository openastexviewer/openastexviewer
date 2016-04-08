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

public class Animate extends Thread {
	/** The list of animation objects. */
	public Vector stages = null;

	/** The MoleculeViewer we are associated with. */
	public MoleculeViewer moleculeViewer = null;

	/** Should we maintain interactive display. */
	private boolean interactive      = true;

	/** Set whether we are in interactive mode. */
	public void setInteractive(boolean b){
		interactive = b;
	}

	/** Are we interactive. */
	public boolean getInteractive(){
		return interactive;
	}

	/** Set the MoleculeViewer. */
	public void setMoleculeViewer(MoleculeViewer mv){
		moleculeViewer = mv;
	}

	/** Set the stages. */
	public void setStages(Vector v){
		stages = v;
	}

	/** Return the total number of steps in the animation. */
	public int getStepCount(){
		int totalSteps = 0;

		int stageCount = stages.size();

		Log.info("stages %3d", stageCount);

		for(int s = 0; s < stageCount; s++){

			AnimationObject stage = (AnimationObject)stages.elementAt(s);

			totalSteps += stage.getSteps();
		}

		return totalSteps;
	}

	/** The run method. */
	public void run(){
		int stageCount = stages.size();

		Log.info("stages %3d", stageCount);

		for(int s = 0; s < stageCount; s++){

			AnimationObject stage = (AnimationObject)stages.elementAt(s);

			stage.setAnimateThread(this);

			stage.initialise();

			Log.info("stage %3d", s);

			while(stage.executeAnimationFunction()){

				yield();

				if(getInteractive()){
					moleculeViewer.dirtyRepaint();
				}else{
					MoleculeRenderer mr = moleculeViewer.getMoleculeRenderer();
					mr.execute("view -writeimage 'default';");
				}

				try {
					int sleepDuration = stage.getSleepDuration();
					//System.out.println("sleepDuration " + sleepDuration);
					sleep(sleepDuration);
				}catch(Exception e){
				}
			}
		}

		moleculeViewer.removeAnimationThread(this);
	}
}
