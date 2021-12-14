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
  i++;
  x+=2;

  long long int iTmpVariableReallyTmp = i;
  long long int iterationsAmount = 1000000 - iTmpVariableReallyTmp;
  int xTmp = x;

  x = (iterationsAmount - 1)*2 + xTmp;
  (((iterationsAmount - 1)*2 + xTmp) != x) + 2147483647;

  __VERIFIER_assert(x%2 == 0);
  return 0;
}
