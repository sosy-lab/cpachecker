#!/bin/bash
if [ -z "$CPAchecker_mt" ]; then
    echo 'Error: $CPAChecker_mt is not set'
    exit 1
fi  
cd $CPAchecker_mt

scripts/cpa.sh -concurrent -config test/config/various/rg-cegar-read_write_lock-lazy.properties test/programs/multi-threaded/benchmarks/read_write_lock-thr0.cil.c test/programs/multi-threaded/benchmarks/read_write_lock-thr1.cil.c 