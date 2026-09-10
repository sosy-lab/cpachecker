// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

int main(void) {
  int *a = malloc(4 * sizeof(int));
  for (int i = 0; i < 4; i++) {
    a[i] = i;
  }
  a[4] = 4; // out of bounds
  free(a);
  return 0;
}
