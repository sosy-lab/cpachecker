// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The error is in a function that main() never calls, so no execution reaches it.
extern void reach_error(void);
extern int __VERIFIER_nondet_int(void);

void never_called(void) { reach_error(); }

int main(void) {
  int x = __VERIFIER_nondet_int();
  if (x > 0) {
    return 1;
  }
  return 0;
}
