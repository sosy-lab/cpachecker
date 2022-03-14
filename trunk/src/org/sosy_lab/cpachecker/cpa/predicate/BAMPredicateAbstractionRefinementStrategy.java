// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * This is an extension of {@link PredicateAbstractionRefinementStrategy} that takes care of BAM
 * internals for counterexamples.
 */
public class BAMPredicateAbstractionRefinementStrategy
    extends PredicateAbstractionRefinementStrategy {

  protected BAMPredicateAbstractionRefinementStrategy(
      final Configuration config,
      final LogManager logger,
      final Solver pSolver,
      final PredicateAbstractionManager pPredAbsMgr)
      throws InvalidConfigurationException {
    super(config, logger, pPredAbsMgr, pSolver);
  }

  @Override
  public boolean performRefinement(
      ARGReachedSet pReached,
      List<ARGState> abstractionStatesTrace,
      List<BooleanFormula> pInterpolants,
      boolean pRepeatedCounterexample)
      throws CPAException, InterruptedException {

    // overriding this method is needed, as it is possible
    // to get two or even more successive spurious counterexamples,
    // which only differ in its abstractions (with 'aggressive caching').
    // The return-value is irrelevant and never used, because pRepeatedCounterexample is ignored.

    return super.performRefinement(pReached, abstractionStatesTrace, pInterpolants, false);
  }
}
