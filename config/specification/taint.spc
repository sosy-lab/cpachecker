// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

CONTROL AUTOMATON TAINTSTATUS

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(TaintAnalysisCPA, "tainted-sink") -> ERROR("tainted-sink: unsafe call with tainted values in $location");

END AUTOMATON
