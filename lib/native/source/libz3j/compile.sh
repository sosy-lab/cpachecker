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
OUTFILENAME="libz3j"
Z3_SO_FILENAME="libz3"

if [[ `uname` == CYGWIN* ]] ; then
    FILE_SUFFIX=".dll"
    # the 64bit environment of Cygwin is not yet stable. We have to use a cross compiler.
    COMPILER_CMD="./gcc64/bin/x86_64-pc-mingw32-gcc.exe"
else
    FILE_SUFFIX=".so"
    COMPILER_CMD="gcc"
fi

Z3_LIBNAME="-lz3"

Z3_DIR="$1"
Z3_SRC_DIR="$Z3_DIR"/src/api
Z3_LIB_DIR="$Z3_DIR"/build

USE_FOCI="$2"

if [[ $USE_FOCI == "-interp" ]] ; then
  echo "Using interpolation with external library FOCI"
  OUTFILENAME="${OUTFILENAME}_interp"
  Z3_LIBNAME="${Z3_LIBNAME}_interp"
  cp ${Z3_LIB_DIR}/${Z3_SO_FILENAME}${FILE_SUFFIX} ${Z3_LIB_DIR}/${Z3_SO_FILENAME}_interp${FILE_SUFFIX}
  Z3_SO_FILENAME="${Z3_SO_FILENAME}_interp"
fi

if [ ! -f "${Z3_LIB_DIR}/${Z3_SO_FILENAME}${FILE_SUFFIX}" ]; then
	echo "You need to specify the directory with the successfully built Z3 on the command line!"
	exit 1
fi

echo "Building the C wrapper code"
./buildZ3wrapper.py "$Z3_SRC_DIR"

echo "Compiling the C wrapper code and creating the \"z3j\" library"

# This will compile the JNI wrapper part, given the JNI and the Z3 header files
$COMPILER_CMD -g $JNI_HEADERS -I$Z3_SRC_DIR org_sosy_lab_cpachecker_util_predicates_z3_Z3NativeApi.c -fPIC -c

if [ $? -eq 0 ]; then
	echo "JNI wrapper compiled"
else
	echo "There was a problem during compilation of \"org_sosy_lab_cpachecker_util_predicates_z3_Z3NativeApi.o\""
	exit 1
fi

if [[ `uname` == CYGWIN* ]] ; then
    echo "A failure in the following steps might be due to a wrong compiler environment. Has libz3.dll been compiled for 64bit?"
fi

$COMPILER_CMD -Wall -g -o ${OUTFILENAME}${FILE_SUFFIX} -shared -Wl,-soname,${OUTFILENAME}${FILE_SUFFIX} -Wl,-rpath,'$ORIGIN' -L. -L$Z3_LIB_DIR -L$Z3_DIR org_sosy_lab_cpachecker_util_predicates_z3_Z3NativeApi.o $Z3_LIBNAME


if [[ ! `uname` == CYGWIN* ]] ; then

    if [ $? -eq 0 ]; then
    	strip ${OUTFILENAME}$FILE_SUFFIX
    else
    	echo "There was a problem during compilation of \"org_sosy_lab_cpachecker_util_predicates_z3_Z3NativeApi.c\""
    	exit 1
    fi

    MISSING_SYMBOLS="$(readelf -Ws ${OUTFILENAME}$FILE_SUFFIX | grep NOTYPE | grep GLOBAL | grep UND)"
    if [ ! -z "$MISSING_SYMBOLS" ]; then
    	echo "Warning: There are the following unresolved dependencies in libz3j.so:"
    	readelf -Ws ${OUTFILENAME}$FILE_SUFFIX | grep NOTYPE | grep GLOBAL | grep UND
    	exit 1
    fi

    MISSING_SYMBOLS="$(readelf -Ws $Z3_LIB_DIR/${Z3_SO_FILENAME}$FILE_SUFFIX | grep NOTYPE | grep GLOBAL | grep UND)"
    if [ ! -z "$MISSING_SYMBOLS" ]; then
    	echo "Warning: There are the following unresolved dependencies in libz3.so:"
    	readelf -Ws $Z3_LIB_DIR/${Z3_SO_FILENAME}$FILE_SUFFIX | grep NOTYPE | grep GLOBAL | grep UND
    	exit 1
    fi
fi

echo "All Done"
