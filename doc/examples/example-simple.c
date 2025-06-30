// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>
#include <stdlib.h>
int* getPointer() {
  int *p;
  static int a =1;
  if (a == 1) {
    p = malloc(sizeof(int) * 10);
  }
  else{
    p = malloc(sizeof(int) * 5);
  }
  return p;
}
int main() {
  int *result = getPointer();
  assert(result != NULL);
  free(result);
  return 0;
}
