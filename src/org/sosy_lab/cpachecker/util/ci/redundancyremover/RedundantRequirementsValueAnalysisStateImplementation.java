/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ci.redundancyremover;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.ci.redundancyremover.RedundantRequirementsRemover.RedundantRequirementsRemoverImplementation;


public class RedundantRequirementsValueAnalysisStateImplementation extends
    RedundantRequirementsRemoverImplementation<ValueAnalysisState, Value> {

  @Override
  public int compare(Value pO1, Value pO2) {
    // TODO
    // one of arguments null -> NullPointerException
    // ClassCastException if p01 or p02 instanceof ArrayValue, BooleanValue, EnumConstantValue, NullValue
    // 0 if both are unknown Value
    // -1 if p02 unknown Value
    // 1 if p01 unknown value
    // otherwise p01.doubleValues()-pO2.doubleValues()
    return 0;
  }

  @Override
  protected Value getAbstractValue(ValueAnalysisState pAbstractState, String pVarOrConst) {
    // TODO
    // if pVarOrConst number, return NumericValue
    // if state contains pVarOrConst return value saved in state
    // otherwise unknown
    return null;
  }

  @Override
  protected Value[] emptyArrayOfSize(int pSize) {
    // TODO similar to RedundantRequirementsRemoverIntervalStateImplementation
    return null;
  }

  @Override
  protected Value[][] emptyMatrixOfSize(int pSize) {
    // TODO similar to RedundantRequirementsRemoverIntervalStateImplementation
    return null;
  }

  @Override
  protected ValueAnalysisState extractState(AbstractState pWrapperState) {
    // TODO AbstractStates.extractStateByType....
    return null;
  }

}
