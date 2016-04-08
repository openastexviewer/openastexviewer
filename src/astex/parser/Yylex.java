package astex.parser;
import java_cup.runtime.Symbol;
import astex.*;


public class Yylex implements java_cup.runtime.Scanner {
	private final int YY_BUFFER_SIZE = 512;
	private final int YY_F = -1;
	private final int YY_NO_STATE = -1;
	private final int YY_NOT_ACCEPT = 0;
	private final int YY_START = 1;
	private final int YY_END = 2;
	private final int YY_NO_ANCHOR = 4;
	private final int YY_BOL = 128;
	private final int YY_EOF = 129;
	private java.io.BufferedReader yy_reader;
	private int yy_buffer_index;
	private int yy_buffer_read;
	private int yy_buffer_start;
	private int yy_buffer_end;
	private char yy_buffer[];
	private boolean yy_at_bol;
	private int yy_lexical_state;

	public Yylex (java.io.Reader reader) {
		this ();
	}

	public Yylex (java.io.InputStream instream) {
		this ();
	}

	public void setInput(java.io.Reader reader){
		yy_buffer_read = 0;
		yy_buffer_index = 0;
		yy_buffer_start = 0;
		yy_buffer_end = 0;
		yy_at_bol = true;
		yy_lexical_state = YYINITIAL;
		yy_reader = new java.io.BufferedReader(reader);
	}
	private Yylex () {
		yy_buffer = new char[YY_BUFFER_SIZE];
		yy_buffer_read = 0;
		yy_buffer_index = 0;
		yy_buffer_start = 0;
		yy_buffer_end = 0;
		yy_at_bol = true;
		yy_lexical_state = YYINITIAL;
	}

