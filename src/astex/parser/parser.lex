package astex.parser;

import java_cup.runtime.Symbol;
import astex.*;
%%
%cup
%public
%eofval{
    return new Symbol(sym.EOF);
%eofval}
%%
"-"		{ return new Symbol(sym.DASH); }
";"		{ return new Symbol(sym.SEMI); }
":"		{ return new Symbol(sym.COLON); }
"("		{ return new Symbol(sym.LPAREN); }
")"		{ return new Symbol(sym.RPAREN); }
"{"		{ return new Symbol(sym.LCURLY); }
"}"		{ return new Symbol(sym.RCURLY); }
"$"		{ return new Symbol(sym.DOLLAR); }

">"		{ return new Symbol(sym.OPERATOR, new Integer(Selection.GT)); }
">="		{ return new Symbol(sym.OPERATOR, new Integer(Selection.GE)); }
"<"		{ return new Symbol(sym.OPERATOR, new Integer(Selection.LT)); }
"<="		{ return new Symbol(sym.OPERATOR, new Integer(Selection.LE)); }
"="		{ return new Symbol(sym.OPERATOR, new Integer(Selection.EQ)); }
"!="		{ return new Symbol(sym.OPERATOR, new Integer(Selection.NE)); }

"and"		{ return new Symbol(sym.AND); }
"or"		{ return new Symbol(sym.OR); }
"not"		{ return new Symbol(sym.NOT); }

"x"		{ return new Symbol(sym.ATTRIBUTE, new Integer(Atom.X)); }
"y"		{ return new Symbol(sym.ATTRIBUTE, new Integer(Atom.Y)); }
"z"		{ return new Symbol(sym.ATTRIBUTE, new Integer(Atom.Z)); }
"b"		{ return new Symbol(sym.ATTRIBUTE, new Integer(Atom.B)); }
"o"		{ return new Symbol(sym.ATTRIBUTE, new Integer(Atom.O)); }

