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

public class ConcolicExecution extends AbstractStrategy {

  public ConcolicExecution(
      LogManager pLogger, ShutdownNotifier pShutdownNotifier, int pStrategyNumber) {
    super(pLogger, pShutdownNotifier, pStrategyNumber);
  }

  @Override
  public Optional<Collection<? extends AbstractState>> summarizeLoopState(
      AbstractState pState, Precision pPrecision, TransferRelation pTransferRelation)
      throws CPATransferException, InterruptedException {
    // TODO Execute the Loop symbolically and concretely to get the values after the loop.
    // A heuristic needs to be determined in order to decide when to apply this and when not.
    // See Comments on Conrete Execution for some ideas on how to implement this.
    return Optional.empty();
  }

  @Override
  public boolean isPrecise() {
    return true;
  }

}
