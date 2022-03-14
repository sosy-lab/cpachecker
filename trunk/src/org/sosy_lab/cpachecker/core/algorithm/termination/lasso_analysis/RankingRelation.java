// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.BINARY_OR;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.EQUALS;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.ONE;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.ZERO;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class RankingRelation {

  private final ImmutableSet<CExpression> rankingRelations;
  private final ImmutableSet<BooleanFormula> rankingRelationFormulas;
  private final ImmutableSet<BooleanFormula> supportingInvariants;
  private final FormulaManagerView formulaManagerView;
  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  public RankingRelation(
      Optional<CExpression> pRankingRelation,
      BooleanFormula pFormula,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      FormulaManagerView pFormulaManagerView) {
    rankingRelations = pRankingRelation.map(ImmutableSet::of).orElse(ImmutableSet.of());
    rankingRelationFormulas = ImmutableSet.of(pFormula);
    supportingInvariants = ImmutableSet.of();
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
    return rankingRelations.stream()
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

  public ImmutableSet<BooleanFormula> getSupportingInvariants() {
    return supportingInvariants;
  }

  public FormulaManagerView getFormulaManager() {
    return formulaManagerView;
  }

  /**
   * Create a new {@link RankingRelation} that is the disjunction of this and <code>other</code>
   *
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
   *
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
    return rankingRelationFormulas.equals(that.rankingRelationFormulas);
  }

  @Override
  public int hashCode() {
    return rankingRelationFormulas.hashCode();
  }

  @Override
  public String toString() {
    return asFormula().toString();
  }
}
