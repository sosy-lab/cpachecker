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
  CHECK(AutomatonAnalysis_AssumptionAutomaton, "state == __TRUE") -> STOP;
  TRUE -> GOTO Init;

END AUTOMATON
