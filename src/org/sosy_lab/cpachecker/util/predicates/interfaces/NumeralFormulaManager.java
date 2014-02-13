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
  public NumeralFormula makeNumber(long pI);
  public NumeralFormula makeNumber(BigInteger pI);
  public NumeralFormula makeNumber(String pI);

  public NumeralFormula makeVariable(String pVar);

  public FormulaType<NumeralFormula> getFormulaType();

  // ----------------- Arithmetic relations, return type NumeralFormula -----------------

  public NumeralFormula negate(NumeralFormula number);
  public boolean isNegate(NumeralFormula number);

  public NumeralFormula add(NumeralFormula number1, NumeralFormula number2);
  public boolean isAdd(NumeralFormula number);

  public NumeralFormula subtract(NumeralFormula number1, NumeralFormula number2);
  public boolean isSubtract(NumeralFormula number);

  public NumeralFormula divide(NumeralFormula number1, NumeralFormula number2);
  public boolean isDivide(NumeralFormula number);

  public NumeralFormula modulo(NumeralFormula number1, NumeralFormula number2);
  public boolean isModulo(NumeralFormula number);

  public NumeralFormula multiply(NumeralFormula number1, NumeralFormula number2);
  public boolean isMultiply(NumeralFormula number);

  // ----------------- Numeric relations, return type BooleanFormula -----------------

  public BooleanFormula equal(NumeralFormula number1, NumeralFormula number2);
  public boolean isEqual(BooleanFormula number);

  public BooleanFormula greaterThan(NumeralFormula number1, NumeralFormula number2);
  public boolean isGreaterThan(BooleanFormula number);

  public BooleanFormula greaterOrEquals(NumeralFormula number1, NumeralFormula number2);
  public boolean isGreaterOrEquals(BooleanFormula number);

  public BooleanFormula lessThan(NumeralFormula number1, NumeralFormula number2);
  public boolean isLessThan(BooleanFormula number);

  public BooleanFormula lessOrEquals(NumeralFormula number1, NumeralFormula number2);
  public boolean isLessOrEquals(BooleanFormula number);
}
