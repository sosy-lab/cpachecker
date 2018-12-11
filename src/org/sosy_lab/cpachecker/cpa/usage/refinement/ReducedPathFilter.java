/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.usage.refinement;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.lock.AbstractLockState;
import org.sosy_lab.cpachecker.cpa.lock.LockTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

public class ReducedPathFilter extends GenericSinglePathRefiner {

  private final LockTransferRelation transfer;

  public ReducedPathFilter(
      ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> pWrapper,
      LockTransferRelation pTransfer) {
    super(pWrapper);
    transfer = pTransfer;
  }

  @Override
  protected RefinementResult call(ExtendedARGPath pPath) throws CPAException, InterruptedException {

    ARGState initialState = pPath.getFirstState();
    AbstractLockState initialLockState =
        AbstractStates.extractStateByType(initialState, AbstractLockState.class);
    // We cannot use last arg state, as it is not expanded!
    // LockState in Usage is correct
    AbstractLockState lastLockState = pPath.getUsageInfo().getLockState();
    List<CFAEdge> edges = pPath.getInnerEdges();

    AbstractLockState currentState = initialLockState;

    for (CFAEdge edge : edges) {
      Collection<? extends AbstractState> successors =
          transfer
              .getAbstractSuccessorsForEdge(currentState, SingletonPrecision.getInstance(), edge);

      currentState = (AbstractLockState) Iterables.getOnlyElement(successors);
    }
    if (currentState.equals(lastLockState)) {
      return RefinementResult.createTrue();
    } else {
      return RefinementResult.createFalse();
    }
  }

}
