// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void* malloc(unsigned long);
extern void __VERIFIER_error();

int main() {
  int a[2];
  int b;

  if (((long)&b) % _Alignof(int) != 0) {
    goto ERROR;
  }

  if (((long)&a) % _Alignof(int) != 0) {
    goto ERROR;
  }

  int *p = malloc(4);
  if (((long)p) % _Alignof(long long) != 0) {
    goto ERROR;
  }

  return 0;
ERROR:
  return 1;
}
