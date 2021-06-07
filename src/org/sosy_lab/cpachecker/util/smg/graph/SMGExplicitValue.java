// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.graph;

import java.math.BigInteger;

public class SMGExplicitValue implements SMGValue {

  private static final SMGValue NULL_VALUE = of(BigInteger.ZERO, 0);

  private final BigInteger value;
  private final int nestingLevel;

  private SMGExplicitValue(BigInteger pValue, int pNestingLevel) {
    value = pValue;
    nestingLevel = pNestingLevel;
  }

  public static SMGValue nullInstance() {
    return NULL_VALUE;
  }

  public static SMGExplicitValue of(BigInteger pValue, int pNestingLevel) {
    return new SMGExplicitValue(pValue, pNestingLevel);
  }

  @Override
  public int getNestingLevel() {
    return nestingLevel;
  }

  @Override
  public BigInteger getValue() {
    return value;
  }
}
