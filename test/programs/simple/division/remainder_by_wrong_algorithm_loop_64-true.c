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
  for (int a = -100; a <= 100; a++) {
    for (int b = -100; b <= 100; b++) {
      if (b != 0) {
        // We have 40200 calculations in total
        int calculated = smt_mod(a, b);
        // C division is equal to SMT based remainder 21164 times
        if (calculated == (a % b)) {
          c++;
        }
      }
    }
  }

  assert(c == 21164);
  return 0;
}
