// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
//
// This automaton contains the specification of the __VERIFIER_postpone() function used for user guided state space exploration.
// For calls to the __VERIFIER_postpone() functions it goes to Postpone states.

CONTROL AUTOMATON UserGuidedPostpone

LOCAL int traverse = 1;

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH {__VERIFIER_postpone($?)} -> DO traverse = 0 GOTO Postpone;

STATE USEFIRST Postpone :
  TRUE -> GOTO Postpone;

END AUTOMATON
