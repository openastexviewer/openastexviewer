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

int incr1, incr2, d, x, y, xend, yend, xdirflag, ydirflag;
int z, dz;
int pb[] = pbuffer;
int zb[] = zbuffer;

int dx = x2-x1;
if(dx < 0) dx = -dx;
int dy = y2-y1;
if(dy < 0) dy = -dy;
int pixelIndex = 0;

// single point line
if(dy == 0 && dx == 0){
    x = x1;
    y = y1;
    pixelIndex = y * pixelWidth + x;
    z = z1 > z2 ? z1 : z2;
    SET_PIXEL(pixelIndex, z, rgb);
    return;
}

if (dy <= dx) {
    d = 2*dy - dx;
    incr1 = 2*dy;
    incr2 = 2 * (dy - dx);
    if (x1 > x2) {
	x = x2;
	y = y2;
	z = z2;
	dz = (z1 - z2)/dx;
	ydirflag = (-1);
	xend = x1;
    } else {
	x = x1;
	y = y1;
	z = z1;
	dz = (z2 - z1)/dx;
	ydirflag = 1;
	xend = x2;
    }
    
    pixelIndex = y * pixelWidth + x;
    
    SET_PIXEL(pixelIndex, z, rgb);
    
    if (((y2 - y1) * ydirflag) > 0) {
	while (x < xend) {
	    x++;
	    z += dz;
	    if (d <0) {
		d+=incr1;
	    } else {
		y++;
		pixelIndex += pixelWidth;
		d+=incr2;
	    }
	    
	    pixelIndex++;
	    
	    SET_PIXEL(pixelIndex, z, rgb);
	}
    } else {
	while (x < xend) {
	    x++;
	    z += dz;
	    if (d <0) {
		d+=incr1;
	    } else {
		y--;
		pixelIndex -= pixelWidth;
		d+=incr2;
	    }
	    
	    pixelIndex++;
	    
	    SET_PIXEL(pixelIndex, z, rgb);
	}
    }		
} else {
    d = 2*dx - dy;
    incr1 = 2*dx;
    incr2 = 2 * (dx - dy);
    if (y1 > y2) {
	y = y2;
	x = x2;
	z = z2;
	dz = (z1 - z2)/dy;
	yend = y1;
	xdirflag = (-1);
    } else {
	y = y1;
	x = x1;
	z = z1;
	dz = (z2 - z1)/dy;
	yend = y2;
	xdirflag = 1;
    }
    
    pixelIndex = y * pixelWidth + x;
    SET_PIXEL(pixelIndex, z, rgb);
    
    if (((x2 - x1) * xdirflag) > 0) {
	while (y < yend) {
	    y++;
	    z += dz;
	    if (d <0) {
		d+=incr1;
	    } else {
		x++;
		pixelIndex++;
		d+=incr2;
	    }
	    
	    pixelIndex += pixelWidth;
	    SET_PIXEL(pixelIndex, z, rgb);
	}
    } else {
	while (y < yend) {
	    y++;
	    z += dz;
	    
	    if (d <0) {
		d+=incr1;
	    } else {
		x--;
		pixelIndex--;
		d+=incr2;
	    }
	    
	    pixelIndex += pixelWidth;
	    
	    SET_PIXEL(pixelIndex, z, rgb);
	}
    }
}
