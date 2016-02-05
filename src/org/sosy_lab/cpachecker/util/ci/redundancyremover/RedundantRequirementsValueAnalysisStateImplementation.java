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
import org.sosy_lab.cpachecker.cpa.value.type.ArrayValue;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.EnumConstantValue;
import org.sosy_lab.cpachecker.cpa.value.type.NullValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.ci.redundancyremover.RedundantRequirementsRemover.RedundantRequirementsRemoverImplementation;


public class RedundantRequirementsValueAnalysisStateImplementation extends
    RedundantRequirementsRemoverImplementation<ValueAnalysisState, Value> {

  private static final long serialVersionUID = 2875464105471673418L;

  @Override
  public int compare(Value pO1, Value pO2) {
    // one of arguments null -> NullPointerException
    // ClassCastException if p01 or p02 instanceof ArrayValue, BooleanValue, EnumConstantValue, NullValue
    // 0 if both are unknown Value
    // -1 if p02 unknown Value
    // 1 if p01 unknown value
    // otherwise p01.doubleValues()-pO2.doubleValues()
    if (pO1 == null || pO2 == null) {
      throw new NullPointerException("At least one of the arguments " + pO1 + " or " + pO2 + " is null.");
    } else if (pO1 instanceof ArrayValue || pO2 instanceof ArrayValue ||
        pO1 instanceof BooleanValue || pO2 instanceof BooleanValue ||
        pO1 instanceof EnumConstantValue || pO2 instanceof EnumConstantValue ||
        pO1 instanceof NullValue || pO2 instanceof NullValue) {
      throw new ClassCastException("Expected NumericValue.");
    } else if (pO1.isUnknown() && pO2.isUnknown()) {
      return 0;
    } else if (pO2.isUnknown()) {
      return -1;
    } else if (pO1.isUnknown()) {
      return 1;
    }

    return (int) (pO1.asNumericValue().getNumber().doubleValue() - pO2.asNumericValue().getNumber().doubleValue()); // TODO
  }

  @Override
  protected boolean covers(Value pCovering, Value pCovered) {
    // return true if pCovering UnknownValue, pCovering equals pCovered
    // otherwise false
    if (pCovering.isUnknown() || pCovering.equals(pCovered)) {
      return true;
    }

    return false;
  }

  @Override
  protected Value getAbstractValue(ValueAnalysisState pAbstractState, String pVarOrConst) {
    // if pVarOrConst number, return NumericValue
    // if state contains pVarOrConst return value saved in state
    // otherwise unknown

    int constant;
    try {
      constant = Integer.parseInt(pVarOrConst);
      return new NumericValue(constant);
    } catch (NumberFormatException e) {
      if (pAbstractState.contains(pVarOrConst)) {
        return pAbstractState.getValueFor(pVarOrConst);
      }
    }

    return Value.UnknownValue.getInstance();
  }

  @Override
  protected Value[] emptyArrayOfSize(int pSize) {
    // similar to RedundantRequirementsRemoverIntervalStateImplementation
    return new Value[pSize];
  }

  @Override
  protected Value[][] emptyMatrixOfSize(int pSize) {
    // similar to RedundantRequirementsRemoverIntervalStateImplementation
    return new Value[pSize][];
  }

  @Override
  protected ValueAnalysisState extractState(AbstractState pWrapperState) {
    // AbstractStates.extractStateByType....
    return AbstractStates.extractStateByType(pWrapperState, ValueAnalysisState.class);
  }

}
