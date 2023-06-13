// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class QuantifiedFormulaManagerView extends BaseManagerView
    implements QuantifiedFormulaManager {

  private final QuantifiedFormulaManager manager;
  private final BooleanFormulaManagerView bfm;
  private final IntegerFormulaManager ifm;

  QuantifiedFormulaManagerView(
      FormulaWrappingHandler pWrappingHandler,
      QuantifiedFormulaManager pManager,
      BooleanFormulaManagerView pBmgr,
      IntegerFormulaManager pImgr) {
    super(pWrappingHandler);
    manager = pManager;
    bfm = pBmgr;
    ifm = pImgr;
  }

  @Override
  public BooleanFormula exists(List<? extends Formula> pVariables, BooleanFormula pBody) {
    return manager.exists(unwrap(pVariables), pBody);
  }

  @Override
  public BooleanFormula exists(Formula pVariable, BooleanFormula pBody) {
    return manager.exists(unwrap(pVariable), pBody);
  }

  @Override
  public BooleanFormula forall(List<? extends Formula> pVariables, BooleanFormula pBody) {
    return manager.forall(unwrap(pVariables), pBody);
  }

  @Override
  public BooleanFormula mkQuantifier(
      Quantifier q, List<? extends Formula> pVariables, BooleanFormula pBody) {
    return manager.mkQuantifier(q, unwrap(pVariables), pBody);
  }

  @Override
  public BooleanFormula forall(Formula pVariable, BooleanFormula pBody) {
    return manager.forall(unwrap(pVariable), pBody);
  }

  @Override
  public BooleanFormula eliminateQuantifiers(BooleanFormula pF)
      throws InterruptedException, SolverException {
    return manager.eliminateQuantifiers(pF);
  }

  /**
   * Create a universal quantified formula for that the quantification is restricted to a specific
   * range (an interval.
   *
   * <p>The result is a 'range predicate' (this term is used in several papers that describe
   * quantified formulas over arrays).
   *
   * @param pVariable The variable for that the quantification should be restricted to a specific
   *     range.
   * @param pLowerBound The lower bound of the range (interval; included in the range).
   * @param pUpperBound The upper bound of the range (included in the range).
   * @param pBody Formula for that the (restricted) quantification is applied.
   */
  public <R extends IntegerFormula> BooleanFormula forall(
      final R pVariable, final R pLowerBound, final R pUpperBound, final BooleanFormula pBody) {

    Preconditions.checkNotNull(pVariable);
    Preconditions.checkNotNull(pLowerBound);
    Preconditions.checkNotNull(pUpperBound);
    Preconditions.checkNotNull(pBody);

    List<BooleanFormula> rangeConstraint = makeRangeConstraint(pVariable, pLowerBound, pUpperBound);

    return manager.forall(
        Collections.singletonList(pVariable), bfm.implication(bfm.and(rangeConstraint), pBody));
  }

  /**
   * See {@link #forall(IntegerFormula, IntegerFormula, IntegerFormula, BooleanFormula)}.
   *
   * @return An (restricted) existential quantified formula.
   */
  public <R extends IntegerFormula> BooleanFormula exists(
      final R pVariable, final R pLowerBound, final R pUpperBound, final BooleanFormula pBody) {

    Preconditions.checkNotNull(pVariable);
    Preconditions.checkNotNull(pLowerBound);
    Preconditions.checkNotNull(pUpperBound);
    Preconditions.checkNotNull(pBody);

    List<BooleanFormula> rangeConstraint = makeRangeConstraint(pVariable, pLowerBound, pUpperBound);

    List<BooleanFormula> bodyPredicates = new ArrayList<>(rangeConstraint.size() + 1);
    bodyPredicates.addAll(rangeConstraint);
    bodyPredicates.add(pBody);

    return manager.exists(Collections.singletonList(pVariable), bfm.and(bodyPredicates));
  }

  private <R extends IntegerFormula> List<BooleanFormula> makeRangeConstraint(
      final R pVariable, final R pLowerBound, final R pUpperBound) {

    return ImmutableList.of(
        ifm.greaterOrEquals(pVariable, pLowerBound), ifm.lessOrEquals(pVariable, pUpperBound));
  }
}
