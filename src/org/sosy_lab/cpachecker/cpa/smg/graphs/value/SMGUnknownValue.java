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
public final class SMGUnknownValue implements SMGSymbolicValue, SMGExplicitValue, SMGAddressValue {

  private static final SMGUnknownValue instance = new SMGUnknownValue();

  @Override
  public String toString() {
    return "UNKNOWN";
  }

  public static SMGUnknownValue getInstance() {
    return instance;
  }

  @Override
  public boolean isUnknown() {
    return true;
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
    return instance;
  }

  @Override
  public SMGExplicitValue xor(SMGExplicitValue pRVal) {
    return instance;
  }

  @Override
  public SMGExplicitValue or(SMGExplicitValue pRVal) {
    return instance;
  }

  @Override
  public SMGExplicitValue and(SMGExplicitValue pRVal) {
    return instance;
  }

  @Override
  public SMGExplicitValue shiftLeft(SMGExplicitValue pRVal) {
    return instance;
  }

  @Override
  public SMGExplicitValue multiply(SMGExplicitValue pRVal) {
    return instance;
  }

  @Override
  public SMGExplicitValue divide(SMGExplicitValue pRVal) {
    return instance;
  }

  @Override
  public SMGExplicitValue subtract(SMGExplicitValue pRVal) {
    return instance;
  }

  @Override
  public SMGExplicitValue add(SMGExplicitValue pRVal) {
    return instance;
  }

  @Override
  public SMGExplicitValue getOffset() {
    return instance;
  }

  @Override
  public SMGObject getObject() {
    return null;
  }
}

