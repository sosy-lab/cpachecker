// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() {};
void __VERIFIER_assert(int cond) {
  if (!cond) {
    reach_error();
  }
}

int main() {
  int a = 0;
  int x[2] = {0, 0};
  while (a < 5) {
    x[a % 2] = a;
    a = a + 1;
  }
  __VERIFIER_assert(x[0] == 4 && a == 5);
  return 0;
}
