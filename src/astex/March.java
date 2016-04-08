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

/* Copyright Astex Technology Ltd. 2003 */

import java.lang.*;

/**
 * A class for (hopefully) implementing marching 
 * cubes algorithm for isosurfaces.
 */
public class March {

    /** Number of edges per layer. */
    private static int nLayerEdges = 0;
    
    /** Indexing tags for edges of each layer. */    
    private static int layerEdges[] = null; 

    /** Layer dimensions. */
    private static int iDim = 0;
    private static int jDim = 0;
    private static int kDim = 0;
    
    private static int iDim1 = 0;
    private static int jDim1 = 0;
    private static int kDim1 = 0;

    private static int iDim2 = 0;
    private static int jDim2 = 0;
    private static int kDim2 = 0;

    private static int ijDim = 0;

    /** Debugging? */
    private static boolean debug = false;

    /** Empty edge identifier. */
    private static final int emptyEdge = -1;

    /** Effective zero. */
    private static final float epsilon = 0.000001f;

    /** tmesh object so that we can output triangles. */
    public static Tmesh tmesh = null;

    /** Looukp hoojami. */
    private static int nedge01 = 0;
    private static int ngrid01 = 0;

    private static int count = 0;

    public static boolean generateTriangles = true;

    /** Vertex masks. */
    private static final int V0 =   1;
    private static final int V1 =   2;
    private static final int V2 =   4;
    private static final int V3 =   8;
    private static final int V4 =  16;
    private static final int V5 =  32;
    private static final int V6 =  64;
    private static final int V7 = 128;

    /** Face masks. */
    private static final int Face0123 = V0|V1|V2|V3;
    private static final int Face0154 = V0|V1|V5|V4;
    private static final int Face0374 = V0|V3|V7|V4;
    private static final int Face4567 = V4|V5|V6|V7;
    private static final int Face1265 = V1|V2|V6|V5;
    private static final int Face2376 = V2|V3|V7|V6;

    /** Constructor. */
    public static synchronized
	Tmesh surface(float data[], int nx, int ny, int nz,
		      float threshold, boolean invert, Tmesh tm){
	
	int i, j, k, e;
	int off1, off2;

	//long then = System.currentTimeMillis();

	tmesh = tm;

	/* Sort out layer dimensions. */
	iDim = nx;
	jDim = ny;
	kDim = nz;
	iDim1 = iDim - 1;
	jDim1 = jDim - 1;
	kDim1 = kDim - 1;
	iDim2 = iDim - 2;
	jDim2 = jDim - 2;
	kDim2 = kDim - 2;
	ijDim = iDim * jDim;

	/* Create Tmesh object */
	//tmesh = new Tmesh();

	/* Allocate enough space for the indexing tags
	   for the edges and set to empty. */
	nLayerEdges = (iDim1) * (jDim1) * 12;
	layerEdges = new int[nLayerEdges];
	for(i = 0; i < nLayerEdges; i++){
	    layerEdges[i] = emptyEdge;
	}

	/* Sort out lookup varible. */
	nedge01 = 12 * (iDim1);
	ngrid01 = iDim * jDim;

	if(debug){
	    System.out.println("Beginning marching cubes algorithm");
	    System.out.println("Dimensions: x " + iDim +
			       " y " + jDim + " z " + kDim);
	}

	marchLayer(data, nx, ny, nz, 0, layerEdges, threshold);

	/* Now do the remaining layers. */
	for (k = 1; k < kDim1; k++) {
	    
	    off1 = getEdgeOffset(0, 0, 0);
	
	    /* Percolate the last layer's top edges 
	       to this layer's bottom edges. */
	    for (j = 0; j < jDim1; j++) {
		for (i = 0; i < iDim1; i++) {

		    /* Copying edges. */
		    layerEdges[off1]   = layerEdges[off1+2];
		    layerEdges[off1+4] = layerEdges[off1+6];
		    layerEdges[off1+8] = layerEdges[off1+11];
		    layerEdges[off1+9] = layerEdges[off1+10];
		    
		    /* Reinitialize all of the remaining edges */
		    layerEdges[off1+1]  = emptyEdge;
		    layerEdges[off1+2]  = emptyEdge;
		    layerEdges[off1+3]  = emptyEdge;
		    layerEdges[off1+5]  = emptyEdge;
		    layerEdges[off1+6]  = emptyEdge;
		    layerEdges[off1+7]  = emptyEdge;
		    layerEdges[off1+10] = emptyEdge;
		    layerEdges[off1+11] = emptyEdge;

		    off1 += 12;
		}
	    }

	    marchLayer(data, nx, ny, nz, k, layerEdges, threshold);
	   
	}

	if(debug){
	    System.out.println("Count " + count);
	    System.out.println("Finished marching cubes algorithm");
	}

	if(invert){
	    for(int iv = 0; iv < tmesh.np; iv++){
		tmesh.nx[iv] = -tmesh.nx[iv];
		tmesh.ny[iv] = -tmesh.ny[iv];
		tmesh.nz[iv] = -tmesh.nz[iv];
	    }
	}

	//long now = System.currentTimeMillis();

	//System.out.println("time " + (now -then ) + "ms");

	//System.out.println("points " + tmesh.np + " lines " + tmesh.nt);

	return tmesh;
    }

    /* Grid values at each cell position. */
    private static float cell[]      = new float[8];

    /* Reference to vertices at the 12 edges of a cell  */
    private static int cellVerts[] = new int[12]; 

