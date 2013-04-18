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

import com.google.common.base.Preconditions;

public class Constant<T> implements InvariantsFormula<T> {

  private final T value;

  private Constant(T pValue) {
    Preconditions.checkNotNull(pValue);
    this.value = pValue;
  }

  public T getValue() {
    return this.value;
  }

  @Override
  public String toString() {
    return getValue().toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Constant) {
      return getValue().equals(((Constant<?>) o).getValue());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getValue().hashCode();
  }

  @Override
  public <ReturnType> ReturnType accept(InvariantsFormulaVisitor<T, ReturnType> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public <ReturnType, ParamType> ReturnType accept(
      ParameterizedInvariantsFormulaVisitor<T, ParamType, ReturnType> pVisitor, ParamType pParameter) {
    return pVisitor.visit(this, pParameter);
  }

  static <T> Constant<T> of(T pValue) {
    return new Constant<>(pValue);
  }

}
