// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef unsigned long size_t;

void reach_error() {}
extern void abort(void);
extern int __VERIFIER_nondet_int(void);
extern unsigned long __VERIFIER_nondet_ulong(void);
extern void *calloc(size_t nmemb, size_t size);

// Like untyped-allocation-on-branch.c, but the read may overlap the write.
void *may_fail_calloc(size_t n) {
  return __VERIFIER_nondet_int() ? 0 : calloc(n, 1);
}

int main() {
  unsigned long n = __VERIFIER_nondet_ulong();
  if (n == 0 || n > 100) {
    return 0;
  }
  unsigned int *p = may_fail_calloc(n * sizeof(unsigned int));
  if (!p) {
    return 0;
  }
  p[0] = 7;
  unsigned char *bytes = (unsigned char *)p;
  unsigned long i = __VERIFIER_nondet_ulong();
  if (i < n * sizeof(unsigned int) && bytes[i] != 0) {
    reach_error();
  }
  return 0;
}
