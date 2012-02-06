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
package org.sosy_lab.cpachecker.cpa.art;

import java.util.Collections;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ARTMergeJoin implements MergeOperator {

  private final MergeOperator wrappedMerge;

  public ARTMergeJoin(MergeOperator pWrappedMerge) {
    wrappedMerge = pWrappedMerge;
  }

  @Override
  public AbstractElement merge(AbstractElement pElement1,
      AbstractElement pElement2, Precision pPrecision) throws CPAException {

    ARTElement artElement1 = (ARTElement)pElement1;
    ARTElement artElement2 = (ARTElement)pElement2;


    // covered elements are not in the reached set
    assert !artElement1.isCovered();
    assert !artElement2.isCovered();

    if (!artElement2.mayCover()) {
      // elements that may not cover should also not be used for merge
      return pElement2;
    }

    AbstractElement wrappedElement1 = artElement1.getWrappedElement();
    AbstractElement wrappedElement2 = artElement2.getWrappedElement();
    AbstractElement retElement = wrappedMerge.merge(wrappedElement1, wrappedElement2, pPrecision);
    if(retElement.equals(wrappedElement2)){
      return pElement2;
    }

    ARTElement mergedElement = new ARTElement(retElement, Collections.<ARTElement, CFAEdge> emptyMap());

    // now replace artElement2 by mergedElement in ART

    for ( Entry<ARTElement, CFAEdge> entry : artElement1.getParentMap().entrySet()){
      mergedElement.addParent(entry.getKey(), entry.getValue());
    }

    for ( Entry<ARTElement, CFAEdge> entry : artElement2.getParentMap().entrySet()){
      mergedElement.addParent(entry.getKey(), entry.getValue());
    }

    // artElement1 is the current successor, it does not have any children yet
    assert artElement1.getChildARTs().isEmpty();

    // TODO check if correct - can get a loop in ARTby the two lines below
    for (Entry<ARTElement, CFAEdge> entry : artElement2.getChildMap().entrySet()){
      entry.getKey().addParent(mergedElement, entry.getValue());
    }


    // artElement1 will only be removed from ART if stop(e1, reached) returns true
    artElement2.removeFromART();

    artElement1.setMergedWith(mergedElement);

    // in rely-guarantee analysis, we need this info
    artElement2.setMergedWith(mergedElement);

    return mergedElement;
  }
}
