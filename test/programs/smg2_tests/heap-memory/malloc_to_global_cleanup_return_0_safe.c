// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef long unsigned int size_t;
extern void *malloc (size_t __size);

int *x = ((void *)0);
int main() {
  x = malloc(3);
  return 0;  // VALGRIND says all memory is reachable (from outside of main), so this is safe
}
