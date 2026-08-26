// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>


// Computes Euclidean division (as defined for Integer theory division in SMT, or bvsmod)
int smt_div(int a, int b) {
  int q = 0;
  int r = a;
  int y = (b < 0) ? 0 - b : b;

  while (r < 0) {
    r = r + y;
    q = q + ((b < 0) ? 1 : -1);
  }

  while (r >= y) {
    r = r - y;
    q = q + ((b < 0) ? -1 : 1);
  }

  return q;
}


int main() {
  int c = 0;
  for (int a = -10; a <= 10; a++) {
    for (int b = -10; b <= 10; b++) {
      if (b != 0) {
        // We have 420 calculations in total
        int calculated = smt_div(a, b);
        // C division is equal to aboves procedure in 274 cases
        assert(calculated == (a / b));
      }
    }
  }

  return 0;
}
