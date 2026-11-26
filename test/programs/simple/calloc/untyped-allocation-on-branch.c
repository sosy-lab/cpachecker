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

// The type of the allocation is never revealed, and it happens only on one branch.
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
  unsigned long i = __VERIFIER_nondet_ulong();
  if (i >= 1 && i < n && p[i] != 0) {
    reach_error();
  }
  unsigned char *bytes = (unsigned char *)p;
  if (i >= sizeof(unsigned int) && i < n * sizeof(unsigned int) && bytes[i] != 0) {
    reach_error();
  }
  return 0;
}
