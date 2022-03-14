// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2017 Rodrigo Castano
// SPDX-FileCopyrightText: 2017-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

CONTROL AUTOMATON ErrorPath7

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
    MATCH "[!(i)]" -> GOTO ARG5;
    TRUE -> STOP;

STATE USEFIRST ARG5 :
    MATCH "i = i - 2;" -> GOTO ARG6;
    TRUE -> STOP;

STATE USEFIRST ARG6 :
    MATCH "i = i - 3;" -> GOTO ARG7;
    TRUE -> STOP;

STATE USEFIRST ARG7 :
    MATCH "return 0;" -> ERROR;
    TRUE -> STOP;

STATE USEFIRST ARG9 :
    TRUE -> STOP;

END AUTOMATON
