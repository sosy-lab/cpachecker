#!/usr/bin/env bash
# This script searches for all included libraries in the current directory first.
# You can use this to override specific libraries installed on your system.
# You can also use this to force static linking of a specific library,
# if you put only the corresponding .a file in this directory, not the .so file.

# For example, to statically link against libstdc++,
# compile this library with --with-pic,
# and put the resulting libstdc++.a file in this directory.

# Enable error handling.
set -o nounset
set -o errexit
set -o pipefail

JNI_HEADERS="$(../get_jni_headers.sh)"

USE_FOCI="$2"
OUTFILENAME="libz3j.so"
Z3_SO_FILENAME="libz3.so"
Z3_LIBNAME="-lz3"
CURRENT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

Z3_DIR="$1"
Z3_SRC_DIR="$Z3_DIR"/src/api
Z3_LIB_DIR="$Z3_DIR"/build
if [ ! -f "$Z3_LIB_DIR/$Z3_SO_FILENAME" ]; then
	echo "You need to specify the directory with the successfully built Z3 on the command line!"
	exit 1
fi

if [ $2 = "-interp" ]; then
	echo "Assuming interpolation support is included"
	OUTFILENAME="libz3j_interp.so"
	Z3_LIBNAME="-lz3_interp"
	# Copy the file.
	cp $Z3_LIB_DIR/$Z3_SO_FILENAME $Z3_LIB_DIR/libz3_interp.so
	cp $Z3_LIB_DIR/$Z3_SO_FILENAME $CURRENT_DIR/libz3_interp.so
	Z3_SO_FILENAME="libz3_interp.so"
fi

echo "Building the C wrapper code"
./buildZ3wrapper.py "$Z3_SRC_DIR"

echo "Compiling the C wrapper code and creating the \"z3j\" library"

# This will compile the JNI wrapper part, given the JNI and the Z3 header files
gcc -g $JNI_HEADERS -I$Z3_SRC_DIR org_sosy_lab_cpachecker_util_predicates_z3_Z3NativeApi.c -fPIC -c

if [ $? -eq 0 ]; then
	echo "JNI wrapper compiled"
else
	echo "There was a problem during compilation of \"org_sosy_lab_cpachecker_util_predicates_z3_Z3NativeApi.o\""
	exit 1
fi

gcc -Wall -g -o $OUTFILENAME -shared -Wl,-soname,$OUTFILENAME -Wl,-rpath,'$ORIGIN' -L. -L$Z3_LIB_DIR -L$Z3_DIR org_sosy_lab_cpachecker_util_predicates_z3_Z3NativeApi.o -lc $Z3_LIBNAME

if [ $? -eq 0 ]; then
	strip $OUTFILENAME
else
	echo "There was a problem during compilation of \"org_sosy_lab_cpachecker_util_predicates_z3_Z3NativeApi.c\""
	exit 1
fi

MISSING_SYMBOLS="$(readelf -Ws $OUTFILENAME | grep NOTYPE | grep GLOBAL | grep UND)"
if [ ! -z "$MISSING_SYMBOLS" ]; then
	echo "Warning: There are the following unresolved dependencies in libz3j.so:"
	readelf -Ws $OUTFILENAME | grep NOTYPE | grep GLOBAL | grep UND
	exit 1
fi

MISSING_SYMBOLS="$(readelf -Ws $Z3_LIB_DIR/$Z3_SO_FILENAME | grep NOTYPE | grep GLOBAL | grep UND)"
if [ ! -z "$MISSING_SYMBOLS" ]; then
	echo "Warning: There are the following unresolved dependencies in libz3.so:"
	readelf -Ws $Z3_LIB_DIR/$Z3_SO_FILENAME | grep NOTYPE | grep GLOBAL | grep UND
	exit 1
fi

echo "All Done"
