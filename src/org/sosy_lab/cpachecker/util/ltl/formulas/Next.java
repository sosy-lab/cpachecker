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
package org.sosy_lab.cpachecker.util.ltl.formulas;

/** Next. */
public final class Next extends UnaryFormula {

  public Next(LtlFormula f) {
    super(f);
  }

  public static LtlFormula of(LtlFormula operand) {
    return of(operand, 1);
  }

  public static LtlFormula of(LtlFormula operand, int n) {
    if (operand instanceof BooleanConstant) {
      return operand;
    }

    LtlFormula ltlFormula = operand;

    for (int i = 0; i < n; i++) {
      ltlFormula = new Next(ltlFormula);
    }

    return ltlFormula;
  }

  @Override
  public char getSymbol() {
    return 'X';
  }

  @Override
  public LtlFormula not() {
    return new Next(operand.not());
  }
}
