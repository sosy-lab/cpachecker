// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef unsigned long myint;

struct s {
  int a[(myint)10];
} s = { 0, 1, 2, 3, 4 };

int main() {
  if (s.a[5] != 0) {
ERROR:
    return 1;
  }
  return 0;
}
