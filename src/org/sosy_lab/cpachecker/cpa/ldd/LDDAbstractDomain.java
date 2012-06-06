/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.ldd;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.ldd.LDDRegionManager;


public class LDDAbstractDomain implements AbstractDomain {

  private final LDDRegionManager regionManager;

  private final AbstractState topElement;

  public LDDAbstractDomain(LDDRegionManager regionManager) {
    this.regionManager = regionManager;
    this.topElement = new LDDAbstractElement(regionManager.makeTrue());
  }

  @Override
  public boolean isLessOrEqual(AbstractState newElement, AbstractState reachedElement) throws CPAException {
    if (this.topElement.equals(reachedElement) || newElement.equals(reachedElement)) { return true; }
    if (newElement instanceof LDDAbstractElement && reachedElement instanceof LDDAbstractElement) {
      LDDAbstractElement lddElement1 = (LDDAbstractElement) newElement;
      LDDAbstractElement lddElement2 = (LDDAbstractElement) reachedElement;
      return this.regionManager.entails(lddElement1.getRegion(), lddElement2.getRegion());
    }
    return false;
  }

  @Override
  public AbstractState join(AbstractState pElement1, AbstractState pElement2) throws CPAException {
    if (isLessOrEqual(pElement1, pElement2)) { return pElement2; }
    if (isLessOrEqual(pElement2, pElement1)) { return pElement1; }
    if (pElement1 instanceof LDDAbstractElement && pElement2 instanceof LDDAbstractElement) {
      LDDAbstractElement lddElement1 = (LDDAbstractElement) pElement1;
      LDDAbstractElement lddElement2 = (LDDAbstractElement) pElement2;
      return new LDDAbstractElement(this.regionManager.makeOr(lddElement1.getRegion(), lddElement2.getRegion()));
    }
    return this.topElement;
  }

}
