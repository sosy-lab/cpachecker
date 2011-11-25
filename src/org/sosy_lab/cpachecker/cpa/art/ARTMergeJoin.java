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

    if (artElement1.getMergedWith() != null) {
      // element was already merged into another element, don't try to widen artElement2
      // TODO In the optimal case (if all merge & stop operators as well as the reached set partitioning fit well together)
      // this case shouldn't happen, but it does sometimes (at least with ExplicitCPA+FeatureVarsCPA).
      return pElement2;
    }

    AbstractElement wrappedElement1 = artElement1.getWrappedElement();
    AbstractElement wrappedElement2 = artElement2.getWrappedElement();
    AbstractElement retElement = wrappedMerge.merge(wrappedElement1, wrappedElement2, pPrecision);
    if(retElement.equals(wrappedElement2)){
      return pElement2;
    }

    ARTElement mergedElement = new ARTElement(retElement, null);

    // now replace artElement2 by mergedElement in ART
    artElement2.replaceInARTWith(mergedElement);

    // and also replace artElement1 with it
    for (ARTElement parentOfElement1 : artElement1.getParents()) {
      mergedElement.addParent(parentOfElement1);
    }

    // artElement1 is the current successor, it does not have any children yet and covered nodes yet
    assert artElement1.getChildren().isEmpty();
    assert artElement1.getCoveredByThis().isEmpty();

    // ArtElement1 will only be removed from ART if stop(e1, reached) returns true.
    // So we can't actually remove it now, but we need to remember this later.
    artElement1.setMergedWith(mergedElement);
    return mergedElement;
  }
}
