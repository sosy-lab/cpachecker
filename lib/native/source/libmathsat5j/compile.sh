#!/bin/sh

# For building libmathsatj.so, there are two dependencies:
# - The static Mathsat4 library as can be downloaded from http://mathsat4.disi.unitn.it/download.html
# - The static GMP library compiled with the "-fPIC" option.
#   To create this, download GMP from http://gmplib.org/ and run
#   ./configure --enable-cxx --with-pic --disable-shared
#   make

JNI_HEADERS="$(../get_jni_headers.sh)"

if [ ! -f "$1/lib/libmathsat.a" ]; then
	echo "You need to specify the directory with the downloaded Mathsat on the command line!"
	exit 1
fi
MSAT_SRC_DIR="$1"/include
MSAT_LIB_DIR="$1"/lib
GMP_LIB_DIR="$2"/.libs
GMP_HEADER_DIR="$2"

if [ ! -f "$GMP_LIB_DIR/libgmp.a" ]; then
	echo "You need to specify the GMP directory on the command line!"
	exit 1
fi

echo $GMP_HEADER_DIR
echo "Compiling the C wrapper code and creating the \"mathsatj\" library"

# This will compile the JNI wrapper part, given the JNI and the Mathsat header files
gcc -g $JNI_HEADERS -I$MSAT_SRC_DIR org_sosy_1lab_cpachecker_util_predicates_mathsat5_Mathsat5NativeApi.c -fPIC -c

# This will link together the file produced above, the Mathsat library, the GMP library and the standard libraries.
# Everything except the standard libraries is included statically.
# The result is a shared library.
gcc -Wall -g -o libmathsat5j.so -shared -Wl,-soname,libmathsat5j.so -L$MSAT_LIB_DIR -L$GMP_LIB_DIR -I$GMP_HEADER_DIR org_sosy_1lab_cpachecker_util_predicates_mathsat5_Mathsat5NativeApi.o -Wl,-Bstatic -lmathsat -lgmpxx -lgmp -Wl,-Bdynamic -lc -lm -lstdc++


if [ $? -eq 0 ]; then
	strip libmathsat5j.so
else
	echo "There was a problem during compilation of \"org_sosy_1lab_cpachecker_util_predicates_mathsat5_Mathsat5NativeApi.c\""
	exit 1
fi

MISSING_SYMBOLS="$(readelf -Ws libmathsatj.so | grep NOTYPE | grep GLOBAL | grep UND)"
if [ ! -z "$MISSING_SYMBOLS" ]; then
	echo "Warning: There are the following unresolved dependencies in libmathsatj.so:"
	readelf -Ws libmathsat5j.so | grep NOTYPE | grep GLOBAL | grep UND
	exit 1
fi

echo "All Done"
