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
package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.cpachecker.core.interfaces.IOMergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class MergeForInferenceObjects implements IOMergeOperator {

  private final PredicateInferenceObjectManager mngr;

  public MergeForInferenceObjects(PredicateInferenceObjectManager pMngr) {
    mngr = pMngr;
  }

  @Override
  public InferenceObject merge(
      InferenceObject pState1, InferenceObject pState2, Precision pPrecision)
      throws CPAException, InterruptedException {

    PredicateInferenceObject object1 = (PredicateInferenceObject) pState1;
    PredicateInferenceObject object2 = (PredicateInferenceObject) pState2;

    InferenceObject result = mngr.join(object1, object2);

    if (result.equals(object2)) {
      return pState2;
    } else {
      return result;
    }
  }

}
