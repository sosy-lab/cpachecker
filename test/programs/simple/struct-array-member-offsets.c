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
    char a2[];  // flexible array member; aka a variable length array. This does NOT have any memory associated!
  } s;

  char *p = (char*)&(s.a2[0]); // The & and [0] erase each other, so this is essentially just the address of s + the offset. (memsafety not violated)
  char *q = (char*)&s;
  if (p != q + size + sizeof(int)) {
    return 0;
  }
  // CPAchecker internal note: if you are wondering why you MIGHT not end up here, 
  // take a look at: https://gitlab.com/sosy-lab/software/cpachecker/-/issues/1150
ERROR:
  return 1;
}
