// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The program has no loop, but it has a recursion, so the rule about loops has to abstain.
extern int __VERIFIER_nondet_int(void);

int sum(int n) {
  if (n <= 0) {
    return 0;
  }
  return n + sum(n - 1);
}

int main(void) { return sum(__VERIFIER_nondet_int()); }
