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


public class Exclusion<ConstantType> extends AbstractFormula<ConstantType> implements NumeralFormula<ConstantType> {

  private final NumeralFormula<ConstantType> excluded;

  private Exclusion(NumeralFormula<ConstantType> pExcluded) {
    super(pExcluded.getTypeInfo());
    this.excluded = pExcluded;
  }

  public NumeralFormula<ConstantType> getExcluded() {
    return this.excluded;
  }

  @Override
  public String toString() {
    return String.format("\\?(%s)", getExcluded());
  }

  @Override
  public int hashCode() {
    return ~getExcluded().hashCode();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO instanceof Exclusion) {
      return getExcluded().equals(((Exclusion<?>) pO).getExcluded());
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

  public static <ConstantType> NumeralFormula<ConstantType> of(NumeralFormula<ConstantType> pToExclude) {
    return new Exclusion<>(pToExclude);
  }

}
