Computing Coverage
==================

The script `generate_coverage.py` implements the notion of coverage presented in the following tech report: [Verification Coverage](https://arxiv.org/abs/1706.03796)

The command-line arguments are documented in the script's help, but here is a quick overview of how it works.
The script generates a number of executions from beginning to end of the program and then calculates a coverage metric of an Assumption Automaton (which captures the progress achieved at the moment the execution of CPAchecker was interrupted).

The script takes as input:
- An Assumption Automaton (read instructions at the bottom to know how to produce one)
- The specification that was used for verification. For example: `config/specifications/ErrorLabel.spc`. This is used to be able to report whether any specification violations were found when attempting to compute coverage.
- The number of executions to generate before computing the coverage under-approximation.
- The directory where to keep the executions produced to compute coverage.
- Instance under analysis.

So, for example, the script can be called with the following command-line arguments:
```bash
./scripts/generate_coverage.py \
    -assumption_automaton_file scripts/post_processing/coverage/tests/aux_files/aa_three_paths_inner_if_both_blocks.spc \
    -spec config/specification/ErrorLabel.spc \
    -cex_count 10 \
    -cex_dir execution_samples \
    scripts/post_processing/coverage/tests/aux_files/three_paths.c
```

How to generate Assumption Automata?
------------------------------------

In order to generate Assumption Automata when execution is interrupted, you need to add the following option to the command line when calling CPAchecker:
`-setprop analysis.collectAssumptions=true`

However, as it is, the execution will most likely result in an error message like the following: 

```
Error: Invalid configuration (AssumptionStorageCPA needed for AssumptionCollectionAlgorithm) (AssumptionCollectorAlgorithm.<init>, SEVERE)
```

That is, we need to add the AssumptionStorageCPA to whatever other CPAs we are using. Appending options to the configuration you already have is [not yet supported](https://github.com/sosy-lab/java-common-lib/issues/9), therefore we have to do it manually.

Some examples:

In the case of predicate analysis (`config/predicateAnalysis.properties`) we have to add, after the configuration file:

`-setprop CompositeCPA.cpas=cpa.location.LocationCPA,cpa.callstack.CallstackCPA,cpa.functionpointer.FunctionPointerCPA,cpa.predicate.PredicateCPA,cpa.assumptions.storage.AssumptionStorageCPA,cpa.conditions.global.GlobalConditionsCPA`

In the case of explicit value analysis (`config/valueAnalysis.properties`) we have to add, after the configuration file:

`-setprop CompositeCPA.cpas=cpa.location.LocationCPA,cpa.callstack.CallstackCPA,cpa.functionpointer.FunctionPointerCPA,cpa.value.ValueAnalysisCPA,cpa.assumptions.storage.AssumptionStorageCPA,cpa.conditions.global.GlobalConditionsCPA`

The Assumption Automaton will be generated in `${output.path}/${assumptions.automatonFile}` (by default output/AssumptionAutomaton.txt).
