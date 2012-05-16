@echo off

set OLDPATH=%PATH%
set PATH=%PATH%;lib\native\x86-win32
java -Djava.library.path=lib\native\x86-win32 -cp bin;cpachecker.jar;lib\*;lib\java\runtime\* -Xmx1200m -ea org.sosy_lab.cpachecker.cmdline.CPAMain %*
set PATH=%OLDPATH%
