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

// this file is included into Renderer.j to generate the different
// styles of triangle filling code

#ifdef ZTEST
#undef ZTEST
#endif

#ifdef CLIPPING
#define ZTEST 	(z < frontClip && z >= zb[i])
#else
#define ZTEST 	(z > zb[i])
#endif

#define RND_PIXEL(p) ((p+(1<<(FixedBits-1)))>>FixedBits)

// need to round to nearest pixel
int ymin = RND_PIXEL(vA.y);
int ymax = RND_PIXEL(vC.y);

if (ymax < 0 || ymin >= pixelHeight){
    return;
}

int dy = ymax - ymin;

if (dy <= 0){
    return;
}

/* Local references to the pixel and z buffer. */
int zb[] = zbuffer, px[] = pbuffer;

/* Local references to the diffuse and specular lookup tables. */
int smap[] = highlightMap, dmap[] = diffuseMap, imap[] = intensityMap;

#ifdef TEXTURE
/* The texture map. */
int tmap[] = null;

if(texture != null){
    tmap = texture.pixels;
}
#endif

// local references to the renderer size
int lw = pixelWidth;
int lh = pixelHeight;
/* The color map. */
int cmap[] = colorMap;

// left side and right side
int xL = vA.x;
int zL = vA.z;
int xR = vB.x;
int zR = vB.z;

#if defined(NORMALS) || defined(UNIVERSAL)
int nxL = vA.nx;
int nyL = vA.ny;
int nxR = vB.nx;
int nyR = vB.ny;
#endif
#if defined(TEXTURE) || defined(UNIVERSAL)
int uL = vA.u;
int vL = vA.v;
int uR = vB.u;
int vR = vB.v;
#endif
#if defined(VERTEXCOLOR) || defined(UNIVERSAL)
int rL = vA.r;
int gL = vA.g;
int bL = vA.b;
int rR = vB.r;
int gR = vB.g;
int bR = vB.b;
#endif

int dxL = (vC.x - vA.x) / dy;
int dzL = (vC.z - vA.z) / dy;
int dxR = (vD.x - vB.x) / dy;
int dzR = (vD.z - vB.z) / dy;

#if defined(NORMALS) || defined(UNIVERSAL)
int dnxL = (vC.nx - vA.nx) / dy;
int dnyL = (vC.ny - vA.ny) / dy;
int dnxR = (vD.nx - vB.nx) / dy;
int dnyR = (vD.ny - vB.ny) / dy;
#endif
#if defined(TEXTURE) || defined(UNIVERSAL)
int duL = (vC.u - vA.u) / dy;
int dvL = (vC.v - vA.v) / dy;
int duR = (vD.u - vB.u) / dy;
int dvR = (vD.v - vB.v) / dy;
#endif
#if defined(VERTEXCOLOR) || defined(UNIVERSAL)
int drL = (vC.r - vA.r) / dy;
int dgL = (vC.g - vA.g) / dy;
int dbL = (vC.b - vA.b) / dy;
int drR = (vD.r - vB.r) / dy;
int dgR = (vD.g - vB.g) / dy;
int dbR = (vD.b - vB.b) / dy;
#endif

if (ymin < 0) {
    xL -= dxL * ymin;
    zL -= dzL * ymin;
    xR -= dxR * ymin;
    zR -= dzR * ymin;

#if defined(NORMALS) || defined(UNIVERSAL)
    nxL -= dnxL * ymin;
    nyL -= dnyL * ymin;
    nxR -= dnxR * ymin;
    nyR -= dnyR * ymin;
#endif
#if defined(TEXTURE) || defined(FASTTEXTURE) || defined(UNIVERSAL)
    uL -= duL * ymin;
    vL -= dvL * ymin;
    uR -= duR * ymin;
    vR -= dvR * ymin;
#endif
#if defined(VERTEXCOLOR) || defined(UNIVERSAL)
    rL -= drL * ymin;
    gL -= dgL * ymin;
    bL -= dbL * ymin;
    rR -= drR * ymin;
    gR -= dgR * ymin;
    bR -= dbR * ymin;
#endif
    ymin = 0;
}

if(lh < ymax){
    ymax = lh;
}

