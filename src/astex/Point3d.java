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
 * 01-11-99 mjh
 *	created
 */

/**
 * A class for manipulating 3d points and vectors.
 */
public class Point3d {
    /**
     * The object space coordinates of the this point.
     */
    public double x, y, z;

    /**
     * Default constructor.
     *
     * The x, y and z coordinates are set to 0.0
     */
    public Point3d(){
	this(0.0, 0.0, 0.0);
    }

    /**
     * Constructor which allows the x, y, and z coordinates to be specified.
     */
    public Point3d(double xx, double yy, double zz){
	initialise();

	x = xx;
	y = yy;
	z = zz;
    }

    /**
     * Initialise a point.
     */
    public void initialise(){
	zero();
    }

    /**
     * Construct a 2D point with specified x and y coordinates.
     *
     * The z coordinates is set to 0.0
     */
    public Point3d(double xx, double yy){
	this(xx, yy, 0.0);
    }

    /**
     * Construct a 3D point with equal x, y and z coordinates.
     *
     * This is useful for initialising 3D bounding boxes and such like.
     */
    public Point3d(double xx){
	this(xx, xx, xx);
    }

    /**
     * Construct a 3D point from the coordinates of another Point3d.
     */
    public Point3d(Point3d p){
	set(p);
    }


    /**
     * Clone method
     */
    public Object clone() {
	Point3d copyPt = new Point3d( this );

	return (Object)copyPt;
    }


    /**
     * Set the x, y and z coordinates of this Point3d.
     *
     * Transformed and screen coordinates are not affected.
     */
    public void set(double xx, double yy, double zz){
	x = xx;
	y = yy;
	z = zz;
    }

    /**
     * Set the x, y and z coordinates to the same value.
     *
     * Transformed and screen coordinates are note affected.
     */
    public void set(double xx){
	set(xx, xx, xx);
    }

    /**
     * Set the x, y and z coordinates to the values from another Point3d.
     */
    public void set(Point3d p){
	x = p.x;
	y = p.y;
	z = p.z;
    }

    /** Set the specified component. */
    public double get(int i){
	switch(i){
	case 0: return x;
	case 1: return y;
	case 2: return z;
	default:
	    System.out.println("Point3d.get: can't get component "+ i);
	}
	return Double.MAX_VALUE;
    }

    /** Set the specified component. */
    public void set(int i, double v){
	switch(i){
	case 0: x = v; return;
	case 1: y = v; return;
	case 2: z = v; return;
	default: System.out.println("Point3d.set: can't set component " + i);
	}
    }

    /**
     * Set the x, y and z coordinates to 0.0
     *
     * Transformed and screen space coordinates are not affected.
     */
    public void zero(){
	set(0.0, 0.0, 0.0);
    }

    /** Return the x coordinate. */
    public double getX(){
	return x;
    }

    /** Return the y coordinate. */
    public double getY(){
	return y;
    }

    /** Return the z coordinate. */
    public double getZ(){
	return z;
    }

    /** Set the x coordinate. */
    public void setX(double xx){
	x = xx;
    }

    /** Set the y coordinate. */
    public void setY(double yy){
	y = yy;
    }

    /** Set the x coordinate. */
    public void setZ(double zz){
	z = zz;
    }

    /**
     * Add the x, y and z coordinates from another Point3d to the
     * coordinates of this point.
     */
    public void add(Point3d p){
	x += p.x;
	y += p.y;
	z += p.z;
    }

    /**
     * Subtract the x, y and z coordinates from another Point3d from the
     * coordinates of this point.
     */
    public void subtract(Point3d p){
	x -= p.x;
	y -= p.y;
	z -= p.z;
    }

    /**
     * translates x and y coordinates the specified amounts
     */
    public void translate( double xtrans, double ytrans ) {
	x += xtrans;
	y += ytrans;
    }

    /**
     * translates x, y and z coordinates the specified amounts
     */
    public void translate( double xtrans, double ytrans, double ztrans ) {
	x += xtrans;
	y += ytrans;
	z += ztrans;
    }

