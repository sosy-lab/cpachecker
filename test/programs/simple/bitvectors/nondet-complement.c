// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned int __VERIFIER_nondet_int(void);

int main() {
  if (__VERIFIER_nondet_int() == ~__VERIFIER_nondet_int()) {
ERROR:
    return 1;
  }
  return 0;
}
