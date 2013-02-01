/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit.refiner;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.PredicateMap;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionRefinementStrategy;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.collect.Lists;

// TODO: check whether this class is needed at all or if it can just be replaced by PredicateAbstractionRefinementStrategy
class PredicatingExplicitRefinementStrategy extends PredicateAbstractionRefinementStrategy {

  private int numberOfPredicateRefinements                    = 0;

  PredicatingExplicitRefinementStrategy(Configuration pConfig,
      LogManager pLogger,
      final FormulaManagerView pFormulaManager,
      final AbstractionManager pAbstractionManager)
          throws CPAException, InvalidConfigurationException {
    super(pConfig, pLogger, pFormulaManager, pAbstractionManager);
  }

  @Override
  public void performRefinement(
      ARGReachedSet pReached,
      List<ARGState> errorPath,
      CounterexampleTraceInfo<BooleanFormula> counterexampleTraceInfo,
      boolean pRepeatedCounterexample)
      throws CPAException {
    numberOfPredicateRefinements++;

    UnmodifiableReachedSet reached = pReached.asReachedSet();
    Precision oldPrecision = reached.getPrecision(reached.getLastState());

    Pair<ARGState, Precision> result = performRefinement(reached, oldPrecision, errorPath, counterexampleTraceInfo);

    ARGState root = result.getFirst();
    logger.log(Level.FINEST, "Found spurious counterexample,",
        "trying strategy 1: remove everything below", root, "from ART.");
    pReached.removeSubtree(root, result.getSecond());
  }

  private Pair<ARGState, Precision> performRefinement(
      UnmodifiableReachedSet reachedSet,
      Precision oldPrecision,
      List<ARGState> errorPath,
      CounterexampleTraceInfo<BooleanFormula> pInfo) throws CPAException {

    // extract predicates from interpolants
    List<Collection<AbstractionPredicate>> newPreds = Lists.newArrayList();
    for (BooleanFormula interpolant : pInfo.getInterpolants()) {
      newPreds.add(convertInterpolant(interpolant));
    }

    // create the mapping of CFA nodes to predicates, based on the counter example trace info
    PredicateMap predicateMap = new PredicateMap(newPreds, errorPath);

    Precision precision = extractPredicatePrecision(oldPrecision)
        .addLocalPredicates(predicateMap.getPredicateMapping());
    ARGState interpolationPoint = predicateMap.firstInterpolationPoint.getFirst();

    return Pair.of(interpolationPoint, precision);
  }

  @Override
  public Statistics getStatistics() {
    return new Statistics() {

      private final Statistics statistics = PredicatingExplicitRefinementStrategy.super.getStatistics();

      @Override
      public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
        out.println(this.getClass().getSimpleName() + ":");
        out.println("  number of predicate refinements:           " + numberOfPredicateRefinements);
        statistics.printStatistics(out, result, reached);
      }

      @Override
      public String getName() {
        return statistics.getName();
      }
    };
  }
}
