// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.automaton.CachingTargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class BackwardBMCAlgorithm implements Algorithm {

  private LogManager logger;
  private Algorithm algorithm;
  private ConfigurableProgramAnalysis cpa;
  private CFA cfa;

  protected final ShutdownNotifier shutdownNotifier;
  private final TargetLocationProvider targetLocationProvider;

  public BackwardBMCAlgorithm(
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCPA,
      LogManager pLogger,
      final ShutdownManager pShutdownManager,
      CFA pCFA) {

    logger = pLogger;
    algorithm = pAlgorithm;
    cpa = pCPA;
    cfa = pCFA;

    shutdownNotifier = pShutdownManager.getNotifier();
    // is this the right target location provider?
    targetLocationProvider = new CachingTargetLocationProvider(shutdownNotifier, logger, cfa);
  }

  @Override
  public AlgorithmStatus run(final ReachedSet reachedSet)
      throws CPAException, InterruptedException {

    AlgorithmStatus status;
    status = BMCHelper.unroll(logger, reachedSet, algorithm, cpa);

    // is this correct? How will we get path formulas of loop heads?
    Optional<AbstractState> optTarget = getTarget(reachedSet);
    AbstractState target;
    if (optTarget.isPresent()) {
      // this is the main entry
      target = optTarget.get();
    } else {
      return status;
    }

    Set<AbstractState> loopHeads = getAbstractLoopHeads(reachedSet);

    // We only have a path formula, as we do not use abstractions?
    // Why is the path formula just 'true'?
    BooleanFormula program =
        AbstractStates.extractStateByType(target, PredicateAbstractState.class)
            .getPathFormula()
            .getFormula();

    return status;
  }

  private Optional<AbstractState> getTarget(final ReachedSet reachedSet) {
    // Should be only one target state, the main entry
    return FluentIterable.from(reachedSet).filter(AbstractStates::isTargetState).first();
  }

  private Set<AbstractState> getAbstractLoopHeads(final ReachedSet reachedSet) {
    Set<CFANode> loopHeadNodes = BMCHelper.getLoopHeads(cfa, targetLocationProvider);
    Set<AbstractState> abstractLoopHeads = new HashSet<>();
    for (AbstractState abstractState : reachedSet) {
      // can we assume only one location per abstract state?
      CFANode loc = AbstractStates.extractLocation(abstractState);
      if (loopHeadNodes.contains(loc)) {
        abstractLoopHeads.add(abstractState);
      }
    }

    return abstractLoopHeads;
  }
}
