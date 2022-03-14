// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;

public class Automata {

  private static final Automaton LOOP_HEAD_TARGET_AUTOMATON;

  static {
    String initStateName = "Init";
    AutomatonTransition toInit =
        new AutomatonTransition.Builder(AutomatonBoolExpr.TRUE, initStateName).build();

    AutomatonInternalState targetState = AutomatonInternalState.ERROR;
    AutomatonTransition toTarget =
        new AutomatonTransition.Builder(AutomatonBoolExpr.MatchLoopStart.INSTANCE, targetState)
            .build();

    AutomatonInternalState initState =
        new AutomatonInternalState(
            initStateName, Lists.newArrayList(toInit, toTarget), false, true);

    List<AutomatonInternalState> states = Lists.newArrayList(initState, targetState);

    try {
      LOOP_HEAD_TARGET_AUTOMATON =
          new Automaton("LoopHeadTarget", ImmutableMap.of(), states, initStateName);
    } catch (InvalidAutomatonException e) {
      throw new AssertionError("Automaton built in code should be valid.");
    }
  }

  public static Automaton getLoopHeadTargetAutomaton() {
    return LOOP_HEAD_TARGET_AUTOMATON;
  }

  private Automata() {}
}
