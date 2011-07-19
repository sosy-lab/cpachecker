#!/bin/bash
exec_path=".."
$exec_path/scripts/cpa.sh -concurrent -config $exec_path/test/config/benchmarks/rg-dekker-abe-it1.properties $exec_path/test/programs/multi-threaded/threader_benchmarks/dekker-thr1.cil.c  $exec_path/test/programs/multi-threaded/threader_benchmarks/dekker-thr2.cil.c