    /**
     * Negate the x, y and z coordinates of this point.
     */
    public void negate(){
	x = -x;
	y = -y;
	z = -z;
    }

    /**
     * Find the vector minimum of the x, y and z coordinates of this point
     * and another Point3d.
     *
     * This x coordinates is set to the minimum of our x coordinate and the
     * x coordinate of the other Point3d.  The same is applied to the y and
     * z coordinates. This is useful for accumulating bounding box values.
     *
     * @see #max
     */
    public void min(Point3d p){
	if(p.x < x) x = p.x;
	if(p.y < y) y = p.y;
	if(p.z < z) z = p.z;
    }

    /**
     * Find the vector maximum of the x, y and z coordinates of this point
     * and another Point3d.
     *
     * This x coordinates is set to the maximum of our x coordinate and the
     * x coordinate of the other Point3d.  The same is applied to the y and
     * z coordinates. This is useful for accumulating bounding box values.
     *
     * @see #min
     */
    public void max(Point3d p){
	if(p.x > x) x = p.x;
	if(p.y > y) y = p.y;
	if(p.z > z) z = p.z;
    }

    /**
     * Construct a point with the x, y and z coordinates equal to the
     * midpoint of two other Point3ds.
     */
    public static Point3d mid(Point3d pmin, Point3d pmax){
	Point3d middle = new Point3d();
	middle.x = 0.5 * (pmin.x + pmax.x);
	middle.y = 0.5 * (pmin.y + pmax.y);
	middle.z = 0.5 * (pmin.z + pmax.z);

	return middle;
    }

    /**
     * Construct a point with the x, y and z coordinates equal to the
     * midpoint of two other Point3ds.
     */
    public static void mid(Point3d pmid, Point3d pmin, Point3d pmax){
	pmid.x = 0.5 * (pmin.x + pmax.x);
	pmid.y = 0.5 * (pmin.y + pmax.y);
	pmid.z = 0.5 * (pmin.z + pmax.z);
    }

    /**
     * Make the position vector of this point have unit length.
     *
     * That is, divide x, y and z by Math.sqrt(x*x + y*y + z*z).
     * If the divisor is zero no change is made.
     */
    public void normalise(){
	double norm = Math.sqrt(x*x + y*y + z*z);

	if(Math.abs(norm) > 1.e-5){
	    x /= norm;
	    y /= norm;
	    z /= norm;
	}
    }

    /**
     * Return the dot product of this vector with another one.
     */
    public double dot(Point3d p){
	return x * p.x + y * p.y + z * p.z;
    }

    /**
     * Return the length of the vector.
     */
    public double length(){
	return Math.sqrt(x*x + y*y + z*z);
    }

    /** Return length of double[] vector. */
    public static double length(double a[]){
	return Math.sqrt(a[0]*a[0] + a[1]*a[1] + a[2]*a[2]);
    }

    /**
     * Return another point, which is at the mid point of two points.
     */
    public static Point3d unitVector(Point3d p1, Point3d p2){
	/* Make a unit vector from p1 to p2. */

	Point3d unit = new Point3d(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);

	unit.normalise();

	return unit;
    }

    /** Set first point to be unitVector from p1 to p2. */
    public static void unitVector(Point3d up12, Point3d p1, Point3d p2){
	/* Make a unit vector from p1 to p2. */

	up12.set(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);

	up12.normalise();
    }

    /**
     * Return a point that is a unit vector from the first to the second.
     */
    public static Point3d unitVector(double xa, double ya,
				     double xb, double yb){
	/*
	 * Generate a unit vector from point a to b.
	 */

	double x = xb - xa;
	double y = yb - ya;
	double norm = Math.sqrt(x*x + y*y);

	if(Math.abs(norm) > 1.e-5){
	    return new Point3d(x/norm, y/norm);
	}else{
	    return new Point3d(1., 1.);
	}
    }

