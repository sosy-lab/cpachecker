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
int* getPointer() {
  int *p = malloc(sizeof(int) * 10);
  return p;
}
int main() {
  int *result = getPointer();
  if (result == getPointer()){
      ERROR:
      __VERIFIER_error();
    }
  return 0;
}