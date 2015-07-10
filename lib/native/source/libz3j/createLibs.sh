#!/bin/bash
# This script creates the libz3j binary
# First parameter is the z3 base directory
# second parameter is the branch of z3 that should be used (e.g. master or opt)
# creating z3 with libFOCI support is not possible with this script.

CUR_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
Z3_DIR="$1"
Z3_BRANCH="$2"

# preparations
cd $Z3_DIR
git checkout $Z3_BRANCH
if [ $? -ne 0 ];then
   echo "Branch $Z3_BRANCH not available."
   exit 1
fi
git pull

## actual library creation
./configure
if [ $? -ne 0 ];then
   echo "Running './configure' in $(pwd) resulted in an error"
   exit 1
fi


python scripts/mk_make.py
# check return value of build
if [ $? -ne 0 ];then
   echo "Running 'pyton scripts/mk_make.py' in $(pwd) resulted in an error"
   exit 1
fi

cd build
make
# check return value of make
if [ $? -ne 0 ];then
   echo "Running 'make' in $(pwd) resulted in an error"
   exit 1
fi

strip libz3.so
# check return value of strip
if [ $? -ne 0 ];then
   echo "Running 'strip libz3.so' in $(pwd) resulted in an error"
   exit 1
fi

cd $CUR_DIR
./buildZ3wrapper.py $Z3_DIR/src/api
# check return value of buildwrapper
if [ $? -ne 0 ];then
   echo "Running './buildZ3wrapper.py $Z3_DIR/src/api' in $(pwd) resulted in an error"
   exit 1
fi


./compile.sh $Z3_DIR ""
if [ $? -ne 0 ];then
   echo "Running './compile.sh $Z3_DIR \"\"' in $(pwd) resulted in an error"
   exit 1
fi

cp $Z3_DIR/build/libz3.so $CUR_DIR/libz3.so

echo "library sucessfully created"
echo "Please copy libz3j.so and libz3.so to the correct directory under CPAchecker/lib/native/..."

exit 0
