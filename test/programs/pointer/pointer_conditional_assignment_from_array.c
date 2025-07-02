// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stddef.h>

extern void __VERIFIER_error();

int main() {
  int a = 2, b = 1;
  int *arr[2];
  arr[0] = NULL;
  arr[1] = &b;
  int *q;
  if (arr[0]) {
    q = &a;
  }
  else {
    q = arr[1];
  }
  if (q != &b) {
    ERROR:
    __VERIFIER_error();
  }
  return 0;
}