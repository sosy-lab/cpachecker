// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

import com.google.common.base.Preconditions;
import java.util.Objects;

class IfThenElse<ConstantType> extends AbstractFormula<ConstantType> {

  private final BooleanFormula<ConstantType> condition;

  private final NumeralFormula<ConstantType> positiveCase;

  private final NumeralFormula<ConstantType> negativeCase;

  private IfThenElse(
      BooleanFormula<ConstantType> pCondition,
      NumeralFormula<ConstantType> pPositiveCase,
      NumeralFormula<ConstantType> pNegativeCase) {
    super(pPositiveCase.getTypeInfo());
    Preconditions.checkNotNull(pCondition);
    Preconditions.checkNotNull(pNegativeCase);
    Preconditions.checkArgument(pPositiveCase.getTypeInfo().equals(pNegativeCase.getTypeInfo()));
    this.condition = pCondition;
    this.positiveCase = pPositiveCase;
    this.negativeCase = pNegativeCase;
  }

  public BooleanFormula<ConstantType> getCondition() {
    return this.condition;
  }

  public NumeralFormula<ConstantType> getPositiveCase() {
    return this.positiveCase;
  }

  public NumeralFormula<ConstantType> getNegativeCase() {
    return this.negativeCase;
  }

  @Override
  public String toString() {
    return String.format("(%s ? %s : %s)", getCondition(), getPositiveCase(), getNegativeCase());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getCondition(), getPositiveCase(), getNegativeCase());
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof IfThenElse) {
      IfThenElse<?> other = (IfThenElse<?>) pOther;
      return getCondition().equals(other.getCondition())
          && getPositiveCase().equals(other.getPositiveCase())
          && getNegativeCase().equals(other.getNegativeCase());
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

  public static <ConstantType> IfThenElse<ConstantType> of(
      BooleanFormula<ConstantType> pCondition,
      NumeralFormula<ConstantType> pPositiveCase,
      NumeralFormula<ConstantType> pNegativeCase) {
    return new IfThenElse<>(pCondition, pPositiveCase, pNegativeCase);
  }
}
