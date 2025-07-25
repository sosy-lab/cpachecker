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
import org.sosy_lab.cpachecker.cpa.invariants.TypeInfo;
import org.sosy_lab.cpachecker.cpa.invariants.Typed;

/**
 * Instances of this class represent constants within invariants formulae.
 *
 * @param <T> the type of the constant value.
 */
public class Constant<T> extends AbstractFormula<T> implements NumeralFormula<T> {

  /** The value of the constant. */
  private final T value;

  /**
   * Creates a new constant with the given value.
   *
   * @param pInfo the type information for the constant.
   * @param pValue the value of the constant.
   */
  private Constant(TypeInfo pInfo, T pValue) {
    super(pInfo);
    Preconditions.checkNotNull(pValue);
    if (pValue instanceof Typed typed) {
      Preconditions.checkArgument(pInfo.equals(typed.getTypeInfo()));
    }
    value = pValue;
  }

  /**
   * Gets the value of the constant.
   *
   * @return the value of the constant.
   */
  public T getValue() {
    return value;
  }

  @Override
  public String toString() {
    return getValue().toString();
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof Constant<?> other
        && getTypeInfo().equals(other.getTypeInfo())
        && getValue().equals(other.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getTypeInfo(), getValue());
  }

  @Override
  public <ReturnType> ReturnType accept(NumeralFormulaVisitor<T, ReturnType> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public <ReturnType, ParamType> ReturnType accept(
      ParameterizedNumeralFormulaVisitor<T, ParamType, ReturnType> pVisitor, ParamType pParameter) {
    return pVisitor.visit(this, pParameter);
  }

  /**
   * Gets an invariants formula representing a constant with the given value.
   *
   * @param pInfo the type information for the constant.
   * @param pValue the value of the constant.
   * @return an invariants formula representing a constant with the given value.
   */
  static <T> Constant<T> of(TypeInfo pInfo, T pValue) {
    return new Constant<>(pInfo, pValue);
  }

  /**
   * Gets an invariants formula representing a constant with the given value.
   *
   * @param pValue the value of the constant.
   * @return an invariants formula representing a constant with the given value.
   */
  static <T extends Typed> Constant<T> of(T pValue) {
    return new Constant<>(pValue.getTypeInfo(), pValue);
  }
}
