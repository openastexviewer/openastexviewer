@rem Compilation batch file for windows
@rem
@rem Resources that OpenAstexViewer depends on are assumed to be unpacked
@rem in the directory ..\lib
@rem
@rem All the resources are appended to the OpenAstexViewer jar at the end
@rem of the process
@rem
@rem OpenAstexViewer uses only java 1.1 code, and this is an important 
@rem requi@rement for the foreseeable future
@rem
@rem To compile java 1.1 bytecode the java 1.4 compiler is used in a 1.1
@rem compatibility mode
@rem
@rem You may use a newer compiler to build versions of OpenAstexViewer for
@rem newer versions of java
@rem
@rem The parser is not compiled by default. Uncomment the relevant sections
@rem to compile the parser if you make any changes to that. It is recommended
@rem that new scripting commands are added through the extensible command 
@rem interface rather than through changes to the parser.
@rem


@set CLASSPATH=.;..\lib

@set JAVA=c:\j2sdk1.4.2_16\bin
@set JAR=%JAVA%\jar

%JAVA%\javac -J-mx100m -target 1.1 astex\*.java astex\design\*.java astex\splitter\*.java xmt2\*.java thinlet\*.java astex\thinlet\*.java astex\xmt\*.java riso\numerical\*.java MoleculeViewerApplet.java

rem %JAR% c0f ..\jar\OpenAstexViewer.jar java_cup\runtime\*.class astex\generic\*.class astex\anasurface\*.class astex\parser\*.class astex\*.class astex\xmt\*.class astex\design\*.class riso\numerical\*.class *.properties images\textures\*.jpg images\*.jpg fonts\* xmt2\*.class xmt2\*.xml xmt2\*.gif thinlet\*.class astex\splitter\*.class astex\thinlet\*.class astex\thinlet\*.properties astex\thinlet\*.gif MoleculeViewerApplet.class

%JAR% c0f ..\jar\OpenAstexViewer.jar java_cup\runtime\*.class astex\generic\*.class astex\xmt\Manipulator.class astex\xmt\XMTUserInterface.class astex\anasurface\*.class astex\parser\*.class astex\*.class astex\design\*.class *.properties images\textures\*.jpg images\*.jpg fonts\* thinlet\*.class astex\splitter\*.class astex\thinlet\*.class astex\thinlet\*.properties astex\thinlet\*.gif MoleculeViewerApplet.class

%JAR% u0f ..\jar\OpenAstexViewer.jar -C ..\lib nanoxml
%JAR% u0f ..\jar\OpenAstexViewer.jar -C ..\lib jclass
%JAR% u0fm ..\jar\OpenAstexViewer.jar AstexViewer.manifest

copy ..\jar\OpenAstexViewer.jar ..\web\OpenAstexViewer.jar
