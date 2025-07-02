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
  int *p = NULL;
  int a = 2;
  int *q;
  if (p == NULL) {
    q = p;
  }
  if (q) {
    ERROR:
    __VERIFIER_error();
  }
  return 0;
}