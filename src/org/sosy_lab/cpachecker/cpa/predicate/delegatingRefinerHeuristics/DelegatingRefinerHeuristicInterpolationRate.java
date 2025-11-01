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
import java.util.HashSet;
import java.util.Set;
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
import org.sosy_lab.java_smt.api.BooleanFormula;

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

  private double currentTotalInterpolantRate;

  @Option(
      secure = true,
      name = "acceptableInterpolantRate",
      description =
          "Acceptable number of interpolants generated per refinement for"
              + " PredicateDelegatingRefiner heuristic.")
  private double acceptableInterpolantRate = 8.0;

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

    int numberOfRefinements = pDeltas.size();
    Set<BooleanFormula> numberInterpolants = new HashSet<>();

    if (numberOfRefinements > 0) {
      for (ReachedSetDelta delta : pDeltas) {
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
              numberInterpolants.add(predState.getAbstractionFormula().asFormula());
            }
          }
        }
      }

      currentTotalInterpolantRate =
          (double) numberInterpolants.size() / (double) numberOfRefinements;

      if (currentTotalInterpolantRate < acceptableInterpolantRate) {
        logger.logf(
            Level.FINER,
            "Checking current rate of interpolants generated per refinement: %.2f.",
            currentTotalInterpolantRate);
      } else {
        logger.logf(
            Level.FINE,
            "Number of interpolants per refinement is too high:  %.2f. Heuristic %s is no longer"
                + " applicable.",
            currentTotalInterpolantRate,
            this.getClass().getSimpleName());
      }
    }

    return currentTotalInterpolantRate < acceptableInterpolantRate;
  }
}
