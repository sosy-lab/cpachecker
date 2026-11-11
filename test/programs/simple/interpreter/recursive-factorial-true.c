// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The result of the recursive call is multiplied with the local variable n of the
// caller, so an analysis that loses the values of a caller during a recursive call
// cannot determine the result.
extern void reach_error(void);

int fac(int n) {
  if (n <= 1) {
    return 1;
  }
  return n * fac(n - 1);
}

int main(void) {
  if (fac(7) != 5040) {
    reach_error();
  }
  return 0;
}
