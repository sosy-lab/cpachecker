// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A recursive program whose main function never returns, because every path
// ends in abort(). Analyses that expect to reach the exit node of main have to
// cope with this, cf. #1539.

extern void abort(void);
extern void __VERIFIER_error(void);

int f(int n) {
  if (n <= 0) {
    return 0;
  }
  return f(n - 1) + 1;
}

int main(void) {
  if (f(3) != 3) {
    __VERIFIER_error();
  }
  abort();
}