	private boolean yy_eof_done = false;
	private final int YYINITIAL = 0;
	private final int yy_state_dtrans[] = {
		0
	};
	private void yybegin (int state) {
		yy_lexical_state = state;
	}
	private int yy_advance ()
		throws java.io.IOException {
		int next_read;
		int i;
		int j;

		if (yy_buffer_index < yy_buffer_read) {
			return yy_buffer[yy_buffer_index++];
		}

		if (0 != yy_buffer_start) {
			i = yy_buffer_start;
			j = 0;
			while (i < yy_buffer_read) {
				yy_buffer[j] = yy_buffer[i];
				++i;
				++j;
			}
			yy_buffer_end = yy_buffer_end - yy_buffer_start;
			yy_buffer_start = 0;
			yy_buffer_read = j;
			yy_buffer_index = j;
			next_read = yy_reader.read(yy_buffer,
					yy_buffer_read,
					yy_buffer.length - yy_buffer_read);
			if (-1 == next_read) {
				return YY_EOF;
			}
			yy_buffer_read = yy_buffer_read + next_read;
		}

		while (yy_buffer_index >= yy_buffer_read) {
			if (yy_buffer_index >= yy_buffer.length) {
				yy_buffer = yy_double(yy_buffer);
			}
			next_read = yy_reader.read(yy_buffer,
					yy_buffer_read,
					yy_buffer.length - yy_buffer_read);
			if (-1 == next_read) {
				return YY_EOF;
			}
			yy_buffer_read = yy_buffer_read + next_read;
		}
		return yy_buffer[yy_buffer_index++];
	}
	private void yy_move_end () {
		if (yy_buffer_end > yy_buffer_start &&
		    '\n' == yy_buffer[yy_buffer_end-1])
			yy_buffer_end--;
		if (yy_buffer_end > yy_buffer_start &&
		    '\r' == yy_buffer[yy_buffer_end-1])
			yy_buffer_end--;
	}
	private boolean yy_last_was_cr=false;
	private void yy_mark_start () {
		yy_buffer_start = yy_buffer_index;
	}
	private void yy_mark_end () {
		yy_buffer_end = yy_buffer_index;
	}
	private void yy_to_mark () {
		yy_buffer_index = yy_buffer_end;
		yy_at_bol = (yy_buffer_end > yy_buffer_start) &&
		            ('\r' == yy_buffer[yy_buffer_end-1] ||
		             '\n' == yy_buffer[yy_buffer_end-1] ||
		             2028/*LS*/ == yy_buffer[yy_buffer_end-1] ||
		             2029/*PS*/ == yy_buffer[yy_buffer_end-1]);
	}
	private java.lang.String yytext () {
		return (new java.lang.String(yy_buffer,
			yy_buffer_start,
			yy_buffer_end - yy_buffer_start));
	}
	private int yylength () {
		return yy_buffer_end - yy_buffer_start;
	}
	private char[] yy_double (char buf[]) {
		int i;
		char newbuf[];
		newbuf = new char[2*buf.length];
		for (i = 0; i < buf.length; ++i) {
			newbuf[i] = buf[i];
		}
		return newbuf;
	}
	private final int YY_E_INTERNAL = 0;
	private final int YY_E_MATCH = 1;
	private java.lang.String yy_error_string[] = {
		"Error: Internal error.\n",
		"Error: Unmatched input.\n"
	};
	private void yy_error (int code,boolean fatal) {
		java.lang.System.out.print(yy_error_string[code]);
		java.lang.System.out.flush();
		if (fatal) {
			throw new Error("Fatal Error.\n");
		}
	}
	private int[][] unpackFromString(int size1, int size2, String st) {
		int colonIndex = -1;
		String lengthString;
		int sequenceLength = 0;
		int sequenceInteger = 0;

		int commaIndex;
		String workString;

		int res[][] = new int[size1][size2];
		for (int i= 0; i < size1; i++) {
			for (int j= 0; j < size2; j++) {
				if (sequenceLength != 0) {
					res[i][j] = sequenceInteger;
					sequenceLength--;
					continue;
				}
				commaIndex = st.indexOf(',');
				workString = (commaIndex==-1) ? st :
					st.substring(0, commaIndex);
				st = st.substring(commaIndex+1);
				colonIndex = workString.indexOf(':');
				if (colonIndex == -1) {
					res[i][j]=Integer.parseInt(workString);
					continue;
				}
				lengthString =
					workString.substring(colonIndex+1);
				sequenceLength=Integer.parseInt(lengthString);
				workString=workString.substring(0,colonIndex);
				sequenceInteger=Integer.parseInt(workString);
				res[i][j] = sequenceInteger;
				sequenceLength--;
			}
		}
		return res;
	}
	private int yy_acpt[] = {
		/* 0 */ YY_NOT_ACCEPT,
		/* 1 */ YY_NO_ANCHOR,
		/* 2 */ YY_NO_ANCHOR,
		/* 3 */ YY_NO_ANCHOR,
		/* 4 */ YY_NO_ANCHOR,
		/* 5 */ YY_NO_ANCHOR,
		/* 6 */ YY_NO_ANCHOR,
		/* 7 */ YY_NO_ANCHOR,
		/* 8 */ YY_NO_ANCHOR,
		/* 9 */ YY_NO_ANCHOR,
		/* 10 */ YY_NO_ANCHOR,
		/* 11 */ YY_NO_ANCHOR,
		/* 12 */ YY_NO_ANCHOR,
		/* 13 */ YY_NO_ANCHOR,
		/* 14 */ YY_NO_ANCHOR,
		/* 15 */ YY_NO_ANCHOR,
		/* 16 */ YY_NO_ANCHOR,
		/* 17 */ YY_NO_ANCHOR,
		/* 18 */ YY_NO_ANCHOR,
		/* 19 */ YY_NO_ANCHOR,
		/* 20 */ YY_NO_ANCHOR,
		/* 21 */ YY_NO_ANCHOR,
		/* 22 */ YY_NO_ANCHOR,
		/* 23 */ YY_NO_ANCHOR,
		/* 24 */ YY_NO_ANCHOR,
		/* 25 */ YY_NO_ANCHOR,
		/* 26 */ YY_NO_ANCHOR,
		/* 27 */ YY_NO_ANCHOR,
		/* 28 */ YY_NO_ANCHOR,
		/* 29 */ YY_NO_ANCHOR,
		/* 30 */ YY_NO_ANCHOR,
		/* 31 */ YY_NO_ANCHOR,
		/* 32 */ YY_NO_ANCHOR,
		/* 33 */ YY_END,
		/* 34 */ YY_NO_ANCHOR,
		/* 35 */ YY_NO_ANCHOR,
		/* 36 */ YY_NO_ANCHOR,
		/* 37 */ YY_NO_ANCHOR,
		/* 38 */ YY_NO_ANCHOR,
		/* 39 */ YY_NO_ANCHOR,
		/* 40 */ YY_NO_ANCHOR,
		/* 41 */ YY_NO_ANCHOR,
		/* 42 */ YY_NO_ANCHOR,
		/* 43 */ YY_NO_ANCHOR,
		/* 44 */ YY_NO_ANCHOR,
		/* 45 */ YY_NO_ANCHOR,
		/* 46 */ YY_NO_ANCHOR,
		/* 47 */ YY_NO_ANCHOR,
		/* 48 */ YY_NO_ANCHOR,
		/* 49 */ YY_NO_ANCHOR,
		/* 50 */ YY_NO_ANCHOR,
		/* 51 */ YY_NO_ANCHOR,
		/* 52 */ YY_NO_ANCHOR,
		/* 53 */ YY_NO_ANCHOR,
		/* 54 */ YY_NO_ANCHOR,
		/* 55 */ YY_NO_ANCHOR,
		/* 56 */ YY_NO_ANCHOR,
		/* 57 */ YY_NO_ANCHOR,
		/* 58 */ YY_NO_ANCHOR,
		/* 59 */ YY_NO_ANCHOR,
		/* 60 */ YY_NO_ANCHOR,
		/* 61 */ YY_NO_ANCHOR,
		/* 62 */ YY_NO_ANCHOR,
		/* 63 */ YY_NO_ANCHOR,
		/* 64 */ YY_NO_ANCHOR,
		/* 65 */ YY_NO_ANCHOR,
		/* 66 */ YY_NO_ANCHOR,
		/* 67 */ YY_NO_ANCHOR,
		/* 68 */ YY_NO_ANCHOR,
		/* 69 */ YY_NO_ANCHOR,
		/* 70 */ YY_NO_ANCHOR,
		/* 71 */ YY_NO_ANCHOR,
		/* 72 */ YY_NO_ANCHOR,
		/* 73 */ YY_NO_ANCHOR,
		/* 74 */ YY_NO_ANCHOR,
		/* 75 */ YY_NO_ANCHOR,
		/* 76 */ YY_NO_ANCHOR,
		/* 77 */ YY_NO_ANCHOR,
		/* 78 */ YY_NO_ANCHOR,
		/* 79 */ YY_NO_ANCHOR,
		/* 80 */ YY_NO_ANCHOR,
		/* 81 */ YY_NO_ANCHOR,
		/* 82 */ YY_NO_ANCHOR,
		/* 83 */ YY_NO_ANCHOR,
		/* 84 */ YY_NO_ANCHOR,
		/* 85 */ YY_NO_ANCHOR,
		/* 86 */ YY_NO_ANCHOR,
		/* 87 */ YY_NO_ANCHOR,
		/* 88 */ YY_NO_ANCHOR,
		/* 89 */ YY_NO_ANCHOR,
		/* 90 */ YY_NO_ANCHOR,
		/* 91 */ YY_NO_ANCHOR,
		/* 92 */ YY_NO_ANCHOR,
		/* 93 */ YY_NO_ANCHOR,
		/* 94 */ YY_NO_ANCHOR,
		/* 95 */ YY_NO_ANCHOR,
		/* 96 */ YY_NO_ANCHOR,
		/* 97 */ YY_NO_ANCHOR,
		/* 98 */ YY_NO_ANCHOR,
		/* 99 */ YY_NO_ANCHOR,
		/* 100 */ YY_NO_ANCHOR,
		/* 101 */ YY_NO_ANCHOR,
		/* 102 */ YY_NO_ANCHOR,
		/* 103 */ YY_NO_ANCHOR,
		/* 104 */ YY_NO_ANCHOR,
		/* 105 */ YY_NO_ANCHOR,
		/* 106 */ YY_NO_ANCHOR,
		/* 107 */ YY_NO_ANCHOR,
		/* 108 */ YY_NO_ANCHOR,
		/* 109 */ YY_NO_ANCHOR,
		/* 110 */ YY_NO_ANCHOR,
		/* 111 */ YY_NO_ANCHOR,
		/* 112 */ YY_NO_ANCHOR,
		/* 113 */ YY_NO_ANCHOR,
		/* 114 */ YY_NO_ANCHOR,
		/* 115 */ YY_NO_ANCHOR,
		/* 116 */ YY_NO_ANCHOR,
		/* 117 */ YY_NO_ANCHOR,
		/* 118 */ YY_NO_ANCHOR,
		/* 119 */ YY_NO_ANCHOR,
		/* 120 */ YY_NO_ANCHOR,
		/* 121 */ YY_NO_ANCHOR,
		/* 122 */ YY_NO_ANCHOR,
		/* 123 */ YY_NO_ANCHOR,
		/* 124 */ YY_NO_ANCHOR,
		/* 125 */ YY_NO_ANCHOR,
		/* 126 */ YY_NO_ANCHOR,
		/* 127 */ YY_NO_ANCHOR,
		/* 128 */ YY_NO_ANCHOR,
		/* 129 */ YY_NO_ANCHOR,
		/* 130 */ YY_NO_ANCHOR,
		/* 131 */ YY_NO_ANCHOR,
		/* 132 */ YY_NO_ANCHOR,
		/* 133 */ YY_NO_ANCHOR,
		/* 134 */ YY_NO_ANCHOR,
		/* 135 */ YY_NO_ANCHOR,
		/* 136 */ YY_NO_ANCHOR,
		/* 137 */ YY_NO_ANCHOR,
		/* 138 */ YY_NO_ANCHOR,
		/* 139 */ YY_NO_ANCHOR,
		/* 140 */ YY_NO_ANCHOR,
		/* 141 */ YY_NO_ANCHOR,
		/* 142 */ YY_NO_ANCHOR,
		/* 143 */ YY_NO_ANCHOR,
		/* 144 */ YY_NO_ANCHOR,
		/* 145 */ YY_NO_ANCHOR,
		/* 146 */ YY_NO_ANCHOR,
		/* 147 */ YY_NO_ANCHOR,
		/* 148 */ YY_NO_ANCHOR,
		/* 149 */ YY_NO_ANCHOR,
		/* 150 */ YY_NO_ANCHOR,
		/* 151 */ YY_NO_ANCHOR,
		/* 152 */ YY_NO_ANCHOR,
		/* 153 */ YY_NO_ANCHOR,
		/* 154 */ YY_NO_ANCHOR,
		/* 155 */ YY_NO_ANCHOR,
		/* 156 */ YY_NO_ANCHOR,
		/* 157 */ YY_NO_ANCHOR,
		/* 158 */ YY_NO_ANCHOR,
		/* 159 */ YY_NO_ANCHOR,
		/* 160 */ YY_NO_ANCHOR,
		/* 161 */ YY_NO_ANCHOR,
		/* 162 */ YY_NO_ANCHOR,
		/* 163 */ YY_NO_ANCHOR,
		/* 164 */ YY_NO_ANCHOR,
		/* 165 */ YY_NO_ANCHOR,
		/* 166 */ YY_NO_ANCHOR,
		/* 167 */ YY_NO_ANCHOR,
		/* 168 */ YY_NO_ANCHOR,
		/* 169 */ YY_NO_ANCHOR,
		/* 170 */ YY_NOT_ACCEPT,
		/* 171 */ YY_NO_ANCHOR,
		/* 172 */ YY_NO_ANCHOR,
		/* 173 */ YY_NO_ANCHOR,
		/* 174 */ YY_NO_ANCHOR,
		/* 175 */ YY_END,
		/* 176 */ YY_NOT_ACCEPT,
		/* 177 */ YY_NO_ANCHOR,
		/* 178 */ YY_NO_ANCHOR,
		/* 179 */ YY_NO_ANCHOR,
		/* 180 */ YY_NOT_ACCEPT,
		/* 181 */ YY_NO_ANCHOR,
		/* 182 */ YY_NOT_ACCEPT,
		/* 183 */ YY_NO_ANCHOR,
		/* 184 */ YY_NOT_ACCEPT,
		/* 185 */ YY_NO_ANCHOR,
		/* 186 */ YY_NO_ANCHOR,
		/* 187 */ YY_NO_ANCHOR,
		/* 188 */ YY_NO_ANCHOR,
		/* 189 */ YY_NO_ANCHOR,
		/* 190 */ YY_NO_ANCHOR,
		/* 191 */ YY_NO_ANCHOR,
		/* 192 */ YY_NO_ANCHOR,
		/* 193 */ YY_NO_ANCHOR,
		/* 194 */ YY_NO_ANCHOR,
		/* 195 */ YY_NO_ANCHOR,
		/* 196 */ YY_NO_ANCHOR,
		/* 197 */ YY_NO_ANCHOR,
		/* 198 */ YY_NO_ANCHOR,
		/* 199 */ YY_NO_ANCHOR,
		/* 200 */ YY_NO_ANCHOR,
		/* 201 */ YY_NO_ANCHOR,
		/* 202 */ YY_NO_ANCHOR,
		/* 203 */ YY_NO_ANCHOR,
		/* 204 */ YY_NO_ANCHOR,
		/* 205 */ YY_NO_ANCHOR,
		/* 206 */ YY_NO_ANCHOR,
		/* 207 */ YY_NO_ANCHOR,
		/* 208 */ YY_NO_ANCHOR,
		/* 209 */ YY_NO_ANCHOR,
		/* 210 */ YY_NO_ANCHOR,
		/* 211 */ YY_NO_ANCHOR,
		/* 212 */ YY_NO_ANCHOR,
		/* 213 */ YY_NO_ANCHOR,
		/* 214 */ YY_NO_ANCHOR,
		/* 215 */ YY_NO_ANCHOR,
		/* 216 */ YY_NO_ANCHOR,
		/* 217 */ YY_NO_ANCHOR,
		/* 218 */ YY_NO_ANCHOR,
		/* 219 */ YY_NO_ANCHOR,
		/* 220 */ YY_NO_ANCHOR,
		/* 221 */ YY_NO_ANCHOR,
		/* 222 */ YY_NO_ANCHOR,
		/* 223 */ YY_NO_ANCHOR,
		/* 224 */ YY_NO_ANCHOR,
		/* 225 */ YY_NO_ANCHOR,
		/* 226 */ YY_NO_ANCHOR,
		/* 227 */ YY_NO_ANCHOR,
		/* 228 */ YY_NO_ANCHOR,
		/* 229 */ YY_NO_ANCHOR,
		/* 230 */ YY_NO_ANCHOR,
		/* 231 */ YY_NO_ANCHOR,
		/* 232 */ YY_NO_ANCHOR,
		/* 233 */ YY_NO_ANCHOR,
		/* 234 */ YY_NO_ANCHOR,
		/* 235 */ YY_NO_ANCHOR,
		/* 236 */ YY_NO_ANCHOR,
		/* 237 */ YY_NO_ANCHOR,
		/* 238 */ YY_NO_ANCHOR,
		/* 239 */ YY_NO_ANCHOR,
		/* 240 */ YY_NO_ANCHOR,
		/* 241 */ YY_NO_ANCHOR,
		/* 242 */ YY_NO_ANCHOR,
		/* 243 */ YY_NO_ANCHOR,
		/* 244 */ YY_NO_ANCHOR,
		/* 245 */ YY_NO_ANCHOR,
		/* 246 */ YY_NO_ANCHOR,
		/* 247 */ YY_NO_ANCHOR,
		/* 248 */ YY_NO_ANCHOR,
		/* 249 */ YY_NO_ANCHOR,
		/* 250 */ YY_NO_ANCHOR,
		/* 251 */ YY_NO_ANCHOR,
		/* 252 */ YY_NO_ANCHOR,
		/* 253 */ YY_NO_ANCHOR,
		/* 254 */ YY_NO_ANCHOR,
		/* 255 */ YY_NO_ANCHOR,
		/* 256 */ YY_NO_ANCHOR,
		/* 257 */ YY_NO_ANCHOR,
		/* 258 */ YY_NO_ANCHOR,
		/* 259 */ YY_NO_ANCHOR,
		/* 260 */ YY_NO_ANCHOR,
		/* 261 */ YY_NO_ANCHOR,
		/* 262 */ YY_NO_ANCHOR,
		/* 263 */ YY_NO_ANCHOR,
		/* 264 */ YY_NO_ANCHOR,
		/* 265 */ YY_NO_ANCHOR,
		/* 266 */ YY_NO_ANCHOR,
		/* 267 */ YY_NO_ANCHOR,
		/* 268 */ YY_NO_ANCHOR,
		/* 269 */ YY_NO_ANCHOR,
		/* 270 */ YY_NO_ANCHOR,
		/* 271 */ YY_NO_ANCHOR,
		/* 272 */ YY_NO_ANCHOR,
		/* 273 */ YY_NO_ANCHOR,
		/* 274 */ YY_NO_ANCHOR,
		/* 275 */ YY_NO_ANCHOR,
		/* 276 */ YY_NO_ANCHOR,
		/* 277 */ YY_NO_ANCHOR,
		/* 278 */ YY_NO_ANCHOR,
		/* 279 */ YY_NO_ANCHOR,
		/* 280 */ YY_NO_ANCHOR,
		/* 281 */ YY_NO_ANCHOR,
		/* 282 */ YY_NO_ANCHOR,
		/* 283 */ YY_NO_ANCHOR,
		/* 284 */ YY_NO_ANCHOR,
		/* 285 */ YY_NO_ANCHOR,
		/* 286 */ YY_NO_ANCHOR,
		/* 287 */ YY_NO_ANCHOR,
		/* 288 */ YY_NO_ANCHOR,
		/* 289 */ YY_NO_ANCHOR,
		/* 290 */ YY_NO_ANCHOR,
		/* 291 */ YY_NO_ANCHOR,
		/* 292 */ YY_NO_ANCHOR,
		/* 293 */ YY_NO_ANCHOR,
		/* 294 */ YY_NO_ANCHOR,
		/* 295 */ YY_NO_ANCHOR,
		/* 296 */ YY_NO_ANCHOR,
		/* 297 */ YY_NO_ANCHOR,
		/* 298 */ YY_NO_ANCHOR,
		/* 299 */ YY_NO_ANCHOR,
		/* 300 */ YY_NO_ANCHOR,
		/* 301 */ YY_NO_ANCHOR,
		/* 302 */ YY_NO_ANCHOR,
		/* 303 */ YY_NO_ANCHOR,
		/* 304 */ YY_NO_ANCHOR,
		/* 305 */ YY_NO_ANCHOR,
		/* 306 */ YY_NO_ANCHOR,
		/* 307 */ YY_NO_ANCHOR,
		/* 308 */ YY_NO_ANCHOR,
		/* 309 */ YY_NO_ANCHOR,
		/* 310 */ YY_NO_ANCHOR,
		/* 311 */ YY_NO_ANCHOR,
		/* 312 */ YY_NO_ANCHOR,
		/* 313 */ YY_NO_ANCHOR,
		/* 314 */ YY_NO_ANCHOR,
		/* 315 */ YY_NO_ANCHOR,
		/* 316 */ YY_NO_ANCHOR,
		/* 317 */ YY_NO_ANCHOR,
		/* 318 */ YY_NO_ANCHOR,
		/* 319 */ YY_NO_ANCHOR,
		/* 320 */ YY_NO_ANCHOR,
		/* 321 */ YY_NO_ANCHOR,
		/* 322 */ YY_NO_ANCHOR,
		/* 323 */ YY_NO_ANCHOR,
		/* 324 */ YY_NO_ANCHOR,
		/* 325 */ YY_NO_ANCHOR,
		/* 326 */ YY_NO_ANCHOR,
		/* 327 */ YY_NO_ANCHOR,
		/* 328 */ YY_NO_ANCHOR,
		/* 329 */ YY_NO_ANCHOR,
		/* 330 */ YY_NO_ANCHOR,
		/* 331 */ YY_NO_ANCHOR,
		/* 332 */ YY_NO_ANCHOR,
		/* 333 */ YY_NO_ANCHOR,
		/* 334 */ YY_NO_ANCHOR,
		/* 335 */ YY_NO_ANCHOR,
		/* 336 */ YY_NO_ANCHOR,
		/* 337 */ YY_NO_ANCHOR,
		/* 338 */ YY_NO_ANCHOR,
		/* 339 */ YY_NO_ANCHOR,
		/* 340 */ YY_NO_ANCHOR,
		/* 341 */ YY_NO_ANCHOR,
		/* 342 */ YY_NO_ANCHOR,
		/* 343 */ YY_NO_ANCHOR,
		/* 344 */ YY_NO_ANCHOR,
		/* 345 */ YY_NO_ANCHOR,
		/* 346 */ YY_NO_ANCHOR,
		/* 347 */ YY_NO_ANCHOR,
		/* 348 */ YY_NO_ANCHOR,
		/* 349 */ YY_NO_ANCHOR,
		/* 350 */ YY_NO_ANCHOR,
		/* 351 */ YY_NO_ANCHOR,
		/* 352 */ YY_NO_ANCHOR,
		/* 353 */ YY_NO_ANCHOR,
		/* 354 */ YY_NO_ANCHOR,
		/* 355 */ YY_NO_ANCHOR,
		/* 356 */ YY_NO_ANCHOR,
		/* 357 */ YY_NO_ANCHOR,
		/* 358 */ YY_NO_ANCHOR,
		/* 359 */ YY_NO_ANCHOR,
		/* 360 */ YY_NO_ANCHOR,
		/* 361 */ YY_NO_ANCHOR,
		/* 362 */ YY_NO_ANCHOR,
		/* 363 */ YY_NO_ANCHOR,
		/* 364 */ YY_NO_ANCHOR,
		/* 365 */ YY_NO_ANCHOR,
		/* 366 */ YY_NO_ANCHOR,
		/* 367 */ YY_NO_ANCHOR,
		/* 368 */ YY_NO_ANCHOR,
		/* 369 */ YY_NO_ANCHOR,
		/* 370 */ YY_NO_ANCHOR,
		/* 371 */ YY_NO_ANCHOR,
		/* 372 */ YY_NO_ANCHOR,
		/* 373 */ YY_NO_ANCHOR,
		/* 374 */ YY_NO_ANCHOR,
		/* 375 */ YY_NO_ANCHOR,
		/* 376 */ YY_NO_ANCHOR,
		/* 377 */ YY_NO_ANCHOR,
		/* 378 */ YY_NO_ANCHOR,
		/* 379 */ YY_NO_ANCHOR,
		/* 380 */ YY_NO_ANCHOR,
		/* 381 */ YY_NO_ANCHOR,
		/* 382 */ YY_NO_ANCHOR,
		/* 383 */ YY_NO_ANCHOR,
		/* 384 */ YY_NO_ANCHOR,
		/* 385 */ YY_NO_ANCHOR,
		/* 386 */ YY_NO_ANCHOR,
		/* 387 */ YY_NO_ANCHOR,
		/* 388 */ YY_NO_ANCHOR,
		/* 389 */ YY_NO_ANCHOR,
		/* 390 */ YY_NO_ANCHOR,
		/* 391 */ YY_NO_ANCHOR,
		/* 392 */ YY_NO_ANCHOR,
		/* 393 */ YY_NO_ANCHOR,
		/* 394 */ YY_NO_ANCHOR,
		/* 395 */ YY_NO_ANCHOR,
		/* 396 */ YY_NO_ANCHOR,
		/* 397 */ YY_NO_ANCHOR,
		/* 398 */ YY_NO_ANCHOR,
		/* 399 */ YY_NO_ANCHOR,
		/* 400 */ YY_NO_ANCHOR,
		/* 401 */ YY_NO_ANCHOR,
		/* 402 */ YY_NO_ANCHOR,
		/* 403 */ YY_NO_ANCHOR,
		/* 404 */ YY_NO_ANCHOR,
		/* 405 */ YY_NO_ANCHOR,
		/* 406 */ YY_NO_ANCHOR,
		/* 407 */ YY_NO_ANCHOR,
		/* 408 */ YY_NO_ANCHOR,
		/* 409 */ YY_NO_ANCHOR,
		/* 410 */ YY_NO_ANCHOR,
		/* 411 */ YY_NO_ANCHOR,
		/* 412 */ YY_NO_ANCHOR,
		/* 413 */ YY_NO_ANCHOR,
		/* 414 */ YY_NO_ANCHOR,
		/* 415 */ YY_NO_ANCHOR,
		/* 416 */ YY_NO_ANCHOR,
		/* 417 */ YY_NO_ANCHOR,
		/* 418 */ YY_NO_ANCHOR,
		/* 419 */ YY_NO_ANCHOR,
		/* 420 */ YY_NO_ANCHOR,
		/* 421 */ YY_NO_ANCHOR,
		/* 422 */ YY_NO_ANCHOR,
		/* 423 */ YY_NO_ANCHOR,
		/* 424 */ YY_NO_ANCHOR,
		/* 425 */ YY_NO_ANCHOR,
		/* 426 */ YY_NO_ANCHOR,
		/* 427 */ YY_NO_ANCHOR,
		/* 428 */ YY_NO_ANCHOR,
		/* 429 */ YY_NO_ANCHOR,
		/* 430 */ YY_NO_ANCHOR,
		/* 431 */ YY_NO_ANCHOR,
		/* 432 */ YY_NO_ANCHOR,
		/* 433 */ YY_NO_ANCHOR,
		/* 434 */ YY_NO_ANCHOR,
		/* 435 */ YY_NO_ANCHOR,
		/* 436 */ YY_NO_ANCHOR,
		/* 437 */ YY_NO_ANCHOR,
		/* 438 */ YY_NO_ANCHOR,
		/* 439 */ YY_NO_ANCHOR,
		/* 440 */ YY_NO_ANCHOR,
		/* 441 */ YY_NO_ANCHOR,
		/* 442 */ YY_NO_ANCHOR,
		/* 443 */ YY_NO_ANCHOR,
		/* 444 */ YY_NO_ANCHOR,
		/* 445 */ YY_NO_ANCHOR,
		/* 446 */ YY_NO_ANCHOR,
		/* 447 */ YY_NO_ANCHOR,
		/* 448 */ YY_NO_ANCHOR,
		/* 449 */ YY_NO_ANCHOR,
		/* 450 */ YY_NO_ANCHOR,
		/* 451 */ YY_NO_ANCHOR,
		/* 452 */ YY_NO_ANCHOR,
		/* 453 */ YY_NO_ANCHOR,
		/* 454 */ YY_NO_ANCHOR,
		/* 455 */ YY_NO_ANCHOR,
		/* 456 */ YY_NO_ANCHOR,
		/* 457 */ YY_NO_ANCHOR,
		/* 458 */ YY_NO_ANCHOR,
		/* 459 */ YY_NO_ANCHOR,
		/* 460 */ YY_NO_ANCHOR,
		/* 461 */ YY_NO_ANCHOR,
		/* 462 */ YY_NO_ANCHOR,
		/* 463 */ YY_NO_ANCHOR,
		/* 464 */ YY_NO_ANCHOR,
		/* 465 */ YY_NO_ANCHOR,
		/* 466 */ YY_NO_ANCHOR,
		/* 467 */ YY_NO_ANCHOR,
		/* 468 */ YY_NO_ANCHOR,
		/* 469 */ YY_NO_ANCHOR,
		/* 470 */ YY_NO_ANCHOR,
		/* 471 */ YY_NO_ANCHOR,
		/* 472 */ YY_NO_ANCHOR,
		/* 473 */ YY_NO_ANCHOR,
		/* 474 */ YY_NO_ANCHOR,
		/* 475 */ YY_NO_ANCHOR,
		/* 476 */ YY_NO_ANCHOR,
		/* 477 */ YY_NO_ANCHOR,
		/* 478 */ YY_NO_ANCHOR,
		/* 479 */ YY_NO_ANCHOR,
		/* 480 */ YY_NO_ANCHOR,
		/* 481 */ YY_NO_ANCHOR,
		/* 482 */ YY_NO_ANCHOR,
		/* 483 */ YY_NO_ANCHOR,
		/* 484 */ YY_NO_ANCHOR,
		/* 485 */ YY_NO_ANCHOR,
		/* 486 */ YY_NO_ANCHOR,
		/* 487 */ YY_NO_ANCHOR,
		/* 488 */ YY_NO_ANCHOR,
		/* 489 */ YY_NO_ANCHOR,
		/* 490 */ YY_NO_ANCHOR,
		/* 491 */ YY_NO_ANCHOR,
		/* 492 */ YY_NO_ANCHOR,
		/* 493 */ YY_NO_ANCHOR,
		/* 494 */ YY_NO_ANCHOR,
		/* 495 */ YY_NO_ANCHOR,
		/* 496 */ YY_NO_ANCHOR,
		/* 497 */ YY_NO_ANCHOR,
		/* 498 */ YY_NO_ANCHOR,
		/* 499 */ YY_NO_ANCHOR,
		/* 500 */ YY_NO_ANCHOR,
		/* 501 */ YY_NO_ANCHOR,
		/* 502 */ YY_NO_ANCHOR,
		/* 503 */ YY_NO_ANCHOR,
		/* 504 */ YY_NO_ANCHOR,
		/* 505 */ YY_NO_ANCHOR,
		/* 506 */ YY_NO_ANCHOR,
		/* 507 */ YY_NO_ANCHOR,
		/* 508 */ YY_NO_ANCHOR,
		/* 509 */ YY_NO_ANCHOR,
		/* 510 */ YY_NO_ANCHOR,
		/* 511 */ YY_NO_ANCHOR,
		/* 512 */ YY_NO_ANCHOR,
		/* 513 */ YY_NO_ANCHOR,
		/* 514 */ YY_NO_ANCHOR,
		/* 515 */ YY_NO_ANCHOR,
		/* 516 */ YY_NO_ANCHOR,
		/* 517 */ YY_NO_ANCHOR,
		/* 518 */ YY_NO_ANCHOR,
		/* 519 */ YY_NO_ANCHOR,
		/* 520 */ YY_NO_ANCHOR,
		/* 521 */ YY_NO_ANCHOR,
		/* 522 */ YY_NO_ANCHOR,
		/* 523 */ YY_NO_ANCHOR,
		/* 524 */ YY_NO_ANCHOR,
		/* 525 */ YY_NO_ANCHOR,
		/* 526 */ YY_NO_ANCHOR,
		/* 527 */ YY_NO_ANCHOR,
		/* 528 */ YY_NO_ANCHOR,
		/* 529 */ YY_NO_ANCHOR,
		/* 530 */ YY_NO_ANCHOR,
		/* 531 */ YY_NO_ANCHOR,
		/* 532 */ YY_NO_ANCHOR,
		/* 533 */ YY_NO_ANCHOR,
		/* 534 */ YY_NO_ANCHOR,
		/* 535 */ YY_NO_ANCHOR,
		/* 536 */ YY_NO_ANCHOR,
		/* 537 */ YY_NO_ANCHOR,
		/* 538 */ YY_NO_ANCHOR,
		/* 539 */ YY_NO_ANCHOR,
		/* 540 */ YY_NO_ANCHOR,
		/* 541 */ YY_NO_ANCHOR,
		/* 542 */ YY_NO_ANCHOR,
		/* 543 */ YY_NO_ANCHOR,
		/* 544 */ YY_NO_ANCHOR,
		/* 545 */ YY_NO_ANCHOR,
		/* 546 */ YY_NO_ANCHOR,
		/* 547 */ YY_NO_ANCHOR,
		/* 548 */ YY_NO_ANCHOR,
		/* 549 */ YY_NO_ANCHOR,
		/* 550 */ YY_NO_ANCHOR,
		/* 551 */ YY_NO_ANCHOR,
		/* 552 */ YY_NO_ANCHOR,
		/* 553 */ YY_NO_ANCHOR,
		/* 554 */ YY_NO_ANCHOR,
		/* 555 */ YY_NO_ANCHOR,
		/* 556 */ YY_NO_ANCHOR,
		/* 557 */ YY_NO_ANCHOR,
		/* 558 */ YY_NO_ANCHOR,
		/* 559 */ YY_NO_ANCHOR,
		/* 560 */ YY_NO_ANCHOR,
		/* 561 */ YY_NO_ANCHOR,
		/* 562 */ YY_NO_ANCHOR,
		/* 563 */ YY_NO_ANCHOR,
		/* 564 */ YY_NO_ANCHOR,
		/* 565 */ YY_NO_ANCHOR,
		/* 566 */ YY_NO_ANCHOR,
		/* 567 */ YY_NO_ANCHOR,
		/* 568 */ YY_NO_ANCHOR,
		/* 569 */ YY_NO_ANCHOR,
		/* 570 */ YY_NO_ANCHOR,
		/* 571 */ YY_NO_ANCHOR,
		/* 572 */ YY_NO_ANCHOR,
		/* 573 */ YY_NO_ANCHOR,
		/* 574 */ YY_NO_ANCHOR,
		/* 575 */ YY_NO_ANCHOR,
		/* 576 */ YY_NO_ANCHOR,
		/* 577 */ YY_NO_ANCHOR,
		/* 578 */ YY_NO_ANCHOR,
		/* 579 */ YY_NO_ANCHOR,
		/* 580 */ YY_NO_ANCHOR,
		/* 581 */ YY_NO_ANCHOR,
		/* 582 */ YY_NO_ANCHOR,
		/* 583 */ YY_NO_ANCHOR,
		/* 584 */ YY_NO_ANCHOR,
		/* 585 */ YY_NO_ANCHOR,
		/* 586 */ YY_NO_ANCHOR,
		/* 587 */ YY_NO_ANCHOR,
		/* 588 */ YY_NO_ANCHOR,
		/* 589 */ YY_NO_ANCHOR,
		/* 590 */ YY_NO_ANCHOR,
		/* 591 */ YY_NO_ANCHOR,
		/* 592 */ YY_NO_ANCHOR,
		/* 593 */ YY_NO_ANCHOR,
		/* 594 */ YY_NO_ANCHOR,
		/* 595 */ YY_NO_ANCHOR,
		/* 596 */ YY_NO_ANCHOR,
		/* 597 */ YY_NO_ANCHOR,
		/* 598 */ YY_NO_ANCHOR,
		/* 599 */ YY_NO_ANCHOR,
		/* 600 */ YY_NO_ANCHOR,
		/* 601 */ YY_NO_ANCHOR,
		/* 602 */ YY_NO_ANCHOR,
		/* 603 */ YY_NO_ANCHOR,
		/* 604 */ YY_NO_ANCHOR,
		/* 605 */ YY_NO_ANCHOR,
		/* 606 */ YY_NO_ANCHOR,
		/* 607 */ YY_NO_ANCHOR,
		/* 608 */ YY_NO_ANCHOR,
		/* 609 */ YY_NO_ANCHOR,
		/* 610 */ YY_NO_ANCHOR,
		/* 611 */ YY_NO_ANCHOR,
		/* 612 */ YY_NO_ANCHOR,
		/* 613 */ YY_NO_ANCHOR,
		/* 614 */ YY_NO_ANCHOR,
		/* 615 */ YY_NO_ANCHOR,
		/* 616 */ YY_NO_ANCHOR,
		/* 617 */ YY_NO_ANCHOR,
		/* 618 */ YY_NO_ANCHOR,
		/* 619 */ YY_NO_ANCHOR,
		/* 620 */ YY_NO_ANCHOR,
		/* 621 */ YY_NO_ANCHOR,
		/* 622 */ YY_NO_ANCHOR,
		/* 623 */ YY_NO_ANCHOR,
		/* 624 */ YY_NO_ANCHOR,
		/* 625 */ YY_NO_ANCHOR,
		/* 626 */ YY_NO_ANCHOR,
		/* 627 */ YY_NO_ANCHOR,
		/* 628 */ YY_NO_ANCHOR,
		/* 629 */ YY_NO_ANCHOR,
		/* 630 */ YY_NO_ANCHOR,
		/* 631 */ YY_NO_ANCHOR,
		/* 632 */ YY_NO_ANCHOR,
		/* 633 */ YY_NO_ANCHOR,
		/* 634 */ YY_NO_ANCHOR,
		/* 635 */ YY_NO_ANCHOR,
		/* 636 */ YY_NO_ANCHOR,
		/* 637 */ YY_NO_ANCHOR,
		/* 638 */ YY_NO_ANCHOR,
		/* 639 */ YY_NO_ANCHOR,
		/* 640 */ YY_NO_ANCHOR,
		/* 641 */ YY_NO_ANCHOR,
		/* 642 */ YY_NO_ANCHOR,
		/* 643 */ YY_NO_ANCHOR,
		/* 644 */ YY_NO_ANCHOR,
		/* 645 */ YY_NO_ANCHOR,
		/* 646 */ YY_NO_ANCHOR,
		/* 647 */ YY_NO_ANCHOR,
		/* 648 */ YY_NO_ANCHOR,
		/* 649 */ YY_NO_ANCHOR,
		/* 650 */ YY_NO_ANCHOR,
		/* 651 */ YY_NO_ANCHOR,
		/* 652 */ YY_NO_ANCHOR,
		/* 653 */ YY_NO_ANCHOR,
		/* 654 */ YY_NO_ANCHOR,
		/* 655 */ YY_NO_ANCHOR,
		/* 656 */ YY_NO_ANCHOR,
		/* 657 */ YY_NO_ANCHOR,
		/* 658 */ YY_NO_ANCHOR,
		/* 659 */ YY_NO_ANCHOR,
		/* 660 */ YY_NO_ANCHOR,
		/* 661 */ YY_NO_ANCHOR,
		/* 662 */ YY_NO_ANCHOR,
		/* 663 */ YY_NO_ANCHOR,
		/* 664 */ YY_NO_ANCHOR,
		/* 665 */ YY_NO_ANCHOR,
		/* 666 */ YY_NO_ANCHOR,
		/* 667 */ YY_NO_ANCHOR,
		/* 668 */ YY_NO_ANCHOR,
		/* 669 */ YY_NO_ANCHOR,
		/* 670 */ YY_NO_ANCHOR,
		/* 671 */ YY_NO_ANCHOR,
		/* 672 */ YY_NO_ANCHOR,
		/* 673 */ YY_NO_ANCHOR,
		/* 674 */ YY_NO_ANCHOR,
		/* 675 */ YY_NO_ANCHOR,
		/* 676 */ YY_NO_ANCHOR,
		/* 677 */ YY_NO_ANCHOR,
		/* 678 */ YY_NO_ANCHOR,
		/* 679 */ YY_NO_ANCHOR,
		/* 680 */ YY_NO_ANCHOR,
		/* 681 */ YY_NO_ANCHOR,
		/* 682 */ YY_NO_ANCHOR,
		/* 683 */ YY_NO_ANCHOR,
		/* 684 */ YY_NO_ANCHOR,
		/* 685 */ YY_NO_ANCHOR,
		/* 686 */ YY_NO_ANCHOR,
		/* 687 */ YY_NO_ANCHOR,
		/* 688 */ YY_NO_ANCHOR,
		/* 689 */ YY_NO_ANCHOR,
		/* 690 */ YY_NO_ANCHOR,
		/* 691 */ YY_NO_ANCHOR,
		/* 692 */ YY_NO_ANCHOR,
		/* 693 */ YY_NO_ANCHOR,
		/* 694 */ YY_NO_ANCHOR,
		/* 695 */ YY_NO_ANCHOR,
		/* 696 */ YY_NO_ANCHOR,
		/* 697 */ YY_NO_ANCHOR,
		/* 698 */ YY_NO_ANCHOR,
		/* 699 */ YY_NO_ANCHOR,
		/* 700 */ YY_NO_ANCHOR,
		/* 701 */ YY_NO_ANCHOR,
		/* 702 */ YY_NO_ANCHOR,
		/* 703 */ YY_NO_ANCHOR,
		/* 704 */ YY_NO_ANCHOR,
		/* 705 */ YY_NO_ANCHOR,
		/* 706 */ YY_NO_ANCHOR,
		/* 707 */ YY_NO_ANCHOR,
		/* 708 */ YY_NO_ANCHOR,
		/* 709 */ YY_NO_ANCHOR,
		/* 710 */ YY_NO_ANCHOR,
		/* 711 */ YY_NO_ANCHOR,
		/* 712 */ YY_NO_ANCHOR,
		/* 713 */ YY_NO_ANCHOR,
		/* 714 */ YY_NO_ANCHOR,
		/* 715 */ YY_NO_ANCHOR,
		/* 716 */ YY_NO_ANCHOR,
		/* 717 */ YY_NO_ANCHOR,
		/* 718 */ YY_NO_ANCHOR,
		/* 719 */ YY_NO_ANCHOR,
		/* 720 */ YY_NO_ANCHOR,
		/* 721 */ YY_NO_ANCHOR,
		/* 722 */ YY_NO_ANCHOR,
		/* 723 */ YY_NO_ANCHOR,
		/* 724 */ YY_NO_ANCHOR,
		/* 725 */ YY_NO_ANCHOR,
		/* 726 */ YY_NO_ANCHOR,
		/* 727 */ YY_NO_ANCHOR
	};
	private int yy_cmap[] = unpackFromString(1,130,
"42:9,54,50,42,54,46,42:18,54,12,47,48,8,42:2,45,4,5,53,42,49,1,41,52,40:10," +
"3,2,11,10,9,42:2,43:4,44,43:21,42:4,27,42,13,22,23,15,26,32,34,37,24,38,35," +
"29,30,14,16,33,39,17,28,18,31,25,36,19,20,21,6,42,7,42:2,0,51")[0];

