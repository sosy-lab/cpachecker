// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

CONTROL AUTOMATON taintAnalysis

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK("taintViolation") -> ERROR("taint: tainted variable in $location");

END AUTOMATON
