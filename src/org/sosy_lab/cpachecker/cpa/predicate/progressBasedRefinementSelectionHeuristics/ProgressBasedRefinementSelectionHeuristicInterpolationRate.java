// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionHeuristics;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.TrackingForwardingReachedSet.ReachedSetDelta;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

/**
 * Monitors refinement progress by evaluating the rate of change of net-active interpolants over a
 * sliding window. To detect unbounded predicate growth (e.g., unbounded loop unrolling), this
 * heuristic signals divergence if the growth rate remains above a configured threshold for too many
 * consecutive refinement iterations.
 */
@Options(prefix = "cpa.predicate.progressBasedRefinementSelectionHeuristics.InterpolationRate")
public class ProgressBasedRefinementSelectionHeuristicInterpolationRate
    implements ProgressBasedRefinementSelectionHeuristic {

  /**
   * Describes, relative to the configured bounds, how the abstraction-location-to-refinement ratio
   * compares, and therefore how the acceptable interpolant rate should be adjusted.
   */
  private enum RatioRegime {
    TOO_LOW,
    TOO_HIGH,
    NORMAL
  }

  private final FormulaManagerView formulaManager;
  private final LogManager logger;

  private double currentAbstractionLocationRefinementRatio;
  private double currentTotalInterpolantRate;

  private int totalAbstractionLocationCount = 0;
  private int totalInterpolantCount = 0;

  // Persistent counter of refinement iterations processed by this heuristic. Cannot rely on
  // pDeltas.size(), since the delta history supplied by the CEGAR algorithm is bounded (an
  // EvictingQueue), so its size plateaus once the window is full.
  private long numberRefinements = 0;

  // Holds rateWindowSize + 1 samples, so that rateWindowSize intervals can be derived. This window
  // is owned by the heuristic itself and is independent of the bound on the delta history supplied
  // by the CEGAR algorithm.
  private final EvictingQueue<Integer> recentInterpolantCounts;
  private int consecutivePersistentIterations = 0;

  @Option(
      secure = true,
      name = "acceptableInterpolantRate",
      description =
          "Acceptable interpolant rate generated per refinement for"
              + " ProgressBasedRefinementSelection heuristic.")
  private double acceptableInterpolantRate = 8.0;

  @Option(
      secure = true,
      name = "increaseFactorInterpolants",
      description =
          "Factor to increase the acceptable interpolant rate in productive runs in the"
              + " ProgressBasedRefinementSelection heuristic.")
  private double increaseFactorInterpolants = 1.5;

  @Option(
      secure = true,
      name = "decreaseFactorInterpolants",
      description =
          "Factor to decrease the acceptable interpolant rate in unproductive runs in the"
              + " ProgressBasedRefinementSelection heuristic.")
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

  @Option(
      secure = true,
      name = "rateWindowSize",
      description =
          "The size of the sliding window, in refinement iterations, used to compute the recent"
              + " net-active interpolant rate of change.")
  private int rateWindowSize = 10;

  @Option(
      secure = true,
      name = "persistentRateThreshold",
      description =
          "The minimum net-active interpolant rate of change that is considered 'non-vanishing'"
              + " for the purposes of detecting sustained, unbounded predicate growth.")
  private double persistentRateThreshold = 0.05;

  @Option(
      secure = true,
      name = "maxPersistentIterations",
      description =
          "The maximum number of consecutive refinement iterations for which the net-active"
              + " interpolant rate of change may remain at or above persistentRateThreshold before"
              + " the heuristic signals divergence.")
  private int maxPersistentIterations = 10;

  /**
   * Constructs the heuristic monitoring interpolation rate.
   *
   * @param pConfig configuration used to inject the configurable thresholds
   * @param pLogger logger for diagnostic output
   * @param pFormulaManager FormulaManager needed to filter out trivial predicates, such as {@code
   *     true}
   * @throws InvalidConfigurationException if any provided configuration value is invalid
   */
  public ProgressBasedRefinementSelectionHeuristicInterpolationRate(
      FormulaManagerView pFormulaManager, final LogManager pLogger, Configuration pConfig)
      throws InvalidConfigurationException {
    formulaManager = pFormulaManager;
    logger = pLogger;
    pConfig.inject(this);

    validateConfiguration();

    currentTotalInterpolantRate = 0.0;
    recentInterpolantCounts = EvictingQueue.create(rateWindowSize + 1);
  }

  private void validateConfiguration() throws InvalidConfigurationException {
    if (acceptableInterpolantRate < 0.0) {
      throw new InvalidConfigurationException(
          "Acceptable number of interpolants per refinement used in"
              + " ProgressBasedRefinementSelectionHeuristicInterpolationRate must not be negative");
    }
    if (increaseFactorInterpolants < 0.0) {
      throw new InvalidConfigurationException(
          "Increase factor used in ProgressBasedRefinementSelectionHeuristicInterpolationRate must"
              + " not be negative");
    }
    if (decreaseFactorInterpolants <= 0.0) {
      throw new InvalidConfigurationException(
          "Decrease factor used in ProgressBasedRefinementSelectionHeuristicInterpolationRate must"
              + " be strictly positive");
    }
    if (abstractionLocationRefinementRatioUpper < 0
        || abstractionLocationRefinementRatioLower < 0) {
      throw new InvalidConfigurationException(
          "The bounds for the number of abstraction locations to refinement iterations used in"
              + " ProgressBasedRefinementSelectionHeuristicInterpolationRate must not be negative");
    }
    if (abstractionLocationRefinementRatioLower >= abstractionLocationRefinementRatioUpper) {
      throw new InvalidConfigurationException(
          "The lower bound for the number of abstraction locations to refinement iterations used in"
              + " ProgressBasedRefinementSelectionHeuristicInterpolationRate must be strictly"
              + " smaller than the upper bound");
    }
    if (rateWindowSize <= 0) {
      throw new InvalidConfigurationException(
          "Rate window size used in ProgressBasedRefinementSelectionHeuristicInterpolationRate must"
              + " be strictly positive");
    }
    if (persistentRateThreshold < 0.0) {
      throw new InvalidConfigurationException(
          "Persistent rate threshold used in"
              + " ProgressBasedRefinementSelectionHeuristicInterpolationRate must not be negative");
    }
    if (maxPersistentIterations <= 0) {
      throw new InvalidConfigurationException(
          "Maximum persistent iterations used in"
              + " ProgressBasedRefinementSelectionHeuristicInterpolationRate must be strictly"
              + " positive");
    }
  }

  /**
   * Evaluates if the recent net-active interpolant growth rate is within acceptable limits and has
   * not persistently exceeded the threshold.
   *
   * @param pReached unused
   * @param pDeltas sequence of recently observed deltas, bounded in size by the CEGAR algorithm;
   *     only the latest delta is used to update this heuristic's internal state
   * @return {@code true} if the interpolant rate is acceptable, {@code false} if divergence is
   *     detected
   */
  @Override
  public boolean fulfilled(ReachedSet pReached, ImmutableList<ReachedSetDelta> pDeltas) {
    if (pDeltas.isEmpty()) {
      return false;
    }

    numberRefinements++;
    processLatestDelta(pDeltas.getLast());

    currentAbstractionLocationRefinementRatio =
        (double) totalAbstractionLocationCount / numberRefinements;
    currentTotalInterpolantRate = computeRecentInterpolantRate();

    updatePersistentRateStreak();

    if (consecutivePersistentIterations > maxPersistentIterations) {
      logger.logf(
          Level.FINER,
          "Net-active interpolant rate of change has remained at or above %.2f for %d consecutive"
              + " refinements, indicating sustained unbounded predicate growth. Heuristic %s is no"
              + " longer applicable.",
          persistentRateThreshold,
          consecutivePersistentIterations,
          getClass().getSimpleName());
      return false;
    }

    double effectiveAcceptableInterpolantRate =
        computeEffectiveAcceptableInterpolantRate(determineRatioRegime());
    boolean acceptable = currentTotalInterpolantRate <= effectiveAcceptableInterpolantRate;
    logDecision(acceptable);
    return acceptable;
  }

  /**
   * Updates the interpolant and abstraction-location counts based on the given delta.
   *
   * @param pLatestDelta the most recently observed delta
   */
  private void processLatestDelta(ReachedSetDelta pLatestDelta) {
    // Increment count for newly added states
    for (AbstractState pState : pLatestDelta.addedStates()) {
      PredicateAbstractState predState =
          AbstractStates.extractStateByType(pState, PredicateAbstractState.class);

      if (predState != null && predState.isAbstractionState()) {
        totalAbstractionLocationCount++;
      }

      if (isNonTrivialAbstractionState(predState)) {
        totalInterpolantCount++;
      }
    }

    // Decrement counts for removed states to track a "true" net state
    for (AbstractState pState : pLatestDelta.removedStates()) {
      PredicateAbstractState predState =
          AbstractStates.extractStateByType(pState, PredicateAbstractState.class);

      if (predState != null && predState.isAbstractionState()) {
        totalAbstractionLocationCount--;
      }

      // Only decrement the interpolant count if it was a non-trivial one
      if (isNonTrivialAbstractionState(predState)) {
        totalInterpolantCount--;
      }
    }

    recentInterpolantCounts.add(totalInterpolantCount);
  }

  /**
   * Determines how the current abstraction-location-to-refinement ratio compares to the configured
   * bounds.
   *
   * @return the applicable {@link RatioRegime}
   */
  private RatioRegime determineRatioRegime() {
    if (currentAbstractionLocationRefinementRatio < abstractionLocationRefinementRatioLower) {
      return RatioRegime.TOO_LOW;
    }
    if (currentAbstractionLocationRefinementRatio > abstractionLocationRefinementRatioUpper) {
      return RatioRegime.TOO_HIGH;
    }
    return RatioRegime.NORMAL;
  }

  /**
   * Computes the acceptable interpolant rate, adjusted according to the given ratio regime.
   *
   * @param pRegime the current abstraction-location-to-refinement ratio regime
   * @return the effective acceptable interpolant rate
   */
  private double computeEffectiveAcceptableInterpolantRate(RatioRegime pRegime) {
    return switch (pRegime) {
      // ratio too low: run usually needs a higher threshold to prevent premature termination
      case TOO_LOW -> acceptableInterpolantRate * increaseFactorInterpolants;
      // ratio too high: threshold should be decreased to prevent long runs resulting in timeouts
      case TOO_HIGH -> acceptableInterpolantRate / decreaseFactorInterpolants;
      // ratio within bounds: use the configured acceptable interpolant rate as-is
      case NORMAL -> acceptableInterpolantRate;
    };
  }

  /**
   * Logs the outcome of the current evaluation at {@link Level#FINER}.
   *
   * @param pAcceptable whether the current interpolant rate was found acceptable
   */
  private void logDecision(boolean pAcceptable) {
    if (pAcceptable) {
      logger.logf(
          Level.FINER,
          "Current net-active interpolant rate of change: %.2f and ratio of abstraction location"
              + " to refinements is: %.2f.",
          currentTotalInterpolantRate,
          currentAbstractionLocationRefinementRatio);
    } else {
      logger.logf(
          Level.FINER,
          "Current net-active interpolant rate of change is too high: %.2f. Ratio of abstraction"
              + " location to refinements is: %.2f. Heuristic %s is no longer applicable.",
          currentTotalInterpolantRate,
          currentAbstractionLocationRefinementRatio,
          getClass().getSimpleName());
    }
  }

  /**
   * Computes the rate of change of the net-active interpolant count across the sliding window.
   * Returns {@code 0.0} if fewer than two samples are available.
   *
   * @return the recent rate of change
   */
  private double computeRecentInterpolantRate() {
    if (recentInterpolantCounts.size() < 2) {
      return 0.0;
    }

    int oldest = Iterables.getFirst(recentInterpolantCounts, 0);
    int newest = Iterables.getLast(recentInterpolantCounts);
    int windowSpan = recentInterpolantCounts.size() - 1;

    return (double) (newest - oldest) / (double) windowSpan;
  }

  /**
   * Updates the counter for consecutive iterations where the interpolant rate meets or exceeds
   * {@link #persistentRateThreshold}, resetting to {@code 0} when it drops below.
   */
  private void updatePersistentRateStreak() {
    if (currentTotalInterpolantRate >= persistentRateThreshold) {
      consecutivePersistentIterations++;
    } else {
      consecutivePersistentIterations = 0;
    }
  }

  /**
   * Determines if the given state has a non-trivial abstraction formula (neither trivially {@code
   * true} nor {@code false}).
   *
   * @param pPredState the state to check (may be {@code null})
   * @return {@code true} if the state contains a meaningful interpolant
   */
  private boolean isNonTrivialAbstractionState(PredicateAbstractState pPredState) {
    if (pPredState == null) {
      return false;
    }

    if (!pPredState.isAbstractionState()) {
      return false;
    }

    if (formulaManager
        .getBooleanFormulaManager()
        .isTrue(pPredState.getAbstractionFormula().asFormula())) {
      return false;
    }

    if (formulaManager
        .getBooleanFormulaManager()
        .isFalse(pPredState.getAbstractionFormula().asFormula())) {
      return false;
    }

    return true;
  }
}
