#!/bin/bash
exec_path=".."
$exec_path/scripts/cpa.sh -concurrent -config $exec_path/test/config/benchmarks/rg-dekker-abe-corrected.properties $exec_path/test/programs/multi-threaded/original/dekker-thr1-corrected.cil.c  $exec_path/test/programs/multi-threaded/original/dekker-thr2-corrected.cil.c

