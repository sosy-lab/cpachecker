#!/bin/bash

if [ "`hostname`" != "cs-sel-02" ] ; then
  echo "This script only works on cs-sel-02.cs.surrey.sfu.ca"
  exit 1
fi

cd `dirname $0`
rep_dir=/localhome/dbeyer/SVN-software/cpachecker
rev=`svnlook youngest $rep_dir`
[ -d results.lbe.r$rev ] && exit 0
if ps aux | grep regression-tester.sh | grep -q cpachecker ; then
  echo "Regression test already running"
  exit 1
fi
./regression-tester.sh file://$rep_dir/trunk $rev lbe summary explicit itpexplicit > /dev/null

