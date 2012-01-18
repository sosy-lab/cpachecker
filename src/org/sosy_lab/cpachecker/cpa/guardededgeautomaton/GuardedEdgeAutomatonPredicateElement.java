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

import java.util.Iterator;
import java.util.List;

import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.ecp.ECPPredicate;

public class GuardedEdgeAutomatonPredicateElement extends GuardedEdgeAutomatonStateElement implements Iterable<ECPPredicate> {

  private final List<ECPPredicate> mPredicates;
  private final GuardedEdgeAutomatonStandardElement mStandardElement;

  public GuardedEdgeAutomatonPredicateElement(NondeterministicFiniteAutomaton.State pState, List<ECPPredicate> pPredicates, boolean pIsFinalState) {
    super(pState, pIsFinalState);
    mStandardElement = new GuardedEdgeAutomatonStandardElement(this);
    mPredicates = pPredicates;
  }

  public GuardedEdgeAutomatonStandardElement getStandardElement() {
    return mStandardElement;
  }

  @Override
  public Iterator<ECPPredicate> iterator() {
    return mPredicates.iterator();
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

    GuardedEdgeAutomatonPredicateElement lOther = (GuardedEdgeAutomatonPredicateElement)pOther;

    return (lOther.isFinalState() == isFinalState()) && lOther.getAutomatonState().equals(getAutomatonState()) && lOther.mPredicates.equals(mPredicates);
  }

  @Override
  public int hashCode() {
    return getAutomatonState().hashCode() + mPredicates.hashCode() + 3459;
  }

  @Override
  public String toString() {
    return super.toString() + mPredicates.toString();
  }

  @Override
  public Object getPartitionKey() {
    return this;
  }

}
