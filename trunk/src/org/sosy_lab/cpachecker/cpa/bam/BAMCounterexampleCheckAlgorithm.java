// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
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
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;

public class BAMCounterexampleCheckAlgorithm extends CounterexampleCheckAlgorithm {

  private final BAMCPA cpa;

  public BAMCounterexampleCheckAlgorithm(
      Algorithm algorithm,
      ConfigurableProgramAnalysis pCpa,
      Configuration config,
      LogManager logger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA cfa)
      throws InvalidConfigurationException {
    super(algorithm, pCpa, config, pSpecification, logger, pShutdownNotifier, cfa);

    if (!(pCpa instanceof BAMCPA)) {
      throw new InvalidConfigurationException("BAM CPA needed for BAM counterexample check");
    }
    cpa = (BAMCPA) pCpa;
  }

  @Override
  protected boolean checkErrorPaths(
      CounterexampleChecker checker, ARGState errorState, ReachedSet reached)
      throws CPAException, InterruptedException {

    ARGReachedSet mainReachedSet =
        new ARGReachedSet(reached, cpa.getWrappedCpa(), 0 /* irrelevant number */);

    assert mainReachedSet.asReachedSet().contains(errorState);
    assert errorState == reached.getLastState();

    // compute BAM-reachedset for the reachable states,
    // it contains all error-paths and is sufficient to check counterexample.
    BAMSubgraphComputer graphComputer = new BAMSubgraphComputer(cpa, false);
    Pair<BackwardARGState, BackwardARGState> rootAndTargetOfSubgraph =
        graphComputer.computeCounterexampleSubgraph(errorState, mainReachedSet);
    ARGState rootState = rootAndTargetOfSubgraph.getFirst();
    ARGState target = rootAndTargetOfSubgraph.getSecond();
    ImmutableSet<ARGState> statesOnErrorPath = rootState.getSubgraph().toSet();

    assert Objects.equals(((BackwardARGState) target).getARGState(), errorState);

    return checker.checkCounterexample(rootState, target, statesOnErrorPath);
  }
}
