// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern float fabsf(float);

void __VERIFIER_assert(int cond) {
  if (!cond) {
ERROR:
    return;
  }
}

int main(void) {
  float x = -3.5f;
  float r = fabsf(x);

  // fabsf(-3.5f) is exactly 3.5f
  __VERIFIER_assert(r != 3.5f);

  return 0;
}
