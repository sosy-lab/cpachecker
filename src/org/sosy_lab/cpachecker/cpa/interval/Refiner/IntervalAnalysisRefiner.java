/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.interval.Refiner;

import com.google.common.base.Preconditions;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisPrecision;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisPrecision.IntervalAnalysisFullPrecision;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

public class IntervalAnalysisRefiner implements ARGBasedRefiner {

  StrongestPostOperator<IntervalAnalysisState> strongestPostOperator;

  private LogManager logger;

  private CFA cfa;
  private Configuration config;

  public static Refiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    return AbstractARGBasedRefiner.forARGBasedRefiner(create0(pCpa), pCpa);
  }

  public static ARGBasedRefiner create0(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    final IntervalAnalysisCPA intervalAnalysisCpa =
        CPAs.retrieveCPAOrFail(pCpa, IntervalAnalysisCPA.class, IntervalAnalysisRefiner.class);

    final LogManager logger = intervalAnalysisCpa.getLogger();
    final Configuration config = intervalAnalysisCpa.getConfiguration();
    intervalAnalysisCpa.injectRefinablePrecision();
    CFA pCfa = intervalAnalysisCpa.getCFA();

    final StrongestPostOperator<IntervalAnalysisState> strongestPostOp =
        new IntervalAnalysisStrongestPostOperator(logger, false, 2000);

    return new IntervalAnalysisRefiner(strongestPostOp, config, logger, pCfa);
  }

  IntervalAnalysisRefiner(
      final StrongestPostOperator<IntervalAnalysisState> pStrongestPostOperator,
      final Configuration pConfig,
      final LogManager pLogger,
      final CFA pCfa)
      throws InvalidConfigurationException {

    logger = pLogger;
    pConfig.inject(this, IntervalAnalysisRefiner.class);

    config = pConfig;
    cfa = pCfa;
    strongestPostOperator = pStrongestPostOperator;
  }

  @Override
  public CounterexampleInfo performRefinementForPath(
      final ARGReachedSet pReached, ARGPath targetPathToUse)
      throws InterruptedException, CPAException {

    Set<String> collectionOfVariables = new HashSet<>();

    if (!isFeasible(targetPathToUse, new IntervalAnalysisFullPrecision(), collectionOfVariables)) {
      logger.log(Level.INFO, "performing refinement ...");
      refine(targetPathToUse, pReached, Collections.unmodifiableSet(collectionOfVariables));
      logger.log(Level.INFO, "refinement finished");
      return CounterexampleInfo.spurious();
    }
    logger.log(Level.INFO, "errorpath was feasible, no refinement needed");
    return CounterexampleInfo.feasibleImprecise(targetPathToUse);
  }

  private void refine(
      ARGPath targetPathToUse, final ARGReachedSet pReached, Set<String> collectionOfVariables)
      throws InterruptedException, CPAException {

    IntervalAnalysisPrecision minimalPrecisionRequired =
        new IntervalAnalysisPrecision(collectionOfVariables);

    logger.log(Level.SEVERE, collectionOfVariables.toString());
    for (String x : collectionOfVariables) {
      minimalPrecisionRequired.remove(x);
      if (isFeasible(targetPathToUse, minimalPrecisionRequired, new HashSet<>())) {
        minimalPrecisionRequired.add(x);
      }
    }
    logger.log(Level.INFO, minimalPrecisionRequired.toString());

    UnmodifiableReachedSet reachedSet = pReached.asReachedSet();
    Precision precision = reachedSet.getPrecision(reachedSet.getLastState());
    IntervalAnalysisPrecision oldPrecision =
        Precisions.extractPrecisionByType(precision, IntervalAnalysisPrecision.class);

    minimalPrecisionRequired.join(oldPrecision);

    PathIterator iterator = targetPathToUse.reverseFullPathIterator();

    ARGState cutpoint = null;
    while (iterator.hasNext()) {
      iterator.advance();
      if (!isFeasible(iterator.getSuffixInclusive(), minimalPrecisionRequired, new HashSet<>())) {
        cutpoint = iterator.getAbstractState();
        break;
      }
    }

    Preconditions.checkNotNull(cutpoint);

    pReached.removeSubtree(
        cutpoint, minimalPrecisionRequired, p -> p instanceof IntervalAnalysisPrecision);
  }

  private boolean isFeasible(
      ARGPath pPath, IntervalAnalysisPrecision precisionToUse, Set<String> usedVariables)
      throws InterruptedException, CPAException {
    IntervalAnalysisState next =
        AbstractStates.extractStateByType(pPath.getFirstState(), IntervalAnalysisState.class);
    Deque<IntervalAnalysisState> pCallstack = new ArrayDeque<>();
    PathIterator iterator = pPath.fullPathIterator();
    while (iterator.hasNext()) {
      final CFAEdge edge = iterator.getOutgoingEdge();
      Optional<IntervalAnalysisState> maybeNext =
          strongestPostOperator.step(next, edge, precisionToUse, pCallstack, pPath);
      if (!maybeNext.isPresent()) {
        return false;
      } else {
        next = maybeNext.get();
        usedVariables.addAll(next.getVariables());
      }
      iterator.advance();
    }
    return true;
  }
}
