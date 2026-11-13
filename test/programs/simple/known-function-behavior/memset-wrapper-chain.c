// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Wrappers of a byte-wise memset implementation in two levels: set_bytes is a MEMSET
// implementation, set_bytes_checked and set_memory are wrappers of it.

typedef unsigned long size_t;

extern void abort(void);
extern void reach_error(void);
extern unsigned long __VERIFIER_nondet_ulong(void);

void *set_bytes(void *s, int c, size_t n) {
  char *bytes = (char *)s;
  for (size_t i = 0; i < n; i++) {
    bytes[i] = c;
  }
  return s;
}

void *set_bytes_checked(void *s, int c, size_t n) {
  if (s == 0) {
    abort();
  }
  void *result = set_bytes(s, c, n);
  return result;
}

void set_memory(void *s, int c, size_t n) { set_bytes_checked(s, c, n); }

int main() {
  unsigned long value = __VERIFIER_nondet_ulong();
  set_memory(&value, 0, sizeof(value));
  if (value != 0) {
    reach_error();
  }
  return 0;
}
