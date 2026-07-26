// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/* --- Global State --- */
int waterLevel = 1;
int methaneLevelCritical = 0;

int pumpRunning = 0;
int systemActive = 1;

/* --- Print Counter --- */
int printCounter = 0;

/* --- Error --- */
void reach_error() { abort(); }

/* --- Environment --- */
void lowerWaterLevel() {
    if (waterLevel > 0) waterLevel--;
}

void waterRise() {
    if (waterLevel < 2) waterLevel++;
}

void changeMethaneLevel() {
    methaneLevelCritical = !methaneLevelCritical;
}

int isMethaneLevelCritical() {
    return methaneLevelCritical;
}

int getWaterLevel() {
    return waterLevel;
}

/* --- Pump --- */
void activatePump() { pumpRunning = 1; }
void deactivatePump() { pumpRunning = 0; }
int isPumpRunning() { return pumpRunning; }

void startSystem() { systemActive = 1; }

/* --- Specification (unchanged) --- */
void checkSpecification() {
    if (!isMethaneLevelCritical()) {
        if (getWaterLevel() == 2 && !isPumpRunning()) {
            reach_error();
        }
    }
}

/* --- System Step --- */
void processEnvironment() {printCounter++; }

void timeShift() {
    if (pumpRunning) {
        lowerWaterLevel();
    }
    if (systemActive) {
        processEnvironment();
    }
    checkSpecification();
}

/* --- Print Functions (REPLACED) --- */
void printEnvironment() {
    printCounter++;   // replaces all printf inside
}

void printPump() {
    printCounter++;   // replaces entire printPump + nested prints
}

/* --- Nondet --- */
extern int __VERIFIER_nondet_int(void);

/* --- Test Scenario --- */
void test() {
    int i = 0;

    while (i < 4) {
        if (__VERIFIER_nondet_int()) waterRise();
        if (__VERIFIER_nondet_int()) changeMethaneLevel();

        if (__VERIFIER_nondet_int()) {
            startSystem();
        }

        timeShift();   // unchanged
        i++;
    }

    cleanup();
}

/* --- Cleanup --- */
int cleanupTimeShifts = 4;

void cleanup() {
    timeShift();   // unchanged

    int i = 0;
    while (i < cleanupTimeShifts - 1) {
        timeShift();   // unchanged
        i++;
    }
}

/* --- Scenario (print calls preserved) --- */
void Specification2() {
    timeShift();
    printPump();

    timeShift();
    printPump();

    timeShift();
    printPump();

    waterRise();
    printPump();

    timeShift();
    printPump();

    changeMethaneLevel();
    printPump();

    timeShift();
    printPump();

    cleanup();
}

/* --- Main --- */
int main() {
    test();
    cleanup();
    return 0;
}
