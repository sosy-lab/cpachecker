// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// It checks only for calls to the __VERIFIER_error()/reach_error() functions
// and also implements some functions which usually lead to a program abort.
// It supports detecting multiple violations within one run (USEALL)
CONTROL AUTOMATON MultiErrors

INITIAL STATE Init;

STATE USEALL Init :
  MATCH {__VERIFIER_error($?)} || MATCH {reach_error($?)} || MATCH FUNCTIONCALL "reach_error" -> ERROR("$rawstatement called in line $line");
  MATCH {__assert_fail($?)} || MATCH {abort($?)} || MATCH {exit($?)} -> STOP;
  TRUE -> GOTO Init;

END AUTOMATON
