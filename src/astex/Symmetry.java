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
 * 15-02-02 mjh
 *	fix space group look up to look at the quoted name
 *	in the space group file, rather than the compact
 *	name that is specified on the same line.
 * 30-11-99 mjh
 *	created
 */
import java.util.*;

/**
 * A class for generating symmetry related copies of molecules.
 */
public class Symmetry {
    /** The unit cell for this symmetry object. */
    public double unitCell[] = new double[6];

    /** The matrix that converts fractional to cartesian coordinates. */
    public Matrix fractionalToCartesian = new Matrix();

    /** The matrix that converts cartesian to fractional coordinates. */
    public Matrix cartesianToFractional = new Matrix();

    /**
     * The SCALE matrix from the PDB file if one exists.
     */
    public Matrix scale = null;

    /** Have we precalculated the f2c and c2f matrices from SCALE. */
    public boolean matricesAssigned = false;

    /** The space group number. */
    private int spaceGroupNumber = 0;

    /** The space group name. */
    private String spaceGroupName = null;

    /** The original space group name. */
    private String originalSpaceGroupName = null;

    /** The list of symmetry operators. */
    private DynamicArray symmetryOperators = null;

    /** The encoding of the unit cell. */
    private int ncode = 1;

    /** The default location of the symmetry library. */
    private static String symmetryLibrary = "symmetry.properties";

    /** Get the symmetry operators for the specified space group number. */
    //public static DynamicArray getSymmetryOperators(int spaceGroupNumber){
    //	return getSymmetryOperators(spaceGroupNumber, null);
    //}

    /** Get the symmetry operators for the specified space group name. */
    //public static DynamicArray getSymmetryOperators(String spaceGroupName){
    //	return getSymmetryOperators(-1, spaceGroupName);
    //}

    /** Get the symmetry operators for the specified space group. */
    //public static DynamicArray getSymmetryOperators(int spaceGroupNumber,
    //						    String spaceGroupName){
    public DynamicArray getSymmetryOperators(){
	if(symmetryOperators != null){
	    return symmetryOperators;
	}

	FILE file = FILE.open(symmetryLibrary);
	String compactedOriginalName = null;

	// compact the original space group name
	if(originalSpaceGroupName != null){
	    compactedOriginalName = "";
	    for(int i = 0; i < originalSpaceGroupName.length(); i++){
		char c = originalSpaceGroupName.charAt(i);
		if(c != ' '){
		    compactedOriginalName += c;
		}
	    }
	}

	//System.out.println("looking for symmetry group |" + compactedOriginalName + "|");

	while(file.nextLine()){
	    String line = file.getCurrentLineAsString();
	    StringTokenizer tokenizer = new StringTokenizer(line);
	    String numberToken = tokenizer.nextToken();
	    String operatorCountToken = tokenizer.nextToken();
	    String thirdToken = tokenizer.nextToken();
	    String shortName = tokenizer.nextToken();
	    String name = getSpaceGroupName(line);

	    int number = FILE.readInteger(numberToken);
	    int operatorCount = FILE.readInteger(operatorCountToken);

	    //System.out.println("checking |" + name + "|");

	    if(number == spaceGroupNumber ||
	       (compactedOriginalName != null && name.equals(compactedOriginalName)) ||
	       (spaceGroupName != null && shortName.equals(spaceGroupName))){

		System.out.println("spacegroup matched symmetry definition");
		System.out.println(line);

		symmetryOperators = new DynamicArray();

		//if(number == spaceGroupNumber ||
		//(spaceGroupName != null && name.equals(spaceGroupName))){
		for(int i = 0; i < operatorCount; i++){
		    file.nextLine();
		    String operatorString = file.getCurrentLineAsString();
		    readSymmetryOperator(operatorString, symmetryOperators);
		    //System.out.println("operatorString " + operatorString);
		}

		break;
	    }else{
		for(int i = 0; i < operatorCount; i++){
		    file.nextLine();
		}
	    }
	}

	file.close();

	return symmetryOperators;
    }

