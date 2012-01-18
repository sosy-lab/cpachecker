/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.guardededgeautomaton;

import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;

public class GuardedEdgeAutomatonStandardElement extends GuardedEdgeAutomatonStateElement {

  public GuardedEdgeAutomatonStandardElement(NondeterministicFiniteAutomaton.State pState, boolean pIsFinalState) {
    super(pState, pIsFinalState);
  }

  public GuardedEdgeAutomatonStandardElement(GuardedEdgeAutomatonPredicateElement pElement) {
    super(pElement.getAutomatonState(), pElement.isFinalState());
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }

    if (!pOther.getClass().equals(getClass())) {
      return false;
    }

    GuardedEdgeAutomatonStandardElement lOther = (GuardedEdgeAutomatonStandardElement)pOther;

    return (lOther.isFinalState() == isFinalState()) && lOther.getAutomatonState().equals(getAutomatonState());
  }

  @Override
  public int hashCode() {
    return getAutomatonState().hashCode() + 37239;
  }

  @Override
  public Object getPartitionKey() {
    return this;
  }

}
