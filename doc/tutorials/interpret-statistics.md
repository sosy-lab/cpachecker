<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

# Interpreting Statistics for CPAchecker Results

Suppose we want to answer interesting questions like the following:
What portion of the analysis time is spent on predicate abstraction?

We take some results from [CPAchecker] and post-process the data with [BenchExec]
to have a nice and convenient representation.

## Overview: Quantile Plots, Scatter Plots, Tables

Since we are interested in predicate abstraction, we can use results from a nightly regression run:
[Results](https://buildbot.sosy-lab.org/cpachecker/results/nightly-predicate/00171.-r_integration-nightly-predicate.2020-09-20_22-00-18.results.html#/)
(BuildBot automatically prepares the tables for us.)

### Quantile Plots

Since we use BenchExec's table generator to layout the results, we also have quantile plots:
[Quantile Plot](https://buildbot.sosy-lab.org/cpachecker/results/nightly-predicate/00171.-r_integration-nightly-predicate.2020-09-20_22-00-18.results.html#/quantile?plot=Quantile%20Plot&selection=runset-0)

This is a presentation of the results for 6484 verification tasks.

Please first go to the top right and click on those items in the legend that you want to get rid of,
such that only three graphs remain: for `cputime`, `abstraction`, and `model enumeration`.

Let me explain the remaining four data series:
- cputime: time of the whole tool execution
- abstraction: time for computing a predicate abstraction for a given path formula
- model enumeration: time for processing all satisfying assignments
    (constructing an abstraction formula in DNF that is on-the-fly stored in a BDD)

The way in which CPAchecker computes predicate abstractions is described
in our article ([Unifying View], Section 3.1.6 Precision-Adjustment Operator, page 308)

What we see is that for most of the verification tasks, the predicate abstraction takes less then 10 % of the CPU time.
But for the rest it is above 10 % and can grow up to almost 100 %.
Thus, there are verification tasks for which the abstraction consumes the most time.

### Scatter Plots

The scatter plot also nicely illustrate this observation:
[Scatter Plot](https://buildbot.sosy-lab.org/cpachecker/results/nightly-predicate/00171.-r_integration-nightly-predicate.2020-09-20_22-00-18.results.html#/scatter?toolY=0&columnY=4)

Similarly, the model enumeration (one of the main parts of the algorithm to compute the boolean predicate abstraction)
takes usually less then 10 % of the time for abstraction, while for the rest it also goes up to 100 %.
Thus, there are extreme cases in which the model enumeration consumes almost all of the overall verification time.
[Scatter Plot](https://buildbot.sosy-lab.org/cpachecker/results/nightly-predicate/00171.-r_integration-nightly-predicate.2020-09-20_22-00-18.results.html#/scatter?toolY=0&columnY=7&toolX=0&columnX=4)

## Results of Individual Verification Runs

To inspect results for individual verification tasks, click on the "Table" tab. Here you can filter and sort results.
Clicking on a result in the the `status` column opens a log file from CPAchecker.

### Extremely Large Solving Time

This one seems interesting:

[Log File](https://buildbot.sosy-lab.org/cpachecker/results/nightly-predicate/00171.-r_integration-nightly-predicate.2020-09-20_22-00-18.logfiles/32_1_cilled_ok_nondet_linux-3.4-32_1-drivers--block--paride--bpck.ko-ldv_main0_sequence_infinite_withcheck_stateful.cil.out.yml.log)

What can we learn from the statistics (snippet of the stats repeated below)?

```
Number of abstractions:            71 (0% of all post computations)
    Boolean abstraction:               815.662s
    Model enumeration time:              7.590s
```

Boolean abstraction is usually computed in two steps:
- solve a formula   φ ∧ /\ p_i∈ρ (v_p_i ⇔ p_i)  // See [Unifying View] for explanation
- enumerate all models and assemble abstraction formula from it

In the above example, why was model enumeration inexpensive but the abstraction so expensive?
Because only solving the formula (finding the first satisfying assignment) was so expensive,
and there was only one satisfying assignment here.
```
    Solving time:                      808.056s (Max:   215.923s)
  Max number of models for allsat:        1
```

### Extremely Large Model-Enumeration Time

Here is an example where it is the other way around:  quick solving (30 s) but expensive model enumeration (754 s).
-> there are many predicates in the precision; complicated abstraction formula
[Log File](https://buildbot.sosy-lab.org/cpachecker/results/nightly-predicate/00171.-r_integration-nightly-predicate.2020-09-20_22-00-18.logfiles/elevator_spec9_productSimulator.cil.yml.log)

### Extremely Large Time for Checking Counterexamples

And here is an extreme example where the counterexample check consumes the majority of the time:
[Log File](https://buildbot.sosy-lab.org/cpachecker/results/nightly-predicate/00171.-r_integration-nightly-predicate.2020-09-20_22-00-18.logfiles/pals_opt-floodmax.5.4.ufo.BOUNDED-10.pals.c.v%2Blhb-reducer.yml.log)


## References:
[BenchExec]: https://www.sosy-lab.org/research/pub/2019-STTT.Reliable_Benchmarking_Requirements_and_Solutions.pdf
[CPAchecker]: https://doi.org/10.1007/978-3-642-22110-1_16
[Unifying View]: https://www.sosy-lab.org/research/pub/2018-JAR.A_Unifying_View_on_SMT-Based_Software_Verification.pdf
- [Reliable benchmarking: requirements and solutions][BenchExec]
- [CPAchecker: A Tool for Configurable Software Verification][CPAchecker]
- [A Unifying View on SMT-Based Software Verification][Unifying View]

## Appendix:
```
PredicateCPA statistics
-----------------------
Number of abstractions:            71 (0% of all post computations)
  Times abstraction was reused:    0
  Because of function entry/exit:  0 (0%)
  Because of loop head:            68 (96%)
  Because of join nodes:           0 (0%)
  Because of threshold:            0 (0%)
  Because of target state:         3 (4%)
  Times precision was empty:       2 (3%)
  Times precision was {false}:     0 (0%)
  Times result was cached:         1 (1%)
  Times cartesian abs was used:    0 (0%)
  Times boolean abs was used:      68 (96%)
  Times result was 'false':        2 (3%)
Number of strengthen sat checks:   0
Number of coverage checks:         5054
  BDD entailment checks:           90
Number of SMT sat checks:          0
  trivial:                         0
  cached:                          0

Max ABE block size:                       2887
Avg ABE block size:                                390.72 (sum: 27741, count: 71, min: 2, max: 2887)
Number of predicates discovered:          2
Number of abstraction locations:          0
Max number of predicates per location:    0
Avg number of predicates per location:    0
Total predicates per abstraction:         138
Max number of predicates per abstraction: 2
Avg number of predicates per abstraction: 2.03
Number of irrelevant predicates:          0 (0%)
Number of preds handled by boolean abs:   136 (99%)
  Total number of models for allsat:      66
  Max number of models for allsat:        1
  Avg number of models for allsat:        0.97

Time for post operator:                                2.997s
  Time for path formula creation:                      2.949s
Time for strengthen operator:                          0.160s
Time for prec operator:                              820.875s
  Time for abstraction:                820.803s (Max:   217.577s, Count: 71)
    Boolean abstraction:               815.662s
    Solving time:                      808.056s (Max:   215.923s)
    Model enumeration time:              7.590s
    Time for BDD construction:           0.020s (Max:     0.009s)
Time for merge operator:                               0.389s
Time for coverage checks:                              0.003s
  Time for BDD entailment checks:                      0.001s
Total time for SMT solver (w/o itp):   815.646s
````

