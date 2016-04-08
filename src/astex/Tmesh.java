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

/**
 * Tmesh.java - Mike's Tmesh class.
 *
 * Object for storing a triangular mesh object.
 */

import java.io.*;
import java.util.*;

public class Tmesh {
    /** The number of points in the object. */
    public int np = 0;

    /** The number of points allocated for the object. */
    public int npalloc = 0;

    /** The x coordinates of the points. */
    public float x[] = null;

    /** The y coordinates of the points. */
    public float y[] = null;

    /** The z coordinates of the points. */
    public float z[] = null;

    /** The normal vectors of the points. */
    public float nx[] = null;

    /** The normal vectors of the points. */
    public float ny[] = null;

    /** The normal vectors of the points. */
    public float nz[] = null;

    /** The texture coordinates of the points. */
    public float u[] = null;

    /** The texture coordinates of the points. */
    public float v[] = null;

    /** The color the points. */
    public int vcolor[] = null;

    /** The texture map for this object. */
    Texture texture = null;

    /** The transparency of the object. */
    public int transparency = 0xff;
    
    /** The texture scaling mode. */
    private int textureScalingMode = OriginalScalingMode;

    /** The original textureScalingMode. */
    public static final int OriginalScalingMode = 0;

    /** The min,max textureScalingMode. */
    public static final int MinMaxScalingMode = 1;

    /** The minimum u texture coord. */
    public float umin = 0.0f;

    /** The maximum u texture coord. */
    public float umax = 1.0f;

    /** The minimum v texture coord. */
    public float vmin = 0.0f;

    /** The maximum u texture coord. */
    public float vmax = 1.0f;

    /** The offset for the u texture coord. */
    private float uoffset = 0.0f;

    /** The scale for the u texture coord. */
    private float uscale = 1.0f;

    /** The offset for the v texture coord. */
    private float voffset = 0.0f;

    /** The scale for the v texture coord. */
    private float vscale = 1.0f;

    public static final int UScale = 1;
    public static final int VScale = 2;
    public static final int UOffset = 3;
    public static final int VOffset = 4;

    public static final int UTexture = 1;
    public static final int VTexture = 2;

    /** Sphere graphical object. */
    public Tmesh spheres = null;

    /** Cylinder graphical object. */
    public Tmesh cylinders = null;

    /** Sphere graphical object. */
    public Tmesh lines = null;

    /** The number of triangles in the object. */
    public int nt = 0;

    /** The number of triangles allocated. */
    int ntalloc = 0;

    /** The indices of the points in the triangles. */
    public int t0[] = null;

    /** The indices of the points in the triangles. */
    public int t1[] = null;

    /** The indices of the points in the triangles. */
    public int t2[] = null;

    /** The indices of the points in the triangles. */
    public int tcolor[] = null;

    /** The number of points we will grow a tmesh by. */
    static final int PointAllocationIncrement = 4096;

    /** The number of triangles we will grow a tmesh by. */
    static final int TriangleAllocationIncrement = 8192;

    /** Renderering Style. */
    public int style = TRIANGLES;

    /** Types for style. */
    public static final int DOTS = 1;

    /** Types for style. */
    public static final int LINES = 2;

    /** Types for style. */
    public static final int TRIANGLES = 3;

    /** Types for style. */
    public static final int CYLINDERS = 4;

    /** Types for style. */
    public static final int SPHERES = 5;

    /** Color style. */
    public int colorStyle = ObjectColor;

    /** The color style possibilities. */
    public static final int ObjectColor = 1;

    /** The color style possibilities. */
    public static final int TriangleColor = 2;

    /** The color style possibilities. */
    public static final int VertexColor = 3;

    /** Debugging on? */
    public boolean debug = false;

    /** Set the uscale. */
    public void setUScale(double f){
	uscale = (float)f;
    }

    /** Set the vscale. */
    public void setVScale(double f){
	vscale = (float)f;
    }

    /** Set the uoffset. */
    public void setUOffset(double f){
	uoffset = (float)f;
    }

    /** Set the voffset. */
    public void setVOffset(double f){
	voffset = (float)f;
    }

    /** Get the uscale. */
    public double getUScale(){
	return uscale;
    }

    /** Get the vscale. */
    public double getVScale(){
	return vscale;
    }

    /** Get the uoffset. */
    public double getUOffset(){
	return uoffset;
    }

    /** Get the voffset. */
    public double getVOffset(){
	return voffset;
    }

    /** Empty an object. */
    public void empty(){
	np = 0;
	nt = 0;
    }

    /** Get inverse texture coordinate. */
    public double getInverseTexture(int uv, double val){
	double inv = 0.0;
	if(uv == UTexture){
	    inv = uoffset + (val/uscale);
	}else if(uv == VTexture){
	    inv = voffset + (val/vscale);
	}else{
	    Log.error("texture coordinate must be 1 or 2, not "+ uv);
	}
	
	return inv;
    }

    /** Set the texture range. */
    public void setTextureRange(int uv, double min, double max){
	// protect against equal min max values
	double delta = Math.abs(max - min);

	if(delta < 1.e-3){
	    delta = 1.e-3;
	}
	    
	double scale = 1./delta;

	if(uv == UTexture){
	    setUOffset(min);
	    setUScale(scale);
	}else if(uv == VTexture){
	    setVOffset(min);
	    setVScale(scale);
	}else{
	    Log.error("texture coordinate must be 1 or 2, not "+ uv);
	}
    }

    /** Set min and max for the specified texture coordinate. */
    public void setMinMax(int uv){
	double tmin =  1.e10;
	double tmax = -1.e10;
	
	int pointCount = np;

	if(uv == UTexture || uv == VTexture){
	    float tt[] = null;
	    if(uv == Tmesh.UTexture){
		tt = u;
	    }else if(uv == Tmesh.VTexture){
		tt = v;
	    }
	    for(int i = 0; i < pointCount; i++){
		double t = tt[i];

		if(t < tmin) tmin = t;
		if(t > tmax) tmax = t;
	    }

	    setTextureRange(uv, tmin, tmax);
	}else{
	    Log.error("texture coordinate must be 1 or 2, not "+ uv);
	}
    }

