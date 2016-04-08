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
 * Various involved geometrical operations for the renderer.
 * The ray cylinder intersection is adapted from WildMagics software renderer
 * http://www.geometrictools.com/
 * Which is available under LGPL license
 */
public class Geometry {
	private static double kU[] = new double[3];
	private static double kV[] = new double[3];
	private static double kW[] = new double[3];
	private static double kD[] = new double[3];
	private static double kDiff[] = new double[3];
	private static double kP[] = new double[3];

	private static double capDir[] = new double[3];
	private static double rayDir[] = new double[3];
	private static double nOrigin[] = new double[3];

	private static double cap0[] = new double[3];
	private static double cap1[] = new double[3];

	private static double fTmpStore[] = new double[2];

	private static double afT[] = new double[3];

	private static double fWLength = 0.0;

	private static double fDLength = 0.0;

	private static double fInvDLength = 0.0;

	private static double capRadius = 0.0;

	private static double fRadiusSqr = 0.0;

	/** Initialise cylinder calculations. */
	public static void rayCapsuleIntInit(double c0[], double c1[],
			double cr, double zrange){
		for(int i = 0; i < 3; i++){
			cap0[i] = c0[i];
			cap1[i] = c1[i];
			kW[i] = capDir[i] = cap1[i] - cap0[i];
		}

		fWLength = normalise(kW);

		generateOrthonormalBasis(kU,kV,kW, true);

		capRadius = cr;

		kD[0] = kU[2] * zrange;
		kD[1] = kV[2] * zrange;
		kD[2] = kW[2] * zrange;

		fDLength = normalise(kD);

		fInvDLength = 1.0/fDLength;

		fRadiusSqr = capRadius * capRadius;
	}

	private static final double fEpsilon = 1.0e-6;
	private static final double f1Epsilon = 1.0 - fEpsilon;

	/** Peform ray-sphere intersection and normal generation. */
	public static int raySphereInt(double ray0[], double ray1[],
			double x, double y, double z, double r,
			double pint[], double nint[], boolean top){

		double r2 = r*r;

		double xr = ray0[0];
		double yr = ray0[1];
		double dx = xr - x;
		double dy = yr - y;
		double d2 = dx*dx + dy*dy;

		if(d2 < r2){
			// we have intersection...

			double h = Math.sqrt(r2 - d2);

			pint[0] = ray0[0];
			pint[1] = ray0[1];
			pint[2] = z + h;

			nint[0] = pint[0] - x;
			nint[1] = pint[1] - y;
			nint[2] = pint[2] - z;

			double r1 = 1./r;

			nint[0] *= r1;
			nint[1] *= r1;
			nint[2] *= r1;

			return 1;
		}

		return 0;
	}

