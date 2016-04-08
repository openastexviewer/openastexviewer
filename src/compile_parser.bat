rem Compilation batch file for parser for windows
rem
rem Resources that OpenAstexViewer depends on are assumed to be unpacked
rem in the directory ..\lib
rem
rem
rem The parser is not compiled by default. Uncomment the relevant sections
rem to compile the parser if you make any changes to that. It is recommended
rem that new scripting commands are added through the extensible command 
rem interface rather than through changes to the parser.

set CLASSPATH=.;..\lib

set JAVA=c:\j2sdk1.4.2_16\bin

%JAVA%\javac -J-mx100m -target 1.1 java_cup\*.java java_cup\runtime\*.java JLex\*.java

%JAVA%\java -server -Xmx100000000 java_cup.Main < astex\parser\parser.cup
%JAVA%\java JLex.Main astex\parser\parser.lex
move/y astex\parser\parser.lex.java astex\parser\Yylex.java
move/y parser.java astex\parser\parser.java
move/y sym.java astex\parser\sym.java
