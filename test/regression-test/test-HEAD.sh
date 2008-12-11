#!/bin/bash

cd `dirname $0`
rep_dir=/localhome/dbeyer/SVN-software/cpachecker
rev=`svnlook youngest $rep_dir`
[ -d results.r$rev ] && exit 0
./regression-tester.sh file://$rep_dir/trunk $rev > /dev/null
exit 0