"active"	{ return new Symbol(sym.ACTIVE); }
"active_site"	{ return new Symbol(sym.ACTIVE_SITE); }
"all"		{ return new Symbol(sym.ALL); }
"aminoacid"	{ return new Symbol(sym.AMINOACID); }
"anasurface"	{ return new Symbol(sym.ANASURFACE); }
"animate"	{ return new Symbol(sym.ANIMATE); }
"append"	{ return new Symbol(sym.APPEND); }
"angle"		{ return new Symbol(sym.ANGLE); }
"around"	{ return new Symbol(sym.AROUND); }
"atom"		{ return new Symbol(sym.ATOM); }
"backface"	{ return new Symbol(sym.BACKFACE); }
"background"	{ return new Symbol(sym.BACKGROUND); }
"ball_radius"	{ return new Symbol(sym.BALL_RADIUS); }
"bond_radius"	{ return new Symbol(sym.BOND_WIDTH); }
"bond_width"	{ return new Symbol(sym.BOND_WIDTH); }
"bonded"	{ return new Symbol(sym.BONDED); }
"byresidue"	{ return new Symbol(sym.BYRESIDUE); }
"center"	{ return new Symbol(sym.CENTER); }
"centre"	{ return new Symbol(sym.CENTER); }
"chain"		{ return new Symbol(sym.CHAIN); }
"charge"	{ return new Symbol(sym.CHARGE); }
"clear"		{ return new Symbol(sym.CLEAR); }
"clip"		{ return new Symbol(sym.CLIP); }
"color"		{ return new Symbol(sym.COLOR); }
"color_by_atom"	{ return new Symbol(sym.COLOR_BY_ATOM); }
"color_by_chain"	{ return new Symbol(sym.COLOR_BY_CHAIN); }
"color_by_bvalue"	{ return new Symbol(sym.COLOR_BY_BVALUE); }
"color_by_rainbow"	{ return new Symbol(sym.COLOR_BY_RAINBOW); }
"color_by_energy"	{ return new Symbol(sym.COLOR_BY_ENERGY); }
"color_by_bvalue_range" { return new Symbol(sym.COLOR_BY_BVALUE_RANGE); }
"colour_by_atom"	{ return new Symbol(sym.COLOR_BY_ATOM); }
"colour_by_chain"	{ return new Symbol(sym.COLOR_BY_CHAIN); }
"colour_by_bvalue"	{ return new Symbol(sym.COLOR_BY_BVALUE); }
"colour_by_rainbow"	{ return new Symbol(sym.COLOR_BY_RAINBOW); }
"colour_by_bvalue_range" { return new Symbol(sym.COLOR_BY_BVALUE_RANGE); }
"colour"	{ return new Symbol(sym.COLOR); }
"composite"	{ return new Symbol(sym.COMPOSITE); }
"contact"	{ return new Symbol(sym.CONTACT); }
"contour"	{ return new Symbol(sym.CONTOUR); }
"context"	{ return new Symbol(sym.CONTEXT); }
"copyto"	{ return new Symbol(sym.COPYTO); }
"current"	{ return new Symbol(sym.CURRENT); }
"curvature"	{ return new Symbol(sym.CURVATURE); }
"cylinders"	{ return new Symbol(sym.CYLINDERS); }
"cylinder_radius"	{ return new Symbol(sym.CYLINDER_RADIUS); }
"decrease"	{ return new Symbol(sym.DECREASE); }
"default"	{ return new Symbol(sym.DEFAULT); }
"define"	{ return new Symbol(sym.DEFINE); }
"delete"	{ return new Symbol(sym.DELETE); }
"distance"	{ return new Symbol(sym.DISTANCE); }
"display"	{ return new Symbol(sym.DISPLAY); }
"displayed"	{ return new Symbol(sym.DISPLAYED); }
"dna"		{ return new Symbol(sym.DNA); }
"dotsurface"	{ return new Symbol(sym.DOTSURFACE); }
"edit"		{ return new Symbol(sym.EDIT); }
"electrostatic"	{ return new Symbol(sym.ELECTROSTATIC); }
"element"	{ return new Symbol(sym.ELEMENT); }
"environment"	{ return new Symbol(sym.ENVIRONMENT); }
"evaluate"	{ return new Symbol(sym.EVALUATE); }
"exclude"	{ return new Symbol(sym.EXCLUDE); }
"group"		{ return new Symbol(sym.GROUP); }
"id"		{ return new Symbol(sym.ID); }
"increase"	{ return new Symbol(sym.INCREASE); }
"insertion"	{ return new Symbol(sym.INSERTION); }
"install"	{ return new Symbol(sym.INSTALL); }
"invert"	{ return new Symbol(sym.INVERT); }
"ions"		{ return new Symbol(sym.IONS); }
"false"		{ return new Symbol(sym.FALSE); }
"fetch"		{ return new Symbol(sym.FETCH); }
"fixed"		{ return new Symbol(sym.FIXED); }
"forcefield"	{ return new Symbol(sym.FORCEFIELD); }
"graph"		{ return new Symbol(sym.GRAPH); }
"hbond"		{ return new Symbol(sym.HBOND); }
"label"		{ return new Symbol(sym.LABEL); }
"labelled"	{ return new Symbol(sym.LABELLED); }
"lazy"		{ return new Symbol(sym.LAZY); }
"light"		{ return new Symbol(sym.LIGHT); }
"lines"		{ return new Symbol(sym.LINES); }
"linewidth"	{ return new Symbol(sym.LINEWIDTH); }
"lipophilicity"	{ return new Symbol(sym.LIPOPHILICITY); }
"load"		{ return new Symbol(sym.LOAD); }
"modelling"	{ return new Symbol(sym.MODELLING); }
"molecule"	{ return new Symbol(sym.MOLECULE); }
"molexact"	{ return new Symbol(sym.MOLEXACT); }
"map"		{ return new Symbol(sym.MAP); }
"matrix"	{ return new Symbol(sym.MATRIX); }
"modulo"	{ return new Symbol(sym.MODULO); }
"name"		{ return new Symbol(sym.NAME); }
"none"		{ return new Symbol(sym.NONE); }
"object"	{ return new Symbol(sym.OBJECT); }
"off"		{ return new Symbol(sym.OFF); }
"on"		{ return new Symbol(sym.ON); }
"peek"		{ return new Symbol(sym.PEEK); }
"pop"		{ return new Symbol(sym.POP); }
"print"		{ return new Symbol(sym.PRINT); }
"property"	{ return new Symbol(sym.PROPERTY); }
"push"		{ return new Symbol(sym.PUSH); }
"radius"	{ return new Symbol(sym.RADIUS); }
"range"		{ return new Symbol(sym.RANGE); }
"rectangular"	{ return new Symbol(sym.RECTANGULAR); }
"repaint"	{ return new Symbol(sym.REPAINT); }
"residue"	{ return new Symbol(sym.RESIDUE); }
"remove"	{ return new Symbol(sym.REMOVE); }
"run"		{ return new Symbol(sym.RUN); }
"schematic"	{ return new Symbol(sym.SCHEMATIC); }
"secstruc"	{ return new Symbol(sym.SECSTRUC); }
"select"	{ return new Symbol(sym.SELECT); }
"sequential"	{ return new Symbol(sym.SEQUENTIAL); }
"set"		{ return new Symbol(sym.SET); }
"simple"	{ return new Symbol(sym.SIMPLE); }
"skeleton"	{ return new Symbol(sym.SKELETON); }
"slide"		{ return new Symbol(sym.SLIDE); }
"solid"		{ return new Symbol(sym.SOLID); }
"solvent"	{ return new Symbol(sym.SOLVENT); }
"sphere"	{ return new Symbol(sym.SPHERE); }
"spheres"	{ return new Symbol(sym.SPHERES); }
"surface"	{ return new Symbol(sym.SURFACE); }
"sticks"	{ return new Symbol(sym.STICKS); }
"stick_color"	{ return new Symbol(sym.STICK_COLOR); }
"stick_colour"	{ return new Symbol(sym.STICK_COLOR); }
"stick_radius"	{ return new Symbol(sym.STICK_RADIUS); }
"stick_width"	{ return new Symbol(sym.STICK_RADIUS); }
"texture"	{ return new Symbol(sym.TEXTURE); }
"to"		{ return new Symbol(sym.TO); }
"toggle"	{ return new Symbol(sym.TOGGLE); }
"torsion"	{ return new Symbol(sym.TORSION); }
"transparency"	{ return new Symbol(sym.TRANSPARENCY); }
"true"		{ return new Symbol(sym.TRUE); }
"undefine"	{ return new Symbol(sym.UNDEFINE); }
"update"	{ return new Symbol(sym.UPDATE); }
"vdw"		{ return new Symbol(sym.VDW); }
"view"		{ return new Symbol(sym.VIEW); }
"wide"		{ return new Symbol(sym.WIDE); }
"xray"		{ return new Symbol(sym.XRAY); }
"write"		{ return new Symbol(sym.WRITE); }
"zap"		{ return new Symbol(sym.ZAP); }

