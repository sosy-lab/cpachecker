// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Only the execution in which the allocation fails does not terminate,
// so executing the successful allocation alone must not prove termination.
typedef unsigned int size_t;
extern void *malloc(size_t size);
extern void free(void *ptr);

int main(void) {
  int *p = malloc(sizeof(int));
  if (p == 0) {
    while (1) {
    }
  }
  free(p);
  return 0;
}
