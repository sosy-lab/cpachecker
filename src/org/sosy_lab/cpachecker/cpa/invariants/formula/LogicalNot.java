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


public class LogicalNot<ConstantType> implements InvariantsFormula<ConstantType> {

  private final InvariantsFormula<ConstantType> negated;

  private LogicalNot(InvariantsFormula<ConstantType> pToNegate) {
    this.negated = pToNegate;
  }

  public InvariantsFormula<ConstantType> getNegated() {
    return this.negated;
  }

  @Override
  public String toString() {
    return String.format("(!%s)", getNegated());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof LogicalNot<?>) {
      return getNegated().equals(((LogicalNot<?>) o).getNegated());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return -getNegated().hashCode();
  }

  @Override
  public <ReturnType> ReturnType accept(InvariantsFormulaVisitor<ConstantType, ReturnType> pVisitor) {
    return pVisitor.visit(this);
  }

  static <ConstantType> LogicalNot<ConstantType> of(InvariantsFormula<ConstantType> pToNegate) {
    return new LogicalNot<>(pToNegate);
  }

}