    /** This is the main marching cubes algorithm function. */
    private static void marchLayer(float data[], int nx, int ny, int nz,
				   int layer, int layerEdges[],
				   float threshold){
	
	int i, j, off, e;

	float localData[] = data;
	
	/* Cell index lookup. */
	int    cellIndex;

	/* Initialise cellVerts. */
	for(i = 0;  i < 12; i++){
	    cellVerts[i] = emptyEdge;
	}

	/* Loop over each cube in the layer. */
	int iijDim = iDim + ijDim;
	for(j = 0; j < jDim1; j++) {

	    /** Do this simply first. */
	    int cell0 = getOffset(0, j, layer);

	    /* Initialise cell values in j. */
	    cell[0] = localData[cell0];
	    cell[1] = localData[cell0 + iDim];
	    cell[2] = localData[cell0 + iijDim];
	    cell[3] = localData[cell0 + ijDim];

	    cellIndex = 0;
	    if(cell[0] > threshold) cellIndex |=   1;
	    if(cell[1] > threshold) cellIndex |=   2;
	    if(cell[2] > threshold) cellIndex |=   4;
	    if(cell[3] > threshold) cellIndex |=   8;
	    
	    for(i = 0; i < iDim1; i++) {

		cell0++;
		cell[4] = localData[cell0];
		cell[5] = localData[cell0 + iDim];
		cell[6] = localData[cell0 + iijDim];
		cell[7] = localData[cell0 + ijDim];

		/** Compute the index for the edge intersections for i. */
		if(cell[4] > threshold) cellIndex |=  16;
		if(cell[5] > threshold) cellIndex |=  32;
		if(cell[6] > threshold) cellIndex |=  64;
		if(cell[7] > threshold) cellIndex |= 128;

		/* Skip loop if no edges */
		if(cellIndex != 0 && cellIndex != 255){
		 
		    count++;

		    /* Here find edge(s) that will contain a vertex. */
		    int lookup = 1;
		    for(e = 0; e < 12; e++){
			if((edgeTable[cellIndex] & lookup) != 0){
			    addVertex(data, e, i, j, threshold,
				      cellVerts, layer);
			}
			lookup <<= 1;
		    }
		
		    /* Put the cellVerts references into 
		       this cell's layerEdges table. */
		    off = getEdgeOffset(0, i, j);
		    for(e = 0; e < 12; e++){
			if(cellVerts[e] != emptyEdge){
			    layerEdges[off] = cellVerts[e];
			}
			off++;
		    }
		
		    /* Propagate the vertex/normal references
		       to the adjacent cells to
		       the right and in front of this layer.
		       Propagate to the right */
		    if (i < iDim2) {
			
			off = getEdgeOffset(0, i+1, j);

			layerEdges[off] = cellVerts[4];

			//off = getEdgeOffset(7, i+1, j);
			layerEdges[off+1] = cellVerts[5];
		    
			//off = getEdgeOffset(8, i+1, j);
			layerEdges[off+2] = cellVerts[6];

			//off = getEdgeOffset(11, i+1, j);
			layerEdges[off+3] = cellVerts[7];

		    }

		    /* Propagate to the front. */
		    if (j < jDim2) { 

			off = getEdgeOffset( 3, i, j+1);
			layerEdges[off] = cellVerts[1];
		    
			//off = getEdgeOffset( 9, i, j+1);
			layerEdges[off+5] = cellVerts[9];

			//off = getEdgeOffset( 4, i, j+1);
			layerEdges[off+4] = cellVerts[5];
		    
			//off = getEdgeOffset( 8, i, j+1);
			layerEdges[off+8] = cellVerts[10];
		    }

		    if(generateTriangles){
			/* Add triangles. */
			int ii = 0;
			while (triTable[cellIndex][ii] != -1) {		    
			    tmesh.addTriangle(cellVerts[triTable[cellIndex][ii]],
					      cellVerts[triTable[cellIndex][ii+1]],
					      cellVerts[triTable[cellIndex][ii+2]]);
			    ii += 3;
			}
		    }else{
			// always contour these faces.
			if((cellIndex & Face0123) != 0 && (cellIndex & Face0123) != Face0123){
			    contourFace(cellIndex, threshold, 0, 1, 2, 3, 0, 1, 2, 3);
			}
			if((cellIndex & Face0154) != 0 && (cellIndex & Face0154) != Face0154){
			    contourFace(cellIndex, threshold, 0, 1, 5, 4, 0, 9, 4, 8);
			}
			if((cellIndex & Face0374) != 0 && (cellIndex & Face0374) != Face0374){
			    contourFace(cellIndex, threshold, 0, 3, 7, 4, 3, 11, 7, 8);
			}

			// these are the end faces along each dimension.
			// little point checking they need contouring before calling
			if(i == iDim2){
			    contourFace(cellIndex, threshold, 4, 5, 6, 7, 4, 5, 6, 7);
			}
			if(j == jDim2){
			    contourFace(cellIndex, threshold, 1, 2, 6, 5, 1, 10, 5, 9);
			}
			if(layer == kDim2){
			    contourFace(cellIndex, threshold, 2, 3, 7, 6, 2, 11, 6, 10);
			}
		    }
		}
		
		/* Copy over cell values in direction of i. */
		cell[0] = cell[4];
		cell[1] = cell[5];
		cell[2] = cell[6];
		cell[3] = cell[7];

		/* sort out cellIndex. */
		cellIndex = (cellIndex >>> 4);
	    }
	}
    }

    /** Add edges for one face. */
    private static void contourFace(int cellIndex, float level,
				    int v0, int v1, int v2, int v3,
				    int e0, int e1, int e2, int e3){
	// build mask for this face.
	int faceIndex = 0;
	if((cellIndex & (1<<v0)) != 0) faceIndex |= 1;
	if((cellIndex & (1<<v1)) != 0) faceIndex |= 2;
	if((cellIndex & (1<<v2)) != 0) faceIndex |= 4;
	if((cellIndex & (1<<v3)) != 0) faceIndex |= 8;

	switch(faceIndex){
	case 0: case 15: // nothing
	    break;
	case 1: case 14:
	    tmesh.addLine(cellVerts[e3], cellVerts[e0], faceIndex);
	    break;
	case 2: case 13:
	    tmesh.addLine(cellVerts[e0], cellVerts[e1], faceIndex);
	    break;
	case 4: case 11:
	    tmesh.addLine(cellVerts[e1], cellVerts[e2], faceIndex);
	    break;
	case 8: case 7:
	    tmesh.addLine(cellVerts[e2], cellVerts[e3], faceIndex);
	    break;
	case 3: case 12:
	    tmesh.addLine(cellVerts[e1], cellVerts[e3], faceIndex);
	    break;
	case 6: case 9:
	    tmesh.addLine(cellVerts[e0], cellVerts[e2], faceIndex);
	    break;
	case 5: case 10:
	    // needs to take account of center value
	    double mean = 0.25 * (cell[v0] + cell[v1] + cell[v2] + cell[v3]);
	    // check which side the mean is on relative to one corner
	    if(mean > level == cell[v0] > level){
	    //if(mean > level && cell[v0] > level){
		tmesh.addLine(cellVerts[e0], cellVerts[e1], faceIndex);
		tmesh.addLine(cellVerts[e2], cellVerts[e3], faceIndex);
	    }else{
		tmesh.addLine(cellVerts[e0], cellVerts[e3], faceIndex);
		tmesh.addLine(cellVerts[e1], cellVerts[e2], faceIndex);
	    }
	    break;
	default:
	    Log.error("unhandled faceIndex %d", faceIndex);
	}
    }

    /** Retrieve offset as if a 3D array. */
    private static int getOffset(int i, int j, int k){
        return(i + iDim * j + ngrid01 * k); 
    }

