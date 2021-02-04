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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class loopAcceleration implements strategyInterface {

  @Override
  public Optional<Collection<? extends AbstractState>> summarizeLoopState(
      AbstractState pState, Precision pPrecision, TransferRelation pTransferRelation)
      throws CPATransferException, InterruptedException {
    // TODO Auto-generated method stub
    return Optional.empty();
  }

}
