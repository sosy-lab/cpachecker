// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Write to an array in a struct variable at a symbolic index (#1767).

extern int __VERIFIER_nondet_int(void);

struct lock {
  int locked;
};

struct s {
  int x;
  struct lock locks[3];
} g;

int main(void) {
  int i = __VERIFIER_nondet_int();
  if (i < 0 || i > 3) {
    return 0;
  }
  (&(g.locks[i]))->locked = 1; // UNSAFE for i == 3
  return 0;
}
