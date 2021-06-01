// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary.strategies;

import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class LoopUnrolling extends AbstractStrategy {

  Integer maxUnrollingsStrategy = 0;

  public LoopUnrolling(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      int pStrategyNumber,
      int pMaxUnrollingsStrategy) {
    super(pLogger, pShutdownNotifier, pStrategyNumber);
    maxUnrollingsStrategy = pMaxUnrollingsStrategy;
  }

  @Override
  public Optional<Collection<? extends AbstractState>> summarizeLoopState(
      AbstractState pState, Precision pPrecision, TransferRelation pTransferRelation)
      throws CPATransferException, InterruptedException {
    // TODO Unroll the Loop some amount of times. Can be improved by checking the maximal amount of
    // Loop iterations and unrolling only that amount of iterations.
    // Can be faster than the normal analysis, since it does not expect the Refinement in order to
    // unroll the loop
    // but may also be slower, since the loop unrolling has been done and must be transversed.
    // TODO, how can we see if we already applied loop unrolling in order to not apply it again once
    // the current unrolling has finished?
    return Optional.empty();
  }

  @Override
  public boolean isPrecise() {
    return true;
  }
}
