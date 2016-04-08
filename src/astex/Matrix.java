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
 * 16-11-99 mjh
 *	created
 */

/**
 * A class for storing and manipulating a 4x4 matrix.
 */
public class Matrix {
	/** The elements of the matrix. */
    public double x00, x01, x02, x03;
    public double x10, x11, x12, x13;
    public double x20, x21, x22, x23;
    public double x30, x31, x32, x33;

    /** Default constructor. */
    public Matrix(){
	setIdentity();
    }
    
    /** Default constructor. */
    public Matrix(Matrix in){
	copy(in);
    }
    
    /** Set the matrix to the identity matrix. */
    public void setIdentity(){
	x00 = 1.0; x01 = 0.0; x02 = 0.0; x03 = 0.0;
	x10 = 0.0; x11 = 1.0; x12 = 0.0; x13 = 0.0;
	x20 = 0.0; x21 = 0.0; x22 = 1.0; x23 = 0.0;
	x30 = 0.0; x31 = 0.0; x32 = 0.0; x33 = 1.0;
    }
    
    /** Set the matrix to the zero matrix. */
    public void zero(){
	x00 = 0.0; x01 = 0.0; x02 = 0.0; x03 = 0.0;
	x10 = 0.0; x11 = 0.0; x12 = 0.0; x13 = 0.0;
	x20 = 0.0; x21 = 0.0; x22 = 0.0; x23 = 0.0;
	x30 = 0.0; x31 = 0.0; x32 = 0.0; x33 = 0.0;
    }

    /** Set matrix from another. */
    public void set(Matrix m){
	x00 = m.x00; x01 = m.x01; x02 = m.x02; x03 = m.x03;
	x10 = m.x10; x11 = m.x11; x12 = m.x12; x13 = m.x13;
	x20 = m.x20; x21 = m.x21; x22 = m.x22; x23 = m.x23;
	x30 = m.x30; x31 = m.x31; x32 = m.x32; x33 = m.x33;
    }
    
    public void set(int i, int j, double val){
	if(i < 0 || i > 3 || j < 0 || j > 3){
	    Log.error("trying to set element " + i + "," + j + " to %g", val);
	    return;
	}
	if(i == 0){
	    if(j == 0)      x00 = val;
	    else if(j == 1) x01 = val;
	    else if(j == 2) x02 = val;
	    else if(j == 3) x03 = val;
	}else if(i == 1){
	    if(j == 0)      x10 = val;
	    else if(j == 1) x11 = val;
	    else if(j == 2) x12 = val;
	    else if(j == 3) x13 = val;
	}else if(i == 2){
	    if(j == 0)      x20 = val;
	    else if(j == 1) x21 = val;
	    else if(j == 2) x22 = val;
	    else if(j == 3) x23 = val;
	}else if(i == 3){
	    if(j == 0)      x30 = val;
	    else if(j == 1) x31 = val;
	    else if(j == 2) x32 = val;
	    else if(j == 3) x33 = val;
	}else{
	    Log.error("trying to set row %d", i);
	}
    }

    /** Scale the transformation matrix. */
    public void scale(double s){
	scale(s, s, s);
    }
    
    /** Apply non uniform scale. */
    public void scale(double sx, double sy, double sz){ 
	x00 *= sx; x01 *= sy; x02 *= sz;
	x10 *= sx; x11 *= sy; x12 *= sz;
	x20 *= sx; x21 *= sy; x22 *= sz;
	x30 *= sx; x31 *= sy; x32 *= sz;
    }
    
    /** Translate the transformation matrix. */
    public void translate(double tx, double ty, double tz){
	x00 += x03*tx; x01 += x03*ty; x02 += x03*tz;
	x10 += x13*tx; x11 += x13*ty; x12 += x13*tz;
	x20 += x23*tx; x21 += x23*ty; x22 += x23*tz;
	x30 += x33*tx; x31 += x33*ty; x32 += x33*tz;
    }

    private static Matrix workMatrix = new Matrix();

