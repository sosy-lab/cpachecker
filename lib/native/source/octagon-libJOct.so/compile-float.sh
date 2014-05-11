#!/bin/sh

# For building libJOct.so, you need the compiled octagon library.
# To create this, download octagon library from http://www.di.ens.fr/~mine/oct/ and run
# ./configure --with-num=float --disable-debug --disable-gmp --disable-prof --disable-ocaml
# make

JNI_HEADERS="$(../get_jni_headers.sh)"

if [ ! -f "$1/clib/.libs/oct_util.o" ]; then
	echo "You need to specify the directory with the compiled octagon library on the command line!"
	exit 1
fi
OCT_SRC_DIR="$1"
OCT_LIB_DIR="$1"/clib/.libs

if [ `uname` = "Darwin" ] ; then
  LINK_OPT="-dynamiclib -o libJOct_float.jnilib"
else
  LINK_OPT="-o libJOct_float.so -shared -Wl,-soname,libJOct_float.so"
fi

echo "Compiling the C wrapper code and creating the \"libJOct_float.so\" library"

# This will compile the JNI wrapper part, given the JNI and the octagon header files
gcc -g -O2 $JNI_HEADERS -I$OCT_SRC_DIR -I$OCT_SRC_DIR/clib versions.c -DOCT_NUM_FLOAT -DOCT_PREFIX=CAT\(octfao_ org_sosy_lab_cpachecker_util_octagon_OctFloatWrapper.c -fPIC -c

# This will link together the file produced above, the octagon library, and the standard libraries.
# Everything except the standard libraries is included statically.
# The result is a shared library.
gcc -Wall -g $LINK_OPT org_sosy_lab_cpachecker_util_octagon_OctFloatWrapper.o $OCT_LIB_DIR/*.o versions.o -lc -lm -Wl,--wrap=memcpy

if [ $? -eq 0 ]; then
	strip libJOct_float.so
else
	echo "There was a problem during compilation of \"org_sosy_lab_cpachecker_util_octagon_OctFloatWrapper.c\""
	exit 1
fi

MISSING_SYMBOLS="$(readelf -Ws libJOct_float.so | grep NOTYPE | grep GLOBAL | grep UND)"
if [ ! -z "$MISSING_SYMBOLS" ]; then
	echo "Warning: There are the following unresolved dependencies in libJOct_float.so:"
	readelf -Ws libJOct_float.so | grep NOTYPE | grep GLOBAL | grep UND
	exit 1
fi

echo "All Done"
echo "Please check in the following output that the library does not depend on any GLIBC version >= 2.11, otherwise it will not work on Ubuntu 10.04:"
objdump -p libJOct_float.so |grep -A50 "required from"