    /**
     * Generate a vector from the first point to the second.
     *
     * The vector does not have a length of 1.
     */
    public static Point3d vector(Point3d p1, Point3d p2){
	/* Make a vector from p1 to p2. */

	Point3d unit = new Point3d(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);

	return unit;
    }

    /** Make vector from p1 to p2 in p12. */
    public static void vector(Point3d p12, Point3d p1, Point3d p2){
	/* Make a vector from p1 to p2. */
	p12.x = p2.x - p1.x;
	p12.y = p2.y - p1.y;
	p12.z = p2.z - p1.z;
    }

    /**
     * Construct a unit vector that is perpendicular to the vector.
     *
     * If p is the null vector then (1.,1.,.1) is returned.
     */
    public static Point3d normalToLine(Point3d p){
	/*
	 * Construct a unit vector that is perpendicular to
	 * the vector described by p.  If p is the null
	 * vector then (1.,1.,.1) is returned.
	 */

	Point3d normal = new Point3d(1., 1., 1.);

	if(p.x != 0.) normal.x = (p.z + p.y) / -p.x;
	else if(p.y != 0.) normal.y = (p.x + p.z) / -p.y;
	else if(p.z != 0.) normal.z = (p.x + p.y) / -p.z;

	normal.normalise();

	return normal;
    }

    /**
     * Construct a unit vector that is perpendicular to the vector.
     */
    public static void normalToLine(Point3d p, Point3d n){
	/*
	 * Construct a unit vector that is perpendicular to
	 * the vector described by p.  If p is the null
	 * vector then (1.,1.,.1) is returned.
	 */

	n.set(1., 1., 1.);

	if(p.x != 0.) n.x = (p.z + p.y) / -p.x;
	else if(p.y != 0.) n.y = (p.x + p.z) / -p.y;
	else if(p.z != 0.) n.z = (p.x + p.y) / -p.z;

	n.normalise();
    }

    /**
     * Generate a line perpendicular to the line described
     * by the set of points xa,ya xb,yb.  The line is made to
     * be of length len.
     *
     * The generated vector always points to the right as we look from
     * above at the direction from a to b.
     */
    public static Point3d normalToLine(double xa, double ya,
				       double xb, double yb, double len){
	double x2, y2, norm;
	double x1 = xb - xa;
	double y1 = yb - ya;

	if(Math.abs(y1) < 1.e-5){
	    if(xb > xa){
		x2 = 0.0; y2 = -1.0;
	    }else{
		x2 = 0.0; y2 = 1.0;
	    }
	}else{

	    if(x1 == 0.0){
		x2 = 1.0;
		y2 = 0.0;
	    }else{
		if(y1/x1 < 0.0){
		    if(xb > xa){
			x2 = -1.0; y2 = x1/y1;
		    }else{
			x2 = 1.0; y2 = - x1/y1;
		    }
		}else{
		    if(xb > xa){
			x2 = 1.0; y2 = - x1/y1;
		    }else{
			x2 = -1.0; y2 = x1/y1;
		    }
		}
	    }
	}

	/* The arrangement above always leads to non zero values
	 * for y2 or x2.  So this division should always be safe. */

	norm = len / Math.sqrt(y2 * y2 + x2 * x2);

	x2 *= norm; y2 *= norm;

	return new Point3d(x2, y2, 0.);
    }

    public static Point3d normalToLine(double xb, double yb){
	return normalToLine(0., 0., xb, yb, 1.0);
    }

    /**
     * Evaluate the plane equation for the specified vectors.
     *
     * The result is the distance above the plane defined by origin and normal.
     * If normal is not a unit vector then the signed value simply indicates
     * if the point is above or below the plane.
     */
    public static double planeEquation(Point3d point, Point3d origin,
				       Point3d normal){
	// form vector form origin to point.
	double dx = point.x - origin.x;
	double dy = point.y - origin.y;
	double dz = point.z - origin.z;

	// form dot product (which is the value of the plane equation).
	return dx * normal.x + dy * normal.y + dz * normal.z;
    }

