/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.util.Collection;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PropertyChecker;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Checks if a certain variable has a specific value at a specific location marked by a label in the program.
 */
public class SingleValueChecker implements PropertyChecker {

  private final String varValName;
  private final long varVal;
  private final String labelLocVarVal;

  public SingleValueChecker(String varWithSingleValue, String varValue, String labelForLocationWithSingleValue) {
    varValName = varWithSingleValue;
    labelLocVarVal = labelForLocationWithSingleValue;
    varVal = Long.parseLong(varValue);
  }

  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck) throws UnsupportedOperationException {
    // check if value correctly specified at location
    CFANode node = AbstractStates.extractLocation(pElemToCheck);
    if (node instanceof CLabelNode && ((CLabelNode) node).getLabel().equals(labelLocVarVal)) {
      if (AbstractStates.extractStateByType(pElemToCheck, ExplicitState.class).getValueFor(varValName) != varVal) { return false; }
    }
    return true;
  }

  @Override
  public boolean satisfiesProperty(Collection<AbstractState> pCertificate) {
    for (AbstractState elem : pCertificate) {
      if (!satisfiesProperty(elem)) {
        return false;
      }
    }
    return true;
  }

}
