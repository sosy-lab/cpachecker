#!/bin/bash

SCRIPT_HOME=`readlink -f \`dirname $0\``
LOGFILE="$SCRIPT_HOME/log.`date +%F_%T`"
echo "Logging all output and errors to $LOGFILE"
exec 1>$LOGFILE 2>&1

set -evx

if [ $# -ne 2 ] ; then
  echo "Usage: $0 <SVN-Repo> <Revision>" 1>&2
  exit 1
fi

REPOS=$1
REV=$2

STORAGE=$HOME/cpachecker-regression-test

if [ ! -d $STORAGE -o "`svn info $STORAGE | grep ^URL: | awk '{ print $2 }'`" != "$REPOS" ] ; then
  rm -rf $STORAGE
  svn co -r$REV $REPOS $STORAGE
else
  rm -f $STORAGE/nativeLibs/libmathsatj.so
  svn revert $STORAGE/build.xml $STORAGE/src/cfa/CFABuilder.java $STORAGE/src/cmdline/stubs/StubCodeReaderFactory.java
  svn up -r$REV $STORAGE
fi

cp ../../build.xml $STORAGE
cp ../../nativeLibs/libmathsatj.so $STORAGE/nativeLibs/
cdt_patch=`mktemp`
git show e1317fc5d07f014471ecd98f533a417ccf977064 > $cdt_patch

cd $STORAGE
patch -p1 < $cdt_patch
rm -f $cdt_patch
ant clean
ant

cd $SCRIPT_HOME

export PATH_TO_WORKSPACE=$STORAGE/

# template for the log files. Each log file will be called
# $outfile.$cfg.log, where $cfg is the configuration used (see below)
outfile=results/test_`date +%Y-%m-%d`
mkdir -p results

# the various configurations to test
configurations="summary explicit itpexplicit"

# the benchmark instances
#
# this selects the "simplified" instances
instances=`find ../albertos_tests/test/ -regex ".+[^i]\.cil\.c$"`

# this selects the "original" instances. For these, you should replace the
#"summary" configuration with "summary_cex_suffix", as this works much
# better. I'm still trying to understand why though
#instances=`find test -name"*.i.cil.c"`

# run the tests
for cfg in $configurations; do 
  ../albertos_tests/run_tests.py --config=../albertos_tests/config/$cfg.properties --output=$outfile $instances --timeout=1800 --memlimit=1900000
done