    /** Set the object color style. */
    public void setColorStyle(int s){
	colorStyle = s;
	if(lines != null) lines.setColorStyle(s);
	if(spheres != null) spheres.setColorStyle(s);
	if(cylinders != null) cylinders.setColorStyle(s);
    }

    /** Get the object color style. */
    public int getColorStyle(){
	return colorStyle;
    }

    /** Create a tmesh object. */
    public Tmesh(){
    }

    /** Is this object visible. */
    boolean visible = true;
    
    /**
     * Get the value of visible.
     * @return value of visible.
     */
    public boolean isVisible() {
	return visible;
    }

    /** Set the texture scaling mode. */
    public void setTextureScalingMode(int sm){
	textureScalingMode = sm;
    }
    
    /** Get the texture scaling mode. */
    public int getTextureScalingMode(){
	return textureScalingMode;
    }
    
    /**
     * Set the value of visible.
     * @param v  Value to assign to visible.
     */
    public void setVisible(boolean  v) {
	this.visible = v;
    }

    /** Does this object show backfacing triangles. */
    boolean backface;
    
    /**
     * Get the value of backface.
     * @return value of backface.
     */
    public boolean getBackface() {
	return backface;
    }
    
    /**
     * Set the value of backface.
     * @param v  Value to assign to backface.
     */
    public void setBackface(boolean  v) {
	this.backface = v;
    }

    /** Base color of this object. */
    int color = 0x00ff00;
    
    /**
     * Get the value of color.
     * @return value of color.
     */
    public int getColor() {
	return color;
    }
    
    /**
     * Set the value of color.
     * @param v  Value to assign to color.
     */
    public void setColor(int  v) {
	this.color = v;
	// set the texture to null if we change the
	// color.
	this.texture = null;
	if(lines != null) lines.setColor(v);
	if(spheres != null) spheres.setColor(v);
	if(cylinders != null) cylinders.setColor(v);
    }
    
    /** The name of the object. */
    public String name;

    /**
     * Get the value of name.
     * @return value of name.
     */
    public String getName() {
	return name;
    }

    /** Set the transparency. */
    public void setTransparency(int t){
	if(t < 0){
	    System.out.println("setTransparency: illegal transparency "+ t);
	    t = 0;
	}else if(t > 255){
	    System.out.println("setTransparency: illegal transparency "+ t);
	    t = 255;
	}

	transparency = t;
    }
    
    /**
     * Set the value of name.
     * @param v  Value to assign to name.
     */
    public void setName(String  v) {
	this.name = v;
    }

    /** Set whether this point is clipped. */
    public void setClipped(int i, boolean state){
    }

    /** Default line width (-1.0 indicates line of 1 pixel width). */
    private double lineWidth = -1.0;

    /** Set the linewidth. */
    public void setLineWidth(double d){
	lineWidth = d;

	if(lines != null){
	    lines.setLineWidth(d);
	}
    }

    /** Set the linewidth. */
    public double getLineWidth(){
	return lineWidth;
    }

    /** Add a point to the object. */
    public int addPoint(Point3d x, Point3d n, double tu, double tv){
	return addPoint(x.x, x.y, x.z, n.x, n.y, n.z, tu, tv);
    }
	
    /** Add a point to the object. */
    public int addPoint(double x[], double n[], double tu, double tv){
	return addPoint(x[0], x[1], x[2], n[0], n[1], n[2], tu, tv);
    }
	
    /** Add a point to the object. */
    public int addPoint(double xp, double yp, double zp,
			double xn, double yn, double zn,
			double tu, double tv){
	
	ensurePointCapacity();

	x[np] = (float)xp;
	y[np] = (float)yp;
	z[np] = (float)zp;
	nx[np] = (float)xn;
	ny[np] = (float)yn;
	nz[np] = (float)zn;
	u[np] = (float)tu;
	v[np] = (float)tv;
	vcolor[np] = 0;

	return np++;
    }

    public int addPoint(double xp, double yp, double zp, int c){
	ensurePointCapacity();
	x[np] = (float)xp;
	y[np] = (float)yp;
	z[np] = (float)zp;
	vcolor[np] = c;
	return np++;
    }

    /** Get the vertex info for the specified point. */
    public void getVertex(int v, double px[], double nxx[]){
	px[0] = x[v];
	px[1] = y[v];
	px[2] = z[v];
	if(nxx != null){
	    nxx[0] = nx[v];
	    nxx[1] = ny[v];
	    nxx[2] = nz[v];
	}
    }

    /** Set the vertex info for the specified point. */
    public void setVertex(int v, double px[], double nxx[]){
	x[v] = (float)px[0];
	y[v] = (float)px[1];
	z[v] = (float)px[2];
	if(nxx != null){
	    nx[v] = (float)nxx[0];
	    ny[v] = (float)nxx[1];
	    nz[v] = (float)nxx[2];
	}
    }

    /** Get the number of points. */
    public int getnPoints(){
	return np;
    }

    /** Set the initial capacity for points. */
    public void setPointCapacity(int nn){
	npalloc = nn;
	x = new float[npalloc];
	y = new float[npalloc];
	z = new float[npalloc];
	nx = new float[npalloc];
	ny = new float[npalloc];
	nz = new float[npalloc];
	u = new float[npalloc];
	v = new float[npalloc];
	vcolor = new int[npalloc];
    }

