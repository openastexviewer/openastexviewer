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

public class Featurize {
    public static void handleCommand(MoleculeViewer mv,
                                     MoleculeRenderer mr,
                                     Arguments args){
        String mapName      = args.getString("-map", null);
        String molName      = args.getString("-molecule", null);
        double featureValue = args.getDouble("-level", 1.0);
        int neighbourCount  = args.getInteger("-neighbours", 7);

        if(mapName == null || molName == null){
            System.out.println("astex.Featurize: you must specify -map and -molecule");
            return;
        }

	Molecule featureMolecule = mr.getMolecule(molName);

	// or make it if it doesn't exist
	if(featureMolecule == null){
	    featureMolecule = new Molecule();
	    featureMolecule.setMoleculeType(Molecule.FeatureMolecule);
	    featureMolecule.setName(molName);
	    mv.addMolecule(featureMolecule);
	}

        featureMolecule.initialise();

        DynamicArray maps = mr.getMaps(mapName);

        int mapBoxSize[] = new int[3];

        for(int m = 0; m < maps.size(); m++){
            Map map = (Map)maps.get(m);

            map.getMapBoxDimensions(mapBoxSize);

            double rms = map.getSigma();

	    double level = featureValue * rms;

	    for(int i = 0; i < mapBoxSize[0]; i++){
		for(int j = 0; j < mapBoxSize[1]; j++){
		    for(int k = 0; k < mapBoxSize[2]; k++){
			double val = map.getValueAtRelativeGrid(i, j, k);
			if(val > level){
			    int highCount = 0;

			    for(int ii = -1; ii <= 1; ii++){
				for(int jj = -1; jj <= 1; jj++){
				    for(int kk = -1; kk <= 1; kk++){

					if(ii != 0 || jj != 0 || kk != 0){
					    double neighbourVal =
						map.getValueAtRelativeGrid(i + ii,
									   j + jj,
									   k + kk);
					    if(neighbourVal > val){
						highCount++;
					    }
					}
				    }
				}
			    }

			    if(highCount < neighbourCount){
				Atom a = featureMolecule.addAtom();

				a.setElement(PeriodicTable.UNKNOWN);
				a.setColor(Color32.thistle);
				map.relativeGridToCartesian(i, j, k, a);
			    }
			}
		    }
		}
	    }
	}
    }
}