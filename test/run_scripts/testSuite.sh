#!/bin/bash

testsPositive="benchmarks/lu-fig2.fixed-restarting.sh benchmarks/lu-fig4-complete.fixed-restarting.sh benchmarks/read_write_lock-restarting.sh  various/mutex-safe-restarting.sh various/simple-safe-modular-restarting.sh\
 benchmarks/naivebakery-restarting.sh"
testsPositive="benchmarks/lu-fig2.fixed-restarting.sh benchmarks/lu-fig4-complete.fixed-restarting.sh benchmarks/read_write_lock-restarting.sh  various/mutex-safe-restarting.sh various/simple-safe-modular-restarting.sh"
testsNegative="benchmarks/lu-fig2-restarting.sh benchmarks/lu-fig4-complete-restarting.sh various/mutex-unsafe-restarting.sh various/real-unsafe-restarting.sh various/simple-unsafe-restarting.sh"

positiveMsg='NO, the system is considered safe by the chosen CPAs'
negativeMsg='YES, there is a BUG!'

for test in $testsPositive
do
    echo -n "Running $test..."
    $test > out 2>/dev/null
    grep 'Time for Analysis:' out | cut -d':' -f2-
    grep  "$positiveMsg" out >/dev/null
    if [ $? -ne 0 ]; then
	echo "Error: test $test failed"
	exit 1
    fi
done;

for test in $testsNegative
do
    echo -n "Running $test..."
   $test > out 2>/dev/null
    grep 'Time for Analysis:' out | cut -d':' -f2-
    grep "$negativeMsg" out >/dev/null

    if [ $? -ne 0 ]; then
	echo "Error: test $test failed"
	exit 1
    fi
done;

rm out
echo ""
echo "All tests passed"
exit 0

