    /** Translate the transformation matrix the other way. */
    public void pretranslate(double tx, double ty, double tz){
	x30 = tx*x00 + ty*x10 + tz*x20;
	x31 = tx*x01 + ty*x11 + tz*x21;
	x32 = tx*x02 + ty*x12 + tz*x22;
    }
    
    /** Rotate around x in degrees. */
    public void rotateXdegrees(double d){
	double r = d*Math.PI / 180.0;
	double c = Math.cos(r);
	double s = Math.sin(r);

	double t = 0.0;
	t = x01; x01 = t*c - x02*s; x02 = t*s + x02*c; 
	t = x11; x11 = t*c - x12*s; x12 = t*s + x12*c; 
	t = x21; x21 = t*c - x22*s; x22 = t*s + x22*c; 
	t = x31; x31 = t*c - x32*s; x32 = t*s + x32*c; 
    }

    /** Rotate around y in degrees. */
    public void rotateYdegrees(double d){
	double r = d*Math.PI / 180.0;
	double c = Math.cos(r);
	double s = Math.sin(r);

	double t = 0.0;
	t = x00; x00 = t*c + x02*s; x02 = x02*c - t*s;
	t = x10; x10 = t*c + x12*s; x12 = x12*c - t*s;
	t = x20; x20 = t*c + x22*s; x22 = x22*c - t*s;
	t = x30; x30 = t*c + x32*s; x32 = x32*c - t*s;
    }

    /** Rotate around Z in degrees. */
    public void rotateZdegrees(double d){
	double r = d*Math.PI / 180.0;
	double c = Math.cos(r);
	double s = Math.sin(r);

	Matrix m = new Matrix();
	m.rotateAroundVector(0., 0., 1., r);
	transform(m);
	
	// this is wrong...
	//double t = 0.0;
	//t = x00; x00 = t*c + x01*s; x01 = t*s - x01*c;
	//t = x10; x10 = t*c + x11*s; x11 = t*s - x11*c;
	//t = x20; x20 = t*c + x21*s; x21 = t*s - x21*c;
	//t = x30; x30 = t*c + x31*s; x31 = t*s - x31*c;
    }

    /** Transform by another matrix. */
    public void transform(Matrix m){
	double xx00 = x00, xx01 = x01, xx02 = x02, xx03 = x03;
	double xx10 = x10, xx11 = x11, xx12 = x12, xx13 = x13;
	double xx20 = x20, xx21 = x21, xx22 = x22, xx23 = x23;
	double xx30 = x30, xx31 = x31, xx32 = x32, xx33 = x33;
	
	x00 = xx00*m.x00 + xx01*m.x10 + xx02*m.x20 + xx03*m.x30;
	x01 = xx00*m.x01 + xx01*m.x11 + xx02*m.x21 + xx03*m.x31;
	x02 = xx00*m.x02 + xx01*m.x12 + xx02*m.x22 + xx03*m.x32;
	x03 = xx00*m.x03 + xx01*m.x13 + xx02*m.x23 + xx03*m.x33;
	
	x10 = xx10*m.x00 + xx11*m.x10 + xx12*m.x20 + xx13*m.x30;
	x11 = xx10*m.x01 + xx11*m.x11 + xx12*m.x21 + xx13*m.x31;
	x12 = xx10*m.x02 + xx11*m.x12 + xx12*m.x22 + xx13*m.x32;
	x13 = xx10*m.x03 + xx11*m.x13 + xx12*m.x23 + xx13*m.x33;
	
	x20 = xx20*m.x00 + xx21*m.x10 + xx22*m.x20 + xx23*m.x30;
	x21 = xx20*m.x01 + xx21*m.x11 + xx22*m.x21 + xx23*m.x31;
	x22 = xx20*m.x02 + xx21*m.x12 + xx22*m.x22 + xx23*m.x32;
	x23 = xx20*m.x03 + xx21*m.x13 + xx22*m.x23 + xx23*m.x33;
	
	x30 = xx30*m.x00 + xx31*m.x10 + xx32*m.x20 + xx33*m.x30;
	x31 = xx30*m.x01 + xx31*m.x11 + xx32*m.x21 + xx33*m.x31;
	x32 = xx30*m.x02 + xx31*m.x12 + xx32*m.x22 + xx33*m.x32;
	x33 = xx30*m.x03 + xx31*m.x13 + xx32*m.x23 + xx33*m.x33;
    }
    
