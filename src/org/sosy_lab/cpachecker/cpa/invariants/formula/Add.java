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

public class Add<ConstantType> implements InvariantsFormula<ConstantType> {

  private final InvariantsFormula<ConstantType> summand1;

  private final InvariantsFormula<ConstantType> summand2;

  private Add(InvariantsFormula<ConstantType> pSummand1, InvariantsFormula<ConstantType> pSummand2) {
    this.summand1 = pSummand1;
    this.summand2 = pSummand2;
  }

  public InvariantsFormula<ConstantType> getSummand1() {
    return this.summand1;
  }

  public InvariantsFormula<ConstantType> getSummand2() {
    return this.summand2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Add) {
      Add<?> other = (Add<?>) o;
      return getSummand1().equals(other.getSummand1()) && getSummand2().equals(other.getSummand2()) || getSummand1().equals(other.getSummand2()) && getSummand2().equals(other.getSummand1());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getSummand1().hashCode() + getSummand2().hashCode();
  }

  @Override
  public String toString() {
    return String.format("(%s + %s)", getSummand1(), getSummand2());
  }

  @Override
  public <ReturnType> ReturnType accept(InvariantsFormulaVisitor<ConstantType, ReturnType> pVisitor) {
    return pVisitor.visit(this);
  }

  static <ConstantType> Add<ConstantType> of(InvariantsFormula<ConstantType> pSummand1, InvariantsFormula<ConstantType> pSummand2) {
    return new Add<>(pSummand1, pSummand2);
  }

}