    /** Make sure we have room for the next point. */
    public void ensurePointCapacity(){
	if(np == npalloc){
	    npalloc += PointAllocationIncrement;

	    float newx[] = new float[npalloc];
	    float newy[] = new float[npalloc];
	    float newz[] = new float[npalloc];
	    float newnx[] = new float[npalloc];
	    float newny[] = new float[npalloc];
	    float newnz[] = new float[npalloc];
	    float newu[] = new float[npalloc];
	    float newv[] = new float[npalloc];
	    int newvcolor[] = new int[npalloc];

	    if(np != 0){
		for(int i = 0; i < np; i++){
		    newx[i] = x[i];
		    newy[i] = y[i];
		    newz[i] = z[i];
		    newnx[i] = nx[i];
		    newny[i] = ny[i];
		    newnz[i] = nz[i];
		    newu[i] = u[i];
		    newv[i] = v[i];
		    newvcolor[i] = vcolor[i];
		}
	    }

	    x = newx;
	    y = newy;
	    z = newz;
	    nx = newnx;
	    ny = newny;
	    nz = newnz;
	    u = newu;
	    v = newv;
	    vcolor = newvcolor;
	}
    }

    /** Add a line to the object. */
    public int addLine(int i, int j, int c){
	if(i == -1 || j == -1){
	    System.out.println("addLine i " + i + " j " + j + " faceIndex "+ c);
	    return nt;
	}

	ensureTriangleCapacity();

	t0[nt] = i;
	t1[nt] = j;
	t2[nt] = -1;
	tcolor[nt] = c;
	return nt++;
    }


    public int addTriangle(int i, int j, int k){
	return addTriangle(i, j, k, 0);
    }

    public int addTriangle(int i, int j, int k, int color){
	ensureTriangleCapacity();

	if((i == j || i == k || j == k) || debug){
	    System.out.println("tmesh.addTriangle i=" +
			       i + " j=" + j + " k=" + k);
	}

	if(i != -1 && j != -1 && k != -1){

	    t0[nt] = i;
	    t1[nt] = j;
	    t2[nt] = k;
	    tcolor[nt] = color;

	    return nt++;
	}else{
	    return -1;
	}
    }

    /** Set the initial capacity for triangles. */
    public void setTriangleCapacity(int nn){
	ntalloc = nn;
	t0 = new int[ntalloc];
	t1 = new int[ntalloc];
	t2 = new int[ntalloc];
	tcolor = new int[ntalloc];
    }

    /** Make sure we have room for the next triangle. */
    public void ensureTriangleCapacity(){
	if(nt == ntalloc){
	    ntalloc += TriangleAllocationIncrement;

	    int newt0[] = new int[ntalloc];
	    int newt1[] = new int[ntalloc];
	    int newt2[] = new int[ntalloc];
	    int newtcolor[] = new int[ntalloc];
	    if(nt != 0){
		for(int i = 0; i < nt; i++){
		    newt0[i] = t0[i];
		    newt1[i] = t1[i];
		    newt2[i] = t2[i];
		    newtcolor[i] = tcolor[i];
		}
	    }

	    t0 = newt0;
	    t1 = newt1;
	    t2 = newt2;
	    tcolor = newtcolor;
	}
    }

    /** Add a sphere to this tmesh. */
    public void addSphere(double xx, double yy, double zz, double rr, int cc){
	if(style != SPHERES){
	    if(spheres == null){
		spheres = new Tmesh();
		spheres.style = SPHERES;
	    }

	    spheres.addSphere(xx, yy, zz, rr, cc);
	}else{
	    ensurePointCapacity();

	    x[np] = (float)xx;
	    y[np] = (float)yy;
	    z[np] = (float)zz;
	    vcolor[np] = cc;
	    /** Store radius in nx. */
	    nx[np] = (float)rr;
	    
	    np++;
	}
    }

    /** Add a line to this mesh. */
    public void addLine(double x1, double y1, double z1,
			double x2, double y2, double z2,
			int c1, int c2){
	addCylinder(x1, y1, z1, x2, y2, z2, 0.0, c1, c2);
	
    }

    /** Add a cylinder to this tmesh. */
    public void addCylinder(double x1, double y1, double z1,
			    double x2, double y2, double z2, double r,
			    int c1, int c2){

	if(style != CYLINDERS){
	    if(cylinders == null){
		cylinders = new Tmesh();
		cylinders.style = CYLINDERS;
		cylinders.colorStyle = VertexColor;
	    }

	    cylinders.addCylinder(x1, y1, z1, x2, y2, z2, r, c1, c2);
	}else{
	    ensurePointCapacity();

	    x[np] = (float)x1;
	    y[np] = (float)y1;
	    z[np] = (float)z1;
	    vcolor[np] = c1;
	    /** Store radius in nx. */
	    nx[np] = (float)r;
	    
	    np++;

	    ensurePointCapacity();

	    x[np] = (float)x2;
	    y[np] = (float)y2;
	    z[np] = (float)z2;
	    vcolor[np] = c2;
	    /** Store radius in nx. */
	    nx[np] = (float)r;

	    np++;

	    ensureTriangleCapacity();

	    t0[nt] = np - 2;
	    t1[nt] = np - 1;
	    
	    nt++;
	}
    }

