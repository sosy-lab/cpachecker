// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import com.google.common.collect.ImmutableList;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetDelta;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.TrackingPredicateCPARefinementContext;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * A heuristic that checks whether the ratio of generated interpolants with respect to the number of
 * refinements is below an acceptable threshold.
 */
public class DelegatingRefinerHeuristicInterpolationRate implements DelegatingRefinerHeuristic {
  private final TrackingPredicateCPARefinementContext refinementContext;
  private final LogManager logger;
  private final double acceptableInterpolantRate;
  private double currentTotalInterpolantRate;

  public DelegatingRefinerHeuristicInterpolationRate(
      TrackingPredicateCPARefinementContext pRefinementContext,
      final LogManager pLogger,
      double pInterpolantRate) {
    this.refinementContext = pRefinementContext;
    this.acceptableInterpolantRate = pInterpolantRate;
    this.logger = pLogger;
    currentTotalInterpolantRate = 0.0;
  }

  @Override
  public boolean fulfilled(
      UnmodifiableReachedSet pReached, ImmutableList<ReachedSetDelta> pDeltas) {
    ImmutableList<BooleanFormula> totalInterpolants = refinementContext.getAllInterpolants();
    int totalInterpolantNumber = totalInterpolants.size();
    int numberOfRefinements = refinementContext.getNumberOfRefinements();

    if (numberOfRefinements > 0) {
      currentTotalInterpolantRate = (double) totalInterpolantNumber / (double) numberOfRefinements;

      logger.logf(
          Level.FINEST,
          "Checking current rate of interpolants generated per refinement: %.2f.",
          currentTotalInterpolantRate);
    }

    logger.logf(
        Level.FINEST,
        "Number of interpolants per refinement is too high:  %.2f. Heuristic INTERPOLATION_RATE is"
            + " no longer applicable.",
        currentTotalInterpolantRate);
    return currentTotalInterpolantRate <= acceptableInterpolantRate;
  }
}
