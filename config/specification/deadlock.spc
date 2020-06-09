// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// This automaton contains a specification to check for deadlocks in concurrent programs.
CONTROL AUTOMATON Deadlock

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(ThreadingCPA, "deadlock") -> ERROR("no-deadlock: deadlock detected in $location");

END AUTOMATON
