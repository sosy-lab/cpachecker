// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMGValueAndSMGState {

  private final SMGState state;

  private final SMGValue value;

  public SMGValueAndSMGState(SMGState pState, SMGValue pValue) {
    state = pState;
    value = pValue;
  }

  public static SMGValueAndSMGState of(SMGState pState, SMGValue pValue) {
    return new SMGValueAndSMGState(pState, pValue);
  }

  public SMGState getSMGState() {
    return state;
  }

  public SMGValue getSMGValue() {
    return value;
  }
}
