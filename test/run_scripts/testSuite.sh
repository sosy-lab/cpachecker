#!/bin/bash

testsPositive="benchmarks/lu-fig2.fixed.cil.c benchmarks/lu-fig4-complete.fixed.cil.c benchmarks/read_write_lock.cil.c  various/mutex-safe-modular.cil.c various/simple-safe-modular.cil.c benchmarks/bakery.simple.cil.c benchmarks/bluetooth.cav09.cil.c benchmarks/dekker.cil.c benchmarks/peterson.cil.c benchmarks/time_var_mutex.cil.c benchmarks/szymanski.cil.c benchmarks/bakery.cil.c benchmarks/lamport.cil.c"
testsPositive="benchmarks/lu-fig2.fixed.cil.c benchmarks/lu-fig4-complete.fixed.cil.c benchmarks/read_write_lock.cil.c  various/mutex-safe-modular.cil.c various/simple-safe-modular.cil.c benchmarks/bakery.simple.cil.c benchmarks/dekker.cil.c benchmarks/peterson.cil.c benchmarks/time_var_mutex.cil.c benchmarks/szymanski.cil.c benchmarks/bakery.cil.c benchmarks/lamport.cil.c"
testsNegative="benchmarks/lu-fig2.cil.c benchmarks/lu-fig4-complete.cil.c various/mutex-unsafe.cil.c various/real-unsafe.cil.c various/simple-unsafe.cil.c"

positiveMsg='NO, the system is considered safe by the chosen CPAs'
negativeMsg='YES, there is a BUG!'

testDir=test/programs/multi-threaded
configDir=test/config

if [ -z "$CPAchecker_mt" ]; then
    echo 'Error: $CPAChecker_mt is not set'
    exit 1
fi  

cd $CPAchecker_mt

if [ $# -lt 1 ]; then
    echo "usage: $0 CONFIG"
    echo ""
    echo "CONFIG - config file name in $CPAchecker_mt/$configDir"
    exit 1
fi

if [ ! -f $configDir/$1 ]; then
    echo "Config $CPAchecker_mt/$configDir/$1 not found"
    exit 1;
fi

for test in $testsPositive
do
    echo -n "Running $test"
    scripts/cpa.sh -concurrent -config $configDir/$1 $testDir/$test > out 2>/dev/null
    grep 'Time for Analysis:' out | cut -d':' -f2-
    grep  "$positiveMsg" out >/dev/null
    if [ $? -ne 0 ]; then
	echo ""
	echo "Error:"
	echo "scripts/cpa.sh -concurrent -config $configDir/$1 $testDir/$test > out"
	exit 1
    fi
done;

for test in $testsNegative
do
    echo -n "Running $test"
    scripts/cpa.sh -concurrent -config $configDir/$1 $testDir/$test > out 2>/dev/null
    grep 'Time for Analysis:' out | cut -d':' -f2-
    grep "$negativeMsg" out >/dev/null

    if [ $? -ne 0 ]; then
	echo ""
	echo "Error:"
	echo "scripts/cpa.sh -concurrent -config $configDir/$1 $testDir/$test > out"
	exit 1
    fi
done;

rm out
echo ""
echo "All tests passed"
exit 0

