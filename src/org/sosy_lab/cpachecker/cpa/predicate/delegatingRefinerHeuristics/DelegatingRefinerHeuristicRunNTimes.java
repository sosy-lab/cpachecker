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
 * A heuristic which lets a refiner do a fixed number of iterations so that enough data is collected
 * in order to judge refinement progress with other heuristics.
 */
public class DelegatingRefinerHeuristicRunNTimes implements DelegatingRefinerHeuristic {

  private final int fixedRuns;
  private int currentRuns = 0;

  /**
   * Construct a fixed-run heuristic.
   *
   * @param pFixedRuns number of refinement iterations to allow for collecting data
   * @throws InvalidConfigurationException if the provided number of runs is negative
   */
  public DelegatingRefinerHeuristicRunNTimes(int pFixedRuns) throws InvalidConfigurationException {
    if (pFixedRuns < 0) {
      throw new InvalidConfigurationException(
          "Number of runs for the refiner must not be negative.");
    }
    this.fixedRuns = pFixedRuns;
  }

  /**
   * Evaluates whether enough refinement runs have been done to collect data, based on an internal
   * counter and a configured run count.
   *
   * @param pReached the current ReachedSet (not used directly)
   * @param pDeltas the list of changes in the ReachedSet (not used directly)
   * @return {@code true}, if the run count has not been reached, {@code false} otherwise
   */
  @Override
  public boolean fulfilled(ReachedSet pReached, ImmutableList<ReachedSetDelta> pDeltas) {
    if (currentRuns < fixedRuns) {
      currentRuns++;
      return true;
    }
    return false;
  }

  /**
   * Returns the number of refinement iterations to allow. Used for testing.
   *
   * @return the number of refinement iterations configured
   */
  public int getFixedRuns() {
    return fixedRuns;
  }
}
