// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2017 Rodrigo Castano
// SPDX-FileCopyrightText: 2017-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

OBSERVER AUTOMATON AssumptionAutomaton

INITIAL STATE ARG0;

STATE __TRUE :
    TRUE -> GOTO __TRUE;

STATE __FALSE :
    TRUE -> GOTO __FALSE;

STATE USEFIRST ARG0 :
    MATCH "" -> GOTO ARG1;
    TRUE -> GOTO __TRUE;

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
    TRUE -> GOTO __TRUE;

STATE USEFIRST ARG2 :
    MATCH "[!(i)]" -> GOTO ARG5;
    MATCH "[i]" -> GOTO __FALSE;
    TRUE -> GOTO __TRUE;

STATE USEFIRST ARG5 :
    MATCH "i = i - 2;" -> GOTO ARG6;
    TRUE -> GOTO __TRUE;

STATE USEFIRST ARG6 :
    MATCH "i = i - 3;" -> GOTO __FALSE;
    TRUE -> GOTO __TRUE;

END AUTOMATON
