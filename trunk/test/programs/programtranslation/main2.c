// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void __VERIFIER_assert(int cond) {
  if (!cond) {
    ERROR:
    goto ERROR;
  }
}

int main() {
  int i = 0;
  int x = 0;
  i++;
  while (x<10) {
    if (i>0) {
      i--;
    } else {
      i++;
    }
    __VERIFIER_assert(x<5);
    __VERIFIER_assert(i<2);
    x++;
  }
}
