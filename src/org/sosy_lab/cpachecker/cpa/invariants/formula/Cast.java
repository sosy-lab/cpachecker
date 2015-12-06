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

import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;

public class Cast<ConstantType> extends AbstractFormula<ConstantType> {

  private final NumeralFormula<ConstantType> casted;

  private Cast(BitVectorInfo pInfo, NumeralFormula<ConstantType> pCasted) {
    super(pInfo);
    this.casted = pCasted;
  }

  public NumeralFormula<ConstantType> getCasted() {
    return casted;
  }

  @Override
  public String toString() {
    return String.format("((%d%s) %s)",
        getBitVectorInfo().getSize(),
        getBitVectorInfo().isSigned() ? "" : "U",
        getCasted());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getBitVectorInfo(), getCasted());
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof Cast) {
      Cast<?> other = (Cast<?>) pOther;
      return getBitVectorInfo().equals(other.getBitVectorInfo())
          && getCasted().equals(other.getCasted());
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

  public static <ConstantType> Cast<ConstantType> of(BitVectorInfo pBitVectorInfo, NumeralFormula<ConstantType> pCasted) {
    return new Cast<>(pBitVectorInfo, pCasted);
  }

}