    /** Return cross product with c. */
    public Point3d cross(Point3d c){

	Point3d a = new Point3d((y * c.z) - (z * c.y),
				(z * c.x) - (x * c.z),
				(x * c.y) - (y * c.x));
	a.normalise();

	return a;
    }

    /** Set a to cross product of b and c. */
    public static void cross(Point3d a, Point3d b, Point3d c){

	a.set((b.y * c.z) - (b.z * c.y),
	      (b.z * c.x) - (b.x * c.z),
	      (b.x * c.y) - (b.y * c.x));

	a.normalise();
    }

    /** Set a to cross product of b and c. */
    public static void crossNoNormalise(Point3d a, Point3d b, Point3d c){

	a.set((b.y * c.z) - (b.z * c.y),
	      (b.z * c.x) - (b.x * c.z),
	      (b.x * c.y) - (b.y * c.x));
    }

    /** Generate cross product for double[] vectors. */
    public static void cross(double a[], double b[], double c[]){
	a[0] = (b[1] * c[2]) - (b[2] * c[1]);
	a[1] = (b[2] * c[0]) - (b[0] * c[2]);
	a[2] = (b[0] * c[1]) - (b[1] * c[0]);
    }

	
    /** Are this point identically equal to the specified point. */
    public boolean equal(Point3d b){
	/* Return true if points equal */

	boolean ret;

	ret = ((x == b.x) && (y == b.y) && (z == b.z));

	return ret;
    }


    /**
     * Scale the point by the specified amount.
     */
    public void scale(double len){
	x *= len; y *= len; z *= len;
    }

    /**
     * Return the distance to the specified point.
     */
    public double distance(Point3d p){
	double dx = p.x - x;
	double dy = p.y - y;
	double dz = p.z - z;

	return(Math.sqrt(dx*dx + dy*dy + dz*dz));
    }

    /** Return the square of the distance to the specified point. */
    public double distanceSq(Point3d p){
	double dx = p.x - x;
	double dy = p.y - y;
	double dz = p.z - z;

	return(dx*dx + dy*dy + dz*dz);
    }

    /** Scale a vector by the amount specified for each coordinate. */
    public void divide(double s){
	x /= s;
	y /= s;
	z /= s;
    }

    /** The absolute value that we will consider to be zero. */
    private static double smallNumber = 1.e-5;

    /** Are all the components of this vector 0. */
    public boolean isNullVector(){
	if(Math.abs(x) < smallNumber &&
	   Math.abs(y) < smallNumber &&
	   Math.abs(z) < smallNumber){
	    return true;
	}else{
	    return false;
	}
    }

    /** Transform the point by the passed matrix. */
    public Point3d transformByMatrix(Matrix m){
	Point3d copy = new Point3d(this);
	// copy the orignal coordinates
	double xx = x, yy = y, zz = z;

	// transform them.
	copy.x = xx*m.x00 + yy*m.x10 + zz*m.x20 + m.x30;
	copy.y = xx*m.x01 + yy*m.x11 + zz*m.x21 + m.x31;
	copy.z = xx*m.x02 + yy*m.x12 + zz*m.x22 + m.x32;

	return copy;
    }

