/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;


public class PowersetAutomatonDomain implements AbstractDomain {

  private final PowersetAutomatonState topState;

  public PowersetAutomatonDomain(PowersetAutomatonState pTopState) {
    this.topState = Preconditions.checkNotNull(pTopState);
  }

  @Override
  public AbstractState join(AbstractState pE1, AbstractState pE2) throws CPAException {

    final PowersetAutomatonState e1 = (PowersetAutomatonState) pE1;
    final PowersetAutomatonState e2 = (PowersetAutomatonState) pE2;

    if (e1.equals(topState) || e2.equals(topState)) {
      return topState;
    }

    Builder<AutomatonState> joinedBuilder = ImmutableSet.builder();
    joinedBuilder.addAll(e1.getAutomataStates());
    joinedBuilder.addAll(e2.getAutomataStates());

    return new PowersetAutomatonState(joinedBuilder.build());
  }

  @Override
  public boolean isLessOrEqual(AbstractState pNewState, AbstractState pReachedState) {

    final PowersetAutomatonState newState = (PowersetAutomatonState) pNewState;
    final PowersetAutomatonState reachedState = (PowersetAutomatonState) pReachedState;

    if (newState == reachedState) {
      return true;
    }

    if (reachedState.equals(topState)) {
      return true;
    }

    if (newState.equals(topState) && !(newState.equals(reachedState))) {
      return false;
    }

    if (reachedState.getAutomataStates().containsAll(newState.getAutomataStates())) {
      return true;
    }

    return false;
  }

}
