// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stddef.h>

extern void __VERIFIER_error();

void *malloc(unsigned long size);
void free(void *ptr);
int main() {
  int size = 10;
  int *arr = malloc(sizeof(int) * size);
  for (int i = 0; i < size; i++) {
    int *arr2 = malloc(sizeof(int) * size);
    if (arr2 == arr) {
      ERROR:
      __VERIFIER_error();
    }
    free(arr2);
  }
  free(arr);
  return 0;
}