// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2017 Rodrigo Castano
// SPDX-FileCopyrightText: 2017-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

OBSERVER AUTOMATON AssumptionAutomaton

INITIAL STATE DummyAA;

STATE __TRUE :
    TRUE -> GOTO __TRUE;

STATE __FALSE :
    TRUE -> GOTO __FALSE;

STATE USEFIRST DummyAA :
    TRUE -> GOTO __TRUE;

END AUTOMATON
