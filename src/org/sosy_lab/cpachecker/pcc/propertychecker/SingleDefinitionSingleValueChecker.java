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

import java.util.Collection;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PropertyChecker;

/**
 * Checks if a certain variable is defined at most once by the program and
 * checks if a certain variable has a specific value at a specific location marked by a label in the program.
 */
public class SingleDefinitionSingleValueChecker implements PropertyChecker {

  private SingleDefinitionChecker defChecker;
  private SingleValueChecker valChecker;


  public SingleDefinitionSingleValueChecker(String varWithSingleDef, String varWithSingleValue, String varValue,
      String labelForLocationWithSingleValue) {
    defChecker = new SingleDefinitionChecker(varWithSingleDef);
    valChecker = new SingleValueChecker(varWithSingleValue, varValue, labelForLocationWithSingleValue);
  }

  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck) throws UnsupportedOperationException {
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
