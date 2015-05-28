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

import org.sosy_lab.common.rationals.Rational;


/**
 * This interface represents the floating-foint theory
 */
public interface FloatingPointFormulaManager {
  public FloatingPointFormula makeNumber(double n, FormulaType.FloatingPointType type);
  public FloatingPointFormula makeNumber(BigDecimal n, FormulaType.FloatingPointType type);
  public FloatingPointFormula makeNumber(String n, FormulaType.FloatingPointType type);
  public FloatingPointFormula makeNumber(Rational n, FormulaType.FloatingPointType type);

  public FloatingPointFormula makeVariable(String pVar, FormulaType.FloatingPointType type);

  public FloatingPointFormula makePlusInfinity(FormulaType.FloatingPointType type);
  public FloatingPointFormula makeMinusInfinity(FormulaType.FloatingPointType type);
  public FloatingPointFormula makeNaN(FormulaType.FloatingPointType type);

  public <T extends Formula> T castTo(FloatingPointFormula number, FormulaType<T> targetType);
  public FloatingPointFormula castFrom(Formula number, boolean signed, FormulaType.FloatingPointType targetType);

  // ----------------- Arithmetic relations, return type NumeralFormula -----------------

  public FloatingPointFormula negate(FloatingPointFormula number);

  public FloatingPointFormula add(FloatingPointFormula number1, FloatingPointFormula number2);

  public FloatingPointFormula subtract(FloatingPointFormula number1, FloatingPointFormula number2);

  public FloatingPointFormula divide(FloatingPointFormula number1, FloatingPointFormula number2);

  public FloatingPointFormula multiply(FloatingPointFormula number1, FloatingPointFormula number2);

  // ----------------- Numeric relations, return type BooleanFormula -----------------

  /**
   * Create a term for assigning one floating-point term to another.
   * This means both terms are considered equal afterwards.
   * This method is the same as the method <code>equal</code> for other theories.
   */
  public BooleanFormula assignment(FloatingPointFormula number1, FloatingPointFormula number2);

  /**
   * Create a term for comparing the equality of two floating-point terms,
   * according to standard floating-point semantics (i.e., NaN != NaN).
   * Be careful to not use this method when you really need
   * {@link #assignment(FloatingPointFormula, FloatingPointFormula)}.
   */
  public BooleanFormula equalWithFPSemantics(FloatingPointFormula number1, FloatingPointFormula number2);

  public BooleanFormula greaterThan(FloatingPointFormula number1, FloatingPointFormula number2);

  public BooleanFormula greaterOrEquals(FloatingPointFormula number1, FloatingPointFormula number2);

  public BooleanFormula lessThan(FloatingPointFormula number1, FloatingPointFormula number2);

  public BooleanFormula lessOrEquals(FloatingPointFormula number1, FloatingPointFormula number2);

  public BooleanFormula isNaN(FloatingPointFormula number);
  public BooleanFormula isInfinity(FloatingPointFormula number);
  public BooleanFormula isZero(FloatingPointFormula number);
  public BooleanFormula isSubnormal(FloatingPointFormula number);
}
