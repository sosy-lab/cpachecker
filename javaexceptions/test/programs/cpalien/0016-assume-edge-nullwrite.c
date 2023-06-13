// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <stdbool.h>

extern bool __VERIFIER_nondet_bool();

int main() {
  bool selector = __VERIFIER_nondet_bool();

  int *ptr = NULL;

  if (!selector) {
    ptr = malloc(sizeof(int));
  }

  if (selector) {
    *ptr = 666;
  }
  free(ptr);

  return 0;
}
