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
 * 23-11-99 mjh
 *	created
 */

/**
 * A class for contouring numeric grids in 3-dimensions.
 */
public class Contour {
    /**
     * Main entry point for contouring methods.
     * 
     * data is assumed to be organized such that nx is running fastest.
     */
    public static void contour(float data[],
			       int nx, int ny, int nz,
			       double level,
			       Tmesh contourObject){

	//contourObject.setColor(Color32.yellow);
	for(int z = 0; z < nz; z++){
	    contourSection(data, nx, ny, nz, z, level, contourObject);
	}
    }

    /** Generate an index from a grid coordinate. */
    private static int index(int x, int y, int z, int nx, int ny, int nz){
	return (z * nx * ny) + (y * nx) + x;
    }

    /** Contour one section of the data. */
    private static void
	contourSection(float data[], int nx, int ny, int nz, int z,
		       double level, Tmesh contourObject){
	int nxy = nx * ny;
	int mask = 0;

	for(int y = 0; y < ny; y++){
	    for(int x = 0; x < nx; x++){
		int index1, index2, index3, index4;
		float v1, v2, v3, v4;
		index1 = index(x,   y,   z, nx, ny, nz);
		v1 = data[index1];

		if(y != ny - 1 && x != nx - 1){
		    //index2 = index(x+1, y,   z, nx, ny, nz);
		    index2 = index1 + 1;
		    //index3 = index(x+1, y+1, z, nx, ny, nz);
		    index3 = index1 + 1 + nx;
		    //index4 = index(x,   y+1, z, nx, ny, nz);
		    index4 = index1 + nx;
		    v2 = data[index2];
		    v3 = data[index3];
		    v4 = data[index4];
		    mask = 0;
		    if(level > v1) mask |= 1;
		    if(level > v2) mask |= 2;
		    if(level > v3) mask |= 4;
		    if(level > v4) mask |= 8;

		    contourOneFace(x,   y,   z, v1,
				   x+1, y,   z, v2,
				   x+1, y+1, z, v3,
				   x,   y+1, z, v4,
				   mask, level, contourObject);
		}
		if(y != ny - 1 && z != nz - 1){
		    //index2 = index(x,   y+1, z,   nx, ny, nz);
		    index2 = index1 + nx;
		    //index3 = index(x,   y+1, z+1, nx, ny, nz);
		    index3 = index1 + nx + nxy;
		    //index4 = index(x,   y,   z+1, nx, ny, nz);
		    index4 = index1 + nxy;
		    v2 = data[index2];
		    v3 = data[index3];
		    v4 = data[index4];
		    mask = 0;
		    if(level > v1) mask |= 1;
		    if(level > v2) mask |= 2;
		    if(level > v3) mask |= 4;
		    if(level > v4) mask |= 8;
		    contourOneFace(x,   y,   z, v1,
				   x,   y+1, z, v2,
				   x,   y+1, z+1, v3,
				   x,   y,   z+1, v4,
				   mask, level, contourObject);
		}
		if(x != nx - 1 && z != nz - 1){
		    //index2 = index(x+1, y,   z,   nx, ny, nz);
		    index2 = index1 + 1;
		    //index3 = index(x+1, y,   z+1, nx, ny, nz);
		    index3 = index1 + 1 + nxy;
		    //index4 = index(x,   y,   z+1, nx, ny, nz);
		    index4 = index1 + nxy;
		    v2 = data[index2];
		    v3 = data[index3];
		    v4 = data[index4];
		    mask = 0;
		    if(level > v1) mask |= 1;
		    if(level > v2) mask |= 2;
		    if(level > v3) mask |= 4;
		    if(level > v4) mask |= 8;
		    contourOneFace(x,   y,   z, v1,
				   x+1, y,   z, v2,
				   x+1, y,   z+1, v3,
				   x,   y,   z+1, v4,
				   mask, level, contourObject);
		}
	    }
	}
    }

