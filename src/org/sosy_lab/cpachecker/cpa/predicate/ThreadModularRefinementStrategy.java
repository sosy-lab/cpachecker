/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.predicate;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import java.util.Collection;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

public class ThreadModularRefinementStrategy extends PredicateAbstractionRefinementStrategy {

  public ThreadModularRefinementStrategy(
      Configuration pConfig,
      LogManager pLogger,
      PredicateAbstractionManager pPredAbsMgr,
      Solver pSolver)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pPredAbsMgr, pSolver);
  }

  @Override
  protected Pair<PredicatePrecision, ARGState> computeNewPrecision(
      ARGState pUnreachableState,
      List<ARGState> pAffectedStates,
      ARGReachedSet pReached,
      boolean pRepeatedCounterexample)
      throws RefinementFailedException {
    Pair<PredicatePrecision, ARGState> result =
        super.computeNewPrecision(
            pUnreachableState,
            pAffectedStates,
            pReached,
            pRepeatedCounterexample);

    PredicatePrecision precision = result.getFirst();
    PredicatePrecision tmPrecision =
        precision.addNodesWithCompleteFormulas(
            transformedImmutableListCopy(pAffectedStates, s -> AbstractStates.extractLocation(s)));

    // Need to add a parent of the first state, as we need to compute a corresponding path formula,
    // which starts from the parent
    Collection<ARGState> parents = pAffectedStates.get(0).getParents();
    tmPrecision =
        tmPrecision.addNodesWithCompleteFormulas(
            transformedImmutableListCopy(parents, s -> AbstractStates.extractLocation(s)));

    return Pair.of(tmPrecision, result.getSecond());
  }
}
