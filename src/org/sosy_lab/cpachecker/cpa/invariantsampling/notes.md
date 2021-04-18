# run something
scripts/benchmark.py -N 4 -c 1 test/test-sets/integration-invariantsampling.xml

# run analysis
org.sosy_lab.cpachecker.cmdline.CPAMain
-config config/invariantsampling.properties doc/examples/example.c
