// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.propertychecker;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Checks if a certain variable has a specific value at a specific location marked by a label in the
 * program.
 */
public class SingleValueChecker extends PerElementPropertyChecker {

  private final MemoryLocation varValRep;
  private final Value varValBigInt;
  private final Value varValLong;
  private final String labelLocVarVal;

  public SingleValueChecker(
      final String varWithSingleValue,
      final String varValue,
      final String labelForLocationWithSingleValue) {
    varValRep = MemoryLocation.parseExtendedQualifiedName(varWithSingleValue);
    labelLocVarVal = labelForLocationWithSingleValue;
    varValBigInt = new NumericValue(new BigInteger(varValue));
    varValLong = new NumericValue(Long.parseLong(varValue));
  }

  @Override
  public boolean satisfiesProperty(final AbstractState pElemToCheck)
      throws UnsupportedOperationException {
    // check if value correctly specified at location
    CFANode node = AbstractStates.extractLocation(pElemToCheck);
    if (node instanceof CFALabelNode && ((CFALabelNode) node).getLabel().equals(labelLocVarVal)) {
      Value value =
          AbstractStates.extractStateByType(pElemToCheck, ValueAnalysisState.class)
              .getValueAndTypeFor(varValRep)
              .getValue();
      if (!value.isExplicitlyKnown() || !(value.equals(varValBigInt) || value.equals(varValLong))) {
        return false;
      }
    }
    return true;
  }
}