	/** Peform ray-cylinder intersection and normal generation. */
	public static int rayCapsuleInt(double ray0[], double ray1[],
			double pint[], double nint[], boolean top){
		kDiff[0] = ray0[0] - cap0[0];
		kDiff[1] = ray0[1] - cap0[1];
		kDiff[2] = ray0[2] - cap0[2];


		// set up quadratic Q(t) = a*t^2 + 2*b*t + c
		kP[0] = kU[0]*kDiff[0] + kU[1]*kDiff[1] + kU[2]*kDiff[2];
		kP[1] = kV[0]*kDiff[0] + kV[1]*kDiff[1] + kV[2]*kDiff[2];
		kP[2] = kW[0]*kDiff[0] + kW[1]*kDiff[1] + kW[2]*kDiff[2];

		double fInv, fA, fB, fC, fDiscr, fRoot, fT, fTmp;

		int iQuantity = 0;

		// test intersection with infinite cylinder
		fA = kD[0]*kD[0] + kD[1]*kD[1];
		fB = kP[0]*kD[0] + kP[1]*kD[1];
		fC = kP[0]*kP[0] + kP[1]*kP[1] - fRadiusSqr;
		fDiscr = fB*fB - fA*fC;

		if(fDiscr < 0.0){
			// line does not intersect infinite cylinder
			return 0;
		}

		if(fDiscr > 0.0){
			// line intersects infinite cylinder in two places
			fRoot = Math.sqrt(fDiscr);
			//fRoot = Renderer.fastSqrt(fDiscr);
			fInv = 1.0/fA;
			//fT =(-fB - fRoot)*fInv;
			//fTmp = kP[2] + fT*kD[2];
			//if(0.0 <= fTmp && fTmp <= fWLength){
			//    fTmpStore[iQuantity] = fTmp;
			//    afT[iQuantity++] = fT*fInvDLength;
			//}

			fT =(-fB + fRoot)*fInv;
			fTmp = kP[2] + fT*kD[2];
			if(0.0 <= fTmp && fTmp <= fWLength){
				fTmpStore[iQuantity] = fTmp;
				afT[iQuantity++] = fT*fInvDLength;
			}

			//if(iQuantity == 2){
			//System.out.println("return with two cyl intersects");
			// line intersects capsule wall in two places
			//return 2;
			//}
		}

		if(iQuantity != 1){
			// test intersection with bottom hemisphere
			// fA = 1
			fB += kP[2]*kD[2];
			fC += kP[2]*kP[2];
			fDiscr = fB*fB - fC;
			if(fDiscr > 0.0){
				fRoot = Math.sqrt(fDiscr);
				//fRoot = Renderer.fastSqrt(fDiscr);
				//fT = -fB - fRoot;
				//fTmp = kP[2] + fT*kD[2];
				//if(fTmp <= 0.0){
				//	fTmpStore[iQuantity] = 0.0;
				//	afT[iQuantity++] = fT*fInvDLength;
				//if(iQuantity == 2)
				//    return 2;
				//}

				fT = -fB + fRoot;
				fTmp = kP[2] + fT*kD[2];
				if(fTmp <= 0.0){
					fTmpStore[iQuantity] = 0.0;
					afT[iQuantity++] = fT*fInvDLength;
					//if(iQuantity == 2)
					//    return 2;
				}
			}else if(fDiscr == 0.0){
				fT = -fB;
				fTmp = kP[2] + fT*kD[2];
				if(fTmp <= 0.0){
					fTmpStore[iQuantity] = 0.0;
					afT[iQuantity++] = fT*fInvDLength;
					//if(iQuantity == 2)
					//    return 2;
				}
			}

			if(top && iQuantity != 1){
				// test intersection with top hemisphere
				// fA = 1
				fB -= kD[2]*fWLength;
				fC += fWLength*(fWLength - 2.0*kP[2]);

				fDiscr = fB*fB - fC;
				if(fDiscr > 0.0){
					fRoot = Math.sqrt(fDiscr);
					//fRoot = Renderer.fastSqrt(fDiscr);
					//fT = -fB - fRoot;
					//fTmp = kP[2] + fT*kD[2];
					//if(fTmp >= fWLength){
					//	fTmpStore[iQuantity] = fWLength;
					//	afT[iQuantity++] = fT*fInvDLength;
					//if(iQuantity == 2)
					//    return 2;
					//}

					fT = -fB + fRoot;
					fTmp = kP[2] + fT*kD[2];
					if(fTmp >= fWLength){
						fTmpStore[iQuantity] = fWLength;
						afT[iQuantity++] = fT*fInvDLength;
						//if(iQuantity == 2)
						//    return 2;
					}
				}else if(fDiscr == 0.0){
					fT = -fB;
					fTmp = kP[2] + fT*kD[2];
					if(fTmp >= fWLength){
						fTmpStore[iQuantity] = fWLength;
						afT[iQuantity++] = fT*fInvDLength;
						//if(iQuantity == 2)
						//    return 2;
					}
				}
			}
		}

		pint[0] = ray0[0];
		pint[1] = ray0[1];
		pint[2] = ray0[2] + afT[0] * (ray1[2] - ray0[2]);

		for(int j = 0; j < 2; j++){
			nOrigin[j] = cap0[j] + fTmpStore[0] * kW[j];
			nint[j] = (pint[j] - nOrigin[j])/capRadius;
		}

		return iQuantity;
	}

	/** Normalise the vector. */
	public static double normalise(double p[]){
		double len = p[0]*p[0] + p[1]*p[1] + p[2]*p[2];

		if(len != 0.0){
			len = Math.sqrt(len);
			p[0] /= len;
			p[1] /= len;
			p[2] /= len;
		}

		return len;
	}

	/** Generate orthogonal normalised vector set. */
	public static
	void generateOrthonormalBasis(double rkU[], double rkV[],
			double rkW[], boolean bUnitLengthW){
		if(!bUnitLengthW){
			normalise(rkW);
		}

		double fInvLength = 0.0;

		if(Math.abs(rkW[0]) >= Math.abs(rkW[1])){
			// W.x or W.z is the largest magnitude component, swap them
			fInvLength = 1.0/Math.sqrt(rkW[0]*rkW[0]+rkW[2]*rkW[2]);
			rkU[0] = -rkW[2]*fInvLength;
			rkU[1] = 0.0;
			rkU[2] = +rkW[0]*fInvLength;
		}else{
			// W.y or W.z is the largest magnitude component, swap them
			fInvLength = 1.0/Math.sqrt(rkW[1]*rkW[1]+rkW[2]*rkW[2]);
			rkU[0] = 0.0;
			rkU[1] = +rkW[2]*fInvLength;
			rkU[2] = -rkW[1]*fInvLength;
		}

		cross(rkV, rkW, rkU);
		normalise(rkV);
	}

	/** Form cross product of two vectors(a = b x c). */
	public static double cross(double a[], double b[], double c[]){
		a[0] = (b[1] * c[2]) - (b[2] * c[1]);
		a[1] = (b[2] * c[0]) - (b[0] * c[2]);
		a[2] = (b[0] * c[1]) - (b[1] * c[0]);

		return a[0] + a[1] + a[2];
	}

	/** Generate the dot product. */
	public static double dot(double a[], double b[]){
		return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
	}

	/** Length of vector. */
	private static double length(double v[]){
		return Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
	}
}
