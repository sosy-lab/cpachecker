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
package org.sosy_lab.cpachecker.cpa.singleSuccessorCompactor;

import com.google.common.collect.Iterables;
import java.util.Collection;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class SingleSuccessorCompactorTransferRelation implements TransferRelation {

  private final TransferRelation delegate;
  @Nullable private final BlockPartitioning partitioning;

  SingleSuccessorCompactorTransferRelation(TransferRelation pDelegate, BlockPartitioning pPartitioning) {
    delegate = pDelegate;
    partitioning = pPartitioning;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState state, Precision precision) throws CPATransferException, InterruptedException {

    // this is the main core of this CPA:
    // iterate as long as there is only one successor, abort on target, zero or multiple successors.

    Collection<? extends AbstractState> states;
    do {
      states = delegate.getAbstractSuccessors(state, precision);
      state = Iterables.getFirst(states, null);
    } while (states.size() == 1 && !AbstractStates.isTargetState(state) && !isAtBlockBorder(state));
    return states;
  }

  private boolean isAtBlockBorder(AbstractState pState) {
    if (partitioning == null) {
      return false;
    }
    CFANode node = AbstractStates.extractLocation(pState);
    return partitioning.isCallNode(node) || partitioning.isReturnNode(node);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    throw new UnsupportedOperationException();
  }
}
