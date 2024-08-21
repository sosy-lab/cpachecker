// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// This automaton waits for the end of the program and prints the last statement.
// It is used to test the function of the MATCH EXIT keywords.


OBSERVER AUTOMATON PrintLastStatementAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH EXIT -> PRINT "Last statement is \"$rawstatement\"" GOTO Init;

END AUTOMATON
