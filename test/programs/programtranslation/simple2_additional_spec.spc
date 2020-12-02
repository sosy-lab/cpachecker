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
    MATCH "x++;" -> ASSUME {x==1} GOTO init;
    MATCH "x++;" -> ASSUME {x!=1} ERROR;
    MATCH "x--;" -> ASSUME {x==1} GOTO init;
    MATCH "x--;" -> ASSUME {x!=1} ERROR;

END AUTOMATON
