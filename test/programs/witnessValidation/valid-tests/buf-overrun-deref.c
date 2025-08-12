// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

int main() {
  int* x = (int*) malloc(sizeof(int));
  *x = 42;

  int y = *(x+2*sizeof(int));

  free(x);
  return 0;
}