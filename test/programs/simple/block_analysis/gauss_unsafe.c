// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int,
                           const char *) __attribute__((__nothrow__, __leaf__))
__attribute__((__noreturn__));

void reach_error() {
  __assert_fail("0", "gauss_unsafe.c", 17, "reach_error");
}

extern unsigned int __VERIFIER_nondet_uint();

int main() {
  unsigned int i, n = __VERIFIER_nondet_uint(), sn = 0;
  for (i = 0; i <= n; i++) {
    sn = sn + i;
  }
  if (!(sn == (n * (n + 1)) / 2 || sn == 0)) {
    reach_error();
  }
  return 0;
}
