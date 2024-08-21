// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

public class SMGObjectAndSMGState {

  private final SMGObject object;
  private final SMGState state;

  private SMGObjectAndSMGState(SMGObject pObject, SMGState pState) {
    Preconditions.checkNotNull(pObject);
    Preconditions.checkNotNull(pState);
    object = pObject;
    state = pState;
  }

  public static SMGObjectAndSMGState of(SMGObject pObject, SMGState pState) {
    return new SMGObjectAndSMGState(pObject, pState);
  }

  public SMGObject getSMGObject() {
    return object;
  }

  public SMGState getState() {
    return state;
  }
}
