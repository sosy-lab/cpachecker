/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.counterexample;

import java.math.BigDecimal;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

public final class Address {

  private final Number address;

  public Address(Number pAddress) {

    Preconditions.checkNotNull(pAddress);

    Number addressV = pAddress;

    if (addressV instanceof BigDecimal) {
      addressV = ((BigDecimal) addressV).stripTrailingZeros();
    }

    address = addressV;
  }

  public Address addOffset(Number pOffset) {

    BigDecimal addressV = new BigDecimal(address.toString());
    BigDecimal offsetV = new BigDecimal(pOffset.toString());
    return Address.valueOf(addressV.add(offsetV));
  }

  @Override
  public String toString() {
    return address.toString();
  }

  @Nullable
  public static Address valueOf(Object address) {

    if (address instanceof Address) {
      return (Address) address;
    } else if (address instanceof Number) {
      return new Address((Number) address);
    }

    return null;
  }

  public Number getAsNumber() {
    return address;
  }

  @Override
  public int hashCode() {
    return address.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (!(obj instanceof Address)) {
      return false;
    }

    Address other = (Address) obj;

    if (!address.equals(other.address)) {
      return false;
    }

    return true;
  }
}