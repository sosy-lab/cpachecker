// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void test(int cond) {
  if (!cond) {
    ERROR: goto ERROR;
  }
}

int main(){
  int x = 0;

  while(1) {
    test(x == 0);
  }

  test(x == 0);
}
