// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetDelta;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

/**
 * This class implements a DelegatingRefinerHeuristic. A heuristic that monitors the rate
 * interpolant generation in predicate abstraction. All information necessary for monitoring is
 * retrieved from the predicates stored in the {@link ReachedSetDelta} sequence. It checks if the
 * number of interpolants/predicates produced per refinement remains below the configured threshold.
 * If the interpolant rate exceeds the threshold, the heuristic returns {@code false}.
 */
@Options(prefix = "cpa.predicate.delegatingRefinerHeuristics.InterpolationRate")
public class DelegatingRefinerHeuristicInterpolationRate implements DelegatingRefinerHeuristic {
  private final FormulaManagerView formulaManager;
  private final LogManager logger;
  private double currentAbstractionLocationRefinementRatio;

  private double currentTotalInterpolantRate;

  @Option(
      secure = true,
      name = "acceptableInterpolantRate",
      description =
          "Acceptable interpolant rate generated per refinement for"
              + " PredicateDelegatingRefiner heuristic.")
  private double acceptableInterpolantRate = 8.0;

  @Option(
      secure = true,
      name = "increaseFactorInterpolants",
      description =
          "Factor to increase the acceptable interpolant rate in productive runs in the"
              + " PredicateDelegatingRefiner heuristic.")
  private double increaseFactorInterpolants = 1.5;

  @Option(
      secure = true,
      name = "decreaseFactorInterpolants",
      description =
          "Factor to decrease the acceptable interpolant rate in unproductive runs in the"
              + " PredicateDelegatingRefiner heuristic.")
  private double decreaseFactorInterpolants = 2.0;

  @Option(
      secure = true,
      name = "abstractionLocationRefinementRatioUpper",
      description =
          "The upper bound for the ratio of the number of abstraction locations to the number of"
              + " refinements. Used to prevent unproductive long runs.")
  private double abstractionLocationRefinementRatioUpper = 0.28;

  @Option(
      secure = true,
      name = "abstractionLocationRefinementRatioLower",
      description =
          "The lower bound for the ratio of the number of abstraction locations to the number of"
              + " refinements. Used to prevent premature early termination.")
  private double abstractionLocationRefinementRatioLower = 0.18;

  /**
   * Constructs the heuristic monitoring interpolation rate.
   *
   * @param pConfig configuration used to inject the configurable thresholds
   * @param pLogger logger for diagnostic output
   * @param pFormulaManager FormulaManager needed to filter out trivial predicate, such as {@code
   *     true}
   * @throws InvalidConfigurationException if the rate provided is negative
   */
  public DelegatingRefinerHeuristicInterpolationRate(
      FormulaManagerView pFormulaManager, final LogManager pLogger, Configuration pConfig)
      throws InvalidConfigurationException {
    this.formulaManager = pFormulaManager;
    this.logger = pLogger;
    pConfig.inject(this);
    if (acceptableInterpolantRate < 0.0) {
      throw new InvalidConfigurationException(
          "Acceptable number of interpolants per refinement used in"
              + " DelegatingRefinerHeuristicInterpolationRate must not be negative");
    }
    if (increaseFactorInterpolants < 0.0 || decreaseFactorInterpolants <= 0.0) {
      throw new InvalidConfigurationException(
          "Factors to tune the acceptable number of interpolants per refinement used in"
              + " DelegatingRefinerHeuristicInterpolationRate must not be negative");
    }

    if (abstractionLocationRefinementRatioUpper < 0
        || abstractionLocationRefinementRatioLower < 0) {
      throw new InvalidConfigurationException(
          "The bounds for the number of abstraction locations to refinement iterations used in"
              + " DelegatingRefinerHeuristicInterpolationRate must not be negative");
    }
    currentTotalInterpolantRate = 0.0;
  }

