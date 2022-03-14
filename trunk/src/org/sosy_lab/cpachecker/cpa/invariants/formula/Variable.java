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
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class Variable<ConstantType> extends AbstractFormula<ConstantType>
    implements NumeralFormula<ConstantType> {

  private final MemoryLocation memoryLocation;

  private Variable(TypeInfo pInfo, MemoryLocation pMemoryLocation) {
    super(pInfo);
    this.memoryLocation = pMemoryLocation;
  }

  public MemoryLocation getMemoryLocation() {
    return this.memoryLocation;
  }

  @Override
  public String toString() {
    return getMemoryLocation().getExtendedQualifiedName();
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof Variable) {
      Variable<?> other = (Variable<?>) pOther;
      return getTypeInfo().equals(other.getTypeInfo())
          && getMemoryLocation().equals(other.getMemoryLocation());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getTypeInfo(), getMemoryLocation());
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

  /**
   * Gets an invariants formula representing the variable with the given memory location.
   *
   * @param pInfo the type information.
   * @param pMemoryLocation the memory location of the variable.
   * @return an invariants formula representing the variable with the given memory location.
   */
  static <ConstantType> Variable<ConstantType> of(TypeInfo pInfo, MemoryLocation pMemoryLocation) {
    return new Variable<>(pInfo, pMemoryLocation);
  }
}
