<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

CPAchecker configuration
========================

Configuration of CPAchecker is done via command-line arguments and
configuration files.

Configuration files
-------------------
A configuration file specify a set of options in `key=value` format.
All valid configuration options and their default values
are listed in the file [`ConfigurationOptions.txt`](ConfigurationOptions.txt).
In most cases, the standard configuration files
in the directory [`config/`](../config/) should be sufficient.
A detailed format description can be found in the section below.


Command-line arguments
----------------------
Several configuration options can also be specified as command-line arguments.
If an option appears on the command line as well as in the configuration file,
the value from the command line overrides the one from the file.
The following command-line arguments are allowed:

 - `--help`			print list of command-line argumments and exit
 - `--config <FILE>`		sets configuration file name
 - `--cpas <CPAS>`		sets `cpa = cpa.composite.CompositeCPA` and `CompositeCPA.cpas = <CPAS>`
 - `--spec <FILE>`		sets `specification = <FILE>`
 - `--output-path <DIR>`	sets `output.path = <DIR>`
 - `--benchmark`		sets `coverage.enabled = false`, `output.disable = true`, `statistics.memory = false`, and disables assertions in CPAchecker for improved performance
 - `--no-output-files`		sets `output.disable=true`
 - `--stats`			sets `statistics.print = true`
 - `--entry-function <FUNC>`	sets `analysis.entryFunction = <FUNC>`
 - `--timelimit <TIME>`		sets `limits.time.cpu = <TIME>`
 - `--32`			sets `analysis.machineModel = LINUX32` (this is the default and suitable for 32-bit Linux on x86, i.e. ILP32)
 - `--64`			sets `analysis.machineModel = LINUX64` (this is suitable for 64-bit Linux on x86, i.e., LP64)
 - `--skip-recursion`		sets `cpa.callstack.skipRecursion = true` and `analysis.summaryEdges = true`
 - `--preprocess`		sets `parser.usePreprocessor = true`
 - `--java`  			sets `language = JAVA`
 - `--secure-mode`		enables a secure mode which forbids some configuration options that would allow arbitrary code execution
 - `--jvm-debug` 		enables the JVM debug interface on TCP port 5005 for remote debugging
 - `--disable-java-assertions`	disables assertions in CPAchecker for improved performance (recommended for benchmarking)
 - `--heap HEAP_SIZE`		sets the heap size of the JVM
 - `--stack STACK_SIZE`		sets the stack size of the JVM
 - `--option <KEY>=<VALUE>`	sets any option: `KEY = VALUE`

The file [`ConfigurationOptions.txt`](ConfigurationOptions.txt) contains an explanation
of these options.
The arguments `--config config/CONFIGFILE.properties` can be
abbreviated to `--CONFIGFILE`. In other words, if CPAchecker finds an
unknown command-line argument, it checks if a file with this name
and the ending `.properties` exists in the directory [`config/`](../config)
and uses it as the configuration file.

All other arguments to CPAchecker are interpreted as code files that should be
analyzed (option `analysis.programNames`).

If neither `--cpas` nor a configuration file is specified,
CPAchecker will use a default configuration
that is recommended for most use cases.
Typical command lines for CPAchecker are thus for example:
- `bin/cpachecker doc/examples/example.c` (uses default analysis, which can also explicitly be requested with `--default`)
- `bin/cpachecker --kInduction doc/examples/example.c` (chooses configuration `kInduction`)
- `bin/cpachecker --config config/kInduction.properties doc/examples/example.c` (same as previous example)

Note that before version 2.4 CPAchecker supported only
a different set of command-line arguments, each starting with a single dash.
These still work but are deprecated, and CPAchecker will print warnings
informing about how to replace them when they are used.


Specifying the CPA(s)
---------------------
The CPA that CPAchecker uses is specified with the `cpa` option (default:
`cpa.composite.CompositeCPA`). The syntax of the value is `package.ClassName Alias`,
where the alias is an optional unique identifier for this instance of the
CPA. Without an alias, the class name is used as identifier. Configuration
options that should be used for only one instance of a CPA can be prefixed
with "alias.". Their values override the options without this prefix.

For example, to set the solver logfile in the PredicateCPA, you can use
`PredicateCPA.solver.logfile=predicate.%03d.smt2`, even if the real option is
`solver.logfile` and there is no such option in the PredicateCPA.
The configuration framework will automatically remove the prefix alias and
apply the renamed option for all classes that are instantiated
as part of the PredicateCPA.

If the package name starts with `org.sosy_lab.cpachecker.`, this prefix can be
omitted.

Wrapper CPAs like ARGCPA and CompositeCPA take one option `cpa` or `cpas`
to specify the wrapped CPA, depending whether this CPA wraps one or
several other CPAs (the latter is only used for CompositeCPA). This option
has to be prefixed with the identifier of the CPA as described above.

A simple example (the first line could be omitted as it is the default):

```
cpa = cpa.composite.CompositeCPA
CompositeCPA.cpas = cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.value.ValueAnalysisCPA
cpa.value.merge = "SEP"
```

A more complex example:

```
cpa = cpa.arg.ARGCPA arg
arg.cpa = cpa.composite.CompositeCPA composite
composite.cpas = cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.automaton.ObserverAutomatonCPA ErrorLabelAutomaton, cpa.predicate.PredicateCPA
ErrorLabelAutomaton.cpa.automaton.inputFile = config/specification/ErrorLabel.spc
```

Note that instead of manually specifying an `ObserverAutomatonCPA`, you can
use the option `specification` (or the equivalent command-line argument `--spec`).
The following example is identical to the last one:

```
cpa = cpa.arg.ARGCPA arg
arg.cpa = cpa.composite.CompositeCPA composite
composite.cpas = cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.predicate.PredicateCPA
specification = config/specification/ErrorLabel.spc
```

If the option `specification` is used, CPAchecker will create CPA instances
for each of the automata within the specification
and insert them in an appropriate place in the tree of CPAs,
usually below a CompositeCPA instance.
To override this behavior
and specify where the CPAs for specification automata should be inserted,
use the placeholder `$specification` in the appropriate place.
Thus the following example is again identical to the previous two:

```
cpa = cpa.arg.ARGCPA arg
arg.cpa = cpa.composite.CompositeCPA composite
composite.cpas = cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.predicate.PredicateCPA, $specification
specification = config/specification/ErrorLabel.spc
```

Configuration file format
-------------------------

The configuration file format is a text-based format
that is inspired by Java property files and Windows `ini` files.
Each option is given on a separate line.
Whitespaces around the key and the value are ignored
(so `key=value` and ` key = value ` are the same).
Lines starting with `//` or `# ` are comments.
If a line ends with a backslash (`\`),
the content of the next line is appended to the current line.
If an option supports multiple values, separate them with a comma
(optionally with whitespaces around the comma).

The format supports optional sections
which start with `[Section.Name]` on a separate line.
The key of all options inside a section will be prefixed
with `Section.Name.` (note the leading dot).
Options before the first section header
and those in a section with an empty name (`[]`)
will be used just with their regular names (without prefix).
The same section name can be used several times in a file.

Other configuration files can be included with `#include file`.
Options from included files will be overwritten by options
with the same names in the including file,
no matter where the include statement is placed.
Sections of the including file have no effect on the treatment
of options in the included file, and vice versa.

All relative paths specified in a configuration file
(e.g., for `#include` or for values of options like `specification`)
are interpreted as relative to the directory of the respective configuration file.
