#!/bin/bash

cd `dirname $0`

# Trunk exists since r163, so start from there
rev=163
rep_dir=/localhome/dbeyer/SVN-software/cpachecker

while [ $rev -le `svnlook youngest $rep_dir` ] ; do
  ./regression-tester.sh file://$rep_dir/trunk $rev
  rev=$((rev + 1))
done

