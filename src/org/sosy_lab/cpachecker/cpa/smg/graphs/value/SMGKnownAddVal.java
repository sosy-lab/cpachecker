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

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;

/**
 * A class to represent a value which points to an address. This class is mainly used to store value
 * information.
 */
public final class SMGKnownAddVal extends SMGKnownSymValue implements SMGAddressValue {

  /** The address this value represents. */
  private final SMGKnownAddress address;

  private SMGKnownAddVal(BigInteger pValue, SMGKnownAddress pAddress) {
    super(pValue);
    checkNotNull(pAddress);
    address = pAddress;
  }

  public static SMGKnownAddVal valueOf(
      SMGObject pObject, SMGKnownExpValue pOffset, SMGKnownSymValue pAddress) {
    return new SMGKnownAddVal(pAddress.getValue(), SMGKnownAddress.valueOf(pObject, pOffset));
  }

  @Override
  public SMGKnownAddress getAddress() {
    return address;
  }

  public static SMGKnownAddVal valueOf(BigInteger pValue, SMGKnownAddress pAddress) {
    return new SMGKnownAddVal(pValue, pAddress);
  }

  public static SMGKnownAddVal valueOf(SMGKnownSymValue pValue, SMGKnownAddress pAddress) {
    return new SMGKnownAddVal(pValue.getValue(), pAddress);
  }

  public static SMGKnownAddVal valueOf(int pValue, SMGKnownAddress pAddress) {
    return new SMGKnownAddVal(BigInteger.valueOf(pValue), pAddress);
  }

  public static SMGKnownAddVal valueOf(long pValue, SMGKnownAddress pAddress) {
    return new SMGKnownAddVal(BigInteger.valueOf(pValue), pAddress);
  }

  public static SMGKnownAddVal valueOf(int pValue, SMGObject object, long offset) {
    return new SMGKnownAddVal(BigInteger.valueOf(pValue), SMGKnownAddress.valueOf(object, offset));
  }

  @Override
  public String toString() {
    return "Value: " + super.toString() + " " + address;
  }

  @Override
  public SMGKnownExpValue getOffset() {
    return address.getOffset();
  }

  @Override
  public SMGObject getObject() {
    return address.getObject();
  }
}
