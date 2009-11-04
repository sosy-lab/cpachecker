#!/bin/sh

# the various configurations to test
configurations="summary explicit itpexplicit"

SCRIPT="./`dirname \"$0\"`/run_test_suite.sh"

# run the tests
for cfg in $configurations; do 
	"$SCRIPT" benchmarks-lbe "$cfg.properties" --timeout=1800 --memlimit=1900000
done