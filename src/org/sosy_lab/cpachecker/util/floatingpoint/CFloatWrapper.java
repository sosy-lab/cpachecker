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

public class CFloatWrapper {

  private long exponent;
  private long mantissa;

  public CFloatWrapper() {
    // no-op
  }

  public CFloatWrapper(long pExp, long pMan) {
    this.exponent = pExp;
    this.mantissa = pMan;
  }

  public long getExponent() {
    return exponent;
  }

  public void setExponent(long exponent) {
    this.exponent = exponent;
  }

  public long getMantissa() {
    return mantissa;
  }

  public void setMantissa(long mantissa) {
    this.mantissa = mantissa;
  }

  public CFloatWrapper copy() {
    return new CFloatWrapper(exponent, mantissa);
  }
}
