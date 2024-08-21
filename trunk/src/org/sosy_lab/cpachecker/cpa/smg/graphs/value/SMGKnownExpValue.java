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
    return valueOf(BigInteger.valueOf(pValue));
  }

  public static SMGKnownExpValue valueOf(long pValue) {
    return valueOf(BigInteger.valueOf(pValue));
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

  @Override
  public String toString() {
    return "Exp_" + super.toString();
  }
}
