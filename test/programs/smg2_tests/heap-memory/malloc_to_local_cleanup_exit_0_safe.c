// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

typedef long unsigned int size_t;
extern void *malloc (size_t __size);

int main() {
  int *x = malloc(3);
  //return 0;
  exit(0);
  // Test abort() and other numbers, document, check standard, check these into SV-Benchmarks
}