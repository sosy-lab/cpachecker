// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>


// C division expressed using + and -
int div_addsub(int a, int b) {
  int negate = (a < 0) != (b < 0);

  int x = (a < 0) ? -a : a;
  int y = (b < 0) ? -b : b;

  int q = 0;

  while (x >= y) {
    x = x - y;
    q = q + 1;
  }

  return negate ? -q : q;
  if (negate) {
    return -q;
  } else {
    return q;
  }
}


int main() {
  for (int a = -100; a <= 100; a++) {
    for (int b = -100; b <= 100; b++) {
      if (b != 0) {
        int calculated = div_addsub(a, b);
        // This is correct in all cases
        assert(calculated == (a / b));
      }
    }
  }

  return 0;
}
