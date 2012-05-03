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
package org.sosy_lab.cpachecker.cpa.bdd;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

public class BDDDomain implements AbstractDomain {

  private final NamedRegionManager rmgr;

  public BDDDomain(NamedRegionManager manager) {
    this.rmgr = manager;
  }

  @Override
  public boolean isLessOrEqual(AbstractElement newElement, AbstractElement reachedElement) {
      // returns true if element1 < element2 on lattice
      // true if newElement represents less states (and a subset of the states of) reachedElement
    if (newElement instanceof BDDElement && reachedElement instanceof BDDElement){
      BDDElement newElem = (BDDElement)newElement;
      BDDElement reachedElem = (BDDElement)reachedElement;
      return rmgr.entails(newElem.getRegion(), reachedElem.getRegion());
    } else {
      throw new IllegalArgumentException("Called with non-BDD-Elements");
    }
  }

  @Override
  public AbstractElement join(AbstractElement element1, AbstractElement element2) {
    Region region1 = ((BDDElement)element1).getRegion();
    Region region2 = ((BDDElement)element2).getRegion();
    Region result = rmgr.makeOr(region1, region2);
    if (result.equals(region1)) {
      return element1;
    } else if (result.equals(region2)) {
      return element2;
    } else {
      return new BDDElement(result, rmgr);
    }
  }
}
