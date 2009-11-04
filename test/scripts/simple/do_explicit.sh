#!/bin/sh

# the various configurations to test
#configurations="explicitAnalysis2 explicitAnalysis3 explicitAnalysis5 explicitAnalysisInf explicitAndPredAbs2 explicitAndPredAbs3 explicitAndPredAbs5 explicitPredicateAbs"
configurations="explicitAnalysisInf"

SCRIPT="`dirname \"$0\"`/run_test_suite.sh"

# run the tests
for cfg in $configurations; do 
	"$SCRIPT" benchmarks-explicit "$cfg.properties" --timeout=1200 --memlimit=1000000
done
