/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import java.util.Objects;

import com.google.common.base.Preconditions;

public class IfThenElse<ConstantType> extends AbstractFormula<ConstantType> {

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
      ParameterizedNumeralFormulaVisitor<ConstantType, ParamType, ReturnType> pVisitor, ParamType pParameter) {
    return pVisitor.visit(this, pParameter);
  }

  public static <ConstantType> IfThenElse<ConstantType> of(
      BooleanFormula<ConstantType> pCondition,
      NumeralFormula<ConstantType> pPositiveCase,
      NumeralFormula<ConstantType> pNegativeCase) {
    return new IfThenElse<>(pCondition, pPositiveCase, pNegativeCase);
  }

}
