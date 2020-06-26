<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

Getting Started with CPAchecker
===============================

Installation Instructions:  [`INSTALL.md`](INSTALL.md)
Develop and Contribute:     [`doc/Developing.md`](doc/Developing.md)

More documentation can be found in the [`doc`](doc) folder.

License and Copyright
---------------------
CPAchecker is licensed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0)
with copyright by [Dirk Beyer](https://www.sosy-lab.org/people/beyer/) and others
(cf. [Authors.md](Authors.md) for full list of all contributors).
Third-party libraries are under various other licenses and copyrights,
cf. `lib/java-runtime-licenses.txt` for an overview
and the files in the directory `LICENSES` for the full license texts.
In particular, MathSAT is available for research and evaluation purposes only
(cf. `LICENSES/LicenseRef-MathSAT-CPAchecker.txt`),
so make sure to use a different SMT solver if necessary.
Note that although a GPL program is distributed together with CPAchecker,
CPAchecker is separate from that program and thus not under the terms of the GPL.

Prepare Programs for Verification by CPAchecker
-----------------------------------------------

All programs need to pre-processed with the C pre-processor,
i.e., they may not contain #define and #include directives.
You can enable pre-processing inside CPAchecker
by specifying -preprocess on the command line.
Multiple C files can be given and will be linked together
and verified as a single program (experimental feature).

CPAchecker is able to parse and analyze a large subset of (GNU)C.
If parsing fails for your program, please send a report to
cpachecker-users@googlegroups.com.

Verifying a Program with CPAchecker
-----------------------------------

1. Choose a source code file that you want to be checked.
   If you use your own program, remember to pre-process it as mentioned above.
   Example: `doc/examples/example.c` or `doc/examples/example_bug.c`
   A good source for more example programs is the benchmark set of the
   [International Competition on Software Verification](http://sv-comp.sosy-lab.org/),
   which can be checked out from https://github.com/sosy-lab/sv-benchmarks.

2. If you want to enable certain analyses like predicate analysis,
   choose a configuration file. This file defines for example which CPAs are used.
   Standard configuration files can be found in the directory config/.
   If you do not want a specific analysis,
   we recommend `config/default.properties`.
   However, note that if you are on Windows or MacOS
   you need to provide specifically-compiled MathSAT binaries
   for this configuration to work.
   The configuration of CPAchecker is explained in doc/Configuration.md.

3. Choose a specification file (you may not need this for some CPAs).
   The standard configuration files use `config/specification/default.spc`
   as the default specification. With this one, CPAchecker will look for labels
   named `ERROR` (case insensitive) and assertions in the source code file.
   Other examples for specifications can be found in `config/specification/`

4. Execute `scripts/cpa.sh [ -config <CONFIG_FILE> ] [ -spec <SPEC_FILE> ] <SOURCE_FILE>`
   Either a configuration file or a specification file needs to be given.
   The current directory should be the CPAchecker project directory.
   Additional command line switches are described in doc/Configuration.md.
   Example: `scripts/cpa.sh -config config/default.properties doc/examples/example.c`
   This example can also be abbreviated to:
   `scripts/cpa.sh -default doc/examples/example.c`
   Java 11 or later is necessary. If it is not in your PATH,
   you need to specify it in the environment variable JAVA.
   Example: `export JAVA=/usr/lib/jvm/java-11-openjdk-amd64/bin/java`
   for 64bit OpenJDK 11 on Ubuntu.
   On Windows (without Cygwin), you need to use `cpa.bat` instead of `cpa.sh`.

   Please note that not all analysis configurations are available for Windows and Mac
   because we do not ship binaries for SMT solvers for these platforms.
   You either need to build the appropriate binaries yourself
   or use less powerful analyses that work with Java-based solvers,
   for example this one instead of `-default`:
   `-predicateAnalysis-linear -setprop solver.solver=SMTInterpol`
   Of course you can also use solutions like the Windows Subsystem for Linux (WSL)
   or Docker for executing the Linux version of CPAchecker.

   If you installed CPAchecker using Docker, the above example command line would look like this:
   `docker run -v $(pwd):/workdir -u $UID:$GID registry.gitlab.com/sosy-lab/software/cpachecker -default /cpachecker/doc/examples/example.c`
   This command makes the current directory available in the container,
   so to verify a program in the current directory just provide its file name
   instead of the example that is bundled with CPAchecker.
   Output files of CPAchecker will be placed in `./output/`.

5. Additionally to the console output,
   an interactive HTML report is generated in the directory `output/`,
   either named `Report.html` (for result TRUE) or `Counterexample.*.html` (for result FALSE).
   Open these files in a browser to view the CPAchecker analysis result
   (cf. [`doc/Report.md`](doc/Report.md))

There are also additional output files in the directory `output/`:

 - `ARG.dot`: Visualization of abstract reachability tree (Graphviz format)
 - `cfa*.dot`: Visualization of control flow automaton (Graphviz format)
 - `reached.dot`: Visualization of control flow automaton with the abstract
    states visualized on top (Graphviz format)
 - `coverage.info`: Coverage information (similar to those of testing tools) in `Gcov` format
       Use the following command line to generate an HTML report as `output/index.html`:
       `genhtml output/coverage.info --output-directory output --legend`
 - `Counterexample.*.txt`: A path through the program that leads to an error
 - `Counterexample.*.assignment.txt`: Assignments for all variables on the error path.
 - `predmap.txt`: Predicates used by predicate analysis to prove program safety
 - `reached.txt`: Dump of all reached abstract states
 - `Statistics.txt`: Time statistics (can also be printed to console with `-stats`)
 
Note that not all of these files will be available for all configurations.
Also some of these files are only produced if an error is found (or vice-versa).
CPAchecker will overwrite files in this directory!


Validating a Program with CPA-witness2test
------------------------------------------

You can validate violation witnesses with CPA-witness2test, which is part of CPAchecker.

1. To do so, you need a violation witness, a specification file that fits the violation witness,
   and the source code file that fits the violation witness.
2. To validate the witness, execute the following command:
   ```
   scripts/cpa_witness2test.py -witness <WITNESS_FILE> -spec <SPEC_FILE> <SOURCE_FILE>`
   ```
   Addtional command line switches are viewed with `scripts/cpa_witness2test.py -h`.

3. When finished, and if the violation witness is successfully validated, the console output shows `Verification result: FALSE`.
   Additionally to the console output, CPA-witness2test also creates a file `output/*.harness.c`.
   This file can be compiled against the source file to create an executable test
   that reflects the violation witness.

Note that if the violation witness does not contain enough information to create an executable test,
the validation result will be `ERROR` and the console output will contain the following line:
`Could not export a test harness, some test-vector values are missing.`
