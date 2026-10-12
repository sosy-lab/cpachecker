// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>


extern short __VERIFIER_nondet_short(void);


int main() {
  short short_var = __VERIFIER_nondet_short();

  if (short_var <= -500) { // Prevents underflow and restricts short_var from -499 to short max
    return 0;
  }
  short_var--;

  if (short_var >= 500) {
    return 0;
  }

  assert(short_var != -501); // Safe

  assert(short_var != -500); // Fails -> short_var == -500
  return 0;
}