    /** Transform a point by the current matrix. */
    public void transform(Point3d p){
	double x = p.x, y = p.y, z = p.z;
	p.x = x*x00 + y*x10 + z*x20 + x30;
	p.y = x*x01 + y*x11 + z*x21 + x31;
	p.z = x*x02 + y*x12 + z*x22 + x32;
    }

    /** Transform a point by the inverse matrix (assumes rotation matrix) */
    public void transformByInverse(Point3d p){
	double x = p.x, y = p.y, z = p.z;
	// don't need translation part here.
	p.x = x*x00 + y*x01 + z*x02;
	p.y = x*x10 + y*x11 + z*x12;
	p.z = x*x20 + y*x21 + z*x22;
    }
    
    /** Rotate around a line. */
    public void rotateAroundVector(Point3d p, double theta){
	rotateAroundVector(p.x, p.y, p.z, theta);
    }
    
    /** Rotate around a line. */
    public void rotateAroundVector(double x, double y, double z,
				   double theta){
	double d = x*x + y*y + z*z;

	if(d > 1.e-3){
	    d = Math.sqrt(d);
	    x /= d;
	    y /= d;
	    z /= d;
	}else{
	    System.out.println("rotateAroundVector: direction is zero length");
	    return;
	}

	double s = Math.sin(theta);
	double c = Math.cos(theta);
	double t = 1.0 - c;

	setIdentity();
	
	x00 = t * x * x + c;	/* leading diagonal */
	x11 = t * y * y + c;
	x22 = t * z * z + c;
	
	x10 = t * x * y + s * z;	/* off diagonal elements */
	x20 = t * x * z - s * y;
	
	x01 = t * x * y - s * z;
	x21 = t * y * z + s * x;
	
	x02 = t * x * z + s * y;
	x12 = t * y * z - s * x;
    }
    
    /** A format object for printing matrices. */
    private static Format f6 = new Format("%11.6f");
    
    /** Print a default message with the matrix. */
    public void print(){
	print("-----------------");
    }
    
    /** Print the matrix. */
    public void print(String message){
	System.out.println(message);
	System.out.println("" + f6.format(x00) + " " + f6.format(x01) +
			   " " + f6.format(x02) + " " + f6.format(x03));
	System.out.println("" + f6.format(x10) + " " + f6.format(x11) +
			   " " + f6.format(x12) + " " + f6.format(x13));
	System.out.println("" + f6.format(x20) + " " + f6.format(x21) +
			   " " + f6.format(x22) + " " + f6.format(x23));
	System.out.println("" + f6.format(x30) + " " + f6.format(x31) + 
			   " " + f6.format(x32) + " " + f6.format(x33));
    }

    public String returnScript(){
	String command = "matrix ";
	command += FILE.sprint(" %g", x00) + FILE.sprint(" %g", x01) + FILE.sprint(" %g", x02) + FILE.sprint(" %g", x03);
	command += FILE.sprint(" %g", x10) + FILE.sprint(" %g", x11) + FILE.sprint(" %g", x12) + FILE.sprint(" %g", x13);
	command += FILE.sprint(" %g", x20) + FILE.sprint(" %g", x21) + FILE.sprint(" %g", x22) + FILE.sprint(" %g", x23);
	command += FILE.sprint(" %g", x30) + FILE.sprint(" %g", x31) + FILE.sprint(" %g", x32) + FILE.sprint(" %g", x33);
	command += ";";

	return command;
    }

    /** Small number for matrix equivalence. */
    private static final double TOL = 1.e-5;

