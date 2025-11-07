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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetDelta;

/**
 * A heuristic which lets a refiner do several number of iterations without assessing the refinement
 * progress. It is used so that easier to verify programs can be verified without the overhead of
 * diagnostic heuristics and, at the same time, data can be collected in order to evaluate
 * refinement progress with subsequent diagnostic heuristics.
 */
@Options(prefix = "cpa.predicate.delegatingRefinerHeuristics.ReachedSetRatio")
public class DelegatingRefinerHeuristicReachedSetRatio implements DelegatingRefinerHeuristic {
  private final LogManager logger;
  private double currentAbstractionLocationRefinementRatio;

  @Option(
      secure = true,
      name = "abstractionLocationRefinementRatio",
      description =
          "The maximum acceptable ratio of the number of abstraction locations to the number of"
              + " refinements before this heuristic should stop in favor of other diagnostic"
              + " heuristics.")
  private double abstractionLocationRefinementRatio = 3.0;

  @Option(
      secure = true,
      name = "refinementThreshold",
      description =
          "The maximum number of refinement iterations allowed before this heuristic should stop in"
              + " favor of other diagnostic heuristics.")
  private int refinementThreshold = 10;

  @Option(
      secure = true,
      name = "abstractionLocationThreshold",
      description =
          "The maximum number of abstraction locations discovered before this heuristic should stop"
              + " in favor of other diagnostic heuristics.")
  private int abstractionLocationThreshold = 3;

  /**
   * Creates a heuristic that runs as long as the ratio of the number of discovered abstraction
   * locations to the number of refinements or the number of refinement iterations does not exceed a
   * configured threshold. Returns {@code true} as long as the ratio of abstraction locations to
   * refinements remains below the configured threshold; returns {@code false} otherwise.
   *
   * @param pConfig configuration used to inject the configurable thresholds
   * @throws InvalidConfigurationException if the provided ratio is negative
   */
  public DelegatingRefinerHeuristicReachedSetRatio(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    if (abstractionLocationRefinementRatio < 0) {
      throw new InvalidConfigurationException(
          "The ratio of the number of abstraction locations to refinement iterations used in"
              + " DelegatingRefinerHeuristicReachedSetRatio must not be negative");
    }
    if (refinementThreshold < 0) {
      throw new InvalidConfigurationException(
          "The maximum number of refinement iterations allowed in"
              + " DelegatingRefinerHeuristicReachedSetRatio must not be negative");
    }
    if (abstractionLocationThreshold < 0) {
      throw new InvalidConfigurationException(
          "The maximum number of abstraction locations discovered in"
              + " DelegatingRefinerHeuristicReachedSetRatio must not be negative");
    }

    this.logger = pLogger;
    currentAbstractionLocationRefinementRatio = 0.0;
  }

  /**
   * Evaluates whether enough refinement runs have been done to collect data, based on the amount of
   * abstraction locations and already executed refinements iterations.
   *
   * @param pReached the current ReachedSet, used to compute the number of abstraction locations
   *     already discovered
   * @param pDeltas the list of changes in the ReachedSet, its size represents the already executed
   *     refinements iterations
   * @return {@code true} as long as the observed metrics stay below the configured thresholds,
   *     {@code false} otherwise
   */
  @Override
  public boolean fulfilled(ReachedSet pReached, ImmutableList<ReachedSetDelta> pDeltas) {
    int numberRefinements = pDeltas.size();
    if (numberRefinements == 0) {
      return false;
    }

    // Get the current number of abstraction locations from the delta
    int currentAbstractionLocationCount =
        pDeltas.stream().mapToInt(ReachedSetDelta::abstractionLocationsCount).sum();

    if (currentAbstractionLocationCount < abstractionLocationThreshold) {
      return true;
    }

    currentAbstractionLocationRefinementRatio =
        (double) currentAbstractionLocationCount / numberRefinements;

    if (currentAbstractionLocationRefinementRatio > abstractionLocationRefinementRatio) {
      logger.logf(
          Level.INFO,
          "ratio of abstraction location to refinements is too high: %.2f for threshold %.2f."
              + " Heuristic %s is no longer applicable.",
          currentAbstractionLocationRefinementRatio,
          abstractionLocationRefinementRatio,
          this.getClass().getSimpleName());
    }

    if (numberRefinements > refinementThreshold) {
      logger.logf(
          Level.INFO,
          "number refinements is too high: %d for threshold %d."
              + " Heuristic %s is no longer applicable.",
          numberRefinements,
          refinementThreshold,
          this.getClass().getSimpleName());
    }

    return (currentAbstractionLocationRefinementRatio < abstractionLocationRefinementRatio
        && numberRefinements < refinementThreshold);
  }

  /**
   * Size of the configured ratio of reached set size to the number of refinements. Used for
   * testing.
   *
   * @return the reached set size to the number of refinements configured
   */
  public double getAbstractionLocationRefinementRatio() {
    return abstractionLocationRefinementRatio;
  }
}
