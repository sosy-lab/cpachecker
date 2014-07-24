/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.pcc.propertychecker;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.Value;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Checks if a certain variable has a specific value at a specific location marked by a label in the program.
 */
public class SingleValueChecker extends PerElementPropertyChecker {

  private final MemoryLocation varValRep;
  private final Value varValBigInt;
  private final Value varValLong;
  private final String labelLocVarVal;

  public SingleValueChecker(final String varWithSingleValue, final String varValue,
      final String labelForLocationWithSingleValue) {
    varValRep = MemoryLocation.valueOf(varWithSingleValue);
    labelLocVarVal = labelForLocationWithSingleValue;
    varValBigInt = new NumericValue(new BigInteger(varValue));
    varValLong = new NumericValue(Long.parseLong(varValue));
  }

  @Override
  public boolean satisfiesProperty(final AbstractState pElemToCheck) throws UnsupportedOperationException {
    // check if value correctly specified at location
    CFANode node = AbstractStates.extractLocation(pElemToCheck);
    if (node instanceof CLabelNode && ((CLabelNode) node).getLabel().equals(labelLocVarVal)) {
      Value value =
          AbstractStates.extractStateByType(pElemToCheck, ValueAnalysisState.class).getConstantsMapView()
              .get(varValRep);
      if (value == null || !value.isExplicitlyKnown() ||
          !(value.equals(varValBigInt) || value.equals(varValLong))) { return false; }
    }
    return true;
  }

}
