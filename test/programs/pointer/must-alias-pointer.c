// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __VERIFIER_error();
int main() {
  int i = 1;
  int *p = &i;
  int a = 3;
  int *q = &a;
  p = q;
  if (p != q) {
    ERROR:
    __VERIFIER_error();
  }
  return 0;
}