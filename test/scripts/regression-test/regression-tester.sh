#!/bin/bash
#
# Michael Tautschnig <tautschnig@forsyte.de>
#
# Code includes lots of stuff shamelessly stolen from other scripts of this
# project (like ../albertos_tests/do_run.sh) and
# http://code.google.com/p/crocopat/source/browse/trunk/test/regrtest.sh
#

if [ $# -lt 4 ] ; then
  echo "Usage: $0 <SVN-Repo> <Revision> <Suite> <Configs...>" 1>&2
  exit 1
fi

REPOS=$1
REV=$2
SUITE=$3
shift ; shift ; shift
CONFIGURATIONS=$@
# revision must be numeric to make all log output got to appropriate locations
# (name-based revisioning like HEAD will change from time to time ...)
if ! echo $REV | egrep -q '^[[:digit:]]+$' ; then
  echo "<Revision> must be numeric for consistency reasons" 1>&2
  if [ "$REV" = "HEAD" ] ; then
    echo "Use svnlook youngest to resolve HEAD" 1>&2
  fi
  exit 1
fi

# checkout or update from the given SVN URL and the specified revision
STORAGE=$HOME/cpachecker-regression-test
# variable used by cpa.sh
export PATH_TO_CPACHECKER=$STORAGE/
if [ ! -d $STORAGE ] || [ "`svn info $STORAGE | grep ^URL: | awk '{ print $2 }'`" != "$REPOS" ] ; then
  rm -rf $STORAGE
  svn co -r$REV $REPOS $STORAGE
else
  rm -f $STORAGE/build.xml $STORAGE/src/cfa/CFABuilder.java $STORAGE/src/cmdline/stubs/StubCodeReaderFactory.java
  svn up -r$REV $STORAGE
fi

# go home
cd `dirname $0`

if [ ! -d ../../benchmarks-$SUITE/ ] ; then
  echo "Suite $SUITE not found as ../../benchmarks-$SUITE" 1>&2
  exit 1
else
  BENCHMARK_SUITE="`pwd`/../../benchmarks-$SUITE"
  for c in $CONFIGURATIONS ; do
    if [ ! -s $BENCHMARK_SUITE/config/$c.properties ] ; then
      echo "Configuration $c unknown in $SUITE" 1>&2
      exit 1
    fi
  done
fi

# installation of eclipse
ECLIPSE_SEARCH_PATH="$HOME/eclipse /opt/eclipse $HOME/Desktop/eclipse"

for d in $ECLIPSE_SEARCH_PATH ; do
  if [ -d $d/plugins ] ; then
    export ECLIPSE_HOME="$d"
    break
  fi
done
if [ -z "$ECLIPSE_HOME" ] ; then
  echo "Eclipse plugin directory not found" 1>&2
  exit 1
fi
CDT_VERSION="`basename $ECLIPSE_HOME/plugins/plugins/org.eclipse.cdt.core_*`"
CDT_VERSION="`echo $CDT_VERSION | cut -f2 -d_ | cut -b1`"

# make ourselves log all output
SCRIPT_HOME=`pwd`
LOGDIR="$SCRIPT_HOME/results.$SUITE.r$REV"
mkdir -p $LOGDIR
LOGFILE="$LOGDIR/run-log.`date +%F_%T`"
echo "Logging all output and errors to $LOGFILE"
exec 1>$LOGFILE 2>&1

# log all executed commands and exit on error
set -evx

if [ $CDT_VERSION -gt 4 ] ; then
  # This will only work for tautschnig@cs-sel-02.cs.surrey.sfu.ca
  # the patch for CDT 5
  cdt_patch=`mktemp`
  git show 155b386a6dd94d6e40285ab47600c5a1dec623b8 > $cdt_patch
  cd $STORAGE
  patch -p1 < $cdt_patch
  rm -f $cdt_patch
fi

# record the patches that have been applied
cd $STORAGE
svn diff > $LOGDIR/patches.`date +%F_%T`.diff

# go and build!
ant clean
ant

# build is fine, let's run the test suite
cd $SCRIPT_HOME

# template for the log files. Each log file will be called
# $outfile.$cfg.log, where $cfg is the configuration used (see below)
outfile="$LOGDIR/test_`date +%Y-%m-%d`"

# the benchmark instances
instances=`find $BENCHMARK_SUITE/working-set/ -name "*.c"`

# remove the old log files
find $BENCHMARK_SUITE/working-set/ -name "*.log" -delete

# run the tests
for cfg in $CONFIGURATIONS; do
  ../simple/run_tests.py --config=$BENCHMARK_SUITE/config/$cfg.properties --output=$outfile $instances --timeout=1800 --memlimit=1900000

  # bring home all the logfiles
  for i in $instances ; do
    name=`echo $i | sed "s#^$BENCHMARK_SUITE/working-set/##"`
    dir=`dirname $name`
    mkdir -p $LOGDIR/$dir
    mv $i.log $LOGDIR/$name
  done

  # now compare the results to the master copy
  master="$BENCHMARK_SUITE/expected-results/$cfg.log"
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
rm -f cex.msat CPALog.txt predmap.txt

