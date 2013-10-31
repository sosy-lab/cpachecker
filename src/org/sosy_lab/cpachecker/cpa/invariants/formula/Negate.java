/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
 * Instances of this class are numerical negations of other invariants
 *  formulae.
 *
 * @param <ConstantType> the type of the constants used in the formulae.
 */
public class Negate<ConstantType> extends AbstractFormula<ConstantType> implements InvariantsFormula<ConstantType> {

  /**
   * The invariants formula numerically negated by this formula.
   */
  private final InvariantsFormula<ConstantType> negated;

  /**
   * Creates a new numerical negation of the given formula.
   *
   * @param pToNegate the formula to negate.
   */
  private Negate(InvariantsFormula<ConstantType> pToNegate) {
    this.negated = pToNegate;
  }

  /**
   * Gets the invariants formula that is numerically negated by this formula,
   * which is, of course, also the numerical negation of this formula.
   *
   * @return the invariants formula that is numerically negated by this
   * formula.
   */
  public InvariantsFormula<ConstantType> getNegated() {
    return this.negated;
  }

  @Override
  public String toString() {
    return String.format("(-%s)", getNegated());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Negate<?>) {
      return getNegated().equals(((Negate<?>) o).getNegated());
    }
    return false;
  }

  @Override
  protected int hashCodeInternal() {
    return -getNegated().hashCode();
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
   * Gets an invariants formula representing the numerical negation of the
   * given invariants formula.
   *
   * @param pToNegate the invariants formula to negate.
   *
   * @return an invariants formula representing the numerical negation of the
   * given invariants formula.
   */
  static <ConstantType> Negate<ConstantType> of(InvariantsFormula<ConstantType> pToNegate) {
    return new Negate<>(pToNegate);
  }

}
