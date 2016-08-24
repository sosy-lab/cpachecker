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

import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;


public class BooleanConstant<ConstantType> implements BooleanFormula<ConstantType> {

  private static final BooleanConstant<?> FALSE = new BooleanConstant<>(false);

  private static final BooleanConstant<?> TRUE = new BooleanConstant<>(true);

  private final boolean value;

  private BooleanConstant(boolean pValue) {
    this.value = pValue;
  }

  boolean getValue() {
    return value;
  }

  @Override
  public boolean equals(Object pOther) {
    assert (this == pOther
            && pOther instanceof BooleanConstant
            && this.value == ((BooleanConstant<?>) pOther).value)
        || (this != pOther
            && (!(pOther instanceof BooleanConstant)
                || this.value != ((BooleanConstant<?>) pOther).value));
    return this == pOther;
  }

  @Override
  public int hashCode() {
    return value ? 0 : 1;
  }

  @Override
  public String toString() {
    return Boolean.toString(value);
  }

  public BooleanConstant<CompoundInterval> negate() {
    return fromBool(!value);
  }

  @Override
  public <ReturnType> ReturnType accept(BooleanFormulaVisitor<ConstantType, ReturnType> pVisitor) {
    return value? pVisitor.visitTrue() : pVisitor.visitFalse();
  }

  @Override
  public <ReturnType, ParamType> ReturnType accept(
      ParameterizedBooleanFormulaVisitor<ConstantType, ParamType, ReturnType> pVisitor, ParamType pParameter) {
    return value ? pVisitor.visitTrue(pParameter) : pVisitor.visitFalse(pParameter);
  }

  public static <ConstantType> BooleanConstant<ConstantType> fromBool(boolean pBoolean) {
    return pBoolean ? BooleanConstant.<ConstantType>getTrue() : BooleanConstant.<ConstantType>getFalse();
  }

  @SuppressWarnings("unchecked")
  public static <ConstantType> BooleanConstant<ConstantType> getFalse() {
    return (BooleanConstant<ConstantType>) FALSE;
  }

  @SuppressWarnings("unchecked")
  public static <ConstantType> BooleanConstant<ConstantType> getTrue() {
    return (BooleanConstant<ConstantType>) TRUE;
  }

  public static boolean isTrue(BooleanFormula<?> pConstant) {
    return TRUE.equals(pConstant);
  }

  public static boolean isFalse(BooleanFormula<?> pConstant) {
    return FALSE.equals(pConstant);
  }

}
