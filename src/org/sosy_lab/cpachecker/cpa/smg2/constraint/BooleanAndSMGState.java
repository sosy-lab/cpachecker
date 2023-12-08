// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.constraint;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;

public class BooleanAndSMGState {

  private final boolean bool;

  private final SMGState state;

  private BooleanAndSMGState(final boolean pBool, final SMGState pState) {
    Preconditions.checkNotNull(pState);
    bool = pBool;
    state = pState;
  }

  public static BooleanAndSMGState of(final boolean pBool, final SMGState pState) {
    return new BooleanAndSMGState(pBool, pState);
  }

  public boolean getBoolean() {
    return bool;
  }

  public SMGState getState() {
    return state;
  }
}
