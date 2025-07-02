// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stddef.h>

extern void __VERIFIER_error();

int global = 10;
int* returnGlobalPointer() {
  return &global;
}
int main() {
  int *ptr = returnGlobalPointer();
  int *ptr2 = returnGlobalPointer();
  if (ptr != ptr2) {
      ERROR:
      __VERIFIER_error();
    }
  return 0;
}