    /** Does this matrix equal another matrix. */
    public boolean equals(Matrix m){
	if(Math.abs(x00 - m.x00) > TOL) return false;
	if(Math.abs(x01 - m.x01) > TOL) return false;
	if(Math.abs(x02 - m.x02) > TOL) return false;
	if(Math.abs(x03 - m.x03) > TOL) return false;
	if(Math.abs(x10 - m.x10) > TOL) return false;
	if(Math.abs(x11 - m.x11) > TOL) return false;
	if(Math.abs(x12 - m.x12) > TOL) return false;
	if(Math.abs(x13 - m.x13) > TOL) return false;
	if(Math.abs(x20 - m.x20) > TOL) return false;
	if(Math.abs(x21 - m.x21) > TOL) return false;
	if(Math.abs(x22 - m.x22) > TOL) return false;
	if(Math.abs(x23 - m.x23) > TOL) return false;
	if(Math.abs(x30 - m.x30) > TOL) return false;
	if(Math.abs(x31 - m.x31) > TOL) return false;
	if(Math.abs(x32 - m.x32) > TOL) return false;
	if(Math.abs(x33 - m.x33) > TOL) return false;

	return true;
    }

    /** Does this matrix equal another matrix. */
    public boolean isIdentity(){
	return isIdentity(TOL);
    }

    public boolean isIdentity(double tol){
	if(Math.abs(x00 - 1.0) > tol) return false;
	if(Math.abs(x01)       > tol) return false;
	if(Math.abs(x02)       > tol) return false;
	if(Math.abs(x03)       > tol) return false;
	if(Math.abs(x10)       > tol) return false;
	if(Math.abs(x11 - 1.0) > tol) return false;
	if(Math.abs(x12)       > tol) return false;
	if(Math.abs(x13)       > tol) return false;
	if(Math.abs(x20)       > tol) return false;
	if(Math.abs(x21)       > tol) return false;
	if(Math.abs(x22 - 1.0) > tol) return false;
	if(Math.abs(x23)       > tol) return false;
	if(Math.abs(x30)       > tol) return false;
	if(Math.abs(x31)       > tol) return false;
	if(Math.abs(x32)       > tol) return false;
	if(Math.abs(x33 - 1.0) > tol) return false;

	return true;
    }

    /** Copy m into this matrix. */
    public void copy(Matrix m){
	x00 = m.x00; x01 = m.x01; x02 = m.x02; x03 = m.x03;
	x10 = m.x10; x11 = m.x11; x12 = m.x12; x13 = m.x13;
	x20 = m.x20; x21 = m.x21; x22 = m.x22; x23 = m.x23;
	x30 = m.x30; x31 = m.x31; x32 = m.x32; x33 = m.x33;
    }

    /** Transpose the matrix. */
    public void transpose(){
	double tmp;

	// remember only transpose once
	tmp = x01; x01 = x10; x10 = tmp;
	tmp = x02; x02 = x20; x20 = tmp;
	tmp = x03; x03 = x30; x30 = tmp;

	tmp = x12; x12 = x21; x21 = tmp;
	tmp = x13; x13 = x31; x31 = tmp;

	tmp = x23; x23 = x32; x32 = tmp;
    }

    /*
     * Matrix Inversion
     * by Richard Carling
     * from "Graphics Gems", Academic Press, 1990
     */

    /** A small number. */
    private static final double SMALL_NUMBER = 1.e-8;

    /**
     *   invert( original_matrix, inverse_matrix )
     * 
     *    calculate the inverse of a 4x4 matrix
     *
     *     -1     
     *     A  = ___1__ adjoint A
     *         det A
     */
    public static void invert(Matrix in, Matrix out ){
	/* calculate the adjoint matrix */
	adjoint(in, out);

	/*  calculate the 4x4 determinant
	 *  if the determinant is zero, 
	 *  then the inverse matrix is not unique.
	 */
	double det = det4x4(in);

	if(Math.abs(det) < SMALL_NUMBER){
	    System.err.println("Matrix.invert: Non-singular matrix, " +
			       "no inverse");
	    return;
	}

	/* scale the adjoint matrix to get the inverse */
	out.x00 /= det; out.x01 /= det; out.x02 /= det; out.x03 /= det;
	out.x10 /= det; out.x11 /= det; out.x12 /= det; out.x13 /= det;
	out.x20 /= det; out.x21 /= det; out.x22 /= det; out.x23 /= det;
	out.x30 /= det; out.x31 /= det; out.x32 /= det; out.x33 /= det;
    }

