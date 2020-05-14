/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
