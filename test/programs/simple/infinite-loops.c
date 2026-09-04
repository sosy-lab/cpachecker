// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

int main() {
  int i = __VERIFIER_nondet_int();

  if (i < -3) {
LOOP:
    goto LOOP;
  }
  if (i < -2) {
    while (1) { }
  }
  if (i < -1) {
    do { } while (1);
  }
  if (i != 0) {
    for (;;) { }
  }

  if (i < 0) {
ERROR:
    return 1;
  }
  return 0;
}
