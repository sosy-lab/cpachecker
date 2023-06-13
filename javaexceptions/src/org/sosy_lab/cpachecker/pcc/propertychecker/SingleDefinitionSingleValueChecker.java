// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.propertychecker;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PropertyChecker;

/**
 * Checks if a certain variable is defined at most once by the program and checks if a certain
 * variable has a specific value at a specific location marked by a label in the program.
 */
public class SingleDefinitionSingleValueChecker implements PropertyChecker {

  private SingleDefinitionChecker defChecker;
  private SingleValueChecker valChecker;

  public SingleDefinitionSingleValueChecker(
      String varWithSingleDef,
      String varWithSingleValue,
      String varValue,
      String labelForLocationWithSingleValue) {
    defChecker = new SingleDefinitionChecker(varWithSingleDef);
    valChecker =
        new SingleValueChecker(varWithSingleValue, varValue, labelForLocationWithSingleValue);
  }

  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean satisfiesProperty(Collection<AbstractState> pCertificate) {
    boolean result = defChecker.satisfiesProperty(pCertificate);
    if (result) {
      result = valChecker.satisfiesProperty(pCertificate);
    }
    return result;
  }
}
