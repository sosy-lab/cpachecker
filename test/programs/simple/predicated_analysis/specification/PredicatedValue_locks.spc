// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

OBSERVER AUTOMATON AlwaysValueAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  !CHECK(ValidVars,"main::flag") || CHECK(ValueAnalysis,"main::flag==1") -> GOTO Init;
  TRUE -> ASSUME {flag!=1} ERROR;
  
END AUTOMATON