    /**
     *   adjoint( original_matrix, inverse_matrix )
     * 
     *     calculate the adjoint of a 4x4 matrix
     *
     *      Let  a   denote the minor determinant of matrix A obtained by
     *            ij
     *
     *      deleting the ith row and jth column from A.
     *
     *                    i+j
     *     Let  b   = (-1)    a
     *           ij            ji
     *
     *    The matrix B = (b  ) is the adjoint of A
     *                     ij
     */
    public static void adjoint(Matrix in, Matrix out){
	double a1, a2, a3, a4, b1, b2, b3, b4;
	double c1, c2, c3, c4, d1, d2, d3, d4;

	/* assign to individual variable names to aid  */
	/* selecting correct values  */

	a1 = in.x00; b1 = in.x01; 
	c1 = in.x02; d1 = in.x03;

	a2 = in.x10; b2 = in.x11; 
	c2 = in.x12; d2 = in.x13;

	a3 = in.x20; b3 = in.x21;
	c3 = in.x22; d3 = in.x23;

	a4 = in.x30; b4 = in.x31; 
	c4 = in.x32; d4 = in.x33;


	/* row column labeling reversed since we transpose rows & columns */

	out.x00 =   det3x3(b2, b3, b4, c2, c3, c4, d2, d3, d4);
	out.x10 = - det3x3(a2, a3, a4, c2, c3, c4, d2, d3, d4);
	out.x20 =   det3x3(a2, a3, a4, b2, b3, b4, d2, d3, d4);
	out.x30 = - det3x3(a2, a3, a4, b2, b3, b4, c2, c3, c4);
        
	out.x01 = - det3x3(b1, b3, b4, c1, c3, c4, d1, d3, d4);
	out.x11 =   det3x3(a1, a3, a4, c1, c3, c4, d1, d3, d4);
	out.x21 = - det3x3(a1, a3, a4, b1, b3, b4, d1, d3, d4);
	out.x31 =   det3x3(a1, a3, a4, b1, b3, b4, c1, c3, c4);
        
	out.x02 =   det3x3(b1, b2, b4, c1, c2, c4, d1, d2, d4);
	out.x12 = - det3x3(a1, a2, a4, c1, c2, c4, d1, d2, d4);
	out.x22 =   det3x3(a1, a2, a4, b1, b2, b4, d1, d2, d4);
	out.x32 = - det3x3(a1, a2, a4, b1, b2, b4, c1, c2, c4);
        
	out.x03 = - det3x3(b1, b2, b3, c1, c2, c3, d1, d2, d3);
	out.x13 =   det3x3(a1, a2, a3, c1, c2, c3, d1, d2, d3);
	out.x23 = - det3x3(a1, a2, a3, b1, b2, b3, d1, d2, d3);
	out.x33 =   det3x3(a1, a2, a3, b1, b2, b3, c1, c2, c3);
    }

    /**
     * double = det4x4( matrix )
     * 
     * calculate the determinant of a 4x4 matrix.
     */
    private static double det4x4(Matrix m){
	double a1, a2, a3, a4, b1, b2, b3, b4, c1, c2, c3, c4, d1, d2, d3, d4;

	/* assign to individual variable names to aid selecting */
	/*  correct elements */

	a1 = m.x00; b1 = m.x01; 
	c1 = m.x02; d1 = m.x03;

	a2 = m.x10; b2 = m.x11; 
	c2 = m.x12; d2 = m.x13;

	a3 = m.x20; b3 = m.x21; 
	c3 = m.x22; d3 = m.x23;

	a4 = m.x30; b4 = m.x31; 
	c4 = m.x32; d4 = m.x33;

	double ans;

	ans = a1 * det3x3(b2, b3, b4, c2, c3, c4, d2, d3, d4)
	    - b1 * det3x3(a2, a3, a4, c2, c3, c4, d2, d3, d4)
	    + c1 * det3x3(a2, a3, a4, b2, b3, b4, d2, d3, d4)
	    - d1 * det3x3(a2, a3, a4, b2, b3, b4, c2, c3, c4);
	return ans;
    }

