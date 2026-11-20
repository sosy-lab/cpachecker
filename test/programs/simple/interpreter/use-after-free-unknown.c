// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The SMG analysis does not report the invalid write as a target state for termination, so the
// execution has to stop there instead of continuing with an undefined behavior.
typedef unsigned int size_t;
extern void *malloc(size_t size);
extern void free(void *ptr);

int main(void) {
  int *p = malloc(sizeof(int));
  if (p == 0) {
    return 0;
  }
  free(p);
  *p = 1;
  return 0;
}
