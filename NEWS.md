<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

Changes since CPAchecker 2.1.1
------------------------------
* Preparation for Java 18  
  CPAchecker before revision 39743 might not work correctly if
  - Java 18 or newer is used,
  - the system's default encoding is different from UTF-8
    (this is usually the case on Windows, but uncommon on Linux and Mac), and
  - non-ASCII characters appear in input files or are otherwise relevant during the analysis.
  This is due to a change in Java, for full details cf. [JEP 400](https://openjdk.java.net/jeps/400).
  Note that due to the same change, textual output files of CPAchecker
  on non-UTF-8 machines will be in the system encoding if Java 17 or older is used
  and in UTF-8 if Java 18 or newer is used
  (this behavior is the same for all versions of CPAchecker).


Changes from CPAchecker 2.1 to CPAchecker 2.1.1
-----------------------------------------------
* CPAchecker can be used as abstract model explorer
  and precision refiner for component-based CEGAR
  (cf. "Decomposing Software Verification into Off-the-Shelf Components:
  An Application to CEGAR", Proc. ICSE, 2022, to be released).


Changes from CPAchecker 2.0 to CPAchecker 2.1
---------------------------------------------
* Interpolation-Sequence based Model Checking (ISMC)  
  A new reachability-safety analysis (config `-bmc-interpolationSequence`),
  which adopts a verification algorithm for hardware
  proposed by Yakir Vizel and Orna Grumberg
  (cf. ["Interpolation-sequence based model checking", Proc. FMCAD, 2009](https://doi.org/10.1109/FMCAD.2009.5351148))
  to software, has been added to CPAchecker.

* Self-contained HTML reports  
  The [HTML report](https://cpachecker.sosy-lab.org/counterexample-report/ErrorPath.0.html)
  that CPAchecker creates for the analysis result
  (for example showing the counterexample)
  now is a fully self contained file with all dependencies bundled.
  This means one does no longer need Internet access to open one.


Changes from CPAchecker 1.9.1 to CPAchecker 2.0
-----------------------------------------------
* Better support for Windows  
  We now bundle binaries of SMT solvers like MathSAT and Z3 for Windows,
  such that most configurations of CPAchecker work on Windows out of the box.

* REUSE compliance  
  CPAchecker now follows the [licensing best practices](https://reuse.software/)
  of the FSFE and is [REUSE compliant](https://api.reuse.software/info/gitlab.com/sosy-lab/software/cpachecker),
  i.e., everything in the repository is labeled with machine-readable
  headers that include copyright and license information.
  This makes it easy to check the licenses of all CPAchecker-internal and
  third-party components and ensure that all requirements such as bundling
  license texts and copyright notices are fulfilled
  when redistributing CPAchecker.
  More information about the license status can be found in [README.md](README.md).

* Interpolation-based Model Checking (IMC)  
  A new reachability-safety analysis (config `-bmc-interpolation`),
  which adopts a state-of-the-art verification algorithm
  for hardware proposed by K. L. McMillan
  (cf. ["Interpolation and SAT-Based Model Checking", Proc. CAV, 2003](https://doi.org/10.1007/978-3-540-45069-6_1))
  to software, has been added to CPAchecker.

* Automated Fault Localization  
  CPAchecker now supports multiple techniques for automatic fault-localization.
  If fault localization is enabled and CPAchecker finds a counterexample during
  analysis, CPAchecker will mark likely faults in the program that lead to that counterexample.
  Fault-localization results are presented in the produced HTML reports
  (`Counterexample.*.html`).
  The following fault-localization configurations exist:

    * [Coverage-based fault localization](https://ieeexplore.ieee.org/abstract/document/4041886):
      `-setprop analysis.algorithm.faultLocalization.by_coverage=true`
    * [Interpolation-based fault localization](https://link.springer.com/chapter/10.1007/978-3-642-32759-9_17):
      `-setprop analysis.algorithm.faultLocalization.by_traceformula=true`
    * [Distance metrics](https://dl.acm.org/doi/abs/10.1145/1029894.1029908):
      `-setprop analysis.algorithm.faultlocalization.by_distance=true`


Changes from CPAchecker 1.9 to CPAchecker 1.9.1
-----------------------------------------------
CPAchecker 1.9.1 celebrates our SVN revision 33333,
which was recently committed. :-)

Thank you very much to all the contributors
who have made this possible (cf. [Authors.md](Authors.md) for full list)!

Most important changes:

* Java 11 or later is required now.

* There is now an official Docker image for CPAchecker,
  cf. [INSTALL.md](INSTALL.md) for more details.

* CPAchecker now supports the changes made to the SV-Benchmarks repository
  in early 2020, e.g., the replacement of `__VERIFIER_error` and `__VERIFIER_assume`.

* Fix termination analysis, which did not work if CPAchecker was started
  from a different directory than its project directory.

* Witness export for multi-threaded programs is improved,
  such that a following validation performs faster.

* CPAchecker now supports verifying programs that make use of 128-bit types.


Changes from CPAchecker 1.8 to CPAchecker 1.9
---------------------------------------------
* CPAchecker 1.9 is the last release that works on Java 8,
  future versions of CPAchecker will require Java 11 or newer.

* CPAchecker can now use the new BDD library [ParallelJBDD](https://gitlab.com/sosy-lab/software/paralleljbdd)
  as replacement for JavaBDD if the option `bdd.package = PJBDD` is used.

* Support for Cyclic Analysis Combinations  
  Next to sequential combinations and parallel combinations, CPAchecker now
  also provides an algorithms for cyclic combinations of analyses that can
  be e.g. be used to interleave analyses or iteratively execute a sequence.
  of analyses. Different modes are supported. For example, analyses may
  resume their previous exploration, start from scratch.

* Cooperative Verifier-based Testing (CoVeriTest)  
  CoVeriTest uses a cyclic combination of analyses to generate test cases
  for standard coverage criteria like statement or branch coverage. Also,
  the coverage properties of the international competition on software
  testing (Test-Comp) are supported. The specialty of CoVeriTest is that
  it cannot only configure the analyses participating in test-generation
  as well as their individual time limits in each iteration, but also which
  information are exchanged between different analysis runs. For details,
  we refer to the main CoVeriTest publication:
  Beyer, D.; Jakobs, M.-C.: CoVeriTest: Cooperative Verifier-Based Testing.
  In: Proc. FASE, Springer, 2019.
  CoVeriTest also participated in Test-Comp'19 and won the 3rd place.


Changes from CPAchecker 1.7 to CPAchecker 1.8
---------------------------------------------
* Support for Algorithm Selection  
  CPAchecker can now analyze the given program
  and select an appropriate configuration depending on program features
  such as whether loops are contained and which data types are used.

* Improved Witness Export and Validation  
  The termination analysis has been enhanced to produce a violation witness,
  when it detects that a program does not always terminate. Moreover, the
  witness validator has been updated to support the validation of violation
  witnesses for termination.

* Execution-based Witness Validation and Harness Generation  
  A new, execution-based witness validation has been added to CPAchecker,
  called CPA-witness2test (CPA-w2t). It can be used to create executable
  tests from a given violation witness.
  See the help dialog of `scripts/cpa_witness2test.py` for more information.
  As an addition, CPAchecker can also be directly used to create
  compilable test harnesses for found property violations
  using configuration option `counterexample.export.exportHarness = true`.

* Reducers  
  The cooperation of CPAchecker with other verifiers has been extended.
  To sequentially combine CPAchecker with another verifier via conditional
  model checking, the other verifier no longer needs to understand CPAchecker's
  condition format. Instead, one can use one of the reducers implemented in CPAchecker
  as preprocessor for the other verifier and let the reducer transform the
  condition into a residual program (C code).
  (cf. "Reducer-Based Construction of Conditional Verifiers".
  Dirk Beyer, Marie-Christine Jakobs, Thomas Lemberger, Heike Wehrheim.
  In Proc. ICSE, ACM, 2018).

* Block Abstraction memoization for multi-core machines  
  BAM combined with, e.g., Value Analysis or Interval Analysis
  can analyze a task in parallel and thus benefit from a multi-core machine
  (cf. "Domain-Independent Multi-threaded Software Model Checking".
  Dirk Beyer, Karlheinz Friedberger. In Proc. ASE, ACM, 2018).

* Analyses based on Slicing Abstractions  
  CPAchecker now supports two variants of abstraction slicing.
  The first represents the program counter symbolically (cf. "Slicing Abstractions",
  Brueckner, Draeger, Finkbeiner, Wehrheim, Fundamenta Informaticae, 2008). The
  second (cf. "Splitting via Interpolants", Ermis, Hoenicke, Podelski, VMCAI, 2012)
  treats the program counter explicitly.


Changes from CPAchecker 1.6.1 to CPAchecker 1.7
-----------------------------------------------
* CPAchecker requires Java 8.

* New Default Configuration `-default`  
  This configuration should work reasonably well across a large range
  of programs and properties and is recommended in general,
  unless a more specific configuration for a certain use case exists
  (for example, for SV-COMP tasks the configuration '-svcomp18'
  is better suited).

* New Command-Line Parameter `-benchmark`  
  This parameter should always be used when running CPAchecker for
  (performance) benchmarking.
  For example, it disables internal assertions and output files
  for improved performance (cf. [doc/Benchmark.md](doc/Benchmark.md)).

* Termination Analysis  
  An analysis that supports termination properties
  and is based on finding lassos has been added to CPAchecker.

* Overflow Analysis  
  An analysis that supports finding overflows
  and is based on predicate abstraction has been added to CPAchecker.

* Improved Precision of Predicate Analysis  
  The predicate analysis now uses an improved encoding of program semantics
  that is more precise for bitvector operations, overflows, and pointers.
  The default SMT solver is now MathSAT5, for which only a Linux binary is bundled.
  Additional binaries are available from the [MathSAT homepage](http://mathsat.fbk.eu/).
  Please note that the bundled license of MathSAT permits
  only research and evaluation purposes.

* Improved HTML Report  
  The HTML report with verification results that is generated by CPAchecker
  has been updated and is now produced directly by CPAchecker
  (no need to call report-generator.py anymore).

* Improved Witness Validation  
  The witness validator has been updated and now supports the validation of
  violation witnesses for concurrent programs.


Changes from CPAchecker 1.6 to CPAchecker 1.6.1
-----------------------------------------------
* Important bug fix for all configurations that use a sequential combination
  of analyses, for the example the `-sv-comp16` configuration
  (internal time limits were not handled correctly).


Changes from CPAchecker 1.4 to CPAchecker 1.6
---------------------------------------------
* Local Policy Iteration  
  A new analysis has been added to CPAchecker
  that derives numerical invariants from linear templates
  with policy iteration in an efficient manner.

* Formula Slicing Analysis  
  A new analysis was developed for CPAchecker that derives inductive invariants
  from loop-free traces from the analyzed program.

* Refinement Selection  
  The value analysis and the predicate analysis can now use
  refinement selection for choosing well-suited ways to refine
  the analysis for an infeasible counterexample.

* Symbolic Execution  
  CPAchecker now supports symbolic execution
  by enhancing the value analysis to not only track explicit,
  but also symbolic values.

* Improved Heap Support for Predicate Analysis  
  The predicate analysis now supports a heap analysis
  with unbounded memory regions by using the SMT theory of arrays.
  This can be enabled with `cpa.predicate.handleHeapArray=true`.

* Concurrency Support  
  CPAchecker now supports the analysis of concurrent programs
  with a limited number of threads
  by using value analysis or BDD-based analysis.

* Witness Export and Validation  
  After an analysis, CPAchecker exports witnesses
  that contain information about found counterexamples or correctness-proofs.
  The format of the witnesses is standardized and
  a description is [available](https://sv-comp.sosy-lab.org/2016/witnesses).

* Support for Verification Tasks with Multiple Source Files  
  Multiple C source files can be given as input to CPAchecker,
  and they will be linked together and analyzed as a single program.

* BenchExec  
  The benchmarking script (scripts/benchmark.py) of CPAchecker
  evolved into the independent project [BenchExec](https://github.com/sosy-lab/benchexec).
  It provides reliable benchmarking and resource measurement
  for arbitrary tools.

* JavaSMT  
  The SMT interface layer of CPAchecker got refactored into its own library
  [JavaSMT](https://github.com/sosy-lab/java-smt)
  and can now be used by other projects as well.
  Because of this, a lot of configuration options related to solver usage
  were renamed from `cpa.predicate.*` to `solver.*`,
  so please check your configuration if necessary.

* 32bit binaries of native libraries and tools removed  
  If you need to run CPAchecker on a 32bit system with a configuration
  that relies on native libraries, you need to compile them yourself
  and put them in the directory `lib/native/x86-linux/`
  (cf. documentation of JavaSMT).


Changes from CPAchecker 1.3.4 to CPAchecker 1.4
-----------------------------------------------
* Sliced Interpolation for Value Analysis  
  The refinement for value analysis now uses an improved interpolation procedure
  that allows to choose better interpolants
  and thus increases the performance of the analysis.

* Continuously-Refined Invariants for k-Induction  
  The k-induction-based analysis can now be supplied
  with a sequence of increasingly precise invariants throughout the analysis,
  leading to a more efficient and effective combination
  of k-induction and invariant generation.

* Counterexample Checks  
  CPAchecker provides the ability to double-check a counterexample
  found by one analysis with a different analysis,
  in order to decrease the number of false alarms with only little overhead.
  The second analysis can usually be more precise,
  because it is only used for loop-free paths of the original program.
  This is now enabled by default for the predicate analysis
  (the counterexample check is done by the value analysis)
  and the value analysis (the counterexample check is done by
  a bitprecise configuration of the predicate analysis).
  This combines the respective advantages of both analyses.

* Floating-Point Arithmetic with Predicate Analysis  
  The predicate analysis has got support for precise modeling
  of floating-point arithmetic (in config `-predicateAnalyis-bitprecise`),
  thanks to the addition of support for this in the SMT solver MathSAT5.

* The default configuration of the predicate analysis is now more precise:
  it uses integers to approximate int variables instead of rationals.
  Overflows and bit operators like shifts are still not handled,
  the (slower) configuration `-predicateAnalysis-bitprecise`
  can be used for this.

* The default SMT solver is now SMTInterpol,
  which is written in Java and available on all platforms.
  MathSAT5 continues to be used for configurations
  that SMTInterpol does not support.

* The SMT solver [Princess](http://www.philipp.ruemmer.org/princess.shtml)
  has been integrated into CPAchecker's predicate analysis
  and can be selected with `cpa.predicate.solver=princess`.

* The configuration files for value analysis have been renamed.
  Instead of `-explicitAnalysis` or similar,
  you now need to use `-valueAnalysis`.


Changes from CPAchecker 1.2 to CPAchecker 1.3.4
-----------------------------------------------

Main changes:

* Conditional Model Checking (CMC)  
  The flexibility of sequential combinations of analyses inside CPAchecker has been extended.
  A configuration based on this won two categories of SV-COMP'14
  and a silver medal in the category Overall
  (cf. [results overview](http://sv-comp.sosy-lab.org/2014/results/)).

* Symbolic Memory Graphs (SMG)  
  An analysis that models the heap precisely as SMGs has been added
  and can be used for finding memory violations.
  It won the category MemorySafety of SV-COMP'14
  (cf. [results overview](http://sv-comp.sosy-lab.org/2014/results/)).

* Precision Reuse  
  The explicit-value analysis and the predicate analysis have gained support for precision reuse,
  a technique that allows much faster regression verification,
  i.e., verification of a new version of a program
  (cf. "Precision Reuse for Efficient Regression Verification".
  Dirk Beyer, Stefan Löwe, Evgeny Novikov, Andreas Stahlbauer, and Philipp Wendler.
  In Proc. ESEC/FSE, ACM, 2013).

* Domain Types  
  A suitable abstract domain can be selected automatically
  depending on the usage pattern of each variable,
  for example a BDD or explicit-value analysis
  (cf. "Domain Types: Abstract-Domain Selection Based on Variable Usage".
  Sven Apel, Dirk Beyer, Karlheinz Friedberger, Franco Raimondi, and Alexander von Rhein.
  In Proc. HVC, LNCS 8244, Springer, 2013).

* k-Induction  
  A proof method based on k-Induction (using an SMT solver) has been added
  and can be combined with our bounded-model-checking implementation
  (config `-bmc-induction`).

* Predicate Analysis  
  A much improved pointer handling is implemented and enabled by default.
  Furthermore, support for a bitprecise handling of all int variables has been added
  (including overflows and bitwise operators).
  This configuration is available as `-predicateAnalysis-bitprecise`.

* Google App Engine  
  CPAchecker has been successfully ported to the Google App Engine,
  accessible via a [web frontend](http://cpachecker.appspot.com) and a JSON API.

* Improved C Frontend  
  Support for many additional C features has been added to CPAchecker.
  Pre-processing with CIL is not necessary any more.
  CPAchecker can be given several C files at once
  and links them together before verifying them as a single program.
  This simplifies verification of programs consisting of multiple source files.

* Experimental Java Support  
  A Java frontend has been added to CPAchecker,
  and some analyses were extended to support verification of Java programs.
  This is still experimental and several language features are still missing
  (e.g., exceptions).


Further changes:

* Java 7 is required now.

* Specification  
  Support for property files of the SV-COMP added, specify them with `-spec <FILE.prp>`
  (cf. [SV-COMP rules](http://sv-comp.sosy-lab.org/2014/rules.php)).

* Configuration-File Changes:  
  File names are now relative to the file in which they are given,
  several CPAs have been renamed (ABMCPA -> BAMCPA, ExplicitCPA -> ValueAnalysisCPA),
  many changes to other configuration options as well.
  If you use your own configuration files, you will need to adjust them
  (cf. doc/ConfigurationOptions.txt)

* Error Paths  
  Multiple error paths can be searched and written to disk
  with the option `analysis.stopAfterError = false`.
  More information about variable assignments has been added to the error paths.
  The use of the report generator has been simplified,
  just call scripts/report-generator.py (cf. doc/BuildReport.txt).

* SMT solvers for Predicate Analysis:  
  [SMTInterpol](http://ultimate.informatik.uni-freiburg.de/smtinterpol/index.html)
  is now well integrated.
  The support for MathSAT4 was dropped (MathSAT5 continues to be the default solver).

* Benchmarking Support  
  CPAchecker provides scripts for benchmarking with large sets of programs.
  These have been extended and now provide more precise time and memory measurement
  (using Linux cgroups). Also the generated HTML tables have more features now.
  (cf. doc/Benchmark.txt)


Changes from CPAchecker 1.1 to CPAchecker 1.2
---------------------------------------------

CPAchecker now supports several new analyses:

* CEGAR for ExplicitCPA (as submitted to SV-COMP'13)  
  (c.f. "Explicit-State Software Model Checking Based on CEGAR and Interpolation", to appear in FASE'13)
  This can be enabled with the `explicitAnalysis` and `explicitAnalysis-ItpRefiner*` configurations
  (`explicitAnalysis-ItpRefiner-ABElf` is recommended)

* Conditional Model Checking (CMC)  
  (c.f. "Conditional Model Checking: A Technique to Pass Information between Verifiers", FSE'12)
  To use two or more CMC-enabled configurations, use the `-cmc` command-line argument:
  Example: `-cmc explicitAnalysis-100s-generate-cmc-condition -cmc predicateAnalysis-use-cmc-condition`

* Sequential composition of analyses (as submitted to SV-COMP'13)  
  The `-cmc` command-line argument can be used for this, too.
  Example: `-cmc explicitAnalysis-100s -cmc predicateAnalysis`

* Predicate-based analysis using the Impact refinement strategy  
  (c.f. "Algorithms for Software Model Checking: Predicate Abstraction vs. IMPACT", FMCAD'12)
  This can be enabled with the `predicateAnalysis-ImpactRefiner-*` configurations.

* BDD-based analysis tracking a subset of variables,
  with ExplicitCPA tracking the remaining variables.
  This can be enabled with the `explicit-BDD-*` configurations.

Other changes to CPAchecker:
* Pre-processing of C files with CIL is no longer needed.
* MathSAT5 is now used as as SMT solver by default.
  MathSAT4 can still be selected by setting the option
  `cpa.predicate.solver=Mathsat4`
* In configuration files, #include directives can be used
  to include other configuration files.
* The file format of the file with the initial set of predicates
  (option cpa.predicate.abstraction.initialPredicates) and
  the final set of predicates (option cpa.predicate.predmap.file) was changed.
  It is now the same format for both files, and based on the SMTlib2 format.
  See doc/examples/predmap.txt for an example.
* The option cpa.predicate.machineModel was renamed to analysis.machineModel.
* The Cudd BDD library was removed, now JavaBDD's implementation is used by default
  (it has similar performance, but more features).
* The ARTCPA was renamed to ARGCPA.
  Replace cpa.art with cpa.arg and ARTCPA with ARGCPA in existing configuration files.
* The option analysis.traversal.useTopsort (used in most configuration files)
  was renamed to analysis.traversal.useReversePostorder as this name is more precise.
* SMTInterpol, an SMT solver written in Java, is now integrated into CPAchecker.
  With this solver, predicate analysis works on all platforms.
  Some configuration options were renamed in order to not be MathSAT-specific.
* The log level for the console can now be adjusted during runtime.
  Use a JMX client to do that, e.g., jconsole or VisualVM.
  Connect to the CPAchecker process,
  locate the MXBean `org.sosy_lab.common:type=LogManager`,
  and set the attribute.
* The option `cpa.removeIrrelevantForErrorLocations` was renamed to
  `cfa.removeIrrelevantForSpecification`, as this name is more appropriate.
* A time limit of 15 minutes is now enabled by default in most configurations.
  If the analysis is not yet finished, CPAchecker will stop after this time and report UNKNOWN.
  The time limit can be controlled with the `cpa.conditions.global.time.wall` option
  and the `-timelimit` command-line argument.
  Example: `scripts/cpa.sh -predicateAnalysis -timelimit 1min test.c`
* If the `#include` directive of specification automata is used with relative paths,
  the base directory of the relative path is now the directory of the file which contains the `#include`,
  not the CPAchecker root directory.
  If `#include` is used with relative paths in a specification file, it most probably needs adjustment.
