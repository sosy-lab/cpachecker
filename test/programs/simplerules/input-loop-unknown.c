// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Whether the loop ends depends on the input, so every trivial rule has to abstain.
extern unsigned int __VERIFIER_nondet_uint(void);

int main(void) {
  unsigned int n = __VERIFIER_nondet_uint();
  while (n > 0) {
    n = n - 1;
  }
  return 0;
}
