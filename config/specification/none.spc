// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// An automaton which does nothing. Use for code-to-code translations where the correctness of the
// original code is not checked.
CONTROL AUTOMATON None

INITIAL STATE Init;

STATE USEFIRST Init:
  FALSE -> BREAK;

END AUTOMATON
