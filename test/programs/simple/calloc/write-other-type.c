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

// A write with another type changes the zeroed bytes.
int main() {
  long *p = calloc(2, sizeof(long));
  if (!p) {
    return 0;
  }
  p[0] = 0x41;
  unsigned char *bytes = (unsigned char *)p;
  if (bytes[8] != 0 || bytes[15] != 0) {
    reach_error();
  }
  if (bytes[0] == 0x41 || bytes[7] == 0x41) {
    reach_error();
  }
  return 0;
}
