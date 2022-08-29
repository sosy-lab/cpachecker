// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int increment(int x) {
  return x = x + 1;
}


int main() {
  int a = 0;
  a = increment(a);
  if (a == 1) {
    goto ERROR;
  }
  return 0;
ERROR:
  return 1;
}
