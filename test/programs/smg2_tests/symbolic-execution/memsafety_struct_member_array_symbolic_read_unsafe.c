// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Read from a struct in an array in a struct variable at a symbolic index (#1768).

extern int __VERIFIER_nondet_int(void);

struct node {
  struct node *next;
};

struct s {
  int x;
  struct node slots[3];
} g;

int main(void) {
  int i = __VERIFIER_nondet_int();
  if (i < 0 || i > 3) {
    return 0;
  }
  struct node *p = g.slots[i].next; // UNSAFE for i == 3
  return 0;
}
