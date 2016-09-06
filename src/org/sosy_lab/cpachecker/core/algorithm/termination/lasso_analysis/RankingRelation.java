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
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.BINARY_OR;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.EQUALS;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.ONE;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.ZERO;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;

public class RankingRelation {

  private final Set<CExpression> rankingRelations;
  private final Set<BooleanFormula> rankingRelationFormulas;
  private final Set<BooleanFormula> supportingInvariants;
  private final FormulaManagerView formulaManagerView;
  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  public RankingRelation(
      Optional<CExpression> pRankingRelation,
      BooleanFormula pFormula,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      FormulaManagerView pFormulaManagerView) {
    rankingRelations = pRankingRelation.map(ImmutableSet::of).orElse(ImmutableSet.of());
    rankingRelationFormulas = ImmutableSet.of(pFormula);
    supportingInvariants = Collections.emptySet();
    formulaManagerView = checkNotNull(pFormulaManagerView);
    binaryExpressionBuilder = checkNotNull(pBinaryExpressionBuilder);
  }

  public RankingRelation(
      CExpression pRankingRelation,
      BooleanFormula pFormula,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      FormulaManagerView pFormulaManagerView) {
    this(Optional.of(pRankingRelation), pFormula, pBinaryExpressionBuilder, pFormulaManagerView);
  }

  private RankingRelation(
      Set<CExpression> pRankingRelations,
      Set<BooleanFormula> pRankingRelationFormulas,
      Set<BooleanFormula> pSupportingInvariants,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      FormulaManagerView pFormulaManagerView) {
    rankingRelations = ImmutableSet.copyOf(pRankingRelations);
    rankingRelationFormulas = ImmutableSet.copyOf(pRankingRelationFormulas);
    supportingInvariants = ImmutableSet.copyOf(pSupportingInvariants);
    formulaManagerView = checkNotNull(pFormulaManagerView);
    binaryExpressionBuilder = checkNotNull(pBinaryExpressionBuilder);
  }

  public CExpression asCExpression() {
    assert !rankingRelationFormulas.isEmpty();
    return rankingRelations
        .stream()
        .reduce((a, b) -> binaryExpressionBuilder.buildBinaryExpressionUnchecked(a, b, BINARY_OR))
        .orElseGet(() -> binaryExpressionBuilder.buildBinaryExpressionUnchecked(ZERO, ONE, EQUALS));
  }

  public BooleanFormula asFormula() {
    assert !rankingRelationFormulas.isEmpty();
    return formulaManagerView.getBooleanFormulaManager().or(rankingRelationFormulas);
  }

  public BooleanFormula asFormulaFromOtherSolver(FormulaManagerView pFormulaManagerView) {
    return pFormulaManagerView.translateFrom(asFormula(), formulaManagerView);
  }

  public Collection<FormulaReportingState> getSupportingInvariants() {
    return transformedImmutableListCopy(
        supportingInvariants, i -> new TerminationInvariantSupplierState(formulaManagerView, i));
  }

  /**
   * Create a new {@link RankingRelation} that is the disjunction of this and <code>other</code>
   * @param other the {@link RankingRelation} to merge with
   * @return a new {@link RankingRelation}
   */
  @CheckReturnValue
  public RankingRelation merge(RankingRelation other) {
    ImmutableSet.Builder<CExpression> newRankingRelations = ImmutableSet.builder();
    newRankingRelations.addAll(rankingRelations);
    newRankingRelations.addAll(other.rankingRelations);

    ImmutableSet.Builder<BooleanFormula> newRankingRelationFormulas = ImmutableSet.builder();
    newRankingRelationFormulas.addAll(rankingRelationFormulas);
    newRankingRelationFormulas.addAll(other.rankingRelationFormulas);

    ImmutableSet.Builder<BooleanFormula> newSupportingInvariants = ImmutableSet.builder();
    newSupportingInvariants.addAll(supportingInvariants);
    newSupportingInvariants.addAll(other.supportingInvariants);

    return new RankingRelation(
        newRankingRelations.build(),
        newRankingRelationFormulas.build(),
        newSupportingInvariants.build(),
        binaryExpressionBuilder,
        formulaManagerView);
  }

  /**
   * Creates a new {@link RankingRelation} that contains the given supporting invariants.
   * @param pSupportingInvariants the invariants to add to the created {@link RankingRelation}
   * @return a new {@link RankingRelation}
   */
  @CheckReturnValue
  public RankingRelation withSupportingInvariants(
      Collection<BooleanFormula> pSupportingInvariants) {
    ImmutableSet<BooleanFormula> newSupportingInvariants =
        ImmutableSet.<BooleanFormula>builder()
            .addAll(supportingInvariants)
            .addAll(pSupportingInvariants)
            .build();
    return new RankingRelation(
        rankingRelations,
        rankingRelationFormulas,
        newSupportingInvariants,
        binaryExpressionBuilder,
        formulaManagerView);
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
    return this.rankingRelationFormulas.equals(that.rankingRelationFormulas);
  }

  @Override
  public int hashCode() {
    return this.rankingRelationFormulas.hashCode();
  }

  @Override
  public String toString() {
    return asFormula().toString();
  }

  private static class TerminationInvariantSupplierState implements FormulaReportingState {

    private final FormulaManagerView fmgr;
    private final BooleanFormula invariant;

    public TerminationInvariantSupplierState(FormulaManagerView pFmgr, BooleanFormula pInvariant) {
      fmgr = checkNotNull(pFmgr);
      invariant = checkNotNull(pInvariant);
    }

    @Override
    public BooleanFormula getFormulaApproximation(FormulaManagerView pManager) {
      return pManager.translateFrom(invariant, fmgr);
    }

    @Override
    public String toString() {
      return TerminationInvariantSupplierState.class.getSimpleName() + "[" + invariant + "]";
    }
  }
}
