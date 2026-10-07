// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The program creates no thread, so it has only one thread of execution and no two accesses to
// a memory location are concurrent.
extern int __VERIFIER_nondet_int(void);

int shared = 0;

int main(void) {
  int x = __VERIFIER_nondet_int();
  if (x > 0) {
    shared = x;
  }
  return shared;
}
