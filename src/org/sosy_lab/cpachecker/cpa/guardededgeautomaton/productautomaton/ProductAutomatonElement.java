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
package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton;

import java.util.LinkedList;
import java.util.List;

import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonPredicateElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.IGuardedEdgeAutomatonStateElement;

public abstract class ProductAutomatonElement extends CompositeState {

  private static final long serialVersionUID = -3085597005126968140L;

  public static ProductAutomatonElement createElement(List<AbstractState> pElements) {
    List<ECPPredicate> lPredicates = new LinkedList<>();

    for (AbstractState lElement : pElements) {
      if (lElement instanceof GuardedEdgeAutomatonPredicateElement) {
        GuardedEdgeAutomatonPredicateElement lPredicateElement = (GuardedEdgeAutomatonPredicateElement)lElement;

        for (ECPPredicate lPredicate : lPredicateElement) {
          lPredicates.add(lPredicate);
        }
      }
    }

    if (lPredicates.isEmpty()) {
      return new StateElement(pElements);
    }
    else {
      return new PredicateElement(pElements, lPredicates);
    }
  }

  public static class StateElement extends ProductAutomatonElement {

    private static final long serialVersionUID = -6070433962986801950L;

    public StateElement(List<AbstractState> pElements) {
      super(pElements);
    }

  }

  public static class PredicateElement extends ProductAutomatonElement {

    private static final long serialVersionUID = 9111700516213583815L;
    private final List<ECPPredicate> mPredicates;

    public PredicateElement(List<AbstractState> pElements, List<ECPPredicate> pPredicates) {
      super(pElements);

      mPredicates = pPredicates;
    }

    public List<ECPPredicate> getPredicates() {
      return mPredicates;
    }

    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }

      if (pOther == null) {
        return false;
      }

      if (getClass().equals(pOther.getClass())) {
        PredicateElement lOther = (PredicateElement)pOther;

        return (super.equals(lOther) && mPredicates.equals(lOther.mPredicates));
      }

      return false;
    }

    @Override
    public int hashCode() {
      return mPredicates.hashCode();
    }

  }

  public ProductAutomatonElement(List<AbstractState> pElements) {
    super(pElements);
  }

  public boolean isFinalState() {
    if (super.getNumberOfStates() == 0) {
      return false;
    }

    for (AbstractState lElement : getWrappedStates()) {
      IGuardedEdgeAutomatonStateElement lStateElement = (IGuardedEdgeAutomatonStateElement)lElement;

      if (!lStateElement.isFinalState()) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean isTarget() {
    if (isFinalState()) {
      return true;
    }

    for (AbstractState lElement : getWrappedStates()) {
      Targetable lSubelement = (Targetable)lElement;

      if (lSubelement.isTarget()) {
        return true;
      }
    }

    return false;
  }

}
