// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef long unsigned int size_t;

extern void __VERIFIER_nondet_memory(void *ptr, size_t size);
extern void *malloc(size_t size);
extern void free(void *ptr);

int main() {
  int *values = malloc(sizeof(int) * 4);
  if (values == 0) {
    return 0;
  }
  values[0] = 0;
  values[1] = 0;
  values[2] = 0;
  values[3] = 0;
  __VERIFIER_nondet_memory(values, sizeof(int) * 4);
  unsigned int index = (unsigned int)values[0];
  values[index] = 1; // invalid dereference for potential index >= 4
  free(values);
}
