/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.smt;

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.UFManager;

import java.math.BigInteger;

/**
 * Replacing non-linear arithmetics with UF
 * for formulas over integers.
 */
class IntegerNonLinearUFNumeralFormulaManager
  extends NonLinearUFNumeralFormulaManager<IntegerFormula, IntegerFormula>
  implements IntegerFormulaManager  {

  private final IntegerFormulaManager integerFormulaManager;

  IntegerNonLinearUFNumeralFormulaManager(
      FormulaWrappingHandler pWrappingHandler,
      IntegerFormulaManager numeralFormulaManager,
      UFManager pFunctionManager) {
    super(pWrappingHandler, numeralFormulaManager, pFunctionManager);
    integerFormulaManager = numeralFormulaManager;
  }

  @Override
  public BooleanFormula modularCongruence(
      IntegerFormula number1, IntegerFormula number2, long n) {
    return integerFormulaManager.modularCongruence(number1, number2, n);
  }
  @Override
  public BooleanFormula modularCongruence(
      IntegerFormula number1, IntegerFormula number2, BigInteger n) {
    return integerFormulaManager.modularCongruence(number1, number2, n);
  }
}
