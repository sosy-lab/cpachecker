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
  int x = 2147483647-1000000;
  int i = 0;
  while (0 <= i && i<1000000) {
    i++;
    x+=2;
  }
  __VERIFIER_assert(x%2 == 0);
  return 0;
}