// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef long long intmax_t;

extern int abs(int);
extern long labs(long);
extern long long llabs(long long);
extern intmax_t imaxabs(intmax_t);

void __VERIFIER_assert(int cond) {
  if (!cond) {
ERROR:
    return;
  }
}

int main(void) {
  // Check that the solver computes the precise absolute value
  __VERIFIER_assert(abs(-5) != 5);
  __VERIFIER_assert(labs(-5L) != 5L);
  __VERIFIER_assert(llabs(-5LL) != 5LL);
  __VERIFIER_assert(imaxabs((intmax_t)-5) != (intmax_t)5);

  return 0;
}
