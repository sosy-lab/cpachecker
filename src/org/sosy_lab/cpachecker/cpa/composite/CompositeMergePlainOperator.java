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
package org.sosy_lab.cpachecker.cpa.composite;

import java.util.Iterator;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.ImmutableList;

/**
 * Provides a MergeOperator implementation that just delegates to the component
 * CPAs without any further logic.
 */
public class CompositeMergePlainOperator implements MergeOperator{

  private final ImmutableList<MergeOperator> mergeOperators;

  public CompositeMergePlainOperator(ImmutableList<MergeOperator> mergeOperators)
  {
    this.mergeOperators = mergeOperators;
  }

  @Override
  public AbstractState merge(AbstractState element1,
                               AbstractState element2,
                               Precision precision) throws CPAException {

    // Merge Sep Code
    CompositeState comp1 = (CompositeState) element1;
    CompositeState comp2 = (CompositeState) element2;
    CompositePrecision prec = (CompositePrecision) precision;

    assert(comp1.getNumberofElements() == comp2.getNumberofElements());

    ImmutableList.Builder<AbstractState> mergedElements = ImmutableList.builder();
    Iterator<AbstractState> iter1 = comp1.getWrappedElements().iterator();
    Iterator<AbstractState> iter2 = comp2.getWrappedElements().iterator();
    Iterator<Precision> precIter = prec.getPrecisions().iterator();

    boolean identicElements = true;
    for (MergeOperator mergeOp : mergeOperators) {
      AbstractState absElem1 = iter1.next();
      AbstractState absElem2 = iter2.next();
      AbstractState merged = mergeOp.merge(absElem1, absElem2, precIter.next());

      if (merged != absElem2) {
        identicElements = false;
      }
      mergedElements.add (merged);
    }

    if (identicElements) {
      return element2;
    } else {
      return new CompositeState(mergedElements.build());
    }
  }
}