    /** Contour one face. Vertices anticlockwise from bottom left. */
    private static  void
	contourOneFace(int x1, int y1, int z1, float v1,
		       int x2, int y2, int z2, float v2,
		       int x3, int y3, int z3, float v3,
		       int x4, int y4, int z4, float v4,
		       int mask, double level, Tmesh contourObject){
	//int mask = 0;
	//if(level > v1) mask |= 1;
	//if(level > v2) mask |= 2;
	//if(level > v3) mask |= 4;
	//if(level > v4) mask |= 8;
	
	switch(mask){
	case 0: case 15: /* no contour. */ break;
	case 1: case 14:
	    addLine(x1, y1, z1, v1, x2, y2, z2, v2,
		    x1, y1, z1, v1, x4, y4, z4, v4, level, contourObject);
	    break;
	case 2: case 13:
	    addLine(x1, y1, z1, v1, x2, y2, z2, v2,
		    x2, y2, z2, v2, x3, y3, z3, v3, level, contourObject);
	    break;
	case 4: case 11:
	    addLine(x2, y2, z2, v2, x3, y3, z3, v3,
		    x3, y3, z3, v3, x4, y4, z4, v4, level, contourObject);
	    break;
	case 8: case 7:
	    addLine(x3, y3, z3, v3, x4, y4, z4, v4,
		    x4, y4, z4, v4, x1, y1, z1, v1, level, contourObject);
	    break;
	case 3: case 12:
	    addLine(x2, y2, z2, v2, x3, y3, z3, v3,
		    x4, y4, z4, v4, x1, y1, z1, v1, level, contourObject);
	    break;
	case 6: case 9:
	    addLine(x1, y1, z1, v1, x2, y2, z2, v2,
		    x4, y4, z4, v4, x3, y3, z3, v3, level, contourObject);
	    break;
	case 5: case 10:
	    // needs to take account of center value
	    double mean = 0.25 * (v1 + v2 + v3 + v4);
	    if(mean > level && v1 > level){
		addLine(x1, y1, z1, v1, x2, y2, z2, v2,
			x2, y2, z2, v2, x3, y3, z3, v3, level, contourObject);
		addLine(x3, y3, z3, v3, x4, y4, z4, v4,
			x4, y4, z4, v4, x1, y1, z1, v1, level, contourObject);
	    }else{
		addLine(x1, y1, z1, v1, x2, y2, z2, v2,
			x4, y4, z4, v4, x1, y1, z1, v1, level, contourObject);
		addLine(x3, y3, z3, v3, x4, y4, z4, v4,
			x2, y2, z2, v2, x3, y3, z3, v3, level, contourObject);
	    }
	    break;
	}
    }
	
    /** Add one line to the contour object. */
    private static void
	addLine(int x1, int y1, int z1, float v1,
		int x2, int y2, int z2, float v2,
		int x3, int y3, int z3, float v3,
		int x4, int y4, int z4, float v4,
		double level, Tmesh contourObject){
	int point1 = addIntersection(x1, y1, z1, v1,
				     x2, y2, z2, v2, level, contourObject);
	int point2 = addIntersection(x3, y3, z3, v3,
				     x4, y4, z4, v4, level, contourObject);
	// fix
	contourObject.addLine(point1, point2, 0);
    }

    /** Add one intersection. */
    private static int
	addIntersection(int x1, int y1, int z1, float v1,
			int x2, int y2, int z2, float v2,
			double level, Tmesh contourObject){
	double fraction = (level - v1)/(v2 - v1);
	double x = x1 + fraction * (x2 - x1);
	double y = y1 + fraction * (y2 - y1);
	double z = z1 + fraction * (z2 - z1);
	int vertex = findVertex(contourObject, x, y, z);
	if(vertex == VertexNotFound){
	    contourObject.addPoint(x, y, z, 0);
	    return contourObject.np - 1;
	}else{
	    return vertex;
	}
    }

    /** A small number. */
    private static double tolerance = 1.0e-3;
	
    /** Vertex wasn't found in the object. */
    private static int VertexNotFound = -1;

    /** Where we left off looking for the vertex. */
    private static int findVertex(Tmesh object,
				  double x, double y, double z){
	int pointCount = object.np;
	float objectx[] = object.x;
	float objecty[] = object.y;
	float objectz[] = object.z;

	for(int i = pointCount - 1; i >= 0; i--){
	    if(objectz[i] < z - 1.1){
		return VertexNotFound;
	    }else{
		double dx = objectx[i] - x;
		if(dx < 0.0){
		    dx = -dx;
		}
		if(dx < tolerance){
		    double dy = Math.abs(objecty[i] - y);
		    if(dy < tolerance){
			double dz = Math.abs(objectz[i] - z);
			if(dz < tolerance){
			    return i;
			}
		    }
		}
	    }
	}

	return VertexNotFound;
    }
}
