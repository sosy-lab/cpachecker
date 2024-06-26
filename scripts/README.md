<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

# Start Scripts

The following scripts are deprecated
and users should switch to their replacements in the `../bin` directory.
For full documentation on how to execute CPAchecker
cf. the [README](../README.md) in main directory.

- `cpa.bat`: replaced by `../bin/cpachecker.bat`
- `cpa.sh`: replaced by `../bin/cpachecker`
- `cpa_witness2test.py`: replaced by `../bin/cpa-witness2test`

# Benchmarking Scripts
(an extension of [BenchExec](https://github.com/sosy-lab/benchexec))

- `benchmark.py`: for benchmarking collections of runs
                (c.f. [doc/Benchmark.md](../doc/Benchmark.md))
- `runexecutor.py`: for benchmarking a single run
- `table-generator.py`:
  Creates HTML and CSV tables that contain the output of several `benchmark.py` runs.
  Also creates tables with just those results differing between two or more runs.
  As params you can either give names of result files
  or run the script without params (result files will be searched in `test/results/`).

# Post-processing
- `generate_coverage.py`:
  Computes a coverage under-approximation that complements the one already reported by CPAchecker
  (cf. the respective [README](post_processing/coverage/README.md)).
