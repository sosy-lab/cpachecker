// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


/* ---------------- Global State ---------------- */

extern int __VERIFIER_nondet_int();

int waterLevel = 1;              // 0..2
int methaneLevelCritical = 0;    // 0/1
int pumpRunning = 0;             // 0/1
int systemActive = 1;            // 0/1

/* ---------------- Safety Property ---------------- */
/* ERROR if pump runs while water level is 0 */

void specification4(void) {
    if (waterLevel == 0 && pumpRunning) {
      reach_error();
    }
}

/* ---------------- Environment ---------------- */

void lowerWaterLevel(void) {
    if (waterLevel > 0)
        waterLevel--;
}

void waterRise(void) {
    if (waterLevel < 2)
        waterLevel++;
}

void changeMethaneLevel(void) {
    methaneLevelCritical = !methaneLevelCritical;
}

int isMethaneAlarm(void) {
    return methaneLevelCritical;
}

int isHighWaterLevel(void) {
    return waterLevel == 2;
}

/* ---------------- Pump Control ---------------- */

void activatePump(void) {
    if (!isMethaneAlarm())
        pumpRunning = 1;
}

void deactivatePump(void) {
    pumpRunning = 0;
}

void startSystem(void) {
    systemActive = 1;
}

void stopSystem(void) {
    pumpRunning = 0;
    systemActive = 0;
}

/* ---------------- Core Logic ---------------- */

void processEnvironment(void) {

    if (pumpRunning && isMethaneAlarm()) {
        deactivatePump();
        return;
    }

    if (!pumpRunning && isHighWaterLevel()) {
        activatePump();
    }
}

void timeShift(void) {

    if (pumpRunning)
        lowerWaterLevel();

    if (systemActive)
        processEnvironment();

    specification4();
}

/* ---------------- Scenario ---------------- */

void test(void) {
    for (int i = 0; i < 4; i++) {

        if (__VERIFIER_nondet_int() % 2)
            waterRise();

        if (__VERIFIER_nondet_int() % 2)
            changeMethaneLevel();

        if (__VERIFIER_nondet_int() % 2)
            startSystem();
        else if (__VERIFIER_nondet_int() % 2)
            stopSystem();

        timeShift();
    }
}

/* ---------------- Main ---------------- */

int main(void) {
    test();
    return 0;
}