    /** Adds a vertex to the list. */
    private static void addVertex(float data[], int edgeNum, int i, int j,
				  float threshold, 
				  int cellVerts[], int layer){
	
	/* Get the edge vertex. */
	int edgeOffset = getEdgeOffset(edgeNum, i, j);

	if(layerEdges[edgeOffset] == emptyEdge){
	    cellVerts[edgeNum] = makeVertex(data, edgeNum, i, j, layer, threshold);
	} else {
	    cellVerts[edgeNum] = layerEdges[edgeOffset];
	}
    }

    /** Returns the offset for edge lookup */
    private static int getEdgeOffset(int edgeNum, int i, int j){
	int off = edgeNum + (12 * i) + nedge01 * j;
	return (off);
    }

    private static int   from[]     = new int[3];
    private static int   to[]       = new int[3];
    private static float normFrom[] = new float[3];
    private static float normTo[]   = new float[3];
    private static float v[]        = new float[3];
    private static float n[]        = new float[3];

    /** Creates a vertex */
    private static int makeVertex(float data[], int edgeNum,
				  int i, int j, int k, float threshold){
	
	float d;
	float len;
	int    ii;
	float localData[] = data;

	switch(edgeNum){
	case 0:
	    from[0] =  i;  from[1] =  j;  from[2] =  k;
	    to[0]   = i;   to[1] =  j+1;    to[2] =  k;
	    break;
	case 1: 
	    from[0] = i;   from[1] =  j+1;  from[2] =  k;
	    to[0]   = i;   to[1] = j+1;   to[2] =  k+1;
	    break;
	case 2: 
	    from[0] = i;   from[1] = j+1; from[2] =  k+1;
	    to[0] =   i;    to[1] = j;   to[2] =  k+1;
	    break;
	case 3: 
	    from[0] =  i;  from[1] = j; from[2] =  k;
	    to[0] =    i;    to[1] =  j;  to[2] =  k+1;
	    break;
	case 4: 
	    from[0] =  i+1;  from[1] =  j;  from[2] = k;
	    to[0] = i+1;   to[1] =  j+1;    to[2] = k;
	    break;
	case 5: 
	    from[0] = i+1; from[1] =  j+1;  from[2] =  k;
	    to[0] = i+1;   to[1] = j+1;   to[2] =  k+1;
	    break;
	case 6: 
	    from[0] = i+1; from[1] = j+1; from[2] = k+1;
	    to[0] =  i+1;    to[1] = j;   to[2] = k+1;
	    break;
	case 7: 
	    from[0] =  i+1;  from[1] = j; from[2] =  k;
	    to[0] =  i+1;    to[1] =  j;    to[2] =  k+1;
	    break;
	case 8: 
	    from[0] =  i;  from[1] =  j;  from[2] =  k;
	    to[0] =  i+1;    to[1] =  j;    to[2] = k;
	    break;
	case 9: 
	    from[0] = i; from[1] =  j+1;  from[2] = k;
	    to[0] = i+1;   to[1] =  j+1;    to[2] = k;
	    break;
	case 10: 
	    from[0] = i; from[1] = j+1; from[2] =  k+1;
	    to[0] = i+1;   to[1] = j+1;   to[2] = k+1;
	    break;
	case 11: 
	    from[0] =  i;  from[1] = j; from[2] =  k+1;
	    to[0] =  i+1;    to[1] = j;   to[2] = k+1;
	    break;
	default:
	    System.out.println("makeVertex: bad edge index " + edgeNum);
	    System.exit(2);
	    break;
	}

	int fromLookup   = getOffset(from[0], from[1], from[2]);
	int toLookup     = getOffset(to[0], to[1], to[2]);
	float fromValue = localData[fromLookup];
	float toValue   = localData[toLookup];

	/* Calculate the relative distance from -> to. */
	d = (fromValue - threshold) / 
	    (fromValue - toValue);
       
	if(d < epsilon){
	    d = 0.0f;
	} else if(d > (1 - epsilon)){
	    d = 1.0f;
	} 

	v[0] = from[0] + d * (to[0] - from[0]);
	v[1] = from[1] + d * (to[1] - from[1]);
	v[2] = from[2] + d * (to[2] - from[2]);

	if(generateTriangles){
	    /* Determine the gradients at the endpoints of the edge
	       and interpolate the normal for the isosurface vertex. */	
	    if(from[0] == 0){ /* On left edge. */
		normFrom[0] = 0.5f * (-3.0f * localData[fromLookup] +
				      4.0f * localData[fromLookup + 1] -
				      localData[fromLookup + 2]);
	    }else if(from[0] == iDim1){ /* On right edge. */
		normFrom[0] = 0.5f * (localData[fromLookup - 2] -
				      4.0f * localData[fromLookup - 1] +
				      3.0f * localData[fromLookup]);
	    }else{ /* In the interior. */
		normFrom[0] = 0.5f * (localData[fromLookup + 1] -
				      localData[fromLookup - 1]);
	    }
	

	    if(from[1] == 0){ /* On front edge. */
		normFrom[1] = 0.5f * (-3.0f * localData[fromLookup] +
				      4.0f * localData[fromLookup + iDim] -
				      localData[fromLookup + (2 * iDim)]);
	    }else if(from[1] == jDim1){ /* On back edge. */
		normFrom[1] = 0.5f * (localData[fromLookup - (2 * iDim)] -
				      4.0f * localData[fromLookup - iDim] +
				      3.0f * localData[fromLookup]);
	    }else{ /* In the interior. */
		normFrom[1] = 0.5f * (localData[fromLookup + iDim] -
				      localData[fromLookup - iDim]);
	    }
	
	    if(from[2] == 0){ /* On bottom edge. */
		normFrom[2] = 0.5f * (-3.0f * localData[fromLookup] +
				      4.0f * localData[fromLookup + ijDim] -
				      localData[fromLookup + (2 * ijDim)]);
	    }else if(from[2] == kDim1){ /* On top edge. */
		normFrom[2] = 0.5f * (localData[fromLookup - (2 * ijDim)] -
				      4.0f * localData[fromLookup - ijDim] +
				      3.0f * localData[fromLookup]);
	    }else{ /* In the interior. */
		normFrom[2] = 0.5f * (localData[fromLookup + ijDim] -
				      localData[fromLookup - ijDim]);
	    }

	    /* Normal for to vertex. */
	    if(to[0] == 0){ /* On left edge. */
		normTo[0] = 0.5f * (-3.0f * localData[toLookup] +
				    4.0f * localData[toLookup + 1] -
				    localData[toLookup + 2]);
	    }else if(to[0] == iDim1){ /* On right edge. */
		normTo[0] = 0.5f * (localData[toLookup - 1] -
				    4.0f * localData[toLookup - 1] +
				    3.0f * localData[toLookup]);
	    }else{ /* In the interior. */
		normTo[0] = 0.5f * (localData[toLookup + 1] -
				    localData[toLookup - 1]);
	    }
	
	    if(to[1] == 0){ /* On front edge. */
		normTo[1] = 0.5f * ( -3.0f * localData[toLookup] +
				     4.0f * localData[toLookup + iDim] -
				     localData[toLookup + (2 * iDim)]);
	    }else if(to[1] == jDim1){ /* On back edge. */
		normTo[1] = 0.5f * ( localData[toLookup - (2 * iDim)] -
				     4.0f * localData[toLookup - iDim] +
				     3.0f * localData[toLookup]);
	    }else{ /* In the interior. */
		normTo[1] = 0.5f * ( localData[toLookup + iDim] -
				     localData[toLookup - iDim]);
	    }
	
	    if(to[2] == 0){ /* On bottom edge. */
		normTo[2] = 0.5f * (  -3.0f * localData[toLookup] +
				      4.0f * localData[toLookup + ijDim] -
				      localData[toLookup + (2 * ijDim)]);
	    }else if(to[2] == kDim1){ /* On top edge. */
		normTo[2] = 0.5f * ( localData[toLookup - (2 * ijDim)] -
				     4.0f * localData[toLookup - ijDim] +
				     3.0f * localData[toLookup]);
	    }else{ /* In the interior. */
		normTo[2] = 0.5f * ( localData[toLookup + ijDim] -
				     localData[toLookup - ijDim]);
	    }
	
	    /* Now that we have the normals at the two endpoints, interpolate */
	    n[0] = normFrom[0] + d * (normTo[0] - normFrom[0]);
	    n[1] = normFrom[1] + d * (normTo[1] - normFrom[1]);
	    n[2] = normFrom[2] + d * (normTo[2] - normFrom[2]);
	
	    /* Normalize the normal at the isosurface vertex */
	    len = (float)Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);

	    if(len > epsilon){
		n[0] /= len;
		n[1] /= len;
		n[2] /= len;
	    }else{ /* We have to fake this normal. */
		//System.out.println("!!! null normal");
		n[0] = 1.0f;
		n[1] = 0.0f;
		n[2] = 0.0f;
	    }
	}
    
