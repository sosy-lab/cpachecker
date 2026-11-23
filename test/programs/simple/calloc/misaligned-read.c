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

// A read of an int overlaps the write of a char.
int main() {
  char *p = calloc(16, 1);
  if (!p) {
    return 0;
  }
  p[5] = 1;
  int *q = (int *)(p + 4);
  if (*q != 0) {
    reach_error();
  }
  return 0;
}
