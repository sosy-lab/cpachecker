/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.threadmodular;

import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.defaults.EpsilonState;
import org.sosy_lab.cpachecker.core.defaults.TauInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.IOMergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ThreadModularMergeOperator implements MergeOperator {

  private final MergeOperator stateMerge;
  private final IOMergeOperator ioMerge;
  private final ThreadModularStatistics tStats;

  public ThreadModularMergeOperator(
      MergeOperator sMerge,
      IOMergeOperator iMerge,
      ThreadModularStatistics pStatistics) {
    stateMerge = sMerge;
    ioMerge = iMerge;
    tStats = pStatistics;
  }

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {
    ThreadModularState tmState1 = (ThreadModularState) pState1;
    ThreadModularState tmState2 = (ThreadModularState) pState2;

    try {
      tStats.mergeTimer.start();
      AbstractState state1 = tmState1.getWrappedState();
      AbstractState state2 = tmState2.getWrappedState();
      InferenceObject object1 = tmState1.getInferenceObject();
      InferenceObject object2 = tmState2.getInferenceObject();

      if (object1 == EmptyInferenceObject.getInstance()
          || object2 == EmptyInferenceObject.getInstance()) {
        assert false : "We should not add them";
      }

      InferenceObject mergedIO;
      if (object1 == TauInferenceObject.getInstance()
          && object2 == TauInferenceObject.getInstance()) {
        mergedIO = object2;
      } else if (object1 == TauInferenceObject.getInstance()
          || object2 == TauInferenceObject.getInstance()) {
        // Do not merge tau object and a real one
        return pState2;
      } else {
        tStats.ioMergeTimer.start();
        mergedIO = ioMerge.merge(object1, object2, pPrecision);
        tStats.ioMergeTimer.stop();
      }

      AbstractState mergedState;
      if (state1 == EpsilonState.getInstance() || state2 == EpsilonState.getInstance()) {
        mergedState = state2;
      } else {
        ARGState argElement1 = (ARGState) state1;
        ARGState argElement2 = (ARGState) state2;

        if (argElement1.isCovered()) {
          return pState2;
        }

        while (argElement2.getReplacedWith() != null) {
          argElement2 = argElement2.getReplacedWith();
        }

        if (!argElement2.mayCover()) {
          // elements that may not cover should also not be used for merge
          return pState2;
        }

        if (argElement1.getMergedWith() != null) {
          // element was already merged into another element, don't try to widen argElement2
          // TODO In the optimal case (if all merge & stop operators as well as the reached set
          // partitioning fit well together)
          // this case shouldn't happen, but it does sometimes (at least with
          // ExplicitCPA+FeatureVarsCPA).
          return pState2;
        }
        AbstractState aState1 = argElement1.getWrappedState();
        AbstractState aState2 = argElement2.getWrappedState();
        tStats.stateMergeTimer.start();
        AbstractState innerState = stateMerge.merge(aState1, aState2, pPrecision);
        tStats.stateMergeTimer.stop();

        if (!innerState.equals(aState2)) {
          tStats.argMergeTimer.start();
          ARGState mergedElement = new ARGState(innerState, null);

          // now replace argElement2 by mergedElement in ARG
          argElement2.replaceInARGWith(mergedElement);

          // and also replace argElement1 with it
          for (ARGState parentOfElement1 : argElement1.getParents()) {
            if (mergedElement != parentOfElement1) {
              mergedElement.addParent(parentOfElement1);
            }
          }

          // argElement1 is the current successor, it does not have any children yet and covered
          // nodes
          // yet
          assert argElement1.getChildren().isEmpty();
          assert argElement1.getCoveredByThis().isEmpty();

          // ARGElement1 will only be removed from ARG if stop(e1, reached) returns true.
          // So we can't actually remove it now, but we need to remember this later.
          argElement1.setMergedWith(mergedElement);

          mergedState = mergedElement;
          tStats.argMergeTimer.stop();
        } else {
          mergedState = argElement2;
        }
      }

      if (mergedState == tmState2.getWrappedState() && mergedIO == tmState2.getInferenceObject()) {
        return pState2;
      } else {
        return new ThreadModularState(mergedState, mergedIO);
      }
    } finally {
      tStats.mergeTimer.stop();
    }
  }
}
