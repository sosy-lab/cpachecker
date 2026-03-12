// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void reach_error();
extern __VERIFIER_nondet_int();

int g = 0;
int max = 3;

void count(int *c) {
  if (*c < max) {
    int* a = c;
    (*a)++;
    count(a);
    int* b = a;
    (*b) += 1;
  }
}


int main() {
  max = __VERIFIER_nondet_int();
  if (max >= 3 || max <= 0) return 0;

  count(&g);
  if (g != 2*max) {
    ERROR: reach_error();
  }
}