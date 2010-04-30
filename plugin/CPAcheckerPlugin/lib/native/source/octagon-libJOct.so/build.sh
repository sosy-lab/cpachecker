#!/bin/bash

wget http://www.di.ens.fr/~mine/oct/oct-0.9.10.tar.gz
tar xzf oct-0.9.10.tar.gz
cd oct-0.9.10
./configure --with-num=float
make
cd ..

if [ `uname` = "Darwin" ] ; then
  COMPILE_OPT="-I/usr/local/include -I/sw/include -I/System/Library/Frameworks/JavaVM.framework/Headers"
  LINK_OPT="-dynamiclib -o libJOct.jnilib"
elif [ `uname` = "Linux" ] ; then
  java_home=`readlink -f \`which java\``
  java_home=`echo $java_home | sed 's#/jre/bin/java##'`
  COMPILE_OPT="-fPIC -I$java_home/include/ -I$java_home/include/linux/"
  LINK_OPT="-shared -o libJOct.so"
else
  echo "Missing build information for `uname`"
  exit 1
fi
gcc -g -O2 $COMPILE_OPT -Ioct-0.9.10/ -Ioct-0.9.10/clib/ -DOCT_HAS_GMP -DOCT_ENABLE_ASSERT -DOCT_NUM_FLOAT -DOCT_PREFIX=CAT\(octfag_ -c OctWrapper.c
gcc $LINK_OPT OctWrapper.o oct-0.9.10/clib/.libs/*.o -lm

