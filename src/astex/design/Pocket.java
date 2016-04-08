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

/**
 * An implementation of the Pocket identification algorithm of
 * An, Totrov and Abagyan.
 */

package astex.design;

import astex.*;

public class Pocket {
    public static void handleCommand(MoleculeViewer mv,
                                     MoleculeRenderer mr,
                                     Arguments args){
        DynamicArray atoms = (DynamicArray)args.get("-selection");
        DynamicArray boxAtoms = (DynamicArray)args.get("-boxatoms");
        // different border to original to allow for radius of atoms
        double border      = args.getDouble("-border", 3.0);
        double spacing     = args.getDouble("-spacing", 1.0);
        int iterations     = args.getInteger("-iterations", 10);
        double cutoff      = args.getDouble("-cutoff", -0.6);
        double contour     = args.getDouble("-contour", -0.75);
        double sigma       = args.getDouble("-sigma", 4.6);
        String probeType   = args.getString("-probetype", "6");
        String parameters  = args.getString("-parameters", "charmm19");

        if(atoms.size() == 0){
            Log.error("no atoms to generate Pockets for");
            return;
        }

        double min[] = new double[3];
        double max[] = new double[3];

        if(boxAtoms == null || boxAtoms.size() == 0){
            boxAtoms = atoms;
        }

        findExtents(boxAtoms, min, max, border);

        Point3d.print("min", min);
        Point3d.print("max", max);

        String mapName = args.getString("-name", "pocket");

        boolean newMap = false;
        astex.Map map = mr.getMap(mapName);

        if(map == null){
            newMap = true;
            map = astex.Map.createSimpleMap();
        }

        map.setName(mapName);
        map.setFile(mapName);

        map.origin.x = min[0];
        map.origin.y = min[1];
        map.origin.z = min[2];

        map.spacing.x = spacing;
        map.spacing.y = spacing;
        map.spacing.z = spacing;

        map.ngrid[0] = 1 + (int)(0.5 + (max[0] - min[0])/spacing);
        map.ngrid[1] = 1 + (int)(0.5 + (max[1] - min[1])/spacing);
        map.ngrid[2] = 1 + (int)(0.5 + (max[2] - min[2])/spacing);

        System.out.println("grid size " + map.ngrid[0] +
                         " " + map.ngrid[1] + " " + map.ngrid[2]);
        
        int gridPoints = map.ngrid[0] * map.ngrid[1] * map.ngrid[2];

        map.data = new float[gridPoints];

        String method = args.getString("-algorithm", "pocketfinder");

        if(method.equals("pocketfinder")){
            contour = fillMap(atoms, map, probeType, cutoff, iterations, sigma, parameters);
        }else if(method.equals("ligsite")){
            contour = args.getDouble("-contour", 6.0);
            double dummy = ligsite(atoms, map);
            smoothMap(map, iterations);
        }else{
            Log.error("no such method: " + method);
        }

        print.f("adding map");

        /*
        String commandPrefix = "map " + mapName + " contour 0 ";
        mr.execute(commandPrefix + (contour) + ";");
        mr.execute(commandPrefix + "on;");
        mr.execute(commandPrefix + "green;");
        mr.execute(commandPrefix + "wire;");
        */

        map.setContourDisplayed(0, true);
        map.setContourLevel(0, contour);
        map.setContourStyle(0, Map.Surface);
        map.setContourColor(0, Color32.green);
        map.setContourColor(1, 0x66ff66);

        if(newMap){
            mv.addMap(map);
        }

        if(method.equals("ligsite")){
            // make a molecule out of the contoured map
            Tmesh tm = mr.renderer.getGraphicalObject(mapName + "_0");
            print.f("tm " + tm);

            int npoints = tm.np;

            Molecule mol = new Molecule();

            for(int i = 0; i < npoints; i++){
                Atom a = mol.addAtom();
                a.setElement(0);
                a.setVDWRadius(1.4);
                a.set(tm.x[i], tm.y[i], tm.z[i]);
            }

            mol.setName(mapName + "_mol");

            mr.addMolecule(mol);
        }
    }

