// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

CONTROL AUTOMATON MultiErrors
INITIAL STATE Init;
STATE USEALL Init:
  MATCH {__VERIFIER_error($?)} || MATCH {reach_error($?)} || MATCH FUNCTIONCALL "reach_error" -> PRINTONCE "$rawstatement called in line $line" ERROR("$rawstatement called in line $line");
END AUTOMATON
