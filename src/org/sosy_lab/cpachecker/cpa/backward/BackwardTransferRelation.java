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
