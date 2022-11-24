<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

Fault Localization
-----------------

CPAchecker provides a variety of fault localization techniques. These techniques aim at finding
error-prone statements in the program. Additionally, they try to explain the results to ease
debugging.

Please conduct the following papers for further reading:
- [Distance Metrics I](https://doi.org/10.1145/1029894.1029908)
- [Distance Metrics II](https://doi.org/10.1007/s10009-005-0202-0)
- [MaxSat](https://doi.org/10.1145/1993498.1993550)
- [Error Invariants](https://doi.org/10.1007/978-3-642-32759-9_17)
- [Ochiai](https://doi.org/10.1145/1831708.1831715)
- [DStar](https://doi.org/10.1109/TR.2013.2285319)
- [Tarantula](https://doi.org/10.1145/1831708.1831717)

Running Fault Localization
----------

The above-mentioned algorithms can be run by executing one of the following options:
```
-predicateAnalysis-faultlocalization-distanceMetrics
-predicateAnalysis-faultlocalization-errinv
-predicateAnalysis-faultlocalization-maxsatorg
-predicateAnalysis-faultlocalization-tarantula
-predicateAnalysis-faultlocalization-dstar
-predicateAnalysis-faultlocalization-ochiai
```

Here is a full example for running `ErrorInvariants`:

```
-predicateAnalysis-faultlocalization-errinv \
-spec config/specification/sv-comp-reachability.spc \
<path to program>
```

Open the report in `output/` to see more details and the hints from fault localization. Usually, the
name of the report is similar to `output/Counterexample.2.html`.

Interpreting Results
------------------
All the above-mentioned techniques and how to interpret their resuls will be explained here.

### Distance Metrics

The goal of this technique is to find the closest successful execution of the program given a
counterexample. The output shows modifications of the counterexample transforming it into a
successful run.

A fault will contain statements like this:

```
    LINE 14 WAS: !(num < 1), CHANGED TO: num < 1
    LINE 16, DELETED: int i = 2;
    LINE 16, DELETED: i <= num
    LINE 17, DELETED: (num % i) == 0
    LINE 17, DELETED: int __CPAchecker_TMP_0;
    LINE 17, DELETED: isPrime(i)
    LINE 5, DELETED: int j = 2;
    LINE 5, DELETED: !(j <= (n / 2))
    LINE 8, DELETED: return 1;
    LINE 17, DELETED:
    LINE 17, DELETED: !(__CPAchecker_TMP_0 == 0)
    LINE 18, DELETED: num = num / (i + 1);
    LINE 19, DELETED: int __CPAchecker_TMP_1 = i;
    LINE 19, DELETED: i = i - 1;
    LINE 19, DELETED: __CPAchecker_TMP_1;
    LINE 16, DELETED: i = i + 1;
    LINE 16, DELETED: !(i <= num)
    LINE 23, DELETED: num != 1
    LINE 24, DELETED: reach_error();
    LINE 14, WAS EXECUTED: return 0;
```

It shows how to change the presented counterexample to a feasible non-violating execution path.
Changing line 14 to `num < 1`, suggests taking the then-branch rather than the else-branch.
`DELETED` means that we do not travers this statement along our path anymore.
`WAS EXECUTED` indicates that we do not change this line.

### Error Invariants

Error Invariants takes the counterexample and negates the last assume statement. Hence, the path is
not feasible anymore, allowing the calculation of Craig interpolants in-between every edge. The
authors of this work state that interpolants may be inductive on more than one position in the code
although the interpolation engine does not provide the same interpolant on these positions. The
algorithm finds for every interpolant, the inductive interval on the error path. Thus, we know from
which and until which statement an interpolant is valid. Therefore, we can calculate an abstract
error trace consisting of real statements in the counterexample and inductive interpolants. The
abstract error trace is an alternating sequence of statements and interpolants. Whenever an
inductive interpolant is not valid after a statement anymore, the authors assume this statement to
be error-prone. The output of the report shows the alternating sequence of interpolants and
statements.

### MaxSat

Similar to `ErrorInvariants`, this approach also takes the counterexample and negates the last
assume statement to make the error trace infeasible. Our goal is to find a maximal set of statements
of the counterexample that conjuncted with the negated assume statement is still satisfiable. If we
now compute the complement of the counterexample in this set, we know which statements would lead to
unsatisfiability of the error trace. The algorithm marks these statements and prints them in the
report.

### DStar, Tarantula, and Ochiai

All of these techniques use a set of test cases to calculate a suspiciousness score for each
operation along the counterexample. The calculation of the scores relies on the number of total test
cases, total failed test cases, total successful test cases, test cases that fail and execute a
certain operation, and test cases that succeed and execute a certain operation. The report shows
every edge and the computed score.

Implementing New Techniques
------------------
In this tutorial we implement a new fault localization technique that simply marks all assume edges.
This technique allows users to find decision points and wrong assumptions in the error path that
eventually lead to the error. The technique solely serves demonstration purposes. A set of
error-prone statements is called _fault_.

We start the tutorial by creating a new class
`FaultLocalizationAssumption` in `org.sosy_lab.cpachecker.core.algorithm`.

Our class realizes the interface `Algorithm` and needs two inputs: 
the logger and the algorithm that finds us a counterexample.

```java
package org.sosy_lab.cpachecker.core.algorithm;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class FaultLocalizationAssumption implements Algorithm {

  private final Algorithm algorithm;
  private final LogManager logger;

  public FaultLocalizationAssumption(
      final Algorithm pStoreAlgorithm,
      final LogManager pLogger) {
    algorithm = pStoreAlgorithm;
    logger = pLogger;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status = algorithm.run(reachedSet);
    return status;
  }
}
```

We proceed by extracting the counterexample from the result
and the extraction of all assume edges.
Additionally, we add the sequence of actions we want to take.

```java
package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;

public class FaultLocalizationAssumption implements Algorithm {

  private final Algorithm algorithm;
  private final LogManager logger;

  public FaultLocalizationAssumption(
      final Algorithm pStoreAlgorithm,
      final LogManager pLogger) {
    algorithm = pStoreAlgorithm;
    logger = pLogger;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status = algorithm.run(reachedSet);
    FluentIterable<CounterexampleInfo> counterExamples =
        Optionals.presentInstances(
            from(reachedSet)
                .filter(AbstractStates::isTargetState)
                .filter(ARGState.class)
                .transform(ARGState::getCounterexampleInformation));

    logger.log(Level.INFO, "Starting fault localization...");
    // iterate over all counterexamples
    for (CounterexampleInfo info : counterExamples) {
      logger.log(Level.INFO, "Find explanations for fault #" + info.getUniqueId());
      // extract path from start to the error location
      CFAPathWithAssumptions path = info.getCFAPathWithAssignments();
      // transform the path to a list of edges ("statements")
      List<CFAEdge> edgeList =
          transformedImmutableListCopy(path, assumption -> assumption.getCFAEdge());
      // perform an arbitrary fault localization algorithm that promises to find a set of faults
      Collection<Fault> faults = performFaultLocalization(edgeList);
      // explain and rank the faults
      List<Fault> ranked = rankFaults(explainFaults(faults));
      // visualize our faults
      visualize(info, ranked);
    }
    logger.log(Level.INFO, "Stopping fault localization...");
    return status;
  }
}
```

In general, we need a _Localizer_ that finds a collection of faults
(`Collection<Fault> faults = performFaultLocalization(edgeList)`), an _Explainer_ that adds
information on why we think the fault is error-prone
(`explainFaults(faults)`), a _Ranker_ that sorts the faults by an arbitrary measure
(`List<Fault> ranked = rankFaults(explainFaults(faults))`), and a _Visualizer_ that transforms the
result in, e.g., an HTML report (`visualize(info, ranked)`).

Here is the full code snippet showing the implementation of our demo algorithm:
```java
package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;

public class FaultLocalizationAssumption implements Algorithm {

  private final Algorithm algorithm;
  private final LogManager logger;

  public FaultLocalizationAssumption(
      final Algorithm pStoreAlgorithm,
      final LogManager pLogger) {
    algorithm = pStoreAlgorithm;
    logger = pLogger;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status = algorithm.run(reachedSet);
    FluentIterable<CounterexampleInfo> counterExamples =
        Optionals.presentInstances(
            from(reachedSet)
                .filter(AbstractStates::isTargetState)
                .filter(ARGState.class)
                .transform(ARGState::getCounterexampleInformation));

    logger.log(Level.INFO, "Starting fault localization...");
    // iterate over all counterexamples
    for (CounterexampleInfo info : counterExamples) {
      logger.log(Level.INFO, "Find explanations for fault #" + info.getUniqueId());
      // extract path from start to the error location
      CFAPathWithAssumptions path = info.getCFAPathWithAssignments();
      // transform the path to a list of edges ("statements")
      List<CFAEdge> edgeList =
          transformedImmutableListCopy(path, assumption -> assumption.getCFAEdge());
      // perform an arbitrary fault localization algorithm that promises to find a set of faults
      Collection<Fault> faults = performFaultLocalization(edgeList);
      // explain and rank the faults
      List<Fault> ranked = rankFaults(explainFaults(faults));
      // visualize our faults
      visualize(info, ranked);
    }
    logger.log(Level.INFO, "Stopping fault localization...");
    return status;
  }
  
  private Collection<Fault> performFaultLocalization(List<CFAEdge> edgeList) {
    Set<FaultContribution> assumes = FluentIterable
        .from(edgeList)
        .filter(e -> e.getEdgeType() == CFAEdgeType.AssumeEdge)
        .transform(edge -> new FaultContribution(edge))
        .toSet();
    // collect all error-prone statements in a Fault
    Fault fault = new Fault(assumes);
    return ImmutableSet.of(fault);
  }
  
  private List<Fault> rankFaults(Collection<Fault> faults) {
    return ImmutableList.copyOf(faults);
  }
  
  private Collection<Fault> explainFaults(Collection<Fault> faults) {
    faults.forEach(f -> f.addInfo(FaultInfo.possibleFixFor(f)));
    return faults;
  }
  
  private void visualize(CounterexampleInfo parent, List<Fault> faults) {
    FaultLocalizationInfo faultLocalizationInfo =
        new FaultLocalizationInfo(faults, parent);
    // apply changes to Counterexample `info`
    faultLocalizationInfo.apply();
  }
}
```

We will now step through the code.
```java
// extract path from start to the error location
CFAPathWithAssumptions path = info.getCFAPathWithAssignments();
// transform the path to a list of edges ("statements")
List<CFAEdge> edgeList = transformedImmutableListCopy(path, assumption -> assumption.getCFAEdge());
```

This is the CPAchecker way of extracting a counterexample in case a violation was found. We can
access the path of edges along the CFA that lead to the error in `edgeList`. Note that a single edge
might be traversed multiple times. Hence, it can also appear multiple times in an error path.

A _Localizer_ promises us to return a collection of faults. In our case, the _Localizer_ can be
inlined in the code with:

```java
private Collection<Fault> performFaultLocalization(List<CFAEdge> edgeList){
    // Filter all assume edges 
    Set<FaultContribution> assumes=FluentIterable
        .from(edgeList)
        .filter(e->e.getEdgeType()==CFAEdgeType.AssumeEdge)
        .transform(edge->new FaultContribution(edge))
        .toSet();
    // collect all error-prone statements in a Fault
    Fault fault = new Fault(assumes);
    // return a collection of faults
    return ImmutableSet.of(fault);
}
```

The "_Localizer_" transforms the counterexample (list of edges) to a collection of faults. Here, we
only have one fault containing all assume edges along the path. We collect these edges in a `Fault`
after we transformed them into `FaultContributions`.
`FaultContributions` allow us to attach information to `CFAEdges`. Since the same edge might be
traversed several times in one counterexample, it makes sense to have `FaultContributions` to add
different information to the same edge but at different "points in time".

```java
private Collection<Fault> explainFaults(Collection<Fault> faults){
    faults.forEach(f->f.addInfo(FaultInfo.possibleFixFor(f)));
    return faults;
}
```

In a next step, we (optionally) explain the faults.
Our framework provides a variety of explanations that do not require any context.
In the future, we could add more sophisticated techniques such as 
automatic program repair to synthesize a patch that fixes the bug.

```java
private List<Fault> rankFaults(Collection<Fault> faults){
    return ImmutableList.copyOf(faults);
}
```

Now, we want to rank our faults. The simplest of all rankers can be implemented as seen above. We
just use the iterator of the underlying `Collection` to create a list. Our framework provides a
variety of `FaultScoring` techniques that sort faults with the help of heuristics (set size,
distance to error location,...).

```java
private void visualize(CounterexampleInfo parent,List<Fault> faults){
    FaultLocalizationInfo faultLocalizationInfo=new FaultLocalizationInfo(faults,parent);
    // apply changes to counterexample `parent`
    faultLocalizationInfo.apply();
}
```

Finally, we want to visualize our results. In our framework, we can simply call the code snippet
above. In the background, the actual `CounterexampleInfo parent`
is replaced by `faultLocalizationInfo` allowing the `ReportGenerator`
to access the detailed information.

To run our algorithm, we have to add it to the `CoreComponentsFactory` contained in the
package `org.sosy_lab.cpachecker.core`. We add a new option called `assumes` and create our new
algorithm in case this option is enabled. For this we have to add the following code right before
the constructor of `CoreComponentsFactory.

```java
@Option(secure = true, name = "assumes", description = "Enable our fault localization technique")
private boolean assumes=false;
```
Lastly, we append the following check in the very last `else` branch of the method
`createAlgorithm`.

```java
public Algorithm createAlgorithm(final ConfigurableProgramAnalysis cpa,final CFA cfa,final Specification specification)
    throws InvalidConfigurationException,CPAException,InterruptedException {
    if(/*...*/){
        // ...
    } else {
        // ...
        if(assumes){
          algorithm=new FaultLocalizationAssumption(algorithm,logger);
        }
    }
    return algorithm;
}
```

Afterwards, we can execute the following snippet from the root directory of CPAchecker to run our
implementation:
```
ant && \
./scripts/cpa.sh \
-predicateAnalysis \
-setprop analysis.assumes=true \
-spec config/specification/sv-comp-reachability.spc \
doc/tutorials/fault-localization/factorization-plain.c
```
The report can be found in `output/Counterexample.2.html`.
It should look like [this](report.html).

# Importing Faults
Our framework allows the import of faults from external tool
via our format in JSON.
The format is defined here:
```JSON
{
  "faults" : [ {
    "fault" : {
      "score" : 0.4104287868993752,
      "intendedIndex" : -1,
      "infos" : [ {
        "fault-info" : {
          "description" : "...",
          "score" : 0.0,
          "type" : "FIX"
        }
      }, ... ],
      "contributions" : [ {
        "fault-contribution" : {
          "score" : 0.0,
          "infos" : [ ... ],
          "location" : {
            "startLine" : 11,
            "endLine" : 11,
            "startOffset" : 189,
            "endOffset" : 204,
            "code" : "num = num / (i + 1);",
            "filename" : "test/primefactor.c"
          }
        }
      } ]
    }
  }, ... ],
  "error-location" : {
   ... 
  }
}
```
It consists of two parts: a list of faults and the error location.
The error location is optional and has the same attributes as other "location".
It should indicate the location where a violation was found (e.g., violated assertion).
A "fault" has a score, an intended index (in case it should not be ranked according to score),
a list of additional infos (explanations, ranking information, etc.), and the actual list of 
contributions to that specific fault.
Every "fault-contribution" has a score and additional information like "fault" has it.
Additionally, every contribution provides the exact location by specifying the
start line of the statement, the end line of the statement, and the character offset of the first
and last character of the statement. Optionally, a code snippet and the filename can be provided.
A "fault-info" consists of a description like "possible off-by-one-error detected", a score, and
a type. The type is either "FIX" for a possible bug-fix, "REASON" that explains why the fault
localization technique marked this statement, or "RANK_INFO" to explain why a certain score
was assigned.
Enable the option `-setprop counterexample.export.exportFaults=true` to export faults in that format
with cpachecker.
To import faults again, use:
```
-importFaults \
-setprop faultLocalization.import.importFile=<file> \
-setprop faultLocalization.import.explanations=SUSPICIOUS_CALCULATION,NO_CONTEXT \
-setprop faultLocalization.import.scorings=VARIABLE_COUNT \
<program>
```
This allows the application of a list of rankings and explanations to the faults that are
again exported in our format. Also, an HTML report is generated.