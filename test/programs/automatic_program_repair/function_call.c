// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

// Error on line 16, add(first, second)
int main() {
    int first = __VERIFIER_nondet_int();
    int second = __VERIFIER_nondet_int();

    int result = add(first, first);

    if (first + second == result){
        goto EXIT;
    } else {
        goto ERROR;
    }

  EXIT: return 0;
  ERROR: return 1;
}

int add(int a, int b){
  return a + b;
}