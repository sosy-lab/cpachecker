// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// This automaton contains the specification of a proposed category for taint checking.
// It queries the TaintCPA for information about taint violations.

CONTROL AUTOMATON TAINTSTATUS

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(TaintAnalysisCPA, "taint-error") -> ERROR("taint-error: unsafe call with tainted values in $location");

END AUTOMATON
