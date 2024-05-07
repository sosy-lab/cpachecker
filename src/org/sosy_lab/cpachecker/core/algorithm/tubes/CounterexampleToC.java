// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.tubes;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class CounterexampleToC implements Algorithm {

  private final Algorithm algorithm;

  public CounterexampleToC(Algorithm pAlgorithm) {
    algorithm = pAlgorithm;
  }

  public String convertCounterexampleToC(CounterexampleInfo counterexample) {
    CounterexampleToCodeVisitor visitor = new CounterexampleToCodeVisitor();
    for (CFAEdge cfaEdge : counterexample.getTargetPath().getFullPath()) {
      visitor.setCurrentEdge(cfaEdge);
      cfaEdge.getRawAST().ifPresent(e -> e.accept_(visitor));
    }
    return visitor.finish();
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
    for (CounterexampleInfo counterExample : counterExamples) {
      String result = convertCounterexampleToC(counterExample);
    }
    return status;
  }
}
