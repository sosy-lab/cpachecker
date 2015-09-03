#!/bin/sh

# This script builds either libmathsat5j.so (bindings to mathsat5), or
# liboptimathsat5j.so (bindings to optimathsat5).
# In future, mathsat5 and optimathsat should merge, making the switching
# obsolete.

# For building libmathsat5j.so, there are two dependencies:
# - The static Mathsat5 library as can be downloaded from http://mathsat.fbk.eu/download.html
# - The static GMP library compiled with the "-fPIC" option.
#   To create this, download GMP from http://gmplib.org/ and run
#   ./configure --enable-cxx --with-pic --disable-shared
#   make

# For building liboptimathsat5.so, OptiMathSat5 can be downloaded from
# http://optimathsat.disi.unitn.it/.

# To build mathsat bindings: ./compile.sh $MATHSAT_DIR $GMP_DIR
# To build optimathsat bindings: ./compile.sh $MATHSAT_DIR $GMP_DIR -opt

# This script searches for all included libraries in the current directory first.
# You can use this to override specific libraries installed on your system.
# You can also use this to force static linking of a specific library,
# if you put only the corresponding .a file in this directory, not the .so file.

# For example, to statically link against libstdc++,
# compile this library with --with-pic,
# and put the resulting libstdc++.a file in this directory.

# This script uses a crude hack to produce downwards-compatible binaries.
# On systems with libc6 >= 2.14, there is a new memcpy function.
# Binaries which link against this function do not work on older systems (e.g., Ubuntu before 12.04)
# We force the linker to use memcpy from libc6 2.2.5 with the following trick:
# 1) Define a wrapper function which just calls memcpy in versions.c.
# 2) Also in versions.c, set the version of memcpy to GLIBC_2.2.5.
# 3) Tell the linker that it should wrap all calls to memcpy with the wrapper function.
# This will need to be extended if there appear other functions in newer a libc
# which are also not downwards compatible.
# Always check with ldd -v what the newest required version of libc and libstdc++ are.

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

SRC_FILES="org_sosy_1lab_cpachecker_util_predicates_mathsat5_Mathsat5NativeApi.c versions.c"
OBJ_FILES="org_sosy_1lab_cpachecker_util_predicates_mathsat5_Mathsat5NativeApi.o"

if [ "$3" = "-opt" ]; then
    echo "Compiling bindings to OptiMathSAT"
    SRC_FILES="$SRC_FILES optimization.c"
    OBJ_FILES="$OBJ_FILES optimization.o"
    OUT_FILE="liboptimathsat5j.so"
else
    OUT_FILE="libmathsat5j.so"
fi

echo "Compiling the C wrapper code and creating the \"$OUT_FILE\" library"

# This will compile the JNI wrapper part, given the JNI and the Mathsat header files
gcc -g $JNI_HEADERS -I$MSAT_SRC_DIR -I$GMP_HEADER_DIR $SRC_FILES -fPIC -c

# This will link together the file produced above, the Mathsat library, the GMP library and the standard libraries.
# Everything except the standard libraries is included statically.
# The result is a shared library.
if [ `uname -m` = "x86_64" ]; then
	gcc -Wall -g -o $OUT_FILE -shared -Wl,-soname,libmathsat5j.so -L. -L$MSAT_LIB_DIR -L$GMP_LIB_DIR -I$GMP_HEADER_DIR versions.o $OBJ_FILES -Wl,-Bstatic -lmathsat -lgmpxx -lgmp -Wl,-Bdynamic -lc -lm -lstdc++ -Wl,--wrap=memcpy
else
	gcc -Wall -g -o $OUT_FILE -shared -Wl,-soname,libmathsat5j.so -L$MSAT_LIB_DIR -L$GMP_LIB_DIR -I$GMP_HEADER_DIR $OBJ_FILES -Wl,-Bstatic -lmathsat -lgmpxx -lgmp -Wl,-Bdynamic -lc -lm -lstdc++
fi


if [ $? -eq 0 ]; then
	strip $OUT_FILE
else
	echo "There was a problem during compilation of \"org_sosy_1lab_cpachecker_util_predicates_mathsat5_Mathsat5NativeApi.c\""
	exit 1
fi

MISSING_SYMBOLS="$(readelf -Ws $OUT_FILE | grep NOTYPE | grep GLOBAL | grep UND)"
if [ ! -z "$MISSING_SYMBOLS" ]; then
	echo "Warning: There are the following unresolved dependencies in libmathsat5j.so:"
	readelf -Ws $OBJ_FILES | grep NOTYPE | grep GLOBAL | grep UND
	exit 1
fi

echo "All Done"
echo "Please check in the following output that the library does not depend on any GLIBC version >= 2.11, otherwise it will not work on Ubuntu 10.04:"
LANG=C objdump -p $OUT_FILE |grep -A50 "required from"
