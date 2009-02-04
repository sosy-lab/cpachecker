#!/bin/bash
#
# Michael Tautschnig <tautschnig@forsyte.de>
#
# Code includes lots of stuff shamelessly stolen from other scripts of this
# project (like ../albertos_tests/do_run.sh) and
# http://code.google.com/p/crocopat/source/browse/trunk/test/regrtest.sh
#
# In various places specifics of the account of
# tautschnig@cs-sel-02.cs.surrey.sfu.ca are assumed; this includes
# - use from within a git repository that has the CDT 5 patch
# - use from within a location that has a proper build.xml and the MathSAT
#   library
# - proper cpa.sh with all the paths set for eclipse and stuff

if [ $# -ne 2 ] ; then
  echo "Usage: $0 <SVN-Repo> <Revision>" 1>&2
  exit 1
fi

REPOS=$1
REV=$2
# revision must be numeric to make all log output got to appropriate locations
# (name-based revisioning like HEAD will change from time to time ...)
if ! echo $REV | egrep -q '^[[:digit:]]+$' ; then
  echo "<Revision> must numeric for consistency reasons" 1>&2
  if [ "$REV" = "HEAD" ] ; then
    echo "Use svnlook youngest to resolve HEAD" 1>&2
  fi
  exit 1
fi

# checkout or update from the given SVN URL and the specified revision
STORAGE=$HOME/cpachecker-regression-test
# variable used by cpa.sh
export PATH_TO_WORKSPACE=$STORAGE/
if [ ! -d $STORAGE ] || [ "`svn info $STORAGE | grep ^URL: | awk '{ print $2 }'`" != "$REPOS" ] ; then
  rm -rf $STORAGE
  svn co -r$REV $REPOS $STORAGE
else
  rm -f $STORAGE/nativeLibs/libmathsatj.so $STORAGE/build.xml $STORAGE/src/cfa/CFABuilder.java $STORAGE/src/cmdline/stubs/StubCodeReaderFactory.java
  svn up -r$REV $STORAGE
fi

# make ourselves log all output
SCRIPT_HOME=`cd \`dirname $0\` > /dev/null 2>&1 ; pwd`
LOGDIR="$SCRIPT_HOME/results.r$REV"
mkdir -p $LOGDIR
LOGFILE="$LOGDIR/run-log.`date +%F_%T`"
echo "Logging all output and errors to $LOGFILE"
exec 1>$LOGFILE 2>&1

# log all executed commands and exit on error
set -evx

# install all the files and library for the specific build infrastructure
cp ../../build.xml $STORAGE
cp ../../nativeLibs/libmathsatj.so $STORAGE/nativeLibs/
# the patch for CDT 5
cdt_patch=`mktemp`
git show 155b386a6dd94d6e40285ab47600c5a1dec623b8 > $cdt_patch
cd $STORAGE
patch -p1 < $cdt_patch
rm -f $cdt_patch
# record the patches that have been applied
svn diff > $LOGDIR/patches.`date +%F_%T`.diff

# go and build!
ant clean
ant

# build is fine, let's run the test suite
cd $SCRIPT_HOME

# template for the log files. Each log file will be called
# $outfile.$cfg.log, where $cfg is the configuration used (see below)
outfile="$LOGDIR/test_`date +%Y-%m-%d`"

# the various configurations to test
configurations="summary explicit itpexplicit"

# the benchmark instances
# this selects the "simplified" instances
instances=`find ../albertos_tests/test/ -regex ".+[^i]\.cil\.c$"`
# this selects the "original" instances. For these, you should replace the
#"summary" configuration with "summary_cex_suffix", as this works much
# better. I'm still trying to understand why though
#instances=`find test -name"*.i.cil.c"`

# remove the old log files
find ../albertos_tests/test/ -name "*.log" -delete
# get the script to use our cpa.sh
ln -sf ../albertos_tests/run_tests.py

# run the tests
for cfg in $configurations; do
  ./run_tests.py --config=../albertos_tests/config/$cfg.properties --output=$outfile $instances --timeout=1800 --memlimit=1900000

  # bring home all the logfiles
  for i in `find ../albertos_tests/test/ -name "*.log"` ; do
    name=`echo $i | sed 's#^../albertos_tests/test/##'`
    dir=`dirname $name`
    mkdir -p $LOGDIR/$dir
    mv $i $LOGDIR/$name
  done

  # now compare the results to our master copy
  # results.master has a single file per configuration stating all the results
  master="results.master/$cfg.log"
  if [ ! -s $master ] ; then
    echo "No definitive results available, can't verify results" 1>&2
    continue
  fi

  current="$outfile.$cfg.log"
  [ -s "$current" ] || exit 1
  cmp_result="$outfile.$cfg.cmp"
  grep "^/" $current | while read f t r ; do
    master_result="`grep -w "$f" $master | awk '{ print $3 ":" $2 }'`"
    mr="`echo $master_result | cut -f1 -d:`"
    mt="`echo $master_result | cut -f2 -d:`"
    if [ -z "$master_result" ] ; then
      echo "$f WARN_NOT_IN_MASTER" >> $cmp_result
    elif [ "$mr" = "ERROR" -o "$mr" = "UNKNOWN" ] ; then
      if [ "$r" != "ERROR" -a "$r" != "UNKNOWN" ] ; then
        echo "$f WARN_ERROR_IN_MASTER" >> $cmp_result
      else
        echo "$f WARN_ERROR_ALL" >> $cmp_result
      fi
    elif [ "$r" = "ERROR" -o "$r" = "UNKNOWN" ] ; then
      echo "$f ERR_NEW_ERROR" >> $cmp_result
    elif [ "$r" != "$mr" ] ; then
      echo "$f ERR_WRONG_RESULT" >> $cmp_result
    else
      perl -e "
      if ($t - $mt > 10) { print '$f WARN_TOO_SLOW'; }
      elsif ($t - $mt < -10) { print '$f WARN_TOO_FAST'; }
      else { print '$f OK'; }" >> $cmp_result
      echo >> $cmp_result
    fi
  done
done

# cleanup
rm -f run_tests.py cex.msat CPALog.txt predmap.txt

