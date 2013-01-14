Supplied scripts:

- benchmark.py: for benchmarking model checking tools like CPAchecker or CBMC
                (see doc/Benchmark.txt)
- cpa.bat: to start CPAchecker on Windows
- cpa.sh: to start CPAchecker on Linux and similar platforms
          (see README.txt)
- report-generator.py: for building an interactive HTML report of a CPAchecker run
                       (see doc/BuildReport.txt)
- table-generator.py
  Creates HTML and CSV tables that contain the output of several benchmark.py runs.
  Also creates tables with just those results differing between two or more runs.
  As params you can either give names of result files
  or run the script without params (result files will be searched in test/results/).

Further files:
- benchmark.dtd: DTD for validating benchmark.py input files
- benchmark-result.dtd: DTD for validating benchmark.py output files
- report-template.html: template for the report generator script
- table-generator-template.*: templates for the table generator script
