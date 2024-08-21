// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

CONTROL AUTOMATON BadnessAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(ModificationsPropCPA, "is_bad") -> ERROR("Bad state found in $location");

END AUTOMATON
