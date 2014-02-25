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
 * This interface represents the Rational-Theory
 */
public interface RationalFormulaManager {
  public RationalFormula makeNumber(long pI);
  public RationalFormula makeNumber(BigInteger pI);
  public RationalFormula makeNumber(String pI);

  public RationalFormula makeVariable(String pVar);

  public FormulaType<RationalFormula> getFormulaType();


  public RationalFormula negate(RationalFormula number);
  public boolean isNegate(RationalFormula number);

  public RationalFormula add(RationalFormula number1, RationalFormula number2);
  public boolean isAdd(RationalFormula number);

  public RationalFormula subtract(RationalFormula number1, RationalFormula number2);
  public boolean isSubtract(RationalFormula number);

  public RationalFormula divide(RationalFormula number1, RationalFormula number2);
  public boolean isDivide(RationalFormula number);

  public RationalFormula modulo(RationalFormula number1, RationalFormula number2);
  public boolean isModulo(RationalFormula number);

  public RationalFormula multiply(RationalFormula number1, RationalFormula number2);
  public boolean isMultiply(RationalFormula number);

  // ----------------- Numeric relations -----------------

  public BooleanFormula equal(RationalFormula number1, RationalFormula number2);
  public boolean isEqual(BooleanFormula number);

  public BooleanFormula greaterThan(RationalFormula number1, RationalFormula number2);
  public boolean isGreaterThan(BooleanFormula number);

  public BooleanFormula greaterOrEquals(RationalFormula number1, RationalFormula number2);
  public boolean isGreaterOrEquals(BooleanFormula number);

  public BooleanFormula lessThan(RationalFormula number1, RationalFormula number2);
  public boolean isLessThan(BooleanFormula number);

  public BooleanFormula lessOrEquals(RationalFormula number1, RationalFormula number2);
  public boolean isLessOrEquals(BooleanFormula number);
}
