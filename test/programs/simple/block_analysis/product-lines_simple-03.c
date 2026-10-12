// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2011-2013 Alexander von Rhein, University of Passau
// SPDX-FileCopyrightText: 2011-2021 The SV-Benchmarks Community
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
//
// This is a heavily modified and simplified version of
// product-lines/minepump_spec4_product48.cil.c in SV-Benchmarks.

void reach_error() {
}

/* nondet input */
extern int __VERIFIER_nondet_int(void);

/* --- Environment --- */
int waterLevel = 1;
int methaneLevelCritical = 0;

/* --- System state --- */
int pumpRunning = 0;
int systemActive = 1;

/* --- Error condition --- */
void check_spec() {
    if (waterLevel == 0 && pumpRunning) {
        reach_error();
    }
}

/* --- Scenario --- */
void test() {
    int i = 0;
    while (i < 4) {
        if (__VERIFIER_nondet_int()) if (waterLevel < 2) waterLevel++;
        if (__VERIFIER_nondet_int()) methaneLevelCritical = !methaneLevelCritical;

        if (__VERIFIER_nondet_int()) {
            systemActive = 1;
        } else if (__VERIFIER_nondet_int()) {
            systemActive = 0;
            pumpRunning = 0;
        }

        if (pumpRunning) {
            if (waterLevel > 0) waterLevel--;
        }

        if (systemActive) {
          if (pumpRunning && methaneLevelCritical) {
              pumpRunning = 0;
          } else if (!pumpRunning && waterLevel >= 2) {
                if (!methaneLevelCritical) {
                    pumpRunning = 1;
                }
          }
        }

        check_spec();
        i++;
    }
}

/* --- Main --- */
int main() {
    test();
    return 0;
}
