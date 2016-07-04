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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.CheckReturnValue;

public class RankingRelation {

  private final Map<CExpression, BooleanFormula> rankingRelations;
  private final FormulaManagerView formulaManagerView;
  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  public RankingRelation(
      CExpression pCExpression,
      BooleanFormula pFormula,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      FormulaManagerView pFormulaManagerView) {
    rankingRelations = ImmutableMap.of(pCExpression, pFormula);
    formulaManagerView = checkNotNull(pFormulaManagerView);
    binaryExpressionBuilder = checkNotNull(pBinaryExpressionBuilder);
  }

  private RankingRelation(
      ImmutableMap<CExpression, BooleanFormula> pRankingRelations,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      FormulaManagerView pFormulaManagerView) {
    rankingRelations = checkNotNull(pRankingRelations);
    formulaManagerView = checkNotNull(pFormulaManagerView);
    binaryExpressionBuilder = checkNotNull(pBinaryExpressionBuilder);
  }

  public CExpression asCExpression() {
    Preconditions.checkState(!rankingRelations.isEmpty());
    return rankingRelations
        .keySet()
        .stream()
        .reduce((a,b) -> binaryExpressionBuilder.buildBinaryExpressionUnchecked(a, b, BINARY_OR))
        .get();
  }

  public BooleanFormula asFormula() {
    return formulaManagerView.getBooleanFormulaManager().or(rankingRelations.values());
  }

  public BooleanFormula asFormulaFromOtherSolver(FormulaManagerView pFormulaManagerView) {
    return pFormulaManagerView.translateFrom(asFormula(), formulaManagerView);
  }

  /**
   * Create a new {@link RankingRelation} that is the disjunction of this and <code>other</code>
   * @param other the {@link RankingRelation} to merge with
   * @return a new {@link RankingRelation}
   */
  @CheckReturnValue
  public RankingRelation merge(RankingRelation other) {
    HashMap<CExpression, BooleanFormula> newRankingRelations = Maps.newHashMap();
    newRankingRelations.putAll(rankingRelations);

    for (Entry<CExpression, BooleanFormula> entry : other.rankingRelations.entrySet()) {
      newRankingRelations.putIfAbsent(entry.getKey(), entry.getValue());
    }

    return new RankingRelation(
        ImmutableMap.copyOf(newRankingRelations), binaryExpressionBuilder, formulaManagerView);
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
    return this.rankingRelations.keySet().equals(that.rankingRelations.keySet());
  }

  @Override
  public int hashCode() {
    return this.rankingRelations.keySet().hashCode();
  }


  @Override
  public String toString() {
    return asCExpression().toASTString();
  }
}
