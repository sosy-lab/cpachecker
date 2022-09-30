<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

Fault Localization
-----------------

CPAchecker provides a variety of fault localization techniques.
These techniques aim at finding error-prone statements in the program.
Additionally, some even try to explain the results to ease debugging.

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

Open the report in `output/` to see more details and the hints from fault localization.
Usually, the name of the report is similar to `output/Counterexample.2.html`.

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

Here is the full code snippet to implement our demo algorithm:
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
might be traversed more than once. Hence, it can also appear more than one time in an error path.

A _Localizer_ promises us to return a collection of faults. In our case, the _Localizer_ can be
inlined in the code with:
```java
  private Collection<Fault> performFaultLocalization(List<CFAEdge> edgeList) {
    Set<FaultContribution> assumes = FluentIterable
        .from(edgeList)
        .filter(e -> e.getEdgeType() == CFAEdgeType.AssumeEdge)
        .transform(edge -> new FaultContribution(edge))
        .toSet();
    // collect all error-prone statements in a Fault
    Fault fault = new Fault(assumes);
    // return a collection of faults
    return ImmutableSet.of(fault);
  }
```

The "_Localizer_" transforms the counterexample (list of edges) to a list of faults. Here, we only
have one fault containing all assume edges along the path. We collect these edges in a `Fault` after
we transformed them into `FaultContributions`.
`FaultContributions` allow us to attach information to `CFAEdges`. Since the same edge might be
traversed several times in one counterexample, it makes sense to have `FaultContributions` to add
different information to the same edge but at different "points in time".

```java
  private Collection<Fault> explainFaults(Collection<Fault> faults) {
    faults.forEach(f -> f.addInfo(FaultInfo.possibleFixFor(f)));
    return faults;
  }
```

In a next step, we (optionally) explain the faults.
Our framework provides a variety of explanations that do not require any context.
In the future, we could add more sophisticated techniques such as 
automatic program repair to synthesize a patch that fixes the bug.

```java
  private List<Fault> rankFaults(Collection<Fault> faults) {
    return ImmutableList.copyOf(faults);
  }
```

Now, we want to rank our faults. The simplest of all rankers can be implemented as seen above. We
just use the iterator of the underlying Collection to create a list. Our framework provides a
variety of `FaultScoring` techniques that sort faults with the help of heuristics (set size,
distance to error location...).

```java
  private void visualize(CounterexampleInfo parent, List<Fault> faults) {
    FaultLocalizationInfo faultLocalizationInfo =
        new FaultLocalizationInfo(faults, parent);
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
  private boolean assumes = false;
```
Lastly, we append the following check in the very last `else` branch of the method
`createAlgorithm`.
```java
  if (assumes) {
    algorithm = new FaultLocalizationAssumption(algorithm, logger);
  }
```

Afterwards, we can execute the following snippet 
from the root directory of CPAchecker to try our implementation:
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