    /**
     * double = det3x3(  a1, a2, a3, b1, b2, b3, c1, c2, c3 )
     * 
     * calculate the determinant of a 3x3 matrix
     * in the form
     *
     *     | a1,  b1,  c1 |
     *     | a2,  b2,  c2 |
     *     | a3,  b3,  c3 |
     */
    private static double det3x3(double a1, double a2, double a3,
				 double b1, double b2, double b3,
				 double c1, double c2, double c3){
	double ans;

	ans = a1 * det2x2(b2, b3, c2, c3)
	    - b1 * det2x2(a2, a3, c2, c3)
	    + c1 * det2x2(a2, a3, b2, b3);
	return ans;
    }

    /**
     * double = det2x2( double a, double b, double c, double d )
     * 
     * calculate the determinant of a 2x2 matrix.
     */
    private static double det2x2(double a, double b, double c, double d){
	double ans = a * d - b * c;
	return ans;
    }

    /** Interpolate a new matrix. */
    public static Matrix interpolate(Matrix MS, Matrix MF, double frac){
	Matrix MI = new Matrix();

	interpolate(MS, MF, frac, MI);

	return MI;
    }

    /** Interpolate a new matrix. */
    public static void interpolate(Matrix MS, Matrix MF, double frac, Matrix MI){
	double qS[] = new double[4];
	double qF[] = new double[4];
	double qI[] = new double[4];

	//MS.print("start");

	MS.toQuaternion(qS);
	MF.toQuaternion(qF);

	slerp(qS, qF, frac, qI);

	MI.fromQuaternion(qI);

	//System.out.println("frac " + frac);
	//MI.print("interpolated");
	//MF.print("final");
    }

    /** Convert a matrix to a quaternion. */
    public void toQuaternion(double q[]){
	double trace = x00 + x11 + x22 + 1.0;

	if( trace > 1.e-7 ) {
	    double s = 0.5 / Math.sqrt(trace);
	    q[0] = ( x21 - x12 ) * s;
	    q[1] = ( x02 - x20 ) * s;
	    q[2] = ( x10 - x01 ) * s;
	    q[3] = 0.25 / s;
	} else {
	    if ( x00 > x11 && x00 > x22 ) {
		double s = 2.0 * Math.sqrt( 1.0 + x00 - x11 - x22);
		q[0] = 0.25 * s;
		q[1] = (x01 + x10 ) / s;
		q[2] = (x02 + x20 ) / s;
		q[3] = (x12 - x21 ) / s;
	    } else if (x11 > x22) {
		double s = 2.0 * Math.sqrt( 1.0 + x11 - x00 - x22);
		q[0] = (x01 + x10 ) / s;
		q[1] = 0.25 * s;
		q[2] = (x12 + x21 ) / s;
		q[3] = (x02 - x20 ) / s;    
	    } else {
		double s = 2.0 * Math.sqrt( 1.0 + x22 - x00 - x11 );
		q[0] = (x02 + x20 ) / s;
		q[1] = (x12 + x21 ) / s;
		q[2] = 0.25 * s;
		q[3] = (x01 - x10 ) / s;
	    }
	}

	double len = q[0]*q[0] +q[1]*q[1] +q[2]*q[2] +q[3]*q[3];

	len = Math.sqrt(len);

	for(int i = 0; i < 4; i++){
	    q[i] /= len;
	}
    }

