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
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetDelta;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils.TrackingPredicateCPARefinementContext;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * This class implements a DelegatingRefinerHeuristic. A heuristic that monitors the rate
 * interpolant generation in predicate abstraction. All information necessary for monitoring is
 * retrieved from the {@link TrackingPredicateCPARefinementContext}, which stores a history of the
 * number refinements and the generated interpolants. It checks if the number of interpolants
 * produced per refinement remains below the configured threshold. If the interpolant rate exceeds
 * the threshold, the heuristic returns {@code false}.
 */
public class DelegatingRefinerHeuristicInterpolationRate implements DelegatingRefinerHeuristic {
  private final TrackingPredicateCPARefinementContext refinementContext;
  private final LogManager logger;
  private final double acceptableInterpolantRate;
  private double currentTotalInterpolantRate;

  /**
   * Constructs the heuristic monitoring interpolation rate.
   *
   * @param pRefinementContext provides information about refinement count and interpolants across
   *     iterations
   * @param pLogger logger for diagnostic output
   * @param pInterpolantRate maximum allowed rate of interpolants generated per refinement
   * @throws InvalidConfigurationException if the rate provided is negative
   */
  public DelegatingRefinerHeuristicInterpolationRate(
      TrackingPredicateCPARefinementContext pRefinementContext,
      final LogManager pLogger,
      double pInterpolantRate)
      throws InvalidConfigurationException {
    this.refinementContext = pRefinementContext;
    this.acceptableInterpolantRate = pInterpolantRate;
    this.logger = pLogger;
    if (pInterpolantRate < 0.0) {
      throw new InvalidConfigurationException(
          "Acceptable number of interpolants per refinement must not be negative");
    }
    currentTotalInterpolantRate = 0.0;
  }

  /**
   * Evaluates if the current interpolant rate is below the acceptable threshold. It computes the
   * average number of interpolants generated per refinement and compares it to the provided limit.
   *
   * @param pReached the current ReachedSet: unused in this heuristic as the ReachedSet does not
   *     provide information about the refinement iterations
   * @param pDeltas the list of changes in the ReachedSet: unused in this heuristic as the
   *     ReachedSet does not provide information about the refinement iterations
   * @return {@code true} if the current interpolant rate is below the acceptable threshold, {@code
   *     false} otherwise
   */
  @Override
  public boolean fulfilled(ReachedSet pReached, ImmutableList<ReachedSetDelta> pDeltas) {
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
