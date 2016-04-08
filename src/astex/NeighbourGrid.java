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

public class NeighbourGrid {
    /** The maximum number of cells along an edge. */
    private static final int MaxDim = 64;

    /** The default expected number of objects. */
    private static final int DefaultObjectCount = 512;

    /** The minimum coordinate of the box.. */
    private double xmin = 0.0;
    private double ymin = 0.0;
    private double zmin = 0.0;

    /** The maximum coordinate of the box. */
    private double xmax = 0.0;
    private double ymax = 0.0;
    private double zmax = 0.0;

    /** The minimum distance we wish to search. */
    private double spacing = 0.0;

    /** The number of boxes along each edge. */
    private int nx = 0;
    private int ny = 0;
    private int nz = 0;

    /** The total number of grid boxes. */
    private int ncell = 0;

    /** The list of cell positions for atoms. */
    private IntArray list = null;

    /** The head pointers for each cell. */
    private int head[] = null;

    /**
     * The cell offsets for the half space that generates
     * icell2 > icell1.
     */
    private int offsets[][] = {
	{-1, -1, -1},
	{ 0, -1, -1},
	{ 1, -1, -1},
	{-1,  0, -1},
	{ 0,  0, -1},
	{ 1,  0, -1},
	{-1,  1, -1},
	{ 0,  1, -1},
	{ 1,  1, -1},
	{-1, -1,  0},
	{ 0, -1,  0},
	{ 1, -1,  0},
	{-1,  0,  0},
    };

    /**
     * Construct a neighbour grid with the following specification.
     */
    public NeighbourGrid(double xmin, double ymin, double zmin,
			 double xmax, double ymax, double zmax,
			 double dmin, int sizeHint){
	this.xmin = xmin;
	this.ymin = ymin;
	this.zmin = zmin;
	
	this.xmax = xmax;
	this.ymax = ymax;
	this.zmax = zmax;
	
	this.spacing = dmin;

	nx = 1 + (int)((xmax - xmin) / dmin);
	ny = 1 + (int)((ymax - ymin) / dmin);
	nz = 1 + (int)((zmax - zmin) / dmin);

	if(nx > MaxDim || ny > MaxDim || nz > MaxDim){
	    System.out.println("resetting spacing from " + spacing);
	    double biggest = xmax - xmin;
	    if(ymax - ymin > biggest) biggest = ymax - ymin;
	    if(zmax - zmin > biggest) biggest = zmax - zmin;
	    
	    spacing = biggest / (MaxDim + 1);

	    System.out.println("setting dmin to     " + spacing);
	    System.out.println("nx = " + nx + " ny = " + ny + " nz = " + nz);

	    nx = 1 + (int)((xmax - xmin) / spacing);
	    ny = 1 + (int)((ymax - ymin) / spacing);
	    nz = 1 + (int)((zmax - zmin) / spacing);
	}

	ncell = nx * ny * nz;

	if(sizeHint == -1){
	    sizeHint = DefaultObjectCount;
	}

	head = new int[ncell];
	list = new IntArray(sizeHint);

	// initialise cell head pointers
	// -1 shows that the cell is empty
	for(int i = 0; i < ncell; i++){
	    head[i] = -1;
	}
    }

    /** Generate a cell index for the given grid. */
    private int cellIndex(int ix, int iy, int iz,
			  int nx, int ny, int nz){
	return ix + iy * nx + iz * nx * ny;
    }

    /** Add an object to the appropriate cell. */
    public void add(int i, double x, double y, double z){
	if(x < xmin || y < ymin || z < zmin ||
	   x > xmax || y > ymax || z > zmax){
	    System.out.println("NeighbourGrid.add(): unable to add " + i +
			       " coordinate outside of box");
	    return;
	}

	int ix = (int)((x - xmin)/spacing);
	int iy = (int)((y - ymin)/spacing);
	int iz = (int)((z - zmin)/spacing);
	int icell = cellIndex(ix, iy, iz, nx, ny, nz);

	if(icell < 0 || icell >= ncell){
	    System.out.println("invalid cell " + icell + " for object " + i);
	    return;
	}

	list.set(i, head[icell]);
	head[icell] = i;
    }

    /**
     * Return the possible neighbours of the point.
     */
    public int getPossibleNeighbours(int id,
				     double x, double y, double z,
				     IntArray neighbours,
				     boolean allNeighbours){
	int ibox = ((int)((x - xmin)/spacing));
	int jbox = ((int)((y - ymin)/spacing));
	int kbox = ((int)((z - zmin)/spacing));

	int l[] = list.getArray();

	for(int i = -1; i <= 1; i++){
	    int ii = ibox + i;
	    for(int j = -1; j <= 1; j++){
		int jj = jbox + j;
		for(int k = -1; k <= 1; k++){
		    int kk = kbox + k;
		    int c = findcell(ii, jj, kk);

		    if(c != -1){
			int iobj = head[c];

			if(iobj != -1){
			    if(allNeighbours){
				if(id == -1){
				    while(iobj >= 0){
					neighbours.add(iobj);
					iobj = l[iobj];
				    }
				}else{
				    while(iobj >= 0){
					// don't put ourselves
					// in the list of neighbours
					if(iobj != id){
					    //System.out.println(".add");
					    neighbours.add(iobj);
					}
					iobj = l[iobj];
				    }
				}
			    }else{
				while(iobj >= 0){
				    // don't put things less than us
				    // in the list of neighbours
				    if(iobj > id){
					//System.out.println(".add");
					neighbours.add(iobj);
				    }
				    iobj = l[iobj];
				}
			    }
			}
		    }
		}
	    }
	}

	return neighbours.size();
    }

    /** Get the contents of the cell. */
    public int getCellContents(int icell, IntArray c){
	if(icell == -1){
	    return 0;
	}

	int nc = 0;
	int j = head[icell];

	if(j == -1){
	    return 0;
	}

	while(j >= 0){
	    c.add(list.get(j));
	    j = list.get(j);
	}

	return c.size();
    }

    /** Find the cell that corresponds to the id's. */
    private int findcell(int i, int j, int k){
	if(i < 0   || j < 0   || k < 0) return -1;
	if(i >= nx || j >= ny || k >= nz) return -1;

	int hashval = cellIndex(i, j, k, nx, ny, nz);

	return hashval;
    }

}
