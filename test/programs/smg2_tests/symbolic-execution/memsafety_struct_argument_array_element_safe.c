// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A struct argument is copied from the array element, not from the array start.

struct node {
  int v;
};

struct node arr[3];

int check(struct node n) { return n.v; }

int main(void) {
  arr[2].v = 5;
  if (check(arr[2]) != 5) {
    int *p = 0;
    *p = 1;
  }
  return 0;
}
