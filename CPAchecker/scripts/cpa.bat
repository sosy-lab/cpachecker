@echo off
set params=""

set OLDPATH=%PATH%
set PATH=%PATH%;lib\native\x86-windows
java -cp bin;cpachecker.jar;lib\*;lib\java\runtime\* -Xmx1200m -ea org.sosy_lab.cpachecker.cmdline.CPAMain %*
set PATH=%OLDPATH%
