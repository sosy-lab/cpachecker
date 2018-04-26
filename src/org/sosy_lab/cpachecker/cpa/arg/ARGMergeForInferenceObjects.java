/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg;

import org.sosy_lab.cpachecker.core.interfaces.IOMergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ARGMergeForInferenceObjects implements IOMergeOperator {

  private final IOMergeOperator wrappedMerge;

  public ARGMergeForInferenceObjects(IOMergeOperator merge) {
    wrappedMerge = merge;
  }

  @Override
  public InferenceObject merge(
      InferenceObject pState1, InferenceObject pState2, Precision pPrecision)
      throws CPAException, InterruptedException {
    ARGInferenceObject object1 = (ARGInferenceObject) pState1;
    ARGInferenceObject object2 = (ARGInferenceObject) pState2;

    /*assert !object1.isCovered() : "Trying to merge covered element " + object1;

    if (!object2.mayCover()) {
      // elements that may not cover should also not be used for merge
      return object2;
    }

    if (object1.getMergedWith() != null) {
      // element was already merged into another element, don't try to widen argElement2
      // TODO In the optimal case (if all merge & stop operators as well as the reached set partitioning fit well together)
      // this case shouldn't happen, but it does sometimes (at least with ExplicitCPA+FeatureVarsCPA).
      return object2;
    }*/

    InferenceObject wrappedState1 = object1.getWrappedObject();
    InferenceObject wrappedState2 = object2.getWrappedObject();
    InferenceObject retElement = wrappedMerge.merge(wrappedState1, wrappedState2, pPrecision);

    boolean continueMerge = !retElement.equals(wrappedState2);
    if (!continueMerge) {
      return object2;
    }

    ARGInferenceObject mergedElement = new ARGInferenceObject(retElement, null);

    // now replace argElement2 by mergedElement in ARG
    /*object2.replaceInARGWith(mergedElement);

    // and also replace argElement1 with it
    for (ARGState parentOfElement1 : object1.getParents()) {
      mergedElement.addParent(parentOfElement1);
    }

    // argElement1 is the current successor, it does not have any children yet and covered nodes yet
    assert object1.getChildren().isEmpty();
    assert object1.getCoveredByThis().isEmpty();

    // ARGElement1 will only be removed from ARG if stop(e1, reached) returns true.
    // So we can't actually remove it now, but we need to remember this later.
    object1.setMergedWith(mergedElement);*/
    return mergedElement;
  }

}
