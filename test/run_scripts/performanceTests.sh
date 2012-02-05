#!/bin/bash

perTests="rg-global-sbe-cartesian-fa.properties rg-global-sbe-cartesian-sa.properties rg-global-sbe-cartesian-st.properties"

for test in $perTests
do
    echo "./testSuite.sh $test"
    ./testSuite.sh $test
    echo ""
done
