// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.regions.SymbolicRegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.SolverViewBasedTest0;

public class CombinePredicatePrecisionOperatorTest extends SolverViewBasedTest0 {

  private CombinePredicatePrecisionOperator operator;
  private AbstractionManager abstractionManager;

  @Before
  public void setUp() throws Exception {
    operator = new CombinePredicatePrecisionOperator();
    abstractionManager =
        new AbstractionManager(new SymbolicRegionManager(solver), config, logger, solver);
  }

  private AbstractionPredicate predicate(String pVariable, int pValue) {
    return abstractionManager.makePredicate(
        imgrv.equal(imgrv.makeVariable(pVariable), imgrv.makeNumber(pValue)));
  }

  private static PredicatePrecision precision(AbstractionPredicate... pPredicates) {
    return PredicatePrecision.empty().addGlobalPredicates(ImmutableList.copyOf(pPredicates));
  }

  private static ImmutableList<AbstractionPredicate> globalPredicatesOf(Precision pPrecision) {
    return ((PredicatePrecision) pPrecision).getGlobalPredicates().asList();
  }

  @Test
  public void combineKeepsEverything() throws Exception {
    // dropping x = 1 would require refining it again
    AbstractionPredicate iIsZero = predicate("i", 0);
    AbstractionPredicate xIsOne = predicate("x", 1);
    assertThat(
            globalPredicatesOf(
                operator.combine(ImmutableList.of(precision(iIsZero), precision(iIsZero, xIsOne)))))
        .containsExactly(iIsZero, xIsOne);
  }

  @Test
  public void combineWithCoveredPrecisionIsUnchanged() throws Exception {
    AbstractionPredicate iIsZero = predicate("i", 0);
    AbstractionPredicate xIsOne = predicate("x", 1);
    PredicatePrecision stronger = precision(iIsZero, xIsOne);
    assertThat(operator.combine(ImmutableList.of(stronger, precision(xIsOne)))).isEqualTo(stronger);
  }
}
