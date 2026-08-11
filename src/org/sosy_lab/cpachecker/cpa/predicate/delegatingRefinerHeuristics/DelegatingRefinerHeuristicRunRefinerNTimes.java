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
import org.sosy_lab.cpachecker.core.reachedset.TrackingForwardingReachedSet.ReachedSetDelta;

/**
 * A simple heuristic that runs a configurable number of times. To mirror the default predicate
 * abstraction functionality, this heuristic should be set with N = 1 and used as the first
 * heuristic in the PredicateDelegatingRefiner, paired with a PredicateStaticRefiner.
 */
@Options(prefix = "cpa.predicate.delegatingRefinerHeuristics.RunRefinerNTimes")
public class DelegatingRefinerHeuristicRunRefinerNTimes implements DelegatingRefinerHeuristic {

  private final LogManager logger;
  private int currentCount = 0;

  @Option(
      secure = true,
      description = "Number of times the RunRefinerNTimes heuristic is allowed to run.")
  private int numberRuns = 1;

  public DelegatingRefinerHeuristicRunRefinerNTimes(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    if (numberRuns < 0) {
      throw new InvalidConfigurationException(
          "Number of times DelegatingRefinerHeuristicRunRefinerNTimes should run must not be"
              + " negative.");
    }
    this.logger = pLogger;
  }

  /**
   * Evaluates if the heuristic has already run the configured number of times.
   *
   * @param pReached the current ReachedSet (not used directly)
   * @param pDeltas the list of changes in the ReachedSet (not used directly)
   * @return {@code true}, if the heuristic has not yet run the configured number of times, {@code
   *     false} otherwise
   */
  @Override
  public boolean fulfilled(ReachedSet pReached, ImmutableList<ReachedSetDelta> pDeltas) {
    if (currentCount < numberRuns) {
      currentCount++;
      logger.logf(
          Level.FINE,
          "DelegatingRefinerHeuristicRunRefinerNTimes has run %d times out of %d configured.",
          currentCount,
          numberRuns);
      return true;
    }
    return false;
  }
}
