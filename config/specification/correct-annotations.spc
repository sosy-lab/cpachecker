// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// all annotations in a SV-LIB program are valid
CONTROL AUTOMATON CorrectAnnotations

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK("correct-annotations") -> ERROR("specification violation of program in $location");

END AUTOMATON