    /** Find the extent of the atoms and add in the border. */
    public static void findExtents(DynamicArray atoms,
                                   double min[], double max[],
                                   double border){
        int atomCount = atoms.size();

        for(int i = 0; i < 3; i++){
            min[i] = Double.POSITIVE_INFINITY;
            max[i] = Double.NEGATIVE_INFINITY;
        }

        for(int i = 0; i < atomCount; i++){
            Atom atom = (Atom)atoms.get(i);
            for(int j = 0; j < 3; j++){
                if(atom.get(j) < min[j]) min[j] = atom.get(j);
                if(atom.get(j) > max[j]) max[j] = atom.get(j);
            }
        }

        for(int j = 0; j < 3; j++){
            min[j] -= border;
            max[j] += border;
        }
    }

    /** Apply ligsite algorithm. */
    public static double ligsite(DynamicArray atoms, Map map){
        int nx = map.ngrid[0], ny = map.ngrid[1], nz = map.ngrid[2];
        int gridPoints = nx*ny*nz;

        int mask[] = new int[gridPoints];

        int box = 15;

        int atomCount = atoms.size();

        for(int a = 0; a < atomCount; a++){
            Atom atom = (Atom)atoms.get(a);
            double ax = atom.x;
            double ay = atom.y;
            double az = atom.z;
            double r = atom.getVDWRadius() + 1.4;
            r = r * r;

            int agx = (int)(0.5 + ((ax - map.origin.x)/map.spacing.x));
            int agy = (int)(0.5 + ((ay - map.origin.y)/map.spacing.y));
            int agz = (int)(0.5 + ((az - map.origin.z)/map.spacing.z));

            int xstart = Math.max(0,            agx - box);
            int xstop  = Math.min(map.ngrid[0], agx + box);
            int ystart = Math.max(0,            agy - box);
            int ystop  = Math.min(map.ngrid[1], agy + box);
            int zstart = Math.max(0,            agz - box);
            int zstop  = Math.min(map.ngrid[2], agz + box);

            for(int k = zstart; k < zstop; k++){
                double gz = map.origin.z + k * map.spacing.z;
                for(int j = ystart; j < ystop; j++){
                    double gy = map.origin.y + j * map.spacing.y;
                    for(int i = xstart; i < xstop; i++){
                        int v = index(i, j, k, nx, ny, nz);

                        if(mask[v] >= 0){
                            double gx = map.origin.x + i * map.spacing.x;

                            double dx = ax - gx;
                            double dy = ay - gy;
                            double dz = az - gz;
                            double d2 = dx*dx + dy*dy + dz*dz;
                            if(d2 < r){
                                mask[v] = -1;
                            }
                        }
                    }
                }
            }
        }
        
        if(true){
            
            int gp = 0;
            
            int dirs[][] = {
                { 1, 0, 0},{ 0, 1, 0},{ 0, 0, 1},
                { 1, 1, 1},{ 1, 1,-1},{ 1,-1, 1},{ 1,-1,-1}
            };  
            
            for(int k = 0; k < nz; k++){
                for(int j = 0; j < ny; j++){
                    for(int i = 0; i < nx; i++){
                        
                        if(mask[gp] >= 0){
                            for(int d = 0; d < 7; d++){
                                int dx = dirs[d][0];
                                int dy = dirs[d][1];
                                int dz = dirs[d][2];
                                if(psp_line(map, mask,
                                            i, j, k,
                                            dx, dy, dz) > 0){
                                    mask[gp]++;
                                }
                            }
                        }
                        
                        gp++;
                    }
                }
            }
        }

        print.f("copying data");

        int max = 0;

        for(int i = 0; i < gridPoints; i++){
            if(mask[i] > max) max = mask[i];
            if(mask[i] >= 0){
                map.data[i] = (float)mask[i];
            }else{
                map.data[i] = 0.0f;
            }
        }

        print.f("max " + max);

        return 6.0;
    }


