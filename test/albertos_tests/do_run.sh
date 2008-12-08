#!/bin/sh

# template for the log files. Each log file will be called
# $outfile.$cfg.log, where $cfg is the configuration used (see below)
outfile=results/test_`date +%Y-%m-%d`
mkdir -p results

# the various configurations to test
configurations="summary explicit itpexplicit"

# the benchmark instances
#
# this selects the "simplified" instances
instances=`find test/ -regex ".+[^i]\.cil\.c$"`

# this selects the "original" instances. For these, you should replace the
#"summary" configuration with "summary_cex_suffix", as this works much
# better. I'm still trying to understand why though
#instances=`find test -name"*.i.cil.c"`

# run the tests
for cfg in $configurations; do 
     ./run_tests.py --config=config/$cfg.properties --output=$outfile $instances --timeout=1800 --memlimit=1900000
done
