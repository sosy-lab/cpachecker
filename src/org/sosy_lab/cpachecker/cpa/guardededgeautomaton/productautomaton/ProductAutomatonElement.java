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

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonPredicateElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.IGuardedEdgeAutomatonStateElement;
import org.sosy_lab.cpachecker.util.ecp.ECPPredicate;

public abstract class ProductAutomatonElement extends CompositeElement {

  public static ProductAutomatonElement createElement(List<AbstractElement> pElements) {
    List<ECPPredicate> lPredicates = new LinkedList<ECPPredicate>();

    for (AbstractElement lElement : pElements) {
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

    public StateElement(List<AbstractElement> pElements) {
      super(pElements);
    }

  }

  public static class PredicateElement extends ProductAutomatonElement {

    private final List<ECPPredicate> mPredicates;

    public PredicateElement(List<AbstractElement> pElements, List<ECPPredicate> pPredicates) {
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

  public ProductAutomatonElement(List<AbstractElement> pElements) {
    super(pElements);
  }

  public boolean isFinalState() {
    if (super.getNumberofElements() == 0) {
      return false;
    }

    for (AbstractElement lElement : getElements()) {
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

    for (AbstractElement lElement : getElements()) {
      Targetable lSubelement = (Targetable)lElement;

      if (lSubelement.isTarget()) {
        return true;
      }
    }

    return false;
  }

}
