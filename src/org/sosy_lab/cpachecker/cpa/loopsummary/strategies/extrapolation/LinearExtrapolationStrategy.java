// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary.strategies.extrapolation;

import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class LinearExtrapolationStrategy extends AbstractExtrapolationStrategy {

  // See
  // https://math.stackexchange.com/questions/2079950/compute-the-n-th-power-of-triangular-3-times3-matrix

  public LinearExtrapolationStrategy(
      final LogManager pLogger, ShutdownNotifier pShutdownNotifier, int strategyIndex) {
    super(pLogger, pShutdownNotifier, strategyIndex);
  }

  @Override
  public Optional<Collection<? extends AbstractState>> summarizeLoopState(
      AbstractState pState, Precision pPrecision, TransferRelation pTransferRelation)
      throws CPATransferException, InterruptedException {
    Optional<Integer> loopBranchIndexOptional =
        getLoopBranchIndex(AbstractStates.extractLocation(pState));
    Integer loopBranchIndex;

    if (loopBranchIndexOptional.isEmpty()) {
      return Optional.empty();
    } else {
      loopBranchIndex = loopBranchIndexOptional.get();
    }

    Optional<CExpression> loopBoundOptional = bound(AbstractStates.extractLocation(pState));
    @SuppressWarnings("unused")
    CExpression loopBound;
    if (loopBoundOptional.isEmpty()) {
      return Optional.empty();
    } else {
      loopBound = loopBoundOptional.get();
    }

    if (!linearArithmeticExpressionsLoop(AbstractStates.extractLocation(pState), loopBranchIndex)) {
      return Optional.empty();
    }

    // Get Matrix and make it Upper Diagonal
    // See if the loop bound delta can be easily calculated, because a variable is increased a
    // constant amount or not
    // Get the general representation of the matrix and apply the loop unrolling, the extrapolation
    // and the unrolling again

    return Optional.empty();

    /*Map<String, Integer> loopVariableDelta = getLoopVariableDeltas(pState, loopBranchIndex);

    int boundDelta = boundDelta(loopVariableDelta, loopBound);
    if (boundDelta >= 0) { // TODO How do you treat non Termination?
      return Optional.empty();
    }

    GhostCFA ghostCFA;
    Optional<GhostCFA> ghostCFASuccess =
        summaryCFA(pState, loopVariableDelta, loopBound, boundDelta, loopBranchIndex);

    if (ghostCFASuccess.isEmpty()) {
      return Optional.empty();
    } else {
      ghostCFA = ghostCFASuccess.get();
    }

    Collection<AbstractState> realStatesEndCollection =
        transverseGhostCFA(ghostCFA, pState, pPrecision, pTransferRelation, loopBranchIndex);

    return Optional.of(realStatesEndCollection);*/
  }
}
