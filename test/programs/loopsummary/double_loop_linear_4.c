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
  int x = 0;
  int i = 0;
  int k = 0;
  while (i<10000000) {
    i+=1;
    k = 0;
    while (k < i) {
        x += 1;
        k += 1;
    }
  }
  __VERIFIER_assert(x != i*(i+1)/2);
  return 0;
}
