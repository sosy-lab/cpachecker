// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

import java.util.Objects;
import org.sosy_lab.cpachecker.cpa.invariants.TypeInfo;

class Cast<ConstantType> extends AbstractFormula<ConstantType> {

  private final NumeralFormula<ConstantType> casted;

  private Cast(TypeInfo pInfo, NumeralFormula<ConstantType> pCasted) {
    super(pInfo);
    this.casted = pCasted;
  }

  public NumeralFormula<ConstantType> getCasted() {
    return casted;
  }

  @Override
  public String toString() {
    return String.format("((%s) %s)", getTypeInfo().abbrev(), getCasted());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getTypeInfo(), getCasted());
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof Cast) {
      Cast<?> other = (Cast<?>) pOther;
      return getTypeInfo().equals(other.getTypeInfo()) && getCasted().equals(other.getCasted());
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

  public static <ConstantType> Cast<ConstantType> of(
      TypeInfo pTypeInfo, NumeralFormula<ConstantType> pCasted) {
    return new Cast<>(pTypeInfo, pCasted);
  }
}
