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
 * Class for least squares fitting of sets of points.
 */
public class Fit {
    /**
     * Fit the pairs of coordinates using Kearsley's quaternion based
     * algorithm. Method returns the RMSD for the fitted point, and
     * the 4x4 transformation matrix that will fit the coordinates
     * together.
     *
     * The RMSD is generated directly from the size of the smallest
     * eigenvector. This is nice as it saves you actually having to
     * apply the transform to all the coordinates to calculate the
     * RMSD.
     *
     * The method will fit xprime to x. The coordinates are left
     * unchanged, the rotation matrix is appropriate for transforming
     * the coordinates xprime onto x.
     */
    public static double fit(double x[], double y[], double z[],
			     double xprime[], double yprime[], double zprime[],
			     int n, Matrix trans){

	if(n == 0){
	    Log.error("no coordinates");
	    return 9999.9;
	}

	trans.setIdentity();

	double xpc = 0.0;
	double ypc = 0.0;
	double zpc = 0.0;
	double xc = 0.0;
	double yc = 0.0;
	double zc = 0.0;

	for(int i = 0; i < n; i++){
	    xpc += xprime[i];
	    ypc += yprime[i];
	    zpc += zprime[i];
	    xc += x[i];
	    yc += y[i];
	    zc += z[i];
	}

	xpc /= (double)n;
	ypc /= (double)n;
	zpc /= (double)n;
	xc /= (double)n;
	yc /= (double)n;
	zc /= (double)n;

	trans.translate(-xpc, -ypc, -zpc);

	// clear the quaternion matrix.
	for(int i = 1; i <= 4; i++){
	    for(int j = 1; j <= 4; j++){
		m[i][j] = 0.0;
	    }
	}

	// build the initial matrix
	for(int i = 0; i < n; i++){
	    // streamline later...
	    double xm = (xprime[i]-xpc) - (x[i]-xc);
	    double ym = (yprime[i]-ypc) - (y[i]-yc);
	    double zm = (zprime[i]-zpc) - (z[i]-zc);
	    double xp = (xprime[i]-xpc) + (x[i]-xc);
	    double yp = (yprime[i]-ypc) + (y[i]-yc);
	    double zp = (zprime[i]-zpc) + (z[i]-zc);

	    m[1][1] += (xm*xm + ym*ym + zm*zm);
	    m[1][2] += (yp*zm - ym*zp);
	    m[1][3] += (xm*zp - xp*zm);
	    m[1][4] += (xp*ym - xm*yp);

	    m[2][2] += (yp*yp + zp*zp + xm*xm);
	    m[2][3] += (xm*ym - xp*yp);
	    m[2][4] += (xm*zm - xp*zp);

	    m[3][3] += (xp*xp + zp*zp + ym*ym);
	    m[3][4] += (ym*zm - yp*zp);

	    m[4][4] += (xp*xp + yp*yp + zm*zm);
	}

	// build the symmetric other half
	for(int i = 1; i <= 4; i++){
	    for(int j = i+1; j <= 4; j++){
		m[j][i] = m[i][j];
	    }
	}

	if(debug){
	    // print out the matrix
	    for(int i = 1; i <= 4; i++){
		for(int j = 1; j <= 4; j++){
		    FILE.out.print("%10.3f", m[i][j]);
		}
		FILE.out.print("\n");
	    }
	}

	// diagonalise the matrix.
	tred2(m, 4, d, e);
	tqli(d, e, 4, m);

	if(debug){
	    // print the eigen values
	    for(int i = 1; i <= 4; i++){
		FILE.out.print("d[%d] = ", i);
		FILE.out.print("%10.3f\n", d[i]);
	    }
	
	    // print the quaternion
	    FILE.out.print("quaternion matrix\n");
	    for(int i = 1; i <= 4; i++){
		for(int j = 1; j <= 4; j++){
		    FILE.out.print("%10.3f", m[i][j]);
		}
		
		FILE.out.print("\n");
	    }
	}

	// find the smallest eigen value
	// they are not sorted by the numerical recipes routines
	int emin = 1;

	for(int i = 2; i <= 4; i++){
	    if(d[i] < d[emin]){
		emin = i;
	    }
	}

	if(debug){
	    FILE.out.print("minimum eigen value %d\n", emin);
	}

	// calculate the norm of the quaternion
	double nq = 0.0;

	for(int j = 1; j <= 4; j++){
	    nq += m[j][emin] * m[j][emin];
	}

	nq = Math.sqrt(nq);

	if(debug){
	    FILE.out.print("quaternion transform\n");
	    for(int j = 1; j <= 4; j++){
		FILE.out.print("%10.3f", m[j][emin]);
	    }
	    FILE.out.print("\n");

	    FILE.out.print("quaternion norm %8.3f\n", nq);
	}

	// if it isn't close to 1.0 print a warning
	if(Math.abs(nq - 1.0) > 1.e-5){
	    Log.warn("quaternion norm is not 1.0... %8.3f", nq);
	}

	rotation.fromQuaternion(m[1][emin], m[2][emin], m[3][emin], m[4][emin]);

	trans.transform(rotation);

	if(debug) trans.print("rotation matrix");

	trans.translate(xc, yc, zc);

	if(debug) trans.print("rotation/translation matrix");

	if(Math.abs(d[emin]) < 1.e-5){
	    return 0.0;
	}else{
	    return Math.sqrt(Math.abs(d[emin])/n);
	}
    }

