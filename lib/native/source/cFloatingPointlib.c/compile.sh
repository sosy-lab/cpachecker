#!/bin/sh

gcc -std='c11' -shared -fpic -o 'libFloatingPoints.so' \
 	-I"$JAVA_HOME/include/" \
	-I"$JAVA_HOME/include/linux/" \
	floatingPoints.c
