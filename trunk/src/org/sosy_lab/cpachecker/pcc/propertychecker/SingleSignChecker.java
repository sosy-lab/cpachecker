// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.propertychecker;

import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.sign.SIGN;
import org.sosy_lab.cpachecker.cpa.sign.SignState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class SingleSignChecker extends PerElementPropertyChecker {

  private final String varName;
  private final SIGN value;
  private final String label;

  public SingleSignChecker(String pName, String pValue, String pLabel) {
    varName = pName;
    value = SIGN.valueOf(pValue);
    label = pLabel;
  }

  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck)
      throws UnsupportedOperationException {
    CFANode node = AbstractStates.extractLocation(pElemToCheck);
    if (node instanceof CFALabelNode && ((CFALabelNode) node).getLabel().equals(label)) {
      SignState state = AbstractStates.extractStateByType(pElemToCheck, SignState.class);
      if (state != null) {
        if (state.getSignForVariable(varName) == value) {
          return true;
        }
      }
      return false;
    }
    return true;
  }
}