    /** Private working space for fitting routines. */
    private static final double m[][]    = new double[5][5];
    private static final double d[]      = new double[5];
    private static final double e[]      = new double[5];
    private static final Matrix rotation = new Matrix();

    /** Should we output debugging info. */
    public static boolean debug = false;

    /**
     * Householder reduction of a real,symmetric matrix
     * a[1..n][1..n]. On output, a is replaced by the orthogonal
     * matrix Q e .ecti g the transformation. d[1..n] returns the
     * diagonal elements of the tridiagonal matrix, and e[1..n] the
     * off diagonal elements, with e[1]=0 Several statements,as noted
     * in comments, can be omitted if only eigenvalues are to be
     * found,in which case a contains no useful information on
     * output. Otherwise they are to be included.
     */
    private static void tred2(double a[][], int n, double d[], double e[]) {
	int l,k,j,i;
	double scale,hh,h,g,f;

	for (i=n;i>=2;i--) {
	    l=i-1;
	    h=scale=0.0;
	    if (l > 1) {
		for (k=1;k<=l;k++){
		    scale += Math.abs(a[i][k]);
		}
		if (scale == 0.0){ // Skip transformation.
		    e[i]=a[i][l];
		}else {
		    for (k=1;k<=l;k++) {
			a[i][k] /= scale; // Use scaled a 's for tra sformatio .
			h += a[i][k]*a[i][k]; //Form  in h
		    }
		    f=a[i][l];
		    g=(f >= 0.0 ? -Math.sqrt(h) : Math.sqrt(h));
		    e[i]=scale*g;
		    h -= f*g; // Now h is equation (11.2.4).
		    a[i][l]=f-g; // Store u in the i row of a
				     f=0.0;
		    for (j=1;j<=l;j++) {
			/* Next statement can be omitted if eigenvectors not wanted */
			a[j][i]=a[i][j]/h; // Store u H in i colum of a
			g=0.0; //
			for (k=1;k<=j;k++)
			    g += a[j][k]*a[i][k];
			for (k=j+1;k<=l;k++)
			    g += a[k][j]*a[i][k];
			e[j]=g/h; //Form element of p i temporarily unused
			//element of e
			f += e[j]*a[i][j];
		    }
		    hh=f/(h+h); // Form K ,equation (11.2.11).
		    for (j=1;j<=l;j++) { //Form q and store in e overwriting p
			f=a[i][j];
			e[j]=g=e[j]-hh*f;
			for (k=1;k<=j;k++) //Reduce a equation (11.2.13).
			    a[j][k] -= (f*e[k]+g*a[i][k]);
		    }
		}
	    } else
		e[i]=a[i][l];
	    d[i]=h;
	}
	/* Next statement can be omitted if eigenvectors not wanted */
	d[1]=0.0;
	e[1]=0.0;
	/* Contents of this loop can be omitted if eigenvectors not
	   wanted except for statement d[i]=a[i][i]; */
	for (i=1;i<=n;i++) { //Begin accumulation of transformation matrices.
	    l=i-1;
	    if (d[i] != 0.0) { //This block skipped when i=1
		for (j=1;j<=l;j++) {
		    g=0.0;
		    for (k=1;k<=l;k++)
			g += a[i][k]*a[k][j];
		    for (k=1;k<=l;k++)
			a[k][j] -= g*a[k][i];
		}
	    }
	    d[i]=a[i][i]; // This statement remains.
	    a[i][i]=1.0; //Reset row and colum of a to identity
	    //matrix for next iteration.
	    for (j=1;j<=l;j++) a[j][i]=a[i][j]=0.0;
	}
    }

