// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Termination analysis must not copy values of incomplete types (#924).
extern int __VERIFIER_nondet_int(void);

struct incomplete;
extern struct incomplete global;
extern struct incomplete *pointer;

int main() {
  int n = __VERIFIER_nondet_int();
  int i = 0;
  while (i < n) {
    i++;
  }
  return 0;
}
