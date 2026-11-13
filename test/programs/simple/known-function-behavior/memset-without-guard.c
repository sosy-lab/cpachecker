// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A byte-wise memset implementation without a check of the count: fill_bytes is a MEMSET
// implementation.

typedef unsigned long size_t;

extern void reach_error(void);
extern unsigned long __VERIFIER_nondet_ulong(void);

void *fill_bytes(void *s, int c, size_t n) {
  char *bytes = (char *)s;
  for (size_t i = 0; i < n; i++) {
    bytes[i] = c;
  }
  return s;
}

struct pair {
  unsigned long first;
  unsigned long second;
};

int main() {
  struct pair p = {__VERIFIER_nondet_ulong(), __VERIFIER_nondet_ulong()};
  fill_bytes(&p, 0, sizeof(p));
  if (p.first != 0 || p.second != 0) {
    reach_error();
  }
  return 0;
}
