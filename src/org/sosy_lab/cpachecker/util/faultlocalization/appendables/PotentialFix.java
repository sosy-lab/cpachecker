// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization.appendables;

public class PotentialFix extends FaultInfo {

  protected PotentialFix(InfoType pType, String pDescription) {
    super(pType);
    description = pDescription;
    score = 0;
  }
}
