// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>

// Computes remainder (%) by using addition and subtraction only
int rem_addsub(int a, int b) {
  int negate = (a < 0);

  int x = (a < 0) ? -a : a;
  int y = (b < 0) ? -b : b;

  while (x >= y) {
    x = x - y;
  }

  if (negate) {
    return -x;
  } else {
    return x;
  }
}

int main() {
  for (int a = -10; a <= 10; a++) {
    for (int b = -10; b <= 10; b++) {
      if (b != 0) {
        int calculated_rem = rem_addsub(a, b);

        // This is == in all cases
        assert(calculated_rem == (a % b));
      }
    }
  }

  return 0;
}
