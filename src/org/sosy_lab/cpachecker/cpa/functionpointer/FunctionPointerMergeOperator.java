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
package org.sosy_lab.cpachecker.cpa.functionpointer;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class FunctionPointerMergeOperator implements MergeOperator {

  private final MergeOperator wrappedMerge;

  public FunctionPointerMergeOperator(MergeOperator pWrappedMerge) {
    wrappedMerge = pWrappedMerge;
  }

  @Override
  public AbstractState merge(AbstractState pElement1,
      AbstractState pElement2, Precision pPrecision) throws CPAException {

    FunctionPointerState fpElement1 = (FunctionPointerState)pElement1;
    FunctionPointerState fpElement2 = (FunctionPointerState)pElement2;

    if (!fpElement1.isLessOrEqualThan(fpElement2)) {
      // don't merge here
      return pElement2;
    }

    AbstractState wrappedState1 = fpElement1.getWrappedState();
    AbstractState wrappedState2 = fpElement2.getWrappedState();
    AbstractState retElement = wrappedMerge.merge(wrappedState1, wrappedState2, pPrecision);
    if (retElement.equals(wrappedState2)) {
      return pElement2;
    }

    return fpElement2.createDuplicateWithNewWrappedState(retElement);
  }
}
