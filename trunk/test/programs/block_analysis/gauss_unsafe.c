// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned int __VERIFIER_nondet_uint();

int main() {
  unsigned int i, n = __VERIFIER_nondet_uint(), sn = 0;
  for (i = 0; i <= n; i++) {
    sn = sn + i;
  }
  if (!(sn == (n * (n + 1)) / 2 || sn == 0)) {
  ERROR:
    return 1;
  }
  return 0;
}
