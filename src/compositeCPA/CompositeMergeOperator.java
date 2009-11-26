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
package compositeCPA;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cpa.common.CallStack;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import exceptions.CPAException;

public class CompositeMergeOperator implements MergeOperator{
  
  private final CompositeDomain compositeDomain;
  private final List<MergeOperator> mergeOperators;

  public CompositeMergeOperator(CompositeDomain compositeDomain, List<MergeOperator> mergeOperators)
  {
    this.compositeDomain = compositeDomain;
    this.mergeOperators = mergeOperators;
  }

  // TODO fix this part
  public AbstractElement merge (AbstractElement element1, AbstractElement element2, Precision precision) throws CPAException
  {
    return merge((AbstractElementWithLocation)element1, (AbstractElementWithLocation)element2, precision);
  }

  public AbstractElementWithLocation merge(AbstractElementWithLocation element1,
                                           AbstractElementWithLocation element2,
                                           Precision precision) throws CPAException {

    // TODO check
    if (element1 == compositeDomain.getBottomElement()) {
      return element2;
    }

    // Merge Sep Code
    CompositeElement comp1 = (CompositeElement) element1;
    CompositeElement comp2 = (CompositeElement) element2;
    CompositePrecision prec = (CompositePrecision) precision;

    assert(comp1.getNumberofElements() == comp2.getNumberofElements());

    if (!comp1.getElementWithLocation().equals (comp2.getElementWithLocation()))
    {
      return element2;
    }
    // check for call stack
    CallStack cs1 = comp1.getCallStack();
    CallStack cs2 = comp2.getCallStack();

    // do not merge if call stacks are not equal
    if(!cs1.equals(cs2)){
      return element2;
    }

    List<AbstractElement> mergedElements = new ArrayList<AbstractElement> ();
    Iterator<AbstractElement> iter1 = comp1.getElements().iterator();
    Iterator<AbstractElement> iter2 = comp2.getElements().iterator();
    Iterator<Precision> precIter = (prec == null) ? null : prec.getPrecisions().iterator();

    boolean identicElements = true;
    for (MergeOperator mergeOp : mergeOperators) {
      AbstractElement absElem1 = iter1.next();
      AbstractElement absElem2 = iter2.next();
      AbstractElement merged = mergeOp.merge(absElem1, absElem2, (precIter == null) ? null : precIter.next());
      // if the element is not location and it is not merged we do not need to merge
      if (merged != absElem2) {
        identicElements = false;
      }
      mergedElements.add (merged);
    }
    
    if (identicElements) {
      return element2;
    } else {
      return new CompositeElement(mergedElements, cs2);
    }
  }
}
