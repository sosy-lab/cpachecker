<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

Benchmarking CPAchecker
=======================

CPAchecker provides a command-line flag `-benchmark`
that should be used for all kinds of benchmarks.
It improves performance by disabling assertions and optional features
such as extended statistics and output files.
Note that you can re-enable specific output files individually if desired.

In addition, care needs to be taken to specify meaningful resource limits
and some other options depending on the specifics of the verification task,
as described in the following.

Measurements
------------
For benchmarking, it is recommended to use
[BenchExec](https://github.com/sosy-lab/benchexec).
An extended version of BenchExec is bundled with CPAchecker
and usable by calling `scripts/benchmark.py`.
This script should run with any Python version >= 3.5.
This script takes as input one or more XML files which specify the tool,
the resource limits, the tool configuration, the input programs
and the columns that should appear in the output table.
The script puts all the results into the test/results/ directory.
Commented examples for these XML files are given in the BenchExec 
[documentation](https://github.com/sosy-lab/benchexec/blob/master/doc/INDEX.md).
The file [./examples/benchmark-cpachecker.xml](./examples/benchmark-cpachecker.xml)
can be used as base for own benchmarks.
Several useful benchmark configurations are in `test/test-sets/*.xml`.

For further help on the benchmark script use `scripts/benchmark.py -h`.

Specifications
--------------
If the benchmark script should evaluate whether the returned results
are correct or wrong and calculate scores like in the SV-COMP,
there needs to be a specification defined in the benchmark definition
that tells the script what should be checked (error label reachability,
memory safety, termination, etc.).
The specification is given in a property file (`.prp`) just like in the SV-COMP.
There are examples for such files in `test/programs/benchmarks/property*.prp`.
To specify them in the benchmark, use the `<propertyfiles>` tag.
It can occur either in global scope or inside a `<tasks>` tag.
We provide common program properties in `config/properties`.
You do not need to specify a separate command-line option
with the specification for the verifier,
as the script will automatically add it to the command line.

Specifying Machine Model and Further Program Characteristics
------------------------------------------------------------

It is important to specify the correct values for the following options
according to the characteristics of the input files to avoid wrong answers:

- Machine model (32 vs. 64 bit):
  32-bit model is assumed by default.
  For 64 bit, specify `-64` on the command line.

- Whether `malloc` may return null or not:
  If the program assumes `malloc` never returns null
  and you are using a predicate-based analysis,
  set `cpa.predicate.memoryAllocationsAlwaysSucceed=true`.
  Note that this assumption is true for all SV-Comp files,
  thus this option is already set in all `-svcomp*` configurations.


Specifying Resource Limits
--------------------------
When benchmarking CPAchecker, it is important to correctly specify
the time and memory limits.

- *CPU Time*.
  Firstly, in order to get statistics even in case of a timeout,
  is is important to specify different "soft" and "hard" CPU-time limits
  like in this example:
  `<benchmark timelimit="900s" hardtimelimit="1000s" ...`
  The soft time limit is automatically passed as parameter to CPAchecker,
  so there is no need to specify the `-timelimit` option manually.

- *Memory*.
  The memory limit is specified in SI units (i.e., 1 MB = 1,000,000 Bytes)
  with the attribute `memlimit` in the `<benchmark>` tag
  of the benchmark definition XML file. Example:
  `<benchmark ... memlimit="8000 MB">`
  This limit will be enforced by the OS
  and CPAchecker will be killed if it needs more memory.

Additionally, it is important to specify the amount of memory
that Java uses for its own heap with the `-heap` command-line parameter.
This value needs to be lower than the external limit.
Setting it too low will hurt the performance due to increased garbage collection
and provoke `OutOfMemoryError`,
setting it too high limits the memory that is available to native libraries
such as MathSAT.
For analyses without MathSAT,
start to experiment with 1000 MB less than the external limit.
IMPORTANT: Java does not use SI units here, but IEC units (factor 1024).
`7000M` here are 5% more than `7000 MB` for the memory limit above!
Example:
`<option name="-heap">7000M</option>`

Summary:
For correct and useful benchmarks, choose a memory limit (e.g., 8000MB),
a Java heap size (e.g., 7000MiB), and a timelimit (e.g., 900s).
Then specify them as follows:

```xml
<benchmark ... timelimit="900s" hardtimelimit="1000s" memlimit="8000 MB">
  <option name="-heap">7000M</option>
  ...
</benchmark>
```


Result Table Generation
-----------------------
In order to combine the results from several benchmark runs into
a single table the script scripts/table-generator.py can be used.
It also takes an XML file as input that gives the table configuration
and is described in the
[table-generator documentation](https://github.com/sosy-lab/benchexec/blob/master/doc/table-generator.md).

The output will be tables in HTML and CSV format,
the former having some additional features like showing the log files
and being able to plot the results.
