Supplied Scripts:

- cpa.bat: to start CPAchecker on Windows
- cpa.sh: to start CPAchecker on Linux and similar platforms
          (see README.txt)

Benchmarking Scripts:
(extension of BenchExec, c.f. https://github.com/dbeyer/benchexec)

- benchmark.py: for benchmarking collections of runs
                (c.f. doc/Benchmark.txt)
- runexecutor.py: for benchmarking a single run
- table-generator.py
  Creates HTML and CSV tables that contain the output of several benchmark.py runs.
  Also creates tables with just those results differing between two or more runs.
  As params you can either give names of result files
  or run the script without params (result files will be searched in test/results/).

Post-processing:
- generate_coverage.py:
  Computes a coverage under-approximation that complements the one already
  reported by CPAchecker. For more information: https://github.com/sosy-lab/cpachecker/tree/trunk/scripts/post_processing/coverage
