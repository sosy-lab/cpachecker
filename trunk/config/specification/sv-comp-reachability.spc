// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// This automaton contains the specification of the
// Competition on Software Verification.
// It checks only for calls to the __VERIFIER_error()/reach_error() functions
// and also implements some functions which usually lead to a program abort.
CONTROL AUTOMATON SVCOMP

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH {__VERIFIER_error($?)} || MATCH {reach_error($?)} || MATCH FUNCTIONCALL "reach_error"
      -> ERROR("unreach-call: $rawstatement called in $location");
  MATCH {__assert_fail($?)} || MATCH {abort($?)} || MATCH {exit($?)} -> STOP;

END AUTOMATON
