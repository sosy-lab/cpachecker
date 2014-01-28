/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.tiger.testcases;

import java.util.LinkedList;
import java.util.List;

public class ImpreciseInputsTestCase extends TestCase {

  private double[] mExactInputValues;

  public ImpreciseInputsTestCase(int[] pInputs, double[] pValues) {
    super(pInputs, false);
    copy(pValues);
  }

  public ImpreciseInputsTestCase(List<Integer> pInputs, List<Double> pValues) {
    super(pInputs, false);
    copy(pValues);
  }

  public PreciseInputsTestCase toPreciseTestCase() {
    LinkedList<Integer> lApproximatedValues = new LinkedList<>();

    for (Double lDoubleValue : mExactInputValues) {
      Double lTmpValue = Math.abs(lDoubleValue) + 0.5;
      int lApproxValue = (lDoubleValue >= 0)?lTmpValue.intValue():(-lTmpValue.intValue());
      lApproximatedValues.add(lApproxValue);
    }

    return new PreciseInputsTestCase(lApproximatedValues);
  }

  private void copy(double[] pValues) {
    mExactInputValues = new double[pValues.length];
    for (int i = 0; i < pValues.length; i++) {
      mExactInputValues[i] = pValues[i];
    }
  }

  private void copy(List<Double> pValues) {
    mExactInputValues = new double[pValues.size()];
    int lIndex = 0;
    for (Double lValue : pValues) {
      mExactInputValues[lIndex] = lValue;
      lIndex++;
    }
  }

  @Override
  public String toString() {
    StringBuffer lBuffer = new StringBuffer();

    for (int lIndex = 0; lIndex < mExactInputValues.length; lIndex++) {
      if (lIndex > 0) {
        lBuffer.append(",");
      }

      lBuffer.append(mExactInputValues[lIndex]);
    }

    return super.toString() + " was <" + lBuffer.toString() + ">";
  }

  public double[] getExactInputs() {
    return mExactInputValues;
  }

}