    /** Read a ply file. */
    public static Tmesh readPly(FILE f){
	System.out.println("reading ply file");
	
	Tmesh tmesh = new Tmesh();

	int vcount = 0;
	int fcount = 0;

	while(f.nextLine()){
	    int fieldCount = f.getFieldCount();
	    if(fieldCount == 3 &&
	       f.getField(0).equals("element")){
		if(f.getField(1).equals("vertex")){
		    vcount = f.getInteger(2);
		    tmesh.setPointCapacity(vcount);
		    System.out.println("vertex count " + vcount);
		}else if(f.getField(1).equals("face")){
		    fcount = f.getInteger(2);
		    tmesh.setTriangleCapacity(fcount);
		    System.out.println("face count " + fcount);
		}
	    }else if(fieldCount == 1 && f.getField(0).equals("end_header")){
		System.out.println("seen end of header");
		break;
	    }
	}

	for(int i = 0; i < vcount; i++){
	    f.nextLine();
	    int fieldCount = f.getFieldCount();
	    float x, y, z;
	    float u = 0.0f, v = 0.0f;
	    x = f.getFloat(0);
	    y = f.getFloat(1);
	    z = f.getFloat(2);

	    if(fieldCount == 5){
		u = v = f.getFloat(4);
	    }
	    
	    tmesh.addPoint((float)f.getDouble(0),
			   (float)f.getDouble(1),
			   (float)f.getDouble(2),
			   0.0, 0.0, 0.0, u, v);

	    if(i != 0 && i % 100000 == 0){
		System.out.println("vertex " + i);
	    }
	}

	for(int i = 0; i < fcount; i++){
	    f.nextLine();
	    int fieldCount = f.getFieldCount();
	    
	    if(fieldCount == 4){
		tmesh.addTriangle(f.getInteger(1),
				  f.getInteger(2),
				  f.getInteger(3));
	    }else{
		System.out.println("more than 4 fields on face record.");
		break;
	    }

	    if(i != 0 && i % 100000 == 0){
		System.out.println("face " + i);
	    }
	}

	double a[] = new double[3];
	double b[] = new double[3];
	double n[] = new double[3];

	for(int i = 0;i < tmesh.nt; i++){
	    int ti = tmesh.t0[i];
	    int tj = tmesh.t1[i];
	    int tk = tmesh.t2[i];

	    a[0] = tmesh.x[ti] - tmesh.x[tj];
	    a[1] = tmesh.y[ti] - tmesh.y[tj];
	    a[2] = tmesh.z[ti] - tmesh.z[tj];
	    b[0] = tmesh.x[tk] - tmesh.x[tj];
	    b[1] = tmesh.y[tk] - tmesh.y[tj];
	    b[2] = tmesh.z[tk] - tmesh.z[tj];

	    Point3d.cross(n, b, a);

	    tmesh.nx[ti] += n[0];
	    tmesh.ny[ti] += n[1];
	    tmesh.nz[ti] += n[2];
	    tmesh.nx[tj] += n[0];
	    tmesh.ny[tj] += n[1];
	    tmesh.nz[tj] += n[2];
	    tmesh.nx[tk] += n[0];
	    tmesh.ny[tk] += n[1];
	    tmesh.nz[tk] += n[2];
	}

	for(int i = 0;i < tmesh.np; i++){
	    double dx = tmesh.nx[i];
	    double dy = tmesh.ny[i];
	    double dz = tmesh.nz[i];
	    double len = Math.sqrt(dx*dx+dy*dy+dz*dz);
	    
	    tmesh.nx[i] /= len;
	    tmesh.ny[i] /= len;
	    tmesh.nz[i] /= len;
	}

	return tmesh;
    }

    /** Create a tmesh from a file. */
    public static Tmesh read(String file){
	FILE f = FILE.open(file);

	if(f == null){
	    FILE.getException().printStackTrace();
	    return null;
	}

	if(file.indexOf(".ply") != -1){
	    return readPly(f);
	}else if(file.indexOf(".gro") != -1){
	    //return GraphicalObject.readGraphicalObject(f);
	}

	f.nextLine();

	Tmesh tmesh = new Tmesh();
	tmesh.name = file;

	int nv = f.readIntegerFromField(0);

	tmesh.setPointCapacity(nv);
	
	for(int i = 0; i < nv; i++){
	    f.nextLine();
	    double vx = f.readDoubleFromField(0);
	    double vy = f.readDoubleFromField(1);
	    double vz = f.readDoubleFromField(2);
	    double nx = f.readDoubleFromField(3);
	    double ny = f.readDoubleFromField(4);
	    double nz = f.readDoubleFromField(5);
	    // field 6 is the colour index
	    double u = f.readDoubleFromField(7);
	    double v = f.readDoubleFromField(8);
	    
	    tmesh.addPoint(vx, vy, vz, nx, ny, nz, u, v);
	}

	f.nextLine();
	int ntm = f.readIntegerFromField(0);

	// guess number of triangles as about 2*nv
	tmesh.setTriangleCapacity(2 * nv);

	System.out.println("number of tmeshes " + ntm);

	int vertices[] = new int[100];

	int tc = 0;

	for(int i = 0; i < ntm; i++){
	    f.nextLine();
	    int np = f.readIntegerFromField(0);

	    if(np > vertices.length){
		vertices = new int[np];
	    }

	    tc = 0;

	    if(f.getFieldCount() > 1){
		tc = f.readIntegerFromField(1);
	    }

	    for(int j = 0; j < np; j++){
		f.nextLine();
		vertices[j] = f.readIntegerFromField(0);
	    }
	    
	    for(int j = 0; j < np - 2; j++){
		tmesh.addTriangle(vertices[j],vertices[j+1],vertices[j+2], tc);
	    }
	}

	f.close();

	return tmesh;
    }

