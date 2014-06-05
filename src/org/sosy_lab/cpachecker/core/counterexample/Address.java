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

public class Address {

  private final Number addressAsNumber;

  private final Object symbolicAddress;


  private Address(Object pAddress) {
    symbolicAddress = pAddress;

    if (pAddress instanceof Number) {
      addressAsNumber = (Number) pAddress;
    } else {
      addressAsNumber = null;
    }
  }

  public boolean comparesToUFArgument(Object pArgument) {

    if (pArgument instanceof BigDecimal && this.isNumericalType()) {
      return ((BigDecimal) pArgument).compareTo(new BigDecimal(addressAsNumber.toString())) == 0;
    }

    if (symbolicAddress instanceof BigDecimal && pArgument instanceof Number) {
      return ((BigDecimal) symbolicAddress).compareTo(new BigDecimal(((Number) pArgument).toString())) == 0;
    }

    return pArgument.equals(symbolicAddress);
  }

  public boolean isNumericalType() {
    return addressAsNumber != null;
  }

  public Address addOffset(Number pOffset) {

    if (addressAsNumber == null) {
      throw new IllegalStateException(
        "Can't add offsets to a non numerical type of address");
    }

    BigDecimal address = new BigDecimal(pOffset.toString());
    BigDecimal offset = new BigDecimal(addressAsNumber.toString());

    return Address.valueOf(address.add(offset));
  }

  @Override
  public String toString() {
    return "Address = [" + symbolicAddress + "]";
  }

  public static Address valueOf(Object address) {
    return new Address(address);
  }

  public Object getSymbolicValue() {
    return symbolicAddress;
  }

  public Number getAsNumber() {
    return addressAsNumber;
  }
}