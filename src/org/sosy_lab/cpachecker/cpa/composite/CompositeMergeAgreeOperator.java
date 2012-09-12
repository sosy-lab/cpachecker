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

import java.util.Collections;
import java.util.Iterator;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.ImmutableList;

/**
 * Provides a MergeOperator implementation that delegates to the component CPA.
 * If any of those CPAs returns an element that does not cover both its input
 * elements, this implementation returns its second input element
 * (i.e., it behaves like MergeSep).
 *
 * This operator is good for the combination of CPAs where some CPAs never merge
 * and some may merge.
 *
 * Note that the definition of MergeOperator already requires that the returned
 * element covers the second input element. This implementation relies on that
 * guarantee and always assumes this is true.
 */
public class CompositeMergeAgreeOperator implements MergeOperator {

  private final ImmutableList<MergeOperator> mergeOperators;
  private final ImmutableList<StopOperator> stopOperators;

  public CompositeMergeAgreeOperator(ImmutableList<MergeOperator> mergeOperators, ImmutableList<StopOperator> stopOperators) {
    this.mergeOperators = mergeOperators;
    this.stopOperators = stopOperators;
  }

  @Override
  public AbstractState merge(AbstractState element1,
                               AbstractState element2,
                               Precision precision) throws CPAException {

    // Merge Sep Code
    CompositeState comp1 = (CompositeState) element1;
    CompositeState comp2 = (CompositeState) element2;
    CompositePrecision compositePrec = (CompositePrecision) precision;

    assert(comp1.getNumberOfStates() == comp2.getNumberOfStates());

    ImmutableList.Builder<AbstractState> mergedElements = ImmutableList.builder();
    Iterator<StopOperator> stopIter = stopOperators.iterator();
    Iterator<AbstractState> iter1 = comp1.getWrappedStates().iterator();
    Iterator<AbstractState> iter2 = comp2.getWrappedStates().iterator();
    Iterator<Precision> precIter = compositePrec.getPrecisions().iterator();

    boolean identicElements = true;
    for (MergeOperator mergeOp : mergeOperators) {
      AbstractState absElem1 = iter1.next();
      AbstractState absElem2 = iter2.next();
      Precision prec = precIter.next();
      StopOperator stopOp = stopIter.next();

      AbstractState merged = mergeOp.merge(absElem1, absElem2, prec);

      // check whether merged covers absElem1
      // by definition of MergeOperator, we know it covers absElem2
      if (!stopOp.stop(absElem1, Collections.singleton(merged), prec)) {
        // the result of merge doesn't cover absElem1
        // (which is the successor element currently considered by the CPAAlgorithm
        // We prevent merging for all CPAs in this case, because the current
        // element wouldn't be covered anyway, so widening other elements is just a loss of precision.
        return element2;
      }

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