    private static final double SQR(double a){
	return a*a;
    }

    private static final double SIGN(double a, double b){
	double absa = (a < 0.0 ? -a : a);
	if(b >= 0.0){
	    return absa;
	}else{
	    return -absa;
	}
    }

    /**
     * Computes (a^2 + b^2 )^1/2 without destructive under or overflow.
     */
    private static final double pythag(double a, double b) {
	double absa,absb;
	//absa=Math.abs(a);
	//absb=Math.abs(b);
	absa=(a < 0.0 ? -a : a);
	absb=(b < 0.0 ? -b : b);
	if (absa > absb) return absa*Math.sqrt(1.0+SQR(absb/absa));
	else return (absb == 0.0 ? 0.0 : absb*Math.sqrt(1.0+SQR(absa/absb)));
    }

    /**
     * QL algorithm with implicit shifts, to determine the eigenvalues
     * and eigenvectors of a real, symmetric, tridiagonal matrix, or
     * of a real, symmetric matrix previously reduced by tred2
     * 11.2. On input, d[1..n] contains the diagonal elements of 
     * tridiagonal matrix. On output, it returns the eigenvalues. The
     * vector e[1..n] inputs the subdiagonal elements of the
     * tridiagonal matrix, with e[1] arbitrary. On output e is
     * destroyed. When finding only the eigenvalues, several lines may
     * be omitted, as noted in the comments. If the eigenvectors of a
     * tridiagonal matrix are desired, the matrix z[1..n][1..n] is
     * input as the identity matrix. If the eigenvectors of a matrix
     * that has been reduced by tred2 are required, then z is input as
     * the matrix output by tred2.  In either case, the kth column of z
     * returns the normalized eigenvector corresponding to d[k].
     */
    private static void tqli(double d[], double e[], int n, double z[][]) {
	int m,l,iter,i,k;
	double s,r,p,g,f,dd,c,b;
	for (i=2;i<=n;i++) e[i-1]=e[i]; // Convenient to renumber the elements of e. 
	e[n]=0.0;
	for (l=1;l<=n;l++) {
	    iter=0;
	    do {
		for (m=l;m<=n-1;m++) { //Look for a single small subdiagonal
		    //element to split the matrix.
		    dd=Math.abs(d[m])+Math.abs(d[m+1]);
		    if ((Math.abs(e[m])+dd) == dd) break;
		}
		if (m != l) {
		    if (iter++ == 30){
			System.out.println("Too many iterations in tqli");
			return;
		    }
		    g=(d[l+1]-d[l])/(2.0*e[l]); //Form shift.
		    r=pythag(g,1.0);
		    g=d[m]-d[l]+e[l]/(g+SIGN(r,g)); //This is dm - ks.
		    s=c=1.0;
		    p=0.0;

		    for (i=m-1;i>=l;i--) { //A plane rotation as in the origi-nal
			//QL, followed by Givens
			// rotations to restore tridiag-onal
			//form.
			f=s*e[i];
			b=c*e[i];
			e[i+1]=(r=pythag(f,g));
			if (r == 0.0) { // Recover from underflow.
			    d[i+1] -= p;
			    e[m]=0.0;
			    break;
			}
			s=f/r;
			c=g/r;
			g=d[i+1]-p;
			r=(d[i]-g)*s+2.0*c*b;
			d[i+1]=g+(p=s*r);
			g=c*r-b;
			/* Next loop can be omitted if eigenvectors not wanted*/
			for (k=1;k<=n;k++) { //Form eigenvectors.
			    f=z[k][i+1];
			    z[k][i+1]=s*z[k][i]+c*f;
			    z[k][i]=c*z[k][i]-s*f;
			}
		    }
		    if (r == 0.0 && i >= l) continue;
		    d[l] -= p;
		    e[l]=g;
		    e[m]=0.0;
		}
	    } while (m != l);
	}
    }

