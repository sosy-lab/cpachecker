// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <assert.h>
int main() {
  int size = 100;
  int num = __VERIFIER_nondet_int();
  int * arr = malloc(sizeof(int) * size);
  for (int i = 0; i < size; i++) {
    arr[i] = num;
    num++;
  }
  for (int i = size; i >= 0; i--) {
    assert(*(arr + i) == num);
    num--;
  }
  return 0;
}
