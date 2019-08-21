@echo off
java -cp bin;cpachecker.jar;lib\*;lib\java\runtime\* -Xmx1200m -ea org.sosy_lab.cpachecker.cmdline.CPAMain %*
