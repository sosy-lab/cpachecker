// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned int __VERIFIER_nondet_uint();

int main() {
  unsigned int n = __VERIFIER_nondet_uint();
  int x = 0;
  int y = 0;
  while (x < n) {
    x++;
    y++;
    if (x != y) goto ERROR;
  }
  return 0;
ERROR:
  return 1;
}