    /**
     * Return the symmetry name from the spage group line.
     *
     * The line is of the form
     * 4 2 2 P21 PG2 MONOCLINIC 'P 1 21 1'
     *
     * Previous versions compressed the name from the pdb file
     * and compared it to P21, but we need to compare it to
     * 'P 1 21 1'. This method returns that compacted name.
     */
    public static String getSpaceGroupName(String spaceGroupDescription){
	int firstApostrophe = spaceGroupDescription.indexOf('\'');
	int lastApostrophe = spaceGroupDescription.lastIndexOf('\'');
	String spaceGroupName = "";
	for(int i = firstApostrophe + 1; i < lastApostrophe; i++){
	    char c = spaceGroupDescription.charAt(i);
	    if(c != ' '){
		spaceGroupName += c;
	    }
	}

	return spaceGroupName;
    }

    /** Decode one symmetry operator. */
    public static void readSymmetryOperator(String line,
					    DynamicArray symmetryOperators){
	StringTokenizer lineTokenizer = new StringTokenizer(line, ",");
	String xToken = lineTokenizer.nextToken().trim();
	String yToken = lineTokenizer.nextToken().trim();
	String zToken = lineTokenizer.nextToken().trim();
	double c[] = new double[4];

	Matrix m = new Matrix();
	m.setIdentity();

	decodeSymmetryToken(xToken, c);
	//m.x00 = c[0]; m.x01 = c[1]; m.x02 = c[2]; m.x03 = c[3];
	m.x00 = c[0]; m.x10 = c[1]; m.x20 = c[2]; m.x30 = c[3];
	decodeSymmetryToken(yToken, c);
	//m.x10 = c[0]; m.x11 = c[1]; m.x12 = c[2]; m.x13 = c[3];
	m.x01 = c[0]; m.x11 = c[1]; m.x21 = c[2]; m.x31 = c[3];
	decodeSymmetryToken(zToken, c);
	//m.x20 = c[0]; m.x21 = c[1]; m.x22 = c[2]; m.x23 = c[3];
	m.x02 = c[0]; m.x12 = c[1]; m.x22 = c[2]; m.x32 = c[3];

	symmetryOperators.add(m);

	//m.print("Symmetry Operator: "+ line);
    }

    /** The positive axes. */
    private static String positiveAxes[] = {"X", "Y", "Z"};

    /** The fractions. */
    private static String fractions[] = {
	"1/2", "1/3", "2/3", "1/4", "3/4", "1/6", "5/6"
    };

    private static double fractionValues[] = {
	1./2., 1./3., 2./3., 1./4., 3./4., 1./6., 5./6.
    };

    /** Decode the symmetry token in the String. */
    public static void decodeSymmetryToken(String token, double components[]){
	//System.out.println("token " + token);
		
	for(int i = 0; i < components.length; i++){
	    components[i] = 0.0;
	}

	for(int i = 0; i < 3; i++){
	    if(token.indexOf("-" + positiveAxes[i]) != -1){
		components[i] = -1.0;
	    }else if(token.indexOf(positiveAxes[i]) != -1){
		components[i] = 1.0;
	    }
	}

	for(int i = 0; i < fractions.length; i++){
	    if(token.indexOf("-" + fractions[i]) != -1){
		components[3] = - fractionValues[i];
		break;
	    }else if(token.indexOf(fractions[i]) != -1){
		components[3] = fractionValues[i];
		break;
	    }
	}
    }

    /** Set the unit cell. */
    public void setUnitCell(double newCell[]){
	for(int i = 0; i < 6; i++){
	    unitCell[i] = newCell[i];
	}

	cartesianToFractional = new Matrix();
	fractionalToCartesian = new Matrix();

	generateMatrices(unitCell,
			 cartesianToFractional,
			 fractionalToCartesian);
    }

