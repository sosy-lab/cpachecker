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
package org.sosy_lab.cpachecker.fshell.testcases;

import java.util.List;

public class ImpreciseInputsTestCase extends TestCase {

  private double[] mInputs;

  public ImpreciseInputsTestCase(int[] pInputs, double[] pValues) {
    super(pInputs, false);
    copy(pValues);
  }

  public ImpreciseInputsTestCase(List<Integer> pInputs, List<Double> pValues) {
    super(pInputs, false);
    copy(pValues);
  }

  private void copy(double[] pValues) {
    mInputs = new double[pValues.length];
    for (int i = 0; i < pValues.length; i++) {
      mInputs[i] = pValues[i];
    }
  }

  private void copy(List<Double> pValues) {
    mInputs = new double[pValues.size()];
    int lIndex = 0;
    for (Double lValue : pValues) {
      mInputs[lIndex] = lValue;
      lIndex++;
    }
  }

  @Override
  public String toString() {
    StringBuffer lBuffer = new StringBuffer();
    
    for (int lIndex = 0; lIndex < mInputs.length; lIndex++) {
      if (lIndex > 0) {
        lBuffer.append(",");
      }

      lBuffer.append(mInputs[lIndex]);
    }

    return super.toString() + " was <" + lBuffer.toString() + ">";
  }

}
