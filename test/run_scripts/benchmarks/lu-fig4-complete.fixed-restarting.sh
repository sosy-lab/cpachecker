#!/bin/bash
if [ -z "$CPAchecker_mt" ]; then
    echo 'Error: $CPAChecker_mt is not set'
    exit 1
fi  
cd $CPAchecker_mt

scripts/cpa.sh -concurrent -config test/config/various/rg-cegar-lu-fig4-complete.fixed-restarting.properties test/programs/multi-threaded/benchmarks/lu-fig4-complete.fixed.cil.c  