	private int yy_rmap[] = unpackFromString(1,728,
"0,1,2,1:7,3,4,5,6,7,8,4,9,10,11,12,13,1,14,4:5,15,4,1:2,16,4:27,17,4:4,18,4" +
":10,1,4,19,4:8,20,4:8,21,4:4,22,4:58,23,4:4,24,4:3,13,25,26,27,28,1,28,29,3" +
"0,1,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54" +
",55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79" +
",80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,10" +
"3,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121,1" +
"22,123,124,125,126,127,128,129,130,131,132,133,134,135,136,137,138,139,140," +
"141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,4,1" +
"59,160,161,162,163,164,165,166,167,168,169,170,171,172,173,174,175,176,177," +
"178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196" +
",197,198,199,200,201,202,203,204,205,206,207,208,209,210,211,212,213,214,21" +
"5,216,217,218,219,220,221,222,223,224,225,226,227,228,229,230,231,232,233,2" +
"34,235,236,237,238,239,240,241,242,243,244,245,246,247,248,249,250,251,252," +
"253,254,255,256,257,258,259,260,261,262,263,264,265,266,267,268,269,270,271" +
",272,273,274,275,276,277,278,279,280,281,282,283,284,285,286,287,288,289,29" +
"0,291,292,293,294,295,296,297,298,299,300,301,302,303,304,305,306,307,308,3" +
"09,310,311,312,313,314,315,316,317,318,319,320,321,322,323,324,325,326,327," +
"328,329,330,331,332,333,334,335,336,337,338,339,340,341,342,343,344,345,346" +
",347,348,349,350,351,352,353,354,355,356,357,358,359,360,361,362,363,364,36" +
"5,366,367,368,369,370,371,372,373,374,375,376,377,378,379,380,381,382,383,3" +
"84,385,386,387,388,389,390,391,392,393,394,395,396,397,398,399,400,401,402," +
"403,404,405,406,407,408,409,410,411,412,413,414,415,416,417,418,419,420,421" +
",422,423,424,425,426,427,428,429,430,431,432,433,434,435,436,437,438,439,44" +
"0,441,442,443,444,445,446,447,448,449,450,451,452,453,454,455,456,457,458,4" +
"59,460,461,462,463,464,465,466,467,468,469,470,471,472,473,474,475,476,477," +
"478,479,480,481,482,483,484,485,486,487,488,489,490,491,492,493,494,495,496" +
",497,498,499,500,501,502,503,504,505,506,507,508,509,510,511,512,513,514,51" +
"5,516,517,518,519,520,521,522,523,524,525,526,527,528,529,530,531,532,533,5" +
"34,535,536,537,538,539,540,4,541,542,543,544,545,546,547,548,549,550,551,55" +
"2,553,554,555,556,557,558,559,560,561,562,563,564,565,566,567,568,569,570,5" +
"71,572,573,574,575,576")[0];