    /** Generate rotation matrix from quaternion. */
    public void fromQuaternion(double q[]){
	//fromQuaternion(q[3], q[0], q[1], q[2]);
	double X = q[0];
	double Y = q[1];
	double Z = q[2];
	double W = q[3];

	double xx = X * X;
	double xy = X * Y;
	double xz = X * Z;
	double xw = X * W;
	double yy = Y * Y;
	double yz = Y * Z;
	double yw = Y * W;
	double zz = Z * Z;
	double zw = Z * W;

	setIdentity();

	x00  = 1 - 2 * ( yy + zz );
	x01  =     2 * ( xy - zw );
	x02  =     2 * ( xz + yw );
	x10  =     2 * ( xy + zw );
	x11  = 1 - 2 * ( xx + zz );
	x12  =     2 * ( yz - xw );
	x20  =     2 * ( xz - yw );
	x21  =     2 * ( yz + xw );
	x22  = 1 - 2 * ( xx + yy );
	//mat[3]  = mat[7] = mat[11] = mat[12] = mat[13] = mat[14] = 0;
	//mat[15] = 1;
	/*
	mat[0]  = 1 - 2 * ( yy + zz );
	mat[1]  =     2 * ( xy - zw );
	mat[2]  =     2 * ( xz + yw );
	mat[4]  =     2 * ( xy + zw );
	mat[5]  = 1 - 2 * ( xx + zz );
	mat[6]  =     2 * ( yz - xw );
	mat[8]  =     2 * ( xz - yw );
	mat[9]  =     2 * ( yz + xw );
	mat[10] = 1 - 2 * ( xx + yy );
	mat[3]  = mat[7] = mat[11] = mat[12] = mat[13] = mat[14] = 0;
	mat[15] = 1;
	*/
    }

    /**
     * Generate rotation matrix from quaternion.
     * Used by the fitting routine
     */
    public void fromQuaternion(double q1, double q2, double q3, double q4){
	x00 = q1*q1 + q2*q2 - q3*q3 - q4*q4;
	x10 = 2. * (q2*q3 - q1*q4);
	x20 = 2. * (q2*q4 + q1*q3);
	x30 = 0.0;

	x01 = 2. * (q2*q3 + q1*q4);
	x11 = q1*q1 - q2*q2 + q3*q3 - q4*q4;
	x21 = 2. * (q3*q4 - q1*q2);
	x31 = 0.0;

	x02 = 2. * (q2*q4 - q1*q3);
	x12 = 2. * (q3*q4 + q1*q2);
	x22 = q1*q1 - q2*q2 - q3*q3 + q4*q4;
	x32 = 0.0;

	x03 = x13 = x23 = 0.0;
	x33 = 1.0;
    }

    /** The famous quaternion slerp. */
    public static void slerp(double Q0[], double Q1[], double T, double Result[]) {
	//double CosTheta = Q0.DotProd(Q1);
	double CosTheta = Q0[3]*Q1[3] - (Q0[0]*Q1[0] + Q0[1]*Q1[1] + Q0[2]*Q1[2]);
	double Theta = Math.acos(CosTheta);
	double SinTheta = Math.sqrt(1.0-CosTheta*CosTheta);
	
        if(Math.abs(SinTheta) < 1.e-5){
            for(int i = 0; i < 4; i++){
                Result[i] = Q0[i];
            }
            return;
        }

	double Sin_T_Theta = Math.sin(T*Theta)/SinTheta;
	double Sin_OneMinusT_Theta = Math.sin((1.0-T)*Theta)/SinTheta;

	//Result = Q0*Sin_OneMinusT_Theta;
	//Result += (Q1*Sin_T_Theta);
	for(int i = 0; i < 4; i++){
	    Result[i] = Q0[i]*Sin_OneMinusT_Theta + Q1[i]*Sin_T_Theta;
	}

	double len =
	    Result[0]*Result[0] +
	    Result[1]*Result[1] +
	    Result[2]*Result[2] +
	    Result[3]*Result[3];
	len = Math.sqrt(len);

	for(int i = 0; i < 4; i++){
	    Result[i] /= len;
	}

    }
}
