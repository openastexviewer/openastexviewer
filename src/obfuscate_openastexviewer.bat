@rem Obfuscation batch file for windows


@set JAVA=c:\j2sdk1.4.2_16\bin
@set JAR=%JAVA%\jar

%JAVA%\java -Xmx512m -jar ..\..\proguard4.1\lib\proguard.jar @proguard.pro

copy ..\jar\OpenAstexViewerObf.jar ..\web\OpenAstexViewerObf.jar
