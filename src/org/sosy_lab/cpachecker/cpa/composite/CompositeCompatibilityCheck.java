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
import java.util.List;
import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CompatibilityCheck;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;


public class CompositeCompatibilityCheck implements CompatibilityCheck {

  private final ImmutableList<CompatibilityCheck> wrappedChecks;

  public CompositeCompatibilityCheck(List<CompatibilityCheck> pList) {
    wrappedChecks = ImmutableList.copyOf(pList);
  }

  @Override
  public boolean compatible(AbstractState pState, InferenceObject pObject) {
    if (pObject == EmptyInferenceObject.getInstance()) {
      return false;
    }

    CompositeState state = (CompositeState) pState;
    CompositeInferenceObject object = (CompositeInferenceObject) pObject;

    for (int i = 0; i < state.getNumberOfStates(); i++) {
      AbstractState innerState = state.get(i);
      InferenceObject innerObject = object.getInferenceObject(i);
      CompatibilityCheck innerCheck = wrappedChecks.get(i);

      boolean result = innerCheck.compatible(innerState, innerObject);
      if (!result) {
        return false;
      }
    }
    return true;
  }

}
