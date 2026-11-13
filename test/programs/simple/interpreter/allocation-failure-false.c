// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The error is reachable only in the execution in which the allocation fails.
typedef unsigned int size_t;
extern void *malloc(size_t size);
extern void free(void *ptr);
extern void reach_error(void);

int main(void) {
  int *p = malloc(sizeof(int));
  if (p == 0) {
    reach_error();
  }
  free(p);
  return 0;
}
