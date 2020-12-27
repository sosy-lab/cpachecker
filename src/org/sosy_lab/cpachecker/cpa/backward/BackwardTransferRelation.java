// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// Copyright (C) 2007-2016  Dirk Beyer
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.backward;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;

/** The transfer-relation switches the direction of the analysis.
 * Calling {@link #getAbstractPredecessors} returns the predecessors and vice versa. */
class BackwardTransferRelation implements TransferRelation {

  private final TransferRelation wrapped;

  BackwardTransferRelation(TransferRelation pTransferRelation) {
    wrapped = pTransferRelation;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    return wrapped.getAbstractPredecessors(pState, pPrecision);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    return wrapped.getAbstractPredecessorsForEdge(pState, pPrecision, pCfaEdge);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractPredecessors(
      AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    return wrapped.getAbstractSuccessors(pState, pPrecision);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractPredecessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    return wrapped.getAbstractSuccessorsForEdge(pState, pPrecision, pCfaEdge);
  }
}
