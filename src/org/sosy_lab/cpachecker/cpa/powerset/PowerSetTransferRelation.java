/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.powerset;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class PowerSetTransferRelation extends SingleEdgeTransferRelation {

  private final TransferRelation wrapperTransfer;

  public PowerSetTransferRelation(final TransferRelation pTransferRelation) {
    wrapperTransfer = pTransferRelation;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(AbstractState pState,
      Precision pPrecision, CFAEdge pCfaEdge) throws CPATransferException, InterruptedException {
    PowerSetState state = (PowerSetState) pState;
    Set<AbstractState> successors =
        Sets.newHashSetWithExpectedSize(state.getWrappedStates().size());

    for (AbstractState wrappedState : state.getWrappedStates()) {
      successors.addAll(wrapperTransfer.getAbstractSuccessorsForEdge(wrappedState, pPrecision, pCfaEdge));
    }

    return Collections.singleton(new PowerSetState(successors));
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {

    PowerSetState setStates = (PowerSetState) state;
    Set<AbstractState> newStates =
        Sets.newHashSetWithExpectedSize(setStates.getWrappedStates().size());

    boolean changed = false;

    for (AbstractState stateInSet : setStates.getWrappedStates()) {
      Collection<? extends AbstractState> strengtheningRes =
          wrapperTransfer.strengthen(
              stateInSet, Collections.singletonList(stateInSet), cfaEdge, precision);
      if (strengtheningRes != null && strengtheningRes.size() > 0) {
        changed = true;
        newStates.addAll(strengtheningRes);
      }
    }

    return changed ? Collections.singleton(new PowerSetState(newStates)) : ImmutableSet.of();
  }

}
