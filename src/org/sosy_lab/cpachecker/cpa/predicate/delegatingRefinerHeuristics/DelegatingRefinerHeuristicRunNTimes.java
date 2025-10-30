// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetDelta;

/**
 * A heuristic which lets a refiner do several number of iterations without assessing the refinement
 * progress. It is used so that easier to verify programs can be verified without the overhead of
 * diagnostic heuristics and, at the same time, data can be collected in order to evaluate
 * refinement progress with subsequent diagnostic heuristics.
 */
public class DelegatingRefinerHeuristicRunNTimes implements DelegatingRefinerHeuristic {

  // in converging runs, the ratio of reached set to number of refinement doesn't typically exceed
  // 20 - if it does, this heuristic should stop in favour of diagnostic heuristics
  private final int defaultReachedSetRefinementRatioExceeded;
  private static final int REFINEMENT_NUMBERS_HIGH = 40;
  private static final int REACHED_SET_SIZE_HIGH = 1000;

  /**
   * Constructs a heuristic that runs as long as the ratio of the size of the reached set with
   * respect to the number of refinements does not exceed a configured threshold. If this limit is
   * reached, the heuristic returns {@code false} in favour of diagnostic heuristics.
   *
   * @param pDefaultReachedSetRefinementRatioExceeded ratio of the size of reached set with respect
   *     to number of refinement iterations that decides how long the refiner should run
   * @throws InvalidConfigurationException if the provided ratio is negative
   */
  public DelegatingRefinerHeuristicRunNTimes(int pDefaultReachedSetRefinementRatioExceeded)
      throws InvalidConfigurationException {
    if (pDefaultReachedSetRefinementRatioExceeded < 0) {
      throw new InvalidConfigurationException(
          "the ratio of reached set size to refinement iterations must not be negative");
    }
    this.defaultReachedSetRefinementRatioExceeded = pDefaultReachedSetRefinementRatioExceeded;
  }

  /**
   * Evaluates whether enough refinement runs have been done to collect data, based on ratio of the
   * size of the reached set with respect to the number of refinements already executed.
   *
   * @param pReached the current ReachedSet, used to compute the current ratio of reached set size
   *     to refinement iterations
   * @param pDeltas the list of changes in the ReachedSet, its size represents the number of
   *     refinements
   * @return {@code true} as long as the current ratio stays below the configured threshold, {@code
   *     false} otherwise
   */
  @Override
  public boolean fulfilled(ReachedSet pReached, ImmutableList<ReachedSetDelta> pDeltas) {

    if (pReached.size() > REACHED_SET_SIZE_HIGH && pDeltas.size() > REFINEMENT_NUMBERS_HIGH) {
      int currentReachedSetRefinementRatio = pReached.size() / pDeltas.size();

      return currentReachedSetRefinementRatio < defaultReachedSetRefinementRatioExceeded;
    }

    return true;
  }

  /**
   * Size of the configured ratio of reached set size to the number of refinements. Used for
   * testing.
   *
   * @return the reached set size to the number of refinements configured
   */
  public int getReachedSetRefinementRatio() {
    return defaultReachedSetRefinementRatioExceeded;
  }
}
