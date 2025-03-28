// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

int main() {

  int* x = (int*) malloc(6*sizeof(int));

  for (int i = 0; i < 6; i++) {
    x[i] = i;
  }

  free(x);

  free(x); //To export violation witnesses
  return 0;

}