    /**
     * Generate the cartesian/fractional interconversion matrices.
     * This is more involved than might appear at first.
     * We need to handle a variety of special cases, like
     * when the unit cell does not follow the standard pdb convention.
     * Also there can be mistakes in the supplied SCALE records
     * which we need to trap.
     */
    public void prepareSymmetry(){
	if(scale == null){
	    // no symmetry or definitely no special cases
	    // matrices should already be defined
	    return;
	}

	Matrix s = scale;

	Matrix c2f = getCartesianToFractionalMatrix();

	//c2f.print("c2f");
	//scale.print("scale");

	if(s.equals(c2f)){
	    // this should be the standard case
	    // no more action to take.
	    //System.out.println("scale and c2f are equal");
	}else{
	    System.err.println("prepareSymmetry: SCALE does not match " +
			       "calculated cartesian->fractional matrix");
	    System.err.println("prepareSymmetry: fixing symmetry");

	    Matrix sinv = new Matrix();

	    Matrix.invert(s, sinv);

	    Matrix tmp = new Matrix(s);
	    tmp.transform(sinv);

	    Matrix f2c = getFractionalToCartesianMatrix();

	    Matrix check = new Matrix(s);
	    check.transform(f2c);

	    check.x30 = 0.0;
	    check.x31 = 0.0;
	    check.x32 = 0.0;

	    Matrix checkt = new Matrix(check);

	    checkt.transpose();

	    check.transform(checkt);

	    // this should really take account of the
	    // unit cell parameters but this value seems
	    // to work ok...

	    //check.print("check");
	    if(check.isIdentity(1.e-2)){
		// reorientation was pure rotation
		c2f.copy(s);
		f2c.copy(sinv);
	    }else{
		System.err.println("prepareSymmetry: inconsistent " +
				   "scale matrix - ignored");

		// remove all trace of the scale matrix as it was bogus
		scale = null;
	    }
	}
    }

    /** Set the space group number. */
    public void setSpaceGroupNumber(int number){
	spaceGroupNumber = number;
	spaceGroupName = null;
    }

    /** Get the space group number. */
    public int getSpaceGroupNumber(){
	return spaceGroupNumber;
    }

    /** Set the space group name. */
    public void setSpaceGroupName(String name){
	spaceGroupName = name;
	spaceGroupNumber = 0;
    }

    /** Get the space group name. */
    public String getSpaceGroupName(){
	return spaceGroupName;
    }

    /** Set the original space group name (with spaces). */
    public void setOriginalSpaceGroupName(String s){
	originalSpaceGroupName = s;
    }

    /** Get the original space group name. */
    public String getOriginalSpaceGroupName(){
	return originalSpaceGroupName;
    }

    /** Set the coding. */
    public void setUnitCellCode(int code){
	ncode = code;
    }

    /** Get the coding. */
    public int getUnitCellCode(){
	return ncode;
    }

    /** Get the symmetry operators. */

    /*    public DynamicArray getSymmetryOperators(){
	if(symmetryOperators == null){
	    if(spaceGroupName != null){
		symmetryOperators = getSymmetryOperators(spaceGroupName);
	    }else{
		symmetryOperators = getSymmetryOperators(spaceGroupNumber);
	    }
	}

	return symmetryOperators;
	}*/

    /** Get the fractionalising matrix. */
    public Matrix getCartesianToFractionalMatrix(){
	return cartesianToFractional;
    }

    /** Get the defractionalising matrix. */
    public Matrix getFractionalToCartesianMatrix(){
	return fractionalToCartesian;
    }

    /** Return the square of the argument. */
    public static double SQ(double x){
	return x*x;
    }

