// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

int sum(int *a, int n) {
  if (n == 0) {
    return 0;
  }
  return a[n - 1] + sum(a, n - 1);
}

int main(void) {
  int *a = malloc(4 * sizeof(int));
  for (int i = 0; i < 4; i++) {
    a[i] = i;
  }
  int s = sum(a, 4);
  free(a);
  return s;
}
