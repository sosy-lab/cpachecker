// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error(){}
void __VERIFIER_assert(int cond) {
  if (!cond) {
    reach_error();
  }
}

int main() {
  {
    int x = 42;
    int y = 27;
  }
  int x = 0;
  int y = 5;
  {
    int x = 27;
    int y = 42;
  }
  while (x<1000000) {
    x += 2;
    y += 5;
  }
  __VERIFIER_assert(x%2 == 0);
  return 0;
}
