#!/bin/bash
if [ -z "$CPAchecker_mt" ]; then
    echo 'Error: $CPAChecker_mt is not set'
    exit 1
fi  
cd $CPAchecker_mt
scripts/cpa.sh -concurrent -config test/config/various/rg-cegar-real.properties test/programs/multi-threaded/various/real-unsafe-thr0.cil.c  test/programs/multi-threaded/various/real-unsafe-thr1.cil.c

