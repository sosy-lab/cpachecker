// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned int __VERIFIER_nondet_uint(void);

int main() {
  unsigned int n = __VERIFIER_nondet_uint();
  if (0 % n <= 100) {
ERROR:
    return 1;
  }
  return 0;
}
