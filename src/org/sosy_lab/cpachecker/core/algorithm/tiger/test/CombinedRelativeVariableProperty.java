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

import com.google.common.collect.Lists;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;


public class CombinedRelativeVariableProperty extends CombinedVariableProperty {

  private String compareVariable;

  public CombinedRelativeVariableProperty(String compareVariable, List<String> pCombinedVariables,
      Combinators pCombinator,
      Comparators pComp, GoalPropertyType pInOrOut) {
    super(pCombinedVariables, pCombinator, pComp, pInOrOut);
    this.compareVariable = compareVariable;
  }

  @Override
  public boolean checkProperty(Map<String, BigInteger> pListToCheck, GoalPropertyType pInOrOut) {
    if (this.inOrOut != pInOrOut) { return true; }
    BigInteger combinedValue = combineVariables(pListToCheck);
    BigInteger compareValue = pListToCheck.get(compareVariable);
    return compareValues(combinedValue, compareValue, comp);
  }

  @Override
  public String toString() {
    List<String> tmp = Lists.newLinkedList(combinedVariables);
    String resultString = "";
    String combinedVariableString = tmp.get(0);
    tmp.remove(combinedVariableString);
    for (String s : tmp) {
      switch (combinator) {
        case ADD: {
          combinedVariableString += " + " + s;
          break;
        }
        case SUBSTRACT: {
          combinedVariableString += " - " + s;
          break;
        }
        case MULTIPLY: {
          combinedVariableString += " * " + s;
          break;
        }
        default:
          break;
      }
    }
    resultString += combinedVariableString;

    switch (comp) {
      case EQ:
        return resultString + " == " + compareVariable;
      case LE:
        return resultString + " <= " + compareVariable;
      case LT:
        return resultString + " < " + compareVariable;
      case GE:
        return resultString + " >= " + compareVariable;
      case GT:
        return resultString + " > " + compareVariable;
      default:
        return "incorrect property";
    }
  }
}
