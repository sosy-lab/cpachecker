// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
void reach_error() {}

void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    reach_error();
  }
  return;
}

int main(void) {
  unsigned int x = 0;
  unsigned int y = 0;
  while (x < 100) {
    unsigned int z = 0; // IO
    unsigned int a; // write only
    y = x*x;
    x++;
    z++;
    a = 0;
  }
  __VERIFIER_assert(y<10000);
}
