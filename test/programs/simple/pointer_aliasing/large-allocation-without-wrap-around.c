// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void *malloc(unsigned int size);
extern void abort(void);
extern unsigned int __VERIFIER_nondet_uint(void);

int main() {
  unsigned int size = __VERIFIER_nondet_uint();
  if (size > 0xF0000000u) {
    return 0;
  }
  char *a = malloc(size);
  if (a == 0) {
    abort();
  }
  char c = 0;
  char *p = &c;
  unsigned int i = __VERIFIER_nondet_uint();
  if (i < size) {
    a[i] = 1;
  }
  if (*p != 0) {
ERROR:
    return 1;
  }
  return 0;
}