    private static int psp_line(Map map, int mask[], int x,int y,int z, int dx, int dy, int dz){

        int ix,iy,iz,bump_flag;
        int incx,incy,incz;

        incx = dx;
        incy = dy;
        incz = dz;
  
        /* look in one direction */

        ix = x;
        iy = y;
        iz = z;
        bump_flag=0;
        while (within_limits(map, ix,iy,iz)){

            if (is_grid_point_set(map, mask, ix, iy, iz)) {
                bump_flag = 1;
                break;
            }
            ix += incx;
            iy += incy;
            iz += incz;
        }
        if (bump_flag == 0)
            return 0;
        
        /* look in the other direction */
        
        ix = x;
        iy = y;
        iz = z;
        bump_flag=0;
        while (within_limits(map, ix,iy,iz)){
            if (is_grid_point_set(map, mask, ix,iy,iz)) {
                bump_flag = 1;
                break;
            }
            ix -= incx;
            iy -= incy;
            iz -= incz;
        }
        if (bump_flag == 0)
            return 0;
        return 1;
    }

    private static boolean is_grid_point_set(Map map, int mask[], int i, int j, int k){
        int v = index(i, j, k, map.ngrid[0], map.ngrid[1], map.ngrid[2]);
        if(mask[v] < 0){
            return true;
        }
        return false;
    }

    private static boolean within_limits(Map map, int i, int j, int k){
        if(i < 0 || j < 0 || k < 0 ||
           i >= map.ngrid[0] || j >= map.ngrid[1] || k >= map.ngrid[2]){
            return false;
        }

        return true;
    }

    /** Smooth the map. */
    public static void smoothMap(Map map, int iterations){
        int nx = map.ngrid[0], ny = map.ngrid[1], nz = map.ngrid[2];
        int gridPoints = nx*ny*nz;
        float smooth[] = new float[gridPoints];

        for(int iter = 0; iter < iterations; iter++){
            for(int i = 0; i < nx; i++){
                for(int j = 0; j < ny; j++){
                    for(int k = 0; k < nz; k++){
                        int v = index(i, j, k, nx, ny, nz);
                        double pijk = map.data[v];
                        double pn = 0.0;
                        int nc = 0;
                        if(i > 0){
                            pn += map.data[index(i - 1, j, k, nx, ny, nz)];
                            nc++;
                        }
                        if(i < nx - 1){
                            pn += map.data[index(i + 1, j, k, nx, ny, nz)];
                            nc++;
                        }
                        if(j > 0){
                            pn += map.data[index(i, j - 1, k, nx, ny, nz)];
                            nc++;
                        }
                        if(j < ny - 1){
                            pn += map.data[index(i, j + 1, k, nx, ny, nz)];
                            nc++;
                        }
                        if(k > 0){
                            pn += map.data[index(i, j, k - 1, nx, ny, nz)];
                            nc++;
                        }
                        if(k < nz - 1){
                            pn += map.data[index(i, j, k + 1, nx, ny, nz)];
                            nc++;
                        }

                        smooth[v] = (float)((pijk + pn/(float)nc)/2.0);
                    }
                }
            }

            for(int gp = 0; gp < gridPoints; gp++){
                map.data[gp] = smooth[gp];
            }
        }
    }

    public static int index(int ix, int iy, int iz,
                            int nx, int ny, int nz){
        return iz * (nx * ny) + iy * nx + ix;
    }

