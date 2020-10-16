// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

void assert(int cond) { if (!cond) { ERROR: return; } }

int main() {
  int x = 5;
  int y = 10;
  int nondet = __VERIFIER_nondet_int();
  int p;
  int counter;
  if (nondet) {
    y = 100;
    p = 1;
  } else {
    p = 2;
  }

  while (__VERIFIER_nondet_int()) {
    x++;
    counter = 100;
    while (__VERIFIER_nondet_int()) {
      y++;
    }
    assert(counter == 100);
    while (__VERIFIER_nondet_int()) {
      counter++;
    }
  }

  assert(counter == 100);
}
