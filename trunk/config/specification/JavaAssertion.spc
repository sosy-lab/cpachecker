// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

OBSERVER AUTOMATON AssertionAutomaton
// This automaton detects assertions that may fail.

INITIAL STATE Init;

STATE USEFIRST Init :
   // matches special edge added by CPAchecker
   MATCH ASSERT -> ERROR("assertion in $location");

END AUTOMATON
