/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.composite;

import java.util.Iterator;

import org.sosy_lab.cpachecker.core.CallStack;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.ImmutableList;

public class CompositeMergeOperator implements MergeOperator{

  private final ImmutableList<MergeOperator> mergeOperators;

  public CompositeMergeOperator(ImmutableList<MergeOperator> mergeOperators)
  {
    this.mergeOperators = mergeOperators;
  }

  @Override
  public AbstractElement merge(AbstractElement element1,
                               AbstractElement element2,
                               Precision precision) throws CPAException {

    // Merge Sep Code
    CompositeElement comp1 = (CompositeElement) element1;
    CompositeElement comp2 = (CompositeElement) element2;
    CompositePrecision prec = (CompositePrecision) precision;

    assert(comp1.getNumberofElements() == comp2.getNumberofElements());

    if (!comp1.retrieveLocationElement().equals (comp2.retrieveLocationElement()))
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

    ImmutableList.Builder<AbstractElement> mergedElements = ImmutableList.builder();
    Iterator<AbstractElement> iter1 = comp1.getElements().iterator();
    Iterator<AbstractElement> iter2 = comp2.getElements().iterator();
    Iterator<Precision> precIter = prec.getPrecisions().iterator();

    boolean identicElements = true;
    for (MergeOperator mergeOp : mergeOperators) {
      AbstractElement absElem1 = iter1.next();
      AbstractElement absElem2 = iter2.next();
      AbstractElement merged = mergeOp.merge(absElem1, absElem2, precIter.next());
      // if the element is not location and it is not merged we do not need to merge
      if (merged != absElem2) {
        identicElements = false;
      }
      mergedElements.add (merged);
    }

    if (identicElements) {
      return element2;
    } else {
      return new CompositeElement(mergedElements.build(), cs2);
    }
  }
}