for (int y = ymin ; y < ymax ; y++) {
  int dz = 0, dr = 0, dg = 0, db = 0;
  int dnx = 0, dny = 0, du = 0, dv = 0;

    int ixL = RND_PIXEL(xL);
    int ixR = RND_PIXEL(xR);

    int dx = ixR - ixL;
    int z = zL;
#if defined(NORMALS) || defined(UNIVERSAL)
    int nx = nxL;
    int ny = nyL;
#endif
#if defined(TEXTURE) || defined(UNIVERSAL)
    int u = uL;
    int v = vL;
#endif
#if defined(VERTEXCOLOR) || defined(UNIVERSAL)
    int r = rL;
    int g = gL;
    int b = bL;
#endif

    if (dx > 0) {
	dz = (zR - zL) / dx;
#if defined(NORMALS) || defined(UNIVERSAL)
	dnx = (nxR - nxL) / dx;
	dny = (nyR - nyL) / dx;
#endif
#if defined(TEXTURE) || defined(UNIVERSAL)
	du = (uR - uL) / dx;
	dv = (vR - vL) / dx;
#endif
#if defined(VERTEXCOLOR) || defined(UNIVERSAL)
	dr = (rR - rL) / dx;
	dg = (gR - gL) / dx;
	db = (bR - bL) / dx;
#endif
    }

    if (ixL < 0) {
	z -= dz * ixL;
#if defined(NORMALS) || defined(UNIVERSAL)
	nx -= dnx * ixL;
	ny -= dny * ixL;
#endif
#if defined(TEXTURE) || defined(FASTTEXTURE) || defined(UNIVERSAL)
	u -= du * ixL;
	v -= dv * ixL;
#endif
#if defined(VERTEXCOLOR) || defined(UNIVERSAL)
	r -= dr * ixL;
	g -= dg * ixL;
	b -= db * ixL;
#endif
	ixL = 0;
    }

    if(ixR > lw){
	ixR = lw;
    }

    // index of first pixel on scanline
    int i = ixL + lw * y;

    //for (ix = ixL ; ix < ixR ; ix++) {
    for (; ixL < ixR ; ixL++) {
      if(ZTEST){
	int c;

	// here is the shading part of the process
	// calculate normal index into shading lookup tables
	int lookup = LUT(nx, ny);

	// form the base colour depending on the mode
	// we are rendering the object in
#if defined(TEXTURE) || defined(FASTTEXTURE)
	int ucoord = (u>>FixedBits);
	int vcoord = (v>>FixedBits);

	// clamp u into range
	if(ucoord < 0) ucoord = 0;
	else if(ucoord > 255) ucoord = 255;

	// clip v if outside of range 0-255
	if(vcoord < 0 || vcoord >= 256) continue;

	c = tmap[ucoord+(vcoord<<8)];

	// black is regarded as transparent colour in texture maps
	if((c & 0xffffff) == 0) continue;

	c = Color32.multiply(c, dmap[lookup]);
#elif defined(TRIANGLECOLOR)
	c = triangleColor;
	c = Color32.multiply(c, dmap[lookup]);
#elif defined(VERTEXCOLOR)
	c = Color32.pack(r>>FixedBits, g>>FixedBits, b>>FixedBits);
#elif defined(NORMALS)
	c = cmap[lookup];
#else
	//Oops no defintion for those settings!
	c = 0xff00ff;
#endif
	// ok, the pixel survived, store the z-coordinate
	zb[i] = z;

#if defined(TRANSPARENCY)
	// do the transparency
	// according to what is already in the buffer
	c = Color32.blend(c, px[i], transparency);
#endif
	// finally add in specular highlights
	// highlights get done after transparency so that the highlight doesn't fade
	int s = smap[lookup];
	c = Color32.add(c, s);

	// depth cue according to distance in the scene
	int zscale = (z - backClip)/zRange;
	int shade = depthScale[zscale];

	px[i] = Color32.blend(c, background, shade);
      }

      /* Increment z and normals across scan line. */
      z += dz;

#if defined(NORMALS) || defined(UNIVERSAL)
      nx += dnx;
      ny += dny;
#endif
#if defined(TEXTURE) || defined(UNIVERSAL)
      u += du;
      v += dv;
#endif
#if defined(VERTEXCOLOR) || defined(UNIVERSAL)
      r += dr;
      g += dg;
      b += db;
#endif
      i++;
    }

    /* Increment left and right edges and normals down triangle. */
    xL += dxL;
    zL += dzL;

    xR += dxR;
    zR += dzR;

#if defined(NORMALS) || defined(UNIVERSAL)
    nxL += dnxL;
    nyL += dnyL;
    nxR += dnxR;
    nyR += dnyR;
#endif
#if defined(TEXTURE) || defined(UNIVERSAL)
    uL += duL;
    vL += dvL;
    uR += duR;
    vR += dvR;
#endif
#if defined(VERTEXCOLOR) || defined(UNIVERSAL)
    rL += drL;
    gL += dgL;
    bL += dbL;
    rR += drR;
    gR += dgR;
    bR += dbR;
#endif
}
