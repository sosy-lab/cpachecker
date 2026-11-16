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

// calloc zeroes all bytes, also for reads with another type than the allocated one.
int main() {
  int *p = calloc(4, sizeof(int));
  if (!p) {
    return 0;
  }
  unsigned char *bytes = (unsigned char *)p;
  if (bytes[5] != 0) {
    reach_error();
  }
  return 0;
}
