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

import java.util.Objects;

import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;


public class Variable<ConstantType> extends AbstractFormula<ConstantType> implements NumeralFormula<ConstantType> {

  private final String name;

  private Variable(BitVectorInfo pInfo, String name) {
    super(pInfo);
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  @Override
  public String toString() {
    return getName();
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof Variable) {
      Variable<?> other = (Variable<?>) pOther;
      return getBitVectorInfo().equals(other.getBitVectorInfo())
          && getName().equals(other.getName());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getBitVectorInfo(), getName());
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

  /**
   * Gets an invariants formula representing the variable with the given name.
   *
   * @param pInfo the bit vector information.
   * @param pName the name of the variable.
   *
   * @return an invariants formula representing the variable with the given name.
   */
  static <ConstantType> Variable<ConstantType> of(BitVectorInfo pInfo, String pName) {
    return new Variable<>(pInfo, pName);
  }
}
