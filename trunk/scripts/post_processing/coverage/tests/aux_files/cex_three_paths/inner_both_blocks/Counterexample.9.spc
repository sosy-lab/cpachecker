// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2017 Rodrigo Castano
// SPDX-FileCopyrightText: 2017-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

CONTROL AUTOMATON ErrorPath9

INITIAL STATE ARG0;

STATE USEFIRST ARG0 :
    MATCH "" -> GOTO ARG1;
    TRUE -> STOP;

STATE USEFIRST ARG1 :
    MATCH "int __VERIFIER_nondet_int();" -> GOTO ARG2_1_1;
STATE USEFIRST ARG2_0_1 :
    MATCH "int __VERIFIER_nondet_int();" -> GOTO ARG2_1_1;
STATE USEFIRST ARG2_1_1 :
    MATCH "int main()" -> GOTO ARG2_2_1;
STATE USEFIRST ARG2_2_1 :
    MATCH "" -> GOTO ARG2_3_1;
STATE USEFIRST ARG2_3_1 :
    MATCH "int i = __VERIFIER_nondet_int();" -> GOTO ARG2_4_1;
STATE USEFIRST ARG2_4_1 :
    MATCH "int i = __VERIFIER_nondet_int();" -> GOTO ARG2;
    TRUE -> STOP;

STATE USEFIRST ARG2 :
    MATCH "[i]" -> GOTO ARG4;
    TRUE -> STOP;

STATE USEFIRST ARG4 :
    MATCH "[!(i > 5)]" -> GOTO ARG17;
    TRUE -> STOP;

STATE USEFIRST ARG17 :
    MATCH "i = i + 1;" -> GOTO ARG29;
    TRUE -> STOP;

STATE USEFIRST ARG29 :
    MATCH "return 0;" -> ERROR;
    TRUE -> STOP;

STATE USEFIRST ARG31 :
    TRUE -> STOP;

END AUTOMATON
