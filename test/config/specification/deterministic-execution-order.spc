// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// This automaton contains the specification for deterministic execution behavior/order
// based on compile-time decisions open by the C standard (ref. C11 Annex J).
// It queries the UnseqBehaviorAnalysisCPA for information about possible unsequenced execution order.
// TODO: the term unsequenced might not be correct in all cases according to the C11 standard!

CONTROL AUTOMATON UNSPECBEHCPA

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(UnseqBehaviorAnalysisCPA, "has-unsequenced-execution") -> ERROR("deterministic-execution: unsequenced execution order possible at $location");

END AUTOMATON
