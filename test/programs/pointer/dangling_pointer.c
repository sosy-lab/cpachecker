// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stddef.h>

extern void __VERIFIER_error();

int* getPointer() {
  int a = 5;
  int *p = &a;
  return p;
  }
int main() {
  int *result = getPointer();
  if (!result){
    ERROR:
    __VERIFIER_error();
  }
  return 0;
}