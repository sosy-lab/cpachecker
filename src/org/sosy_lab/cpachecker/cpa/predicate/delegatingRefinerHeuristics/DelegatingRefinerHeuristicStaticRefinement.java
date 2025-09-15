// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetDelta;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

/** A simple heuristic that always chooses a static refinement strategy in the first iteration. */
public class DelegatingRefinerHeuristicStaticRefinement implements DelegatingRefinerHeuristic {

  private boolean staticRefinerUsed = false;

  @Override
  public boolean fulfilled(
      UnmodifiableReachedSet pReached, ImmutableList<ReachedSetDelta> pDeltas) {

    if (!staticRefinerUsed) {
      staticRefinerUsed = true;
      return true;
    }
    return false;
  }
}
