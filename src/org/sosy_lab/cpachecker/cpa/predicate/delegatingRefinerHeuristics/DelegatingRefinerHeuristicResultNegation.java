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
 * This class implements a DelegatingRefinerHeuristicResultNegation. This is a heuristic that wraps
 * any {@link DelegatingRefinerHeuristic} and negates its result. This way, every heuristic can be
 * used to either signal that a refinement should stop or that it should continue.
 */
public class DelegatingRefinerHeuristicResultNegation implements DelegatingRefinerHeuristic {

  private final DelegatingRefinerHeuristic delegateHeuristic;

  /**
   * Constructs a DelegatingRefinerHeuristicResultNegation that negates the result of the given
   * delegate heuristic.
   *
   * @param pDelegateHeuristic the heuristic whose result to negate
   */
  public DelegatingRefinerHeuristicResultNegation(DelegatingRefinerHeuristic pDelegateHeuristic)
      throws InvalidConfigurationException {
    if (pDelegateHeuristic == null) {
      throw new InvalidConfigurationException(
          "DelegatingRefinerHeuristic to be negated cannot be null");
    }
    this.delegateHeuristic = pDelegateHeuristic;
  }

  /**
   * Evaluates the result of the delegate heuristic and returns its negated result.
   *
   * @param pReached the current ReachedSet, used as input parameter for the delegate heuristic
   * @param pDeltas the list of changes in the ReachedSet, used as input parameter for the delegate
   *     heuristic
   * @return the negation of the delegate heuristic's result
   */
  @Override
  public boolean fulfilled(ReachedSet pReached, ImmutableList<ReachedSetDelta> pDeltas) {
    boolean delegateHeuristicResult = delegateHeuristic.fulfilled(pReached, pDeltas);
    return !delegateHeuristicResult;
  }

  /**
   * Returns the delegate heuristic to be negated. Used for testing.
   *
   * @return the wrapped heuristic
   */
  public DelegatingRefinerHeuristic getDelegateHeuristic() {
    return delegateHeuristic;
  }
}