  /**
   * Evaluates if the current interpolant rate is below the acceptable threshold. It computes the
   * average number of interpolants generated per refinement and compares it to the provided limit.
   *
   * @param pReached the current ReachedSet: unused in this heuristic
   * @param pDeltas the list of changes in the ReachedSet, used to compute the number of refinements
   *     and the number of predicates/interpolants generated so far
   * @return {@code true} if the current interpolant rate is below the acceptable threshold, {@code
   *     false} otherwise
   */
  @Override
  public boolean fulfilled(ReachedSet pReached, ImmutableList<ReachedSetDelta> pDeltas) {

    int numberRefinements = pDeltas.size();
    if (numberRefinements == 0) {
      return false;
    }

    int currentAbstractionLocationCount = 0;

    // Compute the number of interpolants added and their average ratio per refinement
    int numberInterpolants = 0;

    for (ReachedSetDelta delta : pDeltas) {
      // Get the current number of abstraction locations from the delta
      currentAbstractionLocationCount += delta.abstractionLocationsCount();

      for (AbstractState pState : delta.addedStates()) {

        PredicateAbstractState predState =
            checkNotNull(AbstractStates.extractStateByType(pState, PredicateAbstractState.class));

        if (predState.isAbstractionState()) {
          if ((!formulaManager
                  .getBooleanFormulaManager()
                  .isTrue(predState.getAbstractionFormula().asFormula())
              && !formulaManager
                  .getBooleanFormulaManager()
                  .isFalse(predState.getAbstractionFormula().asFormula()))) {
            numberInterpolants++;
          }
        }
      }
    }

    currentAbstractionLocationRefinementRatio =
        (double) currentAbstractionLocationCount / numberRefinements;

    currentTotalInterpolantRate = (double) numberInterpolants / (double) numberRefinements;

    // dynamically adjusting interpolant rate
    double effectiveAcceptableInterpolantRate = acceptableInterpolantRate;

    // if abstractionLocations to refinement ratio is very low, run usually needs a higher
    // interpolation threshold
    if (currentAbstractionLocationRefinementRatio < abstractionLocationRefinementRatioLower) {
      // acceptableInterpolantRate should be increased to prevent premature termination
      effectiveAcceptableInterpolantRate = acceptableInterpolantRate * increaseFactorInterpolants;
      if (currentTotalInterpolantRate <= effectiveAcceptableInterpolantRate) {
        logger.logf(
            Level.FINER,
            "Current rate of interpolants generated per refinement: %.2f. Ratio of abstraction"
                + " location to refinements is: %.2f.",
            currentTotalInterpolantRate,
            currentAbstractionLocationRefinementRatio);
        return true;
      } else {
        logger.logf(
            Level.FINE,
            "Current rate of interpolants generated per refinement is too high: %.2f. Ratio of"
                + " abstraction location to refinements is: %.2f. Heuristic %s is no longer"
                + " applicable.",
            currentTotalInterpolantRate,
            currentAbstractionLocationRefinementRatio,
            this.getClass().getSimpleName());
        return false;
      }
    } else if (currentAbstractionLocationRefinementRatio
        > abstractionLocationRefinementRatioUpper) {
      // acceptableInterpolantRate should be decreased to prevent long runs resulting in timeouts
      effectiveAcceptableInterpolantRate = acceptableInterpolantRate / decreaseFactorInterpolants;
      if (currentTotalInterpolantRate <= effectiveAcceptableInterpolantRate) {
        logger.logf(
            Level.FINER,
            "Current rate of interpolants generated per refinement: %.2f and ratio of abstraction"
                + " location to refinements is: %.2f.",
            currentTotalInterpolantRate,
            currentAbstractionLocationRefinementRatio);
        return true;
      } else {
        logger.logf(
            Level.FINE,
            "Current rate of interpolants generated per refinement: %.2f and ratio of abstraction"
                + " location to refinements is: %.2f. Heuristic %s is no longer applicable.",
            currentTotalInterpolantRate,
            currentAbstractionLocationRefinementRatio,
            this.getClass().getSimpleName());
        return false;
      }
    } else {
      // if abstractionLocations to refinement ratio is between the two bounds, use configured
      // acceptableInterpolantRate
      return currentTotalInterpolantRate < acceptableInterpolantRate;
    }
  }
}
