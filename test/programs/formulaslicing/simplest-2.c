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
  int x = 0;
  int p;
  int z = __VERIFIER_nondet_int();
  if (z > 100) {
    p = 0;
  } else {
    p = 1;
  }

  while (__VERIFIER_nondet_int()) {
    x++;
  }

  assert(p = 0);
}
