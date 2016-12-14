/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;


public class Automata {

  private static final Automaton LOOP_HEAD_TARGET_AUTOMATON;

  static {
    String initStateName = "Init";
    AutomatonTransition toInit = new AutomatonTransition(
        AutomatonBoolExpr.TRUE,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        initStateName);

    AutomatonInternalState targetState = AutomatonInternalState.ERROR;
    AutomatonTransition toTarget = new AutomatonTransition(
        AutomatonBoolExpr.MatchLoopStart.INSTANCE,
        Collections.emptyList(),
        Collections.emptyList(),
        targetState);

    AutomatonInternalState initState = new AutomatonInternalState(initStateName, Lists.newArrayList(toInit, toTarget), false, true);

    List<AutomatonInternalState> states = Lists.newArrayList(initState, targetState);

    try {
      LOOP_HEAD_TARGET_AUTOMATON = new Automaton(
          "LoopHeadTarget",
          Collections.emptyMap(),
          states,
          initStateName);
    } catch (InvalidAutomatonException e) {
      throw new AssertionError("Automaton built in code should be valid.");
    }
  }

  public static Automaton getLoopHeadTargetAutomaton() {
    return LOOP_HEAD_TARGET_AUTOMATON;
  }

  private Automata() {

  }

}
