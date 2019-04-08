/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.dca;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class DCATransferRelation extends SingleEdgeTransferRelation {

  private AutomatonTransferRelation automatonTR;

  public DCATransferRelation(AutomatonTransferRelation pAutomatonTransferRelation) {
    automatonTR = pAutomatonTransferRelation;
  }

  @Override
  public Collection<? extends AbstractState>
      getAbstractSuccessorsForEdge(AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
          throws CPATransferException, InterruptedException {
    checkArgument(pState instanceof DCAState);

    List<Set<AutomatonState>> listOfStates = new ArrayList<>();
    for (AutomatonState compositeState : ((DCAState) pState).getCompositeStates()) {
      ImmutableSet<AutomatonState> successorsStates =
          ImmutableSet.copyOf(
              automatonTR.getAbstractSuccessorsForEdge(compositeState, pPrecision, pCfaEdge));
      if (successorsStates.isEmpty()) {
        return ImmutableSet.of();
      }
      listOfStates.add(successorsStates);
    }
    Set<List<AutomatonState>> cartesianProduct = Sets.cartesianProduct(listOfStates);
    return cartesianProduct.stream().map(DCAState::new).collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      List<AbstractState> pOtherStates,
      @Nullable CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    checkArgument(pState instanceof DCAState);

    List<Set<AutomatonState>> listOfStates = new ArrayList<>();
    for (AutomatonState s : ((DCAState) pState).getCompositeStates()) {
      Set<AutomatonState> strengthenResult =
          ImmutableSet.copyOf(automatonTR.strengthen(s, pOtherStates, pCfaEdge, pPrecision));
      if (strengthenResult.isEmpty()) {
        return ImmutableSet.of();
      }
      listOfStates.add(strengthenResult);
    }
    Set<List<AutomatonState>> cartesianProduct = Sets.cartesianProduct(listOfStates);
    return cartesianProduct.stream().map(DCAState::new).collect(ImmutableSet.toImmutableSet());
  }

}
