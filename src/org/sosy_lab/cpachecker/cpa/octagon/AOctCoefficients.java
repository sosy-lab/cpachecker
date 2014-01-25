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
package org.sosy_lab.cpachecker.cpa.octagon;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.util.octagon.NumArray;
import org.sosy_lab.cpachecker.util.octagon.OctagonManager;



public abstract class AOctCoefficients implements IOctCoefficients {

  protected BigInteger[] coefficients;
  protected boolean[] isInfite;
  protected int size;

  protected AOctCoefficients(int size) {
    this.size = size;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NumArray getNumArray() {
    NumArray arr = OctagonManager.init_num_t(coefficients.length);
    for (int i = 0; i < coefficients.length; i++) {
      if (isInfite[i]) {
        OctagonManager.num_set_inf(arr, i);
      } else {
        OctagonManager.num_set_int(arr, i, coefficients[i].intValue());
      }
    }
    return arr;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return size;
  }

}