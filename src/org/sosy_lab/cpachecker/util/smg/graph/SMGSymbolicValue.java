// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.graph;

import java.math.BigInteger;
import org.sosy_lab.common.UniqueIdGenerator;

public class SMGSymbolicValue implements SMGValue {

  private static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();

  private final BigInteger value;
  private final int nestingLevel;

  private SMGSymbolicValue(BigInteger pValue, int pNestingLevel) {
    value = pValue;
    nestingLevel = pNestingLevel;
  }

  public static SMGSymbolicValue of(int pNestingLevel) {
    return new SMGSymbolicValue(BigInteger.valueOf(idGenerator.getFreshId() + 1), pNestingLevel);
  }

  public static SMGSymbolicValue of(BigInteger pValue, int pNestingLevel) {
    return new SMGSymbolicValue(pValue, pNestingLevel);
  }

  @Override
  public int getNestingLevel() {
    return nestingLevel;
  }

  @Override
  public BigInteger getValue() {
    return value;
  }

  @Override
  public int compareTo(SMGValue pArg) {
    return getValue().compareTo(pArg.getValue());
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof SMGSymbolicValue)) {
      return false;
    }
    SMGSymbolicValue otherValue = (SMGSymbolicValue) other;
    return value.equals(otherValue.value);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
