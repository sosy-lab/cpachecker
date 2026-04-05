// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void reach_error();
extern int __VERIFIER_nondet_int();

int a = 0;
int b = 0;
int n = 0;

void f(int x) {
  if (x > 0) {
    f(x - 1);
    n = n + 1;
  }

  if (n % 2 == 0) {
    a = n;
  } else {
    b = n;
  }
}

int main() {
  int x = __VERIFIER_nondet_int();
  if (x <= 1) {
    return 0;
  }

  f(x);

  if (a - b != -1 && a - b != 1) {
    ERROR: reach_error();
  }
}
