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
package org.sosy_lab.cpachecker.cpa.composite;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.lock.LockInferenceObject;
import org.sosy_lab.cpachecker.cpa.thread.ThreadInferenceObject;
import org.sosy_lab.cpachecker.exceptions.CPAException;


public class CompositeMergeForInferenceObjects implements MergeOperator {

  private final ImmutableList<MergeOperator> mergeOperators;

  CompositeMergeForInferenceObjects(ImmutableList<MergeOperator> mergeOperators) {
    this.mergeOperators = mergeOperators;
  }

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision) throws CPAException, InterruptedException {
    if (pState1 == EmptyInferenceObject.getInstance() || pState2 == EmptyInferenceObject.getInstance()) {
      return pState2;
    }

    CompositeInferenceObject object1 = (CompositeInferenceObject) pState1;
    CompositeInferenceObject object2 = (CompositeInferenceObject) pState2;
    CompositePrecision precision = (CompositePrecision) pPrecision;

    assert (object1.getSize() == object2.getSize());

    ImmutableList.Builder<InferenceObject> mergedStates = ImmutableList.builder();
    Iterator<InferenceObject> iter1 = object1.getInferenceObjects().iterator();
    Iterator<InferenceObject> iter2 = object2.getInferenceObjects().iterator();
    Iterator<Precision> iterPrec = precision.getWrappedPrecisions().iterator();

    boolean identicalStates = true;
    for (MergeOperator mergeOp : mergeOperators) {
      AbstractState absSuccessorState = iter1.next();
      AbstractState absReachedState = iter2.next();
      AbstractState mergedState;

      if (absSuccessorState instanceof ThreadInferenceObject ||
          absSuccessorState instanceof LockInferenceObject) {
        //Do not merge
        if (absSuccessorState.equals(absReachedState)) {
          mergedState = absReachedState;
        } else {
          return pState2;
        }
      } else {
        mergedState = mergeOp.merge(absSuccessorState, absReachedState, iterPrec.next());
      }

      if (mergedState != absReachedState) {
        identicalStates = false;
      }
      mergedStates.add((InferenceObject) mergedState);
    }

    if (identicalStates) {
      return object2;
    } else {
      return CompositeInferenceObject.create(mergedStates.build());
    }
  }

}
