// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// An automaton which tells CPAchecker about several statements after which execution ends.
// Note: This automaton is specifically for termination analysis.
// The automaton in TerminatingFunctions.spc is similar and for reachability analyses.
CONTROL AUTOMATON TerminatingFunctions

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH {abort($?)} || MATCH {exit($?)} || MATCH {__assert_fail($?)} || MATCH {__VERIFIER_error($?)} -> BREAK;

END AUTOMATON
