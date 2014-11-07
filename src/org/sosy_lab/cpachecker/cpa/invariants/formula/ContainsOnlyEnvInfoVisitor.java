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


public class ContainsOnlyEnvInfoVisitor<T> extends DefaultFormulaVisitor<T, Boolean> {

  private final CollectVarsVisitor<T> collectVarsVisitor = new CollectVarsVisitor<>();

  @Override
  public Boolean visit(Equal<T> pEqual) {
    return pEqual.accept(collectVarsVisitor).size() == 1;
  }

  @Override
  public Boolean visit(LessThan<T> pLessThan) {
    return pLessThan.accept(collectVarsVisitor).size() == 1;
  }

  @Override
  public Boolean visit(LogicalAnd<T> pAnd) {
    return pAnd.getOperand1().accept(this) && pAnd.getOperand2().accept(this);
  }

  @Override
  public Boolean visit(LogicalNot<T> pNot) {
    return pNot.getNegated().accept(this);
  }

  @Override
  public Boolean visit(Union<T> pUnion) {
    return pUnion.getOperand1().accept(this) && pUnion.getOperand2().accept(this);
  }

  @Override
  protected Boolean visitDefault(InvariantsFormula<T> pFormula) {
    return false;
  }

}
