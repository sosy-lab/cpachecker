// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0void reach_error() {}

void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    reach_error();
  }
  return;
}

int main(void) {
  unsigned int x = 1000000;
  unsigned int y = 1000000;
  while (x > 0 || y > 0) {
    x -= 4;
    y -= 4;
  }
  __VERIFIER_assert(!(x % 4));
  __VERIFIER_assert(!(y % 4));
}