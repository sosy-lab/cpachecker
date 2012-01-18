#!/bin/bash

set -e

SRC=$1

if [ ! -f $SRC ] ; then
  echo "Source file $SRC not found"
  exit 1
fi

export PATH=/nobackup/exp-holzera-tautschn/crest-0.1.1/bin:$PATH
export PATH=/nobackup/exp-holzera-tautschn/klee/klee/Release/bin/:$PATH
export PATH=/nobackup/exp-holzera-tautschn/klee/llvm-gcc4.2-2.7-x86_64-linux/bin/:$PATH
export LD_LIBRARY_PATH=/nobackup/exp-holzera-tautschn/crest-0.1.1/yices-1.0.29/lib/:$LD_LIBRARY_PATH
export LD_LIBRARY_PATH=/nobackup/exp-holzera-tautschn/klee/klee/Release/lib/:$LD_LIBRARY_PATH

mkdir tmp-testing
bn=`basename $SRC`
bn=`basename $bn .c`
gcc -E -D__CREST__ $SRC > tmp-testing/$bn-crest.c
gcc -E -D__KLEE__ $SRC > tmp-testing/$bn-klee.i
cd tmp-testing

crestc $bn-crest.c
run_crest ./$bn-crest 100 -dfs 2>&1

llvm-gcc --emit-llvm -g -c $bn-klee.i -o $bn-klee.bc
time klee -max-time=10 $bn-klee.bc
# gcc $bn-klee.i -g -fprofile-arcs -ftest-coverage -L /home/scratch/mictau/klee/klee/Release/lib/ -lkleeRuntest -o $bn-klee
# for f in klee-last/*.ktest ; do \
#   KTEST_FILE=$$f #   ./$bn-klee ; \
# done
# gcov -b ./$bn-klee | egrep '^(File|Branches|No branches)'

cd ..
rm -r tmp-testing

