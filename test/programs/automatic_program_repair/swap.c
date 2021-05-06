// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

// Error on line 23, should be second = temp
int main() {
    int first = __VERIFIER_nondet_int();
    int second = __VERIFIER_nondet_int();
    int firstCopy = first;
    int secondCopy = second;
    int temp = first;

    temp = first;

    first = second;

    second = first;


    if ((first == firstCopy || second == secondCopy) && firstCopy != secondCopy){
        goto ERROR;

    } else {
        goto EXIT;

    }


  EXIT: return 0;
  ERROR: return 1;
}
