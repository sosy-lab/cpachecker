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

extern int __VERIFIER_nondet_int(void);
extern long __VERIFIER_nondet_long(void);
extern long long __VERIFIER_nondet_longlong(void);
extern int __VERIFIER_assume(int);

void __VERIFIER_assert(int cond) {
  if (!cond) {
ERROR:
    return;
  }
}

int main(void) {
  int i = __VERIFIER_nondet_int();
  long l = __VERIFIER_nondet_long();
  long long ll = __VERIFIER_nondet_longlong();
  intmax_t j = __VERIFIER_nondet_longlong();

  // Exclude each type's minimum value: computing the absolute value there is
  // undefined behavior (C11 7.22.6.1p2/7.8.2.1p2).
  // This assumes the default 32-bit (ILP32) machine model
  __VERIFIER_assume(i != (-2147483647 - 1));
  __VERIFIER_assume(l != (-2147483647L - 1));
  __VERIFIER_assume(ll != (-9223372036854775807LL - 1));
  __VERIFIER_assume(j != (-9223372036854775807LL - 1));

  __VERIFIER_assert(abs(i) >= 0);
  __VERIFIER_assert(labs(l) >= 0);
  __VERIFIER_assert(llabs(ll) >= 0);
  __VERIFIER_assert(imaxabs(j) >= 0);

  return 0;
}
