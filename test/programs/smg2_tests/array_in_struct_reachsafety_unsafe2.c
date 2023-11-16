// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int size = 3;
  struct s {
    char a[size];
    char c;
  } s;

  char *p = (char*)&(s.c);
  char *q = (char*)&s;
  if (p != q + size) {
ERROR:
    return 1;
  }
  return 0;
}
