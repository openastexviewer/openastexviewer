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
 * Class for performing near neighbour calculations.
 */
package astex;

public class Lattice {
    /** List of cells to search for all pairs search. */
    private static int offsets[][] = {
	{  1, -1, 0 },
	{  1,  0, 0 },
	{  1,  1, 0 },
	{  0,  1, 0 },
	{ -1, -1, 1 },
	{  0, -1, 1 },
	{  1, -1, 1 },
	{ -1,  0, 1 },
	{  0,  0, 1 },
	{  1,  0, 1 },
	{ -1,  1, 1 },
	{  0,  1, 1 },
	{  1,  1, 1 }
    };

    /** Maximum distance we will search. */
    private double maxDistance = -1.0;

    /** Set the maximum search distance. */
    public void setMaximumDistance(double d){
	Log.check(d > 0.0, "search distance must be > 0.0");

	if(maxDistance > 0.0){
	    Log.error("can't reset maximum distance");
	}else{
	    maxDistance = d;
	}
    }

    /** Return the maximum search distance. */
    public double getMaximumDistance(){
	return maxDistance;
    }

    /** Return the number of objects. */
    public int getObjectCount(){
	return list.size();
    }

    /** Create an empty lattice object. */
    public Lattice(double d){
	setMaximumDistance(d);
    }

    /** Get the number of occupied cells. */
    public int getCellCount(){
	return celli.size();
    }

    /** Add an object reference to the lattice. */
    public void add(int id, double x, double y, double z){
	int i = BOX(x);
	int j = BOX(y);
	int k = BOX(z);

	// map into an internal id range
	// so that we can trivially handle
	// non-contiguous ids
	int actualId = ids.size();

	ids.add(id);

	int c = findcell(i, j, k);

	if(c == -1){
	    int hashval = HASH(i, j, k);
	    IntArray cellList = hashTable[hashval];

	    if(cellList == null){
		cellList = new IntArray();
		hashTable[hashval] = cellList;
	    }

	    c = celli.size();

	    cellList.add(c);
	    // -1 indicates the cell is currently empty
	    // this will get changed below
	    head.add(-1);

	    celli.add(i);
	    cellj.add(j);
	    cellk.add(k);
	}
	
	// shuffle the object id's
	// to add the new object to the cell chain
	list.add(head.get(c));
	head.set(c, actualId);

	//FILE.out.print("final cell=%3d\n", c);
    }

    /** Get the contents of the cell. */
    public int getCellContents(int icell, IntArray c){
	if(icell == -1){
	    return 0;
	}

	int nc = 0;
	int j = head.get(icell);

	if(j == -1){
	    return 0;
	}

	while(j >= 0){
	    c.add(ids.get(j));
	    j = list.get(j);
	}

	return c.size();
    }

    /** Find the cell that corresponds to the id's. */
    private int findcell(int i, int j, int k){
	int hashval = HASH(i, j, k);

	//Log.assert(hashval >= 0 && hashval < HASHTABLESIZE,
	//	   "invalid hash value");

	// search list with this hashval, if not present return -1
	IntArray cellList = hashTable[hashval];

	if(cellList != null){
	    int ci[] = celli.getArray();
	    int cj[] = cellj.getArray();
	    int ck[] = cellk.getArray();
	    int cl[] = cellList.getArray();
	    int cellEntries = cellList.size();

	    for(int c = 0; c < cellEntries; c++){
		int cellIndex = cl[c];

		if(ci[cellIndex] == i &&
		   cj[cellIndex] == j &&
		   ck[cellIndex] == k){
		    return cellIndex;
		}
	    }
	}

	return -1;
    }

