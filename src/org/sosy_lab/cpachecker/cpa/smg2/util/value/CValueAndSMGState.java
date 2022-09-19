// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util.value;

import org.sosy_lab.cpachecker.cpa.smg2.SMGState;

public class CValueAndSMGState {

  private final CValue value;
  private final SMGState state;

  private CValueAndSMGState(CValue pValue, SMGState pState) {
    value = pValue;
    state = pState;
  }

  public static CValueAndSMGState of(CValue pValue, SMGState pState) {
    return new CValueAndSMGState(pValue, pState);
  }

  public static CValueAndSMGState ofUnknown(SMGState pInitialSmgState) {
    return of(CValue.getUnknownValue(), pInitialSmgState);
  }

  public CValue getValue() {
    return value;
  }

  public SMGState getState() {
    return state;
  }
}
