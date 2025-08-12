// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

int main() {

  int* x = (int*) malloc(5*sizeof(int));
  *x = 42;

  x = (int*) malloc(10*sizeof(int)); // This line overrides the pointer x, which is invalid in the context of memory tracking

  return 0;
}