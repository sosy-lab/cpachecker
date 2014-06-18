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

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;


public class SingleSignInIntervalChecker extends PerElementPropertyChecker {

  private final SingleSignChecker signChecker;
  private final InIntervalChecker intervalChecker;

  public SingleSignInIntervalChecker(final String pLabel, final String pVarNameSign, final String pVarNameInterval,
      final String pSignVal, final String pIntervalMode, final String pIntervalBound) {
    signChecker = new SingleSignChecker(pVarNameSign, pSignVal, pLabel);
    intervalChecker = new InIntervalChecker(pVarNameInterval, pLabel, pIntervalMode, pIntervalBound);
  }

  public SingleSignInIntervalChecker(final String pLabel, final String pVarNameSign, final String pVarNameInterval,
      final String pSignVal, final String pIntervalMode, final String pMinBound, final String pMaxBound) {
    signChecker = new SingleSignChecker(pVarNameSign, pSignVal, pLabel);
    intervalChecker = new InIntervalChecker(pVarNameInterval, pLabel, pIntervalMode, pMinBound, pMaxBound);
  }


  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck) throws UnsupportedOperationException {
    return signChecker.satisfiesProperty(pElemToCheck) && intervalChecker.satisfiesProperty(pElemToCheck);
  }
}
