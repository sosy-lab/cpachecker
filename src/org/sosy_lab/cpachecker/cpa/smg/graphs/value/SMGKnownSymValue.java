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

public class SMGKnownSymValue extends SMGKnownValue implements SMGSymbolicValue {

  public static final SMGKnownSymValue ZERO = new SMGKnownSymValue(BigInteger.ZERO);

  public static final SMGKnownSymValue ONE = new SMGKnownSymValue(BigInteger.ONE);

  public static final SMGKnownSymValue TRUE = new SMGKnownSymValue(BigInteger.valueOf(-1));

  public static final SMGKnownSymValue FALSE = ZERO;

  SMGKnownSymValue(BigInteger pValue) {
    super(pValue);
  }

  public static SMGKnownSymValue valueOf(int pValue) {
    return new SMGKnownSymValue(BigInteger.valueOf(pValue));
  }

  public static SMGKnownSymValue valueOf(long pValue) {
    return new SMGKnownSymValue(BigInteger.valueOf(pValue));
  }

  public static SMGKnownSymValue valueOf(BigInteger pValue) {

    checkNotNull(pValue);

    if (pValue.equals(BigInteger.ZERO)) {
      return ZERO;
    } else if (pValue.equals(BigInteger.ONE)) {
      return ONE;
    } else {
      return new SMGKnownSymValue(pValue);
    }
  }

  @Override
  public final boolean equals(Object pObj) {
    return pObj instanceof SMGKnownSymValue && super.equals(pObj);
  }
}
