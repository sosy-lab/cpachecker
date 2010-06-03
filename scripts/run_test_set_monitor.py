#!/bin/sh

BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

if [ -z "$1" ]; then
	echo "Error: Please specify name of test suite to run."
	exit 1
fi

if [ -z "$2" ]; then
	echo "Error: Please specify name of configuration to use."
	exit 1
fi

SUITE="test/test-sets/$1" 

if [ ! -f "$SUITE" ] ; then
	echo "Error: file $SUITE does not exist."
	exit 1
fi

CONFIG="test/config/$2"

if [ ! -f "$CONFIG" ] ; then
	echo "Error: file $CONFIG does not exist."
	exit 1
fi

mkdir "test/output" 2>/dev/null
OUTFILE="test/output/test_`date +%Y-%m-%d`"

INSTANCES="`cat \"$SUITE\"`"

shift; shift

exec scripts/run_tests_monitor.py "--config=$CONFIG" "--output=$OUTFILE" "$@" $INSTANCES