	private int yy_nxt[][] = unpackFromString(577,55,
"1,2,3,4,5,6,7,8,9,10,11,12,13,171,494,575,14,626,177,15,16,17,18,658,181,67" +
"9,19,691,697,701,704,706,708,710,712,691,713,714,691:2,20,493,691:2,493,21," +
"22,174,183,179,22,1,715,691,22,-1:56,172,-1:11,23:13,178,-1,23:12,173,172,-" +
"1,23,178,-1:19,691,24,691:34,-1:3,691,-1:3,691:2,-1:10,691:36,-1:3,691,-1:3" +
",691:2,-1:10,691,25,691:34,-1:3,691,-1:3,691:2,-1:10,691,26,691:34,-1:3,691" +
",-1:3,691:2,-1:10,691:5,27,691:2,28,691:4,196,691:9,197,691:12,-1:3,691,-1:" +
"3,691:2,-1:10,691:8,203,691:27,-1:3,691,-1:3,691:2,-1:10,691:4,204,691:31,-" +
"1:3,691,-1:3,691:2,-1:10,691:4,205,691:2,206,691:3,207,691:24,-1:3,691,-1:3" +
",691:2,-1:2,172,-1:7,691:5,214,497,691:3,215,691:5,581,493,691:2,505,691:10" +
",493:2,691:2,493,-1:3,691,-1:3,691:2,-1:2,172,-1:7,691:17,493,691:13,20,493" +
",691:2,493,-1:3,691,-1:3,691:2,-1:2,170:44,31,170:5,-1,170:3,-1:13,23:28,-1" +
":2,23:2,-1:19,691:8,237,691:16,508,691:10,-1:3,691,-1:3,691:2,-1:51,175,-1:" +
"13,691:18,358,691:17,-1:3,691,-1:3,691:2,-1:10,691:20,544,691:15,-1:3,691,-" +
"1:3,691:2,-1:10,691:18,546,691:17,-1:3,691,-1:3,691:2,-1:10,691:18,382,691:" +
"17,-1:3,691,-1:3,691:2,-1:10,691:19,117,691:16,-1:3,691,-1:3,691:2,-1:10,69" +
"1:17,401,691:18,-1:3,691,-1:3,691:2,-1:10,691:18,482,691:17,-1:3,691,-1:3,6" +
"91:2,-1:10,691:18,485,691:17,-1:3,691,-1:3,691:2,-1:10,691:5,185,691:2,186," +
"495,691:4,187,691:5,188,189,691:2,190,691:11,-1:3,691,-1:3,691:2,-1:2,172,-" +
"1:24,172,-1:13,172:2,-1:2,172,-1:11,172,-1:24,172,-1:13,173,172,-1:2,172,-1" +
":11,176:46,32,176:3,-1,176:3,-1:9,691:7,29,201,691:8,202,691:18,-1:3,691,-1" +
":3,691:2,-1:2,172,-1:11,23:13,178,23:13,178,172,-1,23,178,-1:11,180:45,33,1" +
"80:3,175:2,180:3,-1:9,691:5,211,30,582,691:28,-1:3,691,-1:3,691:2,-1:2,182:" +
"50,-1:2,184,182,-1,180:8,183:36,180,33,180,183,180,175:2,183:2,180,-1,182:5" +
"0,-1,77,184,182,-1:9,691:4,580,691,34,691:8,579,691:9,632,691:10,-1:3,691,-" +
"1:3,691:2,-1:10,691:7,227,691:28,-1:3,691,-1:3,691:2,-1:10,691:9,680,691:26" +
",-1:3,691,-1:3,691:2,-1:10,691:20,35,691:15,-1:3,691,-1:3,691:2,-1:10,691:1" +
"5,630,691:20,-1:3,691,-1:3,691:2,-1:10,691:24,664,691:11,-1:3,691,-1:3,691:" +
"2,-1:10,691:21,229,691:14,-1:3,691,-1:3,691:2,-1:10,691:5,230,691:3,36,691:" +
"26,-1:3,691,-1:3,691:2,-1:10,691:4,37,691:31,-1:3,691,-1:3,691:2,-1:10,691:" +
"19,231,691:16,-1:3,691,-1:3,691:2,-1:10,691:14,662,691:5,681,691:2,232,691:" +
"12,-1:3,691,-1:3,691:2,-1:10,691:29,692,691:6,-1:3,691,-1:3,691:2,-1:10,691" +
":23,38,691:12,-1:3,691,-1:3,691:2,-1:10,691:5,233,234,691:29,-1:3,691,-1:3," +
"691:2,-1:10,691:14,235,691:4,509,691,236,691:2,660,691:11,-1:3,691,-1:3,691" +
":2,-1:10,691:5,39,691:30,-1:3,691,-1:3,691:2,-1:10,691:4,661,691:17,238,691" +
":13,-1:3,691,-1:3,691:2,-1:10,691:10,510,691:25,-1:3,691,-1:3,691:2,-1:10,6" +
"91:4,239,691:31,-1:3,691,-1:3,691:2,-1:10,691:24,40,691:11,-1:3,691,-1:3,69" +
"1:2,-1:10,691:14,240,691:5,663,691:15,-1:3,691,-1:3,691:2,-1:10,691:5,634,6" +
"91:30,-1:3,691,-1:3,691:2,-1:10,691:8,698,691:27,-1:3,691,-1:3,691:2,-1:10," +
"691:5,589,691:14,511,241,691:2,242,691:11,-1:3,691,-1:3,691:2,-1:10,691:20," +
"588,691:15,-1:3,691,-1:3,691:2,-1:10,691:15,243,691,244,691:18,-1:3,691,-1:" +
"3,691:2,-1:10,691:14,247,691,702,691:2,248,691:16,-1:3,691,-1:3,691:2,-1:10" +
",691:27,41,691:8,-1:3,691,-1:3,691:2,-1:10,691:17,250,691:18,-1:3,691,-1:3," +
"691:2,-1:10,691:16,636,691:19,-1:3,691,-1:3,691:2,-1:10,691:14,252,691:21,-" +
"1:3,691,-1:3,691:2,-1:10,691:28,256,691:7,-1:3,691,-1:3,691:2,-1:10,691:9,4" +
"2,691:4,512,691:5,518,691:9,507,691:5,-1:3,691,-1:3,691:2,-1:10,691:12,258," +
"643,691:22,-1:3,691,-1:3,691:2,-1:10,691:5,667,691:18,590,260,691:10,-1:3,6" +
"91,-1:3,691:2,-1:10,691:9,515,691:14,43,691:11,-1:3,691,-1:3,691:2,-1:10,69" +
"1:6,261,691:13,683,691:15,-1:3,691,-1:3,691:2,-1:10,691:6,693,691:29,-1:3,6" +
"91,-1:3,691:2,-1:10,691:24,44,691:11,-1:3,691,-1:3,691:2,-1:10,691:7,592,69" +
"1:7,262,691:20,-1:3,691,-1:3,691:2,-1:10,691:4,639,691:2,587,691:28,-1:3,69" +
"1,-1:3,691:2,-1:2,182:8,226:36,182:3,226,182:2,-1,691,266,182,-1:9,691:22,5" +
"97,691:13,-1:3,691,-1:3,691:2,-1:10,691:21,45,691:14,-1:3,691,-1:3,691:2,-1" +
":10,691:17,46,691:18,-1:3,691,-1:3,691:2,-1:10,691:17,47,691:18,-1:3,691,-1" +
":3,691:2,-1:10,691:9,593,691:14,594,691:11,-1:3,691,-1:3,691:2,-1:10,691:4," +
"521,691:10,668,691:20,-1:3,691,-1:3,691:2,-1:10,691:25,270,691:10,-1:3,691," +
"-1:3,691:2,-1:10,691:15,598,691:20,-1:3,691,-1:3,691:2,-1:10,691:9,640,691:" +
"26,-1:3,691,-1:3,691:2,-1:10,691:7,520,691:28,-1:3,691,-1:3,691:2,-1:10,691" +
":19,522,691:16,-1:3,691,-1:3,691:2,-1:10,691:17,48,691:18,-1:3,691,-1:3,691" +
":2,-1:10,691:11,49,691:24,-1:3,691,-1:3,691:2,-1:10,691:26,274,691:9,-1:3,6" +
"91,-1:3,691:2,-1:10,691:24,279,691:11,-1:3,691,-1:3,691:2,-1:10,691:11,280," +
"691:24,-1:3,691,-1:3,691:2,-1:10,691:24,50,691:11,-1:3,691,-1:3,691:2,-1:10" +
",691:4,282,691:31,-1:3,691,-1:3,691:2,-1:10,691:8,705,691:7,283,691:19,-1:3" +
",691,-1:3,691:2,-1:10,691:8,284,691:6,285,691:20,-1:3,691,-1:3,691:2,-1:10," +
"691:8,707,691:27,-1:3,691,-1:3,691:2,-1:10,691:9,526,691:7,527,691:18,-1:3," +
"691,-1:3,691:2,-1:10,691:19,51,691:16,-1:3,691,-1:3,691:2,-1:10,691:27,52,6" +
"91:8,-1:3,691,-1:3,691:2,-1:10,691:9,53,691:26,-1:3,691,-1:3,691:2,-1:10,69" +
"1:20,645,691:15,-1:3,691,-1:3,691:2,-1:10,691:14,525,691:6,709,691:14,-1:3," +
"691,-1:3,691:2,-1:10,691:15,287,711,691:19,-1:3,691,-1:3,691:2,-1:10,691:14" +
",288,691:21,-1:3,691,-1:3,691:2,-1:10,691:17,289,691:18,-1:3,691,-1:3,691:2" +
",-1:10,691:23,601,691:12,-1:3,691,-1:3,691:2,-1:10,691:11,54,691:24,-1:3,69" +
"1,-1:3,691:2,-1:10,691:6,55,691:29,-1:3,691,-1:3,691:2,-1:10,691:28,297,691" +
":7,-1:3,691,-1:3,691:2,-1:10,691:17,528,691:4,603,691:13,-1:3,691,-1:3,691:" +
"2,-1:10,691:5,303,691:30,-1:3,691,-1:3,691:2,-1:10,691:26,56,691:9,-1:3,691" +
",-1:3,691:2,-1:10,691:28,57,691:7,-1:3,691,-1:3,691:2,-1:10,691:17,58,691:1" +
"8,-1:3,691,-1:3,691:2,-1:2,182:8,226:36,182:3,226,182:2,-1,308,266,182,-1:9" +
",691:17,59,691:18,-1:3,691,-1:3,691:2,-1:10,691:16,310,691:19,-1:3,691,-1:3" +
",691:2,-1:10,691:22,726,691:13,-1:3,691,-1:3,691:2,-1:10,691:17,60,691:18,-" +
"1:3,691,-1:3,691:2,-1:10,691:6,317,691:29,-1:3,691,-1:3,691:2,-1:10,691:15," +
"605,691:20,-1:3,691,-1:3,691:2,-1:10,691:19,531,691:16,-1:3,691,-1:3,691:2," +
"-1:10,691:23,694,691,695,691:10,-1:3,691,-1:3,691:2,-1:10,691:18,700,691:17" +
",-1:3,691,-1:3,691:2,-1:10,691:17,320,321,691:17,-1:3,691,-1:3,691:2,-1:10," +
"691:4,529,691:2,533,691:9,323,691:18,-1:3,691,-1:3,691:2,-1:10,691:8,61,691" +
":13,324,691:13,-1:3,691,-1:3,691:2,-1:10,691:7,325,691:28,-1:3,691,-1:3,691" +
":2,-1:10,691:9,326,691:26,-1:3,691,-1:3,691:2,-1:10,691:8,328,691:8,329,691" +
":18,-1:3,691,-1:3,691:2,-1:10,691:8,62,691:27,-1:3,691,-1:3,691:2,-1:10,691" +
":4,330,691:31,-1:3,691,-1:3,691:2,-1:10,691:25,331,691:10,-1:3,691,-1:3,691" +
":2,-1:10,691:5,63,691:30,-1:3,691,-1:3,691:2,-1:10,691:8,333,691:27,-1:3,69" +
"1,-1:3,691:2,-1:10,691:6,64,691:29,-1:3,691,-1:3,691:2,-1:10,691:26,336,691" +
":9,-1:3,691,-1:3,691:2,-1:10,691:21,610,691:14,-1:3,691,-1:3,691:2,-1:10,69" +
"1:20,337,691:15,-1:3,691,-1:3,691:2,-1:10,691:14,338,691:21,-1:3,691,-1:3,6" +
"91:2,-1:10,691:17,696,691:18,-1:3,691,-1:3,691:2,-1:10,691:17,65,691:18,-1:" +
"3,691,-1:3,691:2,-1:10,691:20,66,691:15,-1:3,691,-1:3,691:2,-1:10,691:19,67" +
",691:7,534,691:8,-1:3,691,-1:3,691:2,-1:10,691:24,341,691:11,-1:3,691,-1:3," +
"691:2,-1:10,691:9,68,691:26,-1:3,691,-1:3,691:2,-1:10,691:10,652,691:3,606," +
"691:21,-1:3,691,-1:3,691:2,-1:10,691:23,607,691:12,-1:3,691,-1:3,691:2,-1:1" +
"0,691:17,69,691:18,-1:3,691,-1:3,691:2,-1:10,691:6,70,691:29,-1:3,691,-1:3," +
"691:2,-1:10,691:28,71,691:7,-1:3,691,-1:3,691:2,-1:10,691:9,72,691:26,-1:3," +
"691,-1:3,691:2,-1:10,691:28,73,691:7,-1:3,691,-1:3,691:2,-1:10,691:24,74,69" +
"1:11,-1:3,691,-1:3,691:2,-1:10,691:17,75,691:18,-1:3,691,-1:3,691:2,-1:10,6" +
"91:6,76,691:29,-1:3,691,-1:3,691:2,-1:10,691:6,78,691:29,-1:3,691,-1:3,691:" +
"2,-1:10,691:17,79,691:18,-1:3,691,-1:3,691:2,-1:10,691:6,80,691:29,-1:3,691" +
",-1:3,691:2,-1:10,691:5,612,691:30,-1:3,691,-1:3,691:2,-1:10,691:17,81,691:" +
"18,-1:3,691,-1:3,691:2,-1:10,691:17,82,691:18,-1:3,691,-1:3,691:2,-1:10,691" +
":9,83,691:26,-1:3,691,-1:3,691:2,-1:10,691:19,84,691:16,-1:3,691,-1:3,691:2" +
",-1:10,691:22,351,691:13,-1:3,691,-1:3,691:2,-1:10,691:17,85,691:18,-1:3,69" +
"1,-1:3,691:2,-1:10,691:17,86,691:18,-1:3,691,-1:3,691:2,-1:10,691:6,87,691:" +
"29,-1:3,691,-1:3,691:2,-1:10,691:8,727,691:18,722,691:8,-1:3,691,-1:3,691:2" +
",-1:10,691:15,608,691:20,-1:3,691,-1:3,691:2,-1:10,691:10,357,691:25,-1:3,6" +
"91,-1:3,691:2,-1:10,691:8,88,691:27,-1:3,691,-1:3,691:2,-1:10,691:19,359,69" +
"1:16,-1:3,691,-1:3,691:2,-1:10,691:7,89,691:28,-1:3,691,-1:3,691:2,-1:10,69" +
"1:6,360,691:29,-1:3,691,-1:3,691:2,-1:10,691:17,90,691:18,-1:3,691,-1:3,691" +
":2,-1:10,691:8,91,691:27,-1:3,691,-1:3,691:2,-1:10,691:9,362,691:26,-1:3,69" +
"1,-1:3,691:2,-1:10,691:17,92,691:18,-1:3,691,-1:3,691:2,-1:10,691:4,363,691" +
":31,-1:3,691,-1:3,691:2,-1:10,691:9,93,691:26,-1:3,691,-1:3,691:2,-1:10,691" +
":7,365,691:28,-1:3,691,-1:3,691:2,-1:10,691:8,368,691:27,-1:3,691,-1:3,691:" +
"2,-1:10,691:18,371,94,691:16,-1:3,691,-1:3,691:2,-1:10,691:17,95,691:18,-1:" +
"3,691,-1:3,691:2,-1:10,691:9,96,691:26,-1:3,691,-1:3,691:2,-1:10,691:14,372" +
",691:21,-1:3,691,-1:3,691:2,-1:10,691:17,97,691:18,-1:3,691,-1:3,691:2,-1:1" +
"0,691:28,613,691:7,-1:3,691,-1:3,691:2,-1:10,691:10,98,691:25,-1:3,691,-1:3" +
",691:2,-1:10,691:20,654,691:15,-1:3,691,-1:3,691:2,-1:10,691:7,99,691:28,-1" +
":3,691,-1:3,691:2,-1:10,691:17,100,691:18,-1:3,691,-1:3,691:2,-1:10,691:23," +
"676,691:12,-1:3,691,-1:3,691:2,-1:10,691:17,101,691:18,-1:3,691,-1:3,691:2," +
"-1:10,691:11,102,691:24,-1:3,691,-1:3,691:2,-1:10,691:9,103,691:26,-1:3,691" +
",-1:3,691:2,-1:10,691:25,614,691:10,-1:3,691,-1:3,691:2,-1:10,691:17,104,69" +
"1:18,-1:3,691,-1:3,691:2,-1:10,691:9,105,691:26,-1:3,691,-1:3,691:2,-1:10,6" +
"91:5,106,691:30,-1:3,691,-1:3,691:2,-1:10,691:17,107,691:18,-1:3,691,-1:3,6" +
"91:2,-1:10,691:9,108,691:26,-1:3,691,-1:3,691:2,-1:10,691:8,109,691:27,-1:3" +
",691,-1:3,691:2,-1:10,691:9,110,691:26,-1:3,691,-1:3,691:2,-1:10,691:13,381" +
",691:22,-1:3,691,-1:3,691:2,-1:10,691:15,656,691:20,-1:3,691,-1:3,691:2,-1:" +
"10,691:17,553,691:18,-1:3,691,-1:3,691:2,-1:10,691:9,111,691:26,-1:3,691,-1" +
":3,691:2,-1:10,691:22,617,691:13,-1:3,691,-1:3,691:2,-1:10,691:19,383,691:1" +
"6,-1:3,691,-1:3,691:2,-1:10,691:20,112,691:15,-1:3,691,-1:3,691:2,-1:10,691" +
":5,384,691:30,-1:3,691,-1:3,691:2,-1:10,691:17,113,691:18,-1:3,691,-1:3,691" +
":2,-1:10,691:9,385,691:26,-1:3,691,-1:3,691:2,-1:10,691:7,386,691:28,-1:3,6" +
"91,-1:3,691:2,-1:10,691:9,114,691:26,-1:3,691,-1:3,691:2,-1:10,691:9,115,69" +
"1:26,-1:3,691,-1:3,691:2,-1:10,691:8,552,691:5,387,691:12,388,691:8,-1:3,69" +
"1,-1:3,691:2,-1:10,691:17,116,691:18,-1:3,691,-1:3,691:2,-1:10,691:6,392,69" +
"1:29,-1:3,691,-1:3,691:2,-1:10,691:14,394,691:21,-1:3,691,-1:3,691:2,-1:10," +
"691:20,395,691:15,-1:3,691,-1:3,691:2,-1:10,691:4,399,691:31,-1:3,691,-1:3," +
"691:2,-1:10,691:17,118,691:18,-1:3,691,-1:3,691:2,-1:10,691:17,119,691:18,-" +
"1:3,691,-1:3,691:2,-1:10,691:8,557,691:27,-1:3,691,-1:3,691:2,-1:10,691:17," +
"120,691:18,-1:3,691,-1:3,691:2,-1:10,691:11,404,691:24,-1:3,691,-1:3,691:2," +
"-1:10,691:13,405,691:22,-1:3,691,-1:3,691:2,-1:10,691:17,121,691:18,-1:3,69" +
"1,-1:3,691:2,-1:10,691:21,620,691:14,-1:3,691,-1:3,691:2,-1:10,691:17,122,6" +
"91:18,-1:3,691,-1:3,691:2,-1:10,691:19,619,691:16,-1:3,691,-1:3,691:2,-1:10" +
",691:7,559,691:28,-1:3,691,-1:3,691:2,-1:10,691:15,560,691:20,-1:3,691,-1:3" +
",691:2,-1:10,691:14,123,691:21,-1:3,691,-1:3,691:2,-1:10,691:5,124,691:30,-" +
"1:3,691,-1:3,691:2,-1:10,691:6,125,691:29,-1:3,691,-1:3,691:2,-1:10,691:9,4" +
"13,691:26,-1:3,691,-1:3,691:2,-1:10,691:5,415,691:30,-1:3,691,-1:3,691:2,-1" +
":10,691:9,126,691:26,-1:3,691,-1:3,691:2,-1:10,691:17,127,691:18,-1:3,691,-" +
"1:3,691:2,-1:10,691:17,128,691:18,-1:3,691,-1:3,691:2,-1:10,691:17,621,691:" +
"18,-1:3,691,-1:3,691:2,-1:10,691:11,129,691:24,-1:3,691,-1:3,691:2,-1:10,69" +
"1:14,416,691:21,-1:3,691,-1:3,691:2,-1:10,691:6,130,691:29,-1:3,691,-1:3,69" +
"1:2,-1:10,691:6,131,691:29,-1:3,691,-1:3,691:2,-1:10,691:20,561,691:15,-1:3" +
",691,-1:3,691:2,-1:10,691:17,132,691:18,-1:3,691,-1:3,691:2,-1:10,691:18,42" +
"3,691:17,-1:3,691,-1:3,691:2,-1:10,691:11,424,691:24,-1:3,691,-1:3,691:2,-1" +
":10,691:17,133,691:18,-1:3,691,-1:3,691:2,-1:10,691:18,425,134,691:16,-1:3," +
"691,-1:3,691:2,-1:10,691:17,135,691:18,-1:3,691,-1:3,691:2,-1:10,691:5,136," +
"691:30,-1:3,691,-1:3,691:2,-1:10,691:6,562,691:29,-1:3,691,-1:3,691:2,-1:10" +
",691:14,137,691:21,-1:3,691,-1:3,691:2,-1:10,691:4,427,691:31,-1:3,691,-1:3" +
",691:2,-1:10,691:28,138,691:7,-1:3,691,-1:3,691:2,-1:10,691:15,428,691:20,-" +
"1:3,691,-1:3,691:2,-1:10,691:25,139,691:10,-1:3,691,-1:3,691:2,-1:10,691:17" +
",140,691:18,-1:3,691,-1:3,691:2,-1:10,691:9,430,691:26,-1:3,691,-1:3,691:2," +
"-1:10,691:17,141,691:18,-1:3,691,-1:3,691:2,-1:10,691:5,566,691:30,-1:3,691" +
",-1:3,691:2,-1:10,691:6,142,691:29,-1:3,691,-1:3,691:2,-1:10,691:22,432,691" +
":13,-1:3,691,-1:3,691:2,-1:10,691:28,143,691:7,-1:3,691,-1:3,691:2,-1:10,69" +
"1:4,434,691:3,435,691:4,436,437,691:2,438,691:18,-1:3,691,-1:3,691:2,-1:10," +
"691:18,439,691:17,-1:3,691,-1:3,691:2,-1:10,691:8,568,691:27,-1:3,691,-1:3," +
"691:2,-1:10,691:7,442,691:28,-1:3,691,-1:3,691:2,-1:10,691:20,144,691:15,-1" +
":3,691,-1:3,691:2,-1:10,691:14,444,691:21,-1:3,691,-1:3,691:2,-1:10,691:6,1" +
"45,691:29,-1:3,691,-1:3,691:2,-1:10,691:17,146,691:18,-1:3,691,-1:3,691:2,-" +
"1:10,691:8,147,691:27,-1:3,691,-1:3,691:2,-1:10,691:19,148,691:16,-1:3,691," +
"-1:3,691:2,-1:10,691:19,149,691:16,-1:3,691,-1:3,691:2,-1:10,691:9,446,691:" +
"26,-1:3,691,-1:3,691:2,-1:10,691:4,569,691:31,-1:3,691,-1:3,691:2,-1:10,691" +
":16,447,691:19,-1:3,691,-1:3,691:2,-1:10,691:28,570,691:7,-1:3,691,-1:3,691" +
":2,-1:10,691:5,448,691:30,-1:3,691,-1:3,691:2,-1:10,691:4,449,691:3,624,691" +
":4,450,451,691:21,-1:3,691,-1:3,691:2,-1:10,691:9,150,691:26,-1:3,691,-1:3," +
"691:2,-1:10,691:22,453,691:13,-1:3,691,-1:3,691:2,-1:10,691:8,151,691:13,45" +
"4,691:13,-1:3,691,-1:3,691:2,-1:10,691:28,152,691:7,-1:3,691,-1:3,691:2,-1:" +
"10,691:15,571,691:20,-1:3,691,-1:3,691:2,-1:10,691:11,153,691:24,-1:3,691,-" +
"1:3,691:2,-1:10,691:7,455,691:28,-1:3,691,-1:3,691:2,-1:10,691:4,457,691:31" +
",-1:3,691,-1:3,691:2,-1:10,691:17,459,691:18,-1:3,691,-1:3,691:2,-1:10,691:" +
"9,460,691:26,-1:3,691,-1:3,691:2,-1:10,691:16,461,691:19,-1:3,691,-1:3,691:" +
"2,-1:10,691:28,573,691:7,-1:3,691,-1:3,691:2,-1:10,691:6,625,691:29,-1:3,69" +
"1,-1:3,691:2,-1:10,691:19,154,691:16,-1:3,691,-1:3,691:2,-1:10,691:8,155,69" +
"1:27,-1:3,691,-1:3,691:2,-1:10,691:21,156,691:14,-1:3,691,-1:3,691:2,-1:10," +
"691:5,464,691:30,-1:3,691,-1:3,691:2,-1:10,691:20,465,691:15,-1:3,691,-1:3," +
"691:2,-1:10,691:15,466,691:20,-1:3,691,-1:3,691:2,-1:10,691:8,467,691:27,-1" +
":3,691,-1:3,691:2,-1:10,691:7,468,691:28,-1:3,691,-1:3,691:2,-1:10,691:4,47" +
"0,691:31,-1:3,691,-1:3,691:2,-1:10,691:14,157,691:21,-1:3,691,-1:3,691:2,-1" +
":10,691:11,158,691:24,-1:3,691,-1:3,691:2,-1:10,691:13,472,691:22,-1:3,691," +
"-1:3,691:2,-1:10,691:22,473,691:13,-1:3,691,-1:3,691:2,-1:10,691:5,159,691:" +
"30,-1:3,691,-1:3,691:2,-1:10,691:25,474,691:10,-1:3,691,-1:3,691:2,-1:10,69" +
"1:21,160,691:14,-1:3,691,-1:3,691:2,-1:10,691:5,475,691:30,-1:3,691,-1:3,69" +
"1:2,-1:10,691:20,476,691:15,-1:3,691,-1:3,691:2,-1:10,691:15,477,691:20,-1:" +
"3,691,-1:3,691:2,-1:10,691:7,479,691:28,-1:3,691,-1:3,691:2,-1:10,691:17,16" +
"1,691:18,-1:3,691,-1:3,691:2,-1:10,691:11,162,691:24,-1:3,691,-1:3,691:2,-1" +
":10,691:13,480,691:22,-1:3,691,-1:3,691:2,-1:10,691:22,481,691:13,-1:3,691," +
"-1:3,691:2,-1:10,691:5,163,691:30,-1:3,691,-1:3,691:2,-1:10,691:19,164,691:" +
"16,-1:3,691,-1:3,691:2,-1:10,691:27,165,691:8,-1:3,691,-1:3,691:2,-1:10,691" +
":7,483,691:28,-1:3,691,-1:3,691:2,-1:10,691:17,166,691:18,-1:3,691,-1:3,691" +
":2,-1:10,691:8,484,691:27,-1:3,691,-1:3,691:2,-1:10,691:27,167,691:8,-1:3,6" +
"91,-1:3,691:2,-1:10,691:4,486,691:31,-1:3,691,-1:3,691:2,-1:10,691:8,487,69" +
"1:27,-1:3,691,-1:3,691:2,-1:10,691:5,488,691:30,-1:3,691,-1:3,691:2,-1:10,6" +
"91:4,489,691:31,-1:3,691,-1:3,691:2,-1:10,691:25,490,691:10,-1:3,691,-1:3,6" +
"91:2,-1:10,691:5,491,691:30,-1:3,691,-1:3,691:2,-1:10,691:17,168,691:18,-1:" +
"3,691,-1:3,691:2,-1:10,691:25,492,691:10,-1:3,691,-1:3,691:2,-1:10,691:17,1" +
"69,691:18,-1:3,691,-1:3,691:2,-1:2,172,-1:7,691:17,493,691:13,493:2,691:2,4" +
"93,-1:3,691,-1:3,691:2,-1:10,691:4,191,691:2,192,691:28,-1:3,691,-1:3,691:2" +
",-1:10,691:7,228,691:28,-1:3,691,-1:3,691:2,-1:10,691:9,628,691:26,-1:3,691" +
",-1:3,691:2,-1:10,691:15,251,691:20,-1:3,691,-1:3,691:2,-1:10,691:21,513,69" +
"1:14,-1:3,691,-1:3,691:2,-1:10,691:19,264,691:16,-1:3,691,-1:3,691:2,-1:10," +
"691:10,699,691:25,-1:3,691,-1:3,691:2,-1:10,691:4,246,691:31,-1:3,691,-1:3," +
"691:2,-1:10,691:5,637,691:30,-1:3,691,-1:3,691:2,-1:10,691:8,245,691:27,-1:" +
"3,691,-1:3,691:2,-1:10,691:20,254,691:15,-1:3,691,-1:3,691:2,-1:10,691:17,2" +
"53,691:18,-1:3,691,-1:3,691:2,-1:10,691:6,514,691:29,-1:3,691,-1:3,691:2,-1" +
":10,691:22,292,691:13,-1:3,691,-1:3,691:2,-1:10,691:25,641,691:10,-1:3,691," +
"-1:3,691:2,-1:10,691:15,271,691:20,-1:3,691,-1:3,691:2,-1:10,691:9,718,691:" +
"26,-1:3,691,-1:3,691:2,-1:10,691:7,278,691:28,-1:3,691,-1:3,691:2,-1:10,691" +
":19,600,691:16,-1:3,691,-1:3,691:2,-1:10,691:24,290,691:11,-1:3,691,-1:3,69" +
"1:2,-1:10,691:4,646,691:31,-1:3,691,-1:3,691:2,-1:10,691:8,599,691:27,-1:3," +
"691,-1:3,691:2,-1:10,691:20,530,691:15,-1:3,691,-1:3,691:2,-1:10,691:14,604" +
",691:21,-1:3,691,-1:3,691:2,-1:10,691:17,291,691:18,-1:3,691,-1:3,691:2,-1:" +
"10,691:5,307,691:30,-1:3,691,-1:3,691:2,-1:10,691:16,318,691:19,-1:3,691,-1" +
":3,691:2,-1:10,691:22,649,691:13,-1:3,691,-1:3,691:2,-1:10,691:15,524,691:2" +
"0,-1:3,691,-1:3,691:2,-1:10,691:19,322,691:16,-1:3,691,-1:3,691:2,-1:10,691" +
":7,353,691:28,-1:3,691,-1:3,691:2,-1:10,691:9,335,691:26,-1:3,691,-1:3,691:" +
"2,-1:10,691:4,673,691:31,-1:3,691,-1:3,691:2,-1:10,691:8,537,691:27,-1:3,69" +
"1,-1:3,691:2,-1:10,691:20,343,691:15,-1:3,691,-1:3,691:2,-1:10,691:14,355,6" +
"91:21,-1:3,691,-1:3,691:2,-1:10,691:17,609,691:18,-1:3,691,-1:3,691:2,-1:10" +
",691:24,675,691:11,-1:3,691,-1:3,691:2,-1:10,691:5,350,691:30,-1:3,691,-1:3" +
",691:2,-1:10,691:22,356,691:13,-1:3,691,-1:3,691:2,-1:10,691:15,373,691:20," +
"-1:3,691,-1:3,691:2,-1:10,691:19,378,691:16,-1:3,691,-1:3,691:2,-1:10,691:6" +
",366,691:29,-1:3,691,-1:3,691:2,-1:10,691:9,543,691:26,-1:3,691,-1:3,691:2," +
"-1:10,691:4,367,691:31,-1:3,691,-1:3,691:2,-1:10,691:7,655,691:28,-1:3,691," +
"-1:3,691:2,-1:10,691:8,545,691:27,-1:3,691,-1:3,691:2,-1:10,691:14,689,691:" +
"21,-1:3,691,-1:3,691:2,-1:10,691:23,376,691:12,-1:3,691,-1:3,691:2,-1:10,69" +
"1:15,616,691:20,-1:3,691,-1:3,691:2,-1:10,691:17,391,691:18,-1:3,691,-1:3,6" +
"91:2,-1:10,691:22,389,691:13,-1:3,691,-1:3,691:2,-1:10,691:19,657,691:16,-1" +
":3,691,-1:3,691:2,-1:10,691:5,396,691:30,-1:3,691,-1:3,691:2,-1:10,691:9,55" +
"4,691:26,-1:3,691,-1:3,691:2,-1:10,691:7,390,691:28,-1:3,691,-1:3,691:2,-1:" +
"10,691:6,678,691:29,-1:3,691,-1:3,691:2,-1:10,691:20,414,691:15,-1:3,691,-1" +
":3,691:2,-1:10,691:4,410,691:31,-1:3,691,-1:3,691:2,-1:10,691:8,407,691:27," +
"-1:3,691,-1:3,691:2,-1:10,691:15,411,691:20,-1:3,691,-1:3,691:2,-1:10,691:9" +
",422,691:26,-1:3,691,-1:3,691:2,-1:10,691:5,420,691:30,-1:3,691,-1:3,691:2," +
"-1:10,691:17,419,691:18,-1:3,691,-1:3,691:2,-1:10,691:14,418,691:21,-1:3,69" +
"1,-1:3,691:2,-1:10,691:20,426,691:15,-1:3,691,-1:3,691:2,-1:10,691:6,563,69" +
"1:29,-1:3,691,-1:3,691:2,-1:10,691:4,431,691:31,-1:3,691,-1:3,691:2,-1:10,6" +
"91:15,441,691:20,-1:3,691,-1:3,691:2,-1:10,691:9,443,691:26,-1:3,691,-1:3,6" +
"91:2,-1:10,691:5,440,691:30,-1:3,691,-1:3,691:2,-1:10,691:22,433,691:13,-1:" +
"3,691,-1:3,691:2,-1:10,691:14,445,691:21,-1:3,691,-1:3,691:2,-1:10,691:9,62" +
"3,691:26,-1:3,691,-1:3,691:2,-1:10,691:4,452,691:31,-1:3,691,-1:3,691:2,-1:" +
"10,691:15,456,691:20,-1:3,691,-1:3,691:2,-1:10,691:4,458,691:31,-1:3,691,-1" +
":3,691:2,-1:10,691:9,463,691:26,-1:3,691,-1:3,691:2,-1:10,691:15,469,691:20" +
",-1:3,691,-1:3,691:2,-1:10,691:4,471,691:31,-1:3,691,-1:3,691:2,-1:10,691:2" +
"2,478,691:13,-1:3,691,-1:3,691:2,-1:10,691:5,193,691,496,691:7,194,691,195," +
"691:18,-1:3,691,-1:3,691:2,-1:10,691:7,519,691:28,-1:3,691,-1:3,691:2,-1:10" +
",691:9,595,691:26,-1:3,691,-1:3,691:2,-1:10,691:15,255,691:20,-1:3,691,-1:3" +
",691:2,-1:10,691:21,721,691:14,-1:3,691,-1:3,691:2,-1:10,691:19,635,691:16," +
"-1:3,691,-1:3,691:2,-1:10,691:4,717,691:31,-1:3,691,-1:3,691:2,-1:10,691:5," +
"249,691:30,-1:3,691,-1:3,691:2,-1:10,691:8,257,691:27,-1:3,691,-1:3,691:2,-" +
"1:10,691:20,591,691:15,-1:3,691,-1:3,691:2,-1:10,691:17,516,691:18,-1:3,691" +
",-1:3,691:2,-1:10,691:6,265,691:29,-1:3,691,-1:3,691:2,-1:10,691:22,305,691" +
":13,-1:3,691,-1:3,691:2,-1:10,691:15,684,691:20,-1:3,691,-1:3,691:2,-1:10,6" +
"91:9,277,691:26,-1:3,691,-1:3,691:2,-1:10,691:7,296,691:28,-1:3,691,-1:3,69" +
"1:2,-1:10,691:19,300,691:16,-1:3,691,-1:3,691:2,-1:10,691:24,650,691:11,-1:" +
"3,691,-1:3,691:2,-1:10,691:4,312,691:31,-1:3,691,-1:3,691:2,-1:10,691:20,67" +
"1,691:15,-1:3,691,-1:3,691:2,-1:10,691:14,302,691:21,-1:3,691,-1:3,691:2,-1" +
":10,691:17,648,691:18,-1:3,691,-1:3,691:2,-1:10,691:5,309,691:30,-1:3,691,-" +
"1:3,691:2,-1:10,691:22,316,691:13,-1:3,691,-1:3,691:2,-1:10,691:15,342,691:" +
"20,-1:3,691,-1:3,691:2,-1:10,691:9,540,691:26,-1:3,691,-1:3,691:2,-1:10,691" +
":4,339,691:31,-1:3,691,-1:3,691:2,-1:10,691:8,334,691:27,-1:3,691,-1:3,691:" +
"2,-1:10,691:20,344,691:15,-1:3,691,-1:3,691:2,-1:10,691:17,346,691:18,-1:3," +
"691,-1:3,691:2,-1:10,691:5,352,691:30,-1:3,691,-1:3,691:2,-1:10,691:22,375," +
"691:13,-1:3,691,-1:3,691:2,-1:10,691:15,547,691:20,-1:3,691,-1:3,691:2,-1:1" +
"0,691:6,677,691:29,-1:3,691,-1:3,691:2,-1:10,691:9,549,691:26,-1:3,691,-1:3" +
",691:2,-1:10,691:4,548,691:31,-1:3,691,-1:3,691:2,-1:10,691:8,615,691:27,-1" +
":3,691,-1:3,691:2,-1:10,691:14,377,691:21,-1:3,691,-1:3,691:2,-1:10,691:15," +
"551,691:20,-1:3,691,-1:3,691:2,-1:10,691:22,402,691:13,-1:3,691,-1:3,691:2," +
"-1:10,691:9,398,691:26,-1:3,691,-1:3,691:2,-1:10,691:7,409,691:28,-1:3,691," +
"-1:3,691:2,-1:10,691:8,408,691:27,-1:3,691,-1:3,691:2,-1:10,691:15,412,691:" +
"20,-1:3,691,-1:3,691:2,-1:10,691:9,622,691:26,-1:3,691,-1:3,691:2,-1:10,691" +
":17,564,691:18,-1:3,691,-1:3,691:2,-1:10,691:20,429,691:15,-1:3,691,-1:3,69" +
"1:2,-1:10,691:4,567,691:31,-1:3,691,-1:3,691:2,-1:10,691:15,462,691:20,-1:3" +
",691,-1:3,691:2,-1:10,691:4,572,691:31,-1:3,691,-1:3,691:2,-1:10,691:15,574" +
",691:20,-1:3,691,-1:3,691:2,-1:10,691:4,198,691:12,199,691:4,200,691:13,-1:" +
"3,691,-1:3,691:2,-1:10,691:15,665,691:20,-1:3,691,-1:3,691:2,-1:10,691:19,2" +
"69,691:16,-1:3,691,-1:3,691:2,-1:10,691:4,259,691:31,-1:3,691,-1:3,691:2,-1" +
":10,691:5,638,691:30,-1:3,691,-1:3,691:2,-1:10,691:8,517,691:27,-1:3,691,-1" +
":3,691:2,-1:10,691:20,267,691:15,-1:3,691,-1:3,691:2,-1:10,691:17,263,691:1" +
"8,-1:3,691,-1:3,691:2,-1:10,691:6,276,691:29,-1:3,691,-1:3,691:2,-1:10,691:" +
"22,672,691:13,-1:3,691,-1:3,691:2,-1:10,691:15,602,691:20,-1:3,691,-1:3,691" +
":2,-1:10,691:9,281,691:26,-1:3,691,-1:3,691:2,-1:10,691:7,647,691:28,-1:3,6" +
"91,-1:3,691:2,-1:10,691:24,304,691:11,-1:3,691,-1:3,691:2,-1:10,691:4,532,6" +
"91:31,-1:3,691,-1:3,691:2,-1:10,691:20,319,691:15,-1:3,691,-1:3,691:2,-1:10" +
",691:14,315,691:21,-1:3,691,-1:3,691:2,-1:10,691:17,294,691:18,-1:3,691,-1:" +
"3,691:2,-1:10,691:5,311,691:30,-1:3,691,-1:3,691:2,-1:10,691:22,536,691:13," +
"-1:3,691,-1:3,691:2,-1:10,691:9,345,691:26,-1:3,691,-1:3,691:2,-1:10,691:4," +
"541,691:31,-1:3,691,-1:3,691:2,-1:10,691:8,340,691:27,-1:3,691,-1:3,691:2,-" +
"1:10,691:20,349,691:15,-1:3,691,-1:3,691:2,-1:10,691:17,611,691:18,-1:3,691" +
",-1:3,691:2,-1:10,691:5,361,691:30,-1:3,691,-1:3,691:2,-1:10,691:4,374,691:" +
"31,-1:3,691,-1:3,691:2,-1:10,691:14,380,691:21,-1:3,691,-1:3,691:2,-1:10,69" +
"1:15,393,691:20,-1:3,691,-1:3,691:2,-1:10,691:22,556,691:13,-1:3,691,-1:3,6" +
"91:2,-1:10,691:9,406,691:26,-1:3,691,-1:3,691:2,-1:10,691:15,417,691:20,-1:" +
"3,691,-1:3,691:2,-1:10,691:7,208,691:3,209,691:5,502,691:2,210,691,503,691:" +
"5,501,691:7,-1:3,691,-1:3,691:2,-1:10,691:15,666,691:20,-1:3,691,-1:3,691:2" +
",-1:10,691:4,272,691:31,-1:3,691,-1:3,691:2,-1:10,691:5,273,691:30,-1:3,691" +
",-1:3,691:2,-1:10,691:8,703,691:27,-1:3,691,-1:3,691:2,-1:10,691:20,275,691" +
":15,-1:3,691,-1:3,691:2,-1:10,691:17,644,691:18,-1:3,691,-1:3,691:2,-1:10,6" +
"91:6,293,691:29,-1:3,691,-1:3,691:2,-1:10,691:9,306,691:26,-1:3,691,-1:3,69" +
"1:2,-1:10,691:17,295,691:18,-1:3,691,-1:3,691:2,-1:10,691:5,314,691:30,-1:3" +
",691,-1:3,691:2,-1:10,691:22,538,691:13,-1:3,691,-1:3,691:2,-1:10,691:9,347" +
",691:26,-1:3,691,-1:3,691:2,-1:10,691:4,348,691:31,-1:3,691,-1:3,691:2,-1:1" +
"0,691:8,542,691:27,-1:3,691,-1:3,691:2,-1:10,691:20,364,691:15,-1:3,691,-1:" +
"3,691:2,-1:10,691:5,369,691:30,-1:3,691,-1:3,691:2,-1:10,691:4,379,691:31,-" +
"1:3,691,-1:3,691:2,-1:10,691:15,397,691:20,-1:3,691,-1:3,691:2,-1:10,691:22" +
",403,691:13,-1:3,691,-1:3,691:2,-1:10,691:15,421,691:20,-1:3,691,-1:3,691:2" +
",-1:10,691:6,212,691:8,213,691:20,-1:3,691,-1:3,691:2,-1:10,691:15,268,691:" +
"20,-1:3,691,-1:3,691:2,-1:10,691:17,682,691:18,-1:3,691,-1:3,691:2,-1:10,69" +
"1:9,313,691:26,-1:3,691,-1:3,691:2,-1:10,691:17,298,691:18,-1:3,691,-1:3,69" +
"1:2,-1:10,691:5,327,691:30,-1:3,691,-1:3,691:2,-1:10,691:4,535,691:31,-1:3," +
"691,-1:3,691:2,-1:10,691:8,354,691:27,-1:3,691,-1:3,691:2,-1:10,691:5,370,6" +
"91:30,-1:3,691,-1:3,691:2,-1:10,691:4,550,691:31,-1:3,691,-1:3,691:2,-1:10," +
"691:15,400,691:20,-1:3,691,-1:3,691:2,-1:10,691:15,565,691:20,-1:3,691,-1:3" +
",691:2,-1:10,691:17,642,691:18,-1:3,691,-1:3,691:2,-1:10,691:17,299,691:18," +
"-1:3,691,-1:3,691:2,-1:10,691:4,653,691:31,-1:3,691,-1:3,691:2,-1:10,691:8," +
"539,691:27,-1:3,691,-1:3,691:2,-1:10,691:5,724,691:30,-1:3,691,-1:3,691:2,-" +
"1:10,691:7,504,691,578,691:4,216,498,691,217,691:2,627,691,583,691,716,691," +
"585,691:9,-1:3,691,-1:3,691:2,-1:10,691:17,523,691:18,-1:3,691,-1:3,691:2,-" +
"1:10,691:17,301,691:18,-1:3,691,-1:3,691:2,-1:10,691:8,688,691:27,-1:3,691," +
"-1:3,691:2,-1:10,691:4,218,691:2,629,691:7,219,691:20,-1:3,691,-1:3,691:2,-" +
"1:10,691:17,286,691:18,-1:3,691,-1:3,691:2,-1:10,691:17,685,691:18,-1:3,691" +
",-1:3,691:2,-1:10,691:4,220,691:2,221,691:28,-1:3,691,-1:3,691:2,-1:10,691:" +
"17,651,691:18,-1:3,691,-1:3,691:2,-1:10,691:5,222,691:18,506,691:11,-1:3,69" +
"1,-1:3,691:2,-1:10,691:17,332,691:18,-1:3,691,-1:3,691:2,-1:10,691:4,584,69" +
"1:2,631,691:7,500,691,577,691:18,-1:3,691,-1:3,691:2,-1:10,691:17,674,691:1" +
"8,-1:3,691,-1:3,691:2,-1:10,691:7,223,224,691:8,633,691:4,499,691:13,-1:3,6" +
"91,-1:3,691:2,-1:10,691:17,687,691:18,-1:3,691,-1:3,691:2,-1:10,691:8,225,6" +
"91:27,-1:3,691,-1:3,691:2,-1:10,691:8,659,691:6,586,691:20,-1:3,691,-1:3,69" +
"1:2,-1:10,691:13,576,691:22,-1:3,691,-1:3,691:2,-1:10,691:36,-1:3,691,-1:3," +
"691,226,-1:10,691:28,596,691:7,-1:3,691,-1:3,691:2,-1:10,691:20,669,691:15," +
"-1:3,691,-1:3,691:2,-1:10,691:22,686,691:13,-1:3,691,-1:3,691:2,-1:10,691:6" +
",555,691:29,-1:3,691,-1:3,691:2,-1:10,691:4,558,691:31,-1:3,691,-1:3,691:2," +
"-1:10,691:4,670,691:31,-1:3,691,-1:3,691:2,-1:10,691:15,719,691:20,-1:3,691" +
",-1:3,691:2,-1:10,691:23,720,691:12,-1:3,691,-1:3,691:2,-1:10,691:9,618,691" +
":26,-1:3,691,-1:3,691:2,-1:10,691:6,690,691:29,-1:3,691,-1:3,691:2,-1:10,69" +
"1:8,723,691:27,-1:3,691,-1:3,691:2,-1:10,691:4,725,691:31,-1:3,691,-1:3,691" +
":2,-1");

