#!/usr/bin/env bash
# This script searches for all included libraries in the current directory first.
# You can use this to override specific libraries installed on your system.
# You can also use this to force static linking of a specific library,
# if you put only the corresponding .a file in this directory, not the .so file.

# For example, to statically link against libstdc++,
# compile this library with --with-pic,
# and put the resulting libstdc++.a file in this directory.

JNI_HEADERS="$(../get_jni_headers.sh)"

Z3_DIR="$1"
Z3_SRC_DIR="$Z3_DIR"/src/api
Z3_LIB_DIR="$Z3_DIR"/build
if [ ! -f "$Z3_LIB_DIR/libz3.so" ]; then
	echo "You need to specify the directory with the successfully built Z3 on the command line!"
	exit 1
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

gcc -Wall -g -o libz3j.so -shared -Wl,-soname,libz3j.so -Wl,-rpath,'$ORIGIN' -L. -L$Z3_LIB_DIR -L$Z3_DIR org_sosy_lab_cpachecker_util_predicates_z3_Z3NativeApi.o -lc -lz3

if [ $? -eq 0 ]; then
	strip libz3j.so
else
	echo "There was a problem during compilation of \"org_sosy_lab_cpachecker_util_predicates_z3_Z3NativeApi.c\""
	exit 1
fi

MISSING_SYMBOLS="$(readelf -Ws libz3j.so | grep NOTYPE | grep GLOBAL | grep UND)"
if [ ! -z "$MISSING_SYMBOLS" ]; then
	echo "Warning: There are the following unresolved dependencies in libz3j.so:"
	readelf -Ws libz3j.so | grep NOTYPE | grep GLOBAL | grep UND
	exit 1
fi

MISSING_SYMBOLS="$(readelf -Ws $Z3_LIB_DIR/libz3.so | grep NOTYPE | grep GLOBAL | grep UND)"
if [ ! -z "$MISSING_SYMBOLS" ]; then
	echo "Warning: There are the following unresolved dependencies in libz3.so:"
	readelf -Ws $Z3_LIB_DIR/libz3.so | grep NOTYPE | grep GLOBAL | grep UND
	exit 1
fi

echo "All Done"
