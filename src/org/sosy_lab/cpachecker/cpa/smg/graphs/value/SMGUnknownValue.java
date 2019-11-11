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

