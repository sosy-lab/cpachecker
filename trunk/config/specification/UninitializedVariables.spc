// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

OBSERVER AUTOMATON UninitializedVariablesObservingAutomaton
/* Queries the UninitializedVariablesCPA for errors and prints them.
 * Does abort if an error is found.
 */

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(uninitVars,"UNINITIALIZED_RETURN_VALUE") ->  ERROR;
  CHECK(uninitVars,"UNINITIALIZED_VARIABLE_USED") -> ERROR;

END AUTOMATON
