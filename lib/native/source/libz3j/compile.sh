#!/bin/sh

echo "PHASE 1"

gcc -save-temps -std=c99 -g -I/usr/lib64/jvm/java-1.7.0-openjdk-1.7.0/include/ -I/usr/lib64/jvm/java-1.7.0-openjdk-1.7.0/include/linux/ -I./../../../../../Z3/Z3_CPAchecker/z3/src/api ./org_sosy_lab_cpachecker_util_predicates_z3_Z3NativeApi.c -c -fPIC

echo "DONE"

echo "PHASE 2" 

gcc -shared -o libz3j.so org_sosy_lab_cpachecker_util_predicates_z3_Z3NativeApi.o libz3.so 

echo "DONE"