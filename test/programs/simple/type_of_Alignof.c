// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() {}
int main() {
  int x = _Alignof(int);
  unsigned long long z = 2147483647 + _Alignof(int);
  if (z!= 2147483647ULL+_Alignof(int)) {
    reach_error();
  }
}

