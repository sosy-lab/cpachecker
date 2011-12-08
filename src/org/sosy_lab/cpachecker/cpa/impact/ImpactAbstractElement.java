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
package org.sosy_lab.cpachecker.cpa.impact;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

import com.google.common.base.Preconditions;

abstract class ImpactAbstractElement implements AbstractElement {

  private final PathFormula pathFormula;

  ImpactAbstractElement(PathFormula pPathFormula) {
    pathFormula = checkNotNull(pPathFormula);
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  public abstract boolean isAbstractionElement();

  public abstract Formula getStateFormula();

  static AbstractionElement getLastAbstraction(ImpactAbstractElement element) {
    if (element.isAbstractionElement()) {
      return (AbstractionElement)element;
    } else {
      return ((NonAbstractionElement)element).getLastAbstraction();
    }
  }

  static class AbstractionElement extends ImpactAbstractElement {

    private final PathFormula blockFormula;
    private Formula stateFormula;

    public AbstractionElement(PathFormula pPathFormula, Formula pStateFormula, PathFormula pBlockFormula) {
      super(pPathFormula);

      assert pPathFormula.getFormula().isTrue();
      stateFormula = checkNotNull(pStateFormula);
      blockFormula = checkNotNull(pBlockFormula);
    }

    @Override
    public boolean isAbstractionElement() {
      return true;
    }

    public PathFormula getBlockFormula() {
      return blockFormula;
    }

    @Override
    public Formula getStateFormula() {
      return stateFormula;
    }

    public void setStateFormula(Formula pStateFormula) {
      stateFormula = checkNotNull(pStateFormula);
    }
  }

  static class NonAbstractionElement extends ImpactAbstractElement {

    private final AbstractionElement lastAbstraction;
    private ImpactAbstractElement mergedInto = null;

    public NonAbstractionElement(PathFormula pPathFormula, AbstractionElement pLastAbstraction) {
      super(pPathFormula);
      lastAbstraction = checkNotNull(pLastAbstraction);
    }

    @Override
    public boolean isAbstractionElement() {
      return false;
    }

    public AbstractionElement getLastAbstraction() {
      return lastAbstraction;
    }

    ImpactAbstractElement getMergedInto() {
      return mergedInto;
    }

    void setMergedInto(ImpactAbstractElement pMergedInto) {
      Preconditions.checkNotNull(pMergedInto);
      mergedInto = pMergedInto;
    }

    @Override
    public Formula getStateFormula() {
      return lastAbstraction.getStateFormula();
    }
  }
}