Supplied scripts:

- benchmark.py: for benchmarking model checking tools like CPAchecker or CBMC
                (see HowToBenchmark.txt)
- classpath.bat. used by cpa.bat
- cpa.bat: to start CPAchecker on Windows
- cpa.sh: to start CPAchecker on Linux and similar platforms
          (see HowTo.txt)
- report-generator.py: for building an interactive HTML report of a CPAchecker run
                       (see doc/BuildReport.txt)
- table-generator.py: for creating tables that contain the output of several benchmark.py runs.
                 as params you can either give names of result-files
                 or run the script without params (result-files will be searced in test/results/)
- regression.py: creates a table, that shows differences (different status of a file)
                 between runs of a benchmark (i.e. SAFE vs UNSAFE).
                 to run the script you have to give 2 (or more) result-files
                 (i.e. "regression.py  oldResult.xml  newResult.xml").

Further files:
- benchmark.dtd: DTD for validating benchmark.py input files
- benchmark-result.dtd: DTD for validating benchmark.py output files
- report-template.html: template for the report generator script
