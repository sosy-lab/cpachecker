// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition;


import static org.sosy_lab.cpachecker.core.algorithm.composition.AlgSelectionBooleanVector.init;

import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.util.Pair;

public class TimeLimitSelection {

  private static final Map<AlgSelectionBooleanVector, Pair<Integer, Integer>> CONTEXT_TIMELIMIT =
      new HashMap<>();

  static {
    CONTEXT_TIMELIMIT.put(init(0, 0, 1, 0, 1, 0), Pair.of(250, 250));
    CONTEXT_TIMELIMIT.put(init(0, 1, 0, 0, 1, 0), Pair.of(250, 250));
    CONTEXT_TIMELIMIT.put(init(0, 1, 0, 0, 1, 1), Pair.of(250, 250));

    CONTEXT_TIMELIMIT.put(init(0, 0, 0, 0, 1, 0), Pair.of(50, 50));
    CONTEXT_TIMELIMIT.put(init(0, 0, 0, 1, 1, 0), Pair.of(50, 50));
    CONTEXT_TIMELIMIT.put(init(0, 0, 0, 1, 1, 1), Pair.of(50, 50));
    CONTEXT_TIMELIMIT.put(init(1, 0, 1, 0, 1, 1), Pair.of(50, 50));

    CONTEXT_TIMELIMIT.put(init(0, 1, 0, 1, 0, 0), Pair.of(10, 10));
    CONTEXT_TIMELIMIT.put(init(0, 1, 0, 1, 1, 1), Pair.of(10, 10));
    CONTEXT_TIMELIMIT.put(init(1, 0, 0, 0, 1, 1), Pair.of(10, 10));
    CONTEXT_TIMELIMIT.put(init(1, 0, 1, 0, 1, 0), Pair.of(10, 10));
  }

  public static Pair<Integer, Integer> getTimeLimits(final AlgSelectionBooleanVector selectionContext) {
    return CONTEXT_TIMELIMIT.getOrDefault(selectionContext, Pair.of(20, 80));
  }
}