    /** Generate the fractional to cartesian matrices. */
    public static  void generateMatrices(double cell[],
					 Matrix cartesianToFractional,
					 Matrix fractionalToCartesian){
	double cabg[] = new double[3];
	double cabgs[] = new double[3];
	double sabg[] = new double[3];
	double abcs[] = new double[3];
	double sabgs1;
	double volume;

	/* Initialise the transformation matrices. */

	cartesianToFractional.setIdentity();
	fractionalToCartesian.setIdentity();

	for(int i = 0; i < 3; i++){
	    cabg[i]=Math.cos(Math.PI*cell[i+3]/180.0);
	    sabg[i]=Math.sin(Math.PI*cell[i+3]/180.0);
	}

	cabgs[0]=(cabg[1]*cabg[2]-cabg[0])/(sabg[1]*sabg[2]);
	cabgs[1]=(cabg[2]*cabg[0]-cabg[1])/(sabg[2]*sabg[0]);
	cabgs[2]=(cabg[0]*cabg[1]-cabg[2])/(sabg[0]*sabg[1]);
	volume=cell[0]*cell[1]*cell[2]*
	    Math.sqrt(1.0+2.0*cabg[0]*cabg[1]*cabg[2]
		      -SQ(cabg[0])-SQ(cabg[1])-SQ(cabg[2]));
	abcs[0]=cell[1]*cell[2]*sabg[0]/ volume;
	abcs[1]=cell[0]*cell[2]*sabg[1]/ volume;
	abcs[2]=cell[0]*cell[1]*sabg[2]/ volume;
	sabgs1=Math.sqrt(1.0-SQ(cabgs[0]));
		
	/* Cartesian to fractional conversion matrix. */

	cartesianToFractional.x00=1.0/cell[0];
	cartesianToFractional.x10=-cabg[2]/(sabg[2]*cell[0]);
	cartesianToFractional.x20=-(cabg[2]*sabg[1]*cabgs[0]+cabg[1]*sabg[2])/
	    (sabg[1]*sabgs1*sabg[2]*cell[0]);
	cartesianToFractional.x11=1.0/(sabg[2]*cell[1]);
	cartesianToFractional.x21=cabgs[0]/(sabgs1*sabg[2]*cell[1]);
	cartesianToFractional.x22=1.0/(sabg[1]*sabgs1*cell[2]);

	/* Fractional to cartesian matrix. */

	fractionalToCartesian.x00= cell[0];
	fractionalToCartesian.x10= cabg[2]*cell[1];
	fractionalToCartesian.x20= cabg[1]*cell[2];
	fractionalToCartesian.x11= sabg[2]*cell[1];
	fractionalToCartesian.x21=-sabg[1]*cabgs[0]*cell[2];
	fractionalToCartesian.x22=sabg[1]*sabgs1*cell[2];

	//fractionalToCartesian.print("fractionToCartesian matrix");
    }

    /**
     * Transform a point by a crystallographic matrix.
     * This seems to be the transpose of the matrices used for
     * the graphics transformations.
     */
    public static void transformPoint2(Point3d p, Matrix m){
	double xx = p.x, yy = p.y, zz = p.z;
	p.x = xx*m.x00 + yy*m.x01 + zz*m.x02 + m.x03;
	p.y = xx*m.x10 + yy*m.x11 + zz*m.x12 + m.x13;
	p.z = xx*m.x20 + yy*m.x21 + zz*m.x22 + m.x23;
    }

    /** Test method for the symmetry code. */
    public static void main(String args[]){
	//DynamicArray operators = getSymmetryOperators(args[0]);
    }

    public int cnxSpaceGroupNameToNumber(String cnxName){
	int cnxCount = cnxSpaceGroups.length;

	for(int i = 0; i < cnxCount; i++){
	    if(cnxSpaceGroups[i].equals(cnxName)){
		return i;
	    }
	}

	System.out.println("cnxSpaceGroupNameToNumber: " +
			   "couldn't match CNX space group  "+ cnxName);

	return 1;
    }

