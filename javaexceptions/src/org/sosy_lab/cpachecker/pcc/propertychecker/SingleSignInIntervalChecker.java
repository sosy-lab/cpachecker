// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.propertychecker;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class SingleSignInIntervalChecker extends PerElementPropertyChecker {

  private final SingleSignChecker signChecker;
  private final InIntervalChecker intervalChecker;

  public SingleSignInIntervalChecker(
      final String pLabel,
      final String pVarName,
      final String pSignVal,
      final String pIntervalMode,
      final String pIntervalBound) {
    signChecker = new SingleSignChecker(pVarName, pSignVal, pLabel);
    intervalChecker = new InIntervalChecker(pVarName, pLabel, pIntervalMode, pIntervalBound);
  }

  public SingleSignInIntervalChecker(
      final String pLabel,
      final String pVarName,
      final String pSignVal,
      final String pIntervalMode,
      final String pMinBound,
      final String pMaxBound) {
    signChecker = new SingleSignChecker(pVarName, pSignVal, pLabel);
    intervalChecker = new InIntervalChecker(pVarName, pLabel, pIntervalMode, pMinBound, pMaxBound);
  }

  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck)
      throws UnsupportedOperationException {
    return signChecker.satisfiesProperty(pElemToCheck)
        && intervalChecker.satisfiesProperty(pElemToCheck);
  }
}
