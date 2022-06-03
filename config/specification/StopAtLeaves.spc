// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

CONTROL AUTOMATON StopAtLeavesAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(StopAtLeavesState, "at leaf") -> ERROR;
  TRUE -> GOTO Init;

END AUTOMATON
