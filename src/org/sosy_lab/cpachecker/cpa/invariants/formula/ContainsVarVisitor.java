// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

import java.util.Map;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Instances of this class are visitors used to check if the visited formulae contain a specified
 * variable.
 *
 * @param <T> the type of the constants used in the visited formulae.
 */
public class ContainsVarVisitor<T>
    implements ParameterizedNumeralFormulaVisitor<T, MemoryLocation, Boolean>,
        ParameterizedBooleanFormulaVisitor<T, MemoryLocation, Boolean> {

  private final Map<? extends MemoryLocation, ? extends NumeralFormula<T>> environment;

  public ContainsVarVisitor() {
    this(null);
  }

  public ContainsVarVisitor(
      Map<? extends MemoryLocation, ? extends NumeralFormula<T>> pEnvironment) {
    this.environment = pEnvironment;
  }

  @Override
  public Boolean visit(Add<T> pAdd, MemoryLocation pVarName) {
    return pAdd.getSummand1().accept(this, pVarName) || pAdd.getSummand2().accept(this, pVarName);
  }

  @Override
  public Boolean visit(BinaryAnd<T> pAnd, MemoryLocation pVarName) {
    return pAnd.getOperand1().accept(this, pVarName) || pAnd.getOperand2().accept(this, pVarName);
  }

  @Override
  public Boolean visit(BinaryNot<T> pNot, MemoryLocation pVarName) {
    return pNot.getFlipped().accept(this, pVarName);
  }

  @Override
  public Boolean visit(BinaryOr<T> pOr, MemoryLocation pVarName) {
    return pOr.getOperand1().accept(this, pVarName) || pOr.getOperand2().accept(this, pVarName);
  }

  @Override
  public Boolean visit(BinaryXor<T> pXor, MemoryLocation pVarName) {
    return pXor.getOperand1().accept(this, pVarName) || pXor.getOperand2().accept(this, pVarName);
  }

  @Override
  public Boolean visit(Constant<T> pConstant, MemoryLocation pVarName) {
    return false;
  }

  @Override
  public Boolean visit(Divide<T> pDivide, MemoryLocation pVarName) {
    return pDivide.getNumerator().accept(this, pVarName)
        || pDivide.getDenominator().accept(this, pVarName);
  }

  @Override
  public Boolean visit(Equal<T> pEqual, MemoryLocation pVarName) {
    return pEqual.getOperand1().accept(this, pVarName)
        || pEqual.getOperand2().accept(this, pVarName);
  }

  @Override
  public Boolean visit(Exclusion<T> pExclusion, MemoryLocation pParameter) {
    return pExclusion.getExcluded().accept(this, pParameter);
  }

  @Override
  public Boolean visit(LessThan<T> pLessThan, MemoryLocation pVarName) {
    return pLessThan.getOperand1().accept(this, pVarName)
        || pLessThan.getOperand2().accept(this, pVarName);
  }

  @Override
  public Boolean visit(LogicalAnd<T> pAnd, MemoryLocation pVarName) {
    return pAnd.getOperand1().accept(this, pVarName) || pAnd.getOperand2().accept(this, pVarName);
  }

  @Override
  public Boolean visit(LogicalNot<T> pNot, MemoryLocation pVarName) {
    return pNot.getNegated().accept(this, pVarName);
  }

  @Override
  public Boolean visit(Modulo<T> pModulo, MemoryLocation pVarName) {
    return pModulo.getNumerator().accept(this, pVarName)
        || pModulo.getDenominator().accept(this, pVarName);
  }

  @Override
  public Boolean visit(Multiply<T> pMultiply, MemoryLocation pVarName) {
    return pMultiply.getFactor1().accept(this, pVarName)
        || pMultiply.getFactor2().accept(this, pVarName);
  }

  @Override
  public Boolean visit(ShiftLeft<T> pShiftLeft, MemoryLocation pVarName) {
    return pShiftLeft.getShifted().accept(this, pVarName)
        || pShiftLeft.getShiftDistance().accept(this, pVarName);
  }

  @Override
  public Boolean visit(ShiftRight<T> pShiftRight, MemoryLocation pVarName) {
    return pShiftRight.getShifted().accept(this, pVarName)
        || pShiftRight.getShiftDistance().accept(this, pVarName);
  }

  @Override
  public Boolean visit(Union<T> pUnion, MemoryLocation pVarName) {
    return pUnion.getOperand1().accept(this, pVarName)
        || pUnion.getOperand2().accept(this, pVarName);
  }

  @Override
  public Boolean visitFalse(MemoryLocation pParameter) {
    return false;
  }

  @Override
  public Boolean visitTrue(MemoryLocation pParameter) {
    return false;
  }

  @Override
  public Boolean visit(Variable<T> pVariable, MemoryLocation pVarName) {
    return pVariable.getMemoryLocation().equals(pVarName) || refersTo(pVariable, pVarName);
  }

  @Override
  public Boolean visit(Cast<T> pCast, MemoryLocation pVarName) {
    return pCast.getCasted().accept(this, pVarName);
  }

  @Override
  public Boolean visit(IfThenElse<T> pIfThenElse, MemoryLocation pVarName) {
    return pIfThenElse.getCondition().accept(this, pVarName)
        || pIfThenElse.getPositiveCase().accept(this, pVarName)
        || pIfThenElse.getNegativeCase().accept(this, pVarName);
  }

  private boolean refersTo(Variable<T> pVariable, MemoryLocation pVarName) {
    if (this.environment == null) {
      return false;
    }
    NumeralFormula<T> value = this.environment.get(pVariable.getMemoryLocation());
    if (value == null) {
      return false;
    }
    return value.accept(this, pVarName);
  }
}
