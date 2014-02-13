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

import java.math.BigInteger;

/**
 * This interface represents the Numeral-Theory
 */
public interface NumeralFormulaManager {
  public NumericFormula makeNumber(long pI);
  public NumericFormula makeNumber(BigInteger pI);
  public NumericFormula makeNumber(String pI);

  public NumericFormula makeVariable(String pVar);

  public FormulaType<NumericFormula> getFormulaType();

  // ----------------- Arithmetic relations, return type NumericFormula -----------------

  public NumericFormula negate(NumericFormula number);
  public boolean isNegate(NumericFormula number);

  public NumericFormula add(NumericFormula number1, NumericFormula number2);
  public boolean isAdd(NumericFormula number);

  public NumericFormula subtract(NumericFormula number1, NumericFormula number2);
  public boolean isSubtract(NumericFormula number);

  public NumericFormula divide(NumericFormula number1, NumericFormula number2);
  public boolean isDivide(NumericFormula number);

  public NumericFormula modulo(NumericFormula number1, NumericFormula number2);
  public boolean isModulo(NumericFormula number);

  public NumericFormula multiply(NumericFormula number1, NumericFormula number2);
  public boolean isMultiply(NumericFormula number);

  // ----------------- Numeric relations, return type BooleanFormula -----------------

  public BooleanFormula equal(NumericFormula number1, NumericFormula number2);
  public boolean isEqual(BooleanFormula number);

  public BooleanFormula greaterThan(NumericFormula number1, NumericFormula number2);
  public boolean isGreaterThan(BooleanFormula number);

  public BooleanFormula greaterOrEquals(NumericFormula number1, NumericFormula number2);
  public boolean isGreaterOrEquals(BooleanFormula number);

  public BooleanFormula lessThan(NumericFormula number1, NumericFormula number2);
  public boolean isLessThan(BooleanFormula number);

  public BooleanFormula lessOrEquals(NumericFormula number1, NumericFormula number2);
  public boolean isLessOrEquals(BooleanFormula number);
}
