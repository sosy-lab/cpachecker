// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

OBSERVER AUTOMATON BDDCPAErrorLocationAutomaton
// This automaton detects error locations that are specified
// by the label "ERROR"
// it then prints the current content of the BDDCPA state.

INITIAL STATE Init;

STATE USEFIRST Init :
       // this transition matches if the label of the successor CFA location starts with "error"
     MATCH LABEL [[Ee\][Rr\][Rr\][Oo\][Rr\].*]

  -> PRINT "Product violating in line $line: " PRINT EVAL(BDDCPA,"VALUES") PRINT " Tracked " PRINT EVAL(BDDCPA, "VARSETSIZE") PRINT " Vars" ERROR;

  MATCH EXIT -> PRINT "Valid Product: " PRINT EVAL(BDDCPA,"VALUES") GOTO Init;

END AUTOMATON
