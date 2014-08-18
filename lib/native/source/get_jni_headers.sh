#!/bin/sh

if [ `uname` = "Darwin" ] ; then
  echo "-I/usr/local/include -I/sw/include -I/System/Library/Frameworks/JavaVM.framework/Headers"
  LINK_OPT="-dynamiclib -o libJOct.jnilib"
elif [ `uname` = "Linux" ] ; then
  java_home=`readlink -f \`which java\``
  java_home=`echo $java_home | sed 's#/jre/bin/java##'`

  java_home=/import/linux/x86_64/soft/netbeans/8.0/jdk1.8.0
  echo "-I$java_home/include/ -I$java_home/include/linux/"
else
  echo "Missing build information for `uname`" >&2
  exit 1
fi