    public static void fitMolecules(String molname0, String molname1,
				    String molname2){
	DoubleArray x = new DoubleArray();
	DoubleArray y = new DoubleArray();
	DoubleArray z = new DoubleArray();
	DoubleArray xp = new DoubleArray();
	DoubleArray yp = new DoubleArray();
	DoubleArray zp = new DoubleArray();

	Molecule mol0 = MoleculeIO.read(molname0);
	Molecule mol1 = MoleculeIO.read(molname1);

	if(mol0.getAtomCount() != mol1.getAtomCount()){
	    System.out.println("need same number of atoms in molecules");
	    System.exit(1);
	}

	int n = mol0.getAtomCount();

	Matrix mat = new Matrix();

	for(int a = 0; a < n; a++){
	    Atom atom0 = mol0.getAtom(a);
	    Atom atom1 = mol1.getAtom(a);

	    x.add(atom0.x);
	    y.add(atom0.y);
	    z.add(atom0.z);
	    xp.add(atom1.x);
	    yp.add(atom1.y);
	    zp.add(atom1.z);
	}

	Util.startTimer(0);

	int fitCount = 100000;

	double rmsd = 0.0;

	for(int i = 0; i < fitCount; i++){

	    rmsd = fit(x.getArray(), y.getArray(), z.getArray(),
		       xp.getArray(), yp.getArray(), zp.getArray(),
		       n, mat);
	}

	Util.stopTimer("time for " + fitCount + " fits %5dms\n", 0);

	FILE.out.print("rmsd %5.2f\n", rmsd);

	for(int a = 0; a < n; a++){
	    Atom atom1 = mol1.getAtom(a);
	    mat.transform(atom1);
	}

	FILE output = FILE.write(molname2);

	if(output == null){
	    System.out.println("saveMolecule: couldn't open " + molname2);
	    return;
	}

	MoleculeIO.write(mol1, output);

	output.close();
    }

    /** Test harness. */
    public static void main(String args[]){
	if(args.length == 3){
	    fitMolecules(args[0], args[1], args[2]);
	}else{

	    int n = 6;
	    double xa[] = new double[n];
	    double ya[] = new double[n];
	    double za[] = new double[n];
	    double xb[] = new double[n];
	    double yb[] = new double[n];
	    double zb[] = new double[n];

	    double randomScale = 0.1;
	    
	    // construct benzene like coords
	    for(int i = 0; i < n; i++){
		double theta = i * 2.*Math.PI/ (double)n;
		xa[i] =       1.4 * Math.cos(theta);
		ya[i] =       1.4 * Math.sin(theta);
		za[i] = 5.0;
		xb[i] = 3.0 + 1.4 * Math.cos(theta+1.0) + 2.*(Math.random()-0.5) * randomScale;
		yb[i] = 3.0 +                             2.*(Math.random()-0.5) + randomScale;
		zb[i] = 3.0 + 1.4 * Math.sin(theta+1.0) + 2.*(Math.random()-0.5) * randomScale;
	    }

	    Matrix rot = new Matrix();
	    Point3d p = new Point3d();

	    Util.startTimer(0);
	    
	    int fitCount = 100000;
	    double rmsd = 0.0;
	    
	    for(int f = 0; f < fitCount; f++){
		rmsd = fit(xa, ya, za, xb, yb, zb, n, rot);
	    }

	    Util.stopTimer("time for " + fitCount + " fits %10dms\n", 0);

	    double rmsdCalc = 0.0;

	    for(int i = 0; i < n; i++){
		FILE.out.print(" %8.3f", xa[i]);
		FILE.out.print(" %8.3f", ya[i]);
		FILE.out.print(" %8.3f  ", za[i]);
	    
		p.set(xb[i], yb[i], zb[i]);
	    
		rot.transform(p);
	    
		FILE.out.print(" %8.3f", p.x);
		FILE.out.print(" %8.3f", p.y);
		FILE.out.print(" %8.3f\n", p.z);
		
		double dx = xa[i] - p.x;
		double dy = ya[i] - p.y;
		double dz = za[i] - p.z;
	    
		rmsdCalc += (dx*dx + dy*dy + dz*dz);
	    }
	    
	    rmsdCalc = Math.sqrt(rmsdCalc/n);
	    
	    FILE.out.print("rmsd     = %8.3f\n", rmsd);
	    FILE.out.print("rmsdCalc = %8.3f\n", rmsdCalc);
	}
    }
}
