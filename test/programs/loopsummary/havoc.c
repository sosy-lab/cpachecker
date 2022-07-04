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
  unsigned int x = 1000000;
  while (x > 0) {
    x -= 4;
  }
  __VERIFIER_assert(!(x % 4));

  int y = 0;
  for (int z = 0; y<1000000 ; y += 2) {
    ;
  }
  __VERIFIER_assert(y> 999990);

  int z = 0;
  while (z<1000000) {
    z += 2;
  }
  __VERIFIER_assert(z> 999990);
}
