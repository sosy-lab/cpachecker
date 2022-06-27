// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

CONTROL AUTOMATON AssumptionGuidingAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(AutomatonAnalysis_AssumptionAutomaton, "stateType == NON_TARGET") -> STOP;
  //Ensures that if there are path leading to an node in P_unknwon, only these states are considered
  //IF p_unknown = \emptyset, the distnace is set to 0 for all states
  CHECK(AutomatonAnalysis_AssumptionAutomaton, "__DISTANCE_TO_UNKNOWN > 0") -> STOP;
  CHECK(AutomatonAnalysis_AssumptionAutomaton, "stateType == TARGET") -> ERROR("unreach-call");
  CHECK(AutomatonAnalysis_AssumptionAutomaton, "stateType == UNKNOWN") -> ERROR("unreach-call");
  TRUE -> GOTO Init;

END AUTOMATON
