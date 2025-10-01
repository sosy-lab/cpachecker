// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>
#include <stdlib.h>
int main() {
  int size = 100;
  int num = __VERIFIER_nondet_int();
  int *arr = malloc(sizeof(int) * size);
  for (int i = 0; i < size; i++) {
    arr[i] = num;
    num++;
  }
  for (int i = size - 1; i >= 0; i--) {
    num--;
    assert(arr[i] == num);
  }
  free(arr);
  return 0;
}
