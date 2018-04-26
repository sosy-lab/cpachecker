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
package org.sosy_lab.cpachecker.core.interfaces;

import static com.google.common.collect.FluentIterable.from;

import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.defaults.TauInferenceObject;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;

public interface TransferRelationTM extends TransferRelation {

  default Collection<Pair<AbstractState, InferenceObject>> getAbstractSuccessors(
      AbstractState pState, InferenceObject pInferenceObject, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    throw new CPATransferException("Unsupported operation");
  }

  default Collection<Pair<AbstractState, InferenceObject>> getAbstractSuccessorForEdge(
      AbstractState pState, InferenceObject pInferenceObject, Precision pPrecision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {

    if (pInferenceObject == TauInferenceObject.getInstance()) {
      return from(getAbstractSuccessorsForEdge(pState, pPrecision, cfaEdge))
          .transform(
              s -> Pair.of((AbstractState) s, (InferenceObject) EmptyInferenceObject.getInstance()))
          .toList();
    } else {
      return Collections.singleton(Pair.of(pState, EmptyInferenceObject.getInstance()));
    }
  }
}
