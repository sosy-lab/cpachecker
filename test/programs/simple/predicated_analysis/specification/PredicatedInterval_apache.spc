// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

// require bitprecise analysis here
STATE USEFIRST Init :
  !CHECK(ValidVars,"flag") || CHECK(IntervalAnalysis,"0<=flag<=3") -> GOTO Init;
  TRUE -> ASSUME {flag>3 | flag<0} ERROR;
  
END AUTOMATON
