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

  *x = __VERIFIER_nondet_uint();

  int* y = x;

  if (*x > 100) {
    free(y);
    y = NULL; // Waypoints after the closing of a code block '}' cannot be exported
  }

  free(x); //Double free

  return 0;
}