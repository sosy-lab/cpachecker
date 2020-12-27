// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

CONTROL AUTOMATON ARG

INITIAL STATE init;

STATE USEALL init:
    MATCH "[x > 0]" -> ASSUME {x<10} GOTO init;
    MATCH "[x > 0]" -> ASSUME {x<0} ERROR;
    MATCH "[!(x > 0)]" -> ASSUME {x>-20} GOTO init;
    MATCH "[!(x > 0)]" -> ASSUME {x>0} ERROR;

END AUTOMATON
