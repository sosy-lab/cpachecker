#!/bin/sh

##############################################################################
# Adjust the values of these environment variables appropriately
##############################################################################

JAVA_DIR=/usr/lib/jvm/java-6-openjdk
GMP_INCLUDE_DIR=/usr/include
GMP_LIB_DIR=/usr/lib

##############################################################################
# From here on you should not need to change anything
##############################################################################

if [ ! -f "$1/lib/libmathsat.a" ]; then
	echo "You need to specify the directory with the downloaded Mathsat on the command line!"
	exit 1
fi
MSAT_SRC_DIR="$1"/include
MSAT_LIB_DIR="$1"/lib

if [ ! -e "$JAVA_DIR" ]; then
	echo "You do not have a JDK installed in $JAVA_DIR"
	echo "Please adjust the variable in this script."
	exit 1
fi

echo "Compiling the C wrapper code and creating the \"mathsatj\" library (log in \"compile.log\")"
gcc -g -I$JAVA_DIR/include -I$JAVA_DIR/include/linux -I$MSAT_SRC_DIR -I$GMP_INCLUDE_DIR mathsat_api.c -fPIC -c
gcc -g -o libmathsatj.so -shared -Wl,-soname,libmathsatj.so -L$MSAT_LIB_DIR -L$GMP_LIB_DIR mathsat_api.o -lc -lstdc++ -lmathsat -lgmpxx -lgmp > compile.log 2>&1

if [ $? -eq 0 ]; then
	strip libmathsatj.so
	echo "Generating \"mathsat.jar\""
	$JAVA_DIR/bin/javac mathsat/*.java
	find mathsat -name "*.class" -o -name "*.java" | xargs $JAVA_DIR/bin/jar cf mathsat.jar
else
	echo "There was a problem during compilation of \"mathsat_api.c\""
	cat compile.log
	exit 1
fi
echo "All Done"
