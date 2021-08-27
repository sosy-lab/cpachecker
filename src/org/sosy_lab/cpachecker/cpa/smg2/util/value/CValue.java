// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util.value;

import java.math.BigInteger;

public class CValue implements Comparable<CValue> {

  private final BigInteger value;

  private CValue(BigInteger pValue) {
    value = pValue;
  }

  public static CValue zero() {
    return valueOf(BigInteger.ZERO);
  }

  public static CValue valueOf(BigInteger val) {
    return new CValue(val);
  }

  @Override
  public int compareTo(CValue other) {
    return value.compareTo(other.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    return pObj instanceof CValue && value.equals(((CValue) pObj).value);
  }

}
