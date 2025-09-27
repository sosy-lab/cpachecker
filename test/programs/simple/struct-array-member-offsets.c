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
    int i;
    char a1[size];
    char a2[];
  } s;

  char *p = (char*)&(s.a2[0]);
  char *q = (char*)&s;
  if (p != q + size + sizeof(int)) {
    return 0;
  }
ERROR:
  return 1;
}
