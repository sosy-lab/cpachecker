/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.graphs.value;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;

public class SMGKnownExpValue extends SMGKnownValue implements SMGExplicitValue {

  protected SMGKnownExpValue(BigInteger pValue) {
    super(pValue);
  }

  @Override // override for visibility
  public BigInteger getValue() {
    return super.getValue();
  }

  @Override
  public final int getAsInt() {
    return getValue().intValue();
  }

  @Override
  public final long getAsLong() {
    return getValue().longValue();
  }

  @Override
  public boolean equals(Object pObj) {
    return pObj instanceof SMGKnownExpValue && super.equals(pObj);
  }

  @Override
  public int hashCode() {
    return super.hashCode(); // equals() in this class checks nothing more
  }

  @Override
  public SMGExplicitValue negate() {
    return valueOf(getValue().negate());
  }

  @Override
  public SMGExplicitValue xor(SMGExplicitValue pRVal) {
    if (pRVal.isUnknown()) {
      return SMGUnknownValue.INSTANCE;
    }
    return valueOf(getValue().xor(pRVal.getValue()));
  }

  @Override
  public SMGExplicitValue or(SMGExplicitValue pRVal) {
    if (pRVal.isUnknown()) {
      return SMGUnknownValue.INSTANCE;
    }
    return valueOf(getValue().or(pRVal.getValue()));
  }

  @Override
  public SMGExplicitValue and(SMGExplicitValue pRVal) {
    if (pRVal.isUnknown()) {
      return SMGUnknownValue.INSTANCE;
    }
    return valueOf(getValue().and(pRVal.getValue()));
  }

  @Override
  public SMGExplicitValue shiftLeft(SMGExplicitValue pRVal) {
    if (pRVal.isUnknown()) {
      return SMGUnknownValue.INSTANCE;
    }
    return valueOf(getValue().shiftLeft(pRVal.getValue().intValue()));
  }

  @Override
  public SMGExplicitValue multiply(SMGExplicitValue pRVal) {
    if (pRVal.isUnknown()) {
      return SMGUnknownValue.INSTANCE;
    }
    return valueOf(getValue().multiply(pRVal.getValue()));
  }

  @Override
  public SMGExplicitValue divide(SMGExplicitValue pRVal) {
    if (pRVal.isUnknown()) {
      return SMGUnknownValue.INSTANCE;
    }
    return valueOf(getValue().divide(pRVal.getValue()));
  }

  @Override
  public SMGExplicitValue subtract(SMGExplicitValue pRVal) {
    if (pRVal.isUnknown()) {
      return SMGUnknownValue.INSTANCE;
    }
    return valueOf(getValue().subtract(pRVal.getValue()));
  }

  @Override
  public SMGExplicitValue add(SMGExplicitValue pRVal) {
    if (pRVal.isUnknown()) {
      return SMGUnknownValue.INSTANCE;
    }
    return valueOf(getValue().add(pRVal.getValue()));
  }

  public static SMGKnownExpValue valueOf(int pValue) {
    return new SMGKnownExpValue(BigInteger.valueOf(pValue));
  }

  public static SMGKnownExpValue valueOf(long pValue) {
    return new SMGKnownExpValue(BigInteger.valueOf(pValue));
  }

  public static SMGKnownExpValue valueOf(BigInteger pValue) {

    checkNotNull(pValue);

    if (pValue.equals(BigInteger.ZERO)) {
      return SMGZeroValue.INSTANCE;
    } else {
      return new SMGKnownExpValue(pValue);
    }
  }

  @Override
  public String asDotId() {
    return getValue().toString();
  }
}

