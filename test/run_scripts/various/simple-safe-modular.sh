#!/bin/bash
if [ -z "$CPAchecker_mt" ]; then
    echo 'Error: $CPAChecker_mt is not set'
    exit 1
fi  
cd $CPAchecker_mt

scripts/cpa.sh -concurrent -config test/config/various/rg-cegar-simple.properties  test/programs/multi-threaded/various/simple-thr0-safe-modular.cil.c  test/programs/multi-threaded/various/simple-thr1-safe-modular.cil.c