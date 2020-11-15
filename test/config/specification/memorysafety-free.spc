// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// This automaton contains the specification of the category MemorySafety
// of the Competition on Software Verification.
// It queries the SMGCPA for information about invalid freeing of allocations.

CONTROL AUTOMATON SMGCPAFREE

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(SMGCPA, "has-invalid-frees") -> ERROR("valid-free: invalid pointer free in $location");

END AUTOMATON
