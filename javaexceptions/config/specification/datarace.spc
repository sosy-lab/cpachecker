// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// This automaton contains the specification of the
// category NoDataRace of the
// Competition on Software Verification.
CONTROL AUTOMATON DataRace

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK("data-race") -> ERROR("no-data-race: data race in $location");

END AUTOMATON
