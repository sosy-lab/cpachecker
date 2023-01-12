// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import com.google.common.base.Preconditions;
import java.math.BigInteger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

public class ValueAndValueSize {

  private final Value value;

  // null for new interpolants
  private final @Nullable BigInteger sizeOfValueInBits;

  private ValueAndValueSize(Value pValue, BigInteger pSizeOfValueInBits) {
    value = pValue;
    sizeOfValueInBits = pSizeOfValueInBits;
  }

  public static ValueAndValueSize of(Value pValue, BigInteger pSizeOfValueInBits) {
    Preconditions.checkNotNull(pValue);
    // Size can be null for new interpolants
    return new ValueAndValueSize(pValue, pSizeOfValueInBits);
  }

  public Value getValue() {
    return value;
  }

  public @Nullable BigInteger getSizeInBits() {
    return sizeOfValueInBits;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof ValueAndValueSize)) {
      return false;
    }
    ValueAndValueSize otherVAS = (ValueAndValueSize) other;
    if (sizeOfValueInBits == null) {
      return value.equals(otherVAS.value);
    }
    return value.equals(otherVAS.value) && sizeOfValueInBits.equals(otherVAS.sizeOfValueInBits);
  }

  @Override
  public int hashCode() {
    if (sizeOfValueInBits == null) {
      return value.hashCode();
    }
    return value.hashCode() + sizeOfValueInBits.hashCode();
  }

  @Override
  public String toString() {
    return value.toString() + " (" + sizeOfValueInBits + ")";
  }
}
