// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// This automaton contains the specification for
// LDV driver verification framework.
// It checks only for labels named "LDV_ERROR".

CONTROL AUTOMATON LDVErrorLabel

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH LABEL "LDV_ERROR" -> ERROR;
END AUTOMATON