    /** Fill map with charmm19 non-bonded interaction potential. */
    public static double fillMap(DynamicArray atoms, Map map,
                                 String probeType, double cutoff, int iterations, double sigma,
                                 String parameters){

        int nx = map.ngrid[0], ny = map.ngrid[1], nz = map.ngrid[2];
        int gridPoints = nx * ny * nz;

        for(int i = 0; i < gridPoints; i++){
            map.data[i] = 0.0f;
        }

        int atomCount = atoms.size();

        double Rprobe = Settings.getDouble(parameters, probeType + ".R", -1.0);
        double eprobe = Settings.getDouble(parameters, probeType + ".e", -1.0);

        print.f("Rprobe %f\n", Rprobe);
        print.f("eprobe %f\n", eprobe);
        
        int box = 1 + (int)(10.0/map.spacing.x);

        print.f("box %d\n", box);

        for(int a = 0; a < atomCount; a++){
            Atom atom = (Atom)atoms.get(a);
            double ax = atom.x;
            double ay = atom.y;
            double az = atom.z;

            Residue res = atom.getResidue();

            String name = res.getName() + "." + atom.getAtomLabel();
            String type = Settings.getString(parameters, name, probeType);

            double Ra = Settings.getDouble(parameters, type + ".R");
            double ea = Settings.getDouble(parameters, type + ".e");

            double Reff = Ra + Rprobe;
            double Reff12 = Math.pow(Reff, 12.0);
            double Reff6 = Math.pow(Reff, 6.0);
            double eeff = Math.sqrt(ea * eprobe);

            int agx = (int)(0.5 + ((ax - map.origin.x)/map.spacing.x));
            int agy = (int)(0.5 + ((ay - map.origin.y)/map.spacing.y));
            int agz = (int)(0.5 + ((az - map.origin.z)/map.spacing.z));

            int xstart = Math.max(0,            agx - box);
            int xstop  = Math.min(map.ngrid[0], agx + box);
            int ystart = Math.max(0,            agy - box);
            int ystop  = Math.min(map.ngrid[1], agy + box);
            int zstart = Math.max(0,            agz - box);
            int zstop  = Math.min(map.ngrid[2], agz + box);

            for(int k = zstart; k < zstop; k++){
                double gz = map.origin.z + k * map.spacing.z;
                for(int j = ystart; j < ystop; j++){
                    double gy = map.origin.y + j * map.spacing.y;
                    for(int i = xstart; i < xstop; i++){
                        double gx = map.origin.x + i * map.spacing.x;

                        double dx = ax - gx;
                        double dy = ay - gy;
                        double dz = az - gz;
                        double d2 = dx*dx + dy*dy + dz*dz;
                        // only if less than 10A
                        if(d2 < 160.0){
                            double d6 = d2*d2*d2;
                            double d12 = d6*d6;

                            double e = eeff * (Reff12/d12 - 2.0*Reff6/d6);
                            //double e = (90.61e4/d12 - 370.5/d6);
                            
                            int v = index(i, j, k, nx, ny, nz);

                            map.data[v] += e;
                        }
                    }
                }
            }
        }

        double emin = Double.POSITIVE_INFINITY;
        double emax = Double.NEGATIVE_INFINITY;

        boolean good[] = new boolean[gridPoints];
        int ngood = 0;

        for(int i = 0; i < gridPoints; i++){
            if(map.data[i] < emin) emin = map.data[i];
            if(map.data[i] > emax) emax = map.data[i];
            if(map.data[i] > cutoff){
                map.data[i] = (float)cutoff;
                good[i] = false;
            }else{
                good[i] = true;
                ngood++;
            }
        }

        // to override positive regions
        ngood = gridPoints;

        smoothMap(map, iterations);

        print.f("ngood %d\n", ngood);

        double mean = 0.0;
        double rms  = 0.0;

        for(int i = 0; i < gridPoints; i++){
            //if(good[i]){
                mean += map.data[i];
                //}
        }

        mean /= (float)ngood;

        for(int i = 0; i < gridPoints; i++){
            //if(good[i]){
                float v = (float)(map.data[i] - mean);
                rms  += v * v;
                //}
        }

        rms = Math.sqrt(rms/(float)ngood);

        print.f("mean %f\n", mean);
        print.f("rms  %f\n", rms);

        print.f("emin %f\n", emin);
        print.f("emax %f\n", emax);

        print.f("contour is " + (mean  - sigma * rms));

        map.setContourLevel(0, mean - sigma * rms);
        map.setContourLevel(1, emin);

        return mean - sigma * rms;
    }
}