/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Cartesian;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Lists;

public final class PowersetAutomatonTransferRelation extends SingleEdgeTransferRelation {

  private final AutomatonTransferRelation componentTransfer;

  public PowersetAutomatonTransferRelation(AutomatonTransferRelation pComponentTransferRelation)
      throws InvalidConfigurationException {

    this.componentTransfer = pComponentTransferRelation;
  }

  @Override
  public Collection<PowersetAutomatonState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge)
        throws CPATransferException, InterruptedException {

    PowersetAutomatonState compositeState = (PowersetAutomatonState) pElement;
    Collection<Collection<AutomatonState>> componentSuccessors = Lists.newArrayList();

    // Given a composite automaton state e = [q1, q2]
    //  Successors of the states:
    //    succ(q1) = [q3]
    //    succ(q2) = [q4,q5]
    //
    //  This should result in two composite states:
    //    e'  = [q3, q4]
    //    e'' = [q3, q5]
    //    (which is the cartesian product: [q3] x [q4,q5])

    for (AutomatonState comp: compositeState) {
      Collection<AutomatonState> succOfComp = componentTransfer.getAbstractSuccessorsForEdge(comp, pPrecision, pCfaEdge);

      // Splits (several successors for one automaton state)
      //  are possible, and necessary to represent disjunctions in assumptions!
      //
      // Since assumptions are handled in the strengthening of the transfer relation
      //  a merge is possible after the strengthening has been performed!!!

      componentSuccessors.add(succOfComp);
    }

    return buildCartesianProduct(componentSuccessors);
  }

  @Override
  public Collection<PowersetAutomatonState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {

    Preconditions.checkArgument(pState instanceof PowersetAutomatonState);

    final PowersetAutomatonState state = (PowersetAutomatonState) pState;
    Collection<Collection<AutomatonState>> componentSuccessors = Lists.newArrayList();

    for (AutomatonState comp: state) {
      @Nullable Collection<AutomatonState> strengthenedComp = componentTransfer.strengthen(
          comp, pOtherStates, pCfaEdge, pPrecision);

      if (strengthenedComp == null) { // no change
        componentSuccessors.add(ImmutableSet.of(comp));
      } else {
        componentSuccessors.add(strengthenedComp);
      }
    }

    return buildCartesianProduct(componentSuccessors);
  }

  private Collection<PowersetAutomatonState> buildCartesianProduct(
      Collection<Collection<AutomatonState>> componentSuccessors) {

    Preconditions.checkNotNull(componentSuccessors);

    Builder<PowersetAutomatonState> result = ImmutableSet.<PowersetAutomatonState>builder();

    // The result is based on computing the CARTESIAN PRODUCT!
    for (Collection<AutomatonState> c: Cartesian.product(componentSuccessors)) {
      PowersetAutomatonState ca = new PowersetAutomatonState(c);
      result.add(ca);
    }

    return result.build();
  }

}
