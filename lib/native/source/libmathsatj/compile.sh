#!/bin/sh

# For building libmathsatj.so, there are two dependencies:
# - The static Mathsat4 library as can be downloaded from http://mathsat4.disi.unitn.it/download.html
# - The static GMP library compiled with the "-fPIC" option.
#   To create this, download GMP from http://gmplib.org/ and run
#   ./configure --enable-cxx --with-pic --disable-shared
#   make

##############################################################################
# Adjust the values of these environment variables appropriately
##############################################################################

JAVA_DIR=/usr/lib/jvm/java-6-openjdk

##############################################################################
# From here on you should not need to change anything
##############################################################################

if [ ! -f "$1/lib/libmathsat.a" ]; then
	echo "You need to specify the directory with the downloaded Mathsat on the command line!"
	exit 1
fi
MSAT_SRC_DIR="$1"/include
MSAT_LIB_DIR="$1"/lib

if [ ! -f "$2/libgmp.a" ]; then
	echo "You need to specify the directory with the static GMP libraries on the command line!"
	exit 1
fi
GMP_LIB_DIR="$2"

if [ ! -e "$JAVA_DIR" ]; then
	echo "You do not have a JDK installed in $JAVA_DIR"
	echo "Please adjust the variable in this script."
	exit 1
fi

echo "Compiling the C wrapper code and creating the \"mathsatj\" library"

# This will compile the JNI wrapper part, given the JNI and the Mathsat header files
gcc -g -I$JAVA_DIR/include -I$JAVA_DIR/include/linux -I$MSAT_SRC_DIR org_sosy_1lab_cpachecker_util_predicates_mathsat_NativeApi.c -fPIC -c

# This will link together the file produced above, the Mathsat library, the GMP library and the standard libraries.
# Everything except the standard libraries is included statically.
# The result is a shared library.
gcc -Wall -g -o libmathsatj.so -shared -Wl,-soname,libmathsatj.so -L$MSAT_LIB_DIR -L$GMP_LIB_DIR org_sosy_1lab_cpachecker_util_predicates_mathsat_NativeApi.o -lc -lstdc++ -Wl,-Bstatic -lmathsat -lgmpxx -lgmp -Wl,-Bdynamic

if [ $? -eq 0 ]; then
	strip libmathsatj.so
else
	echo "There was a problem during compilation of \"org_sosy_1lab_cpachecker_util_predicates_mathsat_NativeApi.c\""
	exit 1
fi
echo "All Done"
