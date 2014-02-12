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

import org.sosy_lab.cpachecker.cpa.octagon.OctState;
import org.sosy_lab.cpachecker.util.octagon.NumArray;

public final class OctEmptyCoefficients extends AOctCoefficients {

  public static final OctEmptyCoefficients INSTANCE = new OctEmptyCoefficients();

  private OctEmptyCoefficients() {
    super(0, null);
  }

  @Override
  public OctEmptyCoefficients expandToSize(int pSize, OctState oct) {
    return this;
  }

  @Override
  public OctEmptyCoefficients add(IOctCoefficients pOther) {
    return INSTANCE;
  }

  @Override
  public OctEmptyCoefficients sub(IOctCoefficients pOther) {
    return INSTANCE;
  }

  @Override
  public boolean hasOnlyConstantValue() {
    return false;
  }

  @Override
  public NumArray getNumArray() {
    return null;
  }
}