    /** Distance between two points. */
    public double distance(int v0, int v1){
	double dx = x[v0] - x[v1];
	double dy = y[v0] - y[v1];
	double dz = z[v0] - z[v1];

	return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    /** Distance between vertex and point. */
    public double distance(int v0, double p[]){
	double dx = x[v0] - p[0];
	double dy = y[v0] - p[1];
	double dz = z[v0] - p[2];

	return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    /** Find mid point of two vertices. */
    public void mid(double v[], int v0, int v1){
	v[0] = 0.5 * (x[v0] + x[v1]);
	v[1] = 0.5 * (y[v0] + y[v1]);
	v[2] = 0.5 * (z[v0] + z[v1]);
    }

    /** Prints the tmesh to a file. */
    public void output(String filename){
	int i;

	FILE output = FILE.write(filename);

	if(output == null){
	    System.err.println("tmesh.output() couldn't open " + filename);
	    return;
	}

	output.print("%d\n", np);
	for(i = 0; i < np; i++){
	    output.print(" %.3f", x[i]);
	    output.print(" %.3f", y[i]);
	    output.print(" %.3f", z[i]);
	    output.print(" %.3f", nx[i]);
	    output.print(" %.3f", ny[i]);
	    output.print(" %.3f", nz[i]);
            output.print(" 0");
            if(u != null){
                output.print(" %.3f", u[i]);
            }else{
                output.print(" 0");
            }
            if(v != null){
                output.print(" %.3f", v[i]);
            }else{
                output.print(" 0");
            }
	    output.println("");
	}

	output.print("%d\n", nt);
	for(i = 0; i < nt; i++){
	    output.println("3");
	    output.print("%d\n", t0[i]);
	    output.print("%d\n", t1[i]);
	    output.print("%d\n", t2[i]);
	}
	    
	output.close();
    }

    /** Find simple bounding sphere of the tmesh. */
    public double getBoundingSphere(double c[]){
	if(c == null){
	    System.out.println("tmesh " + name + ": null array for center");
	    return 0.0;
	}

	c[0] = c[1] = c[2] = 0.0;

	if(np > 0){
	    for(int i = 0; i < np; i++){
		c[0] += x[i];
		c[1] += y[i];
		c[2] += z[i];
	    }

	    c[0] /= np;
	    c[1] /= np;
	    c[2] /= np;

	    double rsq = 0.0;

	    for(int i = 0; i < np; i++){
		double dx = c[0] - x[i];
		double dy = c[1] - y[i];
		double dz = c[2] - z[i];

		double rnew = dx*dx + dy*dy + dz*dz;

		if(rnew > rsq){
		    rsq = rnew;
		}
	    }

	    return Math.sqrt(rsq);
	}else{
	    return 0.0;
	}
    }

    private Hashtable clipHash = null;

    /** 
     * Clip Tmesh to those triangles and points that have
     * visible texture coordinates.
     */
    public void clip(int uv){
	int pclip[] = new int[np];
	int used[] = new int[np];
	int remainingPoints = 0;

	clipHash = new Hashtable();

	System.out.println("Tmesh.clip: clipping " + getName());

	System.out.println("Initial points " + np + " triangles " + nt);
	// clip and reorder the points.
	for(int i = 0; i < np; i++){
	    int clipped = 0;
	    
	    if((uv | 1) != 0){
		double ut = uscale * (u[i] - uoffset);
		if(ut < 0.0 || ut > 1.0) clipped = 1;
	    }

	    if(clipped == 0 && ((uv | 2) != 0)){
		double vt = vscale * (v[i] - voffset);
		if(vt < 0.0 || vt > 1.0) clipped = 1;
	    }

	    pclip[i] = clipped;
	    used[i] = 1 - clipped;
	}

	// clip the triangles
	int remainingTriangles = 0;

	int t0new[]     = new int[2*nt];
	int t1new[]     = new int[2*nt];
	int t2new[]     = new int[2*nt];
	int tcolornew[] = new int[2*nt];

	for(int i = 0; i < nt; i++){
	    int totalUsed = used[t0[i]] + used[t1[i]] + used[t2[i]];
	    
	    if(totalUsed == 3){
		// simple case take whole triangle
		t0new[remainingTriangles] = t0[i];
		t1new[remainingTriangles] = t1[i];
		t2new[remainingTriangles] = t2[i];
		tcolornew[remainingTriangles] = tcolor[i];
		remainingTriangles++;
	    }else if(totalUsed == 1){
		// next most simple case...
		// one vertex left in
		int v0 = -1, v1 = -1, v2 = -1;

		if(used[t0[i]] == 1){
		    v0 = t0[i];
		    v1 = addClipVertex(t0[i], t1[i], uv);
		    v2 = addClipVertex(t0[i], t2[i], uv);
		}else if(used[t1[i]] == 1){
		    v0 = t1[i];
		    v1 = addClipVertex(t1[i], t2[i], uv);
		    v2 = addClipVertex(t1[i], t0[i], uv);
		}else if(used[t2[i]] == 1){
		    v0 = t2[i];
		    v1 = addClipVertex(t2[i], t0[i], uv);
		    v2 = addClipVertex(t2[i], t1[i], uv);
		}

		if(v0 != -1 && v1 != -1 && v2 != -1){
		    t0new[remainingTriangles] = v0;
		    t1new[remainingTriangles] = v1;
		    t2new[remainingTriangles] = v2;
		    tcolornew[remainingTriangles] = tcolor[i];
		    remainingTriangles++;
		}else{
		    System.out.println("skipping triangle");
		}
	    }else if(totalUsed == 2){
		// most complicated case
		// two vertices left in
		// produces two triangles
		int v0 = -1, v1 = -1, v2 = -1, va = -1, vb = -1;

		if(used[t0[i]] == 0){
		    v0 = t0[i];
		    v1 = t1[i];
		    v2 = t2[i];
		    va = addClipVertex(t0[i], t1[i], uv);
		    vb = addClipVertex(t0[i], t2[i], uv);
		}else if(used[t1[i]] == 0){
		    v0 = t1[i];
		    v2 = t0[i];
		    v1 = t2[i];
		    va = addClipVertex(t1[i], t2[i], uv);
		    vb = addClipVertex(t1[i], t0[i], uv);
		}else if(used[t2[i]] == 0){
		    v0 = t2[i];
		    v1 = t0[i];
		    v2 = t1[i];
		    va = addClipVertex(t2[i], t0[i], uv);
		    vb = addClipVertex(t2[i], t1[i], uv);
		}

		if(v0 != -1 && v1 != -1 && v2 != -1 && va != -1 && vb != -1){
		    t0new[remainingTriangles] = va;
		    t1new[remainingTriangles] = v1;
		    t2new[remainingTriangles] = v2;
		    tcolornew[remainingTriangles] = tcolor[i];
		    remainingTriangles++;
		    t0new[remainingTriangles] = va;
		    t1new[remainingTriangles] = vb;
		    t2new[remainingTriangles] = v2;
		    tcolornew[remainingTriangles] = tcolor[i];
		    remainingTriangles++;
		}else{
		    System.out.println("skipping triangle");
		}
	    }
	}

	nt = remainingTriangles;

	t0     = t0new;
	t1     = t1new;
	t2     = t2new;
	tcolor = tcolornew;

	pclip = new int[np];
	used  = new int[np];

	for(int i = 0; i < np; i++){
	    used[i] = 0;
	}

	for(int i = 0; i < nt; i++){
	    used[t0[i]] = 1;
	    used[t1[i]] = 1;
	    used[t2[i]] = 1;
	}

	// pack down the remaining visible triangles

	remainingPoints = 0;
	
	for(int i = 0; i < np; i++){
 	    if(used[i] == 1){
		x[remainingPoints] = x[i];
		y[remainingPoints] = y[i];
		z[remainingPoints] = z[i];
		nx[remainingPoints] = nx[i];
		ny[remainingPoints] = ny[i];
		nz[remainingPoints] = nz[i];
		u[remainingPoints] = u[i];
		v[remainingPoints] = v[i];
		vcolor[remainingPoints] = vcolor[i];
		pclip[i] = remainingPoints++;
	    }else{
		pclip[i] = -1;
	    }
	}	

	np = remainingPoints;

	for(int i = 0; i < nt; i++){
	    t0[i] = pclip[t0[i]];
	    t1[i] = pclip[t1[i]];
	    t2[i] = pclip[t2[i]];
	}

	System.out.println("Final   points " + np + " triangles " + nt);	
    }

    private int addClipVertex(int v0, int v1, int uv){
	if(v0 == -1 || v1 == -1){
	    System.out.println("vertex is out of use v0 " + v0 + " v1 " + v1);
	    return -1;
	}

	Integer hashVal = null;

	if(v0 < v1){
	    hashVal = new Integer(v0 + 1000000*v1);
	}else{
	    hashVal = new Integer(v1 + 1000000*v0);
	}

	Integer newVertex = (Integer)clipHash.get(hashVal);

	if(newVertex != null){
	    return newVertex.intValue();
	}

	newVertex = new Integer(np);

	double t0 = 0.0;
	double t1 = 0.0;

	if(uv == UTexture){
	    t0 = uscale * (u[v0] - uoffset);
	    t1 = uscale * (u[v1] - uoffset);
	}else{
	    t0 = vscale * (v[v0] - voffset);
	    t1 = vscale * (v[v1] - voffset);
	}

	if(t0 >= 0.0 && t1 >= 0.0 && 
	   t0 <= 1.0 && t1 <= 1.0){
	    System.out.println("t0 " + t0);
	    System.out.println("t1 " + t1);
	    System.out.println("edge shouldn't be clipped, both points are in 0-1");
	    return -1;
	}

	double frac = (1.0 - t0)/(t1 - t0); 

	if(frac < 0.0 || frac > 1.0){
	    System.out.println("t0 " + t0);
	    System.out.println("t1 " + t1);
	    System.out.println("frac " + frac);
	    return -1;
	}

        ensurePointCapacity();

	x[np]  = (float)( x[v0] + frac *  (x[v1] -  x[v0]));
	y[np]  = (float)( y[v0] + frac *  (y[v1] -  y[v0]));
	z[np]  = (float)( z[v0] + frac *  (z[v1] -  z[v0]));
	nx[np] = (float)(nx[v0] + frac * (nx[v1] - nx[v0]));
	ny[np] = (float)(ny[v0] + frac * (ny[v1] - ny[v0]));
	nz[np] = (float)(nz[v0] + frac * (nz[v1] - nz[v0]));
	u[np]  = (float)( u[v0] + frac *  (u[v1] -  u[v0]));
	v[np]  = (float)( v[v0] + frac *  (v[v1] -  v[v0]));
	vcolor[np] = Color32.blend(vcolor[v0], vcolor[v1], frac);

	double len = nx[np]*nx[np];
	len += ny[np]*ny[np];
	len += nz[np]*nz[np];
	len = Math.sqrt(len);

	nx[np] /= len;
	ny[np] /= len;
	nz[np] /= len;

	clipHash.put(hashVal, newVertex);

	np++;

	return newVertex.intValue();
    }

    private Point3d ab = new Point3d();
    private Point3d bc = new Point3d();
    private Point3d norm = new Point3d();
    private Point3d origNorm = new Point3d();

    /** Recalculate normals. */
    public void recalculateNormals(){
	double newnx[] = new double[np];
	double newny[] = new double[np];
	double newnz[] = new double[np];

	for(int i = 0; i < nt; i++){
	    origNorm.x = nx[t0[i]] + nx[t1[i]] + nx[t2[i]];
	    origNorm.y = ny[t0[i]] + ny[t1[i]] + ny[t2[i]];
	    origNorm.z = nz[t0[i]] + nz[t1[i]] + nz[t2[i]];

	    ab.x = x[t0[i]] - x[t1[i]];
	    ab.y = y[t0[i]] - y[t1[i]];
	    ab.z = z[t0[i]] - z[t1[i]];
	    bc.x = x[t1[i]] - x[t2[i]];
	    bc.y = y[t1[i]] - y[t2[i]];
	    bc.z = z[t1[i]] - z[t2[i]];

	    Point3d.crossNoNormalise(norm, ab, bc);

	    double dot =
		norm.x*origNorm.x +
		norm.y*origNorm.y +
		norm.z*origNorm.z;

	    if(dot < 0.0){
		norm.negate();
	    }

	    newnx[t0[i]] += norm.x;
	    newny[t0[i]] += norm.y;
	    newnz[t0[i]] += norm.z;

	    newnx[t1[i]] += norm.x;
	    newny[t1[i]] += norm.y;
	    newnz[t1[i]] += norm.z;

	    newnx[t2[i]] += norm.x;
	    newny[t2[i]] += norm.y;
	    newnz[t2[i]] += norm.z;
	}

	for(int i = 0; i < np; i++){
	    double len = newnx[i]*newnx[i];
	    len += newny[i]*newny[i];
	    len += newnz[i]*newnz[i];
	    len = Math.sqrt(len);

	    nx[i] = (float)(newnx[i] / len);
	    ny[i] = (float)(newny[i] / len);
	    nz[i] = (float)(newnz[i] / len);
	}

	newnx = null;
	newny = null;
	newnz = null;
    }


    /** String representation of object. */
    public String toString(){
	return name + ": " + np + " points, " + nt + " triangles";
    }

    /** Test program. */
    public static void main(String args[]){
	Tmesh tm = Tmesh.read(args[0]);

	System.out.println(tm);
    }

    /** Maximum number of triangle per point. */
    private static final int maxTriangles = 100;

    /** Get lookup for 'like 2D' triangle array. */
    private int triangleLookup(int i, int j){
	return (i + np * j);
    }

    /** Sum of the distances three edges of the triangle. */
    private float sumTriangleDis(int i, int j, int k){
	
	float dis = 0.0f;
	
	dis += distance(i,j);
	dis += distance(j,k);
	dis += distance(i,k);
	
	return dis;
    }
    
    /** Removes triangle. */
    private void removeTriangle(int t, int p1, int p2){

	/* Set keepTriangles to false. */
	keepTriangles[t]  = false;
	keepPoints[p1]    = false;

	/* Create new position for point. */
	interpolate(p1, p2);

	/* Set all the other triangles 
	   referencing p1 to reference p2. */
	int ntri = ntriangles[p1];
	for(int ot = 0; ot < ntri; ot++){
	   
	    int lt   = triangleLookup(p1, ot);
	    int tref = triangleRef[lt];

	    /* Change the triangle reference for p1 to p2. */
	    changeTriangleRef(p1, p2, tref); 
	    
	    /* Add the reference for the new triangle that p2 to triangleRef. */
	    changePointTriangleRef(p2, tref);
	}
    }

    /** Interpolate position poisition of p1 and p2. */
    private void interpolate(int p1, int p2){

	x[p2] = 0.5f * (x[p2] + x[p1]);
	y[p2] = 0.5f * (y[p2] + y[p1]);
	z[p2] = 0.5f * (z[p2] + z[p1]);

	nx[p2] = 0.5f * (nx[p2] + nx[p1]);
	ny[p2] = 0.5f * (ny[p2] + ny[p1]);
	nz[p2] = 0.5f * (nz[p2] + nz[p1]);

	u[p2] = 0.5f * (u[p2] + u[p1]);
	v[p2] = 0.5f * (v[p2] + v[p1]);

    }

    /** changePointTriangleRef. */
    private void changePointTriangleRef(int p2, int t){

	/* Need to check if we already have it.
	   If we do just return else add the new triangle ref. */
	int ntri = ntriangles[p2];
	for(int j = 0; j < ntri; j++){
	    int lt   = triangleLookup(p2, j);
	    int tref = triangleRef[lt];	    
	    if(tref == t) return;
	}
	
	int lt = triangleLookup(p2, ntri);
	triangleRef[lt] = t;

	ntriangles[p2]++;

    }

    /** Changes triangles to reference new point. */
    private void changeTriangleRef(int p1, int p2, int t){

	/* First change triangles to reference
	   new point in the main tmesh object. */
	if(t0[t] == p1){
	    t0[t] = p2;
	    if(t1[t] == p2 || t2[t] == p2){
		keepTriangles[t] = false;
	    }
	}
	if(t1[t] == p1){
	    t1[t] = p2;
	    if(t0[t] == p2 || t2[t] == p2){
		keepTriangles[t] = false;
	    }
	}
        if(t2[t] == p1){
	    t2[t] = p2;
	    if(t0[t] == p2 || t1[t] == p2){
		keepTriangles[t] = false;
	    }
	}

    }
 
    /** Copies the contents of i to c for the
	vertex information in the tmesh. */
    private int copyUp(int c, int i){
	
	x[c]  = x[i];
	y[c]  = y[i];
	z[c]  = z[i];
	nx[c] = nx[i];
	ny[c] = ny[i];
	nz[c] = nz[i];
	u[c] = u[i];
	v[c] = v[i];
	
	c++;

	return c;	
    }

    /** Copies the contents of i to c for the
	triangle information in the tmesh. */
    private int copyUpTriangles(int c, int i){

	t0[c] = t0[i];
	t1[c] = t1[i];
	t2[c] = t2[i];

	c++;
	
	return c;
    }

    /** '2D like' array of references to each triangle
	indexed via each point. */
    private int triangleRef[] = null;

    /** Number of triangles for each point. */
    private int ntriangles[]  = null;

    /** Parallel arrays denoting whether 
	triangle/points are kept or not. */
    private boolean keepTriangles[] = null;
    private boolean keepPoints[] = null;

    /** Smooth the tmesh object. */
    public void smooth(double dlen){
	float flen = (float)dlen;
	
	/* Average distance betwen points. */
	float avDis = 0.0f;

	/* Allocate memory for arrays. */
	triangleRef   = new int[np * maxTriangles];
	ntriangles    = new int[np];
	keepTriangles = new boolean[nt];
	keepPoints    = new boolean[np];

	/* Initialise arrays. */
	for(int i = 0; i < np; i++){
	    ntriangles[i] = 0;
	    keepPoints[i] = false;
	}

	/* Get triangle information into arrays. */
	for(int i = 0; i < nt; i++){

	    int look;
	    int p0 = t0[i];
	    
	    /** Initialise keep triangles. */
	    keepTriangles[i] = true;	    

	    look = triangleLookup(p0, ntriangles[p0]);
	    triangleRef[look] = i;
	    ntriangles[p0]++;

	    int p1 = t1[i];
	    look = triangleLookup(p1, ntriangles[p1]);
	    triangleRef[look] = i;
	    ntriangles[p1]++;

	    int p2 = t2[i];
	    look = triangleLookup(p2, ntriangles[p2]);
	    triangleRef[look] = i;
	    ntriangles[p2]++;

	    /* Add sum of three distances. */
	    avDis += sumTriangleDis(p0, p1, p2);
	}

	/* Calculate avDis. Each traingle shares an 
	   edge so divide by 2*nt. Then calculate flen. */
	avDis /= (2*nt);
	flen *= avDis;
	
	for(int t = 0; t < nt; t++){

	    if(keepTriangles[t]){
		if(distance(t0[t], t1[t]) < flen){
		    removeTriangle(t, t0[t], t1[t]);
		} 
		else if(distance(t1[t], t2[t]) < flen){
		    removeTriangle(t, t1[t], t2[t]);
		}
		else if(distance(t0[t], t2[t]) < flen){
		    removeTriangle(t, t0[t], t2[t]);
		} 
	    }
	}
	
	/* Find certices will still need. */
	for(int i = 0; i < nt; i++){
	    if(keepTriangles[i]){
		keepPoints[t0[i]] = true;
		keepPoints[t1[i]] = true;
		keepPoints[t2[i]] = true;
	    }
	}


	/* Copy up all the vertices and relabel
	   the triangle references. */
	int count = 0;
	int countTriangles = 0;
	for(int i = 0; i < np; i++){
	    if(keepPoints[i]){
		count = copyUp(count, i);
		for(int j = 0; j < ntriangles[i]; j++){
		    int look = triangleRef[triangleLookup(i, j)];
		    if(keepTriangles[look]){
			if(t0[look] == i){
			    t0[look] = count - 1;		
			} 
			if(t1[look] == i){
			    t1[look] = count - 1;
			} 
			if(t2[look] == i){
			    t2[look] = count - 1;	
			} 
		    }  
		}
	    }
	}

	/* Reset the number of points. */
	np = count;

	/* Now copy up all the triangles. */
	count = 0;
	for(int i = 0; i < nt; i++){
	    if(keepTriangles[i]){		
		count = copyUpTriangles(count, i);
	    }
	}
	
	/* Reset the numbr of triangles. */
	nt = count;
	
	/* Report new number of points and triangles. */
	//System.out.println("number of vertices (after smoothing) " + np);
	//System.out.println("number of triangles (after smoothing) " + nt);
	

	/* Dereference the arrays. */
	triangleRef   = null;
	ntriangles    = null;
	keepTriangles = null;
	keepPoints    = null;

    }

    /** Copy the specified objects into a new object. */
    public static Tmesh copy(DynamicArray objects){
	Tmesh newTmesh = new Tmesh();

	int objectCount = objects.size();
	
	for(int o = 0; o < objectCount; o++){
	    Tmesh tm = (Tmesh)objects.get(o);

	    int cp = newTmesh.np;

	    for(int i = 0; i < tm.np; i++){
		newTmesh.addPoint(tm.x[i],  tm.y[i],  tm.z[i],
				  tm.nx[i], tm.ny[i], tm.nz[i],
				  tm.u[i],  tm.v[i]);

		newTmesh.vcolor[cp + i] = tm.vcolor[i];
	    }

	    for(int i = 0; i < tm.nt; i++){
		newTmesh.addTriangle(tm.t0[i] + cp, tm.t1[i] + cp, tm.t2[i] + cp,
				     tm.tcolor[i]);
	    }

	    newTmesh.setRenderPass(tm.getRenderPass());
	    newTmesh.setColorStyle(tm.getColorStyle());
	    newTmesh.texture = tm.texture;
	    newTmesh.transparency = tm.transparency;
	    newTmesh.uoffset  = tm.uoffset;
	    newTmesh.voffset  = tm.voffset;
	    newTmesh.uscale   = tm.uscale;
	    newTmesh.vscale   = tm.vscale;
	    newTmesh.backface = tm.backface;
	    newTmesh.visible  = tm.visible;

	    if(tm.spheres != null){
		DynamicArray tmp = new DynamicArray();
		tmp.add(tm.spheres);
		newTmesh.spheres = copy(tmp);
	    }
	    
	    if(tm.cylinders != null){
		DynamicArray tmp = new DynamicArray();
		tmp.add(tm.cylinders);
		newTmesh.cylinders = copy(tmp);
	    }
	    
	    if(tm.lines != null){
		DynamicArray tmp = new DynamicArray();
		tmp.add(tm.lines);
		newTmesh.lines = copy(tmp);
	    }
	}

	return newTmesh;
    }

    /** The Renderer that we belong to. */
    private Renderer renderer;

    /**
     * Get the MoleculeRenderer value.
     */
    public Renderer getRenderer() {
	return renderer;
    }

    /**
     * Set the Renderer value.
     */
    public void setRenderer(Renderer r) {
	this.renderer = r;
    }
    
    /** Which pass of the renderer do we get drawn in. */
    private int renderPass = Renderer.RenderPass;

    /**
     * Get the RenderPass value.
     * @return the RenderPass value.
     */
    public int getRenderPass() {
	return renderPass;
    }

    /**
     * Set the RenderPass value.
     * @param newRenderPass The new RenderPass value.
     */
    public void setRenderPass(int newRenderPass) {
	this.renderPass = newRenderPass;
    }

    /** 
     * Called when the Tmesh needs to draw itself.
     *
     * Render states are obtained from the moleculeRenderer
     * which gives a reference to the low lying renderer,
     * and the container which holds moleculeRenderer.
     */
    public void render(){
    }
}

