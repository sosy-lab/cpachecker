// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMGValueAndSMGObject {

  private final SMGValue value;
  private final SMGObject object;

  private SMGValueAndSMGObject(SMGValue pValue, SMGObject pObject) {
    Preconditions.checkNotNull(pObject);
    Preconditions.checkNotNull(pValue);
    value = pValue;
    object = pObject;
  }

  public static SMGValueAndSMGObject of(SMGValue pValue, SMGObject pObject) {
    return new SMGValueAndSMGObject(pValue, pObject);
  }

  public SMGValue getValue() {
    return value;
  }

  public SMGObject getObject() {
    return object;
  }
}
