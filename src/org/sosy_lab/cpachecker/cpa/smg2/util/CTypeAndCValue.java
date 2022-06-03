// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.CValue;

public class CTypeAndCValue {
  private final CType cType;
  private final CValue value;

  private CTypeAndCValue(CType pCType, CValue pCValue) {
    cType = pCType;
    value = pCValue;
  }

  public static CTypeAndCValue of(CType pCType, CValue pCValue) {
    return new CTypeAndCValue(pCType, pCValue);
  }

  public static CTypeAndCValue withUnknownValue(CType pCType) {
    return new CTypeAndCValue(pCType, CValue.getUnknownValue());
  }

  public CValue getValue() {
    return value;
  }

  public CType getCType() {
    return cType;
  }
}
