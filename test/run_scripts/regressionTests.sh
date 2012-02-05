#!/bin/bash

regTests="rg-global-lbe-boolean-fa-debug.properties rg-global-lbe-boolean-sa-debug.properties  rg-global-lbe-boolean-st-debug.properties  rg-global-sbe-cartesian-fa-debug.properties rg-global-sbe-cartesian-sa-debug.properties rg-global-sbe-cartesian-st-debug.properties"

for test in $regTests
do
    echo "./testSuite.sh $test"
    ./testSuite.sh $test
    echo ""
done


