// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

int main() {
  unsigned int x = __VERIFIER_nondet_int();
  if (x < 212477449U) {
    return 0;
  }
  if (1139301431U == x * 65599U) {
ERROR:
    return 1;
  }
  return 0;
}
