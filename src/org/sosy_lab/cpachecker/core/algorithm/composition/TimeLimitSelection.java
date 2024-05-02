// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition;


import org.sosy_lab.cpachecker.util.Pair;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.sosy_lab.cpachecker.core.algorithm.composition.AlgSelectionBooleanVector.init;

public class TimeLimitSelection {
    private static final Map<String, String> zeitlimitMap = new ConcurrentHashMap<>() {{
        put("(0, 0, 1, 0, 1, 0)", "250_250");
        put("(0, 1, 0, 0, 1, 0)", "250_250");
        put("(0, 1, 0, 0, 1, 1)", "250_250");

        put("(0, 0, 0, 0, 1, 0)", "50_50");
        put("(0, 0, 0, 1, 1, 0)", "50_50");
        put("(0, 0, 0, 1, 1, 1)", "50_50");
        put("(1, 0, 1, 0, 1, 1)", "50_50");

        put("(0, 1, 0, 1, 0, 0)", "10_10");
        put("(0, 1, 0, 1, 1, 1)", "10_10");
        put("(1, 0, 0, 0, 1, 1)", "10_10");
        put("(1, 0, 1, 0, 1, 0)", "10_10");
    }};

    private static final Map<AlgSelectionBooleanVector, Pair<Integer, Integer>> CONTEXT_TIMELIMIT = new ConcurrentHashMap<>() {{
        put(init(0, 0, 1, 0, 1, 0), Pair.of(250, 250));
        put(init(0, 1, 0, 0, 1, 0), Pair.of(250, 250));
        put(init(0, 1, 0, 0, 1, 1), Pair.of(250, 250));
        //TODO
        put(init(0, 0, 0, 0, 1, 0), Pair.of(50, 50));

    }};

    public static Pair<Integer, Integer> getTimeLimits(final AlgSelectionBooleanVector selectionContext) {
        return CONTEXT_TIMELIMIT.getOrDefault(selectionContext, Pair.of(20, 80));
    }
}
