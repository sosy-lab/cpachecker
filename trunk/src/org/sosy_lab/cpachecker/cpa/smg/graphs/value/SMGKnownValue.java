// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.value;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;

public abstract class SMGKnownValue {

  /**
   * A symbolic value representing an explicit value. Depending on the sub-class, this value either
   * represents a direct explicit numeral value or a unique symbolic identifier.
   */
  private final BigInteger value;

  SMGKnownValue(BigInteger pValue) {
    checkNotNull(pValue);
    value = pValue;
  }

  @Override
  public boolean equals(Object pObj) {
    return pObj instanceof SMGKnownValue && value.equals(((SMGKnownValue) pObj).value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  protected BigInteger getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
