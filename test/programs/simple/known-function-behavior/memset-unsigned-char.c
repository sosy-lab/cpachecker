// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A byte-wise memset implementation that writes through a pointer to unsigned char with pointer
// arithmetic and returns nothing: clear_memory is a MEMSET implementation.

typedef unsigned long size_t;

extern void reach_error(void);
extern unsigned int __VERIFIER_nondet_uint(void);

void clear_memory(void *s, int c, size_t n) {
  unsigned char *bytes = (unsigned char *)s;
  size_t i = 0;
  while (i < n) {
    *(bytes + i) = c;
    i = i + 1;
  }
}

int main() {
  unsigned int values[2] = {__VERIFIER_nondet_uint(), __VERIFIER_nondet_uint()};
  clear_memory(values, 0, sizeof(values));
  if (values[0] != 0 || values[1] != 0) {
    reach_error();
  }
  return 0;
}
