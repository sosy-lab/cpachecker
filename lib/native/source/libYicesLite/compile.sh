#!/bin/sh

##############################################################################
# Adjust the values of these environment variables appropriately
##############################################################################

JAVA_DIR=/usr/lib/jvm/java-6-openjdk
GMP_LIB_DIR=/usr/lib

##############################################################################
# From here on you should not need to change anything
##############################################################################

if [ ! -f "$1/lib/libyices.so" ]; then
	echo "You need to specify the directory with the downloaded Yices on the command line!"
#	exit 1
fi
YICES_SRC_DIR="$1"/include
YICES_LIB_DIR="$1"/lib

if [ ! -e "$JAVA_DIR" ]; then
	echo "You do not have a JDK installed in $JAVA_DIR"
	echo "Please adjust the variable in this script."
	exit 1
fi

echo "Compiling the C wrapper code and creating the \"YicesLite\" library (log in \"compile.log\")"
gcc -Wall -I$JAVA_DIR/include -I$JAVA_DIR/include/linux -I$YICES_SRC_DIR YicesLite.c -c > compile.log 2>&1
gcc -g -o libYicesLite.so -shared -Wall -Wl,-soname,libYicesLite.so -L$YICES_LIB_DIR -L$GMP_LIB_DIR YicesLite.o -lc -lgmp -lyices >> compile.log 2>&1

if [ $? -eq 0 ]; then
	strip libYicesLite.so
else
	echo "There was a problem during compilation of \"YicesLite.c\""
	cat compile.log
	exit 1
fi

MISSING_SYMBOLS="$(readelf -Ws libYicesLite.so | grep NOTYPE | grep GLOBAL | grep UND)"
if [ ! -z "$MISSING_SYMBOLS" ]; then
	echo "Warning: There are the following unresolved dependencies in libmathsatj.so:"
	readelf -Ws libYicesLite.so | grep NOTYPE | grep GLOBAL | grep UND
	exit 1
fi

echo "All Done"
