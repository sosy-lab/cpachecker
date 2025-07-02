// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __VERIFIER_error();

int main() {
  int a = 5;
  int b = 10;
  int *p = &a;
  int **pp = &p;
  int *q = &b;
  *pp = q;
  if ((p != q) || (p == &a)) {
    ERROR:
    __VERIFIER_error();
  }
  return 0;
}