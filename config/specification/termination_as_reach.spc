// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

CONTROL AUTOMATON NonTerminationLabelAutomaton
// Automaton required by the termination algorithm to detect potential non-termination.
// This automaton detects error locations that are specified by the label
// "__CPACHECKER_NON_TERMINATION"

INITIAL STATE Init;

STATE USEFIRST Init :
   // this transition matches if the label of the successor CFA location is
   // "__CPACHECKER_NON_TERMINATION"
   MATCH LABEL [__CPACHECKER_NON_TERMINATION] -> ERROR("non-termination label in $location");
   MATCH {__VERIFIER_error($?)} || MATCH {__assert_fail($?)} -> STOP;
END AUTOMATON
