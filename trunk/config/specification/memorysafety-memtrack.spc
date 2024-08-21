// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// This automaton contains the specification of the category MemorySafety
// of the Competition on Software Verification.
// It queries the SMGCPA for information about memory leaks,
// i.e., forgotten allocations after leaving a scope.

CONTROL AUTOMATON SMGCPAMEMTRACK

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(SMGCPA, "has-leaks") -> ERROR("valid-memtrack");

END AUTOMATON
