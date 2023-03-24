// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

void assert(int cond) { if (!cond) { ERROR: return; } }

extern int __VERIFIER_nondet_int();

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

  int set;
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
    for (int z = 0; z < 100; z++) {}
    if (p == 1 || p == 2) { // need an invariant that "p" can be only 1 or 2.
      set = 1;
    }
    for (int z2 = 0; z2 < 100; z2++) {}
    assert(set == 1);
  }

  assert((nondet && p == 1) || (!nondet && p == 2));
}
