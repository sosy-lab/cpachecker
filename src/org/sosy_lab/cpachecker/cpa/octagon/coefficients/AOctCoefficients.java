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
package org.sosy_lab.cpachecker.cpa.octagon.coefficients;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cpa.octagon.OctState;



public abstract class AOctCoefficients implements IOctCoefficients {

  protected BigInteger[] coefficients;
  protected int size;
  protected OctState oct;

  protected AOctCoefficients(int size, OctState oct) {
    this.size = size;
    this.oct = oct;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return size;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result;

  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof AOctCoefficients)) {
      return false;
    }

    return true;
  }
}