// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
extern void reach_error();

int g = 0;
int max = 0;

void count() {
  if (g < max) {
    g++;
    count();
  }

  if (g == max) {
    ERROR: reach_error();
  }
}


int main() {
  g = __VERIFIER_nondet_int();
  max = __VERIFIER_nondet_int();
  if (g > max) {
    return 0;
  }

  count();
}