    /**
     * Return the possible neighbours of the point.
     */
    public int getPossibleNeighbours(int id,
				     double x, double y, double z,
				     IntArray neighbours,
				     boolean allNeighbours){
	int ibox = BOX(x);
	int jbox = BOX(y);
	int kbox = BOX(z);

	int h[] = head.getArray();
	int l[] = list.getArray();
	int idsArray[] = ids.getArray();

	for(int i = -1; i <= 1; i++){
	    int ii = ibox + i;
	    for(int j = -1; j <= 1; j++){
		int jj = jbox + j;
		for(int k = -1; k <= 1; k++){
		    int kk = kbox + k;
		    int c = findcell(ii, jj, kk);

		    if(c != -1){
			int iobj = h[c];

			if(iobj != -1){
			    if(allNeighbours){
				if(id == Undefined){
				    while(iobj >= 0){
					//System.out.println(".add");
					neighbours.add(idsArray[iobj]);
					iobj = l[iobj];
				    }
				}else{
				    while(iobj >= 0){
					// don't put ourselves
					// in the list of neighbours
					if(idsArray[iobj] != id){
					    //System.out.println(".add");
					    neighbours.add(idsArray[iobj]);
					}
					iobj = l[iobj];
				    }
				}
			    }else{
				while(iobj >= 0){
				    // don't put things less than us
				    // in the list of neighbours
				    if(idsArray[iobj] > id){
					//System.out.println(".add");
					neighbours.add(idsArray[iobj]);
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

    /** Working space for cell objects gathers. */
    private IntArray cell1 = new IntArray();
    private IntArray cell2 = new IntArray();

    /** Get the possible pairs of neighbours from a cell. */
    public int getPossibleCellNeighbours(int cid, IntArray objects){
	cell1.removeAllElements();
	getCellContents(cid, cell1);

	int count1 = cell1.size();
	int c1[] = cell1.getArray();

	for(int i = 0; i < count1; i++){
	    int oi = c1[i];
	    for(int j = 0; j < count1; j++){
		if(i != j){
		    int oj = c1[j];
		    if(oi < oj){
			objects.add(oi);
			objects.add(oj);
		    }
		}
	    }
	}

	int icell = celli.get(cid);
	int jcell = cellj.get(cid);
	int kcell = cellk.get(cid);

	for(int ioff = 0; ioff < offsets.length; ioff++){
	    int ii = icell + offsets[ioff][0];
	    int jj = jcell + offsets[ioff][1];
	    int kk = kcell + offsets[ioff][2];

	    int c = findcell(ii, jj, kk);

	    if(c != -1){
		cell2.removeAllElements();
		getCellContents(c, cell2);
		int count2 = cell2.size();
		int c2[] = cell2.getArray();

		for(int i = 0; i < count1; i++){
		    int oi = c1[i];
		    for(int j = 0; j < count2; j++){
			int oj = c2[j];
			objects.add(ids.get(oi));
			objects.add(ids.get(oj));
		    }
		}
	    }
	}

	return objects.size();
    }

    /** Print info about the Lattice object. */
    public void printStatistics(int info){
	int occupiedHashSlots = 0;
	int minCells = Integer.MAX_VALUE;
	int maxCells = Integer.MIN_VALUE;
	int zeroCells = 0;

	for(int i = 0; i < HASHTABLESIZE; i++){
	    if(hashTable[i] != null){
		occupiedHashSlots++;
		IntArray cellList = hashTable[i];
		int cellCount = cellList.size();

		if(cellCount > maxCells){
		    maxCells = cellCount;
		}
		if(cellCount < minCells){
		    minCells = cellCount;
		}
	    }else{
		zeroCells++;
	    }
	}

	FILE.out.print("hash table size %5d\n", HASHTABLESIZE);
	FILE.out.print("occupied cells  %5d\n", occupiedHashSlots);
	FILE.out.print("zero cells      %5d\n", zeroCells);
	FILE.out.print("max cells/slot  %5d\n", maxCells);
	FILE.out.print("ave cells/slot  %7.1f\n",
		       (double)celli.size()/(double)HASHTABLESIZE);
    }

    /** Return hash value for object cell. */
    private int HASH(int i, int j, int k){
	if(i < 0) i = -i;
	if(j < 0) j = -j;
	if(k < 0) k = -k;

	return (i&HS_MASK) | ((j&HS_MASK) << SHIFT) | ((k&HS_MASK) << SHIFT2);
    }

    /** Return the cell box id along one axis. */
    public int BOX(double x){
	if(x > 0.0){
	    return (int)(x/maxDistance);
	}else{
	    return (int)(x/maxDistance)-1;
	}
    }

    /** Size of Hash box. */
    private static final int HS = 16;

    /** Mask for mapping into Hash box range. */
    private static final int HS_MASK = HS - 1;

    /** Shift for mapping into Hash box range [ln2(HS)]. */
    private static final int SHIFT = 4;

    /** Shift for mapping into Hash box range [ln2(HS)]. */
    private static final int SHIFT2 = 2 * SHIFT;

    /** Size of hashtable. */
    private static final int HASHTABLESIZE = HS*HS*HS;

    /** Table of indexes to cells. */
    private IntArray hashTable[] = new IntArray[HASHTABLESIZE];

    /** The cell indexes for each cell. */
    private IntArray celli = new IntArray();
    private IntArray cellj = new IntArray();
    private IntArray cellk = new IntArray();

    /** The head and list pointers for each cell. */
    private IntArray head  = new IntArray();
    private IntArray list  = new IntArray();

    /** Mapping from passed ids to internal ids. */
    private IntArray ids   = new IntArray();

    /** Constant to indicate we don't care about ids. */
    public final static int Undefined = Integer.MIN_VALUE;

    /**
     * The coordinates for the points.
     *
     * Without a universal data type for points,
     * it is difficult to see how to provide
     * useful functionality without storing the
     * object coordinates.
     */
    //private DoubleArray objx = new DoubleArray();
    //private DoubleArray objy = new DoubleArray();
    //private DoubleArray objz = new DoubleArray();

    /** Number of objects we have in the Lattice. */
    private int objectCount = 0;

    /**
     * Test harness for the lattice construction.
     */
    public static void main(String args[]){
	if(args.length > 0){
	    Molecule mol = MoleculeIO.read(args[0]);
	    double gridSize = 5.0;

	    if(args.length == 2){
		gridSize = FILE.readDouble(args[1]);
	    }

	    FILE.out.print("grid size         %.1f\n", gridSize);

	    int atomCount = mol.getAtomCount();

	    Util.startTimer(0);

	    Lattice l = new Lattice(gridSize * 1.05);

	    for(int a = 0; a < atomCount; a++){
		Atom atom = mol.getAtom(a);

		l.add(a, atom.x, atom.y, atom.z);
	    }

	    Util.stopTimer("lattice creation time %5dms\n", 0);

	    int cellCount   = l.getCellCount();
	    int totalObjectCount = l.getObjectCount();

	    System.out.println("occupied cells = " + cellCount);
	    System.out.println("total objects  = " + totalObjectCount);

	    IntArray objects = new IntArray();

	    boolean seen[] = new boolean[totalObjectCount];

	    for(int c = 0; c < cellCount; c++){
		objects.removeAllElements();
		int objectCount = l.getCellContents(c, objects);

		//FILE.out.print("cell %d\n", c);

		for(int i = 0; i < objectCount; i++){
		    //FILE.out.print("object[%d]=", i);
		    //FILE.out.print("%d\n", objects[i]);
		    seen[objects.get(i)] = true;
		}
	    }

	    for(int o = 0; o < totalObjectCount; o++){
		if(seen[o] == false){
		    System.out.println("haven't seen object " + o);
		}
	    }


	    int neighboursBF[] = new int[atomCount];
	    int neighboursL[] = new int[atomCount];
	    int neighboursC[] = new int[atomCount];

	    boolean doBruteForce = true;

	    long then = System.currentTimeMillis();
		
	    double gs2 = gridSize * gridSize;

	    if(doBruteForce){

		System.out.println("finding neighbour list BF");
	    
		for(int i = 0; i < atomCount; i++){
		    Atom ai = mol.getAtom(i);
		    //if(i % 1000 == 0){
		    //System.out.println("atom " + i);
		    //}
		    for(int j = i + 1; j < atomCount; j++){
			Atom aj = mol.getAtom(j);
			if(ai.distanceSq(aj) < gs2){
			    neighboursBF[i]++;
			    neighboursBF[j]++;
			}
		    }
		}

		FILE.out.print("time %5dms\n", (System.currentTimeMillis() - then));
	    }

	    then = System.currentTimeMillis();

	    System.out.println("finding neighbour list Lattice");
	    
	    for(int i = 0; i < atomCount; i++){
		Atom ai = mol.getAtom(i);

		objects.removeAllElements();

		l.getPossibleNeighbours(i, ai.x, ai.y, ai.z,
					objects, false);

		int nc = objects.size();

		for(int ni = 0; ni < nc; ni++){
		    int j = objects.get(ni);
		    Atom aj = mol.getAtom(j);

		    if(ai.distanceSq(aj) < gs2){
			neighboursL[i]++;
			neighboursL[j]++;
		    }
		}
	    }

	    FILE.out.print("time %5dms\n", (System.currentTimeMillis() - then));

	    then = System.currentTimeMillis();

	    System.out.println("finding neighbour list Lattice cell");
	    
	    for(int c = 0; c < cellCount; c++){
		objects.removeAllElements();

		int nc = l.getPossibleCellNeighbours(c, objects);

		int oo[] = objects.getArray();

		// objects are in pairs
		for(int ni = 0; ni < nc; ni += 2){
		    int i = oo[ni];
		    int j = oo[ni + 1];
		    Atom ai = mol.getAtom(i);
		    Atom aj = mol.getAtom(j);

		    if(ai.distanceSq(aj) < gs2){
			neighboursC[i]++;
			neighboursC[j]++;
		    }
		}
	    }

	    FILE.out.print("time %5dms\n", (System.currentTimeMillis() - then));

	    for(int i = 0; i < atomCount; i++){
		if(neighboursBF[i] != neighboursL[i] &&
		   neighboursC[i] != neighboursL[i]){
		    System.out.println("neighbours don't match for " + i);
		    System.out.println("lattice search " + neighboursL[i]);
		    System.out.println("cell search    " + neighboursC[i]);
		    System.out.println("brute force    " + neighboursBF[i]);
		}
	    }

	    l.printStatistics(0);
	}
    }
}
