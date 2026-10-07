// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern float fabsf(float);
extern float __VERIFIER_nondet_float(void);

void __VERIFIER_assert(int cond) {
  if (!cond) {
ERROR:
    return;
  }
}

int main(void) {
  float x = __VERIFIER_nondet_float();
  float r = fabsf(x);

  // fabsf(x) is never negative for any x, including NaN and infinities
  // (C11 7.12.7.2; Annex F.10.4.2).
  __VERIFIER_assert(r >= 0.0f || r != r); // r != r is true iff r is NaN

  return 0;
}
