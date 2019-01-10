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
package org.sosy_lab.cpachecker.core.algorithm.tiger.test;

import java.math.BigInteger;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCaseVariable;

public abstract class VariableProperty {

  public enum Comparators {
    LE,
    LT,
    EQ,
    GE,
    GT
  }

  public enum GoalPropertyType {
    INPUT,
    OUTPUT,
    INPUTANDOUTPUT
  }

  protected Comparators comp;

  protected GoalPropertyType inOrOut;


  public VariableProperty(Comparators pComp, GoalPropertyType pInOrOut) {
    comp = pComp;
    inOrOut = pInOrOut;
  }

  protected boolean compareValues(BigInteger value1, BigInteger value2, Comparators comp) {

    if (value1 == null || value2 == null) { return false; }

    switch (comp) {
      case EQ:
        return value1.compareTo(value2) == 0;
      case LE:
        return value1.compareTo(value2) <= 0;
      case LT:
        return value1.compareTo(value2) < 0;
      case GE:
        return value1.compareTo(value2) >= 0;
      case GT:
        return value1.compareTo(value2) > 0;
      default:
        return true;
    }
  }

  public abstract boolean
      checkProperty(List<TestCaseVariable> listToCheck,
      GoalPropertyType inOrOut);
}
