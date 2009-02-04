#!/bin/bash

cd `dirname $0`
rep_dir=/localhome/dbeyer/SVN-software/cpachecker
rev=`svnlook youngest $rep_dir`
[ -d results.r$rev ] && exit 0
if ps aux | grep regression-tester.sh | grep -q cpachecker ; then
  echo "Regression test already running"
  exit 1
fi
./regression-tester.sh file://$rep_dir/trunk $rev > /dev/null

