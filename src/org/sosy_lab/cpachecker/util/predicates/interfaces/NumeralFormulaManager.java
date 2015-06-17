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
package org.sosy_lab.cpachecker.util.predicates.interfaces;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.sosy_lab.common.rationals.Rational;


/**
 * This interface represents the Numeral-Theory
 *
 * @param <ParamFormulaType> formulaType of the parameters
 * @param <ResultFormulaType> formulaType of arithmetic results
 */
public interface NumeralFormulaManager
        <ParamFormulaType extends NumeralFormula,
         ResultFormulaType extends NumeralFormula>  {

  public ResultFormulaType makeNumber(long number);

  public ResultFormulaType makeNumber(BigInteger number);

  /**
   * Create a numeric literal with a given value.
   * Note: if the theory represented by this instance cannot handle rational numbers,
   * the value may get rounded or otherwise represented imprecisely.
   */
  public ResultFormulaType makeNumber(double number);

  /**
   * Create a numeric literal with a given value.
   * Note: if the theory represented by this instance cannot handle rational numbers,
   * the value may get rounded or otherwise represented imprecisely.
   */
  public ResultFormulaType makeNumber(BigDecimal number);

  public ResultFormulaType makeNumber(String pI);

  public ResultFormulaType makeNumber(Rational pRational);

  public ResultFormulaType makeVariable(String pVar);

  public FormulaType<ResultFormulaType> getFormulaType();

  // ----------------- Arithmetic relations, return type NumeralFormula -----------------

  public ResultFormulaType negate(ParamFormulaType number);

  public ResultFormulaType add(ParamFormulaType number1, ParamFormulaType number2);
  public ResultFormulaType sum(List<ParamFormulaType> operands);

  public ResultFormulaType subtract(ParamFormulaType number1, ParamFormulaType number2);

  public ResultFormulaType divide(ParamFormulaType number1, ParamFormulaType number2);

  public ResultFormulaType modulo(ParamFormulaType number1, ParamFormulaType number2);

  /**
   * Create a term stating that (n1 == n2) when using modulo arithmetic regarding mod).
   * This is an optional operation,
   * and instead may return `true`.
   */
  public BooleanFormula modularCongruence(ParamFormulaType number1, ParamFormulaType number2, long mod);

  public ResultFormulaType multiply(ParamFormulaType number1, ParamFormulaType number2);

  // ----------------- Numeric relations, return type BooleanFormula -----------------

  public BooleanFormula equal(ParamFormulaType number1, ParamFormulaType number2);

  public BooleanFormula greaterThan(ParamFormulaType number1, ParamFormulaType number2);

  public BooleanFormula greaterOrEquals(ParamFormulaType number1, ParamFormulaType number2);

  public BooleanFormula lessThan(ParamFormulaType number1, ParamFormulaType number2);

  public BooleanFormula lessOrEquals(ParamFormulaType number1, ParamFormulaType number2);
}