-?[0-9]+ 	{
    //System.out.println("INTEGER " + yytext());
    		    return new Symbol(sym.INTEGER, new Integer(yytext()));
		}

-?[-0-9.eE]+ 	{
    //System.out.println("DOUBLE " + yytext());
    		    return new Symbol(sym.DOUBLE, new Double(yytext()));
		}

[^$ \}\{)(\t\r\n\f;:\'\",-]+
		{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.STRING, new String(yytext()));
		}

-[a-zA-Z][a-zA-Z0-9_]*
		{
		    //System.out.println("STRING [" + yytext() +"]");
		    return new Symbol(sym.ARG, new String(yytext()));
		}

[\'][^\']*[\']	{
    //System.out.println("STRING [" + yytext() +"]");
		    int len = yytext().length();
		    String contents = yytext().substring(1, len - 1);
		    //System.out.println("STRING [" + contents + "]");
		    return new Symbol(sym.STRING,
				      new String(contents));
		}
[\"][^\"]*[\"]	{
    //System.out.println("STRING [" + yytext() +"]");
		    int len = yytext().length();
		    String contents = yytext().substring(1, len - 1);
		    //System.out.println("STRING [" + contents + "]");
		    return new Symbol(sym.STRING,
				      new String(contents));
		}
#.*$		{ /* ignore comments */ }
/\*[^/]*\*/	{ /* ignore other kind of comments. */ }
[ \t\r\n\f]	{ /* ignore white space. */ }
.		{ System.err.println("Illegal character: " + yytext()); }
