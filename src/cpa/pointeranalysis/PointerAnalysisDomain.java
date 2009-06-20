/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.pointeranalysis;

import java.util.Map;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import exceptions.CPAException;

/**
 * @author Philipp Wendler
 */
public class PointerAnalysisDomain implements AbstractDomain {

  private static class PointerAnalysisBottomElement extends PointerAnalysisElement {
    @Override
    public String toString() {
      return "<PointerAnalysis BOTTOM>";
    }
  }
  
  private static class PointerAnalysisTopElement extends PointerAnalysisElement {
    @Override
    public String toString() {
      return "<PointerAnalysis TOP>";
    }
  }
  
  private static class PointerAnalysisJoinOperator implements JoinOperator {
    @Override
    public AbstractElement join(AbstractElement element1,
                                AbstractElement element2) throws CPAException {
      
      PointerAnalysisElement pointerElement1 = (PointerAnalysisElement)element1;
      PointerAnalysisElement pointerElement2 = (PointerAnalysisElement)element2;
      
      PointerAnalysisElement pointerElementNew = pointerElement1.clone();
      
      Map<String, Pointer> pointers2   = pointerElement2.getGlobalPointers();
      Map<String, Pointer> pointersNew = pointerElementNew.getGlobalPointers();
      for (String name : pointers2.keySet()) {
        if (pointersNew.containsKey(name)) {
          pointersNew.get(name).join(pointers2.get(name));
        } else {
          pointersNew.put(name, pointers2.get(name).clone());
        }
      }
      
      pointers2   = pointerElement2.getLocalPointers();
      pointersNew = pointerElementNew.getLocalPointers();
      for (String name : pointers2.keySet()) {
        if (pointersNew.containsKey(name)) {
          pointersNew.get(name).join(pointers2.get(name));
        } else {
          pointersNew.put(name, pointers2.get(name).clone());
        }
      }
      // only the local variables of the current context need to be joined,
      // the others are already identical (were joined before calling the last function)

      return pointerElementNew;
    }
  }
  
  private static class PointerAnalysisPartialOrder implements PartialOrder {
    @Override
    public boolean satisfiesPartialOrder(AbstractElement pElement1,
                                         AbstractElement pElement2)
                                         throws CPAException {
      // TODO partial Order
      return false;
    }
  }
  
  private static final JoinOperator joinOperator = new PointerAnalysisJoinOperator();
  private static final PartialOrder partialOrder = new PointerAnalysisPartialOrder();
  private static final PointerAnalysisElement bottomElement = new PointerAnalysisBottomElement();
  private static final PointerAnalysisElement topElement = new PointerAnalysisTopElement();
  
  @Override
  public JoinOperator getJoinOperator() {
    return joinOperator;
  }

  @Override
  public PartialOrder getPartialOrder() {
    return partialOrder;
  }

  @Override
  public PointerAnalysisElement getBottomElement() {
    return bottomElement;
  }

  @Override
  public PointerAnalysisElement getTopElement() {
    return topElement;
  }
}
