// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() {};
int main(){
  int n = 0;
  int i = 0;
  int j = 42;
  int k = 42;
  while (k<1000000) {
    if (k%2==0) {
      if (j%3==0) {
        i = i + 1;
      } else {
        n = n + 1;
      }
    } else {
      j = j + 1;
    }
    i = i + 1;
    j = j + 1;
    k = k + 3;
  }
  int z = k-i-j-n;
  if (z) {
    reach_error();
  }
}
