/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.termination;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.BINARY_OR;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

public class RankingRelation {

  private final CExpression cExpression;
  private final BooleanFormula formula;
  private FormulaManagerView formulaManagerView;
  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  public RankingRelation(
      CExpression pCExpression,
      BooleanFormula pFormula,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      FormulaManagerView pFormulaManagerView) {
    cExpression = checkNotNull(pCExpression);
    formula = checkNotNull(pFormula);
    formulaManagerView = checkNotNull(pFormulaManagerView);
    binaryExpressionBuilder = checkNotNull(pBinaryExpressionBuilder);
  }

  public CExpression asCExpression() {
    return cExpression;
  }

  public BooleanFormula asFormula() {
    return formula;
  }

  public String getRankingFunction() {
    return formula.toString();
  }

  @Override
  public int hashCode() {
    return asCExpression().hashCode();
  }

  /**
   * Create a new {@link RankingRelation} that is the disjunction of this and <code>other</code>
   * @param other the {@link RankingRelation} to merge with
   * @return a new {@link RankingRelation}
   */
  public RankingRelation merge(RankingRelation other) {
    BooleanFormula newFormula = formulaManagerView.makeOr(formula, other.formula);
    CExpression newCExpression =
        binaryExpressionBuilder.buildBinaryExpressionUnchecked(cExpression, cExpression, BINARY_OR);

    return new RankingRelation(
        newCExpression, newFormula, binaryExpressionBuilder, formulaManagerView);
  }

  @Override
  public boolean equals(Object pObj) {
   if (this == pObj) {
     return true;
   }
   if (!(pObj instanceof RankingRelation)) {
     return false;
   }

   RankingRelation that = (RankingRelation) pObj;
   return this.asCExpression().equals(that.asCExpression());
  }

  @Override
  public String toString() {
    return asCExpression().toASTString();
  }
}