    /**
     * One time map of CNX names to space group numbers.
     * Index is space group number.
     */
    private static String cnxSpaceGroups[] = {
	"", "P1", "P-1", "P2", "P2(1)", "C2", "PM", "PC", "CM", "CC", "P2/M",
	"P2(1)/M", "C2/M", "P2/C", "P2(1)/C", "C2/C", "P222", "P222(1)",
	"P2(1)2(1)2", "P2(1)2(1)2(1)", "C222(1)", "C222", "F222", "I222",
	"I2(1)2(1)2(1)", "PMM2", "PMC2(1)", "PCC2", "PMA2", "PCA2(1)",
	"PNC2", "PMN2(1)", "PBA2", "PNA2(1)", "PNN2", "CMM2", "CMC2(1)",
	"CCC2", "AMM2", "ABM2", "AMA2", "ABA2", "FMM2", "FDD2", "IMM2",
	"IBA2", "IMA2", "PMMM", "PNNN", "PCCM", "PBAN", "PMMA", "PNNA",
	"PMNA", "PCCA", "PBAM", "PCCN", "PBCM", "PNNM", "PMMN", "PBCN",
	"PBCA", "PNMA", "CMCM", "CMCA", "CMMM", "CCCM", "CMMA", "CCCA",
	"FMMM", "FDDD", "IMMM", "IBAM", "IBCA", "IMMA", "P4", "P4(1)",
	"P4(2)", "P4(3)", "I4", "I4(1)", "P-4", "I-4", "P4/M", "P4(2)/M",
	"P4/N", "P4(2)/N", "I4/M", "I4(1)/A", "P422", "P42(1)2", "P4(1)22",
	"P4(1)2(1)2", "P4(2)22", "P4(2)2(1)2", "P4(3)22", "P4(3)2(1)2",
	"I422", "I4(1)22", "P4MM", "P4BM", "P4(2)CM", "P4(2)NM", "P4CC",
	"P4NC", "P4(2)MC", "P4(2)BC", "I4MM", "I4CM", "I4(1)MD", "I4(1)CD",
	"P-42M", "P-42C", "P-42(1)M", "P-42(1)C", "P-4M2", "P-4C2", "P-4B2",
	"P-4N2", "I-4M2", "I-4C2", "I-42M", "I-42D", "P4/MMM", "P4/MCC",
	"P4/NBM", "P4/NNC", "P4/MBM", "P4/MNC", "P4/NMM", "P4/NCC",
	"P4(2)/MMC", "P4(2)/MCM", "P4(2)/NBC", "P4(2)/NNM", "P4(2)/MBC",
	"P4(2)/MNM", "P4(2)/NMC", "P4(2)/NCM", "I4/MMM", "I4/MCM",
	"I4(1)/AMD", "I4(1)/ACD", "P3", "P3(1)", "P3(2)", "R3", "P-3", "R-3",
	"P312", "P321", "P3(1)12", "P3(1)21", "P3(2)12", "P3(2)21", "R32",
	"P3M1", "P31M", "P3C1", "P31C", "R3M", "R3C", "P-31M", "P-31C",
	"P-3M1", "P-3C1", "R-3M", "R-3C", "P6", "P6(1)", "P6(5)", "P6(2)",
	"P6(4)", "P6(3)", "P-6", "P6/M", "P6(3)/M", "P622", "P6(1)22",
	"P6(5)22", "P6(2)22", "P6(4)22", "P6(3)22", "P6MM", "P6CC",
	"P6(3)CM", "P6(3)MC", "P-6M2", "P-6C2", "P-62M", "P-62C", "P6/MMM",
	"P6/MCC", "P6(3)/MCM", "P6(3)/MMC", "P23", "F23", "I23", "P2(1)3",
	"I2(1)3", "PM-3", "PN-3", "FM-3", "FD-3", "IM-3", "PA-3", "IA-3",
	"P432", "P4(2)32", "F432", "F4(1)32", "I432", "P4(3)32", "P4(1)32",
	"I4(1)32", "P-43M", "F-43M", "I-43M", "P-43N", "F-43C", "I-43D",
	"PM-3M", "PN-3N", "PM-3N", "PN-3M", "FM-3M", "FM-3C", "FD-3M",
	"FD-3C", "IM-3M", "IA-3D",
    };
}
