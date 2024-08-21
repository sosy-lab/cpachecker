// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

OBSERVER AUTOMATON AssertionAutomaton
// This automaton detects assertions that may fail
// (i.e., a function call to __assert_fail).

INITIAL STATE Init;

STATE USEFIRST Init :
   // Match standard calls to __assert_fail with nice message on violations.
   MATCH {__assert_fail($1, $2, $3, $4)}
    -> ERROR("assertion in $location: Condition $1 failed in $2, line $3");

   // Match if assert_fail or assert_func is called with any number of parameters.
   MATCH {__assert_fail($?)} || MATCH {__assert_func($?)}
   -> ERROR("assertion in $location");

   // Print warnings for other common error functions to warn users about potentially wrong specification.

   MATCH {assert($?)} && !CHECK(location, "functionName==assert")
   -> PRINT "WARNING: Function assert() without body detected. Please run the C preprocessor on this file to enable assertion checking."
      GOTO Init;

   MATCH {__VERIFIER_error($?)} && !CHECK(location, "functionName==__VERIFIER_error")
   -> PRINT "WARNING: Function __VERIFIER_error() is ignored by this specification. If you want to check for reachability of __VERIFIER_error, pass '--spec sv-comp-reachability' as parameter."
      GOTO Init;

  MATCH {reach_error($?)}
   -> PRINT "WARNING: Function reach_error() is ignored by this specification. If you want to check for reachability of reach_error, pass '--spec sv-comp-reachability' as parameter."
      GOTO Init;

END AUTOMATON
