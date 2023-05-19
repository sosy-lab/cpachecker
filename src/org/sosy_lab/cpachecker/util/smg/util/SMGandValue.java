// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/*
 * Sometimes we need to return a SMG and a SMGValue.
 */
public class SMGandValue {

  private final SMG smg;
  private final SMGValue value;

  public SMGandValue(SMG smg, SMGValue value) {
    this.smg = smg;
    this.value = value;
  }

  public SMG getSMG() {
    return smg;
  }

  public SMGValue getValue() {
    return value;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (!(other instanceof SMGandValue)) {
      return false;
    }
    SMGandValue otherSMGaV = (SMGandValue) other;
    return smg.equals(otherSMGaV.getSMG()) && value.equals(otherSMGaV.getValue());
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
