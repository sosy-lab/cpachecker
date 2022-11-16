// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util.value;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;

public class CTypeAndValue {

  private final Value value;
  private final CType type;

  private CTypeAndValue(CType pType, Value pValue) {
    value = pValue;
    type = pType;
  }

  public static CTypeAndValue of(CType pType, Value pValue) {
    return new CTypeAndValue(pType, pValue);
  }

  /** Returns the tuple with the entered type and a new unknown value. */
  public static CTypeAndValue ofUnknownValue(CType pType) {
    return of(pType, UnknownValue.getInstance());
  }

  public Value getValue() {
    return value;
  }

  public CType getCType() {
    return type;
  }
}
