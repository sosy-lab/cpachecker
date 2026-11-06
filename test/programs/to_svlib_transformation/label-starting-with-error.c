// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Only a label named "error" violates specification/ErrorLabel.spc, not one that starts with it.

extern int __VERIFIER_nondet_int(void);

int main(void) {
  int x = __VERIFIER_nondet_int();
  if (x > 0) {
  error_1:
    return 1;
  }
  if (x > 5) {
  ERROR:
    return 2;
  }
  return 0;
}
