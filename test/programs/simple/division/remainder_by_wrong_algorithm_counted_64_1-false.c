// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>


// Computes Euclidean modulo as defined for Integer theory in SMT
int smt_mod(int a, int b) {
  int r = a;
  int y = (b < 0) ? 0 - b : b;

  while (r < 0) {
    r = r + y;
  }

  while (r >= y) {
    r = r - y;
  }

  return r;
}

// Should a C remainder implementation use SMT modulo, belows comparisons should be true more often
int main() {
  int c = 0;
  int i = 0;
  for (int a = -10; a <= 10; a++) {
    for (int b = -10; b <= -1; b++) {
      // We have 210 calculations in total
      int calculated = smt_mod(a, b);
      i++;
      // C division is equal to SMT based remainder 137 times
      if (calculated == (a % b)) {
        c++;
      }
    }
  }

  assert(c != 137);
  return 0;
}
