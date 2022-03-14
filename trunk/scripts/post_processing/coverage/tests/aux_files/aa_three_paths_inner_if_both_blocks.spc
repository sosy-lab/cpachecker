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
    MATCH "[!(i)]" -> GOTO __FALSE;
    MATCH "[i]" -> GOTO ARG7;
    TRUE -> GOTO __TRUE;

STATE USEFIRST ARG7 :
    MATCH "[i > 5]" -> GOTO ARG8;
    MATCH "[!(i > 5)]" -> GOTO ARG9;
    TRUE -> GOTO __TRUE;

STATE USEFIRST ARG8 :
    MATCH "i = i + 5;" -> GOTO ARG10;
    TRUE -> GOTO __TRUE;

STATE USEFIRST ARG9 :
    MATCH "i = i + 1;" -> GOTO ARG11;
    TRUE -> GOTO __TRUE;

STATE USEFIRST ARG10 :
    MATCH "return 0;" -> GOTO __FALSE;
    TRUE -> GOTO __TRUE;

STATE USEFIRST ARG11 :
    MATCH "return 0;" -> GOTO __FALSE;
    TRUE -> GOTO __TRUE;

END AUTOMATON
