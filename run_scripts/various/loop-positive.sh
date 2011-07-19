#!/bin/bash
exec_path=".."
$exec_path/scripts/cpa.sh -concurrent -config $exec_path/test/config/rg-loop-positive.properties $exec_path/test/programs/multi-threaded/original/loop-thr0.cil.c  $exec_path/test/programs/multi-threaded/original/loop-thr1.cil.c

