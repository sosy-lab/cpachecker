// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// This automaton checks for any possible null-pointer dereference in the source code.
// To be able to use this, the following option needs to be set:
// cfa.checkNullPointers = true
CONTROL AUTOMATON NULLDEREF

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH "null-deref" -> ERROR;

END AUTOMATON

// Recognize functions such as exit() and abort() which do not return.
#include TerminatingFunctions.spc
