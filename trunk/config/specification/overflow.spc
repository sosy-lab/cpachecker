// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// This automaton contains the specification of the
// category Overflows of the
// Competition on Software Verification.
CONTROL AUTOMATON Overflows

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK("overflow") -> ERROR("no-overflow: integer overflow in $location");

END AUTOMATON