    /** Static distance method. */
    public static double distance(Point3d a, Point3d b){
	double dx = a.x - b.x, dy = a.y - b.y, dz = a.z - b.z;
		
	return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    /** Static distance squared method. */
    public static double distanceSq(Point3d a, Point3d b){
	double dx = a.x - b.x, dy = a.y - b.y, dz = a.z - b.z;
		
	return dx*dx + dy*dy + dz*dz;
    }

    /** Calculate the angle between the 3 points. */
    public static double angle(Point3d a, Point3d b, Point3d c){
	double xba = a.x - b.x, yba = a.y - b.y, zba = a.z - b.z;
	double xbc = c.x - b.x, ybc = c.y - b.y, zbc = c.z - b.z;

	double ba = Math.sqrt(xba*xba + yba*yba + zba*zba);
	double bc = Math.sqrt(xbc*xbc + ybc*ybc + zbc*zbc);

	double dot = xba*xbc + yba*ybc + zba*zbc;

	dot /= (ba * bc);

	return Math.acos(dot);
    }

    /** Calcluate the angle in degrees. */
    public static double angleDegrees(Point3d a, Point3d b, Point3d c){
	return 180.0 * angle(a, b, c) / Math.PI;
    }

    private static Point3d tp1 = new Point3d();
    private static Point3d tp2 = new Point3d();
    private static Point3d tp3 = new Point3d();
    private static Point3d tp4 = new Point3d();

    public static double torsion(double p1x, double p1y, double p1z,
                                 double p2x, double p2y, double p2z,
                                 double p3x, double p3y, double p3z,
                                 double p4x, double p4y, double p4z){

        tp1.set(p1x, p1y, p1z);
        tp2.set(p2x, p2y, p2z);
        tp3.set(p3x, p3y, p3z);
        tp4.set(p4x, p4y, p4z);

        return torsion(tp1, tp2, tp3, tp4);
    }

    public static double torsionDegrees(double p1x, double p1y, double p1z,
                                        double p2x, double p2y, double p2z,
                                        double p3x, double p3y, double p3z,
                                        double p4x, double p4y, double p4z){
        
        tp1.set(p1x, p1y, p1z);
        tp2.set(p2x, p2y, p2z);
        tp3.set(p3x, p3y, p3z);
        tp4.set(p4x, p4y, p4z);

        return torsionDegrees(tp1, tp2, tp3, tp4);
    }

    /** Calculate the torsion angle between the 4 points. */
    public static double torsion(Point3d p1, Point3d p2,
				 Point3d p3, Point3d p4){
	Point3d v1, v2, v3;
	Point3d n1, n2;
	double angle;
		
	/* generate vectors between points (and normalise) */
	v1 = unitVector(p1, p2);
	v2 = unitVector(p2, p3);
	v3 = unitVector(p3, p4);
		
	/* form xprods and normalise */
	n1 = v1.cross(v2); n1.normalise();
	n2 = v2.cross(v3); n2.normalise();
		
	/*
	 * get the angle between xprods and figure out whether to negate it
	 *
	 * if n1 points in the opposite direction to v3 the angle should be
	 * negated
	 */

	double dot = n1.dot(n2);

	// ensure that the dot product lies
	// within -1.0/+1.0
	if(dot > 1.0) dot = 1.0;
	else if(dot < -1.0) dot = -1.0;

        angle = Math.acos(dot);
	
	if(n1.dot(v3) < 0.0) angle = -angle;
		
	return angle;
    }

    /** Calculate torsion in degrees. */
    public static double torsionDegrees(Point3d p1, Point3d p2,
					Point3d p3, Point3d p4){
	return 180.0 * torsion(p1, p2, p3, p4) / Math.PI;
    }


    /** Transform this atom to screen coordinates. */
    public void transform(Matrix m){
	double xx = x*m.x00 + y*m.x10 + z*m.x20 + m.x30;
	double yy = x*m.x01 + y*m.x11 + z*m.x21 + m.x31;
	double zz = x*m.x02 + y*m.x12 + z*m.x22 + m.x32;

	x = xx;
	y = yy;
	z = zz;
    }

    public static void print(String s, double x[]){
        FILE.out.print(s);
        FILE.out.print(" %8.3f", x[0]);
        FILE.out.print(" %8.3f", x[1]);
        FILE.out.print(" %8.3f\n", x[2]);
    }

    /** Return a string representation of this point. */
    public String toString(){
	return
	    FILE.sprint("%8.3f", x) +
	    FILE.sprint("%8.3f", y) +
	    FILE.sprint("%8.3f", z);
    }
}
