// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.randomWalk;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@Options(prefix = "cpa.randomWalk")
public class RandomWalkTransferRelation extends SingleEdgeTransferRelation {

  private LogManager logger;

  @Option(
      secure = true,
      description =
          "Maximal number of branches taken before the computation aborts (-1 is no abort)")
  private int maxBranchesLength = -1;

  @Option(
      secure = true,
      description =
          "If there is an if and else branch for a loophead, take first with this probability in % ")
  private int probForLeftBranchForLoop = 60;

  @Option(
      secure = true,
      description = "If there is an if and else branch, take first with this probability in % ")
  private int probForLeftBranchForAssign = 50;

  RandomWalkTransferRelation(Configuration pConfig, CFA pCFA, LogManager pLogger)
      throws InvalidConfigurationException {
    checkNotNull(pCFA, "CFA instance needed to create LoopBoundCPA");
    pConfig.inject(this);
    this.logger = pLogger;
    if (probForLeftBranchForLoop > 100
        || probForLeftBranchForLoop < 0
        || probForLeftBranchForAssign > 100
        || probForLeftBranchForAssign < 0) {
      throw new InvalidConfigurationException(
          "Probability for left branch must be between 0 and 100");
    }
    logger.logf(
        Level.INFO,
        "Computing a random path with at most %d branching points",
        this.maxBranchesLength);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, final CFAEdge pCfaEdge)
      throws CPATransferException {

    logger.log(
        Level.FINE, String.format("Processing edge %s, current State is %s", pCfaEdge, pState));

    RandomWalkState state = (RandomWalkState) pState;
    if (pCfaEdge instanceof AssumeEdge) {
      if (state.thisEdgeShouldBeTaken((AssumeEdge) pCfaEdge, probForLeftBranchForLoop, probForLeftBranchForAssign)) {
        return Collections.singleton(state.takeEdge(pCfaEdge, logger));
      } else {
        return ImmutableSet.of();
      }
    }

    return Collections.singleton(state.takeEdge(pCfaEdge, logger));
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    assert state instanceof RandomWalkState;
    RandomWalkState rsState = (RandomWalkState) state;
    for (AbstractState otherState : otherStates) {
      if (otherState instanceof PredicateAbstractState) {
        PredicateAbstractState predicateState = (PredicateAbstractState) otherState;
        rsState.setPathFormula(predicateState.getPathFormula());
        break;
      }
    }
    return Collections.singleton(rsState);
  }
}
