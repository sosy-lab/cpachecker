/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.bam;

import java.util.Collection;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.counterexamplecheck.CounterexampleCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.counterexamplecheck.CounterexampleChecker;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMCEXSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class BAMCounterexampleCheckAlgorithm extends CounterexampleCheckAlgorithm {

  private final BAMCPA cpa;

  public BAMCounterexampleCheckAlgorithm(
      Algorithm algorithm,
      ConfigurableProgramAnalysis pCpa,
      Configuration config,
      LogManager logger,
      ShutdownNotifier pShutdownNotifier,
      CFA cfa,
      String filename)
      throws InvalidConfigurationException {
    super(algorithm, pCpa, config, logger, pShutdownNotifier, cfa, filename);

    if (!(pCpa instanceof BAMCPA)) {
      throw new InvalidConfigurationException("BAM CPA needed for BAM counterexample check");
    }
    cpa = (BAMCPA) pCpa;
  }

  @Override
  protected boolean checkErrorPaths(
      CounterexampleChecker checker,
      ARGState errorState,
      @SuppressWarnings("unused") ReachedSet reached)
      throws CPAException, InterruptedException {

    ARGReachedSet mainReachedSet =
        new ARGReachedSet(reached, (ARGCPA) cpa.getWrappedCpa(), 0 /* irrelevant number */);

    assert mainReachedSet.asReachedSet().contains(errorState);
    assert errorState == (ARGState) reached.getLastState();

    // compute BAM-reachedset for the reachable states,
    // it contains all error-paths and is sufficient to check counterexample.
    BAMCEXSubgraphComputer graphComputer = new BAMCEXSubgraphComputer(cpa);
    BackwardARGState rootState =
        graphComputer.computeCounterexampleSubgraph(errorState, mainReachedSet);
    Set<ARGState> statesOnErrorPath = rootState.getSubgraph();
    ARGState target = getErrorStateFromSubgraph(rootState);

    assert ((BackwardARGState) target).getARGState() == errorState;

    return checker.checkCounterexample(rootState, target, statesOnErrorPath);
  }

  /** Search the last state in the subgraph. It has to be the error state. */
  private static ARGState getErrorStateFromSubgraph(ARGState rootState) {
    ARGState target = rootState;
    Collection<ARGState> children;
    while (!(children = target.getChildren()).isEmpty()) {
      target = children.iterator().next();
    }
    return target;
  }
}
