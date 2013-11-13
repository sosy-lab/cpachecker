/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.util.Stack;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;


public class ARGMergeJoinPredicatedAnalysis implements MergeOperator{

  private final MergeOperator wrappedMerge;

  public ARGMergeJoinPredicatedAnalysis(MergeOperator pWrappedMerge) {
    wrappedMerge = pWrappedMerge;
  }

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision) throws CPAException,
      InterruptedException {

    ARGState argElement1 = (ARGState)pState1;
    ARGState argElement2 = (ARGState)pState2;

    assert !argElement1.isCovered() : "Trying to merge covered element " + argElement1;

    if (!argElement2.mayCover()) {
      // elements that may not cover should also not be used for merge
      return pState2;
    }

    if (argElement1.getMergedWith() != null) {
      // element was already merged into another element, don't try to widen argElement2
      // TODO In the optimal case (if all merge & stop operators as well as the reached set partitioning fit well together)
      // this case shouldn't happen, but it does sometimes (at least with ExplicitCPA+FeatureVarsCPA).
      return pState2;
    }

    AbstractState wrappedState1 = argElement1.getWrappedState();
    AbstractState wrappedState2 = argElement2.getWrappedState();
    AbstractState retElement = wrappedMerge.merge(wrappedState1, wrappedState2, pPrecision);
    if (retElement.equals(wrappedState2)) {
      return pState2;
    }

    ARGState mergedElement = new ARGState(retElement, null);

    // now replace argElement2 by mergedElement in ARG
    deleteChildren(argElement2);
    argElement2.replaceInARGWith(mergedElement);

    if (mergedElement.isTarget()) { throw new PredicatedAnalysisPropertyViolationException(
        "Property violated during merge", mergedElement); }

    return mergedElement;
  }

  private void deleteChildren(ARGState parent) {
    Stack<ARGState> toProcess = new Stack<>();
    toProcess.add(parent);

    ARGState current;
    ARGState child;
    ARGState covered;

    while (!toProcess.isEmpty()) {
      current = toProcess.pop();

      // delete connection to children
      while (current.getChildren().size() != 0) {
        child = current.getChildren().iterator().next();
        // relink or delete child if it is not connected by another parent
        if (child.getParents().size() == 1) {
          if (child.getCoveredByThis().size() != 0) {
            // relink child in ARG to parent of first covered element
            covered = child.getCoveredByThis().iterator().next();
            // remove coverage relation
            covered.uncover();
            covered.replaceInARGWith(child);
          } else {
            // add child for deletion
            toProcess.add(child);
          }
        }
      }
    }
  }

}