	/* Insert the vertex and the normal into the Tmesh object. */
	tmesh.addPoint(v[0], v[1], v[2], -n[0], -n[1], -n[2], 0, 0);

	/* Return the array reference of the vertex. */
	return (tmesh.getnPoints() - 1);
    }

    private static float length(float x[]){
	return (float)Math.sqrt(x[0]*x[0] + x[1]*x[1] + x[2]*x[2]);
    }

    /** Edge table lookup.
	these tables for computing the Marching Cubes algorithm
	are from http://www.mhri.edu.au/~pdb/modelling/polygonise/
	by Paul Bourke, based on code by Cory Gene Bloyd.
	
	The indexing of vertices and edges in a cube are defined
	as: 
  
                _4____________4_____________5
	       /|                           /
	      / |                          /|
	     /  |                         / |
            7   |                        /  |
           /    |                       /5  |
          /     |                      /    |
         /      8                     /     9
	/       |                    /      |
      7/________|______6____________/6      |
       |        |                   |       |
       |        |                   |       |
       |        |                   |       |
       |        |0____________0_____|_______|1
      11       /                    |      /
       |      /                    10     /
       |     /                      |    /
       |    /3                      |   /1
       |   /                        |  /
       |  /                         | /
       | /                          |/
       |/3____________2_____________|2

       For purposes of calculating vertices along the edges and the
       triangulations created, there are 15 distinct cases, with
       upper limits of
       12 edge intersections
       5 triangles created per cell
    */
    // changed from int -> char
    private static final char edgeTable[] = {
	0x0  , 0x109, 0x203, 0x30a, 0x406, 0x50f, 0x605, 0x70c,
	0x80c, 0x905, 0xa0f, 0xb06, 0xc0a, 0xd03, 0xe09, 0xf00,
	0x190, 0x99 , 0x393, 0x29a, 0x596, 0x49f, 0x795, 0x69c,
	0x99c, 0x895, 0xb9f, 0xa96, 0xd9a, 0xc93, 0xf99, 0xe90,
	0x230, 0x339, 0x33 , 0x13a, 0x636, 0x73f, 0x435, 0x53c,
	0xa3c, 0xb35, 0x83f, 0x936, 0xe3a, 0xf33, 0xc39, 0xd30,
	0x3a0, 0x2a9, 0x1a3, 0xaa , 0x7a6, 0x6af, 0x5a5, 0x4ac,
	0xbac, 0xaa5, 0x9af, 0x8a6, 0xfaa, 0xea3, 0xda9, 0xca0,
	0x460, 0x569, 0x663, 0x76a, 0x66 , 0x16f, 0x265, 0x36c,
	0xc6c, 0xd65, 0xe6f, 0xf66, 0x86a, 0x963, 0xa69, 0xb60,
	0x5f0, 0x4f9, 0x7f3, 0x6fa, 0x1f6, 0xff , 0x3f5, 0x2fc,
	0xdfc, 0xcf5, 0xfff, 0xef6, 0x9fa, 0x8f3, 0xbf9, 0xaf0,
	0x650, 0x759, 0x453, 0x55a, 0x256, 0x35f, 0x55 , 0x15c,
	0xe5c, 0xf55, 0xc5f, 0xd56, 0xa5a, 0xb53, 0x859, 0x950,
	0x7c0, 0x6c9, 0x5c3, 0x4ca, 0x3c6, 0x2cf, 0x1c5, 0xcc ,
	0xfcc, 0xec5, 0xdcf, 0xcc6, 0xbca, 0xac3, 0x9c9, 0x8c0,
	0x8c0, 0x9c9, 0xac3, 0xbca, 0xcc6, 0xdcf, 0xec5, 0xfcc,
	0xcc , 0x1c5, 0x2cf, 0x3c6, 0x4ca, 0x5c3, 0x6c9, 0x7c0,
	0x950, 0x859, 0xb53, 0xa5a, 0xd56, 0xc5f, 0xf55, 0xe5c,
	0x15c, 0x55 , 0x35f, 0x256, 0x55a, 0x453, 0x759, 0x650,
	0xaf0, 0xbf9, 0x8f3, 0x9fa, 0xef6, 0xfff, 0xcf5, 0xdfc,
	0x2fc, 0x3f5, 0xff , 0x1f6, 0x6fa, 0x7f3, 0x4f9, 0x5f0,
	0xb60, 0xa69, 0x963, 0x86a, 0xf66, 0xe6f, 0xd65, 0xc6c,
	0x36c, 0x265, 0x16f, 0x66 , 0x76a, 0x663, 0x569, 0x460,
	0xca0, 0xda9, 0xea3, 0xfaa, 0x8a6, 0x9af, 0xaa5, 0xbac,
	0x4ac, 0x5a5, 0x6af, 0x7a6, 0xaa , 0x1a3, 0x2a9, 0x3a0,
	0xd30, 0xc39, 0xf33, 0xe3a, 0x936, 0x83f, 0xb35, 0xa3c,
	0x53c, 0x435, 0x73f, 0x636, 0x13a, 0x33 , 0x339, 0x230,
	0xe90, 0xf99, 0xc93, 0xd9a, 0xa96, 0xb9f, 0x895, 0x99c,
	0x69c, 0x795, 0x49f, 0x596, 0x29a, 0x393, 0x99 , 0x190,
	0xf00, 0xe09, 0xd03, 0xc0a, 0xb06, 0xa0f, 0x905, 0x80c,
	0x70c, 0x605, 0x50f, 0x406, 0x30a, 0x203, 0x109, 0x0 
    };   

    // changed from int -> byte
    private static final byte triTable[][] = {
	{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{0, 8, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{0, 1, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{1, 8, 3, 9, 8, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{1, 2, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{0, 8, 3, 1, 2, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{9, 2, 10, 0, 2, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{2, 8, 3, 2, 10, 8, 10, 9, 8, -1, -1, -1, -1, -1, -1, -1},
	{3, 11, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{0, 11, 2, 8, 11, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{1, 9, 0, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{1, 11, 2, 1, 9, 11, 9, 8, 11, -1, -1, -1, -1, -1, -1, -1},
	{3, 10, 1, 11, 10, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{0, 10, 1, 0, 8, 10, 8, 11, 10, -1, -1, -1, -1, -1, -1, -1},
	{3, 9, 0, 3, 11, 9, 11, 10, 9, -1, -1, -1, -1, -1, -1, -1},
	{9, 8, 10, 10, 8, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{4, 7, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{4, 3, 0, 7, 3, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{0, 1, 9, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{4, 1, 9, 4, 7, 1, 7, 3, 1, -1, -1, -1, -1, -1, -1, -1},
	{1, 2, 10, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{3, 4, 7, 3, 0, 4, 1, 2, 10, -1, -1, -1, -1, -1, -1, -1},
	{9, 2, 10, 9, 0, 2, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1},
	{2, 10, 9, 2, 9, 7, 2, 7, 3, 7, 9, 4, -1, -1, -1, -1},
	{8, 4, 7, 3, 11, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{11, 4, 7, 11, 2, 4, 2, 0, 4, -1, -1, -1, -1, -1, -1, -1},
	{9, 0, 1, 8, 4, 7, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1},
	{4, 7, 11, 9, 4, 11, 9, 11, 2, 9, 2, 1, -1, -1, -1, -1},
	{3, 10, 1, 3, 11, 10, 7, 8, 4, -1, -1, -1, -1, -1, -1, -1},
	{1, 11, 10, 1, 4, 11, 1, 0, 4, 7, 11, 4, -1, -1, -1, -1},
	{4, 7, 8, 9, 0, 11, 9, 11, 10, 11, 0, 3, -1, -1, -1, -1},
	{4, 7, 11, 4, 11, 9, 9, 11, 10, -1, -1, -1, -1, -1, -1, -1},
	{9, 5, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{9, 5, 4, 0, 8, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{0, 5, 4, 1, 5, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{8, 5, 4, 8, 3, 5, 3, 1, 5, -1, -1, -1, -1, -1, -1, -1},
	{1, 2, 10, 9, 5, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{3, 0, 8, 1, 2, 10, 4, 9, 5, -1, -1, -1, -1, -1, -1, -1},
	{5, 2, 10, 5, 4, 2, 4, 0, 2, -1, -1, -1, -1, -1, -1, -1},
	{2, 10, 5, 3, 2, 5, 3, 5, 4, 3, 4, 8, -1, -1, -1, -1},
	{9, 5, 4, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{0, 11, 2, 0, 8, 11, 4, 9, 5, -1, -1, -1, -1, -1, -1, -1},
	{0, 5, 4, 0, 1, 5, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1},
	{2, 1, 5, 2, 5, 8, 2, 8, 11, 4, 8, 5, -1, -1, -1, -1},
	{10, 3, 11, 10, 1, 3, 9, 5, 4, -1, -1, -1, -1, -1, -1, -1},
	{4, 9, 5, 0, 8, 1, 8, 10, 1, 8, 11, 10, -1, -1, -1, -1},
	{5, 4, 0, 5, 0, 11, 5, 11, 10, 11, 0, 3, -1, -1, -1, -1},
	{5, 4, 8, 5, 8, 10, 10, 8, 11, -1, -1, -1, -1, -1, -1, -1},
	{9, 7, 8, 5, 7, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{9, 3, 0, 9, 5, 3, 5, 7, 3, -1, -1, -1, -1, -1, -1, -1},
	{0, 7, 8, 0, 1, 7, 1, 5, 7, -1, -1, -1, -1, -1, -1, -1},
	{1, 5, 3, 3, 5, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{9, 7, 8, 9, 5, 7, 10, 1, 2, -1, -1, -1, -1, -1, -1, -1},
	{10, 1, 2, 9, 5, 0, 5, 3, 0, 5, 7, 3, -1, -1, -1, -1},
	{8, 0, 2, 8, 2, 5, 8, 5, 7, 10, 5, 2, -1, -1, -1, -1},
	{2, 10, 5, 2, 5, 3, 3, 5, 7, -1, -1, -1, -1, -1, -1, -1},
	{7, 9, 5, 7, 8, 9, 3, 11, 2, -1, -1, -1, -1, -1, -1, -1},
	{9, 5, 7, 9, 7, 2, 9, 2, 0, 2, 7, 11, -1, -1, -1, -1},
	{2, 3, 11, 0, 1, 8, 1, 7, 8, 1, 5, 7, -1, -1, -1, -1},
	{11, 2, 1, 11, 1, 7, 7, 1, 5, -1, -1, -1, -1, -1, -1, -1},
	{9, 5, 8, 8, 5, 7, 10, 1, 3, 10, 3, 11, -1, -1, -1, -1},
	{5, 7, 0, 5, 0, 9, 7, 11, 0, 1, 0, 10, 11, 10, 0, -1},
	{11, 10, 0, 11, 0, 3, 10, 5, 0, 8, 0, 7, 5, 7, 0, -1},
	{11, 10, 5, 7, 11, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{10, 6, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{0, 8, 3, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{9, 0, 1, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{1, 8, 3, 1, 9, 8, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1},
	{1, 6, 5, 2, 6, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{1, 6, 5, 1, 2, 6, 3, 0, 8, -1, -1, -1, -1, -1, -1, -1},
	{9, 6, 5, 9, 0, 6, 0, 2, 6, -1, -1, -1, -1, -1, -1, -1},
	{5, 9, 8, 5, 8, 2, 5, 2, 6, 3, 2, 8, -1, -1, -1, -1},
	{2, 3, 11, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{11, 0, 8, 11, 2, 0, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1},
	{0, 1, 9, 2, 3, 11, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1},
	{5, 10, 6, 1, 9, 2, 9, 11, 2, 9, 8, 11, -1, -1, -1, -1},
	{6, 3, 11, 6, 5, 3, 5, 1, 3, -1, -1, -1, -1, -1, -1, -1},
	{0, 8, 11, 0, 11, 5, 0, 5, 1, 5, 11, 6, -1, -1, -1, -1},
	{3, 11, 6, 0, 3, 6, 0, 6, 5, 0, 5, 9, -1, -1, -1, -1},
	{6, 5, 9, 6, 9, 11, 11, 9, 8, -1, -1, -1, -1, -1, -1, -1},
	{5, 10, 6, 4, 7, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{4, 3, 0, 4, 7, 3, 6, 5, 10, -1, -1, -1, -1, -1, -1, -1},
	{1, 9, 0, 5, 10, 6, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1},
	{10, 6, 5, 1, 9, 7, 1, 7, 3, 7, 9, 4, -1, -1, -1, -1},
	{6, 1, 2, 6, 5, 1, 4, 7, 8, -1, -1, -1, -1, -1, -1, -1},
	{1, 2, 5, 5, 2, 6, 3, 0, 4, 3, 4, 7, -1, -1, -1, -1},
	{8, 4, 7, 9, 0, 5, 0, 6, 5, 0, 2, 6, -1, -1, -1, -1},
	{7, 3, 9, 7, 9, 4, 3, 2, 9, 5, 9, 6, 2, 6, 9, -1},
	{3, 11, 2, 7, 8, 4, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1},
	{5, 10, 6, 4, 7, 2, 4, 2, 0, 2, 7, 11, -1, -1, -1, -1},
	{0, 1, 9, 4, 7, 8, 2, 3, 11, 5, 10, 6, -1, -1, -1, -1},
	{9, 2, 1, 9, 11, 2, 9, 4, 11, 7, 11, 4, 5, 10, 6, -1},
	{8, 4, 7, 3, 11, 5, 3, 5, 1, 5, 11, 6, -1, -1, -1, -1},
	{5, 1, 11, 5, 11, 6, 1, 0, 11, 7, 11, 4, 0, 4, 11, -1},
	{0, 5, 9, 0, 6, 5, 0, 3, 6, 11, 6, 3, 8, 4, 7, -1},
	{6, 5, 9, 6, 9, 11, 4, 7, 9, 7, 11, 9, -1, -1, -1, -1},
	{10, 4, 9, 6, 4, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{4, 10, 6, 4, 9, 10, 0, 8, 3, -1, -1, -1, -1, -1, -1, -1},
	{10, 0, 1, 10, 6, 0, 6, 4, 0, -1, -1, -1, -1, -1, -1, -1},
	{8, 3, 1, 8, 1, 6, 8, 6, 4, 6, 1, 10, -1, -1, -1, -1},
	{1, 4, 9, 1, 2, 4, 2, 6, 4, -1, -1, -1, -1, -1, -1, -1},
	{3, 0, 8, 1, 2, 9, 2, 4, 9, 2, 6, 4, -1, -1, -1, -1},
	{0, 2, 4, 4, 2, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{8, 3, 2, 8, 2, 4, 4, 2, 6, -1, -1, -1, -1, -1, -1, -1},
	{10, 4, 9, 10, 6, 4, 11, 2, 3, -1, -1, -1, -1, -1, -1, -1},
	{0, 8, 2, 2, 8, 11, 4, 9, 10, 4, 10, 6, -1, -1, -1, -1},
	{3, 11, 2, 0, 1, 6, 0, 6, 4, 6, 1, 10, -1, -1, -1, -1},
	{6, 4, 1, 6, 1, 10, 4, 8, 1, 2, 1, 11, 8, 11, 1, -1},
	{9, 6, 4, 9, 3, 6, 9, 1, 3, 11, 6, 3, -1, -1, -1, -1},
	{8, 11, 1, 8, 1, 0, 11, 6, 1, 9, 1, 4, 6, 4, 1, -1},
	{3, 11, 6, 3, 6, 0, 0, 6, 4, -1, -1, -1, -1, -1, -1, -1},
	{6, 4, 8, 11, 6, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{7, 10, 6, 7, 8, 10, 8, 9, 10, -1, -1, -1, -1, -1, -1, -1},
	{0, 7, 3, 0, 10, 7, 0, 9, 10, 6, 7, 10, -1, -1, -1, -1},
	{10, 6, 7, 1, 10, 7, 1, 7, 8, 1, 8, 0, -1, -1, -1, -1},
	{10, 6, 7, 10, 7, 1, 1, 7, 3, -1, -1, -1, -1, -1, -1, -1},
	{1, 2, 6, 1, 6, 8, 1, 8, 9, 8, 6, 7, -1, -1, -1, -1},
	{2, 6, 9, 2, 9, 1, 6, 7, 9, 0, 9, 3, 7, 3, 9, -1},
	{7, 8, 0, 7, 0, 6, 6, 0, 2, -1, -1, -1, -1, -1, -1, -1},
	{7, 3, 2, 6, 7, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{2, 3, 11, 10, 6, 8, 10, 8, 9, 8, 6, 7, -1, -1, -1, -1},
	{2, 0, 7, 2, 7, 11, 0, 9, 7, 6, 7, 10, 9, 10, 7, -1},
	{1, 8, 0, 1, 7, 8, 1, 10, 7, 6, 7, 10, 2, 3, 11, -1},
	{11, 2, 1, 11, 1, 7, 10, 6, 1, 6, 7, 1, -1, -1, -1, -1},
	{8, 9, 6, 8, 6, 7, 9, 1, 6, 11, 6, 3, 1, 3, 6, -1},
	{0, 9, 1, 11, 6, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{7, 8, 0, 7, 0, 6, 3, 11, 0, 11, 6, 0, -1, -1, -1, -1},
	{7, 11, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{7, 6, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{3, 0, 8, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{0, 1, 9, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{8, 1, 9, 8, 3, 1, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1},
	{10, 1, 2, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{1, 2, 10, 3, 0, 8, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1},
	{2, 9, 0, 2, 10, 9, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1},
	{6, 11, 7, 2, 10, 3, 10, 8, 3, 10, 9, 8, -1, -1, -1, -1},
	{7, 2, 3, 6, 2, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{7, 0, 8, 7, 6, 0, 6, 2, 0, -1, -1, -1, -1, -1, -1, -1},
	{2, 7, 6, 2, 3, 7, 0, 1, 9, -1, -1, -1, -1, -1, -1, -1},
	{1, 6, 2, 1, 8, 6, 1, 9, 8, 8, 7, 6, -1, -1, -1, -1},
	{10, 7, 6, 10, 1, 7, 1, 3, 7, -1, -1, -1, -1, -1, -1, -1},
	{10, 7, 6, 1, 7, 10, 1, 8, 7, 1, 0, 8, -1, -1, -1, -1},
	{0, 3, 7, 0, 7, 10, 0, 10, 9, 6, 10, 7, -1, -1, -1, -1},
	{7, 6, 10, 7, 10, 8, 8, 10, 9, -1, -1, -1, -1, -1, -1, -1},
	{6, 8, 4, 11, 8, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{3, 6, 11, 3, 0, 6, 0, 4, 6, -1, -1, -1, -1, -1, -1, -1},
	{8, 6, 11, 8, 4, 6, 9, 0, 1, -1, -1, -1, -1, -1, -1, -1},
	{9, 4, 6, 9, 6, 3, 9, 3, 1, 11, 3, 6, -1, -1, -1, -1},
	{6, 8, 4, 6, 11, 8, 2, 10, 1, -1, -1, -1, -1, -1, -1, -1},
	{1, 2, 10, 3, 0, 11, 0, 6, 11, 0, 4, 6, -1, -1, -1, -1},
	{4, 11, 8, 4, 6, 11, 0, 2, 9, 2, 10, 9, -1, -1, -1, -1},
	{10, 9, 3, 10, 3, 2, 9, 4, 3, 11, 3, 6, 4, 6, 3, -1},
	{8, 2, 3, 8, 4, 2, 4, 6, 2, -1, -1, -1, -1, -1, -1, -1},
	{0, 4, 2, 4, 6, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{1, 9, 0, 2, 3, 4, 2, 4, 6, 4, 3, 8, -1, -1, -1, -1},
	{1, 9, 4, 1, 4, 2, 2, 4, 6, -1, -1, -1, -1, -1, -1, -1},
	{8, 1, 3, 8, 6, 1, 8, 4, 6, 6, 10, 1, -1, -1, -1, -1},
	{10, 1, 0, 10, 0, 6, 6, 0, 4, -1, -1, -1, -1, -1, -1, -1},
	{4, 6, 3, 4, 3, 8, 6, 10, 3, 0, 3, 9, 10, 9, 3, -1},
	{10, 9, 4, 6, 10, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{4, 9, 5, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{0, 8, 3, 4, 9, 5, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1},
	{5, 0, 1, 5, 4, 0, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1},
	{11, 7, 6, 8, 3, 4, 3, 5, 4, 3, 1, 5, -1, -1, -1, -1},
	{9, 5, 4, 10, 1, 2, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1},
	{6, 11, 7, 1, 2, 10, 0, 8, 3, 4, 9, 5, -1, -1, -1, -1},
	{7, 6, 11, 5, 4, 10, 4, 2, 10, 4, 0, 2, -1, -1, -1, -1},
	{3, 4, 8, 3, 5, 4, 3, 2, 5, 10, 5, 2, 11, 7, 6, -1},
	{7, 2, 3, 7, 6, 2, 5, 4, 9, -1, -1, -1, -1, -1, -1, -1},
	{9, 5, 4, 0, 8, 6, 0, 6, 2, 6, 8, 7, -1, -1, -1, -1},
	{3, 6, 2, 3, 7, 6, 1, 5, 0, 5, 4, 0, -1, -1, -1, -1},
	{6, 2, 8, 6, 8, 7, 2, 1, 8, 4, 8, 5, 1, 5, 8, -1},
	{9, 5, 4, 10, 1, 6, 1, 7, 6, 1, 3, 7, -1, -1, -1, -1},
	{1, 6, 10, 1, 7, 6, 1, 0, 7, 8, 7, 0, 9, 5, 4, -1},
	{4, 0, 10, 4, 10, 5, 0, 3, 10, 6, 10, 7, 3, 7, 10, -1},
	{7, 6, 10, 7, 10, 8, 5, 4, 10, 4, 8, 10, -1, -1, -1, -1},
	{6, 9, 5, 6, 11, 9, 11, 8, 9, -1, -1, -1, -1, -1, -1, -1},
	{3, 6, 11, 0, 6, 3, 0, 5, 6, 0, 9, 5, -1, -1, -1, -1},
	{0, 11, 8, 0, 5, 11, 0, 1, 5, 5, 6, 11, -1, -1, -1, -1},
	{6, 11, 3, 6, 3, 5, 5, 3, 1, -1, -1, -1, -1, -1, -1, -1},
	{1, 2, 10, 9, 5, 11, 9, 11, 8, 11, 5, 6, -1, -1, -1, -1},
	{0, 11, 3, 0, 6, 11, 0, 9, 6, 5, 6, 9, 1, 2, 10, -1},
	{11, 8, 5, 11, 5, 6, 8, 0, 5, 10, 5, 2, 0, 2, 5, -1},
	{6, 11, 3, 6, 3, 5, 2, 10, 3, 10, 5, 3, -1, -1, -1, -1},
	{5, 8, 9, 5, 2, 8, 5, 6, 2, 3, 8, 2, -1, -1, -1, -1},
	{9, 5, 6, 9, 6, 0, 0, 6, 2, -1, -1, -1, -1, -1, -1, -1},
	{1, 5, 8, 1, 8, 0, 5, 6, 8, 3, 8, 2, 6, 2, 8, -1},
	{1, 5, 6, 2, 1, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{1, 3, 6, 1, 6, 10, 3, 8, 6, 5, 6, 9, 8, 9, 6, -1},
	{10, 1, 0, 10, 0, 6, 9, 5, 0, 5, 6, 0, -1, -1, -1, -1},
	{0, 3, 8, 5, 6, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{10, 5, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{11, 5, 10, 7, 5, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{11, 5, 10, 11, 7, 5, 8, 3, 0, -1, -1, -1, -1, -1, -1, -1},
	{5, 11, 7, 5, 10, 11, 1, 9, 0, -1, -1, -1, -1, -1, -1, -1},
	{10, 7, 5, 10, 11, 7, 9, 8, 1, 8, 3, 1, -1, -1, -1, -1},
	{11, 1, 2, 11, 7, 1, 7, 5, 1, -1, -1, -1, -1, -1, -1, -1},
	{0, 8, 3, 1, 2, 7, 1, 7, 5, 7, 2, 11, -1, -1, -1, -1},
	{9, 7, 5, 9, 2, 7, 9, 0, 2, 2, 11, 7, -1, -1, -1, -1},
	{7, 5, 2, 7, 2, 11, 5, 9, 2, 3, 2, 8, 9, 8, 2, -1},
	{2, 5, 10, 2, 3, 5, 3, 7, 5, -1, -1, -1, -1, -1, -1, -1},
	{8, 2, 0, 8, 5, 2, 8, 7, 5, 10, 2, 5, -1, -1, -1, -1},
	{9, 0, 1, 5, 10, 3, 5, 3, 7, 3, 10, 2, -1, -1, -1, -1},
	{9, 8, 2, 9, 2, 1, 8, 7, 2, 10, 2, 5, 7, 5, 2, -1},
	{1, 3, 5, 3, 7, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{0, 8, 7, 0, 7, 1, 1, 7, 5, -1, -1, -1, -1, -1, -1, -1},
	{9, 0, 3, 9, 3, 5, 5, 3, 7, -1, -1, -1, -1, -1, -1, -1},
	{9, 8, 7, 5, 9, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{5, 8, 4, 5, 10, 8, 10, 11, 8, -1, -1, -1, -1, -1, -1, -1},
	{5, 0, 4, 5, 11, 0, 5, 10, 11, 11, 3, 0, -1, -1, -1, -1},
	{0, 1, 9, 8, 4, 10, 8, 10, 11, 10, 4, 5, -1, -1, -1, -1},
	{10, 11, 4, 10, 4, 5, 11, 3, 4, 9, 4, 1, 3, 1, 4, -1},
	{2, 5, 1, 2, 8, 5, 2, 11, 8, 4, 5, 8, -1, -1, -1, -1},
	{0, 4, 11, 0, 11, 3, 4, 5, 11, 2, 11, 1, 5, 1, 11, -1},
	{0, 2, 5, 0, 5, 9, 2, 11, 5, 4, 5, 8, 11, 8, 5, -1},
	{9, 4, 5, 2, 11, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{2, 5, 10, 3, 5, 2, 3, 4, 5, 3, 8, 4, -1, -1, -1, -1},
	{5, 10, 2, 5, 2, 4, 4, 2, 0, -1, -1, -1, -1, -1, -1, -1},
	{3, 10, 2, 3, 5, 10, 3, 8, 5, 4, 5, 8, 0, 1, 9, -1},
	{5, 10, 2, 5, 2, 4, 1, 9, 2, 9, 4, 2, -1, -1, -1, -1},
	{8, 4, 5, 8, 5, 3, 3, 5, 1, -1, -1, -1, -1, -1, -1, -1},
	{0, 4, 5, 1, 0, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{8, 4, 5, 8, 5, 3, 9, 0, 5, 0, 3, 5, -1, -1, -1, -1},
	{9, 4, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{4, 11, 7, 4, 9, 11, 9, 10, 11, -1, -1, -1, -1, -1, -1, -1},
	{0, 8, 3, 4, 9, 7, 9, 11, 7, 9, 10, 11, -1, -1, -1, -1},
	{1, 10, 11, 1, 11, 4, 1, 4, 0, 7, 4, 11, -1, -1, -1, -1},
	{3, 1, 4, 3, 4, 8, 1, 10, 4, 7, 4, 11, 10, 11, 4, -1},
	{4, 11, 7, 9, 11, 4, 9, 2, 11, 9, 1, 2, -1, -1, -1, -1},
	{9, 7, 4, 9, 11, 7, 9, 1, 11, 2, 11, 1, 0, 8, 3, -1},
	{11, 7, 4, 11, 4, 2, 2, 4, 0, -1, -1, -1, -1, -1, -1, -1},
	{11, 7, 4, 11, 4, 2, 8, 3, 4, 3, 2, 4, -1, -1, -1, -1},
	{2, 9, 10, 2, 7, 9, 2, 3, 7, 7, 4, 9, -1, -1, -1, -1},
	{9, 10, 7, 9, 7, 4, 10, 2, 7, 8, 7, 0, 2, 0, 7, -1},
	{3, 7, 10, 3, 10, 2, 7, 4, 10, 1, 10, 0, 4, 0, 10, -1},
	{1, 10, 2, 8, 7, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{4, 9, 1, 4, 1, 7, 7, 1, 3, -1, -1, -1, -1, -1, -1, -1},
	{4, 9, 1, 4, 1, 7, 0, 8, 1, 8, 7, 1, -1, -1, -1, -1},
	{4, 0, 3, 7, 4, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{4, 8, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{9, 10, 8, 10, 11, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{3, 0, 9, 3, 9, 11, 11, 9, 10, -1, -1, -1, -1, -1, -1, -1},
	{0, 1, 10, 0, 10, 8, 8, 10, 11, -1, -1, -1, -1, -1, -1, -1},
	{3, 1, 10, 11, 3, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{1, 2, 11, 1, 11, 9, 9, 11, 8, -1, -1, -1, -1, -1, -1, -1},
	{3, 0, 9, 3, 9, 11, 1, 2, 9, 2, 11, 9, -1, -1, -1, -1},
	{0, 2, 11, 8, 0, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{3, 2, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{2, 3, 8, 2, 8, 10, 10, 8, 9, -1, -1, -1, -1, -1, -1, -1},
	{9, 10, 2, 0, 9, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{2, 3, 8, 2, 8, 10, 0, 1, 8, 1, 10, 8, -1, -1, -1, -1},
	{1, 10, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{1, 3, 8, 9, 1, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{0, 9, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{0, 3, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
	{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}
    };

    /** main. */
    public static void main(String argv[]){
	

	/* Default contour level. */
	float contour = 2.0f;
	
	/* Default filename for tmesh. */
	String tmeshFilename = null;
	
	/* Filename for contour file. */
	String cntFilename = argv[0];
	
	if(cntFilename != ""){
	    
	    /* Sort out command line params. */
	    if(argv.length > 1){
		contour = (float)FILE.readDouble(argv[1]);
	    }
	    
	    if(argv.length > 2){
		tmeshFilename = argv[2];
	    }
	    
	    /* Create grid object. */
	    //Grid grid = new Grid(cntFilename);
	    

	    //long then = System.currentTimeMillis();
	    /** Create March object. */
	    //March march = new March(grid, contour);
	    //System.out.println("Time: " + (int)(System.currentTimeMillis() - then));
	    
	    /* Smooth tmesh */
	    //march.tmesh.smooth(0.2f);
	    

	    /** Output tmesh. */
	    //if(tmeshFilename != null){
	    //	march.tmesh.output(tmeshFilename);
	    //}
	}	
	
    }
}
