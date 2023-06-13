// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.value;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;

/** Class representing values which can't be resolved. */
public final class SMGUnknownValue implements SMGExplicitValue, SMGAddressValue {

  public static final SMGUnknownValue INSTANCE = new SMGUnknownValue();

  private SMGUnknownValue() {}

  @Override
  public String toString() {
    return "UNKNOWN";
  }

  @Override
  public SMGAddress getAddress() {
    return SMGAddress.UNKNOWN;
  }

  @Override
  public BigInteger getValue() {
    throw new IllegalStateException("Can't get Value of an Unknown Value.");
  }

  @Override
  public int getAsInt() {
    throw new IllegalStateException("Can't get Value of an Unknown Value.");
  }

  @Override
  public long getAsLong() {
    throw new IllegalStateException("Can't get Value of an Unknown Value.");
  }

  @Override
  public SMGExplicitValue negate() {
    return INSTANCE;
  }

  @Override
  public SMGExplicitValue xor(SMGExplicitValue pRVal) {
    return INSTANCE;
  }

  @Override
  public SMGExplicitValue or(SMGExplicitValue pRVal) {
    return INSTANCE;
  }

  @Override
  public SMGExplicitValue and(SMGExplicitValue pRVal) {
    return INSTANCE;
  }

  @Override
  public SMGExplicitValue shiftLeft(SMGExplicitValue pRVal) {
    return INSTANCE;
  }

  @Override
  public SMGExplicitValue multiply(SMGExplicitValue pRVal) {
    return INSTANCE;
  }

  @Override
  public SMGExplicitValue divide(SMGExplicitValue pRVal) {
    return INSTANCE;
  }

  @Override
  public SMGExplicitValue subtract(SMGExplicitValue pRVal) {
    return INSTANCE;
  }

  @Override
  public SMGExplicitValue add(SMGExplicitValue pRVal) {
    return INSTANCE;
  }

  @Override
  public SMGExplicitValue getOffset() {
    return INSTANCE;
  }

  @Override
  public SMGObject getObject() {
    return null;
  }

  @Override
  public String asDotId() {
    return "UNKNOWN";
  }

  @Override
  public BigInteger getId() {
    throw new IllegalStateException("Can't get Value of an Unknown Value.");
  }
}
