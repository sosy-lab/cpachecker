#!/bin/sh

# template for the log files. Each log file will be called
# $outfile.$cfg.log, where $cfg is the configuration used (see below)
outfile=results/test_`date +%Y-%m-%d`
mkdir -p results

# the various configurations to test
#configurations="explicitAnalysis2 explicitAnalysis3 explicitAnalysis5 explicitAnalysisInf explicitAndPredAbs2 explicitAndPredAbs3 explicitAndPredAbs5 explicitPredicateAbs"
configurations="explicitAnalysis2 explicitAnalysis3 explicitAnalysis5 explicitAnalysisInf"
# the benchmark instances
#
# this selects the "simplified" instances
instances=`find ../../benchmarks-explicit/working-set/ -name "*.c"`

# run the tests
for cfg in $configurations; do 
     ./run_tests.py --config=../../benchmarks-explicit/config/$cfg.properties --output=$outfile $instances --timeout=1200 --memlimit=1000000
done
