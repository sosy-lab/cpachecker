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
import org.sosy_lab.cpachecker.core.defaults.TauInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.IOMergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ThreadModularMergeOperator implements MergeOperator {

  private final MergeOperator stateMerge;
  private final IOMergeOperator ioMerge;

  public ThreadModularMergeOperator(MergeOperator sMerge, IOMergeOperator iMerge) {
    stateMerge = sMerge;
    ioMerge = iMerge;
  }

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {
    ThreadModularState tmState1 = (ThreadModularState) pState1;
    ThreadModularState tmState2 = (ThreadModularState) pState2;

    InferenceObject object1 = tmState1.getInferenceObject();
    InferenceObject object2 = tmState2.getInferenceObject();

    if (object1 == EmptyInferenceObject.getInstance()
        || object2 == EmptyInferenceObject.getInstance()) {
      assert false : "We should not add them";
    }

    InferenceObject mergedIO;
    if (object1 == TauInferenceObject.getInstance()
        && object2 == TauInferenceObject.getInstance()) {
      mergedIO = object1;
    } else if (object1 == TauInferenceObject.getInstance()
        || object2 == TauInferenceObject.getInstance()) {
      // Do not merge tau object and a real one
      return pState2;
    } else {
      mergedIO =
          ioMerge.merge(tmState1.getInferenceObject(), tmState2.getInferenceObject(), pPrecision);
    }

    AbstractState mergedState =
        stateMerge.merge(tmState1.getWrappedState(), tmState2.getWrappedState(), pPrecision);

    if (mergedState == tmState2.getWrappedState() && mergedIO == tmState2.getInferenceObject()) {
      return pState2;
    } else {
      return new ThreadModularState(mergedState, mergedIO);
    }
  }
}