	public java_cup.runtime.Symbol next_token ()
		throws java.io.IOException {
		int yy_lookahead;
		int yy_anchor = YY_NO_ANCHOR;
		int yy_state = yy_state_dtrans[yy_lexical_state];
		int yy_next_state = YY_NO_STATE;
		int yy_last_accept_state = YY_NO_STATE;
		boolean yy_initial = true;
		int yy_this_accept;

		yy_mark_start();
		yy_this_accept = yy_acpt[yy_state];
		if (YY_NOT_ACCEPT != yy_this_accept) {
			yy_last_accept_state = yy_state;
			yy_mark_end();
		}
		while (true) {
			if (yy_initial && yy_at_bol) yy_lookahead = YY_BOL;
			else yy_lookahead = yy_advance();
			yy_next_state = YY_F;
			yy_next_state = yy_nxt[yy_rmap[yy_state]][yy_cmap[yy_lookahead]];
			if (YY_EOF == yy_lookahead && true == yy_initial) {

    return new Symbol(sym.EOF);
			}
			if (YY_F != yy_next_state) {
				yy_state = yy_next_state;
				yy_initial = false;
				yy_this_accept = yy_acpt[yy_state];
				if (YY_NOT_ACCEPT != yy_this_accept) {
					yy_last_accept_state = yy_state;
					yy_mark_end();
				}
			}
			else {
				if (YY_NO_STATE == yy_last_accept_state) {
					throw (new Error("Lexical Error: Unmatched Input."));
				}
				else {
					yy_anchor = yy_acpt[yy_last_accept_state];
					if (0 != (YY_END & yy_anchor)) {
						yy_move_end();
					}
					yy_to_mark();
					switch (yy_last_accept_state) {
					case 1:
						
					case -2:
						break;
					case 2:
						{ return new Symbol(sym.DASH); }
					case -3:
						break;
					case 3:
						{ return new Symbol(sym.SEMI); }
					case -4:
						break;
					case 4:
						{ return new Symbol(sym.COLON); }
					case -5:
						break;
					case 5:
						{ return new Symbol(sym.LPAREN); }
					case -6:
						break;
					case 6:
						{ return new Symbol(sym.RPAREN); }
					case -7:
						break;
					case 7:
						{ return new Symbol(sym.LCURLY); }
					case -8:
						break;
					case 8:
						{ return new Symbol(sym.RCURLY); }
					case -9:
						break;
					case 9:
						{ return new Symbol(sym.DOLLAR); }
					case -10:
						break;
					case 10:
						{ return new Symbol(sym.OPERATOR, new Integer(Selection.GT)); }
					case -11:
						break;
					case 11:
						{ return new Symbol(sym.OPERATOR, new Integer(Selection.EQ)); }
					case -12:
						break;
					case 12:
						{ return new Symbol(sym.OPERATOR, new Integer(Selection.LT)); }
					case -13:
						break;
					case 13:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -14:
						break;
					case 14:
						{ return new Symbol(sym.ATTRIBUTE, new Integer(Atom.O)); }
					case -15:
						break;
					case 15:
						{ return new Symbol(sym.ATTRIBUTE, new Integer(Atom.X)); }
					case -16:
						break;
					case 16:
						{ return new Symbol(sym.ATTRIBUTE, new Integer(Atom.Y)); }
					case -17:
						break;
					case 17:
						{ return new Symbol(sym.ATTRIBUTE, new Integer(Atom.Z)); }
					case -18:
						break;
					case 18:
						{ return new Symbol(sym.ATTRIBUTE, new Integer(Atom.B)); }
					case -19:
						break;
					case 19:
						{
    //System.out.println("DOUBLE " + yytext());
    		    return new Symbol(sym.DOUBLE, new Double(yytext()));
		}
					case -20:
						break;
					case 20:
						{
    //System.out.println("INTEGER " + yytext());
    		    return new Symbol(sym.INTEGER, new Integer(yytext()));
		}
					case -21:
						break;
					case 21:
						{ System.err.println("Illegal character: " + yytext()); }
					case -22:
						break;
					case 22:
						{ /* ignore white space. */ }
					case -23:
						break;
					case 23:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.ARG, new String(yytext()));
		}
					case -24:
						break;
					case 24:
						{ return new Symbol(sym.OPERATOR, new Integer(Selection.GE)); }
					case -25:
						break;
					case 25:
						{ return new Symbol(sym.OPERATOR, new Integer(Selection.LE)); }
					case -26:
						break;
					case 26:
						{ return new Symbol(sym.OPERATOR, new Integer(Selection.NE)); }
					case -27:
						break;
					case 27:
						{ return new Symbol(sym.ON); }
					case -28:
						break;
					case 28:
						{ return new Symbol(sym.OR); }
					case -29:
						break;
					case 29:
						{ return new Symbol(sym.TO); }
					case -30:
						break;
					case 30:
						{ return new Symbol(sym.ID); }
					case -31:
						break;
					case 31:
						{
    //System.out.println("STRING [" + yytext() +"]");
		    int len = yytext().length();
		    String contents = yytext().substring(1, len - 1);
		    //System.out.println("STRING [" + contents + "]");
		    return new Symbol(sym.STRING,
				      new String(contents));
		}
					case -32:
						break;
					case 32:
						{
    //System.out.println("STRING [" + yytext() +"]");
		    int len = yytext().length();
		    String contents = yytext().substring(1, len - 1);
		    //System.out.println("STRING [" + contents + "]");
		    return new Symbol(sym.STRING,
				      new String(contents));
		}
					case -33:
						break;
					case 33:
						{ /* ignore comments */ }
					case -34:
						break;
					case 34:
						{ return new Symbol(sym.AND); }
					case -35:
						break;
					case 35:
						{ return new Symbol(sym.ALL); }
					case -36:
						break;
					case 36:
						{ return new Symbol(sym.NOT); }
					case -37:
						break;
					case 37:
						{ return new Symbol(sym.DNA); }
					case -38:
						break;
					case 38:
						{ return new Symbol(sym.OFF); }
					case -39:
						break;
					case 39:
						{ return new Symbol(sym.RUN); }
					case -40:
						break;
					case 40:
						{ return new Symbol(sym.ZAP); }
					case -41:
						break;
					case 41:
						{ return new Symbol(sym.VDW); }
					case -42:
						break;
					case 42:
						{ return new Symbol(sym.SET); }
					case -43:
						break;
					case 43:
						{ return new Symbol(sym.MAP); }
					case -44:
						break;
					case 44:
						{ return new Symbol(sym.POP); }
					case -45:
						break;
					case 45:
						{ return new Symbol(sym.ATOM); }
					case -46:
						break;
					case 46:
						{ return new Symbol(sym.NAME); }
					case -47:
						break;
					case 47:
						{ return new Symbol(sym.NONE); }
					case -48:
						break;
					case 48:
						{ return new Symbol(sym.TRUE); }
					case -49:
						break;
					case 49:
						{ return new Symbol(sym.XRAY); }
					case -50:
						break;
					case 50:
						{ return new Symbol(sym.CLIP); }
					case -51:
						break;
					case 51:
						{ return new Symbol(sym.IONS); }
					case -52:
						break;
					case 52:
						{ return new Symbol(sym.VIEW); }
					case -53:
						break;
					case 53:
						{ return new Symbol(sym.EDIT); }
					case -54:
						break;
					case 54:
						{ return new Symbol(sym.LAZY); }
					case -55:
						break;
					case 55:
						{ return new Symbol(sym.LOAD); }
					case -56:
						break;
					case 56:
						{ return new Symbol(sym.PEEK); }
					case -57:
						break;
					case 57:
						{ return new Symbol(sym.PUSH); }
					case -58:
						break;
					case 58:
						{ return new Symbol(sym.WIDE); }
					case -59:
						break;
					case 59:
						{ return new Symbol(sym.ANGLE); }
					case -60:
						break;
					case 60:
						{ return new Symbol(sym.RANGE); }
					case -61:
						break;
					case 61:
						{ return new Symbol(sym.COLOR); }
					case -62:
						break;
					case 62:
						{ return new Symbol(sym.CLEAR); }
					case -63:
						break;
					case 63:
						{ return new Symbol(sym.CHAIN); }
					case -64:
						break;
					case 64:
						{ return new Symbol(sym.SOLID); }
					case -65:
						break;
					case 65:
						{ return new Symbol(sym.SLIDE); }
					case -66:
						break;
					case 66:
						{ return new Symbol(sym.LABEL); }
					case -67:
						break;
					case 67:
						{ return new Symbol(sym.LINES); }
					case -68:
						break;
					case 68:
						{ return new Symbol(sym.LIGHT); }
					case -69:
						break;
					case 69:
						{ return new Symbol(sym.FALSE); }
					case -70:
						break;
					case 70:
						{ return new Symbol(sym.FIXED); }
					case -71:
						break;
					case 71:
						{ return new Symbol(sym.FETCH); }
					case -72:
						break;
					case 72:
						{ return new Symbol(sym.PRINT); }
					case -73:
						break;
					case 73:
						{ return new Symbol(sym.GRAPH); }
					case -74:
						break;
					case 74:
						{ return new Symbol(sym.GROUP); }
					case -75:
						break;
					case 75:
						{ return new Symbol(sym.WRITE); }
					case -76:
						break;
					case 76:
						{ return new Symbol(sym.HBOND); }
					case -77:
						break;
					case 77:
						{ /* ignore other kind of comments. */ }
					case -78:
						break;
					case 78:
						{ return new Symbol(sym.AROUND); }
					case -79:
						break;
					case 79:
						{ return new Symbol(sym.ACTIVE); }
					case -80:
						break;
					case 80:
						{ return new Symbol(sym.APPEND); }
					case -81:
						break;
					case 81:
						{ return new Symbol(sym.DELETE); }
					case -82:
						break;
					case 82:
						{ return new Symbol(sym.DEFINE); }
					case -83:
						break;
					case 83:
						{ return new Symbol(sym.OBJECT); }
					case -84:
						break;
					case 84:
						{ return new Symbol(sym.RADIUS); }
					case -85:
						break;
					case 85:
						{ return new Symbol(sym.REMOVE); }
					case -86:
						break;
					case 86:
						{ return new Symbol(sym.TOGGLE); }
					case -87:
						break;
					case 87:
						{ return new Symbol(sym.BONDED); }
					case -88:
						break;
					case 88:
						{ return new Symbol(sym.COLOR); }
					case -89:
						break;
					case 89:
						{ return new Symbol(sym.COPYTO); }
					case -90:
						break;
					case 90:
						{ return new Symbol(sym.CENTER); }
					case -91:
						break;
					case 91:
						{ return new Symbol(sym.CENTER); }
					case -92:
						break;
					case 92:
						{ return new Symbol(sym.CHARGE); }
					case -93:
						break;
					case 93:
						{ return new Symbol(sym.INVERT); }
					case -94:
						break;
					case 94:
						{ return new Symbol(sym.STICKS); }
					case -95:
						break;
					case 95:
						{ return new Symbol(sym.SIMPLE); }
					case -96:
						break;
					case 96:
						{ return new Symbol(sym.SELECT); }
					case -97:
						break;
					case 97:
						{ return new Symbol(sym.SPHERE); }
					case -98:
						break;
					case 98:
						{ return new Symbol(sym.MATRIX); }
					case -99:
						break;
					case 99:
						{ return new Symbol(sym.MODULO); }
					case -100:
						break;
					case 100:
						{ return new Symbol(sym.UPDATE); }
					case -101:
						break;
					case 101:
						{ return new Symbol(sym.ANIMATE); }
					case -102:
						break;
					case 102:
						{ return new Symbol(sym.DISPLAY); }
					case -103:
						break;
					case 103:
						{ return new Symbol(sym.DEFAULT); }
					case -104:
						break;
					case 104:
						{ return new Symbol(sym.RESIDUE); }
					case -105:
						break;
					case 105:
						{ return new Symbol(sym.REPAINT); }
					case -106:
						break;
					case 106:
						{ return new Symbol(sym.TORSION); }
					case -107:
						break;
					case 107:
						{ return new Symbol(sym.TEXTURE); }
					case -108:
						break;
					case 108:
						{ return new Symbol(sym.CONTACT); }
					case -109:
						break;
					case 109:
						{ return new Symbol(sym.CONTOUR); }
					case -110:
						break;
					case 110:
						{ return new Symbol(sym.CONTEXT); }
					case -111:
						break;
					case 111:
						{ return new Symbol(sym.CURRENT); }
					case -112:
						break;
					case 112:
						{ return new Symbol(sym.INSTALL); }
					case -113:
						break;
					case 113:
						{ return new Symbol(sym.EXCLUDE); }
					case -114:
						break;
					case 114:
						{ return new Symbol(sym.ELEMENT); }
					case -115:
						break;
					case 115:
						{ return new Symbol(sym.SOLVENT); }
					case -116:
						break;
					case 116:
						{ return new Symbol(sym.SURFACE); }
					case -117:
						break;
					case 117:
						{ return new Symbol(sym.SPHERES); }
					case -118:
						break;
					case 118:
						{ return new Symbol(sym.DISTANCE); }
					case -119:
						break;
					case 119:
						{ return new Symbol(sym.DECREASE); }
					case -120:
						break;
					case 120:
						{ return new Symbol(sym.BACKFACE); }
					case -121:
						break;
					case 121:
						{ return new Symbol(sym.INCREASE); }
					case -122:
						break;
					case 122:
						{ return new Symbol(sym.EVALUATE); }
					case -123:
						break;
					case 123:
						{ return new Symbol(sym.SECSTRUC); }
					case -124:
						break;
					case 124:
						{ return new Symbol(sym.SKELETON); }
					case -125:
						break;
					case 125:
						{ return new Symbol(sym.LABELLED); }
					case -126:
						break;
					case 126:
						{ return new Symbol(sym.MOLEXACT); }
					case -127:
						break;
					case 127:
						{ return new Symbol(sym.MOLECULE); }
					case -128:
						break;
					case 128:
						{ return new Symbol(sym.UNDEFINE); }
					case -129:
						break;
					case 129:
						{ return new Symbol(sym.PROPERTY); }
					case -130:
						break;
					case 130:
						{ return new Symbol(sym.AMINOACID); }
					case -131:
						break;
					case 131:
						{ return new Symbol(sym.DISPLAYED); }
					case -132:
						break;
					case 132:
						{ return new Symbol(sym.BYRESIDUE); }
					case -133:
						break;
					case 133:
						{ return new Symbol(sym.COMPOSITE); }
					case -134:
						break;
					case 134:
						{ return new Symbol(sym.CYLINDERS); }
					case -135:
						break;
					case 135:
						{ return new Symbol(sym.CURVATURE); }
					case -136:
						break;
					case 136:
						{ return new Symbol(sym.INSERTION); }
					case -137:
						break;
					case 137:
						{ return new Symbol(sym.SCHEMATIC); }
					case -138:
						break;
					case 138:
						{ return new Symbol(sym.LINEWIDTH); }
					case -139:
						break;
					case 139:
						{ return new Symbol(sym.MODELLING); }
					case -140:
						break;
					case 140:
						{ return new Symbol(sym.ANASURFACE); }
					case -141:
						break;
					case 141:
						{ return new Symbol(sym.DOTSURFACE); }
					case -142:
						break;
					case 142:
						{ return new Symbol(sym.BACKGROUND); }
					case -143:
						break;
					case 143:
						{ return new Symbol(sym.BOND_WIDTH); }
					case -144:
						break;
					case 144:
						{ return new Symbol(sym.SEQUENTIAL); }
					case -145:
						break;
					case 145:
						{ return new Symbol(sym.FORCEFIELD); }
					case -146:
						break;
					case 146:
						{ return new Symbol(sym.ACTIVE_SITE); }
					case -147:
						break;
					case 147:
						{ return new Symbol(sym.RECTANGULAR); }
					case -148:
						break;
					case 148:
						{ return new Symbol(sym.BALL_RADIUS); }
					case -149:
						break;
					case 149:
						{ return new Symbol(sym.BOND_WIDTH); }
					case -150:
						break;
					case 150:
						{ return new Symbol(sym.ENVIRONMENT); }
					case -151:
						break;
					case 151:
						{ return new Symbol(sym.STICK_COLOR); }
					case -152:
						break;
					case 152:
						{ return new Symbol(sym.STICK_RADIUS); }
					case -153:
						break;
					case 153:
						{ return new Symbol(sym.TRANSPARENCY); }
					case -154:
						break;
					case 154:
						{ return new Symbol(sym.STICK_RADIUS); }
					case -155:
						break;
					case 155:
						{ return new Symbol(sym.STICK_COLOR); }
					case -156:
						break;
					case 156:
						{ return new Symbol(sym.COLOR_BY_ATOM); }
					case -157:
						break;
					case 157:
						{ return new Symbol(sym.ELECTROSTATIC); }
					case -158:
						break;
					case 158:
						{ return new Symbol(sym.LIPOPHILICITY); }
					case -159:
						break;
					case 159:
						{ return new Symbol(sym.COLOR_BY_CHAIN); }
					case -160:
						break;
					case 160:
						{ return new Symbol(sym.COLOR_BY_ATOM); }
					case -161:
						break;
					case 161:
						{ return new Symbol(sym.COLOR_BY_BVALUE); }
					case -162:
						break;
					case 162:
						{ return new Symbol(sym.COLOR_BY_ENERGY); }
					case -163:
						break;
					case 163:
						{ return new Symbol(sym.COLOR_BY_CHAIN); }
					case -164:
						break;
					case 164:
						{ return new Symbol(sym.CYLINDER_RADIUS); }
					case -165:
						break;
					case 165:
						{ return new Symbol(sym.COLOR_BY_RAINBOW); }
					case -166:
						break;
					case 166:
						{ return new Symbol(sym.COLOR_BY_BVALUE); }
					case -167:
						break;
					case 167:
						{ return new Symbol(sym.COLOR_BY_RAINBOW); }
					case -168:
						break;
					case 168:
						{ return new Symbol(sym.COLOR_BY_BVALUE_RANGE); }
					case -169:
						break;
					case 169:
						{ return new Symbol(sym.COLOR_BY_BVALUE_RANGE); }
					case -170:
						break;
					case 171:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -171:
						break;
					case 172:
						{
    //System.out.println("DOUBLE " + yytext());
    		    return new Symbol(sym.DOUBLE, new Double(yytext()));
		}
					case -172:
						break;
					case 173:
						{
    //System.out.println("INTEGER " + yytext());
    		    return new Symbol(sym.INTEGER, new Integer(yytext()));
		}
					case -173:
						break;
					case 174:
						{ System.err.println("Illegal character: " + yytext()); }
					case -174:
						break;
					case 175:
						{ /* ignore comments */ }
					case -175:
						break;
					case 177:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -176:
						break;
					case 178:
						{
    //System.out.println("DOUBLE " + yytext());
    		    return new Symbol(sym.DOUBLE, new Double(yytext()));
		}
					case -177:
						break;
					case 179:
						{ System.err.println("Illegal character: " + yytext()); }
					case -178:
						break;
					case 181:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -179:
						break;
					case 183:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -180:
						break;
					case 185:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -181:
						break;
					case 186:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -182:
						break;
					case 187:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -183:
						break;
					case 188:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -184:
						break;
					case 189:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -185:
						break;
					case 190:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -186:
						break;
					case 191:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -187:
						break;
					case 192:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -188:
						break;
					case 193:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -189:
						break;
					case 194:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -190:
						break;
					case 195:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -191:
						break;
					case 196:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -192:
						break;
					case 197:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -193:
						break;
					case 198:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -194:
						break;
					case 199:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -195:
						break;
					case 200:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -196:
						break;
					case 201:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -197:
						break;
					case 202:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -198:
						break;
					case 203:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -199:
						break;
					case 204:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -200:
						break;
					case 205:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -201:
						break;
					case 206:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -202:
						break;
					case 207:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -203:
						break;
					case 208:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -204:
						break;
					case 209:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -205:
						break;
					case 210:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -206:
						break;
					case 211:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -207:
						break;
					case 212:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -208:
						break;
					case 213:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -209:
						break;
					case 214:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -210:
						break;
					case 215:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -211:
						break;
					case 216:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -212:
						break;
					case 217:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -213:
						break;
					case 218:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -214:
						break;
					case 219:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -215:
						break;
					case 220:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -216:
						break;
					case 221:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -217:
						break;
					case 222:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -218:
						break;
					case 223:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -219:
						break;
					case 224:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -220:
						break;
					case 225:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -221:
						break;
					case 226:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -222:
						break;
					case 227:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -223:
						break;
					case 228:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -224:
						break;
					case 229:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -225:
						break;
					case 230:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -226:
						break;
					case 231:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -227:
						break;
					case 232:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -228:
						break;
					case 233:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -229:
						break;
					case 234:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -230:
						break;
					case 235:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -231:
						break;
					case 236:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -232:
						break;
					case 237:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -233:
						break;
					case 238:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -234:
						break;
					case 239:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -235:
						break;
					case 240:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -236:
						break;
					case 241:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -237:
						break;
					case 242:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -238:
						break;
					case 243:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -239:
						break;
					case 244:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -240:
						break;
					case 245:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -241:
						break;
					case 246:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -242:
						break;
					case 247:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -243:
						break;
					case 248:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -244:
						break;
					case 249:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -245:
						break;
					case 250:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -246:
						break;
					case 251:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -247:
						break;
					case 252:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -248:
						break;
					case 253:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -249:
						break;
					case 254:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -250:
						break;
					case 255:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -251:
						break;
					case 256:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -252:
						break;
					case 257:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -253:
						break;
					case 258:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -254:
						break;
					case 259:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -255:
						break;
					case 260:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -256:
						break;
					case 261:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -257:
						break;
					case 262:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -258:
						break;
					case 263:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -259:
						break;
					case 264:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -260:
						break;
					case 265:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -261:
						break;
					case 266:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -262:
						break;
					case 267:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -263:
						break;
					case 268:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -264:
						break;
					case 269:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -265:
						break;
					case 270:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -266:
						break;
					case 271:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -267:
						break;
					case 272:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -268:
						break;
					case 273:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -269:
						break;
					case 274:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -270:
						break;
					case 275:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -271:
						break;
					case 276:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -272:
						break;
					case 277:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -273:
						break;
					case 278:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -274:
						break;
					case 279:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -275:
						break;
					case 280:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -276:
						break;
					case 281:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -277:
						break;
					case 282:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -278:
						break;
					case 283:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -279:
						break;
					case 284:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -280:
						break;
					case 285:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -281:
						break;
					case 286:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -282:
						break;
					case 287:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -283:
						break;
					case 288:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -284:
						break;
					case 289:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -285:
						break;
					case 290:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -286:
						break;
					case 291:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -287:
						break;
					case 292:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -288:
						break;
					case 293:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -289:
						break;
					case 294:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -290:
						break;
					case 295:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -291:
						break;
					case 296:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -292:
						break;
					case 297:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -293:
						break;
					case 298:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -294:
						break;
					case 299:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -295:
						break;
					case 300:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -296:
						break;
					case 301:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -297:
						break;
					case 302:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -298:
						break;
					case 303:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -299:
						break;
					case 304:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -300:
						break;
					case 305:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -301:
						break;
					case 306:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -302:
						break;
					case 307:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -303:
						break;
					case 308:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -304:
						break;
					case 309:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -305:
						break;
					case 310:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -306:
						break;
					case 311:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -307:
						break;
					case 312:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -308:
						break;
					case 313:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -309:
						break;
					case 314:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -310:
						break;
					case 315:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -311:
						break;
					case 316:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -312:
						break;
					case 317:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -313:
						break;
					case 318:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -314:
						break;
					case 319:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -315:
						break;
					case 320:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -316:
						break;
					case 321:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -317:
						break;
					case 322:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -318:
						break;
					case 323:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -319:
						break;
					case 324:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -320:
						break;
					case 325:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -321:
						break;
					case 326:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -322:
						break;
					case 327:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -323:
						break;
					case 328:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -324:
						break;
					case 329:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -325:
						break;
					case 330:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -326:
						break;
					case 331:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -327:
						break;
					case 332:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -328:
						break;
					case 333:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -329:
						break;
					case 334:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -330:
						break;
					case 335:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -331:
						break;
					case 336:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -332:
						break;
					case 337:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -333:
						break;
					case 338:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -334:
						break;
					case 339:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -335:
						break;
					case 340:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -336:
						break;
					case 341:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -337:
						break;
					case 342:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -338:
						break;
					case 343:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -339:
						break;
					case 344:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -340:
						break;
					case 345:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -341:
						break;
					case 346:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -342:
						break;
					case 347:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -343:
						break;
					case 348:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -344:
						break;
					case 349:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -345:
						break;
					case 350:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -346:
						break;
					case 351:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -347:
						break;
					case 352:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -348:
						break;
					case 353:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -349:
						break;
					case 354:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -350:
						break;
					case 355:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -351:
						break;
					case 356:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -352:
						break;
					case 357:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -353:
						break;
					case 358:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -354:
						break;
					case 359:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -355:
						break;
					case 360:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -356:
						break;
					case 361:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -357:
						break;
					case 362:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -358:
						break;
					case 363:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -359:
						break;
					case 364:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -360:
						break;
					case 365:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -361:
						break;
					case 366:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -362:
						break;
					case 367:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -363:
						break;
					case 368:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -364:
						break;
					case 369:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -365:
						break;
					case 370:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -366:
						break;
					case 371:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -367:
						break;
					case 372:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -368:
						break;
					case 373:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -369:
						break;
					case 374:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -370:
						break;
					case 375:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -371:
						break;
					case 376:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -372:
						break;
					case 377:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -373:
						break;
					case 378:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -374:
						break;
					case 379:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -375:
						break;
					case 380:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -376:
						break;
					case 381:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -377:
						break;
					case 382:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -378:
						break;
					case 383:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -379:
						break;
					case 384:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -380:
						break;
					case 385:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -381:
						break;
					case 386:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -382:
						break;
					case 387:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -383:
						break;
					case 388:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -384:
						break;
					case 389:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -385:
						break;
					case 390:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -386:
						break;
					case 391:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -387:
						break;
					case 392:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -388:
						break;
					case 393:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -389:
						break;
					case 394:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -390:
						break;
					case 395:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -391:
						break;
					case 396:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -392:
						break;
					case 397:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -393:
						break;
					case 398:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -394:
						break;
					case 399:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -395:
						break;
					case 400:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -396:
						break;
					case 401:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -397:
						break;
					case 402:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -398:
						break;
					case 403:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -399:
						break;
					case 404:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -400:
						break;
					case 405:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -401:
						break;
					case 406:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -402:
						break;
					case 407:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -403:
						break;
					case 408:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -404:
						break;
					case 409:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -405:
						break;
					case 410:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -406:
						break;
					case 411:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -407:
						break;
					case 412:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -408:
						break;
					case 413:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -409:
						break;
					case 414:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -410:
						break;
					case 415:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -411:
						break;
					case 416:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -412:
						break;
					case 417:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -413:
						break;
					case 418:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -414:
						break;
					case 419:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -415:
						break;
					case 420:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -416:
						break;
					case 421:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -417:
						break;
					case 422:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -418:
						break;
					case 423:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -419:
						break;
					case 424:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -420:
						break;
					case 425:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -421:
						break;
					case 426:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -422:
						break;
					case 427:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -423:
						break;
					case 428:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -424:
						break;
					case 429:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -425:
						break;
					case 430:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -426:
						break;
					case 431:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -427:
						break;
					case 432:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -428:
						break;
					case 433:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -429:
						break;
					case 434:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -430:
						break;
					case 435:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -431:
						break;
					case 436:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -432:
						break;
					case 437:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -433:
						break;
					case 438:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -434:
						break;
					case 439:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -435:
						break;
					case 440:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -436:
						break;
					case 441:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -437:
						break;
					case 442:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -438:
						break;
					case 443:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -439:
						break;
					case 444:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -440:
						break;
					case 445:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -441:
						break;
					case 446:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -442:
						break;
					case 447:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -443:
						break;
					case 448:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -444:
						break;
					case 449:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -445:
						break;
					case 450:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -446:
						break;
					case 451:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -447:
						break;
					case 452:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -448:
						break;
					case 453:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -449:
						break;
					case 454:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -450:
						break;
					case 455:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -451:
						break;
					case 456:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -452:
						break;
					case 457:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -453:
						break;
					case 458:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -454:
						break;
					case 459:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -455:
						break;
					case 460:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -456:
						break;
					case 461:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -457:
						break;
					case 462:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -458:
						break;
					case 463:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -459:
						break;
					case 464:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -460:
						break;
					case 465:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -461:
						break;
					case 466:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -462:
						break;
					case 467:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -463:
						break;
					case 468:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -464:
						break;
					case 469:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -465:
						break;
					case 470:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -466:
						break;
					case 471:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -467:
						break;
					case 472:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -468:
						break;
					case 473:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -469:
						break;
					case 474:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -470:
						break;
					case 475:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -471:
						break;
					case 476:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -472:
						break;
					case 477:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -473:
						break;
					case 478:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -474:
						break;
					case 479:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -475:
						break;
					case 480:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -476:
						break;
					case 481:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -477:
						break;
					case 482:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -478:
						break;
					case 483:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -479:
						break;
					case 484:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -480:
						break;
					case 485:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -481:
						break;
					case 486:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -482:
						break;
					case 487:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -483:
						break;
					case 488:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -484:
						break;
					case 489:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -485:
						break;
					case 490:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -486:
						break;
					case 491:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -487:
						break;
					case 492:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -488:
						break;
					case 493:
						{
    //System.out.println("DOUBLE " + yytext());
    		    return new Symbol(sym.DOUBLE, new Double(yytext()));
		}
					case -489:
						break;
					case 494:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -490:
						break;
					case 495:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -491:
						break;
					case 496:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -492:
						break;
					case 497:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -493:
						break;
					case 498:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -494:
						break;
					case 499:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -495:
						break;
					case 500:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -496:
						break;
					case 501:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -497:
						break;
					case 502:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -498:
						break;
					case 503:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -499:
						break;
					case 504:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -500:
						break;
					case 505:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -501:
						break;
					case 506:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -502:
						break;
					case 507:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -503:
						break;
					case 508:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -504:
						break;
					case 509:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -505:
						break;
					case 510:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -506:
						break;
					case 511:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -507:
						break;
					case 512:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -508:
						break;
					case 513:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -509:
						break;
					case 514:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -510:
						break;
					case 515:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -511:
						break;
					case 516:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -512:
						break;
					case 517:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -513:
						break;
					case 518:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -514:
						break;
					case 519:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -515:
						break;
					case 520:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -516:
						break;
					case 521:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -517:
						break;
					case 522:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -518:
						break;
					case 523:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -519:
						break;
					case 524:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -520:
						break;
					case 525:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -521:
						break;
					case 526:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -522:
						break;
					case 527:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -523:
						break;
					case 528:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -524:
						break;
					case 529:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -525:
						break;
					case 530:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -526:
						break;
					case 531:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -527:
						break;
					case 532:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -528:
						break;
					case 533:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -529:
						break;
					case 534:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -530:
						break;
					case 535:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -531:
						break;
					case 536:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -532:
						break;
					case 537:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -533:
						break;
					case 538:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -534:
						break;
					case 539:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -535:
						break;
					case 540:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -536:
						break;
					case 541:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -537:
						break;
					case 542:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -538:
						break;
					case 543:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -539:
						break;
					case 544:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -540:
						break;
					case 545:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -541:
						break;
					case 546:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -542:
						break;
					case 547:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -543:
						break;
					case 548:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -544:
						break;
					case 549:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -545:
						break;
					case 550:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -546:
						break;
					case 551:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -547:
						break;
					case 552:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -548:
						break;
					case 553:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -549:
						break;
					case 554:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -550:
						break;
					case 555:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -551:
						break;
					case 556:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -552:
						break;
					case 557:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -553:
						break;
					case 558:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -554:
						break;
					case 559:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -555:
						break;
					case 560:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -556:
						break;
					case 561:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -557:
						break;
					case 562:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -558:
						break;
					case 563:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -559:
						break;
					case 564:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -560:
						break;
					case 565:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -561:
						break;
					case 566:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -562:
						break;
					case 567:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -563:
						break;
					case 568:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -564:
						break;
					case 569:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -565:
						break;
					case 570:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -566:
						break;
					case 571:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -567:
						break;
					case 572:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -568:
						break;
					case 573:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -569:
						break;
					case 574:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -570:
						break;
					case 575:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -571:
						break;
					case 576:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -572:
						break;
					case 577:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -573:
						break;
					case 578:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -574:
						break;
					case 579:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -575:
						break;
					case 580:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -576:
						break;
					case 581:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -577:
						break;
					case 582:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -578:
						break;
					case 583:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -579:
						break;
					case 584:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -580:
						break;
					case 585:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -581:
						break;
					case 586:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -582:
						break;
					case 587:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -583:
						break;
					case 588:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -584:
						break;
					case 589:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -585:
						break;
					case 590:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -586:
						break;
					case 591:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -587:
						break;
					case 592:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -588:
						break;
					case 593:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -589:
						break;
					case 594:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -590:
						break;
					case 595:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -591:
						break;
					case 596:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -592:
						break;
					case 597:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -593:
						break;
					case 598:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -594:
						break;
					case 599:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -595:
						break;
					case 600:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -596:
						break;
					case 601:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -597:
						break;
					case 602:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -598:
						break;
					case 603:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -599:
						break;
					case 604:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -600:
						break;
					case 605:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -601:
						break;
					case 606:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -602:
						break;
					case 607:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -603:
						break;
					case 608:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -604:
						break;
					case 609:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -605:
						break;
					case 610:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -606:
						break;
					case 611:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -607:
						break;
					case 612:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -608:
						break;
					case 613:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -609:
						break;
					case 614:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -610:
						break;
					case 615:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -611:
						break;
					case 616:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -612:
						break;
					case 617:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -613:
						break;
					case 618:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -614:
						break;
					case 619:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -615:
						break;
					case 620:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -616:
						break;
					case 621:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -617:
						break;
					case 622:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -618:
						break;
					case 623:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -619:
						break;
					case 624:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -620:
						break;
					case 625:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -621:
						break;
					case 626:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -622:
						break;
					case 627:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -623:
						break;
					case 628:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -624:
						break;
					case 629:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -625:
						break;
					case 630:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -626:
						break;
					case 631:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -627:
						break;
					case 632:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -628:
						break;
					case 633:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -629:
						break;
					case 634:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -630:
						break;
					case 635:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -631:
						break;
					case 636:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -632:
						break;
					case 637:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -633:
						break;
					case 638:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -634:
						break;
					case 639:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -635:
						break;
					case 640:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -636:
						break;
					case 641:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -637:
						break;
					case 642:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -638:
						break;
					case 643:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -639:
						break;
					case 644:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -640:
						break;
					case 645:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -641:
						break;
					case 646:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -642:
						break;
					case 647:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -643:
						break;
					case 648:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -644:
						break;
					case 649:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -645:
						break;
					case 650:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -646:
						break;
					case 651:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -647:
						break;
					case 652:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -648:
						break;
					case 653:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -649:
						break;
					case 654:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -650:
						break;
					case 655:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -651:
						break;
					case 656:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -652:
						break;
					case 657:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -653:
						break;
					case 658:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -654:
						break;
					case 659:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -655:
						break;
					case 660:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -656:
						break;
					case 661:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -657:
						break;
					case 662:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -658:
						break;
					case 663:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -659:
						break;
					case 664:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -660:
						break;
					case 665:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -661:
						break;
					case 666:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -662:
						break;
					case 667:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -663:
						break;
					case 668:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -664:
						break;
					case 669:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -665:
						break;
					case 670:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -666:
						break;
					case 671:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -667:
						break;
					case 672:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -668:
						break;
					case 673:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -669:
						break;
					case 674:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -670:
						break;
					case 675:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -671:
						break;
					case 676:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -672:
						break;
					case 677:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -673:
						break;
					case 678:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -674:
						break;
					case 679:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -675:
						break;
					case 680:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -676:
						break;
					case 681:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -677:
						break;
					case 682:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -678:
						break;
					case 683:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -679:
						break;
					case 684:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -680:
						break;
					case 685:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -681:
						break;
					case 686:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -682:
						break;
					case 687:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -683:
						break;
					case 688:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -684:
						break;
					case 689:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -685:
						break;
					case 690:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -686:
						break;
					case 691:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -687:
						break;
					case 692:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -688:
						break;
					case 693:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -689:
						break;
					case 694:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -690:
						break;
					case 695:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -691:
						break;
					case 696:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -692:
						break;
					case 697:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -693:
						break;
					case 698:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -694:
						break;
					case 699:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -695:
						break;
					case 700:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -696:
						break;
					case 701:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -697:
						break;
					case 702:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -698:
						break;
					case 703:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -699:
						break;
					case 704:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -700:
						break;
					case 705:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -701:
						break;
					case 706:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -702:
						break;
					case 707:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -703:
						break;
					case 708:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -704:
						break;
					case 709:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -705:
						break;
					case 710:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -706:
						break;
					case 711:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -707:
						break;
					case 712:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -708:
						break;
					case 713:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -709:
						break;
					case 714:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -710:
						break;
					case 715:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -711:
						break;
					case 716:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -712:
						break;
					case 717:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -713:
						break;
					case 718:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -714:
						break;
					case 719:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -715:
						break;
					case 720:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -716:
						break;
					case 721:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -717:
						break;
					case 722:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -718:
						break;
					case 723:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -719:
						break;
					case 724:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -720:
						break;
					case 725:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -721:
						break;
					case 726:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -722:
						break;
					case 727:
						{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}
					case -723:
						break;
					default:
						yy_error(YY_E_INTERNAL,false);
					case -1:
					}
					yy_initial = true;
					yy_state = yy_state_dtrans[yy_lexical_state];
					yy_next_state = YY_NO_STATE;
					yy_last_accept_state = YY_NO_STATE;
					yy_mark_start();
					yy_this_accept = yy_acpt[yy_state];
					if (YY_NOT_ACCEPT != yy_this_accept) {
						yy_last_accept_state = yy_state;
						yy_mark_end();
					}
				}
			}
		}
	}
}
