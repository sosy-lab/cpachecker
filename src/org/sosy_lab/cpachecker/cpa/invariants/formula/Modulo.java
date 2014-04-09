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
package org.sosy_lab.cpachecker.cpa.invariants.formula;

/**
 * Instances of this class represent modulo invariants formulae over other
 * invariants formulae.
 *
 * @param <ConstantType> the type of the constants used in the formulae.
 */
public class Modulo<ConstantType> extends AbstractFormula<ConstantType> implements InvariantsFormula<ConstantType> {

  /**
   * The numerator of the fraction.
   */
  private final InvariantsFormula<ConstantType> numerator;

  /**
   * The denominator of the fraction.
   */
  private final InvariantsFormula<ConstantType> denominator;

  /**
   * Creates a new modulo formula over the given numerator and denominator
   * formulae.
   *
   * @param pNumerator the numerator of the fraction.
   * @param pDenominator the denominator of the fraction.
   */
  private Modulo(InvariantsFormula<ConstantType> pNumerator,
      InvariantsFormula<ConstantType> pDenominator) {
    this.numerator = pNumerator;
    this.denominator = pDenominator;
  }

  /**
   * Gets the numerator of the fraction.
   *
   * @return the numerator of the fraction.
   */
  public InvariantsFormula<ConstantType> getNumerator() {
    return this.numerator;
  }

  /**
   * Gets the denominator of the fraction.
   *
   * @return the denominator of the fraction.
   */
  public InvariantsFormula<ConstantType> getDenominator() {
    return this.denominator;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Modulo) {
      Modulo<?> other = (Modulo<?>) o;
      return getNumerator().equals(other.getNumerator()) && getDenominator().equals(other.getDenominator());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getNumerator().hashCode() % getDenominator().hashCode();
  }

  @Override
  public String toString() {
    return String.format("(%s %% %s)", getNumerator(), getDenominator());
  }

  @Override
  public <ReturnType> ReturnType accept(InvariantsFormulaVisitor<ConstantType, ReturnType> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public <ReturnType, ParamType> ReturnType accept(
      ParameterizedInvariantsFormulaVisitor<ConstantType, ParamType, ReturnType> pVisitor, ParamType pParameter) {
    return pVisitor.visit(this, pParameter);
  }

  /**
   * Gets an invariants formula representing the modulo operation over the
   * given operands.
   *
   * @param pNumerator the numerator of the fraction.
   * @param pDenominator the denominator of the fraction.
   * @return an invariants formula representing the modulo operation over the
   * given operands.
   */
  static <ConstantType> Modulo<ConstantType> of(InvariantsFormula<ConstantType> pNumerator, InvariantsFormula<ConstantType> pDenominator) {
    return new Modulo<>(pNumerator, pDenominator);
  }

}
