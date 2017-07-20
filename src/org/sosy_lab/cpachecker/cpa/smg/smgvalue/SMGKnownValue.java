/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg.smgvalue;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;

public abstract class SMGKnownValue {

  /** A symbolic value representing an explicit value. */
  private final BigInteger value;

  SMGKnownValue(BigInteger pValue) {
    checkNotNull(pValue);
    value = pValue;
  }

  private SMGKnownValue(long pValue) {
    value = BigInteger.valueOf(pValue);
  }

  private SMGKnownValue(int pValue) {
    value = BigInteger.valueOf(pValue);
  }

  @Override
  public boolean equals(Object pObj) {

    if (this == pObj) {
      return true;
    }

    if (!(pObj instanceof SMGKnownValue)) {
      return false;
    }

    SMGKnownValue otherValue = (SMGKnownValue) pObj;

    return value.equals(otherValue.value);
  }

  @Override
  public int hashCode() {

    int result = 5;

    int c = value.hashCode();

    return result * 31 + c;
  }

  public final BigInteger getValue() {
    return value;
  }

  public final int getAsInt() {
    return value.intValue();
  }

  public final long getAsLong() {
    return value.longValue();
  }

  @Override
  public String toString() {
    return value.toString();
  }

  public boolean isUnknown() {
    return false;
  }
}
