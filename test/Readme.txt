Directory structure for /test:

- config: configuration files for CPAchecker
- expected-results: results from earlier runs, to compare with in regression testing
- output: all files produced by CPAchecker during analysis, like log files, graphs, etc. (not in repository)
- original-sources: unmodified source code from which test cases were made
- programs: test cases in a form that CPAchecker can handle (e.g., example C programs)
- results: all files produced by the benchmark script benchmark.py (not in repository)
- test-sets: files that each specify a set of test cases (which we call a test set)
             and files that specify a whole test-suite (used as input for benchmark.py)
