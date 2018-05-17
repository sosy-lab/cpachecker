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
package org.sosy_lab.cpachecker.util.floatingpoint;

public interface CFloat {

  CFloat add(CFloat pSummand);

  CFloat add(CFloat... pSummands);

  CFloat multiply(CFloat pFactor);

  CFloat multiply(CFloat... pFactors);

  CFloat subtract(CFloat pSubtrahend);

  CFloat divideBy(CFloat pDivisor);

  CFloat powTo(CFloat pExponent);

  CFloat powToIntegral(int pExponent);

  CFloat sqrt();

  CFloat round();

  CFloat trunc();

  CFloat ceil();

  CFloat floor();

  CFloat abs();

  boolean isZero();

  boolean isOne();

  default boolean isNan() {
    return false;
  }

  default boolean isInfinity() {
    return false;
  }

  boolean isNegative();

  CFloat copySignFrom(CFloat source);

  CFloat castTo(int toType);

  Number castToOther(int toType);

  CFloatWrapper copyWrapper();

  int getType();
}
