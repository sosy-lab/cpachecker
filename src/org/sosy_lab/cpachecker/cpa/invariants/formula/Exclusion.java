// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

public class Exclusion<ConstantType> extends AbstractFormula<ConstantType>
    implements NumeralFormula<ConstantType> {

  private final NumeralFormula<ConstantType> excluded;

  private Exclusion(NumeralFormula<ConstantType> pExcluded) {
    super(pExcluded.getTypeInfo());
    this.excluded = pExcluded;
  }

  public NumeralFormula<ConstantType> getExcluded() {
    return this.excluded;
  }

  @Override
  public String toString() {
    return String.format("\\?(%s)", getExcluded());
  }

  @Override
  public int hashCode() {
    return ~getExcluded().hashCode();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO instanceof Exclusion) {
      return getExcluded().equals(((Exclusion<?>) pO).getExcluded());
    }
    return false;
  }

  @Override
  public <ReturnType> ReturnType accept(NumeralFormulaVisitor<ConstantType, ReturnType> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public <ReturnType, ParamType> ReturnType accept(
      ParameterizedNumeralFormulaVisitor<ConstantType, ParamType, ReturnType> pVisitor,
      ParamType pParameter) {
    return pVisitor.visit(this, pParameter);
  }

  public static <ConstantType> NumeralFormula<ConstantType> of(
      NumeralFormula<ConstantType> pToExclude) {
    return new Exclusion<>(pToExclude);
  }
}
