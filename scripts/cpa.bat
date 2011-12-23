@echo off

:: parse the parameters into one string and give that to the cpaChecker
:parameterparsing
if "%1"=="" goto weiter
set params=%params% %1
:: SHIFT shifts through the parameters of this batch file (%2 will be %1,...)
SHIFT
goto parameterparsing 
:weiter

:: go to dir "CPAchecker" (assuming we are in dir "CPAchecker" or in a direct subdirectory)
if NOT EXIST bin CD ..\

:: build the Classpath: bin directory, every .jar in lib and every .jar in lib/eclipse/ 
set CLASSPATH=bin
for %%i in (lib\*.jar) do call scripts\classpath.bat %%i
for %%i in (lib\eclipse\*.jar) do call scripts\classpath.bat %%i


java -Djava.library.path=/lib/native/x86-win32/ -Xmx1200m -ea cmdline.CPAMain %params%
