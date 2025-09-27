// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int i = 3;
  int j = 5;
  struct s1 {
    double d;
    struct s2 {
      char c;
      int a[i][j];
    } s2;
  } s1;
  if (sizeof(s1) == (i*j +1 /*padding*/)*sizeof(int) + sizeof(double)) {
ERROR:
    return 1;
  }
  return 0;
}
