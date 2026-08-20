// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Hand-condensed from product-lines/minepump_spec4_product*.cil.c, the family that
// dominates the wrong-TRUE list.
//
// Expected verdict: FALSE. waterLevel starts at 1, the nondeterministic rise takes it
// to 2, the next timeShift() starts the pump, and because this product has no
// pump-deactivation feature the following shifts drain 2 -> 1 -> 0, so the check sees
// waterLevel == 0 with the pump running. --bmc-incremental and --kInduction agree.
// Actual DSS verdict: TRUE, deterministic.
//
// The trigger is processEnvironment__wrappee__base(): an EMPTY function called from
// TWO sites inside processEnvironment(), which sits in a loop. Calling it from one
// site instead (see the control below) gives the correct FALSE, as does removing the
// loop's "if (c < 4) {} else break;" shape.

extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *)
    __attribute__((__nothrow__, __leaf__)) __attribute__((__noreturn__));
void reach_error() { __assert_fail("0", "example_wrong_proof.c", 3, "reach_error"); }
extern int __VERIFIER_nondet_int(void);

int waterLevel = 1;
int pumpRunning = 0;

void processEnvironment__wrappee__base(void) { return; }

void processEnvironment(void) {
  if (!pumpRunning) {
    if (waterLevel < 2) { processEnvironment__wrappee__base(); } else { pumpRunning = 1; }
  } else {
    processEnvironment__wrappee__base();
  }
}

void timeShift(void) {
  if (pumpRunning) { if (waterLevel > 0) { waterLevel = waterLevel - 1; } }
  processEnvironment();
  if (waterLevel == 0) { if (pumpRunning) { ERROR: { reach_error(); abort(); } } }
}

int main(void) {
  int c = 0;
  int tmp;
  while (1) {
    if (c < 4) { } else { break; }
    tmp = __VERIFIER_nondet_int();
    if (tmp) { if (waterLevel < 2) { waterLevel = waterLevel + 1; } }
    timeShift();
  }
  return 0;
}
