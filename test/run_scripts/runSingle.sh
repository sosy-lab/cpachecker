#!/bin/bash

testDir=test/programs/multi-threaded
configDir=test/config

if [ -z "$CPAchecker_mt" ]; then
    echo 'Error: $CPAChecker_mt is not set'
    exit 1
fi  

cd $CPAchecker_mt

if [ $# -lt 2 ]; then
    echo "usage: $0 PROGRAM CONFIG"
    echo ""
    echo "PROGRAM - program in $CPAchecker_mt/$testDir"
    echo "CONFIG - config file name in $CPAchecker_mt/$configDir"
    exit 1
fi

if [ ! -f $configDir/$2 ]; then
    echo "Config $CPAchecker_mt/$configDir/$1 not found"
    exit 1;
fi

if [ ! -f $testDir/$1 ]; then
    echo "program $CPAchecker_mt/$testDir/$2 not found"
    exit 1;
fi

scripts/cpa.sh -concurrent -config $configDir/$2 $testDir/$1

exit 0

