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
import java.util.Map;


public class RelativeVariableProperty extends VariableProperty {

  private String variable1;

  private String variable2;

  public RelativeVariableProperty(String variable1, String variable2, Comparators pComp,
      GoalPropertyType pInOrOut) {
    super(pComp, pInOrOut);
    this.variable1 = variable1;
    this.variable2 = variable2;
  }

  @Override
  public boolean checkProperty(Map<String, BigInteger> pListToCheck, GoalPropertyType pInOrOut) {
    if (inOrOut != pInOrOut) { return true; }
    BigInteger value1 = pListToCheck.get(variable1);
    BigInteger value2 = pListToCheck.get(variable2);
    return compareValues(value1, value2, comp);
  }

  @Override
  public String toString() {
    switch (comp) {
      case EQ:
        return variable1 + " == " + variable2;
      case LE:
        return variable1 + " <= " + variable2;
      case LT:
        return variable1 + " < " + variable2;
      case GE:
        return variable1 + " >= " + variable2;
      case GT:
        return variable1 + " > " + variable2;
      default:
        return "incorrect property";
    